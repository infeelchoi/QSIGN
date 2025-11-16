
// ****************************************************************************
// Copyright (c) 2010 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.PSource.PSpecified;

import com.safenetinc.luna.LunaUtils;
import com.safenetinc.luna.exception.LunaCryptokiException;
import com.safenetinc.luna.exception.LunaException;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.param.LunaParameterSpecOAEP;

/**
 * This sample demonstrates RSA OAEP encryption
 */
public class CipherRsaOAEPDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";

  public static void main(String args[]) {
    // LunaSlotManager manager;
    // manager = LunaSlotManager.getInstance();
    //
    // try {
    // manager.login(slot, passwd); // log in to the designated slot
    // } catch (Exception e) {
    // System.out.println("Exception during login");
    // }
    KeyStore myKeyStore = null;
    try {

      /* Note: could also use a keystore file, which contains the token label or slot no. to use. Load that via
       * "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save objects to the keystore via a
       * FileOutputStream. */

      Security.addProvider(new LunaProvider());
      ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot).getBytes());
      myKeyStore = KeyStore.getInstance("Luna");
      myKeyStore.load(is1, passwd.toCharArray());
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

    KeyPairGenerator kpg = null;
    KeyPair myPair = null;
    try {
      // ********************************************
      // need to make an rsa keypair
      // ********************************************
      kpg = KeyPairGenerator.getInstance("RSA", "LunaProvider");
      kpg.initialize(2048);
      myPair = kpg.generateKeyPair();
    } catch (Exception e) {
      System.out.println("Exception generating keypair");
      e.printStackTrace();
    }

    byte[] input = "abc".getBytes();
    byte[] param = "abcd".getBytes();
    PSource paramSource = new PSpecified(param);
    PSource badParams = new PSpecified("dcba".getBytes());
    Cipher cipher = null;
    Key pubKey = null;
    Key privKey = null;
    byte[] cipherText = null;

    try {

      pubKey = myPair.getPublic();
      privKey = myPair.getPrivate();

      /**
       * Get a Cipher instance using transformation string with SHA1 padding. Init the cipher with no OAEPParamterSpec
       * which will cause the PSource to be empty.
       */
      // cipher = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding",
      // "LunaProvider");
      // cipher = Cipher.getInstance("RSA/None/OAEPWithSHA224AndMGF1Padding",
      // "LunaProvider");
      // cipher = Cipher.getInstance("RSA/None/OAEPWithSHA256AndMGF1Padding",
      // "LunaProvider");
      // cipher = Cipher.getInstance("RSA/None/OAEPWithSHA384AndMGF1Padding",
      // "LunaProvider");
      cipher = Cipher.getInstance("RSA/None/OAEPWithSHA512AndMGF1Padding", "LunaProvider");

      cipher.init(Cipher.ENCRYPT_MODE, pubKey);
      cipherText = cipher.doFinal(input);
      System.out.println("Cipher text 1: " + LunaUtils.getHexString(cipherText, true));

      /**
       * Perform the same operation but this time using the SHA256 hash algorithm.
       */
      cipher = Cipher.getInstance("RSA/None/OAEPWithSHA256AndMGF1Padding", "LunaProvider");

      cipher.init(Cipher.ENCRYPT_MODE, pubKey);
      cipherText = cipher.doFinal(input);
      System.out.println("Cipher text 2: " + LunaUtils.getHexString(cipherText, true));

      /**
       * Get a cipher without specifying the hash algorithm. If an OAEPParameterSpec or a LunaParameterSpecOAEP is not
       * specified, the default will be SHA256.
       */
      cipher = Cipher.getInstance("RSA/None/OAEPPadding", "LunaProvider");

      // Using an OAEP parameter spec with SHA512
      OAEPParameterSpec oaepSpec = new OAEPParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, paramSource);

      cipher.init(Cipher.ENCRYPT_MODE, pubKey, oaepSpec);
      cipherText = cipher.doFinal(input);
      System.out.println("Cipher text 3: " + LunaUtils.getHexString(cipherText, true));

      /**
       * With a Luna OAEP parameter spec with SHA384
       */
      LunaParameterSpecOAEP lunaSpec = new LunaParameterSpecOAEP("SHA384", LunaParameterSpecOAEP.MGF1_SHA384,
          LunaParameterSpecOAEP.sourceType_DATA_SPECIFIED, param);

      cipher.init(Cipher.ENCRYPT_MODE, pubKey, lunaSpec);
      byte[] lunaBytes = cipher.doFinal(input);
      System.out.println("luna cipher Text: " + LunaUtils.getHexString(lunaBytes, true));

      // decrypt
      cipher.init(Cipher.DECRYPT_MODE, privKey, oaepSpec);
      byte[] decryptedText = cipher.doFinal(cipherText);

      cipher.init(Cipher.DECRYPT_MODE, privKey, lunaSpec);
      byte[] lunaDecrypted = cipher.doFinal(lunaBytes);

      // display original and decrypted
      System.out.println("  Original: " + LunaUtils.getHexString(input, true));
      System.out.println("  decrypted: " + LunaUtils.getHexString(decryptedText, true));
      System.out.println("  decrypted (luna spec): " + LunaUtils.getHexString(lunaDecrypted, true));

    } catch (Exception e) {
      System.out.println("*** Unexpected Exception ***");
      e.printStackTrace();
    }

    // try a bad param spec
    try {
      OAEPParameterSpec badSpec = new OAEPParameterSpec("SHA-512", "MGF1", java.security.spec.MGF1ParameterSpec.SHA512,
          badParams);
      cipher.init(Cipher.DECRYPT_MODE, privKey, badSpec);
      byte[] invalidText = cipher.doFinal(cipherText);
      System.out.println("  bad spec: " + LunaUtils.getHexString(invalidText, true));

    } catch (LunaException ex) {
      // the LunaCryptokiException from the JNI layer is wrapped in a LunaException
      if (((LunaCryptokiException) ex.getCause()).GetCKRValue() == 0x05) {
        System.out.println("Got expected exception from an invalid P source used during decryption.");
      } else {
        System.out.println("*** Unexpected Exception ***");
        ex.printStackTrace();
      }
    } catch (Exception e) {
      System.out.println("*** Unexpected Exception ***");
      e.printStackTrace();
    }
  }
}
