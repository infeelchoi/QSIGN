/**
 * Copyright (c) 2018 SafeNet, All rights reserved.
 *
 * All rights reserved.  This file contains information that is proprietary to SafeNet and may not
 * be distributed or copied without written consent from SafeNet.
 */

// ****************************************************************************
// Copyright (c) 2010 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

import com.safenetinc.luna.provider.param.LunaEDDSAParameterSpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

import com.safenetinc.luna.provider.LunaCertificateX509;
import com.safenetinc.luna.provider.LunaProvider;

/**
 * This example illustrates how to generate EDDSA key pairs The pair will then be used to generate a
 * self-signed certificate and to sign/verify some data.
 */
public class SignatureEDDSAphDemo {

    // Configure these as required.
    private static final int slot = 0;
    private static final String passwd = "userpin";
    public static final String provider = "LunaProvider";
    public static final String keystoreProvider = "Luna";

    public static void main(String[] args) {

        KeyStore myStore = null;
        try {

            /* Note: could also use a keystore file, which contains the token label or slot no. to use.
             * Load that via "new FileInputStream(ksFileName)" instead of ByteArrayInputStream. Save
             * objects to the keystore via a FileOutputStream. */

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

        KeyPairGenerator keyGen = null;
        KeyPair keyPair = null;
        try {
            // Generate an EDDSA KeyPair

            /* The KeyPairGenerator class is used to determine the type of KeyPair being generated. For
             * more information concerning the algorithms available in the Luna provider please see the
             * Luna Development Guide. For more information concerning other providers, please read the
             * documentation available for the provider in question. */
            System.out.println("Generating Ed25519 Keypair");

            /* The KeyPairGenerator.getInstance method also supports specifying providers as a parameter
             * to the method. Many other methods will allow you to specify the provider as a parameter.
             * Please see the Sun JDK class reference at http://java.sun.org for more information. */
            keyGen = KeyPairGenerator.getInstance("Ed25519", provider);

            /* EDDSA keys need to know what curve to use. If you know the curve ID to use you can specify
             * it directly. In the Luna Provider all supported curves are defined in LunaECCurve */
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("Ed25519");
            keyGen.initialize(ecSpec);
            keyPair = keyGen.generateKeyPair();
        } catch (Exception e) {
            System.out.println("Exception during Key Generation - " + e.getMessage());
            System.exit(1);
        }

        byte[] bytes = "Some Text to Sign as an Example".getBytes();
        System.out
            .println("PlainText = " + com.safenetinc.luna.LunaUtils.getHexString(bytes, true));

        Signature sig = null;
        byte[] signatureBytes = null;
        try {
            // Create a Signature Object.

            /* Sign/Verify operations like Encrypt/Decrypt operations can be performed in either
             * singlepart or multipart steps. Single part Signing and Verify examples are given in this
             * code. Multipart signatures use the Signature.update() method to load all the bytes and then
             * invoke the Signature.sign() method to get the result. For more information please see the
             * class documentation for the java.security.Signature class with respect to the version of
             * the JDK you are using. */
            System.out.println("Signing encrypted text");
            sig = Signature.getInstance("EDDSA", provider);

            // Create and assign a parameter spec with the pre-hash value set to true.
            // This selects the use of EDDSAph rather than PureEDDSA.
            sig.setParameter(new LunaEDDSAParameterSpec(true));

            // Finally, sign the encrypted text/
            sig.initSign(keyPair.getPrivate());
            sig.update(bytes);
            signatureBytes = sig.sign();

            // Verify the signature
            System.out.println("Verifying signature(via Signature.verify)");
            sig.initVerify(keyPair.getPublic());
            sig.update(bytes);
            boolean verifies = sig.verify(signatureBytes);
            if (verifies == true) {
                System.out.println("Signature passed verification");
            } else {
                System.out.println("Signature failed verification");
            }

            // generate a self-signed EdDSA certificate.
            Date notBefore = new Date();
            Date notAfter = new Date(notBefore.getTime() + 1000000000);
            BigInteger serialNum = new BigInteger("123456");
            LunaCertificateX509 cert = null;
            try {
                cert = LunaCertificateX509
                    .SelfSign(keyPair, "CN=EdDSA Sample Cert", serialNum, notBefore,
                        notAfter);
            } catch (InvalidKeyException ike) {
                System.out.println("Unexpected InvalidKeyException while generating cert.");
                System.exit(-1);
            } catch (CertificateEncodingException cee) {
                System.out
                    .println("Unexpected CertificateEncodingException while generating cert.");
                System.exit(-1);
            }

            try {
                // Verify the signature
                System.out.println("Verifying signature(via cert)");
                sig.initVerify(cert);
                sig.update(bytes);
                verifies = sig.verify(signatureBytes);
                if (verifies == true) {
                    System.out.println("Signature passed verification");
                } else {
                    System.out.println("Signature failed verification");
                }
            } catch (Exception e) {
                System.out.println("Exception during Verification - " + e.getMessage());
                System.exit(1);
            }

        } catch (Exception e) {
            System.out.println("Exception during Signing - " + e.getMessage());
            System.exit(1);
        }

    }
}
