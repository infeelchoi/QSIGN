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
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.spec.IvParameterSpec;

import com.safenetinc.luna.provider.LunaProvider;

public class CipherAESDemo {

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
      keyGen = KeyGenerator.getInstance("AES", "LunaProvider");
      key = keyGen.generateKey();

      byte[] blockSizeData = new byte[1024];
      // get some new random data each time just to mix things up a bit
      Random r = new Random();
      r.nextBytes(blockSizeData);
      byte[] nonblockSizeData = new byte[1021];
      r.nextBytes(nonblockSizeData);

      Cipher encCipher = null;
      Cipher decCipher = null;
      byte[] iv = null;

      AlgorithmParameters lunaParams = null;
//      encCipher = Cipher.getInstance("AES/ECB/NoPadding", "LunaProvider");
//      encCipher = Cipher.getInstance("AES/CBC/NoPadding", "LunaProvider");
      encCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "LunaProvider");
//      encCipher = Cipher.getInstance("AES/CBC/ISO10126Padding", "LunaProvider");
//      encCipher = Cipher.getInstance("AES/CFB8/NoPadding", "LunaProvider");
//      encCipher = Cipher.getInstance("AES/CFB128/NoPadding", "LunaProvider");
//      encCipher = Cipher.getInstance("AES/OFB/NoPadding", "LunaProvider");
//      encCipher = Cipher.getInstance("AES/CTR/NoPadding", "LunaProvider");

//      decCipher = Cipher.getInstance("AES/ECB/NoPadding", "LunaProvider");
//      needParms = false;
//      decCipher = Cipher.getInstance("AES/CBC/NoPadding", "LunaProvider");
      decCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "LunaProvider");
//      decCipher = Cipher.getInstance("AES/CBC/ISO10126Padding", "LunaProvider");
//      decCipher = Cipher.getInstance("AES/CFB8/NoPadding", "LunaProvider");
//      decCipher = Cipher.getInstance("AES/CFB128/NoPadding", "LunaProvider");
//      decCipher = Cipher.getInstance("AES/OFB/NoPadding", "LunaProvider");
//      decCipher = Cipher.getInstance("AES/CTR/NoPadding", "LunaProvider");

      iv = encCipher.getIV();
      if (iv == null) {
        // is AES ok for any secret key?
        lunaParams = AlgorithmParameters.getInstance("AES", provider);
        IvParameterSpec IV16 = new IvParameterSpec(new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
            0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x10, 0x11 });
        lunaParams.init(IV16);
      }
      AlgorithmParameters dummyParams = null;
//      encCipher.init(Cipher.ENCRYPT_MODE, key, dummyParams);
      encCipher.init(Cipher.ENCRYPT_MODE, key, lunaParams);
      IvParameterSpec ivps = new IvParameterSpec(encCipher.getIV());
//      decCipher.init(Cipher.DECRYPT_MODE, key, ivps);
      decCipher.init(Cipher.DECRYPT_MODE, key, lunaParams);

      System.out.println("Encrypting PlainText");
      byte[] encryptedbytes = null;
      encryptedbytes = encCipher.doFinal(blockSizeData);

      System.out.println("Decrypting to PlainText");
      byte[] decryptedbytes = null;
      decryptedbytes = decCipher.doFinal(encryptedbytes);

      Arrays.equals(decryptedbytes, encryptedbytes);

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
    } catch (InvalidAlgorithmParameterException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidParameterSpecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

}
