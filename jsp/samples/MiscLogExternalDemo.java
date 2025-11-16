import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;

import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.provider.LunaProvider;

public class MiscLogExternalDemo {
  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";
  private static LunaSlotManager manager;

  public static void main(String args[]) {

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

    System.out.println("Please ensure that you have enabled audit logging on the appliance first.");
    try {
      manager = LunaSlotManager.getInstance();
      String yourMsg = "Your message here...";
      System.out.println("Attempting to remotely log \"" + yourMsg + "\"");
      manager.logExternal(slot, yourMsg);
      manager.logExternal(yourMsg);
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("It should have worked...go check log file on HSM...");
  }
}
