import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KEM;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.safenetinc.luna.LunaUtils;
import com.safenetinc.luna.X509.AsnSubjectPublicKeyInfo;
import com.safenetinc.luna.provider.LunaProvider;

public class LunaMLKEMDemo {

    // Configure these as required.
    private static final int slot = 0;
    private static final String passwd = "userpin1";
    public static final String provider = "LunaProvider";
    public static final String keystoreProvider = "Luna";
    public static SealedObject so = null;

    public static void main(String[] args) {

        KeyStore myStore = null;
        try {

            /*
             * Note: could also use a keystore file, which contains the token label or slot
             * no. to use. Load that via
             * "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save
             * objects to the keystore via a
             * FileOutputStream.
             */

            Security.addProvider(new LunaProvider());
            ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot).getBytes());
            myStore = KeyStore.getInstance(keystoreProvider);
            myStore.load(is1, passwd.toCharArray());
        } catch (KeyStoreException kse) {
            System.out.println("Unable to create keystore object");
            System.exit(-1);
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("Unexpected NoSuchAlgorithmException while loading keystore");
            System.exit(-1);
        } catch (CertificateException e) {
            System.out.println("Unexpected CertificateException while loading keystore");
            System.exit(-1);
        } catch (IOException e) {
            // this should never happen
            System.out.println("Unexpected IOException while loading keystore.");
            System.exit(-1);
        }

        try {

            KeyPairGenerator kpg = null;
            KeyPair kp = null;
            kpg = KeyPairGenerator.getInstance("ML-KEM", "LunaProvider");
            kp = kpg.generateKeyPair();
            System.out.println("MLKEM key pair generated");
            System.out.println("[Public key: " + kp.getPublic().toString() + "]");
            System.out.println("[Private key: " + kp.getPrivate().toString() + "]");
            byte[] pubKeyEncoded = kp.getPublic().getEncoded();

            System.out.println("Public Key encoded:");
            System.out.println(LunaUtils.getHexString(pubKeyEncoded, false));

            KeyFactory kf = KeyFactory.getInstance("ML-KEM", "LunaProvider");
            X509EncodedKeySpec spec = new X509EncodedKeySpec(pubKeyEncoded);
            PublicKey injectedPublicKey = kf.generatePublic(spec);
            System.out.println("[Injected Public key: " + injectedPublicKey.toString() + "]");

            KEM kem = null;
            KEM.Encapsulator encap = null;
            KEM.Decapsulator decap = null;

            kem = KEM.getInstance("ML-KEM", provider);

            // ENCAP
            encap = kem.newEncapsulator(kp.getPublic());
            System.out.println("Encapsulator Provider: " + encap.providerName());
            KEM.Encapsulated pqCiphertext = encap.encapsulate(0, 32, "AES");
            SecretKey key1 = pqCiphertext.key();
            System.out.println("Encapsulated Key: " + key1.toString());

            // ENCRYPT
            Cipher cipher = null;
            byte[] iv = null;
            String sampleStr = "My test data for encryption";
            byte[] sampleData = sampleStr.getBytes(Charset.defaultCharset());
            // get some new random data each time just to mix things up a bit
            AlgorithmParameters lunaParams = null;
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "LunaProvider");
            iv = cipher.getIV();
            if (iv == null) {
                // is AES ok for any secret key?
                lunaParams = AlgorithmParameters.getInstance("AES", provider);
                IvParameterSpec IV16 = new IvParameterSpec(
                        new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                                0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x10, 0x11 });
                lunaParams.init(IV16);
            }
            cipher.init(Cipher.ENCRYPT_MODE, key1, lunaParams);
            IvParameterSpec ivps = new IvParameterSpec(cipher.getIV());
            System.out.println("Encrypting PlainText");
            byte[] encryptedbytes = null;
            encryptedbytes = cipher.doFinal(sampleData);

            // DECAP
            decap = kem.newDecapsulator(kp.getPrivate());
            SecretKey key2 = decap.decapsulate(pqCiphertext.encapsulation(), 0, 32, "AES");
            System.out.println("Decapsulated Key: " + key2.toString());

            // DECRYPT
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "LunaProvider");
            cipher.init(Cipher.DECRYPT_MODE, key2, ivps);
            System.out.println("Decrypting to PlainText");
            byte[] decryptedbytes = null;
            decryptedbytes = cipher.doFinal(encryptedbytes);
            String decryptedStr = new String(decryptedbytes, Charset.defaultCharset());

            assert decryptedStr.equals(sampleStr) : "Data mismatch";

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
