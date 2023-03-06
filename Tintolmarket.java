import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Tintolmarket {
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println();
            System.err.println("Forma de uso: Tintolmarket <serverAddress> <userID> [password]");
            System.err.println();
            System.exit(-1);
        }
        String serverAddress = args[0]; // TODO
        String userID = args[1];
        String password;

        Tintolmarket client = new Tintolmarket();

        // ve se existe a port
        if (serverAddress.indexOf(":") != -1) {
            String[] hostPort = serverAddress.split(":");
            client.clientSocket = client.connectClient(hostPort[0], Integer.valueOf(hostPort[1]));
        } else {
            client.clientSocket = client.connectClient(args[0], 12345);
        }

        if (args.length == 3) {
            password = args[2];
        } else {
            System.out.print("Introduza a password por favor : ");
            Scanner reader = new Scanner(System.in);
            password = reader.nextLine();
            reader.close();
        }

        if (client.login(userID, password)) {
            printCommands();
        } else {
            System.err.println();
            System.err.println("Erro : password invalida");
            System.err.println();
            System.exit(-1);
        }

        client.run();

        // client.sendFile();

        try {
            client.clientSocket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

    }

    public Socket connectClient(String host, int port) {
        Socket clientSocket = null;
        try {
            clientSocket = new Socket(host, port);
            System.out.println("connected");
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        return clientSocket;
    }

    private Boolean login(String userID, String password) {
        Boolean isUser = false;
        try {

            out.writeObject(userID);
            out.writeObject(password);

            isUser = (Boolean) in.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        return isUser;
    }

    private void run() {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine();
            switch (command) {
                case "a":
                case "add":
                    // add logic
                    break;
                case "s":
                case "sell":
                    // add logic
                    break;
                case "v":
                case "view":
                    // add logic
                    break;
                case "b":
                case "buy":
                    // add logic
                    break;
                case "w":
                case "wallet":
                    // add logic
                    break;
                case "c":
                case "classify":
                    // add logic
                    break;
                case "t":
                case "talk":
                    // add logic
                    break;
                case "r":
                case "read":
                    // add logic
                    break;
                default:
                    // Exit Code
                    scanner.close();
                    return;
            }
            printCommands();
        }
    }

    private static void printCommands() {
        System.out.println("Lista de comandos:");
        System.out.println("\tadd <wine> <image>");
        System.out.println("\tsell <wine> <value> <quantity>");
        System.out.println("\tview <wine>");
        System.out.println("\tbuy <wine> <seller> <quantity>");
        System.out.println("\twallet");
        System.out.println("\tclassify <wine> <stars>");
        System.out.println("\ttalk <user> <message>");
        System.out.println("\tread");
    }

    /*
     * public void sendFile() {
     * try {
     * File f = new File("clientFile");
     * f.createNewFile();
     * FileWriter myWriter = new FileWriter("clientFile");
     * myWriter.write("Fui criado com sucesso");
     * myWriter.close();
     * byte[] content = Files.readAllBytes(f.toPath());
     * 
     * out.writeObject(content);
     * 
     * in.close();
     * out.close();
     * } catch (IOException e) {
     * System.err.println(e.getMessage());
     * System.exit(-1);
     * }
     * }
     */

}
