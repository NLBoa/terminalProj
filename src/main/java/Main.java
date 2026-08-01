import java.io.File;
import java.io.FileOutputStream;
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
    static List<Job> jobs = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        while(true){
            doneJobs();
            System.out.print("$ ");
            String ans = sc.nextLine();
            String path = System.getenv("PATH");
            List<String> tokens = tokenize(ans);

            if(!tokens.isEmpty() && tokens.get(tokens.size() - 1).equals("&")){
                List<String> bgCommand = tokens.subList(0, tokens.size() - 1);
                ProcessBuilder pb = new ProcessBuilder(bgCommand).inheritIO();
                Process process = pb.start();

                Job job = new Job(nextJobNumber(), process.pid(), process, bgCommand);
                jobs.add(job);
                System.out.println("[" + job.jobNumber + "] " + job.pid);
                continue;
            } else if(tokens.contains(">")){
                int redirectIndex = tokens.indexOf(">");
                List<String> commandAction = tokens.subList(0, redirectIndex);
                String fileName = tokens.get(redirectIndex + 1);
                String redirectCommand = commandAction.get(0);
                List<String> redirectArgs = commandAction.subList(1, commandAction.size());
                redirectOutput(redirectCommand, redirectArgs, path, fileName, false);
                continue;
            }
            if(tokens.isEmpty()){
                continue;
            }

            String command = tokens.get(0).strip();
            List<String> commandArgs = tokens.subList(1, tokens.size());

            if(ans.contains("|")){
                pipelineControlCenter(tokens, path);
                continue;
            }

            runOutputs(ans, command, commandArgs, tokens, path);
        }

    }

    private static void redirectOutput(String command, List<String> args, String path, String filePath, boolean append) throws IOException, InterruptedException {
        
        boolean isBuiltinCmd = isBuiltin(command);

        if(isBuiltinCmd){
            PrintStream original = System.out;
            System.setOut(new PrintStream(new FileOutputStream(filePath, append)));
            runBuiltin(command, args, path);
            System.out.flush();
            System.setOut(original);
        } else {
            List<String> full = new ArrayList<>(args);
            full.add(0, command);
            ProcessBuilder pb = new ProcessBuilder(full);
            pb.redirectOutput(append
                ? ProcessBuilder.Redirect.appendTo(new File(filePath))
                : ProcessBuilder.Redirect.to(new File(filePath)));
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process process = pb.start();
            process.waitFor();
        }
    }

    private static int nextJobNumber(){
        int candidate = 1;
        while(true){
            int c = candidate;
            boolean inUse = jobs.stream().anyMatch(j -> j.jobNumber == c);
            if(!inUse){
                return candidate;
            }
            candidate++;
        }
    }

    static class Job{
        int jobNumber;
        long pid;
        Process process;
        List<String> command;
        Job(int jobNumber, long pid, Process process, List<String> command){
            this.jobNumber = jobNumber;
            this.pid = pid;
            this.process = process;
            this.command = command;
        }
    }

    private static void pipelineControlCenter(List<String> commands, String path) throws IOException, InterruptedException{

        boolean isBuiltin = false;

        for(int i = 0; i < commands.size(); i++){
            String val = commands.get(i);

            if(isBuiltin((val))){
                isBuiltin = true;
            }
        }

        ArrayList<ArrayList<String>> commandSegments = new ArrayList<>();
        ArrayList<String> val = new ArrayList<>();
        for(int i = 0; i < commands.size(); i++){
            String c = commands.get(i);
            if(!c.equals("|")){
                val.add(c);
            } else {
                commandSegments.add(val);
                val = new ArrayList<>();
            }
        }
        commandSegments.add(val);

        if(!isBuiltin){
            nonBuiltinPipeline(commandSegments, commands.get(0));
            return;
        }

        List<String> headSeg = commandSegments.get(0);
        List<String> tailSeg = commandSegments.get(1);
        boolean headIsBuiltin = isBuiltin(headSeg.get(0));
        boolean tailIsBuiltin = isBuiltin(tailSeg.get(0));

        if(headIsBuiltin && !tailIsBuiltin){
            pipelineBuiltinToNot(tailSeg, headSeg, path);
        } else if(!headIsBuiltin && tailIsBuiltin){
            pipelineNottoBuiltin(headSeg, tailSeg, path);
        }
        // both builtin: no external process is involved on either side, so there's
        // no pipe to wire up yet — left unhandled until a builtin needs stdin.
    }

    private static void nonBuiltinPipeline(ArrayList<ArrayList<String>> list, String file) throws IOException, InterruptedException{
        List<ProcessBuilder> pb = new ArrayList<ProcessBuilder>();

        for(ArrayList<String> a : list){
            pb.add(new ProcessBuilder(a));
        }

        pb.get(pb.size() - 1).redirectOutput(ProcessBuilder.Redirect.INHERIT);

        List<Process> process = ProcessBuilder.startPipeline(pb);
        process.get(process.size() - 1).waitFor();
    }

    //Pipeline for only builtin Commands
    private static void pipelineBuiltinToNot(List<String> tailArgs, List<String> headArgs, String path) throws IOException, InterruptedException{

        String headCommand = headArgs.get(0);

        ProcessBuilder tailBuilder = new ProcessBuilder(tailArgs);
        tailBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process tailProcess = tailBuilder.start();

        PrintStream original = System.out;
        System.setOut(new PrintStream(tailProcess.getOutputStream()));
        runBuiltin(headCommand, headArgs.subList(1, headArgs.size()), path);
        System.out.flush();
        System.setOut(original);
        tailProcess.getOutputStream().close();

        tailProcess.waitFor();
    }

    private static void pipelineNottoBuiltin(List<String> headArgs, List<String> tailArgs, String path) throws IOException, InterruptedException{

        String tailCommand = tailArgs.get(0);

        Process headProcess = new ProcessBuilder(headArgs).start();

        InputStream original = System.in;
        System.setIn(headProcess.getInputStream());
        runBuiltin(tailCommand, tailArgs.subList(1, tailArgs.size()), path);
        System.setIn(original);

        headProcess.waitFor();
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

            if(c == '>' && backslashStarted == false && inSingleQuotes == false){
                if(tokenStarted){
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
                tokens.add(">");
            }
            else if(c == '\\' && backslashStarted == false && inSingleQuotes == false)
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
            case "jobs" -> printJobs();
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
            case "echo", "exit", "type", "jobs" -> {return  true;}
            default -> { return false;}
        }
    }

    private static void runBuiltin(String command, List<String> args, String path){
        switch(command){
            case "echo" -> echo(args);
            case "exit" -> System.exit(0);
            case "type" -> type(args, path);
            case "jobs" -> System.out.print("");
        }
    }

    private static void echo(List<String> commandArgs){
        System.out.println(String.join(" ", commandArgs));
    }

    private static void doneJobs(){

        List<Job> finished = new ArrayList<>();

        for(int i = 0; i < jobs.size(); i++){
            
            String value;
            String status = jobs.get(i).process.isAlive() ? "Running" : "Done";
            String commandStr = String.join(" ", jobs.get(i).command) + (status.equals("Running") ? " &" : "");
            String paddedStatus = String.format("%-24s", status);

            if(status.equals("Done")){
                if(i == jobs.size() - 1){
                    value = "[" + jobs.get(i).jobNumber + "]+  " + paddedStatus + commandStr;
                } else if(i == jobs.size() - 2) {
                    value = "[" + jobs.get(i).jobNumber + "]-  " + paddedStatus + commandStr;
                } else {
                    value = "[" + jobs.get(i).jobNumber + "]   " + paddedStatus + commandStr;
                }
                
                    if(status.equals("Done")){
                    finished.add(jobs.get(i));
                }

                System.out.println(value); 

            }
        }

        jobs.removeAll(finished);
    }

    private static void printJobs(){

        List<Job> finished = new ArrayList<>();

        for(int i = 0; i < jobs.size(); i++)
        {
            String value;
            String status = jobs.get(i).process.isAlive() ? "Running" : "Done";
            String commandStr = String.join(" ", jobs.get(i).command) + (status.equals("Running") ? " &" : "");
            String paddedStatus = String.format("%-24s", status);
            if(i == jobs.size() - 1){
                value = "[" + jobs.get(i).jobNumber + "]+  " + paddedStatus + commandStr;
            } else if(i == jobs.size() - 2) {
                value = "[" + jobs.get(i).jobNumber + "]-  " + paddedStatus + commandStr;
            } else {
                value = "[" + jobs.get(i).jobNumber + "]   " + paddedStatus + commandStr;
            }

            if(status.equals("Done")){
                finished.add(jobs.get(i));
            }

            System.out.println(value);
        }

        jobs.removeAll(finished);
    };

    private static void type(List<String> commandArgs, String path){
        for(String c : commandArgs){
            switch(c){
                case "echo", "exit", "type", "jobs" -> System.out.println(c + " is a shell builtin");
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
