/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/**
 * Based on Simple1.2ClassLoader.java - simple Java 1.2 class loader
 * Copyright (c) 1999 Ken McCrary, All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * KEN MCCRARY MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE
 * SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. KEN MCCRARY
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT
 * OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

/**
 *  Simple class loader to illustrate custom Class
 *  loading with new delegation model in Java 1.2.
 *
 */
public class PdeClassLoader extends ClassLoader {
  String buildFolderPath;

  /*
  PdeClassLoader(ClassLoader parent) {
    super(parent);
    init();
  }
  */

  PdeClassLoader() {
    //super();  // uses system class loader
    //init();
    //}

    //private void init() {
    buildFolderPath = PdeBase.getBuildFolder().getAbsolutePath();

    /*
    FileInputStream fi = null;

    // Check for a manifest in the store directory
    try
    {
      fi = new FileInputStream("store\\MANIFEST.MF");
      manifest = new Manifest(fi);
    }
    catch (Exception e)
    {
      // No manifest
    }
    finally
    {
      if ( null != fi )
      {
        try
        {
          fi.close();
        }
        catch (Exception e){}
      }
    }
    */
  }


  /**
   *  This is the method where the task of class loading
   *  is delegated to our custom loader.
   *
   * @param  name the name of the class
   * @return the resulting <code>Class</code> object
   * @exception ClassNotFoundException if the class could not be found
   */
  protected Class findClass(String name) throws ClassNotFoundException
  {
    FileInputStream fi = null;

    try {
      //System.out.println("PCL finding class: " + name);
      String path =
        buildFolderPath + File.separator + name.replace('.', '/');
      //System.out.println("(from " + path + ")");
      fi = new FileInputStream(path + ".class");
      byte[] classBytes = new byte[fi.available()];
      fi.read(classBytes);
      //definePackage(name);
      return defineClass(name, classBytes, 0, classBytes.length);

    } catch (Exception e) {
      // We could not find the class, so indicate the problem with an exception
      throw new ClassNotFoundException(name);

    } finally {
      if (fi != null) {
        try {
          fi.close();
        } catch (Exception e) { }
      }
    }
  }


  /**
   *  Identify where to load a resource from, resources for
   *  this simple ClassLoader are in a directory name "store"
   *
   *  @param name the resource name
   *  @return URL for resource or null if not found
   */
  protected URL findResource(String name) {
    String path =
      buildFolderPath + File.separator + name.replace('.', '/');
    File searchResource = new File(path, name);
    //URL result = null;

    if (searchResource.exists()) {
      try {
        return searchResource.toURL();
      } catch (MalformedURLException mfe) { }
    }
    //return result;
    return null;
  }


  /**
   *  Used for identifying resources from multiple URLS
   *  Since our simple Classloader only has one repository
   *  the returned Enumeration contains 0 to 1 items
   *
   *  @param name the resource name
   *  @return Enumeration of one URL
   */
  protected Enumeration findResources(final String name) throws IOException {
    // Since we only have a single repository we will only have one
    // resource of a particular name, the Enumeration will just return
    // this single URL

    return new Enumeration() {
        URL resource = findResource(name);

        public boolean hasMoreElements() {
          return ( resource != null ? true : false);
        }

        public Object nextElement() {
          if (!hasMoreElements()) {
            throw new NoSuchElementException();
          } else {
            URL result = resource;
            resource = null;
            return result;
          }
        }
      };
  }


  /**
   *  Minimal package definition
   *
   */
  /*
  private void definePackage(String className)
  {
    // Extract the package name from the class name,
    String pkgName = className;
    int index = className.lastIndexOf('.');
    if (-1 != index)
    {
      pkgName =  className.substring(0, index);
    }

    // Pre-conditions - need a manifest and the package
    // is not previously defined
    if ( null == manifest ||
         getPackage(pkgName) != null)
    {
      return;
    }

    String specTitle,
           specVersion,
           specVendor,
           implTitle,
           implVersion,
           implVendor;

    // Look up the versioning information
    // This should really look for a named attribute
    Attributes attr = manifest.getMainAttributes();

    if ( null != attr)
    {
      specTitle   = attr.getValue(Name.SPECIFICATION_TITLE);
      specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
      specVendor  = attr.getValue(Name.SPECIFICATION_VENDOR);
      implTitle   = attr.getValue(Name.IMPLEMENTATION_TITLE);
      implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
      implVendor  = attr.getValue(Name.IMPLEMENTATION_VENDOR);

      definePackage(pkgName,
                    specTitle,
                    specVersion,
                    specVendor,
                    implTitle,
                    implVersion,
                    implVendor,
                    null); // no sealing for simplicity
    }
  }

  private Manifest manifest;
  */
}
