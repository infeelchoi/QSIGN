import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.spec.DSAParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import com.safenetinc.luna.provider.LunaProvider;

public class AlgParmGenDSADemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";
  public static final String provider = "LunaProvider";
  public static final String keystoreProvider = "Luna";

  public static void main(String[] args) {

    KeyStore myStore = null;
    try {

      /* Note: could also use a keystore file, which contains the token label or slot no. to use. Load that via
       * "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save objects to the keystore via a
       * FileOutputStream. */

      Security.addProvider(new LunaProvider());
      ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot).getBytes());
      myStore = KeyStore.getInstance(keystoreProvider);
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

    AlgorithmParameterGenerator dsaGen;
    try {
      dsaGen = AlgorithmParameterGenerator.getInstance("DSA", provider);
      dsaGen.init(1024);
      AlgorithmParameters dsaParam = dsaGen.generateParameters();
      if (dsaParam.getAlgorithm().contentEquals("DSA")) {
        System.out.println("DSA parameter is correct");
      }
      ;
      dsaParam.getParameterSpec(DSAParameterSpec.class); // shouldn't throw an exception
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidParameterSpecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
