package server_side;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AlgorithmParameters;
import java.security.DigestInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
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

	private Scanner readWine;
	private FileWriter writeWine;
	private Scanner readUser;
	private FileWriter writeUser;
	private String passCifra;
	private String keyStore;
	private String passKeyStore;

	public static void main(String[] args) throws InvalidKeyException, NumberFormatException, NoSuchAlgorithmException,
			InvalidKeySpecException, NoSuchPaddingException, ClassNotFoundException, SignatureException,
			CertificateException {
		// a pass de tudo é 123456
		System.out.println("servidor: main");
		TintolmarketServer server = new TintolmarketServer();

		if (checkArgs(args)) {
			// assign vars
			int setArgs = 0;
			if (args.length == 3) {
				setArgs = 1;
			}
			server.keyStore = args[2 - setArgs];
			server.passKeyStore = args[3 - setArgs];

			System.setProperty("javax.net.ssl.keyStore", server.keyStore);
			System.setProperty("javax.net.ssl.keyStorePassword", server.passKeyStore);

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

	private boolean verifyBlocks() throws IOException, ClassNotFoundException, SignatureException,
			InvalidKeyException, CertificateException, NoSuchAlgorithmException {
		File dir = new File("server_side/log");
		if (!dir.exists())
			return true;

		File[] files = dir.listFiles();

		if (files.length == 0)
			return true;

		byte[] hashToVerify = null;
		for (int numFile = files.length - 1; numFile >= 0; numFile--) {
			File file = files[numFile];
			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream oIn = new ObjectInputStream(fis);
			byte[] hash = (byte[]) oIn.readObject(); // hash
			long id = (long) oIn.readObject(); // id
			long nTrx = (long) oIn.readObject(); // nTrx
			List<List<byte[]>> transacoes = new ArrayList<>();
			for (int i = 0; i < nTrx; i++) {
				transacoes.add((List<byte[]>) oIn.readObject()); // all transactions
			}
			// verify integrity
			if (nTrx == 5) {
				byte[] signature = (byte[]) oIn.readObject(); // signature
				FileInputStream kfile = new FileInputStream("server_side/key/Server.cer"); // certificate
				CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
				Certificate certificate = certificateFactory.generateCertificate(kfile);
				Signature s = Signature.getInstance("MD5withRSA");
				s.initVerify(certificate.getPublicKey());
				s.update(hash);
				s.update((byte) id);
				s.update((byte) (nTrx));
				for (int i = 0; i < nTrx; i++) {
					s.update(transacoes.get(i).get(0));
					s.update(transacoes.get(i).get(1));
				}

				if (!s.verify(signature)) {
					return false;
				}
			}
			// verify hash
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			if (hashToVerify != null) {

				FileInputStream f = new FileInputStream(file);
				DigestInputStream dis = new DigestInputStream(f, digest);

				while (dis.read() != -1) {
					// The digest is updated automatically as you read the file
				}
				dis.close();
				// Get the final hash (digest) value
				if (!MessageDigest.isEqual(digest.digest(), hashToVerify)) {
					return false;
				}
			}

			// verify signatures
			for (List<byte[]> it : transacoes) {
				String[] data = new String(it.get(0)).split(",");
				String user = data[data.length - 1];
				FileInputStream f = new FileInputStream("server_side/usersCert/" + user + ".cert");
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				Certificate cert = cf.generateCertificate(f);
				Signature s = Signature.getInstance("MD5withRSA");
				s.initVerify(cert.getPublicKey());
				s.update(it.get(0));
				if (!s.verify(it.get(1))) {
					return false;
				}
			}
			hashToVerify = hash;
		}
		return true;
	}

	public void startServer(int port)
			throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException,
			ClassNotFoundException, SignatureException, CertificateException {

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

			if (!verifyBlocks()) {
				System.out.println("A blockchain foi corrompida");
				return;
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
					| NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException
					| BadPaddingException | UnrecoverableKeyException | KeyStoreException e) {
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
			File file3 = new File("server_side/params.bin");
			if (!file1.exists() || !file2.exists() || !file3.exists()) {
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
					// <userID>:<path to cert>
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
							+ fileCert.getPath() + "\n");
					br.close();
					myReader.close();
					myWriter.close();
					encrypt();
				} else {
					byte[] recivedNonce = (byte[]) inStream.readObject();
					Signature s = Signature.getInstance("MD5withRSA");
					File certFile = new File(found[1]);
					FileInputStream fis = new FileInputStream(certFile);
					CertificateFactory cf = CertificateFactory.getInstance("X.509");
					Certificate cert = cf.generateCertificate(fis);
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

		private void receiveCommands(ObjectInputStream inStream, ObjectOutputStream outStream)
				throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
				InvalidAlgorithmParameterException, CertificateException, IllegalBlockSizeException,
				BadPaddingException, InvalidKeySpecException, SignatureException, UnrecoverableKeyException,
				KeyStoreException {
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
					FileInputStream fis = null;
					File certFile = null;
					CertificateFactory cf = null;
					Certificate cert = null;
					Signature s = null;
					String log = null;

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
							byte[] signature = (byte[]) inStream.readObject();
							if (!wineList.contains(new Wine(wineName))) {
								outStream.writeObject("O vinho ainda nao foi adicionado");
								break;
							}
							// verificao da assinatura
							log = "sell," + wineName + "," + value + "," + quantity + "," + currentUser.getName();

							s = Signature.getInstance("MD5withRSA");
							certFile = new File("server_side/usersCert/" + currentUser.getName() + ".cert");
							fis = new FileInputStream(certFile);
							cf = CertificateFactory.getInstance("X.509");
							cert = cf.generateCertificate(fis);
							s.initVerify(cert.getPublicKey());
							s.update(log.getBytes());
							if (!s.verify(signature)) {
								outStream.writeObject("A assinatura nao e valida");
								break;
							}

							Wine wine = null;
							for (Wine vinho : wineList) {
								if (vinho.getId().equals(wineName)) {
									wine = vinho;
									break;
								}
							}
							WineSell tWineSell = currentUser.getWine(wineName);
							if (tWineSell != null) {
								tWineSell.setQuantity(quantity);
								tWineSell.setValue(value);
							} else {
								currentUser.sellWine(new WineSell(wine, quantity, value));
							}

							outStream.writeObject(
									quantity + " unidades de vinho " + wineName + " posto à venda a " + value + ".");
							// registo no log
							List<byte[]> trans = new ArrayList<>();
							trans.add(log.getBytes());
							trans.add(signature);
							registerTransaction(trans);
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

							// verificao da assinatura

							s = Signature.getInstance("MD5withRSA");
							certFile = new File("server_side/usersCert/" + currentUser.getName() + ".cert");
							fis = new FileInputStream(certFile);
							cf = CertificateFactory.getInstance("X.509");
							cert = cf.generateCertificate(fis);
							s.initVerify(cert.getPublicKey());
							// verifica se a assinatura é valida

							// verifica se o vendedor existe
							if (userList.contains(new User(user))) {
								User seller = userList.get(userList.indexOf(new User(user)));
								WineSell sellWine = seller.getWine(wineName);
								outStream.writeObject(sellWine.getValue());
								byte[] sign = (byte[]) inStream.readObject();
								log = "buy," + wineName + "," + quantity + "," + sellWine.getValue() + ","
										+ currentUser.getName();
								s.update(log.getBytes());
								if (s.verify(sign)) {
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
												// registo no log
												List<byte[]> tran = new ArrayList<>();
												tran.add(log.getBytes());
												tran.add(sign);
												registerTransaction(tran);
												// backup
												lines = Files.readAllLines(fUser.toPath());
												for (Iterator<String> iterator = lines.iterator(); iterator
														.hasNext();) {
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
									outStream.writeObject("A assinatura nao e valida");
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

							decrypt();
							List<String> sc = Files.readAllLines(Paths.get("server_side/clientpass.txt"));
							encrypt();
							PublicKey key = null;
							for (String string : sc) {
								if (string.split(":")[0].equals(user)) {
									certFile = new File(string.split(":")[1] + ":" + string.split(":")[2]);
									fis = new FileInputStream(certFile);
									cf = CertificateFactory.getInstance("X.509");
									cert = cf.generateCertificate(fis);
									key = cert.getPublicKey();
									fis.close();
									break;
								}
							}

							User tempUser = userList.get(userList.indexOf(new User(user)));
							Cipher c = Cipher.getInstance("RSA");
							c.init(Cipher.ENCRYPT_MODE, key);
							byte[] enc = c.doFinal(message.getBytes());
							// to string para ser mais facil de escrever no backup
							String encoded = Arrays.toString(enc);
							tempUser.receiveMessage(currentUser.getName(), encoded);
							outStream.writeObject("Mensagem enviada");

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
						case "l":
						case "list":
							outStream.writeObject(getAllTransactions());
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

		private String getAllTransactions() throws ClassNotFoundException, IOException {
			File dir = new File("server_side/log");
			if (!dir.exists())
				return null;

			File[] files = dir.listFiles();

			if (files.length == 0)
				return null;

			StringBuilder sb = new StringBuilder();
			for (File file : files) {
				FileInputStream fis = new FileInputStream(file);
				ObjectInputStream oIn = new ObjectInputStream(fis);
				byte[] hash = (byte[]) oIn.readObject(); // hash
				long id = (long) oIn.readObject(); // id
				long nTrx = (long) oIn.readObject(); // nTrx
				List<List<byte[]>> transacoes = new ArrayList<>();
				for (int i = 0; i < nTrx; i++) {
					transacoes.add((List<byte[]>) oIn.readObject()); // all transactions
				}

				for (List<byte[]> it : transacoes) {
					String[] data = new String(it.get(0)).split(",");

					if (data[0].equals("sell")) {
						sb.append(data[0] + " : Foram postas " + data[2] + " unidades do vinho " + data[1]
								+ " custando cada um " + data[3] + " pelo utilizador " + data[4] + "\n");
					} else {
						sb.append(data[0] + " : Foram vendidas " + data[2] + " unidades do vinho " + data[1]
								+ " custando cada um " + data[3] + " e compradas pelo o utilizador " + data[4] + "\n");
					}
				}

			}
			return sb.toString();
		}

		private void registerTransaction(List<byte[]> transaction)
				throws IOException, NoSuchAlgorithmException, ClassNotFoundException, CertificateException,
				KeyStoreException, UnrecoverableKeyException, InvalidKeyException, SignatureException {

			File[] files = new File("server_side/log").listFiles();
			File currentFile = null;
			if (files == null) {
				currentFile = createBlock();
			} else {
				currentFile = files[files.length - 1];
			}

			// open reader
			FileInputStream fis = new FileInputStream(currentFile);
			ObjectInputStream oIn = new ObjectInputStream(fis);
			//
			// read block
			byte[] hash = (byte[]) oIn.readObject(); // hash
			long id = (long) oIn.readObject(); // id
			long nTrx = (long) oIn.readObject(); // nTrx

			List<List<byte[]>> transacoes = new ArrayList<>();
			for (int i = 0; i < nTrx; i++) {
				transacoes.add((List<byte[]>) oIn.readObject()); // all transactions
			}

			oIn.close();
			fis.close();
			// is block full ?
			if (nTrx == 5L) {
				// create new block
				createBlock();
				registerTransaction(transaction);
				return;
			}
			// open writter
			FileOutputStream fot = new FileOutputStream(currentFile);
			ObjectOutputStream oOut = new ObjectOutputStream(fot);
			//
			oOut.writeObject(hash);
			oOut.writeObject(id);
			oOut.writeObject(nTrx + 1);
			for (int i = 0; i < nTrx; i++) {
				oOut.writeObject(transacoes.get(i));
			}
			oOut.writeObject(transaction);
			if (nTrx == 4) {
				FileInputStream kfile = new FileInputStream(keyStore); // keystore
				KeyStore kstore = KeyStore.getInstance("JKS");
				kstore.load(kfile, passKeyStore.toCharArray());
				PrivateKey privateKey = (PrivateKey) kstore.getKey("server", passKeyStore.toCharArray());
				Signature s = Signature.getInstance("MD5withRSA");
				s.initSign(privateKey);
				s.update(hash);
				s.update((byte) id);
				s.update((byte) (nTrx + 1));
				for (int i = 0; i < nTrx; i++) {
					s.update(transacoes.get(i).get(0));
					s.update(transacoes.get(i).get(1));
				}
				s.update(transaction.get(0));
				s.update(transaction.get(1));
				oOut.writeObject(s.sign());
			}
			oOut.close();
			fot.close();
		}

		private File createBlock() throws IOException, NoSuchAlgorithmException {
			File dir = new File("server_side/log");
			if (!dir.exists())
				dir.mkdir();

			File[] files = dir.listFiles();
			int numFile = 0;
			if (files.length == 0) {
				numFile = 1;
			} else {
				String string = files[files.length - 1].getName();
				String num = string.substring(string.indexOf("_") + 1, string.indexOf("."));
				numFile = Integer.valueOf(num) + 1;
			}

			File file = new File("server_side/log/block_" + numFile + ".blk");
			file.createNewFile();

			FileOutputStream fis = new FileOutputStream(file);
			ObjectOutputStream oOut = new ObjectOutputStream(fis);

			String string = file.getName();
			int num = Integer.parseInt(string.substring(string.indexOf("_") + 1, string.indexOf(".")));
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			if (num == 1) {
				byte[] zeroHash = digest.digest("".getBytes(StandardCharsets.UTF_8));
				oOut.writeObject(zeroHash);
			} else {
				int i = num - 1;
				File previousBlock = new File(
						file.toPath().toString().replace(Integer.toString(num), Integer.toString(i)));
				FileInputStream is = new FileInputStream(previousBlock);

				// Create a DigestInputStream to read the file and update the digest
				DigestInputStream dis = new DigestInputStream(is, digest);

				while (dis.read() != -1) {
					// The digest is updated automatically as you read the file
				}
				dis.close();
				// Get the final hash (digest) value
				byte[] hash = digest.digest();
				oOut.writeObject(hash);
			}
			oOut.writeObject((long) num);
			oOut.writeObject(0L);
			fis.close();
			oOut.close();
			return file;
		}
	}
}