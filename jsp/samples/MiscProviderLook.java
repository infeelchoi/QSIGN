
/*
Ciphers:
            None available.
KeyAgreeents:
            None available.
Macs:
            None available.
MessageDigests:
            SHA-512
            SHA1
            MD2
            SHA
            SHA ImplementedIn
            SHA-256
            MD5 ImplementedIn
            SHA-1
            MD5
            SHA-384
Signatures:
            OID.1.2.840.10040.4.3
            OID.1.2.840.113549.1.1.4
            SHA384withRSA
            1.3.14.3.2.29
            SHA512withRSA SupportedKeyClasses
            SHA/DSA
            SHA1withDSA KeySize
            NONEwithDSA SupportedKeyClasses
            OID.1.2.840.113549.1.1.5
            SHA512withRSA
            MD5withRSA
            DSS
            OID.1.2.840.113549.1.1.11
            SHA384withRSA SupportedKeyClasses
            SHA1withRSA
            MD5withRSA SupportedKeyClasses
            NONEwithDSA
            1.2.840.113549.1.1.4
            MD5andSHA1withRSA
            1.2.840.113549.1.1.11
            OID.1.2.840.113549.1.1.13
            1.3.14.3.2.27
            1.2.840.10040.4.3
            SHA256withRSA
            MD2withRSA SupportedKeyClasses
            1.2.840.113549.1.1.2
            1.2.840.113549.1.1.12
            RawDSA
            SHA1withDSA
            SHA1/DSA
            MD2withRSA
            1.3.14.3.2.13
            SHAwithDSA
            DSAWithSHA1
            1.2.840.113549.1.1.13
            OID.1.3.14.3.2.29
            SHA1withDSA ImplementedIn
            SHA256withRSA SupportedKeyClasses
            SHA1withDSA SupportedKeyClasses
            DSA
            1.2.840.113549.1.1.5
            SHA-1/DSA
            SHA1withRSA SupportedKeyClasses
            OID.1.2.840.113549.1.1.12
            OID.1.2.840.113549.1.1.2
 */
import java.security.Provider;
import java.security.Security;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import com.safenetinc.luna.provider.LunaProvider;

public class MiscProviderLook {
  public static void printSet(String setName, Set algorithms) {
    System.out.println(setName + ":");
    if (algorithms.isEmpty()) {
      System.out.println("            None available.");
    } else {
      Iterator it = algorithms.iterator();
      while (it.hasNext()) {
        String name = (String) it.next();

        System.out.println("            " + name);
      }
    }
  }

  public static void main(String[] args) {
    Set<String> ciphers = new HashSet<String>();
    Set<String> keyAgreements = new HashSet<String>();
    Set<String> macs = new HashSet<String>();
    Set<String> messageDigests = new HashSet<String>();
    Set<String> signatures = new HashSet<String>();
    Set<String> KeyPairGenerators = new HashSet<String>();
    Set<String> KeyGenerators = new HashSet<String>();
    Set<String> KeyFactories = new HashSet<String>();
    Set<String> SecretKeyFactories = new HashSet<String>();
    Set<String> SecureRandoms = new HashSet<String>();

    // get the luna provider
    Security.addProvider(new LunaProvider());
    Provider lunaProvider = Security.getProvider(("LunaProvider"));

    Iterator it = lunaProvider.keySet().iterator();

    while (it.hasNext()) {
      String entry = (String) it.next();

      if (entry.startsWith("Alg.Alias.")) {
        entry = entry.substring("Alg.Alias.".length());
      }

      if (entry.startsWith("Cipher.")) {
        ciphers.add(entry.substring("Cipher.".length()));
      } else if (entry.startsWith("KeyAgreement.")) {
        keyAgreements.add(entry.substring("KeyAgreement.".length()));
      } else if (entry.startsWith("Mac.")) {
        macs.add(entry.substring("Mac.".length()));
      } else if (entry.startsWith("MessageDigest.")) {
        messageDigests.add(entry.substring("MessageDigest.".length()));
      } else if (entry.startsWith("Signature.")) {
        signatures.add(entry.substring("Signature.".length()));
      } else if (entry.startsWith("KeyGenerator.")) {
        KeyGenerators.add(entry.substring("KeyGenerator.".length()));
      } else if (entry.startsWith("KeyPairGenerator.")) {
        KeyPairGenerators.add(entry.substring("KeyPairGenerator.".length()));
      } else if (entry.startsWith("KeyFactory.")) {
        KeyFactories.add(entry.substring("KeyFactory.".length()));
      } else if (entry.startsWith("SecretKeyFactory.")) {
        SecretKeyFactories.add(entry.substring("SecretKeyFactory.".length()));
      } else if (entry.startsWith("SecureRandom.")) {
        SecureRandoms.add(entry.substring("SecureRandom.".length()));
      }

    }

    printSet("Ciphers", ciphers);
    printSet("KeyAgreeents", keyAgreements);
    printSet("Macs", macs);
    printSet("MessageDigests", messageDigests);
    printSet("Signatures", signatures);
    printSet("KeyPairGenerators", KeyPairGenerators);
    printSet("KeyGenerators", KeyGenerators);
    printSet("KeyFactories", KeyFactories);
    printSet("SecretKeyFactories", SecretKeyFactories);
    printSet("SecureRandoms", SecureRandoms);

  }
}
