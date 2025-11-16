import com.safenetinc.luna.provider.LunaProvider;

import java.security.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

/**
 * This example lists all of the algorithms that are supported by the LunaProvider.
 */

public class MiscAlgorithmsList {

  public static ArrayList<String> sortAlgorithms(Enumeration e) {
    ArrayList<String> algorithms = new ArrayList<String>();
    while (e.hasMoreElements()) {
      algorithms.add((String) e.nextElement());
    }
    Collections.sort(algorithms);
    return algorithms;
  }

  public static void main(String[] args) {

    Provider lunaProvider = new LunaProvider();

    System.out.println(lunaProvider + " mechanisms: ");
    ArrayList<String> algorithms = sortAlgorithms(lunaProvider.keys());
    for (String algorithm : algorithms) {
      System.out.println("    " + algorithm);
    }
  }

}
