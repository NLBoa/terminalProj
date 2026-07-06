import java.io.File;
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

                String[] dirs = path.split(File.pathSeparator);

                for(String d : dirs) {
                    Path p = Paths.get(d, command);

                    if(Files.isExecutable(p)){
                        System.out.println(command + "is" + path);
                        break;
                    }
                }
                
                switch(command){
                    case "echo", "exit", "type" -> {
                        System.out.println(command + " is a shell builtin");
                    }
                    default -> { System.out.println(command + ": not found"); }
                    
                }


            }
            else if(ans.equals(" echo") || ans.startsWith("echo ")){
                System.out.println(ans.length() > 4 ? ans.substring(5) : "");
            } else {
                System.out.println(ans + ": command not found");
            }
        }
    }
}
