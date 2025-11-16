
// ****************************************************************************
// Copyright (c) 2010 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

import com.safenetinc.luna.provider.LunaCertificateX509;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.key.LunaKey;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Date;

public class MiscCertificateDemoLegacy {
  /* This example illustrates - Generate an RSA KeyPair using the default provider - Self-Sign with an RSA key - Store a
   * Certificate - Store Keys For more information regarding Luna KeyStores please see KeystoreLunaDemo.java. */

  /* NOTICE: THIS IS FOR LEGACY PURPOSES ONLY. PLEASE USE THE OTHER METHOD PRESENTED IN CertificateDemo.java ONLY The
   * Luna Java Provider gives you an alternative to using keystores for storing keys and single certificate. There are
   * some special methods in our LunaKey and LunaCertificateX509 classes that give you direct access to storing,
   * retrieving, and removing objects from the HSM. The LunaKey methods also permit you do store public key objects,
   * which KeyStores do not. In this example we will generate an RSA KeyPair, self-sign a certificate, and save the
   * certificate and key pair on the HSM. */
  public static void main(String[] args) {
    // Login to the HSM
    /* See HSM_Manager.java for more details */
    Security.addProvider(new LunaProvider());
    HSM_Manager.hsmLogin();

    // List the providers
    /* See ProviderList.java for more details */
    MiscProviderList.listProviders();

    KeyPairGenerator keyPairgen = null;
    KeyPair RSAKeyPair = null;
    try {
      // Generate the RSA KeyPair
      keyPairgen = KeyPairGenerator.getInstance("RSA", "LunaProvider");
      keyPairgen.initialize(1024);
      RSAKeyPair = keyPairgen.generateKeyPair();
    } catch (Exception e) {
      System.out.println("Exception during KeyPair Generation - " + e.getMessage());
      System.exit(1);
    }

    try {
      // Store the RSA key pair (public key and private key) as permanent
      // objects on the HSM using the special LunaKey method.
      /* Even though the RSA KeyPair created above was created with the Luna Provider, the keys are not permanently
       * stored on the Luna HSM automatically. Keys are made persistent (or in PKCS#11 terms, become token objects
       * instead of session objects) only when they are saved in a KeyStore, or when the special LunaKey
       * MakePersistent() method is invoked. */
      LunaKey lkPublic = (LunaKey) RSAKeyPair.getPublic();
      lkPublic.MakePersistent("Public TestKey");

      LunaKey lkPrivate = (LunaKey) RSAKeyPair.getPrivate();
      lkPrivate.MakePersistent("Private TestKey");

      keyPairgen = KeyPairGenerator.getInstance("RSA", "LunaProvider");
      keyPairgen.initialize(1024);
      RSAKeyPair = keyPairgen.generateKeyPair();
    } catch (Exception e) {
      System.out.println("Exception during KeyPair Storage - " + e.getMessage());
      System.exit(1);
    }

    LunaCertificateX509[] certChain = null;
    try {
      certChain = new LunaCertificateX509[1];
      String subjectname = "CN=some guy, L=around, C=US";
      BigInteger serialNumber = new BigInteger("12345");
      Date notBefore = new Date();
      Date notAfter = new Date(notBefore.getTime() + 1000000000);
      /* The LunaCertificateX509 class has a special method that allows you to self-sign a certificate. */
      certChain[0] = LunaCertificateX509.SelfSign(RSAKeyPair, subjectname, serialNumber, notBefore, notAfter);

    } catch (Exception e) {
      System.out.println("Exception during Certification Creation - " + e.getMessage());
      System.exit(1);
    }

    try {
      // Store the Certificate as a permanent objects on the HSM using
      // the special LunacertificateX509 method.
      /* Even though the certificate created above was signed with a key that is stored in the Luna HSM, the certificate
       * is not permanently stored on the Luna HSM automatically. Certificates are made persistent (or in PKCS#11 terms,
       * become token objects instead of session objects) only when they are saved in a KeyStore, or when the special
       * LunaCertificateX509 MakePersistent() method is invoked. */
      certChain[0].MakePersistent("CertificateTest");
    } catch (Exception e) {
      System.out.println("Exception during Certificate Storage - " + e.getMessage());
      System.exit(1);
    }

    LunaKey PrivateKey = null, PublicKey = null;
    LunaCertificateX509[] cert = null;
    try {
      // Locate the stored Certificate and Public/Private keys
      System.out.println("Retrieving the stored Certificate and Public/Private Keypair");

      PublicKey = LunaKey.LocateKeyByAlias("Public TestKey");
      System.out.println("\t-Public key was stored on - " + PublicKey.GetDateMadePersistent());

      PrivateKey = LunaKey.LocateKeyByAlias("Private TestKey");
      System.out.println("\t-Private key was stored on - " + PrivateKey.GetDateMadePersistent());

      cert = new LunaCertificateX509[1];
      cert[0] = LunaCertificateX509.LocateCertByAlias("CertificateTest");

      // Remove the Private, Public keys and the Certificate from the HSM
      System.out.println("Removing Public,Private keys and Certificate");
      PublicKey.DestroyKey();
      PrivateKey.DestroyKey();
      cert[0].DestroyCert();

    } catch (Exception e) {
      System.out.println("Exception while locating Certificate/Key - " + e.getMessage());
      System.exit(1);
    }

    HSM_Manager.hsmLogout();
  }
}
