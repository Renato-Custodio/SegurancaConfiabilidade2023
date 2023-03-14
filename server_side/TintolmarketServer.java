package server_side;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

//Servidor myServer

public class TintolmarketServer {
	List<User> userList = new ArrayList<>();
	List<Wine> wineList = new ArrayList<>();

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
		File f = new File("server_side/clientPass.txt");
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
				FileReader myReader = new FileReader("server_side/clientPass.txt");
				BufferedReader br = new BufferedReader(myReader);
				FileWriter myWriter = new FileWriter("server_side/clientPass.txt", true);

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
					}
				}

				if (!found) {
					// <userID>:<password>
					myWriter.write(user + ":" + password + "\n");
				} else {
					if (!userList.contains(new User(user))) {
						currentUser = new User(user);
						userList.add(currentUser);
					} else {
						currentUser = userList.stream()
								.filter(us -> us.getName().equals(user)) // verifiquem o que acham disto
								.findFirst()
								.get();
					}
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

				byte[] image = null;
				String wineName = null;
				String user = null;
				Double value = null;
				Integer quantity = null;
				Integer stars = null;
				String message = null;
				try {
					switch (command) {
						case "a":
						case "add":
							wineName = (String) inStream.readObject();
							image = (byte[]) inStream.readObject();

							if (wineList.contains(new Wine(wineName))) {
								outStream.writeObject("Vinho Já Existe.");
								break;
							}

							add(wineName, image);
							outStream.writeObject("Vinho adicionado com sucesso.");
							break;
						case "s":
						case "sell":
							wineName = (String) inStream.readObject();
							value = (Double) inStream.readObject();
							quantity = (int) inStream.readObject();

							if (!wineList.contains(new Wine(wineName))) {
								outStream.writeObject("Este utilizador não possui este vinho.");
								break;
							}

							Wine wine = null;
							for (Wine vinho : wineList) {
								if (vinho.getId().equals(wineName)) {
									wine = vinho;
									break;
								}
							}

							currentUser.sellWine(new WineSell(wine, quantity, value));

							outStream.writeObject(
									quantity + " unidades de vinho " + wineName + " posto à venda a " + value + ".");
							break;
						case "v":
						case "view":
							wineName = (String) inStream.readObject();
							StringBuilder sb = new StringBuilder("Informações para o vinho " + wineName + ":\n");

							if (wineList.contains(new Wine(wineName))) {

								// dar print da imagem

								File f = new File("server_side/wineImages/" + wineName + ".jpg");
								byte[] content = Files.readAllBytes(f.toPath());
								outStream.writeObject(content);

								// informacoes
								sb.append("\tclassificação media: "
										+ wineList.get(wineList.indexOf(new Wine(wineName))).getClassificationAvarage()
										+ "\n");
								for (User tempUser : userList) {
									WineSell tempWineSell = tempUser.getWine(wineName);
									if (tempWineSell != null) {
										sb.append("\tvendedores:\n");
										sb.append(
												"\t\tNome: " + tempUser.getName() + ", Preço: "
														+ tempWineSell.getValue()
														+ ", Quantidade: " + tempWineSell.getQuantity() + ".\n");
									}
								}
								outStream.writeObject(sb.toString());
							} else {
								outStream.writeObject("Erro : O vinho nao existe");
							}

							break;
						case "b":
						case "buy":
							wineName = (String) inStream.readObject();
							user = (String) inStream.readObject();
							quantity = (Integer) inStream.readObject();
							// verifica se o vendedor existe
							if (userList.contains(new User(user))) {
								User seller = userList.get(userList.indexOf(new User(user)));
								WineSell sellWine = seller.getWine(wineName);
								// verifica se o vinho existe
								if (sellWine != null) {
									// verifica se o comprador tem saldo suficiente para efetuar a compra
									if (currentUser.getBalance() >= sellWine.getValue() * quantity) {
										// verifica se o vendedor tem as unidades requisitadas pelo cliente
										if (sellWine.getQuantity() >= quantity) {
											// comprador
											currentUser.setBalance(
													currentUser.getBalance() - sellWine.getValue() * quantity);

											// vendedor
											seller.setBalance(seller.getBalance() + sellWine.getValue() * quantity);
											sellWine.setQuantity(sellWine.getQuantity() - quantity);

											outStream.writeObject("Compra efetuada com sucesso");

										} else {
											outStream.writeObject(
													"A quantidade de unidades requisitadas é superior ao stock disponível");
										}
									} else {
										outStream.writeObject("Saldo insuficiente");
									}
								} else {
									outStream.writeObject("O vinho não existe");
								}
							} else {
								outStream.writeObject("O vendedor não existe");
							}

							break;
						case "w":
						case "wallet":
							outStream.writeObject(currentUser.getBalance());
							break;
						case "c":
						case "classify":
							wineName = (String) inStream.readObject();
							stars = (Integer) inStream.readObject();

							Wine tempWine = wineList.get(wineList.indexOf(new Wine(wineName)));
							tempWine.setClassification(stars);

							outStream.writeObject("classificacao efetuada com sucesso");
							break;
						case "t":
						case "talk":
							user = (String) inStream.readObject();
							message = (String) inStream.readObject();

							User tempUser = userList.get(userList.indexOf(new User(user)));
							tempUser.reciveMessage(currentUser.getName(), message);
							outStream.writeObject("Mensagem envida");
							break;
						case "r":
						case "read":
							outStream.writeObject(currentUser.readMessages());
							break;
						default:
							return;
					}
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace(); // TODO different exception handler
				}
			}
		}

		private void add(String wine, byte[] image) throws IOException {

			BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image));
			String pathUser = "server_side/wineImages/";

			File foto = new File(pathUser, wine + ".jpg");
			ImageIO.write(bufferedImage, "jpg", foto);

			foto.createNewFile();

			wineList.add(new Wine(wine));
		}
	}
}