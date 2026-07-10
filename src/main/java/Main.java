import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        while(true){
            System.out.print("$ ");
            String ans = sc.nextLine();

            String path = System.getenv("PATH");

            if (ans.equals("exit") || ans.startsWith("exit ")) {
                break;
            } else if(ans.startsWith("type ")){

                String command = ans.substring(5);

                switch(command){
                    case "echo", "exit", "type" -> {
                        System.out.println(command + " is a shell builtin");
                        break;
                    }
                    default -> { 
                        if(!customPath(command, path, true)) {
                            System.out.println(command + ": not found");
                        }
                     }
                    
                }
            }
            else if(ans.equals(" echo") || ans.startsWith("echo ")){
                System.out.println(ans.length() > 4 ? ans.substring(5) : "");
            } else if(customPath((ans.substring(0, ans.indexOf(' '))), path, false)){


                //Breaks each word into it's own array
                String[] words = ans.trim().split("\\s+");
                
                //ProcessBuilder is a command that can execute external commands. InheritIO allows child processes to execute on parent terminal
                ProcessBuilder pb = new ProcessBuilder(words).inheritIO();

                //Process start begins the process set by process builder
                Process process = pb.start();

                //Waitfor is an await sync that pauses the program until process is complete to prevent any errors
                process.waitFor();

            } else {
                System.out.println(ans + ": command not found");
            }
        }
    }

    //checks to see if type path is valid or not (meant for path method)
    private static boolean customPath(String command, String path, Boolean isType){
        
        String[] dirs = path.split(":");

        for(String d : dirs) {
            
            Path p = Paths.get(d, command);

            if(Files.isExecutable(p) && isType){
                System.out.println(command + " is " + p);
                return true;
            } else if(Files.isExecutable(p)) {
                return true;
            }
        }

        return false;
    }
}
