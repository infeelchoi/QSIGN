
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import com.safenetinc.luna.LunaUtils;
import com.safenetinc.luna.provider.LunaProvider;

public class SignatureCUDemo {

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
      ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot + "\nusertype:CKU_CRYPTO_USER").getBytes());
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

      // use keytool to create token-based RSA keys in the HSM.
      // e.g.
      // keytool -genkey -v -storetype luna -keystore lunassl.ks -keypass userpin -storepass userpin -alias cu_testkey
      // -dname "cn=www.gem.com, ou=jsp, o=gemalto, c=CA" -keyalg RSA -validity 360 -keysize 2048 -ext san=ip:127.0.0.1
      PrivateKey privk = (PrivateKey)myStore.getKey("cu_testkey", null);
      PublicKey pubk = myStore.getCertificate("cu_testkey").getPublicKey();

      // sign
      Signature sig = Signature.getInstance("RSA", provider);
      sig.initSign(privk);
      sig.update(msgdata);
      byte[] signature = sig.sign();

      // verify signature
      sig.initVerify(pubk);
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
    } catch (UnrecoverableKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (KeyStoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
