/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2004-11 Ben Fry and Casey Reas
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
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;

class ProgressMonitor2 extends AbstractProgressMonitor {
  JProgressBar progressBar;
  
  public ProgressMonitor2(JProgressBar progressBar) {
    this.progressBar = progressBar;
  }
  
  public void startTask(String name, int maxValue) {
    progressBar.setString(name);
    progressBar.setIndeterminate(maxValue == UNKNOWN);
    progressBar.setMaximum(maxValue);
  }
  
  public void setProgress(int value) {
    super.setProgress(value);
    progressBar.setValue(value);
  }
  
}

/**
 * 
 */
public class LibraryManager {
  
  private static final String DRAG_AND_DROP_SECONDARY =
    ".plb files usually contain contributed libraries for <br>" +
    "Processing. Click “Yes” to install this library to your<br>" +
    "sketchbook. If you wish to add this file to your<br>" +
    "sketch instead, click “No” and use <i>Sketch &gt;<br>Add File...</i>";
  
  class LibraryDownloader implements Runnable {
    URL url;
    ProgressMonitor progressMonitor;
    File libFile;
    
    public LibraryDownloader(URL url, ProgressMonitor progressMonitor) {
      this.url = url;
      this.progressMonitor = progressMonitor;
      libFile = null;
    }

    public void run() {
      libraryUrl.setEnabled(false);
      installButton.setEnabled(false);
      
      libFile = downloadLibrary(url, progressMonitor);
      
      if (libFile != null) {
        progressMonitor.startTask("Installing", ProgressMonitor.UNKNOWN);
        
        installLibrary(libFile);
      }
      
      libraryUrl.setEnabled(true);
      installButton.setEnabled(true);
      
      installProgressBar.setVisible(false);
      dialog.pack();
    }

    public File getFile() {
      return libFile;
    }
    
  }
  
  /**
   * true to use manual URL specification only
   * false to use searchable library list
   */
  static boolean USE_SIMPLE = true;
  
  JFrame dialog;

  // Simple UI widgets:
  JLabel urlLabel;

  JTextField libraryUrl;

  JButton installButton;
  
  JProgressBar installProgressBar;
  
  // Non-simple UI widgets:
  FilterField filterField;
  
  LibraryListPanel libraryListPane;
  
  JComboBox categoryChooser;
  
  // the calling editor, so updates can be applied

  Editor editor;
  
  class FilterField extends JTextField {
    final static String filterHint = "Filter your search...";
    boolean isShowingHint;
    
    public FilterField () {
      super(filterHint);
      
      isShowingHint = true;
      
      addFocusListener(new FocusListener() {
        
        public void focusLost(FocusEvent focusEvent) {
          if (filterField.getText().isEmpty()) {
            isShowingHint = true;
          }
          
          updateStyle();
        }
        
        public void focusGained(FocusEvent focusEvent) {
          if (isShowingHint) {
            isShowingHint = false;
            filterField.setText("");
          }
          
          updateStyle();
        }
      });
    }
    
    public void updateStyle() {
      if (isShowingHint) {
        filterField.setText(filterHint);
        
        // setForeground(UIManager.getColor("TextField.light")); // too light
        setForeground(Color.gray);
      } else {
        setForeground(UIManager.getColor("TextField.foreground"));
      }
    }
  }

