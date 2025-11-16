
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.spec.IvParameterSpec;

import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.param.LunaECIESExt2ParameterSpec;
import com.safenetinc.luna.provider.param.LunaECIESExtParameterSpec;
import com.safenetinc.luna.provider.param.LunaECIESParameterSpec;
import com.safenetinc.luna.provider.param.LunaGcmParameterSpec;

public class CipherECIESDemo {

  // Configure these as required.
  private static final int slot = 3;
  private static final String passwd = "userpin1";
  public static final String provider = "LunaProvider";
  public static final String keystoreProvider = "Luna";
  public static SealedObject so = null;

  public static void areArraysEqual(byte[] expected, byte[] actual) {
    if (expected == null && actual == null) {
        return;
    }
    if (expected == null || actual == null) {
        throw new AssertionError("One of the arrays is null");
    }
    if (expected.length != actual.length) {
        throw new AssertionError("Array lengths differ: expected length " + expected.length + ", but got " + actual.length);
    }
    for (int i = 0; i < expected.length; i++) {
        if (expected[i] != actual[i]) {
            throw new AssertionError("Arrays differ at index " + i + ": expected " + expected[i] + ", but got " + actual[i]);
        }
    }
  }

  public static void main(String[] args) {

    System.out.println("This sample requires an ECIES-capable HSM...");
    System.out.println("");

    KeyStore myStore = null;
    try {

      /* Note: could also use a keystore file, which contains the token label or slot no. to use. Load that via
       * "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save objects to the keystore via a
       * FileOutputStream. */

      String lunaKeyStoreContent = "slot:" + slot;
      Security.addProvider(new LunaProvider());
      ByteArrayInputStream is1 = new ByteArrayInputStream(lunaKeyStoreContent.getBytes());
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

    try {

      KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDSA", "LunaProvider");
      ECGenParameterSpec ecParam = new ECGenParameterSpec("c2pnb304w1");
      kpg.initialize(ecParam);

      KeyPair kp = kpg.generateKeyPair();
      byte[] orig = new byte[1 * 1024];
      Random r = new Random();
      r.nextBytes(orig);

      Cipher cipher = Cipher.getInstance("ECIES", provider);
      AlgorithmParameterSpec paramSpec;

      //ECIES
      paramSpec = new LunaECIESParameterSpec(LunaECIESParameterSpec.DH_PRIMITIVE.ECDHC,
          LunaECIESParameterSpec.KDF.SHA256, LunaECIESParameterSpec.HMAC.SHA256_HMAC, null, null);
      paramSpec = new LunaECIESExtParameterSpec(LunaECIESParameterSpec.DH_PRIMITIVE.ECDHC,
          LunaECIESParameterSpec.KDF.SHA256, LunaECIESParameterSpec.HMAC.SHA256_HMAC, null, null, null, 0);
      paramSpec = new LunaECIESExtParameterSpec(LunaECIESParameterSpec.DH_PRIMITIVE.ECDHC, LunaECIESParameterSpec.KDF.SHA256,
          LunaECIESParameterSpec.HMAC.SHA256_HMAC, LunaECIESParameterSpec.ENCRYPTION_SCHEME.AES_CBC_PAD, 256, 256, 256,
          null, null, null, 0);

      //ECIES Ext2
      //Flags for KDF additional shared data (sharedData1)
      //0 = no addition to shared data
      //1 = shared data | ephemeral public key
      //2 = shared data | compressed ephemeral public key
      //3 = ephemeral public key            | shared data
      //4 = compressed ephemeral public key | shared data
//      int kdfFlag = 4;
//      paramSpec = new LunaECIESExt2ParameterSpec(LunaECIESParameterSpec.DH_PRIMITIVE.ECDHC,
//          LunaECIESParameterSpec.KDF.SHA256, LunaECIESParameterSpec.HMAC.SHA256_HMAC, null, null, null, 0, kdfFlag);
//      paramSpec = new LunaECIESExt2ParameterSpec(LunaECIESParameterSpec.DH_PRIMITIVE.ECDHC, LunaECIESParameterSpec.KDF.SHA256,
//          LunaECIESParameterSpec.HMAC.SHA256_HMAC, LunaECIESParameterSpec.ENCRYPTION_SCHEME.AES_CBC_PAD, 256, 256, 256,
//          null, null, null, 0, kdfFlag);

      cipher.init(Cipher.ENCRYPT_MODE, kp.getPublic(),paramSpec);
      byte[] enc = cipher.doFinal(orig);
      Cipher decrypter = Cipher.getInstance("ECIES", provider);
      decrypter.init(Cipher.DECRYPT_MODE, kp.getPrivate(), cipher.getParameters());
      byte[] dec = decrypter.doFinal(enc);
      areArraysEqual(orig, dec);
      System.out.println("ECIES AES_CBC_PAD enc/dec succeeded.");

      //ECIES Ext
      //GCM
      String aad = "AAD4";
      byte[] iv = new byte[] { 1, 2, 3, 4};
      LunaGcmParameterSpec encryptSpec = new LunaGcmParameterSpec(iv,aad.getBytes("UTF-8"),128);
      byte[] eciesEncParams = encryptSpec.getDataEncoded();
      paramSpec = new LunaECIESExtParameterSpec(LunaECIESParameterSpec.DH_PRIMITIVE.ECDHC, LunaECIESParameterSpec.KDF.SHA256,
      LunaECIESParameterSpec.HMAC.SHA256_HMAC, LunaECIESParameterSpec.ENCRYPTION_SCHEME.AES_GCM, 256, 256, 256,
      null, null, eciesEncParams, eciesEncParams.length);

      cipher.init(Cipher.ENCRYPT_MODE, kp.getPublic(),paramSpec);
      enc = cipher.doFinal(orig);
      decrypter = Cipher.getInstance("ECIES", provider);
      decrypter.init(Cipher.DECRYPT_MODE, kp.getPrivate(), cipher.getParameters());
      dec = decrypter.doFinal(enc);
      areArraysEqual(orig, dec);
      System.out.println("ECIES GCM enc/dec succeeded.");

      //CTR
      iv = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
      AlgorithmParameters parmIv = AlgorithmParameters.getInstance("IV", "LunaProvider");
      parmIv.init(new IvParameterSpec(iv));
      byte[] ivEncoded = parmIv.getEncoded("DER");
      paramSpec = new LunaECIESExtParameterSpec(LunaECIESParameterSpec.DH_PRIMITIVE.ECDHC, LunaECIESParameterSpec.KDF.SHA256,
      LunaECIESParameterSpec.HMAC.SHA256_HMAC, LunaECIESParameterSpec.ENCRYPTION_SCHEME.AES_CTR, 256, 256, 256,
      null, null, ivEncoded, ivEncoded.length);

      cipher.init(Cipher.ENCRYPT_MODE, kp.getPublic(),paramSpec);
      enc = cipher.doFinal(orig);
      decrypter = Cipher.getInstance("ECIES", provider);
      decrypter.init(Cipher.DECRYPT_MODE, kp.getPrivate(), cipher.getParameters());
      dec = decrypter.doFinal(enc);
      areArraysEqual(orig, dec);
      System.out.println("ECIES AES_CTR enc/dec succeeded.");

      //AES_KW
      iv = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
      parmIv = AlgorithmParameters.getInstance("IV", "LunaProvider");
      parmIv.init(new IvParameterSpec(iv));
      ivEncoded = parmIv.getEncoded("DER");
      paramSpec = new LunaECIESExtParameterSpec(LunaECIESParameterSpec.DH_PRIMITIVE.ECDHC, LunaECIESParameterSpec.KDF.SHA256,
      LunaECIESParameterSpec.HMAC.SHA256_HMAC, LunaECIESParameterSpec.ENCRYPTION_SCHEME.AES_KW, 256, 256, 256,
      null, null, ivEncoded, ivEncoded.length);

      cipher.init(Cipher.ENCRYPT_MODE, kp.getPublic(),paramSpec);
      enc = cipher.doFinal(orig);
      decrypter = Cipher.getInstance("ECIES", provider);
      decrypter.init(Cipher.DECRYPT_MODE, kp.getPrivate(), cipher.getParameters());
      dec = decrypter.doFinal(enc);
      areArraysEqual(orig, dec);
      System.out.println("ECIES AES_KW enc/dec succeeded.");

      //AES_KWP
      iv = new byte[] { 1, 2, 3, 4 };
      parmIv = AlgorithmParameters.getInstance("IV", "LunaProvider");
      parmIv.init(new IvParameterSpec(iv));
      ivEncoded = parmIv.getEncoded("DER");
      paramSpec = new LunaECIESExtParameterSpec(LunaECIESParameterSpec.DH_PRIMITIVE.ECDHC, LunaECIESParameterSpec.KDF.SHA256,
      LunaECIESParameterSpec.HMAC.SHA256_HMAC, LunaECIESParameterSpec.ENCRYPTION_SCHEME.AES_KWP, 256, 256, 256,
      null, null, ivEncoded, ivEncoded.length);

      cipher.init(Cipher.ENCRYPT_MODE, kp.getPublic(),paramSpec);
      enc = cipher.doFinal(orig);
      decrypter = Cipher.getInstance("ECIES", provider);
      decrypter.init(Cipher.DECRYPT_MODE, kp.getPrivate(), cipher.getParameters());
      dec = decrypter.doFinal(enc);
      areArraysEqual(orig, dec);
      System.out.println("ECIES AES_KWP enc/dec succeeded.");

    } catch (InvalidKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidAlgorithmParameterException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchPaddingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalBlockSizeException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (BadPaddingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidParameterSpecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
