/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2005-06 Ben Fry and Casey Reas

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

package processing.app;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import com.apple.mrj.*;
import com.ice.jni.registry.*;

import processing.core.*;


/**
 * Threaded class to check for updates in the background.
 * <P>
 * This is the class that handles the mind control and stuff for
 * spying on our users and stealing their personal information.
 * A random ID number is generated for each user, and hits the server
 * to check for updates. Also included is the operating system and
 * its version and the version of Java being used to run Processing.
 * <P>
 * The ID number also helps provide us a general idea of how many
 * people are using Processing, which helps us when writing grant
 * proposals and that kind of thing so that we can keep Processing free.
 */
public class UpdateCheck extends JDialog implements ActionListener, Runnable {
  Editor editor;
  public static final String downloadURL = "http://mobile.processing.org/download/latest.txt";
  
  public static final String coreURL        = "http://mobile.processing.org/download/mobile.jar";
  public static final String coreVersion    = "http://mobile.processing.org/download/mobile.properties";
  
  public static final String libURL         = "http://mobile.processing.org/download/libraries/";
  public static final String libVersion     = "version.properties";
  boolean cancelled = false;
  boolean outOfDate = false;  
  JLabel label;
  JButton action;

  static final long ONE_DAY = 24 * 60 * 60 * 1000;

  public UpdateCheck(Editor editor) {
    super(editor);
    this.editor = editor;
    Thread thread = new Thread(this);
    thread.start();
  }
  
  public boolean isOutOfDate() {
      return outOfDate;
  }
  
