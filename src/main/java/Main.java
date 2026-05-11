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
        // TODO: Uncomment the code below to pass the first stage

         Scanner scanner = new Scanner(System.in);
         while(true){
             System.out.print("$ ");
             String command = scanner.nextLine();
             if(command.equals("exit")){
                 break ;
             }
             else if (command.startsWith("echo") )
             {
                 String result = command.replaceFirst("^echo\\s+", "");
                 System.out.println( result); // Prints "Hello World"
                 continue;
             }
             else if (command.startsWith("type")) {
                 String result = command.replaceFirst("^type\\s+", "");

                 // Splits the string by spaces to separate the command from the arguments
                 String[] commandParts = command.split("\\s+");

                 if (result.equals("type") || result.equals("echo") ||  result.equals("exit")) {
                     System.out.println(result+ " is a shell builtin");
                 }
                 else {
                     String fullPath = getPath(result);
                     if (fullPath != null) {
                         // Now you have the path needed to start the process!
                         ProcessBuilder pb = new ProcessBuilder(commandParts);
                         pb.inheritIO().start().waitFor();
                     } else {
                         System.out.println(command + ": command not found");
                     }

                 }
             }
             else {
                 System.out.println(command+ ": command not found");

             }
         }

    }
}

