
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import org.ietf.jgss.Oid;

public class teste {
    public static void main(String[] args)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException, UnrecoverableKeyException,
            InvalidKeyException, CertificateException, KeyStoreException, SignatureException {
        /*
         * FileOutputStream fot = new FileOutputStream("teste.blk");
         * ObjectOutputStream oOut = new ObjectOutputStream(fot);
         * oOut.writeObject("boas");
         * oOut.writeObject(2);
         * FileInputStream fis = new FileInputStream("teste.blk");
         * ObjectInputStream oIn = new ObjectInputStream(fis);
         * String string = (String) oIn.readObject();
         * string = "sdd";
         * oOut.writeObject(string);
         * int i = (int) oIn.readObject();
         * oOut.writeObject(i);
         * System.out.println(oIn.readObject());
         * System.out.println(oIn.readObject());
         */

        File file = registerTransaction("ola".getBytes());
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream oIn = new ObjectInputStream(fis);
        System.out.println();
        System.out.println(oIn.readObject());
        System.out.println(oIn.readObject());
        System.out.println(oIn.readObject());
        System.out.println();
    }

    private static File registerTransaction(byte[] transaction)
            throws IOException, NoSuchAlgorithmException, ClassNotFoundException, CertificateException,
            KeyStoreException, UnrecoverableKeyException, InvalidKeyException, SignatureException {
        File[] files = new File("server_side/log").listFiles();
        File currentFile = null;
        if (files == null) {
            System.out.println("entrei");
            currentFile = createBlock();
            System.out.println(currentFile.length());
        } else {
            currentFile = files[files.length - 1];
            System.out.println(currentFile.getPath());
        }

        // open reader an writter
        FileInputStream fis = new FileInputStream(currentFile);
        ObjectInputStream oIn = new ObjectInputStream(fis);

        System.out.println("contnuei");
        //
        // read block
        byte[] hash = (byte[]) oIn.readObject(); // hash
        System.out.println("contnuei");
        long id = (long) oIn.readObject(); // id
        System.out.println(id);
        long nTrx = (long) oIn.readObject(); // nTrx
        System.out.println(nTrx);
        FileOutputStream fot = new FileOutputStream(currentFile);
        ObjectOutputStream oOut = new ObjectOutputStream(fot);
        List<byte[]> transacoes = new ArrayList<>();
        for (int i = 0; i < nTrx; i++) {
            transacoes.add((byte[]) oIn.readObject()); // all transactions
        }

        oIn.close();
        fis.close();
        // is block full ?
        if (nTrx == 5L) {
            System.out.println("loop");
            createBlock();
            registerTransaction(transaction);
            oOut.close();
            return currentFile;
        }

        oOut.writeObject(hash);
        oOut.writeObject(id);
        oOut.writeObject(nTrx + 1);
        for (int i = 0; i < nTrx; i++) {
            oOut.writeObject(transacoes.get(i));
        }
        oOut.writeObject(transaction);
        if (nTrx == 4) {
            FileInputStream kfile = new FileInputStream("server_side/Key/ServerKey"); // keystore
            KeyStore kstore = KeyStore.getInstance("JKS");
            kstore.load(kfile, "123456".toCharArray());
            PrivateKey privateKey = (PrivateKey) kstore.getKey("server", "123456".toCharArray());
            Signature s = Signature.getInstance("MD5withRSA");
            s.initSign(privateKey);
            s.update(Files.readAllBytes(currentFile.toPath()));
            oOut.writeObject(s.sign());
        }
        oOut.close();
        fot.close();
        return currentFile;
    }

    private static File createBlock() throws IOException, NoSuchAlgorithmException {
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
