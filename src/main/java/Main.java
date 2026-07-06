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
            if (ans.equals("exit") || ans.startsWith("exit ")) {
                break;
            } else if(ans.startsWith("type ")){

                String command = ans.substring(5);
                String path = System.getenv("PATH");

                switch(command){
                    case "echo", "exit", "type" -> {
                        System.out.println(command + " is a shell builtin");
                        break;
                    }
                    default -> { 
                        if(!customPath(command, path))
                            System.out.println(command + ": not found");
                     }
                    
                }
            }
            else if(ans.equals(" echo") || ans.startsWith("echo ")){
                System.out.println(ans.length() > 4 ? ans.substring(5) : "");
            } else {
                System.out.println(ans + ": command not found");
            }
        }
    }

    private static boolean customPath(String command, String path){
        
        String[] dirs = path.split(":");

        for(String d : dirs) {
            
            Path p = Paths.get(d, command);

            if(Files.isExecutable(p)){
                System.out.println(command + " is " + p);
                return true;
            }
        }

        return false;
    }
}
