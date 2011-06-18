/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2004-09 Ben Fry and Casey Reas
 Copyright (c) 2001-04 Massachusetts Institute of Technology

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
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * 
 */
public class LibraryManager {

  JFrame dialog;

  LibraryListPanel libraryListPane;
  
  // the calling editor, so updates can be applied

  Editor editor;

  public LibraryManager() {

    // setup dialog for the prefs

    // dialog = new JDialog(editor, "Preferences", true);
    dialog = new JFrame("Library Manager");
    dialog.setResizable(true);

    Base.setIcon(dialog);

    Container pane = dialog.getContentPane();
    pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
    pane.setBackground(Color.red);
    
    libraryListPane = new LibraryListPanel(fetchLibraryInfo());
    
    final JScrollPane scrollPane = new JScrollPane();
    scrollPane.setViewportView(libraryListPane);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    pane.add(scrollPane);
    scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
    scrollPane.getViewport().addChangeListener(new ChangeListener() {
      
      public void stateChanged(ChangeEvent ce) {
        
        int width = scrollPane.getViewportBorderBounds().width;
        libraryListPane.setWidth(width);
      }
    });
    
    dialog.setMinimumSize(new Dimension(400, 400));
    dialog.pack();
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    dialog.setLocation((screen.width - dialog.getWidth()) / 2,
                       (screen.height - dialog.getHeight()) / 2);
  }
  
  /**
   * Returns the presumed name of a library by looking at its filename. For
   * example,
   *   "/path/to/helpfullib.zip" -> "helpfullib"
   *   "helpfullib-0.1.1.plb" -> "helpfullib-0.1.1"
   */
  protected static String guessLibraryName(String filePath) {
    String[] paths = filePath.split("/");
    if (paths.length != 0) {
      String fileName = paths[paths.length - 1];
      int lastDot = fileName.lastIndexOf(".");
      return fileName.substring(0, lastDot);
    }
    
    return null;
  }
  
  protected File downloadLibrary(URL url) {
    try {
      String libName = guessLibraryName(url.getFile());
      if (libName != null) {
        File tmpFolder = Base.createTempFolder(libName, "download");
        
        File libFile = new File(tmpFolder, libName + ".plb");
        libFile.setWritable(true);
      
        if (downloadFile(url, libFile)) {
          return libFile;
        }
      }
    } catch (IOException e) {
      Base.showError("Trouble creating temporary folder",
                     "Could not create a place to store libraries being downloaded.\n" +
                     "That's gonna prevent us from continuing.", e);
    }
    
    return null;
  }

  /**
   * Installs the given library file to the active sketchbook. The contents of
   * the library are extracted to a temporary folder before being moved.
   */
  protected int installLibrary(File libFile) {
    try {
      String libName = guessLibraryName(libFile.getPath());

      File tmpFolder = Base.createTempFolder(libName, "uncompressed");
      unzip(libFile, tmpFolder);
      
      return installLibraries(Library.list(tmpFolder));
    } catch (IOException e) {
      Base.showError("Trouble creating temporary folder",
           "Could not create a place to store libary's uncompressed contents.\n"
         + "That's gonna prevent us from continuing.", e);
    }
    
    return 0;
  }
  
  protected int installLibraries(ArrayList<Library> newLibs) {
    ArrayList<Library> oldLibs = editor.getMode().contribLibraries;

    // Remove any libraries that are already installed.
    Iterator<Library> it = newLibs.iterator();
    while (it.hasNext()) {
      Library lib = it.next();

      // XXX: We need to dynamically load the libraries or restart the PDE for
      // this to work properly. For now, files will be clobbered if the same
      // library is installed twice without restarting the PDE.
      for (Library oldLib : oldLibs) {
        if (oldLib.getName().equals(lib.getName())) {
          System.err.println("A library by the name " + oldLib.getName() + " is already installed.");
          it.remove();
          break;
        }
      }
    }
    
    for (Library newLib : newLibs) {
      String libFolderName = newLib.folder.getName();
      newLib.folder.renameTo(new File(editor.getBase().getSketchbookLibrariesFolder(),
                                      libFolderName));
    }
    
    return newLibs.size();
  }

