import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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

            if(ans.contains("|")){
                List<String> pipeLineCommands = new ArrayList<>();
                pipeLineCommands.add(command);
                int pipeCommandIdx = tokens.indexOf("|");
                pipeLineCommands.add(tokens.get(pipeCommandIdx + 1));
                List<String> headArgs = new ArrayList<>(tokens.subList(1, pipeCommandIdx));
                List<String> tailArgs = new ArrayList<>(tokens.subList(pipeCommandIdx + 2, tokens.size()));

                pipeline(pipeLineCommands, headArgs, tailArgs, path);
                continue;
            }

            runOutputs(ans, command, commandArgs, tokens, path);
        }

    }

    private static List<String> pipelineTokenize(String input){

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for(int i = 0; i < input.length(); i++){
            char c = input.charAt(i);

            if(c == '|'){
                if(input.length() > i+1 && Character.isWhitespace(input.charAt(i+1))){
                    i++;
                }
                tokens.add("|");
            } else if(Character.isWhitespace(c)){
                tokens.add(current.toString());
                current.setLength(0);
            } else {
                    current.append(c);
            }
        }

        return tokens;

    }

    private static void pipelineControlCenter(List<String> commands){

        boolean isBuiltin = false;
        
        for(int i = 1; i < commands.size(); i++){
            String val = commands.get(i);

            if(isBuiltin((val))){
                isBuiltin = true;
            }
        }

        ArrayList<ArrayList<String>> notBuillitinPipelineList = new ArrayList<>();
        if(!isBuiltin){
            ArrayList<String> val = new ArrayList<>();
            for(int i = 1; i < commands.size(); i++){
                String c = commands.get(i);
                if(!c.equals("|")){
                    val.add(c);
                } else {
                    notBuillitinPipelineList.add(val);
                    val = new ArrayList<>();
                }
            }

            nonBuiltinPipeline(notBuillitinPipelineList);
        }

    }

    private static void nonBuiltinPipeline(ArrayList<ArrayList<String>> list){
        List<ProcessBuilder> pb = new ArrayList<ProcessBuilder>();

        for(ArrayList<String> a : list){
            pb.add(new ProcessBuilder(a));
        }

        
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

    private static void runOutputs(String ans, String command, List<String> args, List<String> tokens, String path) throws IOException, InterruptedException{

        switch(command){
            case "echo" -> echo(args);
            case "exit" -> System.exit(0);
            case "type" -> type(args, path);
            default -> {
                if(customPath(command, path, false)){
                    customPath(tokens);
                } else {
                    System.out.println(ans + ": command not found");
                }
            }
        }
    }

    private static boolean isBuiltin(String command){
        switch(command){
            case "echo", "exit", "type" -> {return  true;}
            default -> { return false;}
        }
    }

    private static void runBuiltin(String command, List<String> args, String path){
        switch(command){
            case "echo" -> echo(args);
            case "exit" -> System.exit(0);
            case "type" -> type(args, path);
        }
    }

    private static void echo(List<String> commandArgs){
        System.out.println(String.join(" ", commandArgs));
    }

    private static void type(List<String> commandArgs, String path){
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

    private static void customPath(List<String> tokens) throws IOException, InterruptedException{

        //ProcessBuilder needs the full argument list, with the program name as the first element
        String[] words = tokens.toArray(new String[0]);

        //ProcessBuilder is a command that can execute external commands. InheritIO allows child processes to execute on parent terminal
        ProcessBuilder pb = new ProcessBuilder(words).inheritIO();

        //Process start begins the process set by process builder
        Process process = pb.start();

        //Waitfor is an await sync that pauses the program until process is complete to prevent any errors
        process.waitFor();       
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

    public static void pipeline(List<String> commands, List<String> headArgs, List<String> tailArgs, String path) throws IOException, InterruptedException{

        //Should be pipeline Args, 

        String headCommand = commands.get(0);
        String tailCommand = commands.get(1);

        boolean headIsBuiltin = isBuiltin(headCommand);
        boolean tailIsBuiltin = isBuiltin(tailCommand);


        if(!headIsBuiltin && !tailIsBuiltin){
           headArgs.add(0, headCommand);
           tailArgs.add(0, tailCommand);

           List<ProcessBuilder> builders = new ArrayList<>();
           builders.add(new ProcessBuilder(headArgs));
           builders.add(new ProcessBuilder(tailArgs));
           builders.get(1).redirectOutput(ProcessBuilder.Redirect.INHERIT);

           List<Process> process = ProcessBuilder.startPipeline(builders);
           process.get(process.size() - 1).waitFor();

        } else if(headIsBuiltin && !tailIsBuiltin){
            tailArgs.add(0, tailCommand);
            ProcessBuilder tailBuilder = new ProcessBuilder(tailArgs);
            tailBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            Process tailProcess = tailBuilder.start();

            PrintStream original = System.out;
            System.setOut(new PrintStream(tailProcess.getOutputStream()));
            runBuiltin(headCommand, headArgs, path);
            System.out.flush();
            System.setOut(original);
            tailProcess.getOutputStream().close();

            tailProcess.waitFor();

        } else if(!headIsBuiltin && tailIsBuiltin){
            headArgs.add(0, headCommand);
            Process headProcess = new ProcessBuilder(headArgs).start();

            InputStream original = System.in;
            System.setIn(headProcess.getInputStream());
            runBuiltin(tailCommand, tailArgs, path);
            System.setIn(original);

            headProcess.waitFor();

        } else if( headIsBuiltin && tailIsBuiltin) {

        }
    }

}