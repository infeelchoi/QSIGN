import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.param.LunaDHKeyAgreementParameterSpec;

/**
 * This example shows how two parties can use Diffie Hellman (DH) to agree upon a secret key and using this key to
 * encrypt/decrypt messages to keep communications private.
 */
public class KeyAgreementDHDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";

  // prime number taken from https://www.ietf.org/rfc/rfc3526.txt from group with assigned id 14
  final static private String p14 = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
      + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
      + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
      + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
      + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
      + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" + "15728E5A8AACAA68FFFFFFFFFFFFFFFF";

  // generator taken from https://www.ietf.org/rfc/rfc3526.txt from group with assigned id 14
  final private static String g14 = "2";

  final private static String plaintext = "Diffie Hellman key exchange (DH) is a specific method of securely exchanging cryptographic keys over a public channel and was one of the first public-key protocols as originally conceptualized by Ralph Merkle.";

  static public void main(String[] args) {

    KeyPair dhKeyPairA = null;
    KeyPair dhKeyPairB = null;
    SecretKey aesKeyA = null;
    SecretKey aesKeyB = null;

    // Login to the HSM
    // HSM_Manager.hsmLogin();
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

    System.out.println("DH Param p: " + p14);
    System.out.println("DH Param g: " + g14);

    LunaSlotManager.getInstance().setPrivateKeysExtractable(true);

    // Create a DHParameterSpec that contains the prime number modulus as well as the base (generator).
    BigInteger p = new BigInteger(p14, 16);
    BigInteger g = new BigInteger(g14, 16);
    DHParameterSpec parameterSpec = new DHParameterSpec(p, g);

    try {
      /**
       * Generate a Diffie Hellman Key Pair for both user A and user B. As LunaProvider is passed, these key pairs are
       * generated on the Luna HSM.
       */
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH", "LunaProvider");
      keyPairGenerator.initialize(parameterSpec);
      dhKeyPairA = keyPairGenerator.generateKeyPair();
      dhKeyPairB = keyPairGenerator.generateKeyPair();
    } catch (Exception e) {
      System.out.println("Exception during Key Pair Generation - " + e.getMessage());
      System.exit(1);
    }

    /**
     * Setting the secret keys extractable to true allows for the keys to be used in Software for one. Later on the
     * actual keys are displayed - this setting allows the extraction of the key values. Try setting this value to false
     * and note the difference in the output and what PKCS11 calls are made.
     */
    LunaSlotManager.getInstance().setSecretKeysExtractable(true);

    try {
      /**
       * Perform the key agreement. User A uses her private key the user B's public key. User B uses her private key and
       * user A's public key. Each party generates an AES key and the keys will be identical. The key agreement is done
       * using the LunaProvider so it is performed on the HSM.
       */
      KeyAgreement keyAgreementA = KeyAgreement.getInstance("DH", "LunaProvider");
      KeyAgreement keyAgreementB = KeyAgreement.getInstance("DH", "LunaProvider");

      LunaDHKeyAgreementParameterSpec aParmSpec = null;
      aParmSpec = new LunaDHKeyAgreementParameterSpec(256);
      keyAgreementA.init(dhKeyPairA.getPrivate(), aParmSpec);
      keyAgreementB.init(dhKeyPairB.getPrivate(), aParmSpec);

      keyAgreementA.doPhase(dhKeyPairB.getPublic(), true);
      keyAgreementB.doPhase(dhKeyPairA.getPublic(), true);

      /**
       * Generate the aes secret keys. Some of the other possible key types include rsa, dsa, ec, ecdsa, dh, des, des2,
       * des3, rc2, rc4, rc5, etc.
       */
      aesKeyA = keyAgreementA.generateSecret("aes");
      aesKeyB = keyAgreementB.generateSecret("aes");
    } catch (Exception e) {
      System.out.println("Exception during Key Agreement - " + e.getMessage());
      System.exit(1);
    }

    System.out.println("AES Key A: " + MiscUtils.bytesToHex(aesKeyA.getEncoded()));
    System.out.println("AES Key B: " + MiscUtils.bytesToHex(aesKeyB.getEncoded()));

    System.out.println("plaintext: " + plaintext);

    byte[] iv = null;
    byte[] encrypted = null;

    /**
     * Perform an encryption using User A's agreed upon secret key and decrypt using User B's agreed upon secret key
     * demonstrating the two users can communicate in a secret fashion.
     */

    try {
      Cipher cipherA = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipherA.init(Cipher.ENCRYPT_MODE, aesKeyA);
      iv = cipherA.getIV();
      encrypted = cipherA.doFinal(plaintext.getBytes());
    } catch (Exception e) {
      System.out.println("Exception during encryption - " + e.getMessage());
      System.exit(1);
    }

    System.out.println("encrypted: " + MiscUtils.bytesToHex(encrypted));

    try {
      Cipher cipherB = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipherB.init(Cipher.DECRYPT_MODE, aesKeyB, new IvParameterSpec(iv));
      byte[] decrypted = cipherB.doFinal(encrypted);
      System.out.println("decrypted: " + new String(decrypted));
    } catch (Exception e) {
      System.out.println("Exception during encryption - " + e.getMessage());
      System.exit(1);
    }

    // Logout of the token
    // HSM_Manager.hsmLogout();
  }
}
