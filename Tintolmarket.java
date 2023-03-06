import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;

public class Tintolmarket {
    ObjectOutputStream out;
    ObjectInputStream in;
    
    public static void main(String[] args) {
        Tintolmarket client = new Tintolmarket();
        Socket clientSocket = client.connectClient();
        client.login(clientSocket);
        client.sendFile(clientSocket);

        try{
            clientSocket.close();
        }catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
    }

    public void sendFile(Socket clientSocket){
        try{
            File f = new File("clientFile");
            f.createNewFile();
            FileWriter myWriter = new FileWriter("clientFile");
            myWriter.write("Fui criado com sucesso");
            myWriter.close();
            byte[] content = Files.readAllBytes(f.toPath());

            out.writeObject(content);

            in.close();
            out.close();
        }catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
    }

    public Socket connectClient(){
        Socket clientSocket = null;
        try{
		    clientSocket = new Socket("127.0.0.1", 23456);
            System.out.println("connected");
            
        }catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
        return clientSocket;
    }


    public void login(Socket clientSocket){

        String username = "Renato";
        String password = "boas";

        try{
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.writeObject(username);
            out.writeObject(password);

            in = new ObjectInputStream(clientSocket.getInputStream());
            
        }catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
    }

}
