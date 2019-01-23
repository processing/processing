/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2014-19 The Processing Foundation

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.awt.Color;
import java.awt.Font;
import java.awt.SystemColor;
import java.io.*;
import java.util.*;

import processing.app.ui.Toolkit;
import processing.core.*;


/**
 * Storage class for user preferences and environment settings.
 * <P>
 * This class does not use the Properties class because .properties files use
 * ISO 8859-1 encoding, which is highly likely to be a problem when trying to
 * save sketch folders and locations. Like the rest of Processing, we use UTF8.
 * <p>
 * We don't use the Java Preferences API because it would entail writing to
 * the registry (on Windows), or an obscure file location (on Mac OS X) and
 * make it far more difficult (impossible) to remove the preferences.txt to
 * reset them (when they become corrupt), or to find the the file to make
 * edits for numerous obscure preferences that are not part of the preferences
 * window. If we added a generic editor (e.g. about:config in Mozilla) for
 * such things, we could start using the Java Preferences API. But wow, that
 * sounds like a lot of work. Not unlike writing this paragraph.
 */
public class Preferences {
  // had to rename the defaults file because people were editing it
  static final String DEFAULTS_FILE = "defaults.txt"; //$NON-NLS-1$
  static final String PREFS_FILE = "preferences.txt"; //$NON-NLS-1$

  static Map<String, String> defaults;
  static Map<String, String> table = new HashMap<>();
  static File preferencesFile;


//  /** @return true if the sketchbook file did not exist */
//  static public boolean init() {
  static public void init() {
    // start by loading the defaults, in case something
    // important was deleted from the user prefs
    try {
      // Name changed for 2.1b2 to avoid problems with users modifying or
      // replacing the file after doing a search for "preferences.txt".
      load(Base.getLibStream(DEFAULTS_FILE));
    } catch (Exception e) {
      Messages.showError(null, "Could not read default settings.\n" +
                         "You'll need to reinstall Processing.", e);
    }

    // Clone the defaults, then override any them with the user's preferences.
    // This ensures that any new/added preference will be present.
    defaults = new HashMap<>(table);

    // other things that have to be set explicitly for the defaults
    setColor("run.window.bgcolor", SystemColor.control); //$NON-NLS-1$

    // For CJK users, enable IM support by default
    if (Language.useInputMethod()) {
      setBoolean("editor.input_method_support", true);
    }

    // next load user preferences file
    preferencesFile = Base.getSettingsFile(PREFS_FILE);
    boolean firstRun = !preferencesFile.exists();
    if (!firstRun) {
      try {
        load(new FileInputStream(preferencesFile));

      } catch (Exception ex) {
        Messages.showError("Error reading preferences",
                           "Error reading the preferences file. " +
                           "Please delete (or move)\n" +
                           preferencesFile.getAbsolutePath() +
                           " and restart Processing.", ex);
      }
    }

    if (checkSketchbookPref() || firstRun) {
//    if (firstRun) {
      // create a new preferences file if none exists
      // saves the defaults out to the file
      save();
    }

    PApplet.useNativeSelect =
      Preferences.getBoolean("chooser.files.native"); //$NON-NLS-1$

    // Adding option to disable this in case it's getting in the way
    if (get("proxy.system").equals("true")) {
      // Use the system proxy settings by default
      // https://github.com/processing/processing/issues/2643
      System.setProperty("java.net.useSystemProxies", "true");
    }

    // Set HTTP, HTTPS, and SOCKS proxies for individuals
    // who want/need to override the system setting
    // http://docs.oracle.com/javase/6/docs/technotes/guides/net/proxies.html
    // Less readable version with the Oracle style sheet:
    // http://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html
    handleProxy("http", "http.proxyHost", "http.proxyPort");
    handleProxy("https", "https.proxyHost", "https.proxyPort");
    handleProxy("socks", "socksProxyHost", "socksProxyPort");
  }


