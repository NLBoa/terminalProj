import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        while(true){

            System.out.print("$ ");
            String ans = sc.nextLine();
            String path = System.getenv("PATH");

            List<String> tokens = tokenize(ans);
            if(tokens.isEmpty()){
                continue;
            }

            String command = tokens.get(0).strip();
            List<String> commandArgs = tokens.subList(1, tokens.size());

            if (command.equals("exit")) {
                break;
            } else if(command.equals("type")){

                for(String c : commandArgs){
                    switch(c){
                        case "echo", "exit", "type" -> System.out.println(c + " is a shell builtin");
                        default -> {
                            if(!customPath(c, path, true)) {
                                System.out.println(c + ": not found");
                            }
                        }
                    }
                }
            }
            else if(command.equals("echo")){
                System.out.println(String.join(" ", commandArgs));
            }
            else if(customPath(command, path, false)){

                //ProcessBuilder needs the full argument list, with the program name as the first element
                String[] words = tokens.toArray(new String[0]);

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

    //splits a command line into tokens, treating single-quoted spans as literal text merged into the surrounding token
    private static List<String> tokenize(String input){
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean tokenStarted = false;
        boolean backslashStarted = false;

        for(int i = 0; i < input.length(); i++){
            char c = input.charAt(i);
            if(c == '\\' && backslashStarted == false && inSingleQuotes == false)
            { 
                if(inDoubleQuotes){
                    char next = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
                    if(isDoubleQuoteEscape(next)){
                        backslashStarted = true;
                    } else {
                        current.append(c);
                    }
                    tokenStarted = true;
                } else {
                    backslashStarted = true;
                    tokenStarted = true;
                }

            } else if(c == '\'' && inDoubleQuotes == false && backslashStarted == false){
                inSingleQuotes = !inSingleQuotes;
                tokenStarted = true;
            } else if(c == '\"' && backslashStarted == false && inSingleQuotes == false){
                inDoubleQuotes = !inDoubleQuotes;
                tokenStarted = true;
            } else if(Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes && backslashStarted == false){
                if(tokenStarted){
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
            } else {

                if(backslashStarted) backslashStarted = false;
                current.append(c);
                tokenStarted = true;
            }
        }

        if(tokenStarted){
            tokens.add(current.toString());
        }

        return tokens;
    }

    public static boolean isDoubleQuoteEscape(char next){
        return switch(next){
            case '\"', '\\', '$', '`', '\n' -> true;
            default -> false;
        };
    }
}