
// ****************************************************************************
// Copyright (c) 2010 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import com.safenetinc.luna.provider.LunaProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;

/**
 * This example illustrates how to generate a DES key and Encrypt/Decrypt with the generated key.
 */
public class CipherDESDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";

  public static void main(String[] args) {

//    // Login to the HSM
//    HSM_Manager.hsmLogin();
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

    // List the available providers
    MiscProviderList.listProviders();

    KeyGenerator keyGen = null;
    Key desKey = null;
    try {
      // Generate a DES key
      /* The KeyGenerator class is used for determining the type of key being generated. Common options for this are
       * DES, DESede (TripleDES) and AES. This example has specified the LunaProvider explicitly. If the Luna provider
       * is the default then this is not necessary. For more information about the algorithms available in the Luna
       * provider please see the Luna Development Guide. For more information concerning other providers, please read
       * the documentation available for the provider in question. */
      System.out.println("Generating DES key");
      keyGen = KeyGenerator.getInstance("DES", "LunaProvider");
      keyGen.init(56);
      desKey = keyGen.generateKey();

      /* Note that this DES key is not permanently stored on the HSM. See the examples in LunaKeyStoreDemo.java for
       * information on permanently storing keys on the HSM. */
    } catch (Exception e) {
      System.out.println("Exception during Key Generation - " + e.getMessage());
      System.exit(1);
    }

    // Initialize the Cipher for Encryption and encrypt the message
    String starttext = "Some Text to Encrypt as an Example";
    byte[] bytes = starttext.getBytes();
    System.out.println("PlainText = " + starttext);

    Cipher desCipher = null;
    byte[] iv = null;
    try {
      // Initialize the Cipher
      /* There are other DES ciphers available for use: DES/ECB/NoPadding DES/CBC/NoPadding For a full list of supported
       * Ciphers in the Luna provider please see the Luna Development Guide. For a list of supported Ciphers in
       * alternate providers please see the documentation of the provider in question. Since we aren't specifying an IV
       * in the Cipher init, we retrieve the one that was generated for use in the decrypt operation. */
      desCipher = Cipher.getInstance("DES/CBC/PKCS5Padding", "LunaProvider");
//      desCipher = Cipher.getInstance("DES/ECB/NoPadding", "LunaProvider");
//      desCipher = Cipher.getInstance("DES/CBC/NoPadding", "LunaProvider");
      desCipher.init(Cipher.ENCRYPT_MODE, desKey);
      iv = desCipher.getIV();
    } catch (Exception e) {
      System.out.println("Exception during Cipher Initialization - " + e.getMessage());
      System.exit(1);
    }

    byte[] encryptedbytes = null;
    try {
      // Encrypt the message
      /* Encrypt/Decrypt operations can be performed in one of two ways 1. Singlepart 2. Multipart To perform a single
       * part encrypt/decrypt operation use the following example. Multipart encrypt/decrypt operations require use of
       * the Cipher.update() and Cipher.doFinal() methods. For more information please see the class documentation for
       * the java.cryptox.Cipher class with respect to the version of the JDK you are using. */
      System.out.println("Encrypting PlainText");
      encryptedbytes = desCipher.doFinal(bytes);
    } catch (Exception e) {
      System.out.println("Exception during Encryption - " + e.getMessage());
      System.exit(1);
    }

    try {
      // Decrypt the text
      System.out.println("Decrypting PlainText");
      desCipher.init(Cipher.DECRYPT_MODE, desKey, new IvParameterSpec(iv));
      byte[] decryptedbytes = desCipher.doFinal(encryptedbytes);
      // convert the bytes to something more human-readable
      String endtext = new String(decryptedbytes);
      System.out.println("Decrypted PlainText = " + endtext);
    } catch (Exception e) {
      System.out.println("Exception during Decryption - " + e.getMessage());
      System.exit(1);
    }

//    // Logout of the token
//    HSM_Manager.hsmLogout();
  }
}
