
// ****************************************************************************
// Copyright (c) 2010 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.safenetinc.luna.LunaUtils;
import com.safenetinc.luna.exception.LunaCryptokiException;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.key.LunaPrivateKeyRsa;

/**
 * This sample demonstrates wrapping a software key. This demo assumes that the Luna providers are not listed first in
 * the java.security file and that a software provider is the default This could be SUN / IBM / BC / ...
 */
public class CipherSoftwareWrapDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";

  public static void main(String args[]) {

    System.out.println("This sample requires a KE(Key Export) HSM...");
    System.out.println("");

    // LunaSlotManager manager;
    // manager = LunaSlotManager.getInstance();
    //
    // try {
    // manager.login(slot, passwd); // log in to the designated slot
    // } catch (Exception e) {
    // System.out.println("Exception during login");
    // System.exit(1);
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
    KeyGenerator kg = null;
    SecretKey swAESKey = null;
    SecretKey hwAESKey = null;
    try {
      // ********************************************
      // need to make an rsa keypair in software.
      // ********************************************
      kpg = KeyPairGenerator.getInstance("RSA");
      kpg.initialize(2048);
      myPair = kpg.generateKeyPair();
      // ********************************************
      // need to make an aes key in software
      // ********************************************
      kg = KeyGenerator.getInstance("AES");
      kg.init(256);
      swAESKey = kg.generateKey();

      // ********************************************
      // make the wrapping key. AES
      // ********************************************
      kg = KeyGenerator.getInstance("AES", "LunaProvider");
      kg.init(256);
      hwAESKey = kg.generateKey();

    } catch (Exception e) {
      System.out.println("Exception generating keys");
      e.printStackTrace();
      System.exit(1);
    }

    // ********************************************
    // encrypt something
    // ********************************************
    byte[] bytes = "Encrypt Me!".getBytes();
    byte[] encrypted = null;
    try {
      Cipher myCipher = Cipher.getInstance("RSA/NONE/PKCS1v1_5");
      myCipher.init(Cipher.ENCRYPT_MODE, myPair.getPublic());
      encrypted = myCipher.doFinal(bytes);
    } catch (Exception e) {
      System.out.println("Exception ciphering the data with software RSA key");
      e.printStackTrace();
      System.exit(1);
    }

    // ********************************************
    // encrypt something - aes
    // ********************************************
    byte[] aesencrypted = null;
    try {
      Cipher myCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      byte[] ivBytes = { (byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72,
          (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80 };
      AlgorithmParameters mAlgParams = AlgorithmParameters.getInstance("IV");
      mAlgParams.init(new IvParameterSpec(ivBytes));
      myCipher.init(Cipher.ENCRYPT_MODE, swAESKey, mAlgParams);
      aesencrypted = myCipher.doFinal(bytes);
    } catch (Exception e) {
      System.out.println("Exception ciphering the data with software AES key");
      e.printStackTrace();
      System.exit(1);
    }

    // ********************************************
    // try to wrap the private key
    // ********************************************
    byte[] wrappedKey = null;
    try {
      Cipher myWrapper = Cipher.getInstance("AES/CBC/PKCS5Padding", "LunaProvider");
      byte[] ivBytes = { (byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72,
          (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80 };
      AlgorithmParameters mAlgParams = AlgorithmParameters.getInstance("IV", "LunaProvider");
      mAlgParams.init(new IvParameterSpec(ivBytes));
      myWrapper.init(Cipher.WRAP_MODE, hwAESKey, mAlgParams);
      wrappedKey = myWrapper.wrap(myPair.getPrivate());

    } catch (InvalidKeyException ikex) {
      // If the InvalidKeyException is caused by a LunaCryptokiException, we're trying
      // to wrap the RSA private key off the HSM. This is not allowed. The key pair
      // must be created with a software provider for this sample to work properly.
      Throwable cause = ikex.getCause();
      if ((cause != null) && (cause instanceof LunaCryptokiException)) {
        LunaCryptokiException lcex = (LunaCryptokiException) cause;
        if (lcex.GetCKRValue() == 0x69) {
          System.out.println("The private RSA key was created with the Luna provider,"
              + " and cannot be wrapped off the HSM.  This demo requires a software"
              + " crypto provider that supports RSA to have precedence over the" + " Luna provider in java.security");
          System.exit(1);
        }
      }
      System.out.println("Exception during wrapping of software RSA private key");
      ikex.printStackTrace();
      System.exit(1);

    } catch (Exception ex) {
      System.out.println("Exception during wrapping of software RSA private key");
      ex.printStackTrace();
      System.exit(1);
    }

    // ********************************************
    // try to wrap the aes key
    // ********************************************
    byte[] wrappedAESKey = null;
    try {
      Cipher myWrapper = Cipher.getInstance("AES/CBC/PKCS5Padding", "LunaProvider");
      byte[] ivBytes = { (byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72,
          (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80 };
      AlgorithmParameters mAlgParams = AlgorithmParameters.getInstance("IV", "LunaProvider");
      mAlgParams.init(new IvParameterSpec(ivBytes));
      myWrapper.init(Cipher.WRAP_MODE, hwAESKey, mAlgParams);
      wrappedAESKey = myWrapper.wrap(swAESKey);

    } catch (InvalidKeyException ikex) {
      // If the InvalidKeyException is caused by a LunaCryptokiException, we're trying
      // to wrap the AES key off the HSM. This is not allowed because the key is not
      // marked extractable by default. The key must be created with a software provider
      // for this sample to work properly.
      Throwable cause = ikex.getCause();
      if ((cause != null) && (cause instanceof LunaCryptokiException)) {
        LunaCryptokiException lcex = (LunaCryptokiException) cause;
        if (lcex.GetCKRValue() == 0x6A) {
          System.out.println("The AES key was created with the Luna provider, and cannot"
              + " be wrapped off the HSM.  This demo requires a software crypto"
              + " provider that supports AES to have precedence over the Luna provider" + " in java.security");
          System.exit(1);
        }
      }
      System.out.println("Exception during wrapping of software AES key");
      ikex.printStackTrace();
      System.exit(1);

    } catch (Exception ex) {
      System.out.println("Exception during wrapping of software AES key");
      ex.printStackTrace();
      System.exit(1);
    }

    // ********************************************
    // unwrap the private key
    // ********************************************
    PrivateKey unwrappedKey = null;
    try {
      Cipher myUnwrapper = Cipher.getInstance("AES/CBC/PKCS5Padding", "LunaProvider");
      byte[] ivBytes = { (byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72,
          (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80 };
      AlgorithmParameters mAlgParams = AlgorithmParameters.getInstance("IV", "LunaProvider");
      mAlgParams.init(new IvParameterSpec(ivBytes));

      myUnwrapper.init(Cipher.UNWRAP_MODE, hwAESKey, mAlgParams);
      unwrappedKey = (LunaPrivateKeyRsa) myUnwrapper.unwrap(wrappedKey, "RSA", Cipher.PRIVATE_KEY);

    } catch (Exception e) {
      System.out.println("Exception attempting to unwrap wrapped RSA private key");
      e.printStackTrace();
      System.exit(1);
    }

    // ********************************************
    // unwrap the aes key
    // ********************************************
    SecretKey unwrappedAESKey = null;
    try {
      Cipher myUnwrapper = Cipher.getInstance("AES/CBC/PKCS5Padding", "LunaProvider");
      byte[] ivBytes = { (byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72,
          (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80 };
      AlgorithmParameters mAlgParams = AlgorithmParameters.getInstance("IV", "LunaProvider");
      mAlgParams.init(new IvParameterSpec(ivBytes));

      myUnwrapper.init(Cipher.UNWRAP_MODE, hwAESKey, mAlgParams);
      unwrappedAESKey = (SecretKey) myUnwrapper.unwrap(wrappedAESKey, "AES", Cipher.SECRET_KEY);

    } catch (Exception e) {
      System.out.println("Exception attempting to unwrap wrapped AES key");
      e.printStackTrace();
      System.exit(1);
    }

    // ********************************************
    // decrypt the encrypted value
    // ********************************************
    try {
      Cipher myCipher = Cipher.getInstance("RSA/NONE/PKCS1v1_5");
      myCipher.init(Cipher.DECRYPT_MODE, unwrappedKey);
      byte[] decrypted = myCipher.doFinal(encrypted);

      System.out.println("\n\n-----------------------");
      System.out.println("original: " + LunaUtils.getHexString(bytes, true));
      System.out.println("encrypted: " + LunaUtils.getHexString(encrypted, true));
      System.out.println("decrypted: " + LunaUtils.getHexString(decrypted, true));
      System.out.println("\n\n-----------------------");

      if (java.util.Arrays.equals(bytes, decrypted)) {
        System.out.println("Decryption was successful");
      } else
        System.out.println("*** decryption failed");
      System.out.println("-----------------------\n\n");

    } catch (Exception e) {
      System.out.println("Exception deciphering the RSA-ciphered data");
      e.printStackTrace();
    }

    // ********************************************
    // decrypt the encrypted value - AES
    // ********************************************
    try {
      Cipher myCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "LunaProvider");
      byte[] ivBytes = { (byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72,
          (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80 };
      AlgorithmParameters mAlgParams = AlgorithmParameters.getInstance("IV", "LunaProvider");
      mAlgParams.init(new IvParameterSpec(ivBytes));
      myCipher.init(Cipher.DECRYPT_MODE, unwrappedAESKey, mAlgParams);
      byte[] aesdecrypted = myCipher.doFinal(aesencrypted);

      System.out.println("\n\n-----------------------");
      System.out.println("original: " + LunaUtils.getHexString(bytes, true));
      System.out.println("encrypted: " + LunaUtils.getHexString(aesencrypted, true));
      System.out.println("decrypted: " + LunaUtils.getHexString(aesdecrypted, true));
      System.out.println("\n\n-----------------------");

      if (java.util.Arrays.equals(bytes, aesdecrypted)) {
        System.out.println("Decryption was successful");
      } else
        System.out.println("*** decryption failed");
      System.out.println("-----------------------\n\n");

    } catch (Exception e) {
      System.out.println("Exception deciphering the AES-ciphered data");
      e.printStackTrace();
    }
  }
}
