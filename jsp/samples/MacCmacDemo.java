import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.provider.LunaProvider;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;

/**
 * An example of performing CKM_AES_CMAC and CKM_DES3_CMAC Cipher Based Message Authentication Codes.
 */

public class MacCmacDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";
  public static final String provider = "LunaProvider";
  public static final String keystoreProvider = "Luna";

  private static SecretKey keyAes = null;
  private static SecretKey keyDes3 = null;

  public static String toHex(byte[] digest) {
    if (digest == null)
      return new String("(null)");
    String digits = "0123456789abcdef";
    StringBuilder sb = new StringBuilder(digest.length * 2);
    for (byte b : digest) {
      int bi = b & 0xff;
      sb.append(digits.charAt(bi >> 4));
      sb.append(digits.charAt(bi & 0xf));
    }
    return sb.toString();
  }

  public static byte[] doDigest(String algorithm, byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance(algorithm, provider);
      md.update(data);
      byte[] out = md.digest();
      System.out.println("MessageDigest." + algorithm + " = " + toHex(out));
      return out;
    } catch (Exception e) {
      System.out.println("MessageDigest." + algorithm + ": " + e.getMessage());
      System.exit(1);
    }
    return null;
  }

  public static void generateKeys() {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", provider);
      keyGenerator.init(256);
      keyAes = keyGenerator.generateKey();

      KeyGenerator keyGenerator2 = KeyGenerator.getInstance("DES3", provider);
      keyDes3 = keyGenerator2.generateKey();
    } catch (NoSuchAlgorithmException nsae) {
      nsae.printStackTrace();
      System.exit(1);
    } catch (NoSuchProviderException nspe) {
      nspe.printStackTrace();
      System.exit(1);
    }
  }

  public static void doCmac(String algorithm, SecretKey key, byte[] message) {
    try {
      Mac mac = Mac.getInstance(algorithm, provider);
      mac.init(key);
      mac.update(message);
      byte[] out = mac.doFinal();
      System.out.println("Mac." + algorithm + " = " + toHex(out));
      System.out.println("Mac." + algorithm + ".length = " + out.length);
    } catch (Exception e) {
      System.out.println("Mac." + algorithm + ": " + e.getMessage());
      System.exit(1);
    }
  }

  public static void main(String[] args) {

    System.out.println("provider = " + provider);

    KeyStore myKeyStore = null;
    try {

      /* Note: could also use a keystore file, which contains the token label or slot no. to use. Load that via
       * "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save objects to the keystore via a
       * FileOutputStream. */

      Security.addProvider(new LunaProvider());
      ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot).getBytes());
      myKeyStore = KeyStore.getInstance(keystoreProvider);
      myKeyStore.load(is1, passwd.toCharArray());
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

    byte[] digest = doDigest("SHA256", "Message to authenticate!".getBytes());

    generateKeys();

    doCmac("CmacAES", keyAes, digest);
    doCmac("CmacDES3", keyDes3, digest);
    doCmac("aescmac", keyAes, digest);
    doCmac("des3cmac", keyDes3, digest);
  }

}
