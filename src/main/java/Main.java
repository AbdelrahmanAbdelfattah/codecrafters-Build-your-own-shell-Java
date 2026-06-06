import java.util.Scanner;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.regex.Pattern;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.EndOfFileException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Main {

    private static int tabCount = 0;
    private static String lastWord = null;

    public static List<String> getCompletions() {
        List<String> completions = new ArrayList<>();
        completions.add("echo");
        completions.add("exit");
        completions.add("type");
        completions.add("pwd");
        completions.add("cd");

        String pathVariable = System.getenv("PATH");
        String[] directories = pathVariable.split(Pattern.quote(File.pathSeparator));
        for (String directory : directories) {
            File dir = new File(directory);
            if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.canExecute() && !file.isDirectory()) {
                            completions.add(file.getName());
                        }
                    }
                }
            }
        }

        return completions;
    }

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

    public static void writeToFile(String filePath, String content, boolean append) {
        if (content == null || filePath == null) {
            return;
        }
        try {
            File file = new File(filePath);
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            java.io.FileWriter writer = new java.io.FileWriter(file, append);
            writer.write(content);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds a redirect operator (> or 1>) in the raw input string (outside quotes).
     * Returns an array: [commandPart, outputFilePath] or null if no redirection
     * found.
     */
    public static String[] parseRedirection(String input) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inSingleQuote) {
                if (c == '\'')
                    inSingleQuote = false;
            } else if (inDoubleQuote) {
                if (c == '"')
                    inDoubleQuote = false;
                else if (c == '\\' && i + 1 < input.length())
                    i++; // skip escaped char
            } else {
                if (c == '\'')
                    inSingleQuote = true;
                else if (c == '"')
                    inDoubleQuote = true;
                else if (c == '\\' && i + 1 < input.length()) {
                    i++; // skip escaped char
                } else if (c == '>' || (c == '1' && i + 1 < input.length() && input.charAt(i + 1) == '>')
                        || (c == '2' && i + 1 < input.length() && input.charAt(i + 1) == '>')) {
                    // Found redirect operator
                    int opStart = i;
                    int opEnd;
                    String type = null;
                    String mode = "overwrite";
                    if (c == '1') {
                        opEnd = i + 2; // skip "1>"
                        type = "1";
                        // Check for 1>>
                        if (opEnd < input.length() && input.charAt(opEnd) == '>') {
                            opEnd++;
                            mode = "append";
                        }
                    } else if (c == '2') {
                        opEnd = i + 2; // skip "2>"
                        type = "2";
                        if (opEnd < input.length() && input.charAt(opEnd) == '>') {
                            opEnd++;
                            mode = "append";
                        }
                    } else {
                        opEnd = i + 1; // skip bare ">"
                        type = "1";
                        // Check for >>
                        if (opEnd < input.length() && input.charAt(opEnd) == '>') {
                            opEnd++;
                            mode = "append";
                        }
                    }
                    String commandPart = input.substring(0, opStart).trim();
                    String filePath = input.substring(opEnd).trim();
                    return new String[] { commandPart, filePath, type, mode };
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        DefaultParser parser = new DefaultParser();
        parser.setEscapeChars(new char[] {});
        parser.setQuoteChars(new char[] {});

        List<String> allCompletions = getCompletions();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .completer((r, line, candidates) -> {
                    String word = line.word();

                    if (line.wordIndex() == 0) {
                        // Command completion (first word)
                        for (String comp : allCompletions) {
                            if (comp.startsWith(word)) {
                                candidates.add(new Candidate(comp));
                            }
                        }
                    } else {
                        // File/directory completion (arguments)
                        String prefix = word;
                        File dir;
                        String filePrefix;

                        // Check if the user typed a partial path (e.g. "subdir/fil")
                        int lastSep = prefix.lastIndexOf('/');
                        if (lastSep < 0) {
                            lastSep = prefix.lastIndexOf(File.separatorChar);
                        }

                        if (lastSep >= 0) {
                            String dirPath = prefix.substring(0, lastSep + 1);
                            filePrefix = prefix.substring(lastSep + 1);
                            dir = new File(dirPath);
                        } else {
                            dir = new File(".");
                            filePrefix = prefix;
                        }

                        if (dir.isDirectory()) {
                            File[] files = dir.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    if (file.getName().startsWith(filePrefix)) {
                                        String value;
                                        if (lastSep >= 0) {
                                            value = prefix.substring(0, lastSep + 1) + file.getName();
                                        } else {
                                            value = file.getName();
                                        }
                                        if (file.isDirectory()) {
                                            // Directory: append '/' and no trailing space (complete=false)
                                            value += "/";
                                            candidates.add(new Candidate(value, value, null, null, null, null, false));
                                        } else {
                                            // File: trailing space added automatically (complete=true)
                                            candidates.add(new Candidate(value, value, null, null, null, null, true));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Multi-tab behavior (bell on first, sorted list on second)
                    if (candidates.size() > 1) {
                        if (word.equals(lastWord)) {
                            tabCount++;
                        } else {
                            tabCount = 1;
                            lastWord = word;
                        }
                        if (tabCount >= 2) {
                            // Second TAB — show sorted list
                            List<String> names = new ArrayList<>();
                            for (Candidate c : candidates) {
                                names.add(c.value());
                            }
                            Collections.sort(names);
                            terminal.writer().println();
                            terminal.writer().println(String.join("  ", names));
                            terminal.writer().print("$ " + line.line());
                            terminal.writer().flush();
                        } else {
                            // First TAB — ring bell
                            terminal.writer().print("\007");
                            terminal.writer().flush();
                        }
                    }
                })
                .build();
        reader.unsetOpt(LineReader.Option.AUTO_LIST);
        reader.unsetOpt(LineReader.Option.AUTO_MENU);

        while (true) {
            String input;
            try {
                input = reader.readLine("$ ").trim();
            } catch (EndOfFileException | UserInterruptException e) {
                break;
            }
            if (input.isEmpty())
                continue;

            // Check for output redirection (> or 1>)
            String[] redirection = parseRedirection(input);
            String commandInput;
            String outputFile = null;
            String errorFile = null;
            boolean appendMode = false;

            if (redirection != null) {
                commandInput = redirection[0];
                appendMode = redirection[3].equals("append");
                if (redirection[2].equals("1")) {
                    outputFile = redirection[1];
                } else if (redirection[2].equals("2")) {
                    errorFile = redirection[1];
                }
            } else {
                commandInput = input;
            }

            // Parse the command input with quote awareness
            List<String> parts = parseArguments(commandInput);
            if (parts.isEmpty())
                continue;
            String command = parts.get(0);

            if (command.equals("exit")) {
                int exitCode = parts.size() > 1 ? Integer.parseInt(parts.get(1)) : 0;
                System.exit(exitCode);
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
                    writeToFile(outputFile, result + "\n", appendMode);
                } else if (errorFile != null) {
                    System.out.println(result); // stdout still goes to terminal
                    writeToFile(errorFile, "", appendMode); // create empty error file (no stderr from echo)
                } else {
                    System.out.println(result);
                }
            } else if (command.equals("type")) {
                if (parts.size() < 2)
                    continue;
                String target = parts.get(1);

                String result;
                if (target.equals("type") || target.equals("echo") || target.equals("exit")
                        || target.equals("pwd") || target.equals("cd")) {
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
                    writeToFile(outputFile, result + "\n", appendMode);
                } else if (errorFile != null) {
                    System.out.println(result); // stdout still goes to terminal
                    writeToFile(errorFile, "", appendMode); // no stderr from type
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
                        File outParent = outFile.getParentFile();
                        if (outParent != null) {
                            outParent.mkdirs();
                        }
                        if (appendMode) {
                            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outFile));
                        } else {
                            pb.redirectOutput(outFile);
                        }
                    } else if (errorFile != null) {
                        // Redirect only stderr to file; stdout stays on terminal
                        File errFile = new File(errorFile);
                        File errParent = errFile.getParentFile();
                        if (errParent != null) {
                            errParent.mkdirs();
                        }
                        if (appendMode) {
                            pb.redirectError(ProcessBuilder.Redirect.appendTo(errFile));
                        } else {
                            pb.redirectError(errFile);
                        }
                    }
                    pb.start().waitFor();
                } else {
                    System.out.println(command + ": command not found");
                }
            }
        }
    }
}
