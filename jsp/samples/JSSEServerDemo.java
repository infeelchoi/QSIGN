
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.provider.LunaProvider;

public class JSSEServerDemo {

  private static final String passwd = "userpin1";

  // Steps to generate server keys and certs:
  // on the server box:
  // 1. create a file "lunassl.ks" with one record: "slot:0" (N.B. point to whatever slot you're using)
  // 2. generate an SSL-based server RSA key pair and cert
  // keytool -genkey -v -storetype luna -keystore lunassl.ks -keypass userpin -storepass userpin -alias selfsignedserver
  // -dname "cn=www.gem.com, ou=jsp, o=gemalto, c=CA" -keyalg RSA -validity 360 -keysize 2048 -ext san=ip:127.0.0.1

  // 3. export the self-signed server cert from the Luna SA for use on a remote client
  // on the server box:
  // keytool -export -v -storetype luna -keystore lunassl.ks -keypass userpin -storepass userpin -alias selfsignedserver
  // -file serverForClient.cer
  // on the client box:
  // copy serverForClient.cer to client box
  // keytool -import -v -noprompt -trustcacerts -alias selfsignedserver -file serverForClient.cer -keystore cacertsForClient.jks
  // -keypass 123456 -storepass 123456
  // keytool -list -rfc -keystore cacertsForClient.jks -storepass 123456 -keypass 123456

  public static class ServerSocketThread extends Thread {

    // helper inner class to help specify the desired server cert alias...
    class CustomKeyManager implements X509KeyManager {
      private X509KeyManager delegateKeyManager;
      private String alias;

      public CustomKeyManager(X509KeyManager defaultKeyManager, String inAlias) {
        this.delegateKeyManager = defaultKeyManager;
        this.alias = inAlias;
      }

      @Override
      public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return delegateKeyManager.chooseClientAlias(keyType, issuers, socket);
      }

      @Override
      public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        // TODO Auto-generated method stub
        System.out.println("=======START=======");
        String actualAlias = delegateKeyManager.chooseServerAlias(keyType, issuers, socket);
        System.out.println("actual alias=" + actualAlias);
        System.out.println("overloaded:matching alias=" + this.alias);
        System.out.println("=======END=======");
        return this.alias;
      }

      @Override
      public X509Certificate[] getCertificateChain(String alias) {
        // TODO Auto-generated method stub
        return delegateKeyManager.getCertificateChain(alias);
      }

      @Override
      public String[] getClientAliases(String keyType, Principal[] issuers) {
        // TODO Auto-generated method stub
        return delegateKeyManager.getClientAliases(keyType, issuers);
      }

      @Override
      public PrivateKey getPrivateKey(String alias) {
        // TODO Auto-generated method stub
        return delegateKeyManager.getPrivateKey(alias);
      }

      @Override
      public String[] getServerAliases(String keyType, Principal[] issuers) {
        // TODO Auto-generated method stub
        return delegateKeyManager.getServerAliases(keyType, issuers);
      }
    }

    public void serverSocket() throws Exception {

      KeyStore ks = KeyStore.getInstance("Luna");
      FileInputStream ksFile = new FileInputStream("lunassl.ks");
      char[] ksPass = passwd.toCharArray();
      ks.load(ksFile, ksPass);

      // init key manager - for server assume Luna SA
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, ksPass);

      // specify the desired server cert alias...
      CustomKeyManager customKeyMgr = null;
      X509KeyManager aKeyMgr = null;
      List<KeyManager> aCustomTrustKeyMgrList = new ArrayList<KeyManager>();
      for (KeyManager km : kmf.getKeyManagers()) {
        if (km instanceof X509KeyManager) {
          aKeyMgr = (X509KeyManager) km;
          // Ensure this alias refers to the private key of the cert
          // you're actually using
          String alias = "selfsignedserver";
          customKeyMgr = new CustomKeyManager(aKeyMgr, alias);
          aCustomTrustKeyMgrList.add(customKeyMgr);
        }
      }
      KeyManager[] aCustomTrustKeyMgrArry = new KeyManager[aCustomTrustKeyMgrList.size()];
      aCustomTrustKeyMgrArry = aCustomTrustKeyMgrList.toArray(aCustomTrustKeyMgrArry);

      // now trust manager - for server assume Luna SA
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
      tmf.init(ks);

      SSLContext sslctx = SSLContext.getInstance("TLSv1.1");
      sslctx = SSLContext.getInstance("TLSv1.2");
      sslctx = SSLContext.getInstance("TLSv1.3");

      //ensure server picks the correct key for server-side authentication
      sslctx.init(aCustomTrustKeyMgrArry, tmf.getTrustManagers(), null);
      //let the server pick the first private key it finds for server-side authentication
