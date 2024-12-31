import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.print("$ ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        String cwd = Path.of("").toAbsolutePath().toString();

        while (!input.matches("exit 0")) {
            String command = getCommand(input);
            String parameter = getParameter(input);

            switch (command) {
                case "echo":
                    handleEcho(input);
                    break;
                case "type":
                    handleType(parameter);
                    break;
                case "pwd":
                    handlePwd(cwd);
                    break;
                case "cd":
                    cwd = handleCd(input, cwd);
                    break;
                case "cat":
                    handleCat(input);
                    break;
                case "ls":
                    handleLs(input);
                    break;
                default:
                    handleExternalCommand(command, input);
                    break;
            }
            System.out.print("$ ");
            input = scanner.nextLine();
        }

        scanner.close();
    }

    // Method to extract the command from the input
    private static String getCommand(String input) {
        if (input.startsWith("\'") || input.startsWith("\"")) {
            return "cat";
        }
        return input.split(" ")[0];
    }

    // Method to extract the parameter from the input
    private static String getParameter(String input) {
        return input.length() > 1 ? input.substring(input.indexOf(" ") + 1) : "";
    }

    // Handle echo command
    private static void handleEcho(String input) {
        if (input.contains("1>") || input.contains(" >")) {
            boolean flag = false;
            if(input.contains("1>>")){ 
                input = input.replaceAll("1>>", ">");
                flag = true;
            }
            if(input.contains(">>")){ 
                input = input.replaceAll(">>", ">");
                flag = true;
            }
            String[] parts = input.split(">", 2);
            String content = parts[0].replaceAll("\"","").trim();
            String outputPath = parts[1].trim();
            content = content.replace("echo ", "").replaceAll("'", "").replaceAll("1", "").trim();
            try {
                if(!flag) writeToFile(outputPath, content);
                else appendToFile(outputPath, content);   
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (input.contains("2>")) {
            String[] parts = input.split("2>", 2); // Split at "2>"
            String content = parts[0].trim(); // Extract the content part
            String outputPath = parts[1].trim(); // Extract the file path
        
            content = content.replaceFirst("echo", "").trim(); 
            content = content.replaceAll("^['\"]|['\"]$", ""); // Remove surrounding quotes
    
            try {
                writeToFile(outputPath, "");
            } catch (IOException e) {
                e.printStackTrace();
            }
        
            File file = new File(outputPath);
            if (file.exists() && file.isFile()) {
                try {
                    if(Files.readString(file.toPath())!="")System.out.println(Files.readString(file.toPath())); // Print the file content
                    else System.out.println(content);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }               
        else if (input.substring(5).startsWith("'")) {
            System.out.println(input.substring(6, input.lastIndexOf("'")));
        } else if (input.substring(5).startsWith("\"")) {
            String strings = extractStrings(input);
            System.out.println(strings);
        } else {
            System.out.println(input.substring(5).trim().replaceAll("\\s+", " ").replaceAll("\\\\", ""));
        }
    }

    // Handle type command
    private static void handleType(String parameter) {
        String[] commands = { "exit", "echo", "type", "pwd", "cd" };
        if (Arrays.asList(commands).contains(parameter)) {
            System.out.println(parameter + " is a shell builtin");
        } else {
            String path = getPath(parameter);
            if (path != null) {
                System.out.println(parameter + " is " + path);
            } else {
                System.out.println(parameter + ": not found");
            }
        }
    }

    // Handle pwd command
    private static void handlePwd(String cwd) {
        System.out.println(cwd);
    }

    // Handle cd command
    private static String handleCd(String input, String cwd) {
        String dir = input.substring(3);

        // Replace ~ with HOME first
        if (dir.contains("~")) {
            String home = System.getenv("HOME");
            if (home != null) {
                dir = dir.replace("~", home);
            }
        }

        // Handle relative paths
        Path targetDir = Path.of(dir.startsWith("/") ? dir : Path.of(cwd).resolve(dir).toString());

        // Check if the directory exists
        if (Files.isDirectory(targetDir)) {
            return targetDir.normalize().toString();
        } else {
            System.out.printf("cd: %s: No such file or directory%n", dir);
        }
        return cwd;
    }

    // Handle cat command
    private static void handleCat(String input) {
        if (input.contains("1>")) {
            handleRedirectCat(input);
            return;
        }
        if(input.contains("2>>")){
            handleCatAppendError(input);
            return;
        }
        if (input.contains("2>")) {
            handleRedirectErrorCat(input);
            return;
        }
        if (input.startsWith("\'") || input.startsWith("\"")) {
            handleExe(input);
            return;
        }
        List<String> filePaths = extractPaths(input);
        if (filePaths.size() == 0) {
            handleExe(input);
            return;
        }
        StringBuilder content = new StringBuilder();
        for (String path : filePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try {
                    content.append(Files.readString(file.toPath()));
                } catch (IOException e) {
                    System.out.println("Error reading file: " + e.getMessage());
                }
            } else {
                System.out.println("Invalid path or file does not exist: " + path);
            }
        }
        System.out.println(content.toString().trim());
    }

    public static void handleCatAppendError(String input) {
        String command = input.replaceAll("2>> ", "");
        command = command.replaceAll("cat ", "");
        String[] parts = command.split(" ", 2);
        String directory = parts[0].trim();
        String outputPath = parts[1].trim();
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            try {
                appendToFile(outputPath, "cat: "+directory+": No such file or directory");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void handleRedirectErrorCat(String input) {
        String command = input.replaceAll("cat", "")
                .replaceAll("2>", "")
                .replaceAll(">", "")
                .trim();
        String[] parts = command.split(" ", 3);
        String directory = parts[0];
        String fil = parts[1];
        String outputPath = parts[2].trim();
        
        String filePath = directory + "/" + fil;
        File file = new File(filePath);
        String content = "";
        if (file.exists() && file.isFile()) {
            try {
                content = new String(Files.readAllBytes(file.toPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String[] part = directory.split("/", 4);
            System.err.println(part[3]);
            try {
                writeToFile(outputPath, "cat: " + fil + ": No such file or directory");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            writeToFile(outputPath, content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void handleRedirectCat(String input) {
        String command = input.replaceAll("cat", "")
                .replaceAll("1>", "")
                .replaceAll(">", "")
                .trim();
        String[] parts = command.split(" ", 3);
        String directory = parts[0];
        String fil = parts[1];
        String outputPath = parts[2].trim();
        
        String filePath = directory + "/" + fil;
        File file = new File(filePath);
        String content = "";
        if (file.exists() && file.isFile()) {
            try {
                content = new String(Files.readAllBytes(file.toPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("cat: " + fil + ": No such file or directory");
            String[] part = directory.split("/", 4);
            try {
                writeToFile(outputPath, part[3]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            writeToFile(outputPath, content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handleExe(String input) {
        String path = "";
        int i;
        for (i = input.length() - 1; i >= 0; i--) {
            if (input.charAt(i) == ' ')
                break;
        }
        path = input.substring(i + 1);
        String content = "";
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            try {
                content = Files.readString(file.toPath());
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
            }
        } else {
            System.out.println("Invalid path or file does not exist: " + path);
        }
        System.out.println(content.toString().trim());
    }

    public static void handleLs(String input) {
        if(input.contains(" >>") || input.contains("1>>")){
            handleLsAppend(input);
            return;
        }
        if(input.contains("2>>")){
           handleLsErrorAppend(input); 
            return;
        }
        if(input.contains("2>")) {
            handlestderr(input);
            return;
        }
        if (input.contains("1>"))
            input.replaceAll("1>", ">");
        String[] parts = input.split(">", 2);
        String task = parts[0].trim();
        String outputPath = parts[1].trim();
        String directory = task.replace("ls", "").trim();
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            try {
                throw new FileNotFoundException("Directory not found: " + directory);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        String[] files = dir.list();
        Arrays.sort(files);

        if (files == null)
            files = new String[0];

        try {
            writeToFile(outputPath, String.join("\n", files));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handleLsAppend(String input) {
        String command = input.replaceAll("1>> ", "");
        command = command.replaceAll(">> ", "").replaceAll("ls ", "");
        String[] parts = command.split(" ", 2);
        String directory = parts[0].trim();
        String outputPath = parts[1].trim();
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            System.out.println("ls: cannot access '"+directory+"': No such file or directory");
            try {
                writeToFile(outputPath, "");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String[] files = dir.list();
        Arrays.sort(files);
        if (files == null)
            files = new String[0];

        try {
            appendToFile(outputPath, String.join("\n", files));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handleLsErrorAppend(String input) {
        String command = input.replaceAll("2>> ", "");
        command = command.replaceAll("ls ", "");
        String[] parts = command.split(" ", 2);
        String directory = parts[0].trim();
        String outputPath = parts[1].trim();
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            try {
                appendToFile(outputPath, "ls: cannot access '"+directory+"': No such file or directory");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void handlestderr(String input){
        String[] parts = input.split(">", 2);
        String task = parts[0].trim();
        String outputPath = parts[1].trim();
        String directory = task.replace("ls", "").replaceAll("2", "").trim();
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            try {
                writeToFile(outputPath, "ls: cannot access '"+directory+"': No such file or directory");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Handle external command
    private static void handleExternalCommand(String command, String input) {
        String path = getPath(command);
        if (path == null) {
            System.out.printf("%s: command not found%n", command);
        } else {
            String fullPath = path + input.substring(command.length());
            try {
                Process p = Runtime.getRuntime().exec(fullPath.split(" "));
                p.getInputStream().transferTo(System.out);
            } catch (IOException e) {
                System.out.println("Error executing command: " + e.getMessage());
            }
        }
    }

    // Method to get the path of a command
    private static String getPath(String parameter) {
        for (String path : System.getenv("PATH").split(":")) {
            Path fullPath = Path.of(path, parameter);
            if (Files.isRegularFile(fullPath)) {
                return fullPath.toString();
            }
        }
        return null;
    }

    // Method to extract paths (handles both single and double quotes)
    private static List<String> extractPaths(String input) {
        List<String> paths = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"([^\"]*)\"|'([^']*)'");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                paths.add(matcher.group(1)); // Double-quoted string
            } else if (matcher.group(2) != null) {
                paths.add(matcher.group(2)); // Single-quoted string
            }
        }
        return paths;
    }

    // Method to extract strings within double quotes
    private static String extractStrings(String input) {
        String output = "";
        int count = 0;
        boolean flag = false;
        String temp = "";
        for (int i = 5; i < input.length(); i++) {
            if (flag && input.charAt(i) == ' ') {
                temp += ' ';
                continue;
            }
            if (flag && input.charAt(i) == '"') {
                output += " ";
                flag = false;
                continue;
            }
            if (flag && input.charAt(i) != '"') {
                output += temp;
                flag = false;
            }
            if (input.charAt(i) == '"' && count == 0) {
                flag = true;
                continue;
            }
            if (input.charAt(i) == '\\' && count == 0) {
                count++;
                continue;
            }
            output += input.charAt(i);
            count = 0;
        }
        return output;
    }

    public static void writeToFile(String filePath, String content) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    public static void appendToFile(String filePath, String content) throws IOException {
        File file = new File(filePath);
        content = System.lineSeparator() + content;
        file.getParentFile().mkdirs(); // Ensure parent directories exist
        Files.writeString(
            file.toPath(),
            content,
            StandardOpenOption.CREATE, // Create the file if it doesn't exist
            StandardOpenOption.APPEND  // Append content if the file exists
        );
    }
}
