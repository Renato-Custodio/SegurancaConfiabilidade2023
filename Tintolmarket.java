import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;

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
        String serverAddress = args[0];
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
            // pedir pass
        }

        /*
         * client.login();
         * client.sendFile();
         */

        try {
            client.clientSocket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    public void sendFile() {
        try {
            File f = new File("clientFile");
            f.createNewFile();
            FileWriter myWriter = new FileWriter("clientFile");
            myWriter.write("Fui criado com sucesso");
            myWriter.close();
            byte[] content = Files.readAllBytes(f.toPath());

            out.writeObject(content);

            in.close();
            out.close();
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

    public void login() {

        String username = "Renato";
        String password = "boas";

        try {

            out.writeObject(username);
            out.writeObject(password);

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

}
