/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeBase - base class for the main processing application
  Part of the Processing project - http://Proce55ing.net

  Copyright (c) 2001-03 
  Ben Fry, Massachusetts Institute of Technology and 
  Casey Reas, Interaction Design Institute Ivrea

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License 
  along with this program; if not, write to the Free Software Foundation, 
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

#ifdef MACOS
import com.apple.mrj.*;
#endif


/**
 * Primary role of this class is for platform identification and
 * general interaction with the system (launching URLs, loading 
 * files and images, etc) that comes from that.
 */
public class PdeBase /*extends JFrame implements ActionListener*/
{
  static final String VERSION = "0068 Alpha";

  //static Frame frame;  // now 'this'
  //static String encoding;
  //static Image icon;

  // indicator that this is the first time this feller has used p5
  //static boolean firstTime;

  //boolean errorState;
  PdeEditor editor;

  //WindowAdapter windowListener;

  //Menu renderMenu;
  //CheckboxMenuItem normalItem, openglItem;
  //MenuItem illustratorItem;

  // the platforms
  static final int WINDOWS = 1;
  static final int MACOS9  = 2;
  static final int MACOSX  = 3;
  static final int LINUX   = 4;
  static final int IRIX    = 5;
  static int platform;

  static final String platforms[] = {
    "", "windows", "macos9", "macosx", "linux", "irix"
  };


  static public void main(String args[]) {
    //System.getProperties().list(System.out);
    PdeBase app = new PdeBase();
  }


