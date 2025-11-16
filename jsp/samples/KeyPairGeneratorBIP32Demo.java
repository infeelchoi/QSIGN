
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.safenetinc.luna.LunaAPI;
import com.safenetinc.luna.LunaSession;
import com.safenetinc.luna.LunaSessionManager;
import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.LunaTokenObject;
import com.safenetinc.luna.LunaUtils;
import com.safenetinc.luna.provider.LunaProvider;
import com.safenetinc.luna.provider.key.LunaPrivateKeyECBIP32;
import com.safenetinc.luna.provider.key.LunaPublicKeyECBIP32;
import com.safenetinc.luna.provider.key.LunaSecretKey;
import com.safenetinc.luna.provider.param.LunaBIP32ParameterSpec;

/**
 * Using BIP32 KAT values from:
 * https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki#Master_key_generation
 * <p>
 * Follows BIP44 notation described at:
 * https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki#path-levels
 */

public class KeyPairGeneratorBIP32Demo
{

    // Configure these as required.
    private static final int slot = 1;
    private static final String passwd = "userpin1";
    public static final String provider = "LunaProvider";
    public static final String keystoreProvider = "Luna";

    private void TestSignVerify( KeyPair keyPair )
    {
        byte[] bytes = "Some Text to Sign as an Example".getBytes();

        Signature sig = null;
        byte[] signatureBytes = null;

        try
        {
            /**
             * Sign/Verify operations like Encrypt/Decrypt operations can be performed in either singlepart or multipart
             * steps. Single part Signing and Verify examples are given in this code. Multipart signatures use the
             * Signature.update() method to load all the bytes and then invoke the Signature.sign() method to get the
             * result. For more information please see the class documentation for the java.security.Signature class
             * with respect to the version of the JDK you are using.
             */

            // Create a Signature Object and sign the encrypted text

            System.out.println( "Signing encrypted text" );
            sig = Signature.getInstance( "ECDSA", provider );
            sig.initSign( keyPair.getPrivate() );
            sig.update( bytes );
            signatureBytes = sig.sign();

            // Verify the signature
            System.out.println( "Verifying signature (via Signature.verify)" );
            sig.initVerify( keyPair.getPublic() );
            sig.update( bytes );
            boolean verifies = sig.verify( signatureBytes );
            if ( verifies == true )
            {
                System.out.println( "Signature passed verification" );
            }
            else
            {
                System.out.println( "Signature failed verification" );
            }
        }
        catch ( Exception e )
        {
            System.out.println( "Exception during Signing - " + e.getMessage() );
            System.exit( 1 );
        }
    }

    private void DemoImportPublicKey()
    {
        // The key to import (public key from test vector 2, path m/0/2147483647')
        byte[] katPubKey =
        "xpub6ASAVgeehLbnwdqV6UKMHVzgqAG8Gr6riv3Fxxpj8ksbH9ebxaEyBLZ85ySDhKiLDBrQSARLq1uNRts8RuJiHjaDMBU4Zn9h8LZNnBC5y4a"
        .getBytes();

        LunaSlotManager lsm = LunaSlotManager.getInstance();
        LunaAPI lunaAPI = lsm.getLunaAPI();
        LunaSession session = LunaSessionManager.getSession();

        int handle = lunaAPI.BIP32ImportPublicKey( session.GetSessionHandle(), katPubKey );

        LunaPublicKeyECBIP32 pubKey = new LunaPublicKeyECBIP32( handle );
        byte[] pubKeyBytes = pubKey.getEncoded();
        if (!Arrays.equals( katPubKey , pubKeyBytes ))
          System.out.println("Imported public key bytes do NOT match KAT value.");
        else
          System.out.println( "Imported public key bytes match KAT value." );
    }

    private void DemoImportPrivateKey()
    {
        // The key to import (private key from test vector 2, path m/0/2147483647')
        byte[] katPrivKey =
        "xprv9wSp6B7kry3Vj9m1zSnLvN3xH8RdsPP1Mh7fAaR7aRLcQMKTR2vidYEeEg2mUCTAwCd6vnxVrcjfy2kRgVsFawNzmjuHc2YmYRmagcEPdU9"
        .getBytes();

        String keyMech = "AES";
        String unWrapMech = "AES/KWP/NoPadding";

        byte[] encryptedbytes = {};
        Key key = null;
        Cipher cyfer = null;

        /**
         * Generate the cipher / unwrap key.
         */
        try
        {
            KeyGenerator kg = KeyGenerator.getInstance( keyMech, provider );
            key = kg.generateKey();
        }
        catch ( Exception e )
        {
            System.out.println( "Exception generating AES cipher/unwrap key." );
            e.printStackTrace();
            System.exit( 1 );
        }

        /**
         * Encrypt the KAT private key.
         */
        try
        {
            cyfer = Cipher.getInstance( unWrapMech, provider );
            cyfer.init( Cipher.ENCRYPT_MODE, key );
            encryptedbytes = cyfer.doFinal( katPrivKey );
        }
        catch ( Exception e )
        {
            System.out.println( "Exception encrypting the private BIP32 key" );
            e.printStackTrace();
            System.exit( 1 );
        }

        /**
         * Unwrap the encrypted key onto the HSM.
         */
        PrivateKey unwrappedKey = null;
        try
        {
            AlgorithmParameters params = cyfer.getParameters();
            cyfer.init( Cipher.UNWRAP_MODE, key, params );
            unwrappedKey = ( LunaPrivateKeyECBIP32 ) cyfer.unwrap( encryptedbytes, "BIP32", Cipher.PRIVATE_KEY );
        }
        catch ( Exception e )
        {
            System.out.println( "Exception attempting to unwrap BIP32 private key" );
            e.printStackTrace();
            System.exit( 1 );
        }

        // Check that a key was generated.
        LunaTokenObject lto = LunaTokenObject.LocateObjectByHandle( ( ( LunaPrivateKeyECBIP32 ) unwrappedKey ).GetKeyHandle() );
        assert (null != lto);
    }

