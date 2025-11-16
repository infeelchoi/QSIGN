import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;

import javax.crypto.KeyGenerator;

import com.safenetinc.luna.LunaAPI;
import com.safenetinc.luna.attributes.LunaPkcs11Attributes;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.param.LunaKeySizeParameterSpec;
import com.safenetinc.luna.provider.param.LunaPkcs11AttributesParameterSpec;

public class LunaPkcs11AttributesDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";
  public static final String provider = "LunaProvider";

  public static void main(String[] args) throws Exception {

    Security.addProvider(new LunaProvider());
    ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot).getBytes());
    KeyStore myStore = KeyStore.getInstance("Luna");
    myStore.load(is1, passwd.toCharArray());

    //create a LunaPkcs11Attributes instance and set PKCS11 attributes.
    LunaPkcs11Attributes attributes = new LunaPkcs11Attributes();
    attributes.setBooleanAttribute(LunaPkcs11Attributes.PUBLIC, LunaAPI.CKA_ENCRYPT, false);
    attributes.setBooleanAttribute(LunaPkcs11Attributes.PUBLIC, LunaAPI.CKA_WRAP, false);
    attributes.setLongAttribute(LunaPkcs11Attributes.PUBLIC, LunaAPI.CKA_KEY_TYPE, LunaAPI.CKK_RSA);
    attributes.setByteArrayAttribute(LunaPkcs11Attributes.PUBLIC, LunaAPI.CKA_LABEL, "My Public Key".getBytes());

    attributes.setBooleanAttribute(LunaPkcs11Attributes.PRIVATE, LunaAPI.CKA_DECRYPT, false);
    attributes.setBooleanAttribute(LunaPkcs11Attributes.PRIVATE, LunaAPI.CKA_UNWRAP, false);
    attributes.setLongAttribute(LunaPkcs11Attributes.PRIVATE, LunaAPI.CKA_KEY_TYPE, LunaAPI.CKK_RSA);
    attributes.setByteArrayAttribute(LunaPkcs11Attributes.PRIVATE, LunaAPI.CKA_LABEL, "My Private Key".getBytes());

    LunaKeySizeParameterSpec keySizeParams = new LunaKeySizeParameterSpec(2048);
    /*The second parameter to the LunaPkcs11AttributesParameterSpec constructor takes an AlgorithmParameterSpec.
    This AlgorithmParameterSpec is wrapped by the LunaPkcs11AttributesParameterSpec. The PCKS11 attributes are passed
    to the native layer and parameter spec is used as if the parameter spec were passed directly to the KeyPairGenerator
    initialize method.*/
    LunaPkcs11AttributesParameterSpec spec = new LunaPkcs11AttributesParameterSpec(attributes, keySizeParams);

    KeyPairGenerator kpg = null;
    kpg = KeyPairGenerator.getInstance("RSA", provider);
    kpg.initialize(spec);
    KeyPair kp = kpg.genKeyPair();

    System.out.println("Generated RSA key.");

    LunaPkcs11Attributes secretAttributes = new LunaPkcs11Attributes();
    secretAttributes.setBooleanAttribute(LunaPkcs11Attributes.SECRET, LunaAPI.CKA_ENCRYPT, true);
    secretAttributes.setBooleanAttribute(LunaPkcs11Attributes.SECRET, LunaAPI.CKA_DECRYPT, false);
    secretAttributes.setBooleanAttribute(LunaPkcs11Attributes.SECRET, LunaAPI.CKA_VERIFY, false);
    secretAttributes.setBooleanAttribute(LunaPkcs11Attributes.SECRET, LunaAPI.CKA_SIGN, false);
    secretAttributes.setBooleanAttribute(LunaPkcs11Attributes.SECRET, LunaAPI.CKA_WRAP, false);
    secretAttributes.setBooleanAttribute(LunaPkcs11Attributes.SECRET, LunaAPI.CKA_UNWRAP, false);
    secretAttributes.setBooleanAttribute(LunaPkcs11Attributes.SECRET, LunaAPI.CKA_EXTRACTABLE, true);
    secretAttributes.setBooleanAttribute(LunaPkcs11Attributes.SECRET, LunaAPI.CKA_DERIVE, false);
    secretAttributes.setBooleanAttribute(LunaPkcs11Attributes.SECRET, LunaAPI.CKA_MODIFIABLE, false);
    secretAttributes.setLongAttribute(LunaPkcs11Attributes.SECRET, LunaAPI.CKA_VALUE_LEN, 32);
    secretAttributes.setByteArrayAttribute(LunaPkcs11Attributes.SECRET, LunaAPI.CKA_LABEL, "My AES Key".getBytes());
    LunaPkcs11AttributesParameterSpec secretSpec = new LunaPkcs11AttributesParameterSpec(secretAttributes, null);

    KeyGenerator keyGen = KeyGenerator.getInstance("AES", "LunaProvider");
    keyGen.init(secretSpec);
    Key key = keyGen.generateKey();

    System.out.println("Generated AES key.");

  }

}
