
// ****************************************************************************
// Copyright (c) 2015 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.param.LunaGmacParameterSpec;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;

/**
 * This example illustrates how to generate an AES key and using it to generate a GMAC tag.
 */
public class MacGmacAesDemo {
  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";

  /**
   * Print out the GMAC tag in hex format.
   *
   * @param signature
   *          A byte array at most 16 bytes containing the GMAC tag
   *
   * @return none
   */
  private static void outputSignature(byte[] signature, long tagBits) {
    int tagBytes = (int) (tagBits / 8);
    System.out.print("GMAC Tag: ");
    for (int index = 0; index < tagBytes; ++index) {
      System.out.print(String.format("%02x", signature[index]));
    }

    System.out.println();

    if (signature.length > tagBytes) {
      System.out.print("IV: ");
      for (int index = tagBytes; index < signature.length; ++index) {
        System.out.print(String.format("%02x", signature[index]));
      }
      System.out.println();
    }
  }

  /**
   * Create an instance of algorithm parameter specification to generate a GMAC tag.
   *
   * @return an instance of LunaGmacParameterSpec containing a blank IV if HSM is in FIPS mode and additional
   *         authenticated data. If HSM is not in FIPS mode, we pass an IV of size 96 bits
   */
  private static LunaGmacParameterSpec getParameterSpec() {
    LunaSlotManager lsm = LunaSlotManager.getInstance();
    byte[] iv = null;
    if (!lsm.isFIPSEnabled()) {
      iv = new byte[] { 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55 };
    }

    byte[] aad = { 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55 };

    return new LunaGmacParameterSpec(iv, aad);
  }

  public static void main(String[] args) {
//        //Login to our HSM module
//        LunaSlotManager lsm = LunaSlotManager.getInstance();
//        lsm.login(slot, passwd);

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

    // Display the list of providers the system has
    System.out.println("Below is a list of Java Service Providers:");
    MiscProviderList.listProviders();

    // The next block of code is to generate an AES key
    KeyGenerator keyGen = null;
    SecretKey aesKey = null;
    try {
      System.out.print("Generating AES key...");
      keyGen = KeyGenerator.getInstance("AES", "LunaProvider");
      keyGen.init(128);
      aesKey = keyGen.generateKey();
      System.out.println(" successful!");
    } catch (Exception e) {
      System.out.println("Exception during Key Generation - " + e.getMessage());
      System.exit(1);
    }

    try {
      // Let's get the mac algorithm from the provider
      Mac mac = Mac.getInstance("GmacAes", "LunaProvider");

      // initialize it using our defined parameter spec
      System.out.println("Initializing GMAC engine.");
      LunaGmacParameterSpec spec = getParameterSpec();
      mac.init(aesKey, spec);

      // generate a GMAC tag
      String message = "This is a confidential message.";
      System.out.println("Creating a GMAC tag.");
      byte[] digest = mac.doFinal(message.getBytes());

      // output the tag
      outputSignature(digest, spec.getTagBits());
    } catch (NoSuchAlgorithmException e) {
      System.out.println("No Such Algorithm: " + e.getMessage());
      return;
    } catch (InvalidKeyException e) {
      System.out.println("Invalid Key: " + e.getMessage());
      return;
    } catch (InvalidAlgorithmParameterException e) {
      System.out.println("Invalid algorithm parameter: " + e.getMessage());
      return;
    } catch (NoSuchProviderException e) {
      System.out.println(e.getMessage());
    } finally {
//            lsm.logout();
    }
  }
}
