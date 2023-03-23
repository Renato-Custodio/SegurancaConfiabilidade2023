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
import java.util.Scanner;

import javax.imageio.ImageIO;

//Servidor myServer

public class TintolmarketServer {
	List<User> userList = new ArrayList<>();
	List<Wine> wineList = new ArrayList<>();

	Scanner readWine;
	FileWriter writeWine;
	Scanner readUser;
	FileWriter writeUser;

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

		// getbackups
		try {
			File f = new File("server_side//clientPass.txt");
			f.createNewFile();

			// ficheiro para guardar informacao

			File dir = new File("server_side/backups");
			if (!dir.exists())
				dir.mkdir();

			File fileUser = new File("server_side/backups/userList.txt");
			File fileWine = new File("server_side/backups/wineList.txt");
			Boolean fileBool = fileWine.createNewFile();
			// testar
			readWine = new Scanner(fileWine);
			writeWine = new FileWriter(fileWine, true);
			if (!fileBool) {
				while (readWine.hasNext()) {
					wineList.add(Wine.deserialize(readWine.nextLine()));
				}
			}

			fileBool = fileUser.createNewFile();
			readUser = new Scanner(fileUser);
			writeUser = new FileWriter(fileUser, true);
			if (!fileBool) {
				while (readUser.hasNext()) {
					userList.add(User.deserialize(readUser.nextLine(), wineList));
				}
			}

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
				}

				if (!userList.contains(new User(user))) {
					currentUser = new User(user);
					userList.add(currentUser);
					writeUser.append(currentUser.serialize() + "\n");
					writeUser.flush();
				} else {
					currentUser = userList.stream()
							.filter(us -> us.getName().equals(user))
							.findFirst()
							.get();
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
			List<String> lines;
			File fUser = new File("server_side/backups/userList.txt");
			while (true) {
				String command = null;
				try {
					command = (String) inStream.readObject();

					byte[] image = null;
					String wineName = null;
					String user = null;
					Double value = null;
					Integer quantity = null;
					Integer stars = null;
					String message = null;

					switch (command) {
						case "a":
						case "add":
							wineName = (String) inStream.readObject();
							image = (byte[]) inStream.readObject();

							if (wineList.contains(new Wine(wineName))) {
								outStream.writeObject("Vinho Já Existe.");
								break;
							}
							// backup dentro do add
							add(wineName, image, (String) inStream.readObject());
							outStream.writeObject("Vinho adicionado com sucesso.");

							break;
						case "s":
						case "sell":
							// backup sell
							wineName = (String) inStream.readObject();
							value = (Double) inStream.readObject();
							quantity = (int) inStream.readObject();

							if (!wineList.contains(new Wine(wineName))) {
								outStream.writeObject("O vinho ainda nao foi adicionado");
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
							// backup
							lines = Files.readAllLines(fUser.toPath());
							for (String tempString : lines) {

								if (tempString.split("&")[0].equals(currentUser.getName())) {
									lines.remove(tempString);
									lines.add(currentUser.serialize());
									Files.write(fUser.toPath(), lines);
									break;
								}
							}

							break;
						case "v":
						case "view":
							wineName = inStream.readObject().toString();
							StringBuilder sb = new StringBuilder("Informaçoes para o vinho " + wineName + ":\n");
							outStream.writeObject(wineList.contains(new Wine(wineName)));
							if (wineList.contains(new Wine(wineName))) {

								// envio da imagem

								File f = null;
								for (File file : new File("server_side/wineImages/").listFiles()) {
									if (file.getName().split("\\.")[0].equals(wineName)) {
										f = file;
										break;
									}
								}

								byte[] content = Files.readAllBytes(f.toPath());
								outStream.writeObject(content);
								outStream.writeObject(f.getName());
								// informacoes
								sb.append("\tImagem : " + f.getName() + "\n");
								sb.append("\tclassificacao media: "
										+ wineList.get(wineList.indexOf(new Wine(wineName))).getClassificationAvarage()
										+ "\n");
								for (User tempUser : userList) {
									WineSell tempWineSell = tempUser.getWine(wineName);
									if (tempWineSell != null) {
										sb.append("\tvendedores:\n");
										sb.append(
												"\t\tNome: " + tempUser.getName() + ", Preco: "
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
							// backup buy
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

											// backup
											lines = Files.readAllLines(fUser.toPath());
											for (String tempString : lines) {

												if (tempString.split("&")[0].equals(currentUser.getName())) {
													lines.remove(tempString);
													lines.add(currentUser.serialize());
													Files.write(fUser.toPath(), lines);
												} else if (tempString.split("&")[0].equals(seller.getName())) {
													lines.remove(tempString);
													lines.add(seller.serialize());
													Files.write(fUser.toPath(), lines);
												}

											}

										} else {
											outStream.writeObject(
													"A quantidade de unidades requisitadas é superior ao stock disponível");
										}
									} else {
										outStream.writeObject("Saldo insuficiente");
									}
								} else {
									outStream.writeObject("O vinho nao existe");
								}
							} else {
								outStream.writeObject("O vendedor nao existe");
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
							if (!wineList.contains(new Wine(wineName))) {
								outStream.writeObject("Classificacao Falhada - Vinho inexistente");
								break;
							}

							Wine tempWine = wineList.get(wineList.indexOf(new Wine(wineName)));
							tempWine.setClassification(stars);

							outStream.writeObject("classificacao efetuada com sucesso");
							// backup
							File fWine = new File("server_side/backups/wineList.txt");
							lines = Files.readAllLines(fWine.toPath());
							for (String tempString : lines) {
								if (tempString.split(":")[0].equals(tempWine.getId())) {
									lines.remove(tempString);
									lines.add(tempWine.serialize());
									Files.write(fWine.toPath(), lines);
									break;
								}
							}
							break;
						case "t":
						case "talk":
							user = (String) inStream.readObject();
							message = (String) inStream.readObject();
							if (userList.indexOf(new User(user)) == -1) {
								outStream.writeObject("O utilizador nao esta no sistema");
								break;
							}
							User tempUser = userList.get(userList.indexOf(new User(user)));
							tempUser.reciveMessage(currentUser.getName(), message);
							outStream.writeObject("Mensagem envida");
							// backup
							lines = Files.readAllLines(fUser.toPath());
							for (String tempString : lines) {

								if (tempString.split("&")[0].equals(tempUser.getName())) {
									lines.remove(tempString);
									lines.add(tempUser.serialize());
									Files.write(fUser.toPath(), lines);
									break;
								}
							}
							break;
						case "r":
						case "read":
							// clear backed up messages from currentuser
							outStream.writeObject(currentUser.readMessages());
							// backup
							lines = Files.readAllLines(fUser.toPath());
							for (String tempString : lines) {

								if (tempString.split("&")[0].equals(currentUser.getName())) {
									lines.remove(tempString);
									lines.add(currentUser.serialize());
									Files.write(fUser.toPath(), lines);
									break;
								}
							}
							break;
						default:
							return;
					}
				} catch (IOException | ClassNotFoundException e) {
					// server continua a correr sem problemas
				}
			}
		}

		private void add(String wine, byte[] image, String extensao) throws IOException {
			BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image));
			String pathUser = "server_side/wineImages/";

			File dir = new File(pathUser);
			if (!dir.exists())
				dir.mkdir();

			File foto = new File(pathUser, wine + "." + extensao);
			ImageIO.write(bufferedImage, extensao, foto);

			foto.createNewFile();
			Wine tempWine = new Wine(wine);
			writeWine.append(tempWine.serialize() + "\n");
			writeWine.flush();
			wineList.add(tempWine);
		}
	}
}