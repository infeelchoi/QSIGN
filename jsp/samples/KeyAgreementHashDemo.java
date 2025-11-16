
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.provider.LunaProvider;

public class KeyAgreementHashDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";
  public static final String provider = "LunaProvider";
  public static final String keystoreProvider = "Luna";

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
      LunaSlotManager.getInstance().setSecretKeysExtractable(true);
      LunaSlotManager.getInstance().setSecretKeysDerivable( true );

      // create a secret key
      KeyGenerator kg = KeyGenerator.getInstance("AES", provider);
      SecretKey secretKey = kg.generateKey();

      KeyAgreement ka1 = KeyAgreement.getInstance("CKKeyDerivationSHA1", provider);
      KeyAgreement ka2 = KeyAgreement.getInstance("CKKeyDerivationSHA1", provider);
      ka1 = KeyAgreement.getInstance("CKKeyDerivationSM3", provider);
      ka2 = KeyAgreement.getInstance("CKKeyDerivationSM3", provider);

      ka1.init(secretKey);
      ka1.doPhase(null, true);
      byte[] secret1 = ka1.generateSecret();
//      SecretKey secret1 = ka1.generateSecret("AES");

      ka2.init(secretKey);
      ka2.doPhase(null, true);
      byte[] secret2 = ka2.generateSecret();
//      SecretKey secret2 = ka2.generateSecret("AES");

//      assertThat(secret1, equalTo(secret2));

      System.out.println("secret1: " + Arrays.toString(secret1));
      System.out.println("secret2: " + Arrays.toString(secret2));

      if (Arrays.equals(secret1, secret2)) {
        System.out.println("Key agreement derivation of symmetric key worked.");
      } else {
        System.out.println("Key agreement derivation of symmetric key failed.");
      }

    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
