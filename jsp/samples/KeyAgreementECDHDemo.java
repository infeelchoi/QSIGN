/*******************************************************************************
 *
 * Copyright (c) 2018-20 SafeNet, All rights reserved.
 *
 * All rights reserved.  This file contains information that is
 * proprietary to SafeNet and may not be distributed
 * or copied without written consent from SafeNet.
 *******************************************************************************/
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
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.provider.LunaProvider;

/**
 * This example shows how two parties can use Elliptic Curve Diffie Hellman (ECDH) to agree upon a secret key and using
 * this key to encrypt/decrypt messages to keep communications private.
 */
public class KeyAgreementECDHDemo {

  // Configure these as required.
  private static final int slot = 3;
  private static final String passwd = "userpin";
  public static final String provider = "LunaProvider";
  public static final String keystoreProvider = "Luna";

  final private static String plaintext = "Elliptic curve Diffie Hellman (ECDH) is an anonymous key agreement protocol that allows two parties, each having an elliptic curve public/private key pair, to establish a shared secret over an insecure channel.";

  final private static String p = "FFFFFFFF 00000001 00000000 00000000 00000000 FFFFFFFF FFFFFFFF FFFFFFFF".replace(" ",
      "");
  final private static String a = "FFFFFFFF 00000001 00000000 00000000 00000000 FFFFFFFF FFFFFFFF FFFFFFFC".replace(" ",
      "");
  final private static String b = "5AC635D8 AA3A93E7 B3EBBD55 769886BC 651D06B0 CC53B0F6 3BCE3C3E 27D2604B".replace(" ",
      "");
  final private static String x = "6B17D1F2 E12C4247 F8BCE6E5 63A440F2 77037D81 2DEB33A0 F4A13945 D898C296".replace(" ",
      "");
  final private static String y = "4FE342E2 FE1A7F9B 8EE7EB4A 7C0F9E16 2BCE3357 6B315ECE CBB64068 37BF51F5".replace(" ",
      "");
  final private static String n = "FFFFFFFF 00000000 FFFFFFFF FFFFFFFF BCE6FAAD A7179E84 F3B9CAC2 FC632551".replace(" ",
      "");
  final private static int h = 1;

  static public void main(String[] args) {

    KeyPair ecKeyPairA = null;
    KeyPair ecKeyPairB = null;
    SecretKey aesKeyA = null;
    SecretKey aesKeyB = null;

    // // Login to the HSM
    // HSM_Manager.hsmLogin();
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
      /**
       * Generate an EC Key Pair for both user A and user B. As LunaProvider is passed, these key pairs are generated on
       * the Luna HSM.
       */
      LunaSlotManager.getInstance().setPrivateKeysExtractable(true);
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", provider);
      keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
      ecKeyPairA = keyPairGenerator.generateKeyPair();

      EllipticCurve ellipticCurve = new EllipticCurve(new ECFieldFp(new BigInteger(p, 16)), new BigInteger(a, 16),
          new BigInteger(b, 16));
      ECPoint g = new ECPoint(new BigInteger(x, 16), new BigInteger(y, 16));
      ECParameterSpec spec = new ECParameterSpec(ellipticCurve, g, new BigInteger(n, 16), h);

      KeyPairGenerator keyPairGenerator2 = KeyPairGenerator.getInstance("EC", provider);
      keyPairGenerator2.initialize(spec);
      ecKeyPairB = keyPairGenerator2.generateKeyPair();

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
       *
       * For the purpose of this sample, just provide all FIPS mechs and use the last pair.
       */
      KeyAgreement keyAgreementA = KeyAgreement.getInstance("ECDH", provider);
      KeyAgreement keyAgreementB = KeyAgreement.getInstance("ECDH", provider);
//      keyAgreementA = KeyAgreement.getInstance("ECDHC", provider);
//      keyAgreementB = KeyAgreement.getInstance("ECDHC", provider);
//      keyAgreementA = KeyAgreement.getInstance("1.3.133.16.840.63.0.2", provider);
//      keyAgreementB = KeyAgreement.getInstance("1.3.133.16.840.63.0.2", provider);
//      keyAgreementA = KeyAgreement.getInstance("1.3.132.1.11.0", provider);
//      keyAgreementB = KeyAgreement.getInstance("1.3.132.1.11.0", provider);
//      keyAgreementA = KeyAgreement.getInstance("1.3.132.1.11.1", provider);
//      keyAgreementB = KeyAgreement.getInstance("1.3.132.1.11.1", provider);
//      keyAgreementA = KeyAgreement.getInstance("1.3.132.1.11.2", provider);
//      keyAgreementB = KeyAgreement.getInstance("1.3.132.1.11.2", provider);
//      keyAgreementA = KeyAgreement.getInstance("1.3.132.1.11.3", provider);
//      keyAgreementB = KeyAgreement.getInstance("1.3.132.1.11.3", provider);
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
      keyAgreementA = KeyAgreement.getInstance("EcDhWithNistKdf256", provider);
      keyAgreementB = KeyAgreement.getInstance("EcDhWithNistKdf256", provider);

      keyAgreementA.init(ecKeyPairA.getPrivate());

      keyAgreementB.init(ecKeyPairB.getPrivate());

      keyAgreementA.doPhase(ecKeyPairB.getPublic(), true);
      keyAgreementB.doPhase(ecKeyPairA.getPublic(), true);

      /**
       * Generate the aes secret keys. Some of the other possible key types include rsa, dsa, ec, ecdsa, dh, des, des2,
       * des3, rc2, rc4, rc5, etc.
       */
      //don't use CA_DecryptKeyAndUnwrap
//      aesKeyA = keyAgreementA.generateSecret("aes");
//      aesKeyB = keyAgreementB.generateSecret("aes");
      //use CA_DecryptKeyAndUnwrap
      byte[] s1 = keyAgreementA.generateSecret();
      byte[] s2 = keyAgreementB.generateSecret();
      aesKeyA = new SecretKeySpec(s1, 0, s1.length, "AES");
      aesKeyB = new SecretKeySpec(s2, 0, s2.length, "AES");
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

//    // Logout of the token
//    HSM_Manager.hsmLogout();
  }
}