  /**
   * Returns true if the file was successfully downloaded, false otherwise
   */
  protected boolean downloadFile(URL source, File dest) {
    try {
      URLConnection urlConn = source.openConnection();
      urlConn.setConnectTimeout(1000);
      urlConn.setReadTimeout(1000);

      // String expectedType1 = "application/x-zip-compressed";
      // String expectedType2 = "application/zip";
      // String type = urlConn.getContentType();
      // if (expectedType1.equals(type) || expectedType2.equals(type)) {
      // }

      int fileSize = urlConn.getContentLength();
      InputStream in = urlConn.getInputStream();
      FileOutputStream out = new FileOutputStream(dest);

      byte[] b = new byte[256];
      int bytesDownloaded = 0, len;
      while ((len = in.read(b)) != -1) {
        @SuppressWarnings("unused")
        int progress = (int) (100.0 * bytesDownloaded / fileSize);
        // System.out.println("Downloaded " + progress + "%");
        out.write(b, 0, len);
        bytesDownloaded += len;
      }
      out.close();
      // System.out.println("Done!");

      return true;

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return false;
  }
  
  public static void unzip(File zipFile, File dest) {
    try {
      FileInputStream fis = new FileInputStream(zipFile);
      CheckedInputStream checksum = new CheckedInputStream(fis, new Adler32());
      ZipInputStream zis = new ZipInputStream(new BufferedInputStream(checksum));
      ZipEntry next = null;
      while ((next = zis.getNextEntry()) != null) {
        File currentFile = new File(dest, next.getName());
        if (next.isDirectory()) {
          currentFile.mkdir();
        } else {
          currentFile.createNewFile();
          unzipEntry(zis, currentFile);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void unzipEntry(ZipInputStream zin, File f) throws IOException {
    FileOutputStream out = new FileOutputStream(f);
    byte[] b = new byte[512];
    int len = 0;
    while ((len = zin.read(b)) != -1) {
      out.write(b, 0, len);
    }
    out.close();
  }
  
  protected void showFrame(Editor editor) {
    this.editor = editor;
    dialog.setVisible(true);
  }

  public int confirmAndInstallLibrary(Editor editor, File libFile) {
    this.editor = editor;
    
    String prompt = "Install libraries from " + libFile.getName() + "?  ";

    int result = JOptionPane.showConfirmDialog(this.editor, prompt, "Install",
                                               JOptionPane.YES_NO_OPTION,
                                               JOptionPane.QUESTION_MESSAGE);
    
    if (result == 0) {
      return installLibrary(libFile);
    }
    
    return 0;
  }
  
  /**
   * Placeholder function which returns a list of information on libraries.
   */
  public ArrayList<LibraryInfo> fetchLibraryInfo() {
    ArrayList<LibraryInfo> libInfos = new ArrayList<LibraryInfo>();
    
    LibraryInfo libInfo = new LibraryInfo();
    libInfo.name = "BlobDetection";
    libInfo.briefOverview = "Performs the computer vision technique of finding \"blobs\" in an image.";
    libInfo.isInstalled = true;
    libInfo.tags.add("blob");
    libInfo.tags.add("vision");
    libInfo.tags.add("image");
    libInfos.add(libInfo);
    
    libInfo = new LibraryInfo();
    libInfo.name = "OpenCV";
    libInfo.briefOverview = "An OpenCV implementation for processing including blob detection, face recognition and more. This library is highly recommended.";
    libInfo.isInstalled = false;
    libInfo.tags.add("face");
    libInfo.tags.add("vision");
    libInfo.tags.add("image");
    libInfos.add(libInfo);
    
    libInfo = new LibraryInfo();
    libInfo.name = "SQLibrary";
    libInfo.briefOverview = "A library to facilitate communication with MySQL or SQLite databases.";
    libInfo.isInstalled = false;
    libInfo.tags.add("database");
    libInfos.add(libInfo);
    
    return libInfos;
  }

  static class LibraryInfo {
    String name;
    String briefOverview;
    String version;
    String url;
    ArrayList<String> tags;
    boolean isInstalled;
    
    public LibraryInfo() {
      tags = new ArrayList<String>();
      isInstalled = false;
    }
  }
}
