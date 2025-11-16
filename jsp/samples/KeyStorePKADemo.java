/*******************************************************************************
 * Copyright (c) 2020-2022 Thales Group. All rights reserved.
 *
 * All rights reserved. This file contains information that is
 * proprietary to Thales Group. and may not be distributed
 * or copied without written consent from Thales Group.
 *******************************************************************************/
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

import com.safenetinc.luna.LunaAPI;
import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.attributes.LunaPkcs11Attributes;
import com.safenetinc.luna.provider.LunaCertificateX509;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.key.LunaKey;
import com.safenetinc.luna.provider.param.LunaPkcs11AttributesParameterSpec;

/**
 * This example illustrates how to use the Luna KeyStore.
 */
public class KeyStorePKADemo {

  // Configure these as required.
  public static final String provider = "LunaProvider";
  public static final String PKAPassword = "password_for_pka";
  public static final String badPKAPassword = "wrong_password_for_pka";
  public static final String pKeyAlias = "EC_PRIV_KEY";
  private static final int slot = 2;
  private static final String passwd = "userpin1";

  public static void main(String[] args) {

    KeyStore myStore = null;
    LunaKey tempPrivKey = null;

    // You will need to set this to get JSP out of default non-PKA behaviour
    LunaSlotManager.setRequirePKA(true);

    System.out.println("LunaProvider version: " + LunaProvider.getInstance().getVersion());

    try {
      ByteArrayInputStream is1 = new ByteArrayInputStream(
          ("slot:" + slot + "\ncaching:false" + "\ndefertokenization:true").getBytes());
//      is1 = new ByteArrayInputStream(("tokenlabel:" + label).getBytes());

      // The Luna keystore is a Java Cryptography view of the contents
      // of the HSM.
      Security.addProvider(new LunaProvider());
      myStore = KeyStore.getInstance("Luna");

      /*
       * Loading a Luna keystore can be done without specifying an input stream or
       * password if a login was previously done to the first slot. In this case we
       * have not logged in to the slot and shall do so here. The byte array input
       * stream contains "slot:1" specifying that we wish to open a keystore
       * corresponding to the slot with ID 1. You can also open keystores by name.
       * using the syntax "tokenlabel:PartitionName"
       */
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

    //Just create any old key pair...ECDSA is convenient
    KeyPairGenerator keyGen = null;
    KeyPair keyPair = null;
    try {
      // Generate an ECDSA KeyPair
      System.out.println("Generating the ECDSA Keypair.");
      keyGen = KeyPairGenerator.getInstance("ECDSA", provider);
      /* ECDSA keys need to know what curve to use. If you know the curve ID to use you can specify it directly. In the
       * Luna Provider all supported curves are defined in LunaECCurve */
      ECGenParameterSpec ecSpec = new ECGenParameterSpec("c2pnb304w1");

      //add PKA p11 attributes here
      LunaPkcs11Attributes attributes = new LunaPkcs11Attributes();
      attributes.setByteArrayAttribute(LunaPkcs11Attributes.PRIVATE, LunaAPI.CKA_AUTH_DATA, PKAPassword.getBytes());
      LunaPkcs11AttributesParameterSpec params = new LunaPkcs11AttributesParameterSpec(attributes, ecSpec);

      keyGen.initialize(params);
      keyPair = keyGen.generateKeyPair();
      tempPrivKey = (LunaKey)(keyPair.getPrivate());
      tempPrivKey.authorize(PKAPassword,false);
    } catch (Exception e) {
      System.out.println("Exception during Key Generation - " + e.getMessage());
      System.exit(1);
    }

    //We need a cert chain for the following step of inserting a key into the HSM
    //as a token object via JCA KeyStore API
    LunaCertificateX509[] certChain = null;
    try {
      certChain = new LunaCertificateX509[1];
      String subjectname = "CN=some guy, L=around, C=US";
      BigInteger serialNumber = new BigInteger("12345");
      Date notBefore = new Date();
      Date notAfter = new Date(notBefore.getTime() + 1000000000);

      // The LunaCertificateX509 class has a special method that allows
      // you to self-sign a certificate.
      certChain[0] = LunaCertificateX509.SelfSign(keyPair, subjectname, serialNumber, notBefore, notAfter);
    } catch (Exception e) {
      System.out.println("Exception during Certification Creation - " + e.getMessage());
      System.exit(1);
    }

    try {
      System.out.println("inserting the private key into keystore...");
      try {
        myStore = KeyStore.getInstance("Luna");
        myStore.load(null, null);
      } catch (NoSuchAlgorithmException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (CertificateException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      myStore.setKeyEntry(pKeyAlias, tempPrivKey, PKAPassword.toCharArray(), certChain);
    } catch (KeyStoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      System.out.println("retrieving the previously written private key from the keystore...");
      // enabling PKA above and providing a password here will take advantage of
      // Luna's proprietary PKA mechanism.
      tempPrivKey = (LunaKey) myStore.getKey(pKeyAlias, PKAPassword.toCharArray());
      Certificate[] newCertChain = myStore.getCertificateChain(pKeyAlias);
      myStore.setKeyEntry("DUMMY_IN_MEMORY_KEY", tempPrivKey, PKAPassword.toCharArray(), newCertChain);
      tempPrivKey = (LunaKey) myStore.getKey("DUMMY_IN_MEMORY_KEY", PKAPassword.toCharArray());
    } catch (KeyStoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (UnrecoverableKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    byte[] bytes = "Some Text to Sign as an Example".getBytes();
    String sigAlg = "SHA256withECDSA";
    Signature sig = null;
    Signature ver = null;
    try {
      System.out.println("Signing with the authorized private key.");
      sig = Signature.getInstance(sigAlg, "LunaProvider");
      sig.initSign((PrivateKey) tempPrivKey);
      sig.update(bytes);
      byte[] signature = sig.sign();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SignatureException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      // Remove the key from the KeyStore
      /*
       * Luna SA partitions have a maximum number of persistent objects they can hold.
       * It is important to clean up old keys and certificates that are no longer
       * used, just like it is necessary to free disk space by erasing unused files.
       * The LunaSlotManager (TODO: Confirm) class has some special methods to allow
       * you to keep track of the amount of space left on a Luna SA partition.
       */

      //try deleting it from the in-memory keystore
//      System.out.println("Removing key and cert from Keystore.");
//      myStore.deleteEntry("DUMMY_IN_MEMORY_KEY");
//      myStore.deleteEntry(pKeyAlias);

      //...OR...persist it (keep in mind that it will remain until explicitly removed)
//      System.out.println("Persisting key and cert from keystore to keystore file...");
//      myStore.store(null, null);

    } catch (Exception e) {
      System.out.println("Exception removing Key - " + e.getMessage());
      System.exit(1);
    }

  }
}
