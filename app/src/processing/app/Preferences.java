/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2014 The Processing Foundation

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

import java.awt.*;
import java.io.*;
import java.util.*;

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

  static HashMap<String, String> defaults;
  static HashMap<String, String> table = new HashMap<String, String>();
  static File preferencesFile;

  static final String PROMPT_YES     = Language.text("prompt.yes");
  static final String PROMPT_NO      = Language.text("prompt.no");
  static final String PROMPT_CANCEL  = Language.text("prompt.cancel");
  static final String PROMPT_OK      = Language.text("prompt.ok");
  static final String PROMPT_BROWSE  = Language.text("prompt.browse");

  /**
   * Standardized width for buttons. Mac OS X 10.3 wants 70 as its default,
   * Windows XP needs 66, and my Ubuntu machine needs 80+, so 80 seems proper.
   */
  static public int BUTTON_WIDTH =
    Integer.parseInt(Language.text("preferences.button.width"));

  // Indents and spacing standards. These probably need to be modified
  // per platform as well, because Mac OS X is so huge, Windows is smaller,
  // and Linux is all over the map. Consider these deprecated.

  static final int GUI_BIG     = 13;
  static final int GUI_BETWEEN = 8;
  static final int GUI_SMALL   = 6;


  static public void init() {
    // start by loading the defaults, in case something
    // important was deleted from the user prefs
    try {
      // Name changed for 2.1b2 to avoid problems with users modifying or
      // replacing the file after doing a search for "preferences.txt".
      load(Base.getLibStream(DEFAULTS_FILE));
    } catch (Exception e) {
      Base.showError(null, "Could not read default settings.\n" +
                           "You'll need to reinstall Processing.", e);
    }

    /* provisionally removed in 3.0a6, see changes in load()

    // check for platform-specific properties in the defaults
    String platformExt = "." + PConstants.platformNames[PApplet.platform]; //$NON-NLS-1$
    int platformExtLength = platformExt.length();

    // Get a list of keys that are specific to this platform
    ArrayList<String> platformKeys = new ArrayList<String>();
    for (String key : table.keySet()) {
      if (key.endsWith(platformExt)) {
        platformKeys.add(key);
      }
    }

    // Use those platform-specific keys to override
    for (String key : platformKeys) {
      // this is a key specific to a particular platform
      String actualKey = key.substring(0, key.length() - platformExtLength);
      String value = get(key);
      set(actualKey, value);
    }
    */

    // Clone the defaults, then override any them with the user's preferences.
    // This ensures that any new/added preference will be present.
    defaults = new HashMap<String, String>(table);

    // other things that have to be set explicitly for the defaults
    setColor("run.window.bgcolor", SystemColor.control); //$NON-NLS-1$

    // next load user preferences file
    preferencesFile = Base.getSettingsFile(PREFS_FILE);
    if (preferencesFile.exists()) {
      try {
        load(new FileInputStream(preferencesFile));

      } catch (Exception ex) {
        Base.showError("Error reading preferences",
                       "Error reading the preferences file. " +
                       "Please delete (or move)\n" +
                       preferencesFile.getAbsolutePath() +
                       " and restart Processing.", ex);
      }
    }

    if (checkSketchbookPref() || !preferencesFile.exists()) {
      // create a new preferences file if none exists
      // saves the defaults out to the file
      save();
    }

    PApplet.useNativeSelect =
      Preferences.getBoolean("chooser.files.native"); //$NON-NLS-1$

    // So that the system proxy setting are used by default
    // https://github.com/processing/processing/issues/2643
    System.setProperty("java.net.useSystemProxies", "true");

    // Set http proxy for folks that require it.
    // http://docs.oracle.com/javase/6/docs/technotes/guides/net/proxies.html
    String proxyHost = get("proxy.host");
    String proxyPort = get("proxy.port");
    if (proxyHost != null && proxyHost.length() != 0 &&
        proxyPort != null && proxyPort.length() != 0) {
      System.setProperty("http.proxyHost", proxyHost);
      System.setProperty("http.proxyPort", proxyPort);
    }
  }


  static protected String getPreferencesPath() {
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


  static protected void save() {
    // on startup, don't worry about it
    // this is trying to update the prefs for who is open
    // before Preferences.init() has been called.
    if (preferencesFile == null) return;

    // Fix for 0163 to properly use Unicode when writing preferences.txt
    PrintWriter writer = PApplet.createWriter(preferencesFile);

    String[] keyList = table.keySet().toArray(new String[table.size()]);
    // Sorting is really helpful for debugging, diffing, and finding keys
    keyList = PApplet.sort(keyList);
    for (String key : keyList) {
      writer.println(key + "=" + table.get(key)); //$NON-NLS-1$
    }

    writer.flush();
    writer.close();
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
      Base.log("Error with font " + get(attr) + " for attribute " + attr);
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


  static protected String getSketchbookPath() {
    return get("sketchbook.path.three"); //$NON-NLS-1$
  }


  static protected void setSketchbookPath(String path) {
    set("sketchbook.path.three", path); //$NON-NLS-1$
  }
}
