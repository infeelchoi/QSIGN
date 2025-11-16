import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.security.auth.x500.X500Principal;
import com.safenetinc.luna.provider.LunaProvider;

//import org.apache.commons.codec.binary.Hex;

public class SignatureSignCertWithRootCADemo {

  static String passwd = "userpin";
  static Integer slot = 0;
  static KeyStore myStore = null;

  void doSomeSigning() {
    // do some signing with the root CA
    try {
      // Get the root CA private key from the key store
      String rootCAAliasName = new String("root");
      Key signingKey = myStore.getKey(rootCAAliasName, null);

      String dataToSign = "Sign me!";
      char[] dataToSignArry = dataToSign.toCharArray();
      long startTime = new Date().getTime();
      for (int ctr = 0; ctr < 100; ctr++) {
        // sign with it
        Signature signature = Signature.getInstance("RSA", "LunaProvider");
        // The key found should be a private key otherwise an exception will be thrown
        signature.initSign((PrivateKey) signingKey);
        signature.update(new String(dataToSignArry).getBytes());
        byte[] sig = signature.sign();
//        System.out.print(ctr + " Signature: ");
//        System.out.println(Hex.encodeHex(sig));
        // now, change the data to be signed so the signature changes
        // System.out.println(new String(dataToSignArry) + " (ctr % dataToSign.length()):" + ctr % dataToSign.length());
        dataToSignArry[ctr % dataToSign.length()]++;
      }
      long endTime = new Date().getTime();
      System.out.println("elapsed time(ms): " + (endTime - startTime));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws Exception {

    System.out.println("This sample is dependent upon MiscCSRCertificateDemo...");
    System.out.println("Please ensure that MiscCSRCertificateDemo has bee run first...");
    System.out.println();

    myStore = null;
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

    // Set the root CA alias name
    String rootCAAliasName = new String("root");

    // build an intermediate or leaf cert
    KeyPair pair = MiscCSRCertificateDemo.generateRSAKeyPair();
    String csrPEM = MiscCSRCertificateDemo.generateCertificateSigningRequest(pair, new X500Principal(
        "C=CA, ST=Ontario, L=Ottawa, O=IssuedCert, CN=www.issuedcert.com, EmailAddress=user@issuedcert.com"));
    System.out.println("*************************Certificate Signing Request*************************");
    System.out.print(csrPEM);
    System.out.println("*****************************************************************************\n");

    // sign the cert with the pre-existing root CA
    X509Certificate[] chain = MiscCSRCertificateDemo.issueCertFromCSR(csrPEM, myStore, rootCAAliasName);
    myStore.setKeyEntry("issued", pair.getPrivate(), null, chain);

    PKIXCertPathBuilderResult result = MiscCSRCertificateDemo.validateCert(myStore, rootCAAliasName, "issued");
    System.out.println("*************************Certificate Path Validation***********************");
    System.out.println(result.toString());
    System.out.println("*****************************************************************************");

    System.out.println("*************************Issued certificate chain*****************************");
    MiscCSRCertificateDemo.printCertChain(myStore, "issued");
    System.out.println("*****************************************************************************");

    // just some playing with bulk signing
//    new SignCertWithRootCADemo().doSomeSigning();
  }

}
