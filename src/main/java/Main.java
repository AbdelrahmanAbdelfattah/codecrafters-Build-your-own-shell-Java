import java.util.Scanner;
import java.io.File;
import java.util.regex.Pattern;

public class Main {


    public static String getPath(String command){
        String pathVariable = System.getenv("PATH");

        // This will safely split by ';' on Windows and ':' on Linux
        String[] directories = pathVariable.split(Pattern.quote(File.pathSeparator));

        for (String directory : directories) {
            String PATH = directory +File.separator+command;
            File file = new File(PATH);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }


    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine().trim(); // Use a separate variable for the whole line
            if (input.isEmpty()) continue;

            // Split the string by spaces right away. It's useful for everything.
            String[] commandParts = input.split("\\s+");
            String command = commandParts[0]; // The actual command (e.g., "echo", "type", "ls")

            if (command.equals("exit")) {
                break;
            } else if (command.equals("echo")) {


                // Print everything after "echo "
                String result = input.replaceFirst("^echo\\s+", "");
                StringBuilder parsedString = new StringBuilder();
                for  (int i = 0; i < result.length(); i++) {
                    if (result.charAt(i) == '\'' && (i==0 || i == result.length()-1) )
                    {
                        continue;
                    }

                    else
                    {
                        if (result.charAt(i) == '\'' && result.charAt(i+1) == '\'')
                        {
                            i++;
                            continue;
                        }
                        parsedString.append(result.charAt(i));
                    }
                }
                System.out.println(parsedString);
                continue;


            } else if (command.equals("type")) {
                // The target of the type command is the second word
                if (commandParts.length < 2) continue;
                String target = commandParts[1];

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
                    // Now you have the path needed to start the process!
                    ProcessBuilder pb = new ProcessBuilder(commandParts);
                    pb.inheritIO().start().waitFor();
                } else {
                    // If it's not a builtin, and not in the PATH, then it's truly not found
                    System.out.println(input + ": command not found");
                }
            }
        }
    }
}
