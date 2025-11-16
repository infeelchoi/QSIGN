// ****************************************************************************
// Copyright (c) 2017 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import com.safenetinc.luna.provider.LunaProvider;

/**
 * This example illustrates how to use the Luna KeyStore with Object Caching enabled
 */
public class KeyStoreCachingLunaDemo {

    // Configure these as required.
    private static final int slot = 0;
    private static final String passwd = "userpin";

    public static void main(String[] args) {

        KeyStore myStore = null;

        try {
            // This ByteArrayInputStream indicates that Object Caching should be enabled and 20 threads should be used
            // to load all of the keys and certificates.  In this mode, all keys and certificates are cached so that
            // later access to get the objects is faster.  This feature can enabled to improve performance,
            // especially in the case where there are many objects on the partition.
            ByteArrayInputStream is1 = new ByteArrayInputStream(("slot:" + slot + "\ncaching:true\nloadingthreads:20").getBytes());

            Security.addProvider(new LunaProvider());
            myStore = KeyStore.getInstance("Luna");
            myStore.load(is1, passwd.toCharArray());
            Enumeration<String> aliases = myStore.aliases();
            System.out.println("aliases:");
            while (aliases.hasMoreElements()) {
                System.out.println("  " + aliases.nextElement());
            }



        } catch (KeyStoreException kse) {
            System.out.println("Unable to create keystore object");
            kse.printStackTrace();
            System.exit(-1);
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("Unexpected NoSuchAlgorithmException while loading keystore");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (CertificateException e) {
            System.out.println("Unexpected CertificateException while loading keystore");
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e) {
            // this should never happen
            System.out.println("Unexpected IOException while loading keystore.");
            e.printStackTrace();
            System.exit(-1);
        }

    }
}
