package com.julianhoelz.raspremote;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;


public final class Main {

    private static final String SPLIT_REGEX = "\\s+";
    private static final String PROMPT_MESSAGE = "opener> ";
    private static final String ERROR_MESSAGE = "Error! ";
    private static final String TABULATOR = "    ";
    private static final String configPath = "etc/openers";


    private static boolean run = true;

    private static HashMap<String, Opener> openersKey = new HashMap<>();
    private static HashMap<String, Opener> openersName = new HashMap<>();
    private static int port;
    private static int returnPort;
    private static UdpSocket socket;
    private static boolean started;

    private Main() {
        throw new UnsupportedOperationException();
    }

    private static void execute(String input) {
        Scanner commands = new Scanner(input);
        commands.useDelimiter(SPLIT_REGEX);
        String command;
        if (commands.hasNext()) {
            command = commands.next();
        } else {

            // Returns to a new command prompt, if nothing is entered
            commands.close();
            return;
        }
        switch (Character.toLowerCase(command.charAt(0))) {
            case 'n':
                newOpener(commands);
                break;
            case 'l':
                listOpeners();
                break;
            case 's':
                start();
                break;
            case 'h':
                help();
                break;
            case 'q':
                run = false;
                socket.stop();
                break;
            default:
                error("'" + command + "' is an unknown "
                        + "command. Enter 'help' for Help.");
        }
        commands.close();
    }

    private static void load() {
        File config = new File(configPath);
        boolean made = false;
        if (config.exists() && config.isDirectory()) {
            FileReader reader = null;

            for (final File fileEntry : config.listFiles()) {
                Properties prop = new Properties();
                try {
                    reader = new FileReader(fileEntry.getAbsolutePath());
                    prop.load(reader);
                }
                catch ( IOException e )
                {
                    error("Foreign object has been found in folder '/etc/opneners'! Please remove!");
                    e.printStackTrace();
                }
                finally
                {
                    try { reader.close(); } catch ( Exception e ) { }
                }
                Opener opener = new Opener(prop);
                openersKey.put(opener.getKey(), opener);
                openersName.put(opener.getName(), opener);
            }
        } else {
            made = config.mkdir();
        }
        if(made) {
            System.out.println("Directory '/etc/openers' has been created.");
        }
    }

    private static void newOpener(Scanner commands) {
        String name = readString(commands);
        String key = readString(commands);
        String confirmation = readString(commands);
        Integer address = readInt(commands);
        String modeName = readString(commands);

        if (isCorrect(name, key, confirmation, address, modeName)) {
            if (doesExist(name, key)) {
                error("Entered name or key does already exist.");
            } else {
                Mode mode = Mode.getMode(modeName);

                if (mode != null) {
                    Opener opener = new Opener(name, key, address, confirmation, mode);
                    openersKey.put(key, opener);
                    openersName.put(name, opener);
                    FileWriter writer = null;

                    try
                    {
                        writer = new FileWriter( configPath + "/" + opener.getName() + ".config" );

                        Properties prop = opener.getProperties();
                        prop.store( writer, opener.getName() );
                    }
                    catch ( IOException e )
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        try { writer.close(); } catch ( Exception e ) { }
                    }

                } else {
                    error("Mode does not exist!");
                }
            }
        } else {
            error("Wrong Format!");
        }
    }

    private static boolean doesExist(String name, String key) {
        return openersName.containsKey(name) || openersKey.containsKey(key);
    }

    private static boolean isCorrect(String name, String key, String
            confirmation, Integer address, String modeName) {
        return name != null && key != null && confirmation != null && address
                != null && modeName != null;
    }

    private static void listOpeners() {
        System.out.println("Port: " + port);
        System.out.println("Return Port: " + returnPort +"\n");
        for(Opener opener : openersKey.values()) {
            System.out.println(opener.toString());
        }
    }

    /**
     * @Deprecated
     * @param commands
     */
    private static void remove(Scanner commands) {
        String name  = readString(commands);
        if (openersName.containsKey(name)) {
            Opener opener = openersName.remove(name);
            openersKey.remove(opener.getKey());
        } else {
            error("No opener corresponding to entered name!");
        }
    }

    private static void start() {
        System.out.println("Program starting...");
        if (openersKey.isEmpty()) {
            error("No Openers configured!");
        } else {
            started = true;
            System.out.println("Program started.");
            String request;
            do {
                request = request("Enter 'stop' to return to config mode: ");
            } while (!request.equals("stop"));

            System.out.println("Program is stopping...");
            started = false;
            System.out.println("Program stopped. Returning to config mode.");
        }
    }

    public static void inform(String data) {
        if (started && openersKey.containsKey(data)) {
            Opener requested = openersKey.get(data);
            requested.action();
            socket.send(requested.getConfirmation());
        }
    }

    private static void help() {
        System.out.println("Help Menu:");
        System.out.println(TABULATOR + "new <name> <key> <confirmation> <pin> <mode> -- creates a new opener and "
                + "starts it.");
        System.out.println(TABULATOR + "list -- lists all openersKey");

        System.out.println("Pin numbering is done according to internal pi pin addresses.");
        System.out.println("Possible Modes are:");
        System.out.println(TABULATOR + "on -- turns the pin on");
        System.out.println(TABULATOR + "off -- turns the pin off");
        System.out.println(TABULATOR + "switch -- toggles the pin");
        System.out.println(TABULATOR + "impulse -- blinks the pin");
    }

    private static Integer readInt(Scanner commands) {
        Integer number;
        if (commands.hasNextInt()) {
            number = commands.nextInt();
        } else {
            number = null;
        }
        return number;
    }

    private static String readString(Scanner commands) {
        String string;
        if (commands.hasNext()) {
            string = commands.next();
        } else {
            string = null;
        }
        return string;
    }

    private static void error(String message) {
        System.out.println(ERROR_MESSAGE + message);
    }

    private static int initialize(String prompt) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(prompt);
        String input = reader.readLine();
        if (input != null) {
            Scanner scanner = new Scanner(input);
            if (scanner.hasNextInt()) {
                return readInt(scanner);
            } else {
                return initialize(prompt);
            }
        } else {
            return initialize(prompt);
        }
    }

    private static String request(String prompt) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(prompt);
        String input = null;
        try {
            input = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (input != null) {
            Scanner scanner = new Scanner(input);
            if (scanner.hasNext()) {
                return readString(scanner);
            } else {
                return request(prompt);
            }
        } else {
            return request(prompt);
        }
    }

    /**
     * The main method which is run as soon as the program is run.
     *
     * It creates a {@link BufferedReader} which reads the human input and
     * prints the command prompt message in order to signal the human that
     * the program is able to take a new command.
     *
     * @param args The shell input Parameters args are received here.
     * @throws IOException is thrown if the buffered reader throws one.
     */
    public static void main(String[] args) throws IOException {
        port = initialize("Port: ");
        returnPort = initialize("ReturnPort: ");
        socket = new UdpSocket(port, returnPort);
        Thread thread = new Thread(socket);
        thread.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (run) {
            System.out.print(PROMPT_MESSAGE);
            String input = reader.readLine();
            if (input == null) {
                break;
            }
            execute(input);
        }
        reader.close();
    }
}