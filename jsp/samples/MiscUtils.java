import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Properties;

public class MiscUtils {

  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static byte[] hexToBytes(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  public static void dumpClassPath(String args[]) {

    ClassLoader cl = ClassLoader.getSystemClassLoader();

    URL[] urls = ((URLClassLoader) cl).getURLs();

    for (URL url : urls) {
      System.out.println(url.getFile());
    }

  }

  public static void dumpPathsEtc(String args[]) {

//    Properties p = System.getProperties();
//    Enumeration keys = p.keys();
//    while (keys.hasMoreElements()) {
//        String key = (String)keys.nextElement();
//        String value = (String)p.get(key);
//        System.out.println(key + ": " + value);
//    }

    String javaBootClassPath = System.getProperty("sun.boot.class.path");
    System.out.println("sun.boot.class.path:");
    System.out.println(javaBootClassPath);

    String javaExtDirs = System.getProperty("java.ext.dirs");
    System.out.println("java.ext.dirs");
    System.out.println(javaExtDirs);

//    ClassLoader cl = ClassLoader.getSystemClassLoader();
//    URL[] urls = ((URLClassLoader)cl).getURLs();
//    System.out.println("CLASSPATH:");
//    for(URL url: urls){
//      System.out.println(url.getFile());
//    }

    String javaClassPath = System.getProperty("java.class.path");
    System.out.println("java.class.path:");
    System.out.println(javaClassPath);

    String javaLibraryPath = System.getProperty("java.library.path");
    System.out.println("java.library.path:");
    System.out.println(javaLibraryPath);
  }
}
