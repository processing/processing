/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-18 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core.util.io;

import processing.core.PApplet;

import java.io.*;
import java.net.*;
import java.util.zip.GZIPInputStream;


/**
 * Factory producing InputStreams under various parameter sets.
 */
public class InputFactory {

  /**
   * Create a new input stream from a file.
   *
   * @param file The file for which an input stream should be created.
   * @return InputStream for the input file.
   */
  public static InputStream createInput(File file) {
    if (file == null) {
      throw new IllegalArgumentException("File passed to createInput() was null");
    }
    if (!file.exists()) {
      System.err.println(file + " does not exist, createInput() will return null");
      return null;
    }
    try {
      InputStream input = new FileInputStream(file);
      final String lower = file.getName().toLowerCase();
      if (lower.endsWith(".gz") || lower.endsWith(".svgz")) {
        return new BufferedInputStream(new GZIPInputStream(input));
      }
      return new BufferedInputStream(input);

    } catch (IOException e) {
      System.err.println("Could not createInput() for " + file);
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Create an InputStream using path information from a PApplet.
   *
   * <p>
   * Create an InputStream using path information from a PApplet with some pre-processing of file
   * contents like unzipping a gzip compressed file. This is in contrast to createInputRaw which
   * will not perform any pre-processing.
   * </p>
   *
   * @param pApplet The PApplet whose sketch path informatino should be used.
   * @param filename THe filename (url, absolute path, or relative path) to open.
   * @return InputStream for the given filename or null if no input path could be created.
   */
  public static InputStream createInput(PApplet pApplet, String filename) {
    InputStream input = createInputRaw(pApplet, filename);
    if (input != null) {
      // if it's gzip-encoded, automatically decode
      if (isGzipCompressed(filename)) {
        try {
          // buffered has to go *around* the GZ, otherwise 25x slower
          return new BufferedInputStream(new GZIPInputStream(input));

        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        return new BufferedInputStream(input);
      }
    }
    return null;
  }

  /**
   * Create an InputStream without any pre-processing of file content before it is returned.
   *
   * <p>
   * Create an InputStream using path information from a PApplet without any pre-processing of file
   * contents like unzipping a gzip compressed file. This is in contrast to createInput which will
   * perform some pre-processing.
   * </p>
   *
   * @param pApplet The PApplet whose sketch path information should be used.
   * @param filename THe filename (url, absolute path, or relative path) to open.
   * @return InputStream for the given filename or null if no input path could be created.
   */
  public static InputStream createInputRaw(PApplet pApplet, String filename) {
    if (filename == null) return null;

    String sketchPath = pApplet.sketchPath();

    if (sketchPath == null) {
      System.err.println("The sketch path is not set.");
      throw new RuntimeException("Files must be loaded inside setup() or after it has been called.");
    }

    if (filename.length() == 0) {
      // an error will be called by the parent function
      //System.err.println("The filename passed to openStream() was empty.");
      return null;
    }

    // First check whether this looks like a URL
    if (filename.contains(":")) {  // at least smells like URL
      try {
        URL url = new URL(filename);
        URLConnection conn = url.openConnection();

        if (conn instanceof HttpURLConnection) {
          HttpURLConnection httpConn = (HttpURLConnection) conn;
          // Will not handle a protocol change (see below)
          httpConn.setInstanceFollowRedirects(true);
          int response = httpConn.getResponseCode();
          // Default won't follow HTTP -> HTTPS redirects for security reasons
          // http://stackoverflow.com/a/1884427
          if (response >= 300 && response < 400) {
            String newLocation = httpConn.getHeaderField("Location");
            return createInputRaw(pApplet, newLocation);
          }
          return conn.getInputStream();
        } else if (conn instanceof JarURLConnection) {
          return url.openStream();
        }
      } catch (MalformedURLException mfue) {
        // not a url, that's fine

      } catch (FileNotFoundException fnfe) {
        // Added in 0119 b/c Java 1.5 throws FNFE when URL not available.
        // http://dev.processing.org/bugs/show_bug.cgi?id=403

      } catch (IOException e) {
        // changed for 0117, shouldn't be throwing exception
        e.printStackTrace();
        //System.err.println("Error downloading from URL " + filename);
        return null;
        //throw new RuntimeException("Error downloading from URL " + filename);
      }
    }

    InputStream stream = null;

    // Moved this earlier than the getResourceAsStream() checks, because
    // calling getResourceAsStream() on a directory lists its contents.
    // http://dev.processing.org/bugs/show_bug.cgi?id=716
    try {
      // First see if it's in a data folder. This may fail by throwing
      // a SecurityException. If so, this whole block will be skipped.
      File file = new File(pApplet.dataPath(filename));

      if (!file.exists()) {
        // next see if it's just in the sketch folder
        file = pApplet.sketchFile(filename);
      }

      if (file.isDirectory()) {
        return null;
      }
      if (file.exists()) {
        try {
          // handle case sensitivity check
          String filePath = file.getCanonicalPath();
          String filenameActual = new File(filePath).getName();
          // make sure there isn't a subfolder prepended to the name
          String filenameShort = new File(filename).getName();
          // if the actual filename is the same, but capitalized
          // differently, warn the user.
          //if (filenameActual.equalsIgnoreCase(filenameShort) &&
          //!filenameActual.equals(filenameShort)) {
          if (!filenameActual.equals(filenameShort)) {
            throw new RuntimeException("This file is named " +
                filenameActual + " not " +
                filename + ". Rename the file " +
                "or change your code.");
          }
        } catch (IOException e) { }
      }

      // if this file is ok, may as well just load it
      stream = new FileInputStream(file);
      if (stream != null) {
        return stream;
      }

      // have to break these out because a general Exception might
      // catch the RuntimeException being thrown above
    } catch (IOException ioe) {
    } catch (SecurityException se) { }

    // Using getClassLoader() prevents java from converting dots
    // to slashes or requiring a slash at the beginning.
    // (a slash as a prefix means that it'll load from the root of
    // the jar, rather than trying to dig into the package location)
    ClassLoader cl = pApplet.getClass().getClassLoader();

    // by default, data files are exported to the root path of the jar.
    // (not the data folder) so check there first.
    stream = cl.getResourceAsStream("data/" + filename);
    if (stream != null) {
      String cn = stream.getClass().getName();
      // this is an irritation of sun's java plug-in, which will return
      // a non-null stream for an object that doesn't exist. like all good
      // things, this is probably introduced in java 1.5. awesome!
      // http://dev.processing.org/bugs/show_bug.cgi?id=359
      if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
        return stream;
      }
    }

    // When used with an online script, also need to check without the
    // data folder, in case it's not in a subfolder called 'data'.
    // http://dev.processing.org/bugs/show_bug.cgi?id=389
    stream = cl.getResourceAsStream(filename);
    if (stream != null) {
      String cn = stream.getClass().getName();
      if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
        return stream;
      }
    }

    try {
      // attempt to load from a local file, used when running as
      // an application, or as a signed applet
      try {  // first try to catch any security exceptions
        try {
          stream = new FileInputStream(pApplet.dataPath(filename));
          if (stream != null) return stream;
        } catch (IOException e2) { }

        try {
          stream = new FileInputStream(pApplet.sketchPath(filename));
          if (stream != null) return stream;
        } catch (Exception e) { }  // ignored

        try {
          stream = new FileInputStream(filename);
          if (stream != null) return stream;
        } catch (IOException e1) { }

      } catch (SecurityException se) { }  // online, whups

    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Determine if a file is gzip compressed.
   *
   * @param filename The name of the fiel to check.
   * @return True if the file is a gzip compressed file and false otherwise.
   */
  private static boolean isGzipCompressed(String filename) {
    String extension = PathUtil.parseExtension(filename);
    return extension.equalsIgnoreCase("gz") || extension.equalsIgnoreCase("svgz");
  }

}
