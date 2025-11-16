
// ****************************************************************************
// Copyright (c) 2010 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

import com.safenetinc.luna.LunaAPI;
import com.safenetinc.luna.exception.LunaCryptokiException;
import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.provider.key.LunaKey;
import com.safenetinc.luna.provider.key.LunaPrivateKeyRsa;
import com.safenetinc.luna.provider.key.LunaSIMKey;

import javax.crypto.KeyGenerator;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;

/**
 * This sample demonstrates how to access the Luna SIM interface through Java.
 * <p>
 * <b>This sample application will not run on SA 5.0 since it does not support theSIM capability.</b>
 */
public class MiscSIMDemo {

  // This is the challenge for the main partition
  public static final String challenge = "userpin";

  // these passwords are used for SIM extraction / insertion
  public String password = "password";
  public String badPassword = "badpass";

  // some text to sign
  byte[] document = "This document will never be trusted unless signed".getBytes();

  public static void main(String[] args) {

    System.out.println("This test is OBSOLETE for now...SIM might resurrect though...");

    // login to the first available slot on the HSM
    LunaSlotManager manager = LunaSlotManager.getInstance();
    try {
      manager.login(challenge);
    } catch (Exception e) {
      System.out.println("Exception during login");
      System.exit(0);
    }

    // single key examples
    MiscSIMDemo se = new MiscSIMDemo();
    se.simNoPassword();
    se.simWithPassword();
    se.simMofNPasswords();

    // portable / multiple key example
    se.multiKeyExample();
  }

  /**
   * Tests SIM operation without passwords.
   */
  public boolean simNoPassword() {
    System.out.println("\n===========================");
    System.out.println("Running simNoPassword Test");
    System.out.println("===========================");

    KeyPair myPair = genKeyPair();
    if (myPair == null)
      return false;

    // sim the private key without a password
    LunaSIMKey simKey = null;
    try {
      simKey = new LunaSIMKey(myPair.getPrivate());
    } catch (InvalidKeyException ike) {
      System.out.println("Unable to create a SIM key from the given key.");
      return false;
    }

    // get the SIM blob
    byte[] myBlob = simKey.getEncoded();

    // destroy the private key
    ((LunaKey) myPair.getPrivate()).DestroyKey();

    // insert the key from the blob
    simKey = new LunaSIMKey(myBlob);
    LunaPrivateKeyRsa privKey = null;

    try {
      privKey = (LunaPrivateKeyRsa) simKey.getKey();
    } catch (InvalidKeyException ike) {
      System.out.println("Unable to unSIM the given key.");
      return false;
    }

    // sign some data
    signTest(myPair.getPublic(), privKey);

    // cleanup
    ((LunaKey) myPair.getPublic()).DestroyKey();
    privKey.DestroyKey();
    return true;
  }

  /**
   * Tests SIM operation with passwords.
   */
  public boolean simWithPassword() {
    System.out.println("\n===========================");
    System.out.println("Running simWithPassword Test");
    System.out.println("===========================");
    // generate a KeyPair
    KeyPair myPair = genKeyPair();
    if (myPair == null)
      return false;

    // sim the private key without a password
    LunaSIMKey simKey = null;
    try {
      simKey = new LunaSIMKey(myPair.getPrivate(), password.toCharArray());
    } catch (InvalidKeyException ike) {
      System.out.println("Unable to create a SIM key from the given key.");
      return false;
    }

    // get the sim blob
    byte[] myBlob = simKey.getEncoded();

    // destroy the private key
    ((LunaKey) myPair.getPrivate()).DestroyKey();
    // let our simKey object be collected by the GC
    simKey = null;

    // insert the key from the blob
    simKey = new LunaSIMKey(myBlob);
    LunaPrivateKeyRsa privKey = null;

    try {
      // if we use no password it shouldn't work
      privKey = (LunaPrivateKeyRsa) simKey.getKey();
    } catch (InvalidKeyException ike) {
      System.out.println("Invalid Key Exception during UnSIM operation without password.");
      return false;
    } catch (LunaCryptokiException lce) {
      if (lce.getMessage().contains("80000017"))
        System.out.println("UnSIM operation correctly failed with Cryptoki error CKR_SIM_AUTHORIZATION_FAILED");
    }

    try {
      // if we use an incorrect password it shouldn't work
      privKey = (LunaPrivateKeyRsa) simKey.getKey(badPassword.toCharArray());
    } catch (InvalidKeyException ike) {
      System.out.println("Invalid Key Exception during UnSIM operation with a bad password.");
      return false;
    } catch (LunaCryptokiException lce) {
      if (lce.getMessage().contains("80000017"))
        System.out.println("UnSIM operation correctly failed with Cryptoki error CKR_SIM_AUTHORIZATION_FAILED");
    }

    try {
      // if we use an incorrect password it shouldn't work
      privKey = (LunaPrivateKeyRsa) simKey.getKey(password.toCharArray());
    } catch (InvalidKeyException ike) {
      System.out.println("UnSIM operation failed unexpectedly.");
    }

    // sign some data
    signTest(myPair.getPublic(), privKey);

    // cleanup
    ((LunaKey) myPair.getPublic()).DestroyKey();
    privKey.DestroyKey();

    return true;
  }

