import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.Security;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

import com.safenetinc.luna.LunaUtils;
import com.safenetinc.luna.provider.LunaProvider;

/**
 * This sample demonstrates generating an RSA-PSS Signature where no hashing is done by the provider but
 * the Signature object accepts raw hash bytes and the provider sends the raw hash bytes to the Luna HSM.
 */

public class SignatureRSAPSSDemo {

  // Configure these as required.
  private static final int slot = 2;
  private static final String passwd = "userpin1";
  public static final String provider = "LunaProvider";
  public static final String keystoreProvider = "Luna";


  public static void main(String[] args) throws Exception {

    Security.addProvider(new LunaProvider());
    ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot).getBytes());
    KeyStore myStore = KeyStore.getInstance(keystoreProvider);
    myStore.load(is1, passwd.toCharArray());

    System.out.println("Generating RSA Keypair");
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "LunaProvider");
    keyGen.initialize(2048);
    KeyPair keyPair = keyGen.generateKeyPair();

    MessageDigest messageDigest = MessageDigest.getInstance("SHA256", new LunaProvider() );
    String message = "RSA PSS Signature Message!";
    System.out.println("Message: " + message);

    //use hash with NONEwithRSAPSS
    byte[] hash = messageDigest.digest(message.getBytes());
    System.out.println("Hash of the message: " + LunaUtils.getHexString(hash, false));

    Signature sigSign = Signature.getInstance("RSASSA-PSS", "LunaProvider");
//    Signature sigSign = Signature.getInstance("NONEwithRSAPSS", "LunaProvider");
//    sigSign = Signature.getInstance("SHA3-256withRSAandMGF1", "LunaProvider");

    //Construct the PSSParameterSpec and assign to the Signature object
    PSSParameterSpec spec = new PSSParameterSpec("SHA256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1);
    spec = new PSSParameterSpec("SHA512", "MGF1", MGF1ParameterSpec.SHA512, 32, 1);
//  spec = new PSSParameterSpec("SHA3-256", "MGF1", MGF1ParameterSpec.SHA3_256, 32, 1);
    sigSign.setParameter(spec);

    sigSign.initSign(keyPair.getPrivate());
//    sigSign.update(hash);
    sigSign.update(message.getBytes());
    byte[] signature = sigSign.sign();

    System.out.println("RSA PSS Signature: " + LunaUtils.getHexString(signature, false));

    Signature sigVerify = Signature.getInstance("RSASSA-PSS", "LunaProvider");
//    Signature sigVerify = Signature.getInstance("NONEwithRSAPSS", "LunaProvider");
//    sigVerify = Signature.getInstance("SHA3-256withRSAandMGF1", "LunaProvider");
    sigVerify.setParameter(spec);
    sigVerify.initVerify(keyPair.getPublic());
//    sigVerify.update(hash);
    sigVerify.update(message.getBytes());

    if (sigVerify.verify(signature)) {
      System.out.println("The signature was verified successfully!");
    } else {
      System.out.println("The signature was invalid.");
    }
  }
}
