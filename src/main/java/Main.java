import java.util.Scanner;
import java.util.zip.GZIPInputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        while(true){
            System.out.print("$ ");
            String ans = sc.nextLine();
            if (ans.equals("exit") || ans.startsWith("exit ")) {
                break;
            }

            if(ans.equals("echo") || ans.startsWith("echo ")){
                System.out.println(ans.length() > 4 ? ans.substring(5) : "");
            } else {
                System.out.println(ans + ": command not found");
            }
        }
    }
}
