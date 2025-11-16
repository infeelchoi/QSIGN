import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
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
import java.security.spec.DSAParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.ListIterator;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.param.LunaECIESParameterSpec;
import com.safenetinc.luna.provider.param.LunaECIESParameterSpec.DH_PRIMITIVE;
import com.safenetinc.luna.provider.param.LunaECIESParameterSpec.ENCRYPTION_SCHEME;
import com.safenetinc.luna.provider.param.LunaECIESParameterSpec.HMAC;
import com.safenetinc.luna.provider.param.LunaECIESParameterSpec.KDF;
import com.safenetinc.luna.provider.param.LunaGcmParameterSpec;
import com.safenetinc.luna.provider.param.LunaParameterSpecOAEP;

public class AlgParmsDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";
  public static final String provider = "LunaProvider";
  public static final String keystoreProvider = "Luna";

  private class EciesPermutations {
    public DH_PRIMITIVE dhPrimitive;
    public KDF kdf;
    public HMAC hmacScheme;
    public ENCRYPTION_SCHEME encScheme;
    public int encKeyLen;
    public int macKeyLen;
    public int macLen;
    public byte[] sharedData1;
    public byte[] sharedData2;

    public EciesPermutations(DH_PRIMITIVE dhPrimitive_, KDF kdf_, HMAC hmacScheme_, ENCRYPTION_SCHEME encScheme_,
        int encKeyLen_, int macKeyLen_, int macLen_, byte[] sharedData1_, byte[] sharedData2_) {

      dhPrimitive = dhPrimitive_;
      kdf = kdf_;
      hmacScheme = hmacScheme_;
      encScheme = encScheme_;
      encKeyLen = encKeyLen_;
      macKeyLen = macKeyLen_;
      macLen = macLen_;
      sharedData1 = sharedData1_;
      sharedData2 = sharedData2_;
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

    try {
      int IV_SIZE = 8;
      // Algorithm parameters
      AlgorithmParameters params = null;
      // Algorithm parameters spec.
      AlgorithmParameterSpec paramSpec = null;

      params = AlgorithmParameters.getInstance("IV", provider);
      byte[] iv = new byte[IV_SIZE];
      (new Random()).nextBytes(iv);
      params.init(new IvParameterSpec(iv));
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      e.printStackTrace();
    } catch (InvalidParameterSpecException e) {
      e.printStackTrace();
    }

    try {
      // Algorithm parameters
      AlgorithmParameters params = null;
      // Algorithm parameters spec.
      AlgorithmParameterSpec paramSpec = null;

      params = AlgorithmParameters.getInstance("OAEP", "LunaProvider");
      paramSpec = new LunaParameterSpecOAEP("SHA1", LunaParameterSpecOAEP.mgfType_MGF1);
      params.init(paramSpec);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      e.printStackTrace();
    } catch (InvalidParameterSpecException e) {
      e.printStackTrace();
    }

    try {
      // Algorithm parameters
      AlgorithmParameters params = null;
      // Algorithm parameters spec.
      AlgorithmParameterSpec paramSpec = null;

      params = AlgorithmParameters.getInstance("PSS", "LunaProvider");
      paramSpec = new PSSParameterSpec("SHA1", "MGF1", MGF1ParameterSpec.SHA1, 20, 1);
      params.init(paramSpec);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      e.printStackTrace();
    } catch (InvalidParameterSpecException e) {
      e.printStackTrace();
    }

    try {
      // Algorithm parameters
      AlgorithmParameters params = null;
      // Algorithm parameters spec.
      AlgorithmParameterSpec paramSpec = null;

      params = AlgorithmParameters.getInstance("DSA", "LunaProvider");
      BigInteger bigP = new BigInteger("0"); // LunaAPI.CKA_PRIME
      BigInteger bigQ = new BigInteger("0"); // LunaAPI.CKA_SUBPRIME
      BigInteger bigG = new BigInteger("0"); // LunaAPI.CKA_BASE
      paramSpec = new DSAParameterSpec(bigP, bigQ, bigG);
      params.init(paramSpec);
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

    try {
      byte[] iv;
      byte[] aad;
      // FIPS mode requires no IV be sent.
      iv = new byte[16];
      aad = new byte[16];
      int DEFAULT_TAG_BITS = 128;

      // Algorithm parameters
      AlgorithmParameters params = null;
      // Algorithm parameters spec.
      AlgorithmParameterSpec paramSpec = null;

      params = AlgorithmParameters.getInstance("GCM", "LunaProvider");
      paramSpec = new LunaGcmParameterSpec(aad, iv, DEFAULT_TAG_BITS);
      params.init(paramSpec);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      e.printStackTrace();
    } catch (InvalidParameterSpecException e) {
      e.printStackTrace();
    }

    try {
      EciesPermutations perm = null;
      ArrayList<EciesPermutations> permutations = new ArrayList<EciesPermutations>();

      // build list of all permutations for ECIES parameters
      AlgParmsDemo aPd = new AlgParmsDemo();
      for (DH_PRIMITIVE p : EnumSet.allOf(DH_PRIMITIVE.class)) {
        for (KDF k : EnumSet.allOf(KDF.class)) {
          if (k != KDF.NULL) {
            for (HMAC h : EnumSet.allOf(HMAC.class)) {
              // aPd is needed for inner class creation as inner class instantiation
              // requires an outer class object instance
              permutations.add(aPd.new EciesPermutations(p, k, h, ENCRYPTION_SCHEME.XOR, 0, h.getDefaultSize(),
                  h.getDefaultSize(), null, null));
              permutations.add(aPd.new EciesPermutations(p, k, h, ENCRYPTION_SCHEME.AES_CBC_PAD, 128,
                  h.getDefaultSize(), h.getDefaultSize(), null, null));
              permutations.add(aPd.new EciesPermutations(p, k, h, ENCRYPTION_SCHEME.AES_CBC_PAD, 192,
                  h.getDefaultSize(), h.getDefaultSize(), null, null));
              permutations.add(aPd.new EciesPermutations(p, k, h, ENCRYPTION_SCHEME.AES_CBC_PAD, 256,
                  h.getDefaultSize(), h.getDefaultSize(), null, null));
              permutations.add(aPd.new EciesPermutations(p, k, h, ENCRYPTION_SCHEME.AES_CBC, 128, h.getDefaultSize(),
                  h.getDefaultSize(), null, null));
              permutations.add(aPd.new EciesPermutations(p, k, h, ENCRYPTION_SCHEME.AES_CBC, 192, h.getDefaultSize(),
                  h.getDefaultSize(), null, null));
              permutations.add(aPd.new EciesPermutations(p, k, h, ENCRYPTION_SCHEME.AES_CBC, 256, h.getDefaultSize(),
                  h.getDefaultSize(), null, null));
              permutations.add(aPd.new EciesPermutations(p, k, h, ENCRYPTION_SCHEME.DESede_CBC_PAD, 192,
                  h.getDefaultSize(), h.getDefaultSize(), null, null));
              permutations.add(aPd.new EciesPermutations(p, k, h, ENCRYPTION_SCHEME.DESede_CBC, 192, h.getDefaultSize(),
                  h.getDefaultSize(), null, null));
            }
          }
        }
      }

      ListIterator<EciesPermutations> it = permutations.listIterator();

      // Algorithm parameters
      AlgorithmParameters params = null;
      // Algorithm parameters spec.
      AlgorithmParameterSpec paramSpec = null;

      while (it.hasNext()) {
        perm = it.next();

        params = AlgorithmParameters.getInstance("ECIES", "LunaProvider");
        paramSpec = new LunaECIESParameterSpec(perm.dhPrimitive, perm.kdf, perm.hmacScheme, perm.encScheme,
            perm.encKeyLen, perm.macKeyLen, perm.macLen, perm.sharedData1, perm.sharedData2);
        params.init(paramSpec);

      }
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      e.printStackTrace();
    } catch (InvalidParameterSpecException e) {
      e.printStackTrace();
    }

  }
}