  public LibraryManager() {

    // setup dialog for the prefs

    // dialog = new JDialog(editor, "Preferences", true);
    dialog = new JFrame("Library Manager");
    dialog.setResizable(true);

    Base.setIcon(dialog);
    
    Container pane = dialog.getContentPane();
    pane.setLayout(new GridBagLayout());
    
    GridBagConstraints c; 
    
    if (USE_SIMPLE) {
      dialog.setResizable(false);
      
      urlLabel = new JLabel("Library URL:");
      libraryUrl = new JTextField();
      installButton = new JButton("Install");
      installProgressBar = new JProgressBar();

//      Dimension d = installProgressBar.getSize();
//      d.height = 2 * pane.getFontMetrics(installProgressBar.getFont()).getHeight();
//      installProgressBar.setPreferredSize(d);
      installProgressBar.setVisible(true);
      installProgressBar.setString("");
      installProgressBar.setStringPainted(true);

      installButton.setEnabled(false);
      libraryUrl.getDocument().addDocumentListener(new DocumentListener() {
        
        private void checkUrl() {
          try {
            new URL(libraryUrl.getText());
            installButton.setEnabled(true);
            installButton.setToolTipText("");
          } catch (MalformedURLException e) {
            installButton.setEnabled(false);
            installButton.setToolTipText("URL is malformed");
          }
        }
        
        public void removeUpdate(DocumentEvent de) {
          checkUrl();
        }
        
        public void insertUpdate(DocumentEvent de) {
          checkUrl();
        }
        
        public void changedUpdate(DocumentEvent de) {
          checkUrl();
        }
      });
      
      ActionListener installLibAction = new ActionListener() {

        public void actionPerformed(ActionEvent arg) {
          try {
            installProgressBar.setVisible(true);
            dialog.pack();
            
            URL url = new URL(libraryUrl.getText());
            
            final ProgressMonitor2 pm = new ProgressMonitor2(installProgressBar);
            
            dialog.addWindowListener(new WindowAdapter() {
              
              public void windowClosing(WindowEvent we) {
                pm.cancel();
              }
            });
            
            LibraryDownloader downloader = new LibraryDownloader(url, pm);
            new Thread(downloader).start();
          } catch (MalformedURLException e) {
            System.err.println("Malformed URL");
          }
          libraryUrl.setText("");
        }
      };
      libraryUrl.addActionListener(installLibAction);
      installButton.addActionListener(installLibAction);

      c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.weightx = 1;
      c.anchor = GridBagConstraints.WEST;
      pane.add(urlLabel, c);
      
      c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 1;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      pane.add(libraryUrl, c);
      
      c = new GridBagConstraints();
      c.gridx = 1;
      c.gridy = 1;
      c.anchor = GridBagConstraints.EAST;
      pane.add(installButton, c);
      
      c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 2;
      c.gridwidth = 2;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      pane.add(installProgressBar, c);
      
      Dimension d = dialog.getSize();
      d.width = 320;
      dialog.setMinimumSize(d);
    } else {
      c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 2;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      filterField = new FilterField();
      pane.add(filterField, c);
      
      libraryListPane = new LibraryListPanel(fetchLibraryInfo());
      
      c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.gridx = 0;
      c.gridy = 1;
      c.gridwidth = 2;
      c.weighty = 1;
      c.weightx = 1;
      
      final JScrollPane scrollPane = new JScrollPane();
      scrollPane.setPreferredSize(new Dimension(300,300));
      scrollPane.setViewportView(libraryListPane);
      scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      pane.add(scrollPane, c);
      scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      
      scrollPane.getViewport().addChangeListener(new ChangeListener() {
        
        public void stateChanged(ChangeEvent ce) {
          
          int width = scrollPane.getViewportBorderBounds().width;
          libraryListPane.setWidth(width);
        }
      });
      
      c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 2;
      pane.add(new Label("Category:"), c);
      
      c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridx = 1;
      c.gridy = 2;
      
      String[] categories = {
        "3D", "Animation", "Compilations", "Computer Vision",
        "Data and Protocols", "Geometry", "Graphic Interface",
        "Hardware Interface", "Import / Export", "Math", "Simulation", "Sound",
        "Tools", "Typography", "Video" };
      categoryChooser = new JComboBox(categories);
      pane.add(categoryChooser, c);
  
      dialog.setMinimumSize(new Dimension(400, 400));
    }
    
    dialog.pack();
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    dialog.setLocation((screen.width - dialog.getWidth()) / 2,
                       (screen.height - dialog.getHeight()) / 2);
    
    if (!USE_SIMPLE) {
      libraryListPane.grabFocus();
    }
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
      if (lastDot != -1) {
        return fileName.substring(0, lastDot);
      }
    }
    
