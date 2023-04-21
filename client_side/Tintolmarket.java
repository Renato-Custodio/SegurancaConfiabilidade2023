package client_side;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Scanner;

import javax.imageio.ImageIO;

public class Tintolmarket {
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public static void main(String[] args) throws FileNotFoundException, KeyStoreException {

        if (args.length < 5) {
            System.err.println();
            System.err.println("Forma de uso: Tintolmarket <serverAddress> <userID> [password]");
            System.err.println(
                    "Forma de uso: Tintolmarket <serverAddress> <truststore> <keystore> <password-keystore> <userID>");
            System.err.println();
            System.exit(-1);
        }
        String serverAddress = args[0];
        String userID = args[1];
        String password;

        Tintolmarket client = new Tintolmarket();
        // set Propreties
        System.setProperty("javax.net.ssl.trustStore", args[1]);
        System.setProperty("javax.net.ssl.trustStorePassword", args[3]);

        if (serverAddress.indexOf(":") != -1) {
            String[] hostPort = serverAddress.split(":");
            client.clientSocket = client.connectClient(hostPort[0], Integer.valueOf(hostPort[1]));
        } else {
            client.clientSocket = client.connectClient(args[0], 12345);
        }

        // get public key
        FileInputStream kfile = new FileInputStream(args[1]); // keystore
        KeyStore kstore = KeyStore.getInstance("JCEKS");
        try {
            kstore.load(kfile, "123456".toCharArray());
        } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Certificate certificate = kstore.getCertificate(args[2]);

        if (client.login(userID, certificate)) {
            printCommands();
        } else {
            /// se que voltar a pedir a pass
            System.err.println();
            System.err.println("Erro : password invalida");
            System.err.println();
            System.exit(-1);
        }

        client.run();

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

    private Boolean login(String userID, Certificate certificate) {
        Boolean isUser = false;
        try {

            out.writeObject(userID);
            out.writeObject(certificate.getPublicKey());

            isUser = (Boolean) in.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        return isUser;
    }

    private void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Comando: ");
            String[] command = scanner.nextLine().split(" ");
            try {
                switch (command[0]) {
                    case "a":
                    case "add":
                        // pedido ao server
                        File f = new File(command[2]);
                        if (f.exists()) {
                            out.writeObject(command[0]);
                            out.writeObject(command[1]);
                            byte[] content = Files.readAllBytes(f.toPath());

                            out.writeObject(content);
                            out.writeObject(command[2].split("\\.")[1]);
                            // resposta do server
                            System.out.println(in.readObject());
                        } else {
                            System.out.println("A imagem nao existe");
                        }
                        break;
                    case "s":
                    case "sell":
                        if (command.length >= 4 && isNumeric(command[2]) && isNumeric(command[3])) {
                            // pedido ao server
                            out.writeObject(command[0]);
                            out.writeObject(command[1]);
                            out.writeObject(Double.parseDouble(command[2]));
                            out.writeObject(Integer.parseInt(command[3]));
                            // resposta do server
                            System.out.println(in.readObject());
                            break;
                        }
                        System.out.println("Invalid Arguments.");
                        break;
                    case "v":
                    case "view":
                        // pedido ao server
                        if (command.length >= 2) {
                            out.writeObject(command[0]);
                            out.writeObject(command[1]);

                            // resposta do server
                            if ((Boolean) in.readObject()) {
                                BufferedImage bufferedImage = ImageIO
                                        .read(new ByteArrayInputStream((byte[]) in.readObject()));
                                String pathUser = "client_side/wineImages/";

                                File dir = new File(pathUser);
                                if (!dir.exists())
                                    dir.mkdir();

                                String nomeFicheiro = (String) in.readObject();

                                File foto = new File(pathUser, nomeFicheiro);
                                ImageIO.write(bufferedImage, nomeFicheiro.split("\\.")[1], foto);

                                foto.createNewFile();
                            }

                            System.out.println(in.readObject());
                        } else {
                            System.out.println("Invalid Arguments.");
                        }

                        break;
                    case "b":
                    case "buy":
                        if (command.length >= 4 && isNumeric(command[3])) {
                            // pedido ao server
                            out.writeObject(command[0]);
                            out.writeObject(command[1]);
                            out.writeObject(command[2]);
                            out.writeObject(Integer.parseInt(command[3]));
                            // resposta do server
                            System.out.println(in.readObject());
                        } else {
                            System.out.println("Invalid Arguments.");
                        }

                        break;
                    case "w":
                    case "wallet":
                        // pedido ao server
                        out.writeObject(command[0]);
                        // resposta do server
                        System.out.println("Saldo : " + in.readObject());
                        break;
                    case "c":
                    case "classify":
                        if (command.length >= 2 && isNumeric(command[2])) {
                            // pedido ao server
                            out.writeObject(command[0]);
                            out.writeObject(command[1]);
                            out.writeObject(Integer.parseInt(command[2]));
                            // resposta do server
                            System.out.println(in.readObject());
                        } else {
                            System.out.println("Invalid Arguments.");
                        }
                        break;
                    case "t":
                    case "talk":
                        // pedido ao server
                        if (command.length >= 3) {
                            if (command[2].replace(",", "").length() > 0) {
                                System.out.println("entrei");
                                out.writeObject(command[0]);
                                out.writeObject(command[1]);
                                out.writeObject(command[2].replace(",", ""));
                                System.out.println(in.readObject());
                            } else {
                                System.out.println("Message cannot only have commas.");
                            }
                            // resposta do server
                        } else {
                            System.out.println("Invalid Arguments.");
                        }
                        break;
                    case "r":
                    case "read":
                        // pedido ao server
                        out.writeObject(command[0]);
                        // resposta do server
                        System.out.println(in.readObject());
                        break;
                    case "q":
                    case "quit":
                        scanner.close();
                        in.close();
                        out.close();
                        return;
                    default:
                        System.out.println("Comando nao reconhecido");
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
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
        System.out.println("\tquit");
    }

    private boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

}