  public PdeBase() {

    // build the editor object

    editor = new PdeEditor();


    // figure out which operating system

    if (System.getProperty("mrj.version") != null) {  // running on a mac
      platform = (System.getProperty("os.name").equals("Mac OS X")) ?
        MACOSX : MACOS9;

    } else {
      String osname = System.getProperty("os.name");

      if (osname.indexOf("Windows") != -1) {
        platform = WINDOWS;

      } else if (osname.equals("Linux")) {  // true for the ibm vm
        platform = LINUX;

      } else if (osname.equals("Irix")) {
        platform = IRIX;

      } else {
        platform = WINDOWS;  // probably safest
        System.out.println("unhandled osname: \"" + osname + "\"");
      }
    }


    // set the look and feel before opening the window

    try {
      if (platform == LINUX) {
        // linux is by default (motif?) even uglier than metal
        // actually, i'm using native menus, so they're ugly and
        // motif-looking. ick. need to fix this.
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      } else {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
    } catch (Exception e) { 
      e.printStackTrace();
    }


    // load in preferences (last sketch used, window placement, etc)

    //preferences = new PdePreferences();


    // read in the keywords for the reference

    //final String KEYWORDS = "pde_keywords.properties";
    /*
    keywords = new Properties();

    try {
      keywords.load(PdeBase.getStream("pde_keywords.properties"));

    } catch (Exception e) {
      String message = 
        "An error occurred while loading the keywords,\n" + 
        "\"Find in reference\" will not be available.";
      JOptionPane.showMessageDialog(editor, message, 
                                    "Problem loading keywords",
                                    JOptionPane.WARNING_MESSAGE);

      System.err.println(e.toString());
      e.printStackTrace();
    }
    */

    // get things rawkin

    //editor.restorePreferences();  // done at end of constructor
    editor.pack();
    editor.show();


    //editor = new PdeEditor(this);
    //getContentPane().setLayout(new BorderLayout());
    //getContentPane().add("Center", editor);


    // load preferences and finish up

    // handle layout
    //this.pack();  // maybe this should be before the setBounds call
    // do window placement before loading sketch stuff
    //restorePreferences();

    // now that everything is set up, open last-used sketch, etc.
    //editor.restorePreferences();

    //show();
  }


  /**
   * Given the reference filename from the keywords list, 
   * builds a URL and passes it to openURL.
   */
  static public void showReference(String referenceFile) {
    String currentDir = System.getProperty("user.dir");
    openURL(currentDir + File.separator + 
            "reference" + File.separator + 
            referenceFile + ".html");
  }


  /**
   * Implements the cross-platform headache of opening URLs
   */
  static public void openURL(String url) { 
    //System.out.println("opening url " + url);
    try {
      if (platform == WINDOWS) {
        // this is not guaranteed to work, because who knows if the 
        // path will always be c:\progra~1 et al. also if the user has
        // a different browser set as their default (which would 
        // include me) it'd be annoying to be dropped into ie.
        //Runtime.getRuntime().exec("c:\\progra~1\\intern~1\\iexplore " 
        // + currentDir 

	// the following uses a shell execute to launch the .html file
        // note that under cygwin, the .html files have to be chmodded +x
        // after they're unpacked from the zip file. i don't know why,
        // and don't understand what this does in terms of windows 
        // permissions. without the chmod, the command prompt says 
        // "Access is denied" in both cygwin and the "dos" prompt.
        //Runtime.getRuntime().exec("cmd /c " + currentDir + "\\reference\\" + 
        //                    referenceFile + ".html");
        if (url.startsWith("http://")) {
          // open dos prompt, give it 'start' command, which will
          // open the url properly. start by itself won't work since
          // it appears to need cmd
          Runtime.getRuntime().exec("cmd /c start " + url);
        } else {
          // just launching the .html file via the shell works
          // but make sure to chmod +x the .html files first
          // also place quotes around it in case there's a space
          // in the user.dir part of the url
          Runtime.getRuntime().exec("cmd /c \"" + url + "\"");
        }

#ifdef MACOS
      } else if (platform == MACOSX) {
        //com.apple.eio.FileManager.openURL(url);

        if (!url.startsWith("http://")) {
          // prepend file:// on this guy since it's a file
          url = "file://" + url;

          // replace spaces with %20 for the file url
          // otherwise the mac doesn't like to open it
          // can't just use URLEncoder, since that makes slashes into
          // %2F characters, which is no good. some might say "useless"
          if (url.indexOf(' ') != -1) {
            StringBuffer sb = new StringBuffer();
            char c[] = url.toCharArray();
            for (int i = 0; i < c.length; i++) {
              if (c[i] == ' ') {
                sb.append("%20");
              } else {
                sb.append(c[i]);
              }
            }
            url = sb.toString();
          }
        }
        //System.out.println("trying to open " + url);
        com.apple.mrj.MRJFileUtils.openURL(url);

      } else if (platform == MACOS9) {
        com.apple.mrj.MRJFileUtils.openURL(url);
#endif

      } else if (platform == LINUX) {
        // how's mozilla sound to ya, laddie?
        Runtime.getRuntime().exec(new String[] { "mozilla", url });

      } else {
        System.err.println("unspecified platform");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /** 
   * Implements the other cross-platform headache of opening
   * a folder in the machine's native file browser.
   */
  static public void openFolder(File file) {
    try {
      String folder = file.getAbsolutePath();

      if (platform == WINDOWS) {
        // doesn't work
        //Runtime.getRuntime().exec("cmd /c \"" + folder + "\"");

        // works fine on winxp, prolly win2k as well
        Runtime.getRuntime().exec("explorer \"" + folder + "\"");

        // not tested
        //Runtime.getRuntime().exec("start explorer \"" + folder + "\"");

#ifdef MACOS
      } else if (platform == MACOSX) {
        openURL(folder);  // handles char replacement, etc

#endif
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * "No cookie for you" type messages. Nothing fatal or all that
   * much of a bummer, but something to notify the user about.
   */
  static public void showMessage(String title, String message) {
    if (title == null) title = "Message";
    JOptionPane.showMessageDialog(new Frame(), message, title,
                                  JOptionPane.INFORMATION_MESSAGE);
  }


  /**
   * Non-fatal error message with optional stack trace side dish.
   */
  static public void showWarning(String title, String message, 
                                 Exception e) {
    if (title == null) title = "Warning";
    JOptionPane.showMessageDialog(new Frame(), message, title,
                                  JOptionPane.WARNING_MESSAGE);

    //System.err.println(e.toString());
    if (e != null) e.printStackTrace();
  }


  /**
   * Show an error message that's actually fatal to the program.
   * This is an error that can't be recovered. Use showWarning()
   * for errors that allow P5 to continue running.
   */
  static public void showError(String title, String message, 
                               Exception e) {
    if (title == null) title = "Error";
    JOptionPane.showMessageDialog(new Frame(), message, title,
                                  JOptionPane.ERROR_MESSAGE);

    if (e != null) e.printStackTrace();
    System.exit(1);
  }


  // ...................................................................


  static public Image getImage(String name, Component who) {
    Image image = null;
    Toolkit tk = Toolkit.getDefaultToolkit();

    if ((PdeBase.platform == PdeBase.MACOSX) ||
        (PdeBase.platform == PdeBase.MACOS9)) {
      image = tk.getImage("lib/" + name);
    } else {
      image = tk.getImage(who.getClass().getResource(name));
    }

    //image =  tk.getImage("lib/" + name);
    //URL url = PdeApplet.class.getResource(name);
    //image = tk.getImage(url);
    //}
    //MediaTracker tracker = new MediaTracker(applet);
    MediaTracker tracker = new MediaTracker(who); //frame);
    tracker.addImage(image, 0);
    try {
      tracker.waitForAll();
    } catch (InterruptedException e) { }      
    return image;
  }


  static public InputStream getStream(/*Class cls,*/ String filename) 
    throws IOException {
    if ((PdeBase.platform == PdeBase.MACOSX) || 
        (PdeBase.platform == PdeBase.MACOS9)) {
      // macos doesn't seem to think that files in the lib folder
      // are part of the resources, unlike windows or linux.
      // actually, this is only the case when running as a .app, 
      // since it works fine from run.sh, but not Processing.app
      return new FileInputStream("lib/" + filename);

    } 

    // all other, more reasonable operating systems
    //return cls.getResource(filename).openStream();
    return PdeBase.class.getResource(filename).openStream();
  }


  // ...................................................................


  static public byte[] grabFile(File file) throws IOException {
    int size = (int) file.length();
    FileInputStream input = new FileInputStream(file);
    byte buffer[] = new byte[size];
    int offset = 0;
    int bytesRead;
    while ((bytesRead = input.read(buffer, offset, size-offset)) != -1) {
      offset += bytesRead;
      if (bytesRead == 0) break;
    }
    input.close();  // weren't properly being closed
    input = null;
    return buffer;
  }


  static public void copyFile(File afile, File bfile) {
    try {
      FileInputStream from = new FileInputStream(afile);
      FileOutputStream to = new FileOutputStream(bfile);
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = from.read(buffer)) != -1) {
        to.write(buffer, 0, bytesRead);
      }
      to.flush();
      from.close(); // ??
      from = null;
      to.close(); // ??
      to = null;

#ifdef JDK13
      bfile.setLastModified(afile.lastModified());  // jdk13 required
#endif
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static public void copyDir(File sourceDir, File targetDir) {
    String files[] = sourceDir.list();
    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || files[i].equals("..")) continue;
      File source = new File(sourceDir, files[i]);
      File target = new File(targetDir, files[i]);
      if (source.isDirectory()) {
        target.mkdirs();
        copyDir(source, target);
#ifdef JDK13
        target.setLastModified(source.lastModified());
#endif
      } else {
        copyFile(source, target);
      }
    }
  }


  /**
   * Remove all files in a directory and the directory itself.
   */
  static public void removeDir(File dir) {
    //System.out.println("removing " + dir);
    removeDescendants(dir);
    dir.delete();
  }


  /**
   * Recursively remove all files within a directory, 
   * used with removeDir().
   */
  static protected void removeDescendants(File dir) {
    String files[] = dir.list();
    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || files[i].equals("..")) continue;
      File dead = new File(dir, files[i]);
      if (!dead.isDirectory()) {
        if (!PdePreferences.getBoolean("compiler.save_build_files")) {
          if (!dead.delete()) {
            // temporarily disabled
            //System.err.println("couldn't delete " + dead);
          }
        }
      } else {
        removeDir(dead);
        //dead.delete();
      }
    }
  }


  /**
   * Calculate the size of the contents of a folder. 
   * Used to determine whether sketches are empty or not. 
   * Note that the function calls itself recursively.
   */
  static public int calcFolderSize(File folder) {
    int size = 0;

    //System.out.println("calcFolderSize " + folder);
    String files[] = folder.list();
    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || (files[i].equals("..")) ||
          files[i].equals(".DS_Store")) continue;
      File fella = new File(folder, files[i]);
      if (fella.isDirectory()) {
        size += calcFolderSize(fella);
      } else {
        size += (int) fella.length();
      }
    }
    return size;
  }
}
