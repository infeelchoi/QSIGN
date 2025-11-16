import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.safenetinc.luna.LunaUtils;
import com.safenetinc.luna.provider.LunaProvider;

public class KeyFactoryDemo {

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

    try {
      String hexPub = "308201b73082012b06072a8648ce3804013082011e02818100fcec6182eb206b43c03e36c0eadabff56a0c2e79def44bc8f2e53699096d1ff270f159785d756921dbff9773ae08483b662fc07df7512ff68b2e5565fd7982e20c244832aba121cc0799cc09f2d5414d5f3966211365f51b83e9ffcccb3d88cdf238f7c2739131ca7aadff662fec1fb0e1d311a404260376fd011fe00d0204c3021500d3807353b51c5f71b22ac3d0c7e394148fcedc6102818042e3778e6ec31b0db07a6b370d7fb6fb4a0bca6deaac371f6adbcbeba38ddf76a47c3c3d79276a0e579ce4e347180fd9b4ad461d6cf0eac51fb08cf452f624570051e518a75a5bb9c3578a14fd4f27f795b22acea62b1fdf1032c1266da081c7fb99c4266626587093fd381617238ee1578fc325548dc1c08e5f9322c3b1205e038185000281810098dbaab2bb4dff0c736eef40e0a5429103338ecdf23a133879413956fe1a1f087af5261438295e37b74f92534dfe6b671cb1bd402eb087b8f6e1a96ba33adb34cfd9fc4cfb0e7832a82a5d21ebfd6ac5d4eb1538e2cb0bb35dd730b92c62ad6936e4c235d4c5ff0b98fb30355c26c240c338b52cee628e0aa79ec4c9b4089602";
      KeySpec pubSpec = new X509EncodedKeySpec(LunaUtils.hexStringToByteArray(hexPub));
      KeyFactory kf = KeyFactory.getInstance("DSA", provider);
      kf.generatePublic(pubSpec);
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidKeySpecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      // known 1024-bit RSA X9.31 key
      BigInteger n = new BigInteger(
          "a758cc8c980d059b154456f6eb2d8068d42e9145cb1730620c6bdfa9ad8aaa83395a00bce6487afddf7bc0b2df3b8482810c58fdd272419ec090059b34b9d42d44faa802d74dd53b1d965e2eabed28bfa323f987363f2c8e108dbbc814e0fa9054110da4a8874e355becf1f1d4eaf95fb0a817e523b9c74064dc6be3b26dc011",
          16);
      BigInteger e = new BigInteger("11", 16);
      BigInteger p = new BigInteger(
          "bb8d51475064cd194f4e9a4e18fbbab4d149a5b2db66e1860435d3d2fef4d557101b89000b9cb8d22e3c998e7a8564241943d6f4fa276bd4d41b9305a671dde5",
          16);
      BigInteger q = new BigInteger(
          "e46bbf38e1569c0973d7b71e3f8e8037d94ed1d68bced09652a216be2a6a11168b4aa6fa349ca5ecd9942cb5623ae0461c25f0fcc11c34425a5180f6036a56bd",
          16);
      BigInteger d = new BigInteger(
          "0762090633e274f404b3f4c720f6b69b36899577c8f5ba9ae9f5b3477c5cb4b2f73c70f94665147c2436669e7aca27ba76a26d567dfd82e51f1569a8bacbf6897474c6af2cf3a2e2f4ee5aea749eb7ab309cbe349bf51fc140a9e75902dc53b63cda5c9a576cd99f92c501ed527a5ace9842bfc3358cbdf6862a8f2c1e7ee445",
          16);
      BigInteger dmp1 = new BigInteger(
          "634ad0ad48add5fe57299cfc2b58178ce7451b7cce81c2b05c94f7abf0638000ea68ee2d3352f86f45a79c96b955ad7c85d89efa2a14debbf7d25ce4dfa5b1b5",
          16);
      BigInteger dmq1 = new BigInteger(
          "5e0e3fae026ef4f4d558d2ee5667da71597acedfdf370a9840249ff3f358d9dc1b3cdb57f78bcbd9ff3d0359bf092f2bed7908e08bc051c0f80371563da441d5",
          16);
      BigInteger iqmp = new BigInteger(
          "3ac4aefd7448c688a170386141bd2beb87c6290fd0fa91b51afb92fa7bfa1b9df41c093729946625bef8e61bd144bcdcf612d231a3bad6ad101c30dfb229889a",
          16);

      RSAPrivateCrtKeySpec priSpec1024 = new RSAPrivateCrtKeySpec(n, e, d, p, q, dmp1, dmq1, iqmp);
      RSAPublicKeySpec pubSpec1024 = new RSAPublicKeySpec(n, e);

      KeyFactory kf = KeyFactory.getInstance("RSA", provider);
      kf.generatePublic(pubSpec1024);
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidKeySpecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      PrivateKey privKey;
      PublicKey pubKey;
      KeySpec privSpec;
      KeySpec pubSpec;
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", provider);
      ECGenParameterSpec ecParam = new ECGenParameterSpec("secp384r1");
      kpg.initialize(ecParam);
      KeyPair kp = kpg.generateKeyPair();
      pubKey = kp.getPublic();
      privKey = kp.getPrivate();
      pubSpec = new X509EncodedKeySpec(pubKey.getEncoded());
      privSpec = new PKCS8EncodedKeySpec(privKey.getEncoded());

      KeyFactory kf = KeyFactory.getInstance("EC", provider);
      kf.generatePublic(pubSpec);
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidKeySpecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidAlgorithmParameterException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      PrivateKey privKey;
      PublicKey pubKey;
      KeySpec privSpec;
      KeySpec pubSpec;
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH", provider);
      kpg.initialize(2048);
      KeyPair kp = kpg.generateKeyPair();
      pubKey = kp.getPublic();
      privKey = kp.getPrivate();
      pubSpec = new X509EncodedKeySpec(pubKey.getEncoded());
      privSpec = new PKCS8EncodedKeySpec(privKey.getEncoded());

      KeyFactory kf = KeyFactory.getInstance("DH", provider);
      KeySpec ks = kf.getKeySpec(pubKey, X509EncodedKeySpec.class);
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidKeySpecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      Key tempKey = null;
      KeyGenerator kg = null;
      kg = KeyGenerator.getInstance("AES", provider);
      Key key = null;
      key = kg.generateKey();
      SecretKeyFactory kf = null;
      kf = SecretKeyFactory.getInstance("Generic", provider);
      kf = SecretKeyFactory.getInstance("AES", provider);
      kf = SecretKeyFactory.getInstance("ARIA", provider);
      tempKey = kf.translateKey((SecretKey) key);

      kg = KeyGenerator.getInstance("DES", provider);
      kg.init(56);
      key = kg.generateKey();
      kf = SecretKeyFactory.getInstance("DES", provider);
      tempKey = kf.translateKey((SecretKey) key);
//
      kg = KeyGenerator.getInstance("DESede", provider);
      kg.init(128);
      key = kg.generateKey();
      kf = SecretKeyFactory.getInstance("DESede", provider);
      tempKey = kf.translateKey((SecretKey) key);

      kg = KeyGenerator.getInstance("RC2", provider);
      kg.init(128);
      key = kg.generateKey();
      kf = SecretKeyFactory.getInstance("RC2", provider);
      tempKey = kf.translateKey((SecretKey) key);

      kg = KeyGenerator.getInstance("RC4", provider);
      kg.init(128);
      key = kg.generateKey();
      kf = SecretKeyFactory.getInstance("RC4", provider);
      tempKey = kf.translateKey((SecretKey) key);

      kf = SecretKeyFactory.getInstance("PBEWithSHA1AndDESede", provider);
      PBEKeySpec keySpec = null;
      keySpec = new PBEKeySpec("password".toCharArray(), new byte[1], 1);
      key = kf.generateSecret(keySpec);
      tempKey = kf.translateKey((SecretKey) key);

      kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1", provider);
      // generate good keyspec
      // PBEKeySpec(char[] password, byte[] salt, int iterationCount, int keyLength)
      char[] password = new String("P@$$word").toCharArray();
      byte[] salt = new String("salt123").getBytes();
      int iterationCount = 1000;
      keySpec = new PBEKeySpec(password, salt, iterationCount, (8 * 32)); /* sm3 length */
      key = kf.generateSecret(keySpec);
      tempKey = kf.translateKey((SecretKey) key);

      kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSM3", provider);
      // generate good keyspec
      // PBEKeySpec(char[] password, byte[] salt, int iterationCount, int keyLength)
      password = new String("P@$$word").toCharArray();
      salt = new String("salt123").getBytes();
      iterationCount = 1000;
      keySpec = new PBEKeySpec(password, salt, iterationCount, (8 * 32)); /* sm3 length */
      key = kf.generateSecret(keySpec);
      tempKey = kf.translateKey((SecretKey) key);

    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidKeySpecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
