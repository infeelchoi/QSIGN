/**
 * Copyright (c) 2018 SafeNet, All rights reserved.
 *
 * All rights reserved.  This file contains information that is
 * proprietary to SafeNet and may not be distributed
 * or copied without written consent from SafeNet.
 */

/**
 *
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.interfaces.ECKey;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.param.LunaECUtils;

/**
 * This example shows how two parties can use Elliptic Curve Diffie Hellman (ECDH) to agree upon a
 * secret key and using this key to encrypt/decrypt messages to keep communications private.
 */
public class KeyAgreementECDHMontgomeryDemo {

  // Configure these as required.
  private static final int slot = 3;
  private static final String passwd = "userpin";
  public static final String provider = "LunaProvider";
  public static final String keystoreProvider = "Luna";

  final private static String plaintext =
      "Elliptic curve Diffie Hellman (ECDH) is an anonymous key agreement protocol that allows two parties, each having an elliptic curve public/private key pair, to establish a shared secret over an insecure channel.";

  static public void main(String[] args) {

    KeyPair ecKeyPairA = null;
    KeyPair ecKeyPairB = null;
    SecretKey aesKeyA = null;
    SecretKey aesKeyB = null;

    // // Login to the HSM
    // HSM_Manager.hsmLogin();
    KeyStore myStore = null;
    try {

      /* Note: could also use a keystore file, which contains the token label or slot no. to use.
       * Load that via "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save
       * objects to the keystore via a FileOutputStream. */

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
      /**
       * Generate an EC Key Pair for both user A and user B. As LunaProvider is passed, these key
       * pairs are generated on the Luna HSM.
       */
      LunaSlotManager.getInstance().setPrivateKeysExtractable(true);
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("CURVE25519", provider);
      keyPairGenerator.initialize(new ECGenParameterSpec("curve25519"));
      ecKeyPairA = keyPairGenerator.generateKeyPair();
      ecKeyPairB = keyPairGenerator.generateKeyPair();

      ECKey ecKey = (ECKey) ecKeyPairA.getPublic();
      int curveLen = LunaECUtils.getFieldSize(ecKey.getParams());

    } catch (Exception e) {
      System.out.println("Exception during Key Pair Generation - " + e.getMessage());
      System.exit(1);
    }

    /**
     * Setting the secret keys extractable to true allows for the keys to be used in Software for
     * one. Later on the actual keys are displayed - this setting allows the extraction of the key
     * values. Try setting this value to false and note the difference in the output and what PKCS11
     * calls are made.
     */
    LunaSlotManager.getInstance().setSecretKeysExtractable(true);

    try {
      /**
       * Perform the key agreement. User A uses her private key the user B's public key. User B uses
       * her private key and user A's public key. Each party generates an AES key and the keys will
       * be identical. The key agreement is done using the LunaProvider so it is performed on the
       * HSM.
       *
       * For the purpose of this sample, just provide all FIPS mechs and use the last pair.
       */
      KeyAgreement keyAgreementA = null;
      KeyAgreement keyAgreementB = null;
      keyAgreementA = KeyAgreement.getInstance("ECDH", provider);
      keyAgreementB = KeyAgreement.getInstance("ECDH", provider);
      keyAgreementA = KeyAgreement.getInstance("EcDhWithNistKdf256", provider);
      keyAgreementB = KeyAgreement.getInstance("EcDhWithNistKdf256", provider);
      keyAgreementA = KeyAgreement.getInstance("1.3.133.16.840.63.0.2", provider);
      keyAgreementB = KeyAgreement.getInstance("1.3.133.16.840.63.0.2", provider);
      keyAgreementA = KeyAgreement.getInstance("1.3.132.1.11.0", provider);
      keyAgreementB = KeyAgreement.getInstance("1.3.132.1.11.0", provider);
      keyAgreementA = KeyAgreement.getInstance("1.3.132.1.11.1", provider);
      keyAgreementB = KeyAgreement.getInstance("1.3.132.1.11.1", provider);
      keyAgreementA = KeyAgreement.getInstance("1.3.132.1.11.2", provider);
      keyAgreementB = KeyAgreement.getInstance("1.3.132.1.11.2", provider);
      keyAgreementA = KeyAgreement.getInstance("1.3.132.1.11.3", provider);
      keyAgreementB = KeyAgreement.getInstance("1.3.132.1.11.3", provider);

      // The following don't work due to CKM_ECDH1_COFACTOR_DERIVE
      //      keyAgreementA = KeyAgreement.getInstance("ECDHC", provider);
      //      keyAgreementB = KeyAgreement.getInstance("ECDHC", provider);
      //      keyAgreementA = KeyAgreement.getInstance("1.3.133.16.840.63.0.3", provider);
      //      keyAgreementB = KeyAgreement.getInstance("1.3.133.16.840.63.0.3", provider);
      //      keyAgreementA = KeyAgreement.getInstance("1.3.132.1.14.0", provider);
      //      keyAgreementB = KeyAgreement.getInstance("1.3.132.1.14.0", provider);
      //      keyAgreementA = KeyAgreement.getInstance("1.3.132.1.14.1", provider);
      //      keyAgreementB = KeyAgreement.getInstance("1.3.132.1.14.1", provider);
      //      keyAgreementA = KeyAgreement.getInstance("1.3.132.1.14.2", provider);
      //      keyAgreementB = KeyAgreement.getInstance("1.3.132.1.14.2", provider);
      //      keyAgreementA = KeyAgreement.getInstance("1.3.132.1.14.3", provider);
      //      keyAgreementB = KeyAgreement.getInstance("1.3.132.1.14.3", provider);

      keyAgreementA.init(ecKeyPairA.getPrivate());
      keyAgreementB.init(ecKeyPairB.getPrivate());

      keyAgreementA.doPhase(ecKeyPairB.getPublic(), true);
      keyAgreementB.doPhase(ecKeyPairA.getPublic(), true);

      /**
       * Generate the aes secret keys. Some of the other possible key types include rsa, dsa, ec,
       * ecdsa, dh, des, des2, des3, rc2, rc4, rc5, etc.
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
     * Perform an encryption using User A's agreed upon secret key and decrypt using User B's agreed
     * upon secret key demonstrating the two users can communicate in a secret fashion.
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

    //    // Logout of the token
    //    HSM_Manager.hsmLogout();
  }
}
