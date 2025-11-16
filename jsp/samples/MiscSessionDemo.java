
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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.safenetinc.luna.LunaSession;
import com.safenetinc.luna.LunaSessionManager;
import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.exception.LunaException;
import com.safenetinc.luna.provider.LunaProvider;

/*
 * Session Test Demo
 * This sample demonstrates how to monitor session usage
 */
public class MiscSessionDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";

  public static void main(String args[]) {
    // LunaSlotManager manager = LunaSlotManager.getInstance();
    // int slot = -1;
    //
    // try {
    // manager.login(passwd); // log in to the first slot
    // slot = manager.getDefaultSlot(); // we'll need the slot number later
    //
    // } catch (Exception e) {
    // System.out.println("Exception during login");
    // System.exit(-1);
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

    // check for pre-existing sessions. this includes special sessions used internally
    // by the provider. the user can't clear those ones without forcing all sessions to
    // close (but this is extremely destructive -- see below)
    System.out.println(LunaSessionManager.getSessionCount() + " pre-existing sessions.\n");

    // make a bunch of new sessions and see how many sessions are managed/cached
    System.out.println("Opening 100 sessions.  They will all remain active.");
    LunaSession sessions[] = new LunaSession[100];
    for (int i = 0; i < 100; i++) {
      sessions[i] = LunaSessionManager.getSession();
    }
    int activeCount = LunaSessionManager.getActiveSessionCount();
    int cacheCount = LunaSessionManager.getCachedSessionCount();
    System.out.println("There are " + activeCount + " active sessions and " + cacheCount + " cached sessions\n");

    // close half the sessions and see how many sessions are managed / cached
    System.out.println("Freeing 50 sessions");
    for (int i = 50; i < 100; i++) {
      sessions[i].Free();
    }
    activeCount = LunaSessionManager.getActiveSessionCount();
    cacheCount = LunaSessionManager.getCachedSessionCount();
    System.out.println("There are " + activeCount + " active sessions and " + cacheCount + " cached sessions\n");

    // clear the cached sessions and see how many sessions are managed/cached
    System.out.println("Forcing cache to clear.");
    LunaSessionManager.closeCachedSessions();
    activeCount = LunaSessionManager.getActiveSessionCount();
    cacheCount = LunaSessionManager.getCachedSessionCount();
    System.out.println("There are " + activeCount + " active sessions and " + cacheCount + " cached sessions\n");

    // clear 20 more sessions, then force clear the rest
    System.out.println("Freeing 20 sessions.");
    for (int i = 0; i < 20; i++) {
      sessions[i].Free();
    }
    activeCount = LunaSessionManager.getActiveSessionCount();
    cacheCount = LunaSessionManager.getCachedSessionCount();
    System.out.println("There are " + activeCount + " active sessions and " + cacheCount + " cached sessions\n");

    System.out.println("Closing all sessions.");
    // if you do this without specifying the slot number it will close all sessions on all slots.
    LunaSessionManager.closeAllSessions(slot);

    activeCount = LunaSessionManager.getActiveSessionCount();
    cacheCount = LunaSessionManager.getCachedSessionCount();
    System.out.println("There are " + activeCount + " active sessions and " + cacheCount + " cached sessions\n");

    // all active sessions were closed, but the provider keeps one special session open
    // per slot to maintain login state. verify this by generating a key.

    KeyGenerator kg = null;
    SecretKey key = null;
    try {
      // create an AES key
      kg = KeyGenerator.getInstance("AES", "LunaProvider");
      kg.init(256);
      key = kg.generateKey();
      System.out.println("Successfully generated an AES key.");

    } catch (Exception e) {
      System.out.println("Could not generate an AES key");
      e.printStackTrace();
    }

    // show the session numbers again
    activeCount = LunaSessionManager.getActiveSessionCount();
    cacheCount = LunaSessionManager.getCachedSessionCount();
    System.out.println("There are " + activeCount + " active sessions and " + cacheCount + " cached sessions\n");

    // demonstrating the potential destructiveness of session operations: start an
    // encryption operation and close all open sessions before finishing.
    byte[] bytes = "Encrypt Me!".getBytes();

    try {
      System.out.println("Starting an encryption operation.");
      Cipher myCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "LunaProvider");
      byte[] ivBytes = { (byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72,
          (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80 };
      AlgorithmParameters mAlgParams = AlgorithmParameters.getInstance("IV");
      mAlgParams.init(new IvParameterSpec(ivBytes));
      myCipher.init(Cipher.ENCRYPT_MODE, key, mAlgParams);

      System.out.println("Clearing all managed sessions");
      // if you do this without specifying the slot number it will close all sessions on all slots.
      LunaSessionManager.closeAllSessions(LunaSlotManager.getInstance().getDefaultSlot());
      System.out.println("Finishing encryption operation.");
      myCipher.doFinal(bytes);

    } catch (LunaException e) {
      System.out.println("\nCaught LunaException (Session logged out).\n"
          + "This is expected behaviour for this test but in general is not desirable.\n"
          + "Calls to closeAllSessions are done at your own risk!\n");

    } catch (Exception e) {
      System.out.println("Caught unexpected exception.");
      e.printStackTrace();
    }

    // show all the sessions open against the hsm. this
    // will reveal the hidden master session used to maintain the login
    // state as well as any sessions that are open by other applications.
    int total = LunaSessionManager.getSessionCount();
    System.out.println("The total of all sessions open on the slot by all processes: " + total);

    // clear all the sessions opened against the hsm by this application ID. this will
    // include any sessions opened internally by the provider, so unexpected behaviour
    // may result. be very careful with this one.
    System.out.println("Forcing all sessions on the HSM to close");
    // if you don't specify the slot number it will force all sessions on all slots to close.
    LunaSessionManager.forceAllHSMSessionsToClose(LunaSlotManager.getInstance().getDefaultSlot());

    // show the session count in LunaSessionManager. you need to specify a slot, since
    // forceAllHSMSessionsToClose() resets the default slot
    total = LunaSessionManager.getSessionCount();
    System.out.println("There are now " + total + " sessions open.\n");
    if (total > 0) {
      System.out.println("If there are still sessions open, these leftover sessions "
          + "may have been created by another application id");
    }
  }
}
