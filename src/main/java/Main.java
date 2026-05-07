import java.util.Scanner;
import java.io.File;
import java.util.regex.Pattern;

public class Main {
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
                 boolean founded = false;
                 if (result.equals("type") || result.equals("echo") ||  result.equals("exit")) {
                     System.out.println(result+ " is a shell builtin");
                 }
                 else {
                     String pathVariable = System.getenv("PATH");

                     // This will safely split by ';' on Windows and ':' on Linux
                     String[] directories = pathVariable.split(Pattern.quote(File.pathSeparator));



                     for (String directory : directories) {
                         String PATH = directory +Pattern.quote(File.pathSeparator)+result;
                         if (new File(PATH).exists() && new File(PATH).canExecute()) {
                             System.out.println(result+ " is " +PATH);
                             founded = true;
                         }
                     }
                     if (!founded) {

                     }


                 }

             }
             else {
                 System.out.println(command+ ": command not found");

             }
         }

    }
}
