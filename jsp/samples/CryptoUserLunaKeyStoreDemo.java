
// ****************************************************************************
// Copyright (c) 2010 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import com.safenetinc.luna.provider.LunaProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Enumeration;

/**
 * This example illustrates how to use the Luna KeyStore logging in as the Crypto User.
 */
public class CryptoUserLunaKeyStoreDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "cuserpin";

  public static void main(String[] args) {

    KeyStore myStore = null;

    try {
      ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot + "\nusertype:CKU_CRYPTO_USER\n").getBytes());

      Security.addProvider(new LunaProvider());

      // First get the instance of the keystore.
      myStore = KeyStore.getInstance("Luna");

      /* Adding the line "usertype:CKU_CRYPTO_USER" to the input stream will cause the keystore to login to the HSM as a
       * Crypto User. This prevents the application from creating or destroying keys but the Crypto User is able to use
       * the keys for crypto operations. The user is only available when using a PED and the "partition createUser" has
       * already been executed using lush. */
      myStore.load(is1, passwd.toCharArray());

      System.out.println("Logged in as Crypto User!");

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

  }
}
