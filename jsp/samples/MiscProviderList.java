
// ****************************************************************************
// Copyright (c) 2010-2020 SafeNet, Inc. All rights reserved.
//
// All rights reserved.  This file contains information that is
// proprietary to SafeNet, Inc. and may not be distributed
// or copied without written consent from SafeNet, Inc.
// ****************************************************************************

import java.security.Provider;
import java.security.Security;

class MiscProviderList {
  /*
   ** Description **
   This example illustrates
   - how to conveniently list security providers debugging purposes.

   There are a multitude of ways a provider can be utilized which can lead
   to confusion.  The methods used in common practice will be illustrated
   here.

   Select either Method 1 or Method 2 for loading the Luna Providers. Do
   not mix the methods together as this will cause undefined behaviour,
   most likely resulting in a exception being thrown.

   For more information please see the Security and Provider class
   definitions in the JDK API documentation for the release of the JDK
   you are using.


                          *NOTE FOR ALL METHODS*
   No matter which way you load the Luna Providers, you need to import the
   Luna Provider methods in your source files as is normal for using any 3rd
   party JARs/Classes in your application.

   ** Method 1 **
   The easiest way to insert a provider is to place it at the top of the
   list in the java.security file.

   For example (this is a snippet from a java.security file)

   security.provider.1=LunaProvider
   security.provider.2=sun.security.provider.Sun
   (etc...)

   By placing the LunaProvider at the top of this list, it will become
   the first choice of any provider-dependent classes when searching for
   a provider.

   When no provider is specified in a cryptographic call, the class searches
   through the provider list (starting with the first) to find an
   implementation of the method. If something is not supported by the top-
   level providers then a search through the remaining providers is
   conducted until an appropriate provider is found or an exception is thrown.

   ** Method 2 **
   This method is a completely code-based method of accessing security
   providers at the cost of adding a little complexity to the application.
   You will need one of the follow setups at the top of your source file for
   this to work.

   import java.security.Provider;
   import java.security.Security;
   OR
   import java.security.*;

   Before calls can be made to the Luna providers, they have to by added to
   the list of providers that the JDK loaded.

   This can be done in one of two ways.

      // This method appends the Luna Provider to the bottom of the
      // list
      Security.addProvider(new LunaProvider());
       OR
      // This method will place the Luna Provider at the top of the
      // list
      Security.insertProviderAt(new LunaProvider, 1);

   Adding the providers to either the beginning or the end of the list needs
   to be done only once for the life of the invoking application.


  */
  public static void listProviders() {
    Provider[] providers = Security.getProviders();
    System.out.println("Provider list");
    for (int i = 0; i < providers.length; i++) {
      System.out.println((i + 1) + ":" + providers[i].toString());
    }
    System.out.println();
  }
}