  static void handleProxy(String protocol, String hostProp, String portProp) {
    String proxyHost = get("proxy." + protocol + ".host");
    String proxyPort = get("proxy." + protocol + ".port");
    if (proxyHost != null && proxyHost.length() != 0 &&
        proxyPort != null && proxyPort.length() != 0) {
      System.setProperty(hostProp, proxyHost);
      System.setProperty(portProp, proxyPort);
    }

  }


  static public String getPreferencesPath() {
    return preferencesFile.getAbsolutePath();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Load a set of key/value pairs from a UTF-8 encoded file into 'table'.
   * For 3.0a6, this removes any platform-specific extensions from keys, so
   * that we don't have platform-specific entries in a user's preferences.txt
   * file, which would require all prefs to be changed twice, or risk being
   * overwritten by the unchanged platform-specific version on reload.
   */
  static public void load(InputStream input) throws IOException {
    HashMap<String, String> platformSpecific = new HashMap<>();

    String[] lines = PApplet.loadStrings(input);  // Reads as UTF-8
    for (String line : lines) {
      if ((line.length() == 0) ||
          (line.charAt(0) == '#')) continue;

      // this won't properly handle = signs being in the text
      int equals = line.indexOf('=');
      if (equals != -1) {
        String key = line.substring(0, equals).trim();
        String value = line.substring(equals + 1).trim();
        if (!isPlatformSpecific(key, value, platformSpecific)) {
          table.put(key, value);
        }
      }
    }
    // Now override the keys with any platform-specific defaults we've found.
    for (String key : platformSpecific.keySet()) {
      table.put(key, platformSpecific.get(key));
    }
  }


  /**
   * @param key original key (may include platform extension)
   * @param value
   * @param specific where to put the key/value pairs for *this* platform
   * @return true if a platform-specific key
   */
  static protected boolean isPlatformSpecific(String key, String value,
                                              Map<String, String> specific) {
    for (String platform : PConstants.platformNames) {
      String ext = "." + platform;
      if (key.endsWith(ext)) {
        String thisPlatform = PConstants.platformNames[PApplet.platform];
        if (platform.equals(thisPlatform)) {
          key = key.substring(0, key.lastIndexOf(ext));
          // store this for later overrides
          specific.put(key, value);
        } else {
          // ignore platform-specific defaults for other platforms,
          // but return 'true' because it needn't be added to the big list
        }
        return true;
      }
    }
    return false;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static public void save() {
    // On startup it'll be null, don't worry about it. It's trying to update
    // the prefs for the open sketch before Preferences.init() has been called.
    if (preferencesFile != null) {
      try {
        File dir = preferencesFile.getParentFile();
        File preferencesTemp = File.createTempFile("preferences", ".txt", dir);
        preferencesTemp.setWritable(true, false);

        // Fix for 0163 to properly use Unicode when writing preferences.txt
        PrintWriter writer = PApplet.createWriter(preferencesTemp);

        String[] keyList = table.keySet().toArray(new String[table.size()]);
        // Sorting is really helpful for debugging, diffing, and finding keys
        keyList = PApplet.sort(keyList);
        for (String key : keyList) {
          writer.println(key + "=" + table.get(key)); //$NON-NLS-1$
        }
        writer.flush();
        writer.close();

        // Rename preferences.txt to preferences.old
        File oldPreferences = new File(dir, "preferences.old");
        if (oldPreferences.exists()) {
          if (!oldPreferences.delete()) {
            throw new IOException("Could not delete preferences.old");
          }
        }
        if (preferencesFile.exists() &&
            !preferencesFile.renameTo(oldPreferences)) {
          throw new IOException("Could not replace preferences.old");
        }
        // Make the temporary file into the real preferences
        if (!preferencesTemp.renameTo(preferencesFile)) {
          throw new IOException("Could not move preferences file into place");
        }

      } catch (IOException e) {
        Messages.showWarning("Preferences",
                             "Could not save the Preferences file.", e);
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // all the information from preferences.txt

  static public String get(String attribute /*, String defaultValue */) {
    return table.get(attribute);
  }


  static public String getDefault(String attribute) {
    return defaults.get(attribute);
  }


  static public void set(String attribute, String value) {
    table.put(attribute, value);
  }


  static public void unset(String attribute) {
    table.remove(attribute);
  }


  static public boolean getBoolean(String attribute) {
    String value = get(attribute); //, null);
    return Boolean.parseBoolean(value);

    /*
      supposedly not needed, because anything besides 'true'
      (ignoring case) will just be false.. so if malformed -> false
    if (value == null) return defaultValue;

    try {
      return (new Boolean(value)).booleanValue();
    } catch (NumberFormatException e) {
      System.err.println("expecting an integer: " + attribute + " = " + value);
    }
    return defaultValue;
    */
  }


  static public void setBoolean(String attribute, boolean value) {
    set(attribute, value ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
  }


  static public int getInteger(String attribute /*, int defaultValue*/) {
    return Integer.parseInt(get(attribute));

    /*
    String value = get(attribute, null);
    if (value == null) return defaultValue;

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      // ignored will just fall through to returning the default
      System.err.println("expecting an integer: " + attribute + " = " + value);
    }
    return defaultValue;
    //if (value == null) return defaultValue;
    //return (value == null) ? defaultValue :
    //Integer.parseInt(value);
    */
  }


  static public void setInteger(String key, int value) {
    set(key, String.valueOf(value));
  }


  static public Color getColor(String name) {
    Color parsed = Color.GRAY;  // set a default
    String s = get(name);
    if ((s != null) && (s.indexOf("#") == 0)) { //$NON-NLS-1$
      try {
        parsed = new Color(Integer.parseInt(s.substring(1), 16));
      } catch (Exception e) { }
    }
    return parsed;
  }


  static public void setColor(String attr, Color what) {
    set(attr, "#" + PApplet.hex(what.getRGB() & 0xffffff, 6)); //$NON-NLS-1$
  }


  // Identical version found in Settings.java
  static public Font getFont(String attr) {
    try {
      boolean replace = false;
      String value = get(attr);
      if (value == null) {
        // use the default font instead
        value = getDefault(attr);
        replace = true;
      }

      String[] pieces = PApplet.split(value, ',');

      if (pieces.length != 3) {
        value = getDefault(attr);
        pieces = PApplet.split(value, ',');
        replace = true;
      }

      String name = pieces[0];
      int style = Font.PLAIN;  // equals zero
      if (pieces[1].indexOf("bold") != -1) { //$NON-NLS-1$
        style |= Font.BOLD;
      }
      if (pieces[1].indexOf("italic") != -1) { //$NON-NLS-1$
        style |= Font.ITALIC;
      }
      int size = PApplet.parseInt(pieces[2], 12);

      // replace bad font with the default from lib/preferences.txt
      if (replace) {
        set(attr, value);
      }

      if (!name.startsWith("processing.")) {
        return new Font(name, style, size);

      } else {
        if (pieces[0].equals("processing.sans")) {
          return Toolkit.getSansFont(size, style);
        } else if (pieces[0].equals("processing.mono")) {
          return Toolkit.getMonoFont(size, style);
        }
      }

    } catch (Exception e) {
      // Adding try/catch block because this may be where
      // a lot of startup crashes are happening.
      Messages.log("Error with font " + get(attr) + " for attribute " + attr);
    }
    return new Font("Dialog", Font.PLAIN, 12);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Check for a 3.0 sketchbook location, and if none exists,
   * try to grab it from the 2.0 sketchbook location.
   * @return true if a location was found and the pref didn't exist
   */
  static protected boolean checkSketchbookPref() {
    // If a 3.0 sketchbook location has never been inited
    if (getSketchbookPath() == null) {
      String twoPath = get("sketchbook.path");
      // If they've run the 2.0 version, start with that location
      if (twoPath != null) {
        setSketchbookPath(twoPath);
        return true;  // save the sketchbook right away
      }
      // Otherwise it'll be null, and reset properly by Base
    }
    return false;
  }


  static public String getOldSketchbookPath() {
    return get("sketchbook.path");
  }


  static public String getSketchbookPath() {
    return get("sketchbook.path.three"); //$NON-NLS-1$
  }


  static protected void setSketchbookPath(String path) {
    set("sketchbook.path.three", path); //$NON-NLS-1$
  }
}
