import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;

import javax.crypto.KeyGenerator;

import com.safenetinc.luna.provider.LunaProvider;

public class LunaPkcs11AttributesConfigDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";
  public static final String provider = "LunaProvider";

  public static void main(String[] args) throws Exception {

    // In this directory there is a file called luna-pkcs11-attributes.conf that serves as
    // an example to globally configure PKCS11 attributes for various key generation mechanisms.
    // Before running this example export the LUNA_PKCS11_ATTRIBUTES_CONFIG environment variable:
    //
    //   export LUNA_PKCS11_ATTRIBUTES_CONFIG=<path to luna-pkcs11-attributes.conf>

    Security.addProvider(new LunaProvider());
    ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot).getBytes());
    KeyStore myStore = KeyStore.getInstance("Luna");
    myStore.load(is1, passwd.toCharArray());

    KeyPairGenerator kpg = null;
    kpg = KeyPairGenerator.getInstance("RSA", provider);
    KeyPair kp = kpg.genKeyPair();

    System.out.println("Generated RSA key.");

    KeyGenerator keyGen = KeyGenerator.getInstance("AES", "LunaProvider");
    Key key = keyGen.generateKey();

    System.out.println("Generated AES key.");

  }

}
