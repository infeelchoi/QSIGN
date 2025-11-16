import java.security.Key;
import java.security.Security;
import java.util.ArrayList;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.key.LunaKey;
import com.safenetinc.luna.provider.param.LunaGcmParameterSpec;

/**
 * This class demonstrates performance of AES/GCM in our hardware The default assumes a 12 byte IV and a Tag length
 * which does not extend beyond the data.
 *
 * @author mgardiner
 *
 */
public class PerformanceUnwrapDemo implements Runnable {

  public static int threadCount = 20;
  // change this to suit the length of data needed
  public static int dataSize = 16;
  public static byte[] data;

  public static byte[] ivBytes16 = { (byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71,
      (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80 };

  public static byte[] ivBytes12 = { (byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71,
      (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, };

  public static byte[] wrappedKey;

  public static Key aesKey;

  /**
   * @param args
   */
  public static void main(String[] args) {
    try {

      // log in to token
      Security.addProvider(new LunaProvider());
      com.safenetinc.luna.LunaSlotManager mgr = com.safenetinc.luna.LunaSlotManager.getInstance();
      mgr.login(0, "userpin");
      mgr.setSecretKeysExtractable(true); // set this to false and you can't wrap any created key...

      // generate our wrapping key
      KeyGenerator kg = KeyGenerator.getInstance("AES", "LunaProvider");
      kg.init(256);
      aesKey = kg.generateKey();

      // wrap a new AES key off
      Cipher c = Cipher.getInstance("AES/CBC/NoPadding", "LunaProvider"); // padding not needed since it's going to be
                                                                          // block aligned
      c.init(Cipher.WRAP_MODE, aesKey, new IvParameterSpec(ivBytes16));
      wrappedKey = c.wrap(kg.generateKey());

      // just to show that AES GCM simply doesn't work for wrapping:
      try {
        c = Cipher.getInstance("AES/GCM/NoPadding", "LunaProvider");
        c.init(Cipher.WRAP_MODE, aesKey, new LunaGcmParameterSpec(ivBytes12, new byte[0], 128)); // 12 byte IV, empty
                                                                                                 // AAD and 128 bit tag
        byte[] gcmWrappedKey = c.wrap(kg.generateKey());
      } catch (UnsupportedOperationException e) {
        System.out.println(
            "got expected error: AES GCM wrapping failed " + "due to unsupported operation exeption...proceeding..");
      } catch (Exception e) {
        System.out.println("Some unexpected error occured doing AES GCM wrap operation.  " + e.getMessage());
        e.printStackTrace();
      }

    } catch (Exception e) {
      // cancel the test
      e.printStackTrace();
      System.exit(-1);
    }

    ArrayList<Thread> threads = new ArrayList<Thread>();
    // make our threads
    for (int i = 0; i < threadCount; i++) {
      threads.add(new Thread(new PerformanceUnwrapDemo()));
    }

    // start the threads
    for (int i = 0; i < threadCount; i++) {
      threads.get(i).start();
    }

    // now monitor it
    PerformanceMonitor.StartMonitor(threadCount);

  }

  public PerformanceUnwrapDemo() {
    // nothing to do here
  }

  @Override
  public void run() {

    try {
      IvParameterSpec iv = new IvParameterSpec(ivBytes16);
      // each thread will make its own Cipher object for unwrapping
      Cipher unwrap = Cipher.getInstance("AES/CBC/NoPadding", "LunaProvider");
      unwrap.init(Cipher.UNWRAP_MODE, aesKey, iv);

      long start = System.nanoTime();

      while (true) /* for(int i = 0; i < iterationCount; i++) */ {

        start = System.nanoTime();

        // unwrap the key
        SecretKey key = (SecretKey) unwrap.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);

        long duration = (System.nanoTime() - start) / 1000000;
        PerformanceMonitor.addRecord((int) duration);

        // destroy the key outside the timed area
        ((LunaKey) key).DestroyKey();
      }

    } catch (Exception e) {
      System.out.println("Thread died with exception " + e.getMessage());
    }
  }

}