  /**
   * Tests SIM operation with M of N passwords.
   */
  public boolean simMofNPasswords() {
    System.out.println("\n===========================");
    System.out.println("Running simMofNPasswords Test");
    System.out.println("===========================");
    // generate a keypair
    KeyPair myPair = genKeyPair();
    if (myPair == null)
      return false;

    // sim the private key without a password
    LunaSIMKey simKey = null;
    try {
      // for M of N we use a collection of strings as passwords.
      String[] pwArray = { "These", "are", "example", "passwords", "for", "M", "of", "N", "SIM", "encryption" };
      List<String> pwList = Arrays.asList(pwArray);

      // this example will use 7 as the M value, thus 7 of 10
      simKey = new LunaSIMKey(myPair.getPrivate(), 7, pwList);
    } catch (InvalidKeyException ike) {
      System.out.println("Unable to create a SIM key from the given key.");
      return false;
    }

    // get the sim blob
    byte[] myBlob = simKey.getEncoded();

    // destroy the private key
    ((LunaKey) myPair.getPrivate()).DestroyKey();
    // let our simKey object be collected by the GC
    simKey = null;

    // insert the key from the blob
    simKey = new LunaSIMKey(myBlob);
    LunaPrivateKeyRsa privKey = null;

    try {
      // if we use no password it shouldn't work
      privKey = (LunaPrivateKeyRsa) simKey.getKey();
    } catch (InvalidKeyException ike) {
      System.out.println("Invalid Key Exception during UnSIM operation without password.");
      return false;
    } catch (LunaCryptokiException lce) {
      if (lce.getMessage().contains("80000017"))
        System.out.println("UnSIM operation correctly failed with Cryptoki error CKR_SIM_AUTHORIZATION_FAILED");
    }

    try {
      // if we use an incorrect password it shouldn't work
      privKey = (LunaPrivateKeyRsa) simKey.getKey(badPassword.toCharArray());
    } catch (InvalidKeyException ike) {
      System.out.println("Invalid Key Exception during UnSIM operation with a bad password.");
      return false;
    } catch (LunaCryptokiException lce) {
      if (lce.getMessage().contains("80000017"))
        System.out.println("UnSIM operation correctly failed with Cryptoki error CKR_SIM_AUTHORIZATION_FAILED");
    }

    try {
      // if we use less than 7 passwords then it should fail
      String[] pwArray = { "These", "are", "example", "passwords", "for" };
      // String[] pwArray = {"These", "are", "example", "passwords",
      // "for", "M", "of", "N", "SIM", "encryption"};
      List<String> pwList = Arrays.asList(pwArray);

      privKey = (LunaPrivateKeyRsa) simKey.getKey(LunaAPI.CKA_SIM_PASSWORD, pwList);
    } catch (InvalidKeyException ike) {
      System.out.println("Invalid Key Exception during UnSIM operation with < M passwords.");
      return false;
    } catch (LunaCryptokiException lce) {
      if (lce.getMessage().contains("80000017"))
        System.out.println("UnSIM operation correctly failed with Cryptoki error CKR_SIM_AUTHORIZATION_FAILED");
    }

    try {
      // should pass - using 9 of 10 which is greater than 7
      String[] pwArray = { "These", "are", "passwords", "for", "M", "of", "N", "SIM", "encryption" };
      List<String> pwList = Arrays.asList(pwArray);
      privKey = (LunaPrivateKeyRsa) simKey.getKey(LunaAPI.CKA_SIM_PASSWORD, pwList);
    } catch (InvalidKeyException ike) {
      System.out.println("UnSIM operation failed unexpectedly.");
    }

    // sign some data
    signTest(myPair.getPublic(), privKey);

    // cleanup
    ((LunaKey) myPair.getPublic()).DestroyKey();
    privKey.DestroyKey();
    return true;
  }

