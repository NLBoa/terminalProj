import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        while(true){
            System.out.print("$ ");
            String ans = sc.nextLine();
            System.out.println(ans + ": command not found");
        }  
    }
}
