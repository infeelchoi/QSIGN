
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.safenetinc.luna.LunaAPI;
import com.safenetinc.luna.LunaCKAttribute;
import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.LunaTokenObject;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.param.LunaPRFKDFParameterSpec;

public class KeyAgreementPRFKDFDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";
  public static final String provider = "LunaProvider";
  public static final String keystoreProvider = "Luna";

  private void testMechanismPerf() {
    // create a secret key
    LunaCKAttribute[] derive_template = { new LunaCKAttribute(LunaAPI.CKA_EXTRACTABLE, true),
        new LunaCKAttribute(LunaAPI.CKA_ENCRYPT, true), new LunaCKAttribute(LunaAPI.CKA_DECRYPT, true),
        new LunaCKAttribute(LunaAPI.CKA_TOKEN, false), new LunaCKAttribute(LunaAPI.CKA_SENSITIVE, true) };
    LunaTokenObject.setDeriveTemplate(derive_template);
    KeyGenerator kg = null;
    for (int i = 0; i < 1000000; i++) {
      try {
        kg = KeyGenerator.getInstance("AES", provider);
        SecretKey secretKey = kg.generateKey();
        // ((LunaKey)secretKey).DestroyKey();
      } catch (NoSuchAlgorithmException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (NoSuchProviderException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    try {
      Thread.sleep(1000000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void testBasicMechanism() {

    try {
      LunaSlotManager.getInstance().setSecretKeysExtractable(true);
      LunaSlotManager.getInstance().setSecretKeysDerivable( true );

      // create a secret key
      LunaCKAttribute[] derive_template = { new LunaCKAttribute(LunaAPI.CKA_EXTRACTABLE, true),
          new LunaCKAttribute(LunaAPI.CKA_ENCRYPT, true), new LunaCKAttribute(LunaAPI.CKA_DECRYPT, true),
          new LunaCKAttribute(LunaAPI.CKA_TOKEN, false), new LunaCKAttribute(LunaAPI.CKA_SENSITIVE, true) };
      LunaTokenObject.setDeriveTemplate(derive_template);
      KeyGenerator kg = KeyGenerator.getInstance("AES", provider);
      SecretKey secretKey = kg.generateKey();

      KeyAgreement ka1 = KeyAgreement.getInstance("CKKeyDerivationPRFKDF", provider);
      KeyAgreement ka2 = KeyAgreement.getInstance("CKKeyDerivationPRFKDF", provider);

      byte[] label = { 9, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
//      byte[] label = null;
      byte[] context = { 9, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
//      byte[] context = null;
      long prfType = LunaAPI.CK_NIST_PRF_KDF_AES_CMAC;
      long counter = 0;
      long encodingScheme = LunaAPI.LUNA_PRF_KDF_ENCODING_SCHEME_1;
      LunaPRFKDFParameterSpec spec = new LunaPRFKDFParameterSpec(label, context, counter, prfType, encodingScheme);

      ka1.init(secretKey, spec);
      ka1.doPhase(null, true);
//      byte[] secret1 = ka1.generateSecret();
      SecretKey secret1 = ka1.generateSecret("AES");

      ka2.init(secretKey, spec);
      ka2.doPhase(null, true);
//      byte[] secret2 = ka2.generateSecret();
      SecretKey secret2 = ka2.generateSecret("AES");

//      assertThat(secret1, equalTo(secret2));

//      System.out.println("secret1: " + Arrays.toString(secret1));
//      System.out.println("secret2: " + Arrays.toString(secret2));
      System.out.println("secret1: " + Arrays.toString(secret1.getEncoded()));
      System.out.println("secret2: " + Arrays.toString(secret2.getEncoded()));

      if (Arrays.equals(secret1.getEncoded(), secret2.getEncoded())) {
        System.out.println("Key agreement derivation of symmetric key worked.");
      } else {
        System.out.println("Key agreement derivation of symmetric key failed.");
      }

      // try the new key
      Cipher encCipher = null;
      Cipher decCipher = null;
      byte[] iv = null;
      byte[] clearText = { 0x01, 0x02 };
//      byte[] blockSizeDataClrTxt = new byte[1024];

      AlgorithmParameters lunaParams = null;
      encCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "LunaProvider");
      decCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "LunaProvider");

      iv = encCipher.getIV();
      if (iv == null) {
        // is AES ok for any secret key?
        lunaParams = AlgorithmParameters.getInstance("AES", provider);
        IvParameterSpec IV16 = new IvParameterSpec(new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
            0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x10, 0x11 });
        lunaParams.init(IV16);
      }
      AlgorithmParameters dummyParams = null;
//      encCipher.init(Cipher.ENCRYPT_MODE, key, dummyParams);
      encCipher.init(Cipher.ENCRYPT_MODE, secret1, lunaParams);
      IvParameterSpec ivps = new IvParameterSpec(encCipher.getIV());
//      decCipher.init(Cipher.DECRYPT_MODE, key, ivps);
      decCipher.init(Cipher.DECRYPT_MODE, secret2, lunaParams);

      System.out.println("Encrypting PlainText");
      byte[] encryptedbytes = null;
      encryptedbytes = encCipher.doFinal(clearText);

      System.out.println("Decrypting to PlainText");
      byte[] decryptedbytes = null;
      decryptedbytes = decCipher.doFinal(encryptedbytes);

      System.out.println("clearText: " + Arrays.toString(clearText));
      System.out.println("encryptedbytes: " + Arrays.toString(encryptedbytes));
      System.out.println("decryptedbytes: " + Arrays.toString(decryptedbytes));
      if (Arrays.equals(clearText, decryptedbytes)) {
        System.out.println("Derived symmetric key worked.");
        // make persistent if you so desire
//        LunaKey lkSecret1 = (LunaKey)secret1;
//        lkSecret1.MakePersistent("secret1");
//        LunaKey lkSecret2 = (LunaKey)secret2;
//        lkSecret2.MakePersistent("secret2");
//        LunaKey lkSecretKey = (LunaKey)secretKey;
//        lkSecretKey.MakePersistent("secretKey");
      } else {
        System.out.println("Derived symmetric key failed.");
      }

    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

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

    KeyAgreementPRFKDFDemo aDemo = new KeyAgreementPRFKDFDemo();
    aDemo.testBasicMechanism();
//    aDemo.testMechanismPerf();
  }

}
