import java.util.Scanner;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Main {

    public static String getPath(String command) {
        String pathVariable = System.getenv("PATH");

        // This will safely split by ';' on Windows and ':' on Linux
        String[] directories = pathVariable.split(Pattern.quote(File.pathSeparator));

        for (String directory : directories) {
            String PATH = directory + File.separator + command;
            File file = new File(PATH);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    // Parses a raw input line into a list of arguments, respecting single quotes
    // Parses a raw input line into a list of arguments, respecting single and
    // double quotes
    public static List<String> parseArguments(String input) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inSingleQuote) {
                // Inside single quotes: EVERYTHING is literal except closing '
                if (c == '\'') {
                    inSingleQuote = false;
                } else {
                    current.append(c);
                }
            } else if (inDoubleQuote) {
                // Inside double quotes: most chars are literal, but \ can escape specific chars
                if (c == '"') {
                    inDoubleQuote = false;
                } else if (c == '\\' && i + 1 < input.length()) {
                    char next = input.charAt(i + 1);
                    // Backslash only escapes these 4 characters inside double quotes
                    if (next == '\\' || next == '"' || next == '$' || next == '`') {
                        current.append(next);
                        i++; // skip the next character, we already consumed it
                    } else {
                        current.append(c); // backslash is literal for anything else
                    }
                } else {
                    current.append(c);
                }
            } else {
                // Outside any quotes
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == '\\' && i + 1 < input.length()) {
                    // Backslash outside quotes: escape the next character (whatever it is)
                    current.append(input.charAt(i + 1));
                    i++;
                } else if (c == ' ') {
                    if (current.length() > 0) {
                        args.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(c);
                }
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }

    public static void writeToFile(String filePath, String content) {
        if (content == null || filePath == null) {
            return;
        }

        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs(); // create parent dirs if needed

            // Write content to the file
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(content);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds a redirect operator (> or 1>) in the raw input string (outside quotes).
     * Returns an array: [commandPart, outputFilePath] or null if no redirection found.
     */
    public static String[] parseRedirection(String input) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inSingleQuote) {
                if (c == '\'') inSingleQuote = false;
            } else if (inDoubleQuote) {
                if (c == '"') inDoubleQuote = false;
                else if (c == '\\' && i + 1 < input.length()) i++; // skip escaped char
            } else {
                if (c == '\'') inSingleQuote = true;
                else if (c == '"') inDoubleQuote = true;
                else if (c == '\\' && i + 1 < input.length()) {
                    i++; // skip escaped char
                } else if (c == '>' || (c == '1' && i + 1 < input.length() && input.charAt(i + 1) == '>')) {
                    // Found redirect operator
                    int opStart = i;
                    int opEnd;
                    if (c == '1') {
                        opEnd = i + 2; // skip "1>"
                    } else {
                        opEnd = i + 1; // skip ">"
                    }
                    String commandPart = input.substring(0, opStart).trim();
                    String filePath = input.substring(opEnd).trim();
                    return new String[] { commandPart, filePath };
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty())
                continue;

            // Check for output redirection (> or 1>)
            String[] redirection = parseRedirection(input);
            String commandInput;
            String outputFile = null;

            if (redirection != null) {
                commandInput = redirection[0];
                outputFile = redirection[1];
            } else {
                commandInput = input;
            }

            // Parse the command input with quote awareness
            List<String> parts = parseArguments(commandInput);
            if (parts.isEmpty())
                continue;
            String command = parts.get(0);

            if (command.equals("exit")) {
                break;
            } else if (command.equals("echo")) {
                // Join all parsed arguments after "echo" with a single space
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.size(); i++) {
                    if (i > 1)
                        sb.append(' ');
                    sb.append(parts.get(i));
                }
                String result = sb.toString();
                if (outputFile != null) {
                    writeToFile(outputFile, result + "\n");
                } else {
                    System.out.println(result);
                }
            } else if (command.equals("type")) {
                if (parts.size() < 2)
                    continue;
                String target = parts.get(1);

                String result;
                if (target.equals("type") || target.equals("echo") || target.equals("exit")) {
                    result = target + " is a shell builtin";
                } else {
                    String fullPath = getPath(target);
                    if (fullPath != null) {
                        result = target + " is " + fullPath;
                    } else {
                        result = target + ": not found";
                    }
                }

                if (outputFile != null) {
                    writeToFile(outputFile, result + "\n");
                } else {
                    System.out.println(result);
                }
            } else {
                String fullPath = getPath(command);

                if (fullPath != null) {
                    ProcessBuilder pb = new ProcessBuilder(parts);
                    pb.inheritIO();
                    if (outputFile != null) {
                        // Redirect only stdout to file; stderr stays on terminal
                        File outFile = new File(outputFile);
                        outFile.getParentFile().mkdirs();
                        pb.redirectOutput(outFile);
                    }
                    pb.start().waitFor();
                } else {
                    System.out.println(command + ": command not found");
                }
            }
        }
    }
}
