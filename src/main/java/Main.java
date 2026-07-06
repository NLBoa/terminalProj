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

                switch(ans.substring(5)){
                    case "echo", "exit", "type" -> {
                        System.out.println(ans.substring(5) + " is a shell builtin");
                    }
                    default -> { System.out.println(ans.substring(5) + ": not found"); }
                    
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