  public void show() {
      setTitle("Checking for updates...");
      setModal(true);
      
      JPanel panel;
      label = new JLabel("Connecting to server...");
      action = new JButton("Cancel");
      action.addActionListener(this);
      
      Container content = getContentPane();      
      panel = new JPanel();
      panel.add(label);
      content.add(panel, BorderLayout.CENTER);      
      panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      panel.add(action);
      content.add(panel, BorderLayout.SOUTH);      
      pack();
      
      Rectangle bounds = editor.getBounds();
      setLocation(bounds.x + ((bounds.width - 400) >> 1), bounds.y + ((bounds.height - 100) >> 1));
      setSize(400, 100);
      setResizable(false);
      
      setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent we) {
              actionPerformed(null);
          }
      });
      
      super.show();
  }
  
  public void actionPerformed(ActionEvent ae) {
      cancelled = true;
      dispose();
  }
  
  public void setMessage(String message) {
      if (label != null) {
          label.setText(message);
          repaint();
      }
  }


  public void run() {
    //System.out.println("checking for updates...");

    // generate a random id in case none exists yet
    Random r = new Random();
    long id = r.nextLong();

    String idString = Preferences.get("update.id");
    if (idString != null) {
      id = Long.parseLong(idString);
    } else {
      Preferences.set("update.id", String.valueOf(id));
    }

    String info =
      URLEncoder.encode(id + "\t" +
                        PApplet.nf(Base.VERSION, 4) + "\t" +
                        System.getProperty("java.version") + "\t" +
                        System.getProperty("java.vendor") + "\t" +
                        System.getProperty("os.name") + "\t" +
                        System.getProperty("os.version") + "\t" +
                        System.getProperty("os.arch"));

    try {
      setMessage("Checking latest version of PDE...");
      int latest = readInt(downloadURL + "?" + info);
      if (cancelled) {
          return;
      }

      String lastString = Preferences.get("update.last");
      long now = System.currentTimeMillis();
      if (lastString != null) {
        long when = Long.parseLong(lastString);
        if (now - when < ONE_DAY) {
          // don't annoy the shit outta people
          return;
        }
      }
      Preferences.set("update.last", String.valueOf(now));

      String prompt =
        "A new version of Mobile Processing is available,\n" +
        "would you like to visit the Mobile Processing download page?";

      if (latest > Base.VERSION) {
        outOfDate = true;
        Object[] options = { "Yes", "No" };
        int result = JOptionPane.showOptionDialog(editor,
                                                  prompt,
                                                  "Update",
                                                  JOptionPane.YES_NO_OPTION,
                                                  JOptionPane.QUESTION_MESSAGE,
                                                  null,
                                                  options,
                                                  options[0]);

        if (result == JOptionPane.YES_OPTION) {
          Base.openURL("http://mobile.processing.org/download/");
          dispose();
          return;
        }
      }
      if (cancelled) {
          return;
      }
      //// check for updated core jar
      setMessage("Checking latest version of mobile core library...");
      checkCore();
      if (cancelled) {
          return;
      }
      //// now do library update check
      setMessage("Checking latest version of libraries...");
      HashMap versions = new HashMap();
      readLibraryVersions(versions);
      if (cancelled) {
          return;
      }
      //// reconcile versions
      compareLibraryVersions(versions);
      if (cancelled) {
          return;
      }
      //// download any updates
      downloadLibraries(versions);
      
      if (action != null) {
        action.setText("OK");
      }
      if (!outOfDate) {
        setMessage("No updates found.");
      } else {
        setMessage("Update check complete. Please restart Mobile Processing.");
      }
    } catch (Exception e) {
      if (action != null) {
        action.setText("OK");
      }
      setMessage("An error occurred while checking for updates.  Please try again later.");
      e.printStackTrace();
    }
  }
  
  protected void checkCore() throws Exception {
      BufferedReader reader = null;
      try {
          URL url = new URL(coreVersion);
          reader = new BufferedReader(new InputStreamReader(url.openStream()));
          String line = reader.readLine();
          int pos, serverVersion = -1;
          while (line != null) {
              pos = line.indexOf('=');
              if (!line.startsWith("#") && (pos >= 0)) {
                  serverVersion = Integer.parseInt(line.substring(line.indexOf('=') + 1).trim());
                  break;
              }
              line = reader.readLine();
          }
          reader.close();
          if (serverVersion >= 0) {
              //// now read local version
              reader = new BufferedReader(new InputStreamReader(Base.getStream("mobile.properties")));
              line = reader.readLine();
              int localVersion = Integer.parseInt(line.substring(line.indexOf('=') + 1).trim());
              if (localVersion < serverVersion) {
                  outOfDate = true;
                  //// backup old version
                  File f = new File("lib/mobile.jar");
                  File backup = new File("lib/mobile_bak.jar");
                  //// delete any old leftover backup
                  if (backup.exists()) {
                      backup.delete();
                  }
                  Base.copyFile(f, backup);
                  //// now delete core
                  f.delete();
                  //// download new
                  url = new URL(coreURL);
                  writeStreamToFile(url.openStream(), f);
                  //// also version properties
                  File version = new File("lib/mobile.properties");
                  File versionBackup = new File("lib/mobile_bak.properties");
                  if (versionBackup.exists()) {
                      versionBackup.delete();
                  }
                  Base.copyFile(version, versionBackup);
                  url = new URL(coreVersion);
                  writeStreamToFile(url.openStream(), version);
                  //// delete backups
                  backup.deleteOnExit();
                  versionBackup.deleteOnExit();
              }
          }
      } finally {
          if (reader != null) {
              reader.close();
          }
      }
  }
  
  protected void readLibraryVersions(HashMap versions) throws Exception {
      BufferedReader reader = null;
      try {
          URL url = new URL(libURL + libVersion);
          reader = new BufferedReader(new InputStreamReader(url.openStream()));
          String line = reader.readLine();
          String lib, version;
          int pos;
          while (line != null) {
              pos = line.indexOf('=');
              if (!line.startsWith("#") && (pos > 0)) {
                  lib = line.substring(0, pos).trim();
                  version = line.substring(pos + 1).trim();
                  versions.put(lib, version);
              }
              line = reader.readLine();
          }
      } finally {
          if (reader != null) {
              reader.close();
          }
      }
  }
  
  protected void compareLibraryVersions(HashMap versions) throws Exception {
      BufferedReader reader = null;
      try {
          Iterator keys = versions.keySet().iterator();
          String lib;
          int localVersion, serverVersion;
          while (keys.hasNext()) {
              lib = (String) keys.next();
              serverVersion = Integer.parseInt((String) versions.get(lib));
              //// open and parse local library version
              File f = new File(Sketchbook.librariesFolder, lib + File.separator + libVersion);
              if (f.exists()) {
                  reader = new BufferedReader(new FileReader(f));
                  //// assume that first line MUST be build version
                  String version = reader.readLine();
                  reader.close();
                  if (version != null) {
                      localVersion = Integer.parseInt(version.substring(version.indexOf('=') + 1).trim());
                  } else {
                      localVersion = 0;
                  }
                  if (localVersion < serverVersion) {
                      //// mark for download
                      versions.put(lib, new Boolean(true));                  
                      outOfDate = true;
                  } else {
                      versions.put(lib, new Boolean(false));
                  }
              } else {
                  //// mark for download
                  versions.put(lib, new Boolean(true));
                  outOfDate = true;
              }
          }
      } finally {
          if (reader != null) {
              reader.close();
          }
      }
  }
  
  protected void downloadLibraries(HashMap versions) throws Exception {
      Iterator keys = versions.keySet().iterator();
      String lib;
      boolean updated;
      while (keys.hasNext()) {
          lib = (String) keys.next();
          updated = ((Boolean) versions.get(lib)).booleanValue();
          if (cancelled) {
              return;
          }
          if (updated) {
              setMessage("Downloading new " + lib + " library...");
              File f = new File(Sketchbook.librariesFolder, lib + ".zip");
              //// check if any previous download exists, and delete if so
              if (f.exists()) {
                  f.delete();
              }
              URL url = new URL(libURL + lib + ".zip");
              writeStreamToFile(url.openStream(), f);
              
              //// rename old library folder as a backup
              File folder = new File(Sketchbook.librariesFolder, lib);
              File backup = new File(Sketchbook.librariesFolder, lib + "_bak");
              if (folder.exists()) { 
                  if (!folder.renameTo(backup)) {
                      throw new Exception("Directory rename failed.");
                  }
              }
              //// create library folder
              if (!folder.mkdir()) {
                  throw new Exception("Directory create failed.");
              }
              setMessage("Extracting " + lib + " library...");
              //// extract files into folder
              ZipFile zip = new ZipFile(f);
              Enumeration e = zip.entries();
              ZipEntry ze;
              while (e.hasMoreElements()) {
                  ze = (ZipEntry) e.nextElement();
                  File zf = new File(Sketchbook.librariesFolder, lib + File.separator + ze.getName());
                  if (ze.isDirectory()) {
                      zf.mkdirs();
                  } else {
                      writeStreamToFile(zip.getInputStream(ze), zf);
                  }
              }
              //// delete zip file
              f.deleteOnExit();
              //// delete backup
              if (backup.exists()) {
                  Base.removeDir(backup);
              }
          }
      }      
  }
  
  protected static void writeStreamToFile(InputStream is, File f) throws Exception {
      BufferedInputStream bis = new BufferedInputStream(is);
      FileOutputStream os = new FileOutputStream(f);
      byte[] buffer = new byte[4096];
      int bytesRead = bis.read(buffer);
      while (bytesRead >= 0) {
          os.write(buffer, 0, bytesRead);
          bytesRead = bis.read(buffer);
      }
      os.close();
      bis.close();
  }

  protected int readInt(String filename) throws Exception {
    URL url = new URL(filename);
    InputStream stream = url.openStream();
    InputStreamReader isr = new InputStreamReader(stream);
    BufferedReader reader = new BufferedReader(isr);
    return Integer.parseInt(reader.readLine());
  }
}
