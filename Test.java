import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class Test {
    public static void main(String[] args) throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
            IOException, UnrecoverableKeyException, InvalidKeyException, SignatureException {

        FileInputStream kfile = new FileInputStream("client_side/User5/User5Key"); // keystore
        KeyStore kstore = KeyStore.getInstance("JKS");
        kstore.load(kfile, "123456".toCharArray());
        String alias = "User5";
        PrivateKey privateKey = (PrivateKey) kstore.getKey(alias, "123456".toCharArray());
        Signature s = Signature.getInstance("MD5withRSA");
        s.initSign(privateKey);
        byte[] bytes = ByteBuffer.allocate(Long.BYTES).putLong(11111L).array();
        s.update(bytes);
        s.sign();

        FileInputStream k = new FileInputStream("truststore"); // keystore
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(k, "123456".toCharArray());
        Certificate certificate = ks.getCertificate(alias);
        Signature s1 = Signature.getInstance("MD5withRSA");
        s1.initVerify(certificate.getPublicKey());
        System.out.println(s1.verify(s.sign()));

        // o codigo funciona mas deve tar mal a comunicao
    }
}
