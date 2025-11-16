import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedDataParser;
import org.bouncycastle.cms.CMSEnvelopedDataStreamGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.Recipient;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.operator.OutputEncryptor;

import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.provider.LunaCertificateX509;
import com.safenetinc.luna.provider.LunaProvider;

public class CipherCMSDemo {

  private static final String WORK_DIR = "/tmp/";

  private static final File SOURCE_PDF = new File(WORK_DIR, "source.pdf");
  private static final File DESTINATION_FILE = new File(WORK_DIR, "encrypted.pdf");
  private static final File DECRYPTED_FILE = new File(WORK_DIR, "decrypted.pdf");

  private static RecipientInformation getSingleRecipient(CMSEnvelopedDataParser parser) {
    Collection<RecipientInformation> recInfos = parser.getRecipientInfos().getRecipients();
    Iterator<RecipientInformation> recipientIterator = recInfos.iterator();
    if (!recipientIterator.hasNext()) {
      throw new RuntimeException("Could not find recipient");
    }
    return recipientIterator.next();
  }

  private static void decrypt(PrivateKey privateKey, File encrypted, File decryptedDestination)
      throws IOException, CMSException {

    byte[] encryptedData = Files.readAllBytes(encrypted.toPath());

    CMSEnvelopedDataParser parser = new CMSEnvelopedDataParser(encryptedData);

    RecipientInformation recInfo = getSingleRecipient(parser);
    Recipient recipient = new JceKeyTransEnvelopedRecipient(privateKey);
    ((JceKeyTransEnvelopedRecipient) recipient).setContentProvider("BC");
    ((JceKeyTransEnvelopedRecipient) recipient).setContentProvider("LunaProvider");

    InputStream decryptedStream = recInfo.getContentStream(recipient).getContentStream();
//    try (InputStream decryptedStream = recInfo.getContentStream(recipient).getContentStream()) {//try-with-resources needs JDK7
    Files.copy(decryptedStream, decryptedDestination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
//    }
    decryptedStream.close();

    System.out.println(
        String.format("Decrypted '%s' to '%s'", encrypted.getAbsolutePath(), decryptedDestination.getAbsolutePath()));
  }

  private static void encrypt(X509Certificate cert, File source, File destination)
      throws CertificateEncodingException, CMSException, IOException {
    CMSEnvelopedDataStreamGenerator gen = new CMSEnvelopedDataStreamGenerator();
    gen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(cert));
    OutputEncryptor encryptor = null;
    encryptor = new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).setProvider("BC").build();
    encryptor = new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).setProvider("LunaProvider").build();

    FileOutputStream fileStream = new FileOutputStream(destination);
    OutputStream encryptingStream = gen.open(fileStream, encryptor);
//    try (FileOutputStream fileStream = new FileOutputStream(destination);//try-with-resources needs JDK7
//        OutputStream encryptingStream = gen.open(fileStream, encryptor)) {
    byte[] unencryptedContent = Files.readAllBytes(source.toPath());
    encryptingStream.write(unencryptedContent);
//    }
    encryptingStream.close();
    fileStream.close();

    System.out
        .println(String.format("Encrypted '%s' to '%s'", source.getAbsolutePath(), destination.getAbsolutePath()));
  }

  public static void main(final String[] args) throws Exception {

    System.out.println();
    System.out.println("BouncyCastle v1.57 works best with this sample...");
    System.out.println();

    Security.addProvider(new LunaProvider());
    ByteArrayInputStream is = new ByteArrayInputStream(("slot:0").getBytes());
    KeyStore ks = null;
    ks = KeyStore.getInstance("Luna");
    ks.load(is, "userpin".toCharArray());
    LunaSlotManager.getInstance().setSecretKeysExtractable(true);

    // generate RSA key pair
    KeyPairGenerator kpg = null;
    kpg = KeyPairGenerator.getInstance("RSA", "BC");
    kpg = KeyPairGenerator.getInstance("RSA", "LunaProvider");
    kpg.initialize(2048);
    KeyPair myPair = kpg.generateKeyPair();

    // make rsa self signed cert
    Date notBefore = new Date();
    Date notAfter = new Date(notBefore.getTime() + 1000000000);
    LunaCertificateX509 myCert = LunaCertificateX509.SelfSign(myPair, "CN=TestCert", new BigInteger("123456"),
        notBefore, notAfter);

    X509Certificate certificate = myCert;
    PrivateKey privateKey = myPair.getPrivate();

    encrypt(certificate, SOURCE_PDF, DESTINATION_FILE);
    decrypt(privateKey, DESTINATION_FILE, DECRYPTED_FILE);
  }

}