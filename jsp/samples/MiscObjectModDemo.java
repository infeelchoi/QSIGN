
// ****************************************************************************
// Copyright (c) 2010 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

import com.safenetinc.luna.LunaAPI;
import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.LunaTokenObject;
import com.safenetinc.luna.LunaUtils;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.key.LunaKey;

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
 * This sample demonstrates how to modify PKCS#11 attributes through java.
 * <p>
 * WARNING: In general, all access to HSM objects should be through the JCA/JCE API. Accessing and modifying
 * LunaTokenObjects directly can have unintended consequences; the code below is for illustration only.
 */
public class MiscObjectModDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";

  public static void main(String args[]) {
//        LunaSlotManager manager = LunaSlotManager.getInstance();

//        try {
//            manager.login(slot, passwd); // log in to the designated slot
//        } catch (Exception e) {
//            System.out.println("Exception during login");
//            System.exit(-1);
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

    KeyPair myPair = null;
    try {
      // first make an rsa keypair so we have objects to examine
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "LunaProvider");
      kpg.initialize(2048);
      myPair = kpg.generateKeyPair();

    } catch (Exception e) {
      System.out.println("Exception generating keypair");
      e.printStackTrace();
      System.exit(-1);
    }

    // now let's modify the objects

    LunaTokenObject pubObj = null;
    LunaTokenObject privObj = null;
    try {
      // get the key handles, then use those to create the LunaTokenObjects
      int privateHandle = ((LunaKey) (myPair.getPrivate())).GetKeyHandle();
      int publicHandle = ((LunaKey) (myPair.getPublic())).GetKeyHandle();
      pubObj = LunaTokenObject.LocateObjectByHandle(publicHandle);
      privObj = LunaTokenObject.LocateObjectByHandle(privateHandle);

      // get/set a boolean attribute value - CKA_EXTRACTABLE
      boolean extractable = privObj.GetBooleanAttribute(LunaAPI.CKA_EXTRACTABLE);
      privObj.SetBooleanAttribute(LunaAPI.CKA_EXTRACTABLE, !extractable);
      boolean extractable2 = privObj.GetBooleanAttribute(LunaAPI.CKA_EXTRACTABLE);

      // it should have changed
      System.out.println("Boolean Attribute test:");
      if (extractable == !extractable2)
        System.out.println("\tAttribute successfully changed.\n");
      else
        System.out.println("\tAttribute was not changed!\n");

      // get/set a small attribute value - CKA_WRAP
      long wrapVal = 0;
      long wrap = pubObj.GetSmallAttribute(LunaAPI.CKA_WRAP);
      pubObj.SetSmallAttribute(LunaAPI.CKA_WRAP, wrapVal);
      long wrap2 = pubObj.GetSmallAttribute(LunaAPI.CKA_WRAP);

      System.out.println("Small Attribute Test:\n\tOld value: " + wrap + "\n\tNew Value: " + wrap2
          + "\nTried to set it as: " + wrapVal + "\n");

      // get/set a large attribute value - CKA_ID
      byte[] newId = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
      // this will be empty (null) by default
      byte[] id = pubObj.GetLargeAttribute(LunaAPI.CKA_ID);
      long result = pubObj.SetLargeAttribute(LunaAPI.CKA_ID, newId);
      byte[] id2 = pubObj.GetLargeAttribute(LunaAPI.CKA_ID);

      System.out.println("Large Attribute Test:\n\tOld value: " + LunaUtils.getHexString(id, true) + "\n\tNew Value: "
          + LunaUtils.getHexString(id2, true) + "\nTried to set it as: " + LunaUtils.getHexString(newId, true) + "\n");
      System.out.println("Result: " + result);

    } catch (Exception ex) {
      System.out.println("Got unexpected Exception while modifying attributes");
      ex.printStackTrace();
      System.exit(-1);
    }

    // do some checks on invalid operations.
    // should not be able to get private exponent
    try {
      privObj.GetLargeAttribute(LunaAPI.CKA_PRIVATE_EXPONENT);
    } catch (Exception ex) {
      System.out.println("Got expected Exception accessing private exponent.");
    }
    // should not be able to change public exponent
    try {
      // get a small attribute value - CKA_PRIVATE_EXPONENT
      long newExponent = 17;
      // set a small attribute value
      pubObj.SetSmallAttribute(LunaAPI.CKA_PUBLIC_EXPONENT, newExponent);

    } catch (Exception ex) {
      System.out.println("Got expected Exception setting public exponent.");
    }

    // log out of the token
//        manager.logout();
  }
}
