
// ****************************************************************************
// Copyright (c) 2010 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.LunaUtils;
import com.safenetinc.luna.provider.LunaProvider;
import javax.crypto.Cipher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;

/**
 * This sample demonstrates RSA encryption with padding.
 */
public class CipherRsaencPADDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";

  public static void main(String args[]) {
//        LunaSlotManager manager;
//        manager = LunaSlotManager.getInstance();
//
//        try {
//            manager.login(slot, passwd); // log in to the designated slot
//        } catch (Exception e) {
//            System.out.println("Exception during login");
//        }
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
      // need to make an rsa keypair in software.
      // ********************************************
      kpg = KeyPairGenerator.getInstance("RSA", "LunaProvider");
      kpg.initialize(2048);
      myPair = kpg.generateKeyPair();
    } catch (Exception e) {
      System.out.println("Exception generating keypair");
      e.printStackTrace();
    }

    // ********************************************
    // encrypt something
    // ********************************************
    byte[] bytes = "10000000008".getBytes();
    byte[] encrypted = null;
    try {
      Cipher myCipher = Cipher.getInstance("RSA/NONE/PKCS1v1_5", "LunaProvider");
      myCipher.init(Cipher.ENCRYPT_MODE, myPair.getPublic());
      encrypted = myCipher.doFinal(bytes);
    } catch (Exception e) {
      System.out.println("Exception ciphering the data");
      e.printStackTrace();
    }

    // ********************************************
    // decrypt the encrypted value
    // ********************************************
    byte[] decrypted = null;
    try {
      Cipher myCipher = Cipher.getInstance("RSA/NONE/PKCS1v1_5", "LunaProvider");
      myCipher.init(Cipher.DECRYPT_MODE, myPair.getPrivate());
      decrypted = myCipher.doFinal(encrypted);
    } catch (Exception e) {
      System.out.println("Exception deciphering the data");
      e.printStackTrace();
    }

    System.out.println("\n\n-----------------------");
    System.out.println("original: ");
    System.out.println("  Size:    " + bytes.length);
    System.out.println("  Content: " + LunaUtils.getHexString(bytes, true));
    System.out.println("encrypted: ");
    System.out.println("  Size:    " + encrypted.length);
    System.out.println("  Content: " + LunaUtils.getHexString(encrypted, true));
    System.out.println("decrypted: ");
    System.out.println("  Size:    " + decrypted.length);
    System.out.println("  Content: " + LunaUtils.getHexString(decrypted, true));
    System.out.println("\n\n-----------------------");

    if (java.util.Arrays.equals(bytes, decrypted)) {
      System.out.println("Decryption was successful");
    } else
      System.out.println("*** decryption failed");
    System.out.println("-----------------------\n\n");
  }
}
