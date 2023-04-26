package server_side;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

//Servidor myServer

public class TintolmarketServer {
	List<User> userList = new ArrayList<>();
	List<Wine> wineList = new ArrayList<>();

	Scanner readWine;
	FileWriter writeWine;
	Scanner readUser;
	FileWriter writeUser;
	String passCifra;

	public static void main(String[] args) throws InvalidKeyException, NumberFormatException, NoSuchAlgorithmException,
			InvalidKeySpecException, NoSuchPaddingException {
		// a pass de tudo é 123456
		System.out.println("servidor: main");
		TintolmarketServer server = new TintolmarketServer();

		if (checkArgs(args)) {
			// assign vars
			int setArgs = 0;
			if (args.length == 3) {
				setArgs = 1;
			}

			System.setProperty("javax.net.ssl.keyStore", args[2 - setArgs]);
			System.setProperty("javax.net.ssl.keyStorePassword", args[3 - setArgs]);

			server.passCifra = args[1 - setArgs];

			if (args.length == 4) {
				server.startServer(Integer.valueOf(args[0]));
			} else {
				server.startServer(12345);
			}
		} else {
			System.err
					.println("Forma de uso: TintolmarketServer <port> <password-cifra> <keystore> <password-keystore>");
		}

	}

	private static boolean checkArgs(String[] args) {
		File f;
		if (args.length == 4) {
			f = new File(args[2]);
		} else {
			f = new File(args[1]);
		}

		return args.length >= 3 && f.exists();
	}

