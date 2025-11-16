
/**
 * Copyright (c) 2017 Gemalto NV, All rights reserved.
 *
 * All rights reserved.  This file contains information that is
 * proprietary to Gemalto and may not be distributed
 * or copied without written consent from Gemalto.
 */

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.safenetinc.luna.provider.LunaCertificateX509;
import com.safenetinc.luna.provider.key.LunaKey;
import com.safenetinc.luna.provider.key.LunaSIMKey;

/**
 * This sample demonstrates performing SIM Extract and SIM Insert allowing keys to be extracted from
 * a Luna SA and inserted into it later on.
 */

public class SIMExtractInsert {

  // ******* IMPORTANT *******
  // the authentication form (or mode) is critical:
  // (Both source and destination HSMs require the use of the same
  // authentication form/mode.)
  // mode = 1 implies NONPORTABLE,password - This means that the SIM master
  // password was cloned and that a SIM blob password was provided
  // at SIM extract time
  // mode = 5 implies PORTABLE,password - This means that the SIM master
  // password was derived from the legacy domain key which is the
  // same on both HSMs and that a SIM blob password was provided
  // at SIM extract time
  // The SIM blob password (m of n => 1 of 1) in this code is "SIMBlobPass"
  // below and you need to record whatever this was entered upon the SIM
  // extract phase of this migration.
  static long authenticationForm = 1;
  static int slot = 3;
  static String hsmPass = "userpin";

  static String RSA_ALIAS = "my-rsa";
  static String AES_ALIAS = "my-aes";

  static Provider lunaProvider = null;

  public static KeyPair generateRSA() {
    KeyPairGenerator keyPairgen = null;
    KeyPair rsaKeyPair = null;
    try {
      keyPairgen = KeyPairGenerator.getInstance("RSA", "LunaProvider");
      keyPairgen.initialize(2048);
      rsaKeyPair = keyPairgen.generateKeyPair();
    } catch (Exception e) {
      System.out.println("Exception during KeyPair Generation - " + e.getMessage());
      System.exit(1);
    }

    return rsaKeyPair;
  }

  public static SecretKey generateAES() {
    KeyGenerator keyGen = null;
    SecretKey key = null;
    try {
      keyGen = KeyGenerator.getInstance("AES", "LunaProvider");
      keyGen.init(128);
      key = keyGen.generateKey();
    } catch (Exception e) {
      System.out.println("Exception during KeyPair Generation - " + e.getMessage());
      System.exit(1);
    }

    return key;
  }

  public static LunaCertificateX509[] getCertficate(KeyPair rsaKeyPair) {

    LunaCertificateX509[] certChain = null;
    try {
      certChain = new LunaCertificateX509[1];
      String subjectname = "CN=some guy, L=around, C=US";
      BigInteger serialNumber = new BigInteger("12345");
      Date notBefore = new Date();
      Date notAfter = new Date(notBefore.getTime() + 1000000000);

      // The LunaCertificateX509 class has a special method that allows
      // you to self-sign a certificate.
      certChain[0] =
          LunaCertificateX509.SelfSign(rsaKeyPair, subjectname, serialNumber, notBefore, notAfter);
    } catch (Exception e) {
      System.out.println("Exception during Certification Creation - " + e.getMessage());
      System.exit(1);
    }
    return certChain;
  }

  public static void listKeys(KeyStore keyStore, String message) throws KeyStoreException {
    System.out.println(message);
    System.out.println("Aliases BEGIN ================");
    for (Enumeration<String> aliasEnum = keyStore.aliases(); aliasEnum.hasMoreElements();)
      System.out.println(aliasEnum.nextElement());
    System.out.println("Aliases END ==================\n");
  }

  public static void main(String[] args) throws Exception {
    try {
      // add provider
      Class<?> providerClass = Class.forName("com.safenetinc.luna.provider.LunaProvider");
      lunaProvider = (Provider) providerClass.newInstance();
      Security.addProvider(lunaProvider);

      KeyStore keyStore = KeyStore.getInstance("Luna", lunaProvider);
      keyStore.load(new ByteArrayInputStream(("slot:" + slot).getBytes()), (hsmPass).toCharArray());

      SecretKey aesKey = generateAES();

      KeyPair keyPair = generateRSA();
      LunaCertificateX509[] certChain = getCertficate(keyPair);

      byte[] cert = certChain[0].getEncoded();

      keyStore.setKeyEntry(RSA_ALIAS, keyPair.getPrivate(), null, certChain);
      keyStore.setKeyEntry(AES_ALIAS, aesKey, null, null);

      System.out.println("KeyStore=" + keyStore.getClass().getName());

      System.out.print("\n\n");

      listKeys(keyStore, "List of \"unique\" objects in HSM...before extract");

      ArrayList<String> passwords = new ArrayList<>();
      passwords.add("password1");
      passwords.add("password2");

      ArrayList<String> insertPasswords = new ArrayList<>();
      insertPasswords.add("password1");
      insertPasswords.add("password2");

      byte[] simBlob =
          LunaSIMKey.SIMExtract(new LunaKey[] { (LunaKey) keyPair.getPrivate(), (LunaKey) aesKey },
              2, 2, authenticationForm, passwords);

      keyStore.deleteEntry(RSA_ALIAS);
      keyStore.deleteEntry(AES_ALIAS);

      listKeys(keyStore, "List of \"unique\" objects in HSM...after extract and deleteEntry");

      LunaSIMKey.SIMInsert(authenticationForm, insertPasswords, // <= this is where SIM blob
                                                                // password is needed for SIM insert
          simBlob);

      Key privateKey = keyStore.getKey(RSA_ALIAS, null);
      certChain[0] = new LunaCertificateX509(cert);
      keyStore.setKeyEntry(RSA_ALIAS, privateKey, null, certChain);

      System.out.print("SIM inserted...\n\n");

      listKeys(keyStore, "List of \"unique\" objects in HSM...after insert");

      keyStore.deleteEntry(RSA_ALIAS);
      keyStore.deleteEntry(AES_ALIAS);

      listKeys(keyStore, "List of \"unique\" objects in HSM...after deleteEntry");
    } catch (Exception e) {
      System.out.println("Exception occured: " + e.getMessage());
    } finally {
      System.out.println("Done...");
    }
  }

}