//      sslctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

      ServerSocketFactory factory = sslctx.getServerSocketFactory();
      SSLServerSocket server = (SSLServerSocket) factory.createServerSocket(10123);
      // uncomment following line to require client auth
      server.setNeedClientAuth(true);
//      server.setEnabledCipherSuites(new String[] {"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384"});
//      server.setEnabledCipherSuites(new String[] {"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA"});
//      server.setEnabledCipherSuites(new String[] {"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"});
//      server.setEnabledCipherSuites(new String[] {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"});
//      server.setEnabledCipherSuites(new String[] {"TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256"});
//      server.setEnabledCipherSuites(new String[] {"TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384"});
//      server.setEnabledCipherSuites(new String[] {"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"});
//    server.setEnabledCipherSuites(new String[] {"TLS_RSA_WITH_AES_128_GCM_SHA256"});
      server.setEnabledCipherSuites(new String[] {
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_RSA_WITH_AES_128_GCM_SHA256"});

      while (true) {
        try {
          System.out.println("Waiting for SSL client to connect...");
          SSLSocket sslSocket = (SSLSocket) server.accept();

          try {
            System.out.println("SSL client connected...");
            BufferedReader input = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream()));
            String message = input.readLine();
            System.out.println("******************************************************");
            System.out.println("rx'd from client: " + message);
            System.out.println("******************************************************");

            output.write("Hello from the SSL/TLS server!\n");
            output.flush();

            input.close();
            output.close();
          } catch (Exception e) {
            System.out.println("SSL client connect error...");
            System.out.println(e.getMessage());
            e.printStackTrace();
          } finally {
            System.out.println("SSL client disconnected...");
            //sslSocket.close();
          }
        } catch (Exception e) {
          System.out.println("SSL server socket accept error...");
          e.printStackTrace();
        }
      }
    }

    @Override
    public void run() {
      try {
        serverSocket();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String args[]) throws Exception {

    //ensure LunaProvider.jar is signed if using Oracle JDK
//    Security.insertProviderAt(new LunaProvider(), 0);
//    Security.insertProviderAt(new LunaProvider(), 1);
//    Security.insertProviderAt(new LunaProvider(), 2);
    Security.insertProviderAt(new LunaProvider(), 3);
//    Security.insertProviderAt(new LunaProvider(), 4);

//    for (Provider provider : Security.getProviders()) {
//      // Provider provider = Security.getProvider("SunEC");
//      System.out.println(provider.getName());
//      for (String key : provider.stringPropertyNames()) {
//        System.out.println("\t" + key + "\t" + provider.getProperty(key));
//      }
//    }
//     System.exit(0);

    System.out.println("This sample should be used in conjunction with JSSEClientDemo...on same machine");
    System.out.println();
    System.out.println(
        "Please see instructions in the comments at the beginning of both of these related classes...");
    System.out.println();

    LunaSlotManager.getInstance().setSecretKeysExtractable(true);
    LunaSlotManager.getInstance().setSecretKeysDerivable( true );

    java.lang.System.setProperty("javax.net.debug", "all");
    java.lang.System.setProperty("javax.net.debug", "none");

    ServerSocketThread serverThread = new ServerSocketThread();
    serverThread.start();

  }

}
