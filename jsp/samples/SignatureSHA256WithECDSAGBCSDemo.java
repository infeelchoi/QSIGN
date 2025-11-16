import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;

import com.safenetinc.luna.LunaAPI;
import com.safenetinc.luna.LunaUtils;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.key.LunaKey;

public class SignatureSHA256WithECDSAGBCSDemo {

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

      // create the keys and make sure they are in the HSM.
      KeyPairGenerator kpg = null;
      kpg = KeyPairGenerator.getInstance("ECDSA", provider);
      // should really use this key gen mech for GBCS
      kpg = KeyPairGenerator.getInstance("ECwithExtraRandomBits", provider);
      ECGenParameterSpec ecSpec = new ECGenParameterSpec("c2pnb304w1");
      ecSpec = new ECGenParameterSpec("secp521r1");
      ecSpec = new ECGenParameterSpec("prime256v1");
      kpg.initialize(ecSpec);
      KeyPair kp = kpg.genKeyPair();

      String privateKey = "3041020100301306072A8648CE3D020106082A8648CE3D030107042730250201010420FC9BB773E6C8350ADB4051AC913CA470CF422D8A53DE8C881DBFFEB40BA47051";
      // Inject the given key into the HSM.
      PrivateKey privKey = LunaKey.InjectPrivateKey(LunaUtils.hexStringToByteArray(privateKey), LunaAPI.CKK_ECDSA);

      String message = "020000000000000001FFFFFFFFFFFFFFFE123456789ABCDEF000B3DA2000000100000300000003030003000300";
      byte[] msgdata = LunaUtils.hexStringToByteArray(message);
      byte[] result;

      // Sign the data
      Signature sig = null;
      sig = Signature.getInstance("ECDSA", "LunaProvider");
      sig = Signature.getInstance("SHA256withECDSAGBCS", "LunaProvider");
//      sig.initSign(privKey);
      sig.initSign(kp.getPrivate());
      sig.update(msgdata);
      result = sig.sign();

      // Verify the signature
      System.out.println("Verifying signature");
      sig.initVerify(kp.getPublic());
      sig.update(msgdata);
      boolean verifies = sig.verify(result);
      if (verifies == true) {
        System.out.println("Signature passed verification");
      } else {
        System.out.println("Signature failed verification");
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
    } catch (SignatureException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidAlgorithmParameterException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
