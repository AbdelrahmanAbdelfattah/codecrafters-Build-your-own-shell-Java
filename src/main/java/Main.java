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
             else if (command.contains("echo") )
             {
                 String result = command.replaceFirst("^echo\\s+", "");
                 System.out.println( result); // Prints "Hello World"
                 continue;
             }
             System.out.println(command+ ": command not found");
         }

    }
}
