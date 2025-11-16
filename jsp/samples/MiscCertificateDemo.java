// ****************************************************************************
// Copyright (c) 2010-2020 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

//import com.safenetinc.luna.LunaSlotManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import com.safenetinc.luna.provider.LunaCertificateX509;
import com.safenetinc.luna.provider.LunaProvider;

/**
 * This is a simple example of creating a self-signed certificate with a generated RSA KeyPair.
 *
 * The keys and certificate will be stored in the first available slot after they are created.
 *
 * The objects will be cleaned off of the HSM before the application exits.
 *
 * For more information regarding Luna KeyStores please see KeyStoreLunaDemo.java
 *
 */
public class MiscCertificateDemo {

  // Configure these as required.
  private static final int slot = 1;
  private static final String passwd = "userpin";
  //use empty string for non-activated PED-based partitions
  //  private static final String passwd = "";
  private static final String privateKeyAlias = "CertificateTest";

  @SuppressWarnings("unused")
  public static void main(String[] args) {
    // Login to the HSM
    // Since we are going to use a KeyStore for this demo
    // we will use that KeyStore to log in to the HSM

    KeyStore myStore = null;
    try {

      /* Note: could also use a keystore file, which contains the token label or slot no. to use. Load that via
       * "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save objects to the keystore via a
       * FileOutputStream. */

      Security.addProvider(new LunaProvider());
      ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot).getBytes());
      myStore = KeyStore.getInstance("Luna");
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

    // List the providers
    // See ProviderList.java for more details
    MiscProviderList.listProviders();

    KeyPairGenerator keyPairgen = null;
    KeyPair RSAKeyPair = null;
    try {
      // Generate the RSA KeyPair
      // Note that while the provider is explicitly specified
      // if you have set the Luna provider as the default
      // then this would not be necessary.
      keyPairgen = KeyPairGenerator.getInstance("RSA", "LunaProvider");
      keyPairgen.initialize(2048);
      RSAKeyPair = keyPairgen.generateKeyPair();
    } catch (Exception e) {
      System.out.println("Exception during KeyPair Generation - " + e.getMessage());
      System.exit(1);
    }

    LunaCertificateX509[] certChain = null;
    try {
      certChain = new LunaCertificateX509[1];
      String subjectname = "CN=some guy, L=around, C=US";
      BigInteger serialNumber = new BigInteger("12345");
      Date notBefore = new Date();
      Date notAfter = new Date(notBefore.getTime() + 1000000000);

      // The LunaCertificateX509 class has a special method that allows
      // you to self-sign a certificate.
      certChain[0] = LunaCertificateX509.SelfSign(RSAKeyPair, subjectname, serialNumber, notBefore, notAfter);
    } catch (Exception e) {
      System.out.println("Exception during Certification Creation - " + e.getMessage());
      System.exit(1);
    }

    // store the certificate and key in the KeyStore.

    try {
      // Save the Certificate to the Luna KeyStore
      System.out.println("Storing Certificate via KeyStore");
      myStore.setKeyEntry(privateKeyAlias, RSAKeyPair.getPrivate(), null, certChain);
      myStore.store(null, null);
    } catch (Exception e) {
      System.out.println("Exception while storing Certificate - " + e.getMessage());
      System.exit(1);
    }


    PrivateKey privateKey = null;
    X509Certificate cert = null;
    PublicKey publicKey = null;
    try {
      // get the private key and Certificate from the Luna KeyStore
      System.out.println("Getting Certificate via KeyStore");
      privateKey=(PrivateKey)myStore.getKey(privateKeyAlias,null);
      cert = (X509Certificate)myStore.getCertificate("CertificateTest");
      publicKey = cert.getPublicKey();
    } catch (Exception e) {
      System.out.println("Exception while getting key and Certificate - " + e.getMessage());
      System.exit(1);
    }

    try {
      // Remove the Certificate from the KeyStore
      // This will delete the key and certificate.
      System.out.println("Removing Certificate via KeyStore");
      myStore.deleteEntry(privateKeyAlias);
    } catch (Exception e) {
      System.out.println("Exception while removing Certificate - " + e.getMessage());
      System.exit(1);
    }

  }
}