  /**
   * Example of SIM Extracting / Inserting a set of keys
   */
  private boolean multiKeyExample() {
    // Portable SIM requires the 'offboard storage' capability
    // SIM TYPES:
    /* CKA_SIM_NO_AUTHORIZATION // no authorization needed CKA_SIM_PASSWORD // plain-text passwords CKA_SIM_CHALLENGE //
     * challenge secrets emitted through the secure port CKA_SIM_SECURE_PORT // PED keys
     * CKA_SIM_PORTABLE_NO_AUTHORIZATION // no authorization needed, portable CKA_SIM_PORTABLE_PASSWORD // plain-text
     * passwords, portable CKA_SIM_PORTABLE_CHALLENGE // challenge secrets emitted through the secure port, portable
     * CKA_SIM_PORTABLE_SECURE_PORT // PED keys, portable */
    long authForm = LunaAPI.CKA_SIM_PASSWORD;

    String[] pwArray = { "password" };
    List<String> pwList = Arrays.asList(pwArray);
    byte[] simBlob = null;

    System.out.println("\n===========================");
    System.out.println("Running sim MultiKey Test\n");
    System.out.println("This test is designed to use a second partition / slot");
    System.out.println("It uses Portable SIM.");
    System.out.println("If the target slot does not use the same domain as the");
    System.out.println("source then it will fail.");
    System.out.println("===========================");

    // get a bunch of keys
    LunaKey[] keys = getArrayOfKeys();

    // sim them out using portable password SIM Extraction
    try {
      simBlob = LunaSIMKey.SIMExtract(keys, 1, 1, authForm, pwList);
    } catch (InvalidKeyException ike) {
      System.out.println("Unexpected InvalidKeyException while SIM Extracting keys");
      System.exit(-1);
    }

    // remove the keys from the HSM
    for (int i = 0; i < keys.length; i++)
      keys[i].DestroyKey();

    keys = null;

    // sim them in to the second slot / partition
    // display list of slots:
    System.out.println("Which slot would you like to use for SIM Insertion?");
    System.out.println("Slot 1 was used for extraction. \n");
    System.out.println(" Choice: [1-" + LunaSlotManager.getInstance().getNumberOfSlots() + "]");
    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
    String input = null;
    try {
      input = br.readLine();
    } catch (IOException ioe) {
      System.out.println("IO Error reading response!");
      System.exit(-1);
    }

    int slot = Integer.valueOf(input);
    try {
      if (slot != 1) {
        System.out.println("Please enter the password for slot: " + slot);
        input = br.readLine();

        LunaSlotManager manager = LunaSlotManager.getInstance();
        manager.login(slot, input); // log in to the specified slot
      } else
        input = challenge;
    } catch (IOException ioe) {
      System.out.println("IO Error during login");
      System.exit(-1);
    }

    try {
      // should pass - using 9 of 10 which is greater than 7

      keys = LunaSIMKey.SIMInsert(authForm, pwList, simBlob);
      System.out.println("SIM INSERT operation inserted " + keys.length + " total keys");

    } catch (InvalidKeyException ike) {
      System.out.println("UnSIM operation failed unexpectedly.");
    }

    return true;

  }

  /**
   * Helper method to generate an RSA key pair
   */
  private KeyPair genKeyPair() {
    KeyPairGenerator kpg = null;
    KeyPair myPair = null;
    try {
      kpg = KeyPairGenerator.getInstance("RSA", "LunaProvider");
      kpg.initialize(1024);
      myPair = kpg.generateKeyPair();
    } catch (Exception e) {
      System.out.println("Exception generating keypair");
      return null;
    }

    return myPair;
  }

  /**
   * Helper method to sign/verify some data using the public and private keys given
   */
  private boolean signTest(PublicKey pubKey, PrivateKey privKey) {
    // sign some data
    byte[] signature = null;
    Signature rsasig = null;
    try {
      rsasig = Signature.getInstance("SHA256withRSA", "LunaProvider");
      rsasig.initSign(privKey);
      rsasig.update(document);
      signature = rsasig.sign();

      // verify with the public key
      rsasig.initVerify(pubKey);
      rsasig.update(document);
      boolean verified = rsasig.verify(signature);
      if (verified == true) {
        System.out.println("Signature passed verification");
      } else {
        System.out.println("Signature failed verification");
      }

    } catch (NoSuchAlgorithmException nsae) {
      System.out.println("No Such Algorithm Exception while performing signature operation");

      return false;
    } catch (InvalidKeyException ike) {
      System.out.println("Invalid Key Exception while performing signature operation");
      return false;
    } catch (NoSuchProviderException nspe) {
      System.out.println("No Such Provider Exception while performing signature operation");
      return false;
    } catch (SignatureException se) {
      System.out.println("Signature Exception while performing signature operation");
      return false;
    }
    return true;
  }

  /**
   * Helper method to generate a some RSA and AES keys
   */
  private LunaKey[] getArrayOfKeys() {

    LunaKey[] keys = new LunaKey[20];
    // make a bunch of RSA key pairs
    KeyPairGenerator kpg = null;
    KeyGenerator kg = null;

    try {
      // 8 RSA key pairs
      for (int i = 0; i < 16; i = i + 2) {
        kpg = KeyPairGenerator.getInstance("RSA", "LunaProvider");
        kpg.initialize(1024);
        KeyPair myPair = kpg.generateKeyPair();
        keys[i + 1] = (LunaKey) myPair.getPublic();
        keys[i] = (LunaKey) myPair.getPrivate();
      }

      // AES keys
      for (int i = 16; i < 20; i++) {
        kg = KeyGenerator.getInstance("AES", "LunaProvider");
        kg.init(256);
        keys[i] = (LunaKey) kg.generateKey();
      }

    } catch (Exception e) {
      System.out.println("Exception generating keypair");
      return null;
    }

    // return the array
    return keys;
  }
}
