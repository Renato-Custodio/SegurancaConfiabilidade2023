package server_side;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

//Servidor myServer

public class TintolmarketServer {
	List<User> userList = new ArrayList<>();

	public static void main(String[] args) {

		System.out.println("servidor: main");
		TintolmarketServer server = new TintolmarketServer();

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
			// ficheiro para guardar informacao
			/*
			 * File file = new File("clientData.txt");
			 * if (file.createNewFile()) {
			 * // clientName/outras cenas
			 * FileReader fileReader = new FileReader(file);
			 * Scanner reader = new Scanner(fileReader);
			 * // adiconar à lista de clientes
			 * while (reader.hasNextLine()) {
			 * reader.nextLine();
			 * }
			 * 
			 * }
			 */
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
		private User currentUser;

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
				if (authentication) {
					currentUser = new User(user);
					receiveCommands(inStream, outStream);
				}
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
				FileWriter myWriter = new FileWriter("clientPass.txt", true);

				String content;
				boolean found = false;
				while ((content = br.readLine()) != null) {
					String[] userPass = content.split(":");
					if (userPass[0].equals(user)) {
						found = true;
						if (!userPass[1].equals(password)) {
							result = false;
							break;
						}
						// ver na lista
						for (User us : userList) {
							if (us.getName().equals(user)) {
								currentUser = us;
								break;
							}
						}
					}
				}

				if (!found) {
					// <userID>:<password>
					myWriter.write(user + ":" + password + "\n");
					/*
					 * myWriter = new FileWriter("clientData.txt");
					 * myWriter.write("");
					 */
					currentUser = new User(user);
					userList.add(currentUser);
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
				String image = null;
				String wine = null;
				String user = null;
				Double value = null;
				Integer quantity = null;
				Integer stars = null;
				String message = null;
				try {
					switch (command) {
						case "a":
						case "add":
							wine = (String) inStream.readObject();
							image = (String) inStream.readObject();
							// TODO receive image

							// verificar se ja existe
							boolean found = false;
							for (User us : userList) {
								if (us.getWines().contains(new Wine(wine))) {
									outStream.writeObject("Vinho Já Existe.");
									found = true;
									break;
								}
							}

							// returnar exception

							if (!found) {
								// o add image ainda n foi testado
								add(wine, image);
								outStream.writeObject("Vinho adicionado com sucesso.");
							}
							break;
						case "s":
						case "sell":
							wine = (String) inStream.readObject();
							value = (Double) inStream.readObject();
							quantity = (int) inStream.readObject();

							if (!currentUser.getWines().contains(new Wine(wine))) {
								outStream.writeObject("Este utilizador não possui este vinho.");
								break;
							}

							Wine userWine = currentUser.getWine(wine);
							userWine.setValue(value);
							userWine.setQuantity(quantity);
							userWine.setSell(true);

							outStream.writeObject("Confirmação de ação de venda.");
							break;
						case "v":
						case "view":
							wine = (String) inStream.readObject();
							// add logic
							break;
						case "b":
						case "buy":
							wine = (String) inStream.readObject();
							user = (String) inStream.readObject();
							quantity = (Integer) inStream.readObject();
							// add logic
							break;
						case "w":
						case "wallet":
							// add logic
							break;
						case "c":
						case "classify":
							wine = (String) inStream.readObject();
							stars = (Integer) inStream.readObject();
							// add logic
							break;
						case "t":
						case "talk":
							user = (String) inStream.readObject();
							message = (String) inStream.readObject();
							// add logic
							break;
						case "r":
						case "read":
							// add logic
							break;
						default:
							return;
					}
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace(); // TODO different exception handler
				}
			}
		}

		private String add(String wine, String image) throws IOException {

			int width = 1000;
			int height = 1000;

			BufferedImage bufferedImage = new BufferedImage(width, height,
					BufferedImage.TYPE_INT_RGB);
			String pathUser = "server_side/imagens/";

			File foto = new File(pathUser, wine + ".jpg");
			// necessário?
			Graphics2D g2d = bufferedImage.createGraphics();
			g2d.setColor(Color.blue);
			g2d.fillOval(10, 10, width, height);
			ImageIO.write(bufferedImage, "jpeg", foto);

			foto.createNewFile();

			currentUser.addWine(wine);

			return "O vinho : " + wine + " foi adicionado";
		}

		private boolean isNumeric(String str) {
			if (str == null) {
				return false;
			}
			try {
				double num = Double.parseDouble(str);
			} catch (NumberFormatException e) {
				return false;
			}

			return true;
		}
	}
}