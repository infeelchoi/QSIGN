import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import com.safenetinc.luna.LunaUtils;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.param.LunaGcmParameterSpec;

public class CipherAESGCMDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";

  private static String plaintext = "Galois/Counter Mode (GCM) is a mode of operation for symmetric key cryptographic block ciphers that has been widely adopted because of its efficiency and performance.";

  public static void main(String[] args) throws Exception {

    KeyStore myStore = null;
    try {

      /* Note: could also use a keystore file, which contains the token label or slot no. to use. Load that via
       * "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save objects to the keystore via a
       * FileOutputStream. */

      Security.addProvider(new LunaProvider());
      ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot).getBytes());
      myStore = KeyStore.getInstance("Luna");
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

    // generate an AES key
    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "LunaProvider");
    keyGenerator.init(256);
    SecretKey key = keyGenerator.generateKey();

    System.out.println("Plaintext: " + plaintext);

    String aad = "AAD4";
    /**
     * Create a GCMParameterSpec spec with zero length byte array for the IV. This way the firmware will generate the IV
     * as part of the encryption operation. In FIPS mode, the firmware requires the IV not to specified but in non-FIPS
     * mode the IV can be specified with the user specified byte array.
     */
    GCMParameterSpec encryptSpec = new GCMParameterSpec(128, new byte[0]);
//        LunaGcmParameterSpec encryptSpec = new LunaGcmParameterSpec(new byte[0],aad.getBytes(),128);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "LunaProvider");
    cipher.init(Cipher.ENCRYPT_MODE, key, encryptSpec);
    /**
     * Specify the AAD (Additional Authenticated Data). For performance reasons, ensure that the AAD is a multiple of
     * four bytes. The HSM accelerator chip is only used if the AAD is a multiple of four bytes, otherwise the HSM CPU
     * has to perform the operation which is significantly slower.
     */
    cipher.updateAAD(aad.getBytes());
    byte[] encrypted = cipher.doFinal(plaintext.getBytes());

    System.out.println("Encrypted: " + LunaUtils.getHexString(encrypted, false));

    byte[] iv = cipher.getIV();
    System.out.println("Generated IV: " + LunaUtils.getHexString(iv, false));

    GCMParameterSpec decryptSpec = new GCMParameterSpec(128, iv);
//        LunaGcmParameterSpec decryptSpec = new LunaGcmParameterSpec(iv,aad.getBytes(),128);

    cipher.init(Cipher.DECRYPT_MODE, key, decryptSpec);
    // Specify the AAD (Additional Authenticated Data)
    cipher.updateAAD(aad.getBytes());
    byte[] decrypted = cipher.doFinal(encrypted);

    System.out.println("Decrypted: " + new String(decrypted));

  }

}