    private void TestBasicMechanism()
    {
        try
        {
            String keyPath = "";

            // This flag will allow derivation from a secret key that will be injected.
            LunaSlotManager.getInstance().setSecretKeysDerivable( true );
            // This flag will allow derivation from private keys.
            LunaSlotManager.getInstance().setPrivateKeysDerivable( true );
            // This flag will permit private keys to be extracted (wrapped) from the HSM.
            LunaSlotManager.getInstance().setPrivateKeysExtractable( false );

            // KAT seed for test vector 1.
            String seed = "000102030405060708090a0b0c0d0e0f";

            // Inject the seed (secret key, used to derive the master keypair) into the HSM.
            SecretKey seedKey = LunaSecretKey.InjectSecretKey( LunaUtils.hexStringToByteArray( seed ) );
            LunaBIP32ParameterSpec spec = new LunaBIP32ParameterSpec( seedKey, keyPath );

            // Generate / derive the master keypair.
            KeyPairGenerator kpg = KeyPairGenerator.getInstance( "BIP32Master", provider );
            kpg.initialize( spec );
            KeyPair keyPair = kpg.generateKeyPair();
            KeyPair masterKeyPair = keyPair;

            System.out.println( "New Master private key: " + keyPair.getPrivate() );
            System.out.println( "New Master public key: " + keyPair.getPublic() );

            // KAT chain (last chain of test vector 1).
            keyPath = "m/0'/1/2'/2/1000000000";
            System.out.println("keyPath:\"" + keyPath + "\"");
            spec = new LunaBIP32ParameterSpec( keyPair.getPrivate(), keyPath );

            kpg = KeyPairGenerator.getInstance( "BIP32Child", provider );
            kpg.initialize( spec );
            keyPair = kpg.generateKeyPair();

            System.out.println( "New Child private key: " + keyPair.getPrivate() );
            System.out.println( "New Child public key: " + keyPair.getPublic() );

            // Test the keys.
            TestSignVerify( keyPair );

            // KAT private and public key bytes for the corresponding keyPath.
            byte[] pubChildBytes =
            "xpub6H1LXWLaKsWFhvm6RVpEL9P4KfRZSW7abD2ttkWP3SSQvnyA8FSVqNTEcYFgJS2UaFcxupHiYkro49S8yGasTvXEYBVPamhGW6cFJodrTHy"
            .getBytes();

            /**
             * Retrieve the public key.
             */
            byte[] pubKeyBytes = keyPair.getPublic().getEncoded();
            System.out.print( "Public key bytes: " );
            System.out.println( new String( pubKeyBytes ) );
            System.out.print( "KAT pubChildBytes: " );
            System.out.println( new String( pubChildBytes ) );

            if (!Arrays.equals( pubChildBytes , pubKeyBytes ))
              System.out.println("Child public key bytes do NOT match KAT value.");
            else
              System.out.println( "Child public key bytes match KAT value." );

            /**
             * Demonstrate import of BIP32 public key.
             */
            DemoImportPublicKey();

            /**
             * Demonstrate import of BIP32 private key.
             */
            DemoImportPrivateKey();

            /**
             * Demonstrate BIP44 derivation
             */
            for (int i = 0; i < 100; i++) {
              keyPath = "m/44'/0'/0'/0'/" + i;
              System.out.println("keyPath:\"" + keyPath + "\"");
              spec = new LunaBIP32ParameterSpec(masterKeyPair.getPrivate(), keyPath);

              kpg = KeyPairGenerator.getInstance("BIP32Child", provider);
              kpg.initialize(spec);
              keyPair = kpg.generateKeyPair();

              System.out.println("New Child private key: " + keyPair.getPrivate());
              System.out.println("New Child public key: " + keyPair.getPublic());

              // Test the keys.
              TestSignVerify(keyPair);
            }

        }
        catch ( NoSuchAlgorithmException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( NoSuchProviderException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main( String[] args )
    {

        KeyStore myStore = null;
        try
        {
            /* Note: could also use a keystore file, which contains the token label or slot no. to use. Load that via
             * "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save objects to the keystore via a
             * FileOutputStream. */

            Security.addProvider(new LunaProvider());
            ByteArrayInputStream is1 = new ByteArrayInputStream( ( "slot:" + slot ).getBytes() );
            myStore = KeyStore.getInstance( keystoreProvider );
            myStore.load( is1, passwd.toCharArray() );
        }
        catch ( KeyStoreException kse )
        {
            System.out.println( "Unable to create keystore object" );
            System.exit( -1 );
        }
        catch ( NoSuchAlgorithmException nsae )
        {
            System.out.println( "Unexpected NoSuchAlgorithmException while loading keystore" );
            System.exit( -1 );
        }
        catch ( CertificateException e )
        {
            System.out.println( "Unexpected CertificateException while loading keystore" );
            System.exit( -1 );
        }
        catch ( IOException e )
        {
            // this should never happen
            System.out.println( "Unexpected IOException while loading keystore." );
            System.exit( -1 );
        }

        KeyPairGeneratorBIP32Demo aDemo = new KeyPairGeneratorBIP32Demo();
        aDemo.TestBasicMechanism();
    }

}