    return null;
  }
  
  protected File downloadLibrary(URL url, ProgressMonitor progressMonitor) {
    try {
      String libName = guessLibraryName(url.getFile());
      if (libName != null) {
        File tmpFolder = Base.createTempFolder(libName, "download");
        
        File libFile = new File(tmpFolder, libName + ".plb");
        libFile.setWritable(true);
        
        try {
          if (downloadFile(url, libFile, progressMonitor)) {
            return libFile;
          }
        } catch (IOException e) {
          Base.showError("Trouble downloading file",
                         "An error occured while downloading the library.", e);
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
    ArrayList<Library> libsToBeBackuped = new ArrayList<Library>();
    
    // Remove any libraries that are already installed.
    Iterator<Library> it = newLibs.iterator();
    while (it.hasNext()) {
      Library lib = it.next();

      // XXX: We need to dynamically load the libraries or restart the PDE for
      // this to work properly. For now, files will be clobbered if the same
      // library is installed twice without restarting the PDE.
      for (Library oldLib : oldLibs) {
        
        if (oldLib.getName().equals(lib.getName())) {
          
          int result = Base.showYesNoQuestion(editor, "Replace",
                 "Replace existing \"" + oldLib.getName() + "\" library?",
                 "An existing copy of the \"" + oldLib.getName() + "\" library<br>"+
                 "has been found in your sketchbook. Clicking “Yes”<br>"+
                 "will move the existing library to a backup folder<br>" +
                 " in <i>libraries/old</i> before replacing it.");
          
          if (result == JOptionPane.YES_OPTION) {
            libsToBeBackuped.add(oldLib);
          } else {
            it.remove();
          }
          break;
        }
      }
    }
    
    for (Library lib : libsToBeBackuped) {
      String libFolderName = lib.folder.getName();
      
      File backupFolder = new File(editor.getBase().getSketchbookLibrariesFolder(),
                                   "old");
      if (!backupFolder.exists() || !backupFolder.isDirectory()) {
        if (!backupFolder.mkdir()) {
          Base.showError("Trouble creating folder to store old libraries in\"" + lib.getName() + "\"",
                         "Could not create folder " + backupFolder.getAbsolutePath() + ".\n"
                         + "That's gonna prevent us from continuing.", null);
        }
      }
      
      String prefix = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
      final String backupName = prefix + "_" + libFolderName;
      File backupFolderForLib;
      int i = 1;
      do {
        String folderName = backupName;
        if (i >= 2) {
          folderName += "(" + i + ")";
        }
        i++;
        
        backupFolderForLib = new File(backupFolder, folderName);
      } while (backupFolderForLib.exists());
      
      if (!lib.folder.renameTo(backupFolderForLib)) {
        Base.showError("Trouble creating backup of old \"" + lib.getName() + "\" library",
                       "Could not move library to "
                     + backupFolderForLib.getAbsolutePath() + "\n"
                     + "That's gonna prevent us from continuing.", null);
      }
    }
    
    for (Library newLib : newLibs) {
      String libFolderName = newLib.folder.getName();
      File libFolder = new File(editor.getBase().getSketchbookLibrariesFolder(),
                                libFolderName);
      if (!newLib.folder.renameTo(libFolder)) {
        Base.showError("Trouble moving new library to the sketchbook",
                       "Could not move \"" + newLib.getName() + "\" to "
                     + libFolder.getAbsolutePath() + ".\n"
                     + "That's gonna prevent us from continuing.", null);
      }
    }
    
    return newLibs.size();
  }

  /**
   * Returns true if the file was successfully downloaded, false otherwise
   * @param progressMonitor 
   * @throws FileNotFoundException 
   */
  protected boolean downloadFile(URL source, File dest, ProgressMonitor progressMonitor) throws IOException, FileNotFoundException {
      URLConnection urlConn = source.openConnection();
      urlConn.setConnectTimeout(1000);
      urlConn.setReadTimeout(5000);

      // String expectedType1 = "application/x-zip-compressed";
      // String expectedType2 = "application/zip";
      // String type = urlConn.getContentType();
      // if (expectedType1.equals(type) || expectedType2.equals(type)) {
      // }

      int fileSize = urlConn.getContentLength();
      progressMonitor.startTask("Downloading", fileSize);
      
      InputStream in = urlConn.getInputStream();
      FileOutputStream out = new FileOutputStream(dest);

      byte[] b = new byte[256];
      int bytesDownloaded = 0, len;
      while (!progressMonitor.isCanceled() && (len = in.read(b)) != -1) {
        out.write(b, 0, len);
        bytesDownloaded += len;
        
        progressMonitor.setProgress(bytesDownloaded);
      }
      out.close();
      
      if (!progressMonitor.isCanceled()) {
        return true;
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
    
    int result = Base.showYesNoQuestion(this.editor, "Install",
                             "Install libraries from " + libFile.getName() + "?",
                             DRAG_AND_DROP_SECONDARY);
    
    if (result == JOptionPane.YES_OPTION) {
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
