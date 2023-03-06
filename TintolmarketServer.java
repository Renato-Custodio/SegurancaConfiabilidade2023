
/***************************************************************************
 *   Seguranca e Confiabilidade 2020/21
 *
 *
 ***************************************************************************/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

//Servidor myServer

public class TintolmarketServer {

	public static void main(String[] args) {

		System.out.println("servidor: main");
		TintolmarketServer server = new TintolmarketServer();
		// verificar intervalo portas ???
		if (args.length > 0) {
			server.startServer(Integer.valueOf(args[0]));
		} else {
			server.startServer(12345);
		}

	}

	public void startServer(int port) {

		ServerSocket sSoc = null;
		File f = new File("clientPass.txt");
		try {
			f.createNewFile();
			sSoc = new ServerSocket(port);
			System.out.println("Servidor a correr...");

		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		while (true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		// sSoc.close();
	}

	// Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente"); // tirar
		}

		public void run() {
			
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				// user authentication

				String user = null;
				String password = null;

				try {
					user = (String) inStream.readObject();
					password = (String) inStream.readObject();

				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}

				boolean authentication = authenticate(outStream, user, password);
				outStream.writeObject(authentication);

				receiveCommands(inStream, outStream);

				outStream.close();
				inStream.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		private Boolean authenticate(ObjectOutputStream outStream, String user, String password) {
			Boolean result = true;
			try {
				FileReader myReader = new FileReader("clientPass.txt");
				BufferedReader br = new BufferedReader(myReader);
				FileWriter myWriter = new FileWriter("clientPass.txt");
				String content;
				boolean found = false;
				while ((content = br.readLine()) != null) {
					String[] userPass = content.split(":");
					if (userPass[0].equals(user)) {
						if (!userPass[1].equals(password)) {
							result = false;
							break;
						}

						found = true;
						break;
					}
				}

				if (!found) {
					// <userID>:<password>
					myWriter.write(user + ":" + password);
				}

				br.close();
				myReader.close();
				myWriter.close();
			} catch (IOException e) {
				System.err.println(e.getMessage());
				System.exit(-1);
			}

			return result;
		}





		private void receiveCommands(ObjectInputStream inStream, ObjectOutputStream outStream) {
			
			while (true) {
				
				String command = null;
				try {
					command = (String) inStream.readObject();
				} catch (ClassNotFoundException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				switch (command) {
				case "a":
				case "add":
					// add logic
					//String wine = (String) in.readObject(); 
					// pensar em estrutura a aolicar para arrecadar todos os dados de casa cliente, proodutos, imagens stocks, valores e quantidades
					// classe extra??
					// reduzir nivel de profundidade no acesso aos dados
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

				}
			}
		}
	}
}