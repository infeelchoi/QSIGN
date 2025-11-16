import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

import com.safenetinc.luna.provider.LunaCertificateX509;
import com.safenetinc.luna.provider.LunaProvider;

public class SignatureDSADemo {

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

    KeyPairGenerator keyGen = null;
    KeyPair keyPair = null;
    try {
      System.out.println("Generating DSA Keypair");
      keyGen = KeyPairGenerator.getInstance("DSA", "LunaProvider");

      // Create a DSA parameter spec with the correct sizes for the key length under test.
      java.security.AlgorithmParameterGenerator dsaGen = java.security.AlgorithmParameterGenerator.getInstance("DSA",
          provider);
      dsaGen.init(2048);
      java.security.AlgorithmParameters dsaParam = dsaGen.generateParameters();
      java.security.spec.DSAParameterSpec spec = dsaParam.getParameterSpec(java.security.spec.DSAParameterSpec.class);

      keyGen.initialize(spec);
      keyPair = keyGen.generateKeyPair();
    } catch (Exception e) {
      System.out.println("Exception during Key Generation - " + e.getMessage());
      System.exit(1);
    }

    // generate a self-signed DSA certificate.
    Date notBefore = new Date();
    Date notAfter = new Date(notBefore.getTime() + 1000000000);
    BigInteger serialNum = new BigInteger("123456");
    LunaCertificateX509 cert = null;
    try {
      cert = LunaCertificateX509.SelfSign(keyPair, "CN=ECDSA Sample Cert", serialNum, notBefore, notAfter);
    } catch (InvalidKeyException ike) {
      System.out.println("Unexpected InvalidKeyException while generating cert.");
      System.exit(-1);
    } catch (CertificateEncodingException cee) {
      System.out.println("Unexpected CertificateEncodingException while generating cert.");
      System.exit(-1);
    }

    byte[] bytes = "Some Text to Sign as an Example".getBytes();
    System.out.println("PlainText = " + com.safenetinc.luna.LunaUtils.getHexString(bytes, true));

    Signature ecdsaSig = null;
    byte[] signatureBytes = null;
    try {
      // Create a Signature Object and sign the encrypted text
      /* Sign/Verify operations like Encrypt/Decrypt operations can be performed in either singlepart or multipart
       * steps. Single part Signing and Verify examples are given in this code. Multipart signatures use the
       * Signature.update() method to load all the bytes and then invoke the Signature.sign() method to get the result.
       * For more information please see the class documentation for the java.security.Signature class with respect to
       * the version of the JDK you are using. */
      System.out.println("Signing encrypted text");
      ecdsaSig = Signature.getInstance("DSA", provider);
      ecdsaSig.initSign(keyPair.getPrivate());
      ecdsaSig.update(bytes);
      signatureBytes = ecdsaSig.sign();
    } catch (Exception e) {
      System.out.println("Exception during Signing - " + e.getMessage());
      System.exit(1);
    }

    try {
      // Verify the signature
      System.out.println("Verifying signature");
      ecdsaSig.initVerify(cert);
      ecdsaSig.update(bytes);
      boolean verifies = ecdsaSig.verify(signatureBytes);
      if (verifies == true) {
        System.out.println("Signature passed verification");
      } else {
        System.out.println("Signature failed verification");
      }
    } catch (Exception e) {
      System.out.println("Exception during Verification - " + e.getMessage());
      System.exit(1);
    }

  }
}
