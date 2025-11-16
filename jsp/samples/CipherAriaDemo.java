import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import com.safenetinc.luna.provider.LunaProvider;

public class CipherAriaDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";
  public static final String provider = "LunaProvider";
  public static final String keystoreProvider = "Luna";
  public static SealedObject so = null;

  public static void main(String[] args) {

    KeyStore myStore = null;
    try {

      /* Note: could also use a keystore file, which contains the token label or slot no. to use. Load that via
       * "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save objects to the keystore via a
       * FileOutputStream. */

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

      KeyGenerator keyGen = null;
      Key key = null;
      keyGen = KeyGenerator.getInstance("ARIA", provider);
      key = keyGen.generateKey();

      byte[] blockSizeData = new byte[1024];
      // get some new random data each time just to mix things up a bit
      Random r = new Random();
      r.nextBytes(blockSizeData);
      byte[] nonblockSizeData = new byte[1021];
      r.nextBytes(nonblockSizeData);

      Cipher desCipher = null;
      byte[] iv = null;

      AlgorithmParameters lunaParams = null;
      desCipher = Cipher.getInstance("ARIA/ECB/NoPadding", provider);
      desCipher = Cipher.getInstance("ARIA/CBC/NoPadding", provider);
      desCipher = Cipher.getInstance("ARIA/CBC/PKCS5Padding", provider);
      desCipher = Cipher.getInstance("ARIA/CBC/ISO10126Padding", provider);
      desCipher = Cipher.getInstance("ARIA/CFB8/NoPadding", provider);
      desCipher = Cipher.getInstance("ARIA/CFB128/NoPadding", provider);
      desCipher = Cipher.getInstance("ARIA/OFB/NoPadding", provider);
      desCipher = Cipher.getInstance("ARIA/CTR/NoPadding", provider);
      iv = desCipher.getIV();
      if (iv != null) {
        // is AES ok for any secret key?
        lunaParams = AlgorithmParameters.getInstance("AES", provider);
        lunaParams.init(iv);
      }
      desCipher.init(Cipher.ENCRYPT_MODE, key, lunaParams);

      System.out.println("Encrypting PlainText");
      byte[] encryptedbytes = null;
      encryptedbytes = desCipher.doFinal(nonblockSizeData);

    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchPaddingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalBlockSizeException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (BadPaddingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidAlgorithmParameterException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

}
