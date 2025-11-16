
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;

import com.safenetinc.luna.LunaUtils;
import com.safenetinc.luna.provider.LunaProvider;

public class SignatureLCODemo {

  // Configure these as required.
  private static final int slot = 5;
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
      MiscProviderList.listProviders();

      ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot + "\nusertype:CKU_LIMITED_CRYPTO_OFFICER").getBytes());
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
      String message = "020000000000000001FFFFFFFFFFFFFFFE123456789ABCDEF000B3DA2000000100000300000003030003000300";
      byte[] msgdata = LunaUtils.hexStringToByteArray(message);
      byte[] result;

      // create the keys and make sure they are in the HSM.
      KeyPairGenerator kpg = null;
      kpg = KeyPairGenerator.getInstance("RSA", provider);
      kpg.initialize(2048);
      KeyPair kp = kpg.genKeyPair();

      // sign
      Signature sig = Signature.getInstance("RSA", provider);
      sig.initSign(kp.getPrivate());
      sig.update(msgdata);
      byte[] signature = sig.sign();

      // verify signature
      sig.initVerify(kp.getPublic());
      sig.update(msgdata);
      boolean verifies = sig.verify(signature);
      if (verifies == true) {
        System.out.println("Signature passed verification");
      } else {
        System.out.println("Signature failed verification");
      }

    } catch (InvalidKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SignatureException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
