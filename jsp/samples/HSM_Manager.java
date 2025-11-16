// ****************************************************************************
// Copyright (c) 2010 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

//imports
import com.safenetinc.luna.LunaSlotManager;

/**
 * This is a simple example of logging in to the HSM. This example does the most basic of logins, using the first
 * available slot and a hard-coded password.
 * </p>
 * <p>
 * Please see KeyStoreLunaDemo.java for other methods of logging in to the HSM through Java which are more consistent
 * with the SUN JCA/JCE specification.
 * </p>
 * <p>
 * Alternatively see the Luna developers reference guide for information on using an application ID which has been
 * opened by another process.
 * </p>
 */
class HSM_Manager {
  public static LunaSlotManager slotManager = null;

  // Configure as required.
  private static final String passwd = "userpin";

  public static LunaSlotManager getSlotManager() {
    return slotManager;
  }

  /**
   * Performs a login operation using the LunaSlotManager. The login operation occurs on the first available slot
   */
  public static void hsmLogin() {
    String tokenlabel;

    // Initialize the SlotManager class
    slotManager = LunaSlotManager.getInstance();

    /* Since your system may have more than one Luna SA partition or Luna HSM, it may be important for your application
     * to see which partitions or HSMs are available and to choose (or allow a user to choose) one with which to perform
     * cryptographic calls. Luna SA partitions and HSMs are based on PKCS#11 (cryptoki) "slots" and "tokens". PKCS#11
     * uses a slot/token relationship to represent a standardized abstraction layer in software or hardware security
     * devices. A device may have many slots and each slot may or may not have a token present in it. Luna SA partitions
     * are represented as slots with a token present. Luna HSMs come in card readers with a set number of slots. Tokens
     * will only appear present in the software if they are physically present in the slot of the card reader. For more
     * information about the Slot/Token relationship please see the PKCS#11 v2.01 specification from http://www.rsa.com */

    int[] activeSlots = slotManager.getSlotList();
    System.out.println("Number of slots: " + activeSlots.length);

    for (int slot : activeSlots) {
      try {
        // Since it is possible to have a slot without a token present
        // check to see if there is a token present
        if (slotManager.isTokenPresent(slot)) {
          tokenlabel = slotManager.getTokenLabel(slot);

          // Each Luna SA partiton or HSM has a label that is created
          // during setup of the HSM. Labels are commonly used to
          // distinguish one partition or HSM from another.
          System.out.println("Slot: " + slot + " token label: " + tokenlabel);
        }
        System.out.println();
      } catch (Exception e) {
        // May just be a device error on a slot.. continue connecting to each slot.
        System.out.println("Exception - " + e.getMessage());
        // System.exit(1);
      }
    }

    try {
      // Login to the HSM
      /* This method unlocks the token for use. There are multiple methods available within the LunaSlotManager class to
       * login to the HSM: Login to the first available partition: login(String password) Login to the partition at the
       * specified slot: login(int slot, String password) Login to the partition with the specified label: login(String
       * TokenLabel, String password) Login to the partition with the specified user role: login(int slot, UserRole
       * userType, String password) The password argument is the challenge password for the Luna SA partition or HSM.
       * (Applications generally ask for password information interactively from the user.) */
      slotManager.login(passwd);

    } catch (Exception e) {
      System.out.println("Exception during login: " + e.getMessage());
    }
  }

  /**
   * Shows the total number of objects stored on the slot.
   */
  public static void showObjectCount() {
    try {
      System.out.println("Number of objects currently stored on HSM slot " + slotManager.getDefaultSlot() + ": "
          + slotManager.getCurrentObjectCount());

    } catch (Exception e) {
      System.out.println("Exception while getting HSM object status: " + e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Logs out of the default session
   */
  public static void hsmLogout() {
    /* When you are done using the Luna HSM, it is customary to log out of the HSM to prevent unauthorized access at a
     * later point in your application. Only use the LunaSlotManager.logout() method if you used one of the
     * LunaSlotManager.login() methods for opening access to the HSM. If you use an external login method, you will need
     * to use an external logout method. */
    slotManager.logout();
  }
}
