import java.util.Scanner;

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
                 if (result.equals("type") || result.equals("echo") ||  result.equals("exit")) {
                     System.out.println(result+ " is a shell builtin");
                 }
                 else {
                     System.out.println(result+ ":not fuond");
                 }

             }
             else {
                 System.out.println(command+ ": command not found");

             }
         }

    }
}
