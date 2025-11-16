import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
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
import com.safenetinc.luna.provider.LunaProvider;

public class SignatureRSAAndMGF1Demo {

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
      byte[] result;

      // create the keys and make sure they are in the HSM.
      KeyPairGenerator kpg = null;
      kpg = KeyPairGenerator.getInstance("RSA", provider);
      kpg.initialize(2048);
      KeyPair kp = kpg.genKeyPair();

      BigInteger dataSHA1 = new BigInteger(
          "26af66361cddecb89d84bc3c74111f3756378a9d7237cd49495f8e9fc0d0f6707385aa1d4d91fd0056a86407cbe12aa274ba64e0325d28d5d822fa3e3b5487688d38964de64b3b0024cc4d4cf57a1b98329f5b0e6f4d63fdd0e89c7b1ee4334e19d0110d10940e82b0f761531a645be138ed56715090e88ad9b95c9d8e0962ec",
          16);
      BigInteger dataNoSHA = new BigInteger("1234567890123456789012345678901234567890123456789012345638cc", 16);// works
                                                                                                                // with
                                                                                                                // NONEwithX9_31RSA
      BigInteger data;
      data = dataSHA1;
      data = dataNoSHA;

      // sign
      Signature sig = null;
      sig = Signature.getInstance("SHA1withRSAandMGF1", provider);
      sig = Signature.getInstance("SHA224withRSAandMGF1", provider);
      sig = Signature.getInstance("SHA256withRSAandMGF1", provider);
      sig = Signature.getInstance("SHA384withRSAandMGF1", provider);
      sig = Signature.getInstance("SHA512withRSAandMGF1", provider);
      sig.initSign(kp.getPrivate());
      sig.update(data.toByteArray());
      result = sig.sign();

      // verify signature
      sig.initVerify(kp.getPublic());
      sig.update(data.toByteArray());
      boolean verifies = sig.verify(result);
      if (verifies == true) {
        System.out.println("Signature passed verification");
      } else {
        System.out.println("Signature failed verification");
      }

    } catch (InvalidKeyException e) {
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (SignatureException e) {
      e.printStackTrace();
    } catch (NoSuchProviderException e1) {
      e1.printStackTrace();
    }

  }

}
