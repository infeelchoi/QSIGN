
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import com.safenetinc.luna.provider.LunaProvider;

/**
 * This example shows how to generate a certificate signing request and generate a certificate from this certificate
 * signing request. As well, a root certificate is generated amd the issued certificate is signed using the private key
 * from the root certificate. The example requires the Bouncy Castle provider.
 *
 * The private keys and certificates are stored on the Luna HSM using the Luna KeyStore. The certificate path is
 * verified showing that the issued certificate is signed by the root certificate.
 */

public class MiscCSRCertificateDemo {

  // Configure these as required.
  private static final int slot = 0;
  private static final String passwd = "userpin";

  public static KeyPair generateRSAKeyPair() throws Exception {
    /**
     * Generate an RSA Key pair.
     */
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "LunaProvider");
    generator.initialize(2048);
    return generator.generateKeyPair();
  }

  public static String generateCertificateSigningRequest(KeyPair pair, X500Principal subject) throws Exception {
    /**
     * Build and sign the certificate signing request. The public key is included in the CSR and the private key is used
     * to sign the CSR.
     */
    ContentSigner signGen = new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate());

    PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, pair.getPublic());
    PKCS10CertificationRequest csr = builder.build(signGen);

    /**
     * Use the JcaPEMWriter to generate a PEM version of the CSR as a String. The String contains everything needed for
     * the CA to generate a signed certificate.
     */
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStreamWriter output = new OutputStreamWriter(baos);
    JcaPEMWriter pem = new JcaPEMWriter(output);
    pem.writeObject(csr);
    pem.close();
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }

  public static X509Certificate generateRootCertificate(KeyPair pair, X500Name name) throws Exception {

    System.out.println("Generating root certificate for " + name);

    // Set the start and end dates for the certificates
    Date startDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
    Date endDate = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000);

    /**
     * Generate a self signed certificate. The public key is included in the certificate and it is signed by the
     * matching private key.
     */
    X509v1CertificateBuilder v1CertBuilder = new JcaX509v1CertificateBuilder(name, BigInteger.ONE, startDate, endDate,
        name, pair.getPublic());

    ContentSigner sigGen = new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate());
    X509CertificateHolder certHolder = v1CertBuilder.build(sigGen);
    JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
    return converter.getCertificate(certHolder);
  }

  public static PKCS10CertificationRequest convertPemToPKCS10CertificationRequest(String pem) throws Exception {
    PKCS10CertificationRequest csr = null;
    ByteArrayInputStream pemStream = null;

    /**
     * Use to PEMParser to obtain a PKCS10CertificationRequest request passing in the bytes from the PEM String.
     */

    pemStream = new ByteArrayInputStream(pem.getBytes("UTF-8"));

    Reader pemReader = new BufferedReader(new InputStreamReader(pemStream));
    PEMParser pemParser = new PEMParser(pemReader);

    Object parsedObj = pemParser.readObject();

    if (parsedObj instanceof PKCS10CertificationRequest) {
      csr = (PKCS10CertificationRequest) parsedObj;
    }

    return csr;
  }

  public static X509Certificate[] issueCertFromCSR(String csrPEM, KeyStore keyStore, String rootAlias)
      throws Exception {

    /**
     * First we have to convert the String into a PKCS10CertificationRequest
     */
    PKCS10CertificationRequest csr = convertPemToPKCS10CertificationRequest(csrPEM);

    X500Name x500Name = csr.getSubject();
    System.out.println("Issuing certificate for " + x500Name);

    /**
     * Access the root certificate and root private key from the KeyStore.
     */

    X509Certificate rootCert = (X509Certificate) keyStore.getCertificate(rootAlias);
    PrivateKey rootKey = (PrivateKey) keyStore.getKey(rootAlias, null);

    /**
     * Calculate the start and end dates for the issued cert.
     */
    Date startDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
    Date endDate = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000);

    /**
     * Get the PublicKey from the CSR.
     */
    SubjectPublicKeyInfo pkInfo = csr.getSubjectPublicKeyInfo();
    RSAKeyParameters rsa = (RSAKeyParameters) PublicKeyFactory.createKey(pkInfo);
    RSAPublicKeySpec rsaSpec = new RSAPublicKeySpec(rsa.getModulus(), rsa.getExponent());
    KeyFactory kf = KeyFactory.getInstance("RSA");
    PublicKey rsaPub = kf.generatePublic(rsaSpec);

    /**
     * Instantiate a certificate builder using the root certificate as the issuer, the subject from the CSR as well and
     * the public key from the CSR.
     */
    X509v3CertificateBuilder v3CertBuilder = new JcaX509v3CertificateBuilder(rootCert, new BigInteger("0123456789", 16),
        startDate, endDate, csr.getSubject(), rsaPub);
    // Instantiate a content signer using the root private key.
    ContentSigner sigGen = new JcaContentSignerBuilder("SHA256withRSA").build(rootKey);
    // Builder the certificate using the content signer to get a X509CertificateHolder
    X509CertificateHolder certHolder = v3CertBuilder.build(sigGen);

    JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
    // grab the issued X509Certificate from the certificate holder
    X509Certificate issuedCert = converter.getCertificate(certHolder);

    return new X509Certificate[] { issuedCert, rootCert };
  }

  public static void removeCertsFromKeystore(KeyStore keyStore) throws Exception {
    /**
     * Delete the root and issued private keys and certificate chains.
     */
    keyStore.deleteEntry("root");
    keyStore.deleteEntry("issued");
  }

  public static PKIXCertPathBuilderResult validateCert(KeyStore keyStore, String trustedAlias, String issuedAlias)
      throws Exception {
    /**
     * Setup trust anchors.
     */
    Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
    X509Certificate trustedCert = (X509Certificate) keyStore.getCertificate(trustedAlias);
    trustAnchors.add(new TrustAnchor(trustedCert, null));

    /**
     * Setup the certificate selector based on the issued certificate that we are validating
     */
    X509Certificate cert = (X509Certificate) keyStore.getCertificate(issuedAlias);
    X509CertSelector selector = new X509CertSelector();
    selector.setCertificate(cert);

    PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchors, selector);

    /**
     * Intermediate certificates (in this case just the issued cert) are added to CertStore the CertStore is added to
     * the PKIXBuilderParameters
     */
    Set<X509Certificate> intermediateCerts = new HashSet<X509Certificate>();
    intermediateCerts.add(cert);
    CertStore intermediateCertStore = CertStore.getInstance("Collection",
        new CollectionCertStoreParameters(intermediateCerts), "BC");
    pkixParams.addCertStore(intermediateCertStore);

    /**
     * CRLs should be used in practice but setting to false for simplicity.
     */
    pkixParams.setRevocationEnabled(false);

    /**
     * Build the certificate path validation using the PKIX parameters
     */
    CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", "BC");
    PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult) builder.build(pkixParams);

    return result;
  }

  public static void printCertChain(KeyStore keyStore, String issuedAlias) throws Exception {
    // Get the certificate chain of the issued cert
    Certificate[] chain = keyStore.getCertificateChain(issuedAlias);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStreamWriter output = new OutputStreamWriter(baos);
    // Use JcaPEMWriter to output the certficates
    JcaPEMWriter pem = new JcaPEMWriter(output);
    for (Certificate cert : chain) {
      pem.writeObject(cert);
    }
    pem.close();
    System.out.print(new String(baos.toByteArray(), StandardCharsets.UTF_8));
  }

  public static void main(String[] args) throws Exception {

    System.out.println();
    System.out.println("BouncyCastle v1.57 works best with this sample...");
    System.out.println();

    KeyStore myKeyStore = null;
    try {

      /* Note: could also use a keystore file, which contains the token label or slot no. to use. Load that via
       * "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save objects to the keystore via a
       * FileOutputStream. */

      Security.addProvider(new LunaProvider());
      ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot).getBytes());
      myKeyStore = KeyStore.getInstance("Luna");
      myKeyStore.load(is1, passwd.toCharArray());
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

    removeCertsFromKeystore(myKeyStore);

    KeyPair rootPair = generateRSAKeyPair();
    X509Certificate rootCert = generateRootCertificate(rootPair,
        new X500Name("CN=Test Root Certificate Authority, O=Root, OU=Test"));
    X509Certificate[] rootChain = { rootCert };
    myKeyStore.setKeyEntry("root", rootPair.getPrivate(), null, rootChain);
    myKeyStore.store(null, null);

    KeyPair pair = generateRSAKeyPair();
    String csrPEM = generateCertificateSigningRequest(pair, new X500Principal(
        "C=CA, ST=Ontario, L=Ottawa, O=IssuedCert, CN=www.issuedcert.com, EmailAddress=user@issuedcert.com"));
    System.out.println("*************************Certificate Signing Request*************************");
    System.out.print(csrPEM);
    System.out.println("*****************************************************************************\n");

    X509Certificate[] chain = issueCertFromCSR(csrPEM, myKeyStore, "root");
    myKeyStore.setKeyEntry("issued", pair.getPrivate(), null, chain);

    PKIXCertPathBuilderResult result = validateCert(myKeyStore, "root", "issued");
    System.out.println("*************************Certificate Path Validation***********************");
    System.out.println(result.toString());
    System.out.println("*****************************************************************************");

    System.out.println("*************************Issued certificate chain*****************************");
    printCertChain(myKeyStore, "issued");
    System.out.println("*****************************************************************************");
  }

}
