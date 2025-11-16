
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.Date;

import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;
import javax.crypto.spec.DHParameterSpec;

import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.LunaTokenObject;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.key.LunaKey;
import com.safenetinc.luna.provider.param.LunaDHX942ParameterSpec;

public class KeyGeneratorDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";
  public static final String provider = "LunaProvider";
  public static final String keystoreProvider = "Luna";
  public static SealedObject so = null;

  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static void main(String[] args) {

    KeyStore myStore = null;
    try {

      /* Note: could also use a keystore file, which contains the token label or slot no. to use. Load that via
       * "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save objects to the keystore via a
       * FileOutputStream. */

      Security.addProvider(new LunaProvider());
      ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot).getBytes());
      myStore = KeyStore.getInstance("Luna");
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
      KeyPairGenerator kpg;
      kpg = KeyPairGenerator.getInstance("ECwithExtraRandomBits", provider);
      KeyPair kp = kpg.generateKeyPair();
      LunaKey key = (LunaKey) kp.getPublic();
      LunaTokenObject lto = LunaTokenObject.LocateObjectByHandle(key.GetKeyHandle());
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      KeyGenerator kg = KeyGenerator.getInstance("DES", provider);
      LunaKey key = (LunaKey) kg.generateKey();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      KeyGenerator kg = KeyGenerator.getInstance("DES2", provider);
      LunaKey key = (LunaKey) kg.generateKey();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      KeyGenerator kg = KeyGenerator.getInstance("DESede", provider);
      LunaKey key = (LunaKey) kg.generateKey();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      KeyGenerator kg = KeyGenerator.getInstance("AES", provider);
      LunaKey key = (LunaKey) kg.generateKey();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      KeyGenerator kg = KeyGenerator.getInstance("ARIA", provider);
      LunaKey key = (LunaKey) kg.generateKey();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      KeyGenerator kg = KeyGenerator.getInstance("HmacSHA1", "LunaProvider");
      LunaKey key = (LunaKey) kg.generateKey();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      KeyGenerator kg = KeyGenerator.getInstance("HmacSM3", "LunaProvider");
      LunaKey key = (LunaKey) kg.generateKey();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // X9.42 DH keygen
    KeyPairGenerator keyGen = null;
    try {

//      // Use DSA keygen to get parameters
//      keyGen = KeyPairGenerator.getInstance("DSA", "LunaProvider");
//      SecureRandom secureRandom = null;
//      secureRandom = SecureRandom.getInstance("LunaRNG", "LunaProvider");
//      keyGen.initialize(1024, secureRandom);
//      KeyPair keypair = keyGen.generateKeyPair();
//      DSAPublicKey publicKey = (DSAPublicKey) keypair.getPublic();
//      DSAParams parms = publicKey.getParams();
//      BigInteger p = parms.getP(); // The prime modulus P.
//      BigInteger g = parms.getG(); // The base generator G.
//      BigInteger q = parms.getQ(); // The subprime Q
//      String hexStr = null;
//      hexStr = bytesToHex(p.toByteArray());
//      BigInteger uP = new BigInteger(hexStr, 16);
//      hexStr = bytesToHex(g.toByteArray());
//      BigInteger uG = new BigInteger(hexStr, 16);
//      hexStr = bytesToHex(q.toByteArray());
//      BigInteger uQ = new BigInteger(hexStr, 16);
//      System.out.println("parms.getP() -> " + uP.toString(16));
//      System.out.println("parms.getG() -> " + uG.toString(16));
//      System.out.println("parms.getQ() -> " + uQ.toString(16));

      long startTime = new Date().getTime();

      // Use DSA param gen to get parameters
      int keysize = 0;
      keysize = 1024;//p len = 1024/q len = 160
      keysize = 2048;//p len = 2048/q len = 224 or 256
      //keysize = 3072;//p len = 3072/q len = 256
      AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DSA", "LunaProvider");
      //set DSA parm gen qbits to desired length
      LunaSlotManager.getInstance().setDSAParmGenQBits(224);
      //set qbits back to default of 256
      //LunaSlotManager.getInstance().resetDSAParmGenQBits();
      paramGen.init(keysize);
      AlgorithmParameters params = paramGen.generateParameters();
      DSAParameterSpec dsaparamSpec = params.getParameterSpec(DSAParameterSpec.class);
      BigInteger p = dsaparamSpec.getP(); // The prime modulus P.
      BigInteger g = dsaparamSpec.getG(); // The base generator G.
      BigInteger q = dsaparamSpec.getQ(); // The subprime Q
      String hexStr = null;
      hexStr = bytesToHex(p.toByteArray());
      BigInteger uP = new BigInteger(hexStr, 16);
      hexStr = bytesToHex(g.toByteArray());
      BigInteger uG = new BigInteger(hexStr, 16);
      hexStr = bytesToHex(q.toByteArray());
      BigInteger uQ = new BigInteger(hexStr, 16);
      System.out.println("dsaparamSpec.getP() -> " + uP.toString(16));
      System.out.println("dsaparamSpec.getG() -> " + uG.toString(16));
      System.out.println("dsaparamSpec.getQ() -> " + uQ.toString(16));

      DHParameterSpec dhParams = new LunaDHX942ParameterSpec(p, g, q);
      KeyPairGenerator keyGenDH = null;
      keyGenDH = KeyPairGenerator.getInstance("DH", "LunaProvider");
      keyGenDH.initialize(dhParams, new SecureRandom());
      KeyPair kp = keyGenDH.generateKeyPair();

      long diffTot = (new Date()).getTime() - startTime;
      System.out.println("OUT -> DURATION: " + diffTot + " ms");

    } catch (NoSuchAlgorithmException | NoSuchProviderException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (InvalidAlgorithmParameterException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidParameterSpecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
