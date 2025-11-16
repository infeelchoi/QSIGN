import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.concurrent.atomic.AtomicInteger;

import com.safenetinc.luna.LunaAPI;
import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.provider.LunaProvider;

//HA mode is required for auto reconnect
//With HA enabled, HAOnly enabled is the strongly recommended HA configuration
public class MiscReconnectDemo {

  // Configure these as required.
  private static int slot = 0;
  private static final String label = "ha1";
  private static final String passwd = "userpin";
  private static final MiscReconnectDemo aMrcd = new MiscReconnectDemo();

  // FOR RECONNECT
  private static final LunaSlotManager slotManager = LunaSlotManager.getInstance();
  public static AtomicInteger threadsOnTheFlyCounter = new AtomicInteger(0);

  KeyStore myStore = null;
  private static ByteArrayInputStream is1 = null;

  private final int maxCtr = 100;

  private void doConnect() {
    try {

      /**
       * Note: could also use a keystore file, which contains the token label or slot no. to use. Load that via
       * "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save objects to the keystore via a
       * FileOutputStream.
       **/

      Security.addProvider(new LunaProvider());
      myStore = KeyStore.getInstance("Luna");
      System.out.println("...logging in to Luna HSM...");
      is1 = new ByteArrayInputStream(("tokenlabel:" + label).getBytes());
      myStore.load(is1, passwd.toCharArray());
    } catch (KeyStoreException kse) {
      System.out.println("Unable to create keystore object");
    } catch (NoSuchAlgorithmException nsae) {
      System.out.println("Unexpected NoSuchAlgorithmException while loading keystore");
    } catch (CertificateException e) {
      System.out.println("Unexpected CertificateException while loading keystore");
    } catch (IOException e) {
      // this should never happen
      System.out.println("Unexpected IOException while loading keystore.");
      e.printStackTrace();
    } catch (Exception e) {
      // this should never happen
      System.out.println("Unexpected Exception while loading keystore.");
      e.printStackTrace();
    }
  }

  private void doRun() {
    KeyPairGenerator keyPairgen = null;
    KeyPair RSAKeyPair = null;
    for (int ctr = 1; ctr <= maxCtr; ctr++) {
      System.out.printf("Attempt #%d to perform crypto operation...\n", ctr);

      // FOR RECONNECT
      threadsOnTheFlyCounter.incrementAndGet();

      try {
        // Generate the RSA KeyPair
        keyPairgen = KeyPairGenerator.getInstance("RSA", "LunaProvider");
        keyPairgen.initialize(1024);
        RSAKeyPair = keyPairgen.generateKeyPair();
      } catch (Exception e) {
        System.out.println("Exception during KeyPair Generation - " + e.getMessage());
      } finally {
        // FOR RECONNECT
        threadsOnTheFlyCounter.decrementAndGet();
      }

      try {
        if (maxCtr - ctr > 0) {
          /**
           * This is just a little delay to allow time to *create* or *repair* a network
           * problem for demo purposes...you can use the local firewall to block/unblock
           * HSM IP traffic for example
           */
          System.out.printf("sleeping for 10s...\n");
          Thread.sleep(10000);
        }
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      // FOR RECONNECT
      detectAndCorrect();
    }
  }

  // FOR RECONNECT
  private void detectAndCorrect() {
    System.out.printf("do we need to attempt to reconnect to the HSM?...\n");
    try {
      slot = slotManager.findSlotFromLabel(label);
      slotManager.detectTokenConnectionProblem(slot);
    } catch (Exception e) {
      System.out.printf("Expected exception \"%s\" while checking for token presence.", e.getMessage());
    }

    // detect a connection failure
    if ( slotManager.getReconnectRequired() ) {
      System.out.println("YES");
      try {
        /**
         * 1) Determine if reconnect is possible first...i.e. drain all tasks from their respective queues and let all
         * in-flight tasks complete as best they can (most likely will fail if connection failed) then, and *only* then,
         * attempt a reconnect. 2) If a reconnect is attempted with tasks left on the fly or queued to soon be on the
         * fly then the ensuing native crypto library calls could fail causing a core dump of the JVM due to the
         * temporarily unstable state of the library caused by the reconnect. 3) A reconnect attempt might be needed
         * multiple times depending upon when the network layer issue is finally repaired.
         */
        if (threadsOnTheFlyCounter.get() == 0) {
          slotManager.reinitialize();
          // ensure you login to all slots that you manage
          aMrcd.doConnect();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      System.out.println("NO");
    }

    try {
      if (!slotManager.isLoggedIn()) {
        // ensure you login to all slots that you manage
        aMrcd.doConnect();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public static void main(String[] args) throws Exception {
    aMrcd.doConnect();
    aMrcd.doRun();
  }
}