	public void startServer(int port)
			throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException {

		SSLServerSocket sSoc = null;

		// getbackups
		try {

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

			// create secure connection
			ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
			sSoc = (SSLServerSocket) ssf.createServerSocket(port);

			System.out.println("Servidor a correr...");

		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		while (true) {
			try {
				ServerThread newServerThread = new ServerThread((sSoc.accept()));
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
		private ObjectOutputStream outStream;
		private ObjectInputStream inStream;

		ServerThread(Socket inSoc) {
			socket = inSoc;
		}

		public void run() {

			try {
				outStream = new ObjectOutputStream(socket.getOutputStream());
				inStream = new ObjectInputStream(socket.getInputStream());

				// user authentication

				String user = null;

				try {
					user = (String) inStream.readObject();
				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}

				boolean authentication = authenticate(user);
				outStream.writeObject(authentication);
				if (authentication) {
					receiveCommands(inStream, outStream);
				}
				outStream.close();
				inStream.close();
				socket.close();
			} catch (IOException | ClassNotFoundException | InvalidKeyException | SignatureException
					| InvalidKeySpecException | NoSuchAlgorithmException | CertificateException
					| NoSuchPaddingException | InvalidAlgorithmParameterException e) {
				e.printStackTrace();
			}
		}

		private void encrypt() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException,
				InvalidKeySpecException, InvalidKeyException {

			File f = new File("server_side/clientPass.txt");
			f.createNewFile();
			// encrypt file
			// genrateKey
			byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e,
					(byte) 0xea,
					(byte) 0xf2 };
			PBEKeySpec keySpec = new PBEKeySpec(passCifra.toCharArray(), salt, 20); // pass, salt, iterations
			SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
			SecretKey key = kf.generateSecret(keySpec);
			// encrypt

			Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
			c.init(Cipher.ENCRYPT_MODE, key);

			FileInputStream fis = new FileInputStream("server_side/clientPass.txt");
			FileOutputStream fos = new FileOutputStream("server_side/clientPass.cif");
			CipherOutputStream cos = new CipherOutputStream(fos, c);

			byte[] b = new byte[16];
			int i = fis.read(b);
			while (i != -1) {
				cos.write(b, 0, i);
				i = fis.read(b);
			}
			byte[] keyEncoded = key.getEncoded();
			FileOutputStream kos = new FileOutputStream("server_side/clientPass.key");
			ObjectOutputStream oos = new ObjectOutputStream(kos);
			oos.writeObject(keyEncoded);
			kos = new FileOutputStream("server_side/params.bin");
			oos = new ObjectOutputStream(kos);
			oos.writeObject(c.getParameters().getEncoded());

			oos.close();
			kos.close();
			fis.close();
			cos.close();
			Files.delete(f.toPath());
		}

		private void decrypt() throws IOException, ClassNotFoundException, NoSuchAlgorithmException,
				NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {

			File file1 = new File("server_side/clientPass.key");
			File file2 = new File("server_side/clientPass.cif");
			if (!file1.exists() || !file2.exists()) {
				File f = new File("server_side/clientPass.txt");
				f.createNewFile();
				return;
			}
			FileInputStream kos = new FileInputStream("server_side/clientPass.key");
			ObjectInputStream oos = new ObjectInputStream(kos);
			byte[] keyEncoded2 = (byte[]) oos.readObject();
			oos.close();
			kos.close();

			SecretKeySpec keySpec2 = new SecretKeySpec(keyEncoded2, "PBEWithHmacSHA256AndAES_128");

			AlgorithmParameters p = AlgorithmParameters.getInstance("PBEWithHmacSHA256AndAES_128");
			FileInputStream in = new FileInputStream("server_side/params.bin");
			ObjectInputStream oin = new ObjectInputStream(in);
			byte[] param = (byte[]) oin.readObject();
			p.init(param);
			Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
			c.init(Cipher.DECRYPT_MODE, keySpec2, p);
			FileOutputStream fis = new FileOutputStream("server_side/clientPass.txt");
			FileInputStream fos = new FileInputStream("server_side/clientPass.cif");
			CipherInputStream cos = new CipherInputStream(fos, c);

			byte[] b = new byte[16];
			int i = cos.read(b);
			while (i != -1) {
				fis.write(b, 0, i);
				i = cos.read(b);
			}
			cos.close();
			fis.close();
			fos.close();
		}

		private Boolean authenticate(String user) throws ClassNotFoundException, SignatureException,
				InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, CertificateException,
				NoSuchPaddingException, InvalidAlgorithmParameterException {

			try {
				decrypt();
				FileReader myReader = new FileReader("server_side/clientPass.txt");
				BufferedReader br = new BufferedReader(myReader);
				FileWriter myWriter = new FileWriter("server_side/clientPass.txt", true);

				String content;
				String[] found = null;
				while ((content = br.readLine()) != null) {
					String[] userPass = content.split(":");
					if (userPass[0].equals(user)) {
						found = userPass;
					}
				}

				Random random = new Random();
				long nonce = random.nextInt(9900000) + 100000L;

				outStream.writeObject(nonce);
				outStream.writeObject(found != null);

				if (found == null) {
					// <userID>:<password>
					long verifyNonce = (long) inStream.readObject();
					byte[] signedNonce = (byte[]) inStream.readObject();
					Certificate certificate = (Certificate) inStream.readObject();

					if (verifyNonce != nonce) {
						br.close();
						myReader.close();
						myWriter.close();
						encrypt();
						return false;
					}

					Signature s = Signature.getInstance("MD5withRSA");
					s.initVerify(certificate.getPublicKey());
					s.update(ByteBuffer.allocate(Long.BYTES).putLong(nonce).array());
					if (!s.verify(signedNonce)) {
						br.close();
						myReader.close();
						myWriter.close();
						encrypt();
						return false;
					}
					// verificar se funciona a autenticacao
					String pathUser = "server_side/usersCert/";

					File dir = new File(pathUser);
					if (!dir.exists())
						dir.mkdir();

					File fileCert = new File(pathUser + user + ".cert");
					FileOutputStream writeCert = new FileOutputStream(fileCert);
					writeCert.write(certificate.getEncoded());
					myWriter.write(user + ":"
							+ fileCert.getAbsolutePath() + "\n");
					br.close();
					myReader.close();
					myWriter.close();
					encrypt();
				} else {

					byte[] recivedNonce = (byte[]) inStream.readObject();
					Signature s = Signature.getInstance("MD5withRSA");
					File certFile = new File("server_side/usersCert/" + user + ".cert");
					FileInputStream fis = new FileInputStream(certFile);
					CertificateFactory cf = CertificateFactory.getInstance("X.509");
					Certificate cert = (X509Certificate) cf.generateCertificate(fis);
					s.initVerify(cert.getPublicKey());
					s.update(ByteBuffer.allocate(Long.BYTES).putLong(nonce).array());
					if (!s.verify(recivedNonce)) {
						br.close();
						myReader.close();
						myWriter.close();
						encrypt();
						return false;
					}
					br.close();
					myReader.close();
					myWriter.close();
					encrypt();
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

			return true;
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
								outStream.writeObject(false);
								break;
							}
							// backup dentro do add
							add(wineName, image, (String) inStream.readObject());
							outStream.writeObject(true);

							break;
						case "s":
						case "sell":
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
							System.out.println(currentUser);
							WineSell tWineSell = currentUser.getWine(wineName);
							if (tWineSell != null) {
								tWineSell.setQuantity(quantity);
								tWineSell.setValue(value);
							} else {
								currentUser.sellWine(new WineSell(wine, quantity, value));
							}

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
							wineName = (String) inStream.readObject();
							outStream.writeObject(wineList.contains(new Wine(wineName)));
							if (wineList.contains(new Wine(wineName))) {
								StringBuilder sb = new StringBuilder("Informaçoes para o vinho " + wineName + ":\n");
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
											for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();) {
												String tempString = iterator.next();
												if (tempString.split("&")[0].equals(currentUser.getName()) ||
														tempString.split("&")[0].equals(seller.getName())) {
													iterator.remove();
												}
											}

											lines.add(seller.serialize());
											lines.add(currentUser.serialize());
											Files.write(fUser.toPath(), lines);
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

							for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();) {
								String tempString = iterator.next();
								if (tempString.split(":")[0].equals(tempWine.getId())) {
									iterator.remove();
									break;
								}
							}

							lines.add(tempWine.serialize());
							Files.write(fWine.toPath(), lines);

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
							tempUser.receiveMessage(currentUser.getName(), message);
							outStream.writeObject("Mensagem envida");
							// backup
							lines = Files.readAllLines(fUser.toPath());

							for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();) {
								String tempString = iterator.next();
								if (tempString.split("&")[0].equals(tempUser.getName())) {
									iterator.remove();
									break;
								}
							}
							lines.add(tempUser.serialize());
							Files.write(fUser.toPath(), lines);

							break;
						case "r":
						case "read":
							// clear backed up messages from currentuser
							outStream.writeObject(currentUser.readMessages());
							// backup
							lines = Files.readAllLines(fUser.toPath());

							for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();) {
								String tempString = iterator.next();
								if (tempString.split("&")[0].equals(currentUser.getName())) {
									iterator.remove();
									break;
								}
							}

							lines.add(currentUser.serialize());
							Files.write(fUser.toPath(), lines);
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