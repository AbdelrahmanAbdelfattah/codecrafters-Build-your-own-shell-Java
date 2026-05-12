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

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine().trim(); // Use a separate variable for the whole line
            if (input.isEmpty())
                continue;

            // Parse the input with single-quote awareness
            List<String> parts = parseArguments(input);
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
                System.out.println(sb.toString());
            } else if (command.equals("type")) {
                if (parts.size() < 2)
                    continue;
                String target = parts.get(1);

                if (target.equals("type") || target.equals("echo") || target.equals("exit")) {
                    System.out.println(target + " is a shell builtin");
                } else {
                    String fullPath = getPath(target);
                    if (fullPath != null) {
                        // FIX: type just prints the path, it doesn't run the process!
                        System.out.println(target + " is " + fullPath);
                    } else {
                        // FIX: Print exactly "[target]: not found"
                        System.out.println(target + ": not found");
                    }
                }
            } else {
                // FIX: This is where external execution goes!
                String fullPath = getPath(command);

                if (fullPath != null) {
                    // Pass quote-aware parsed arguments to ProcessBuilder
                    ProcessBuilder pb = new ProcessBuilder(parts);
                    pb.inheritIO().start().waitFor();
                } else {
                    System.out.println(command + ": command not found");
                }
            }
        }
    }
}
