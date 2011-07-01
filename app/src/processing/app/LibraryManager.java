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
import java.util.List;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.jna.*;

import processing.app.LibraryListPanel.PreferredViewPositionListener;
import processing.app.LibraryListing.LibraryListFetcher;

abstract class JProgressMonitor extends AbstractProgressMonitor {
  JProgressBar progressBar;
  
  public JProgressMonitor(JProgressBar progressBar) {
    this.progressBar = progressBar;
  }
  
  public void startTask(String name, int maxValue) {
    isFinished = false;
    progressBar.setString(name);
    progressBar.setIndeterminate(maxValue == UNKNOWN);
    progressBar.setMaximum(maxValue);
  }
  
  public void setProgress(int value) {
    super.setProgress(value);
    progressBar.setValue(value);
  }
  
  @Override
  public void finished() {
    super.finished();
    finishedAction();
  }

  public abstract void finishedAction();
  
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
  
  static final String ANY_CATEGORY = "Any";

  JFrame dialog;

  LibraryListing libraryListing;
  
  // Non-simple UI widgets:
  FilterField filterField;
  
  LibraryListPanel libraryListPane;
  
  JComboBox categoryChooser;
  
  String category;
  
  // the calling editor, so updates can be applied

  Editor editor;
  
  JProgressBar installProgressBar;
  
  public LibraryManager() {

    dialog = new JFrame("Library Manager");
    
    Base.setIcon(dialog);
    
    createComponents();
    
    registerDisposeListeners();
    
    dialog.pack();
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    dialog.setLocation((screen.width - dialog.getWidth()) / 2,
                       (screen.height - dialog.getHeight()) / 2);
    
    libraryListPane.grabFocus();
    
  }
  
  private void createComponents() {
    dialog.setResizable(true);
    
    Container pane = dialog.getContentPane();
    pane.setLayout(new GridBagLayout());
    
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 2;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    filterField = new FilterField();
    
    pane.add(filterField, c);
   
    libraryListPane = new LibraryListPanel(this, libraryListing);
    if (libraryListing == null) {
      JProgressBar progressBar = libraryListPane.getSetupProgressBar();
      getLibraryListing(progressBar);
    }
    
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
    scrollPane.getViewport().setOpaque(true);
    
    scrollPane.getViewport().setBackground(libraryListPane.getBackground());
    
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    pane.add(scrollPane, c);
    //pane.add(scrollPane, c);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
    libraryListPane.setPreferredViewPositionListener(new PreferredViewPositionListener() {

      public void handlePreferredLocation(Point p) {
        scrollPane.getViewport().setViewPosition(p);
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
    
    categoryChooser = new JComboBox();
    updateCategoryChooser();
    pane.add(categoryChooser, c);
    categoryChooser.addItemListener(new ItemListener() {
      
      public void itemStateChanged(ItemEvent e) {
        category = (String) categoryChooser.getSelectedItem();
        if (ANY_CATEGORY.equals(category)) {
          category = null;
        }
        
        libraryListPane.filterLibraries(category, filterField.filters);
      }
    });
    
    dialog.setMinimumSize(new Dimension(550, 400));
  }

  private void updateCategoryChooser() {
    ArrayList<String> categories;
    if (libraryListing != null) {
      categoryChooser.removeAllItems();
      categories = new ArrayList<String>(libraryListing.getCategories());
      Collections.sort(categories);
      categories.add(0, ANY_CATEGORY);
    } else {
      categories = new ArrayList<String>();
      categories.add(0, ANY_CATEGORY);
    }
    for (String s : categories) {
      categoryChooser.addItem(s);
    }
  }

  private void registerDisposeListeners() {
    dialog.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        disposeFrame();
      }
    });
    ActionListener disposer = new ActionListener() {
      public void actionPerformed(ActionEvent actionEvent) {
        disposeFrame();
      }
    };
    Base.registerWindowCloseKeys(dialog.getRootPane(), disposer);
    
    // handle window closing commands for ctrl/cmd-W or hitting ESC.
    
    dialog.getContentPane().addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        //System.out.println(e);
        KeyStroke wc = Base.WINDOW_CLOSE_KEYSTROKE;
        if ((e.getKeyCode() == KeyEvent.VK_ESCAPE) ||
            (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
          disposeFrame();
        }
      }
    });
  }

  protected void showFrame(Editor editor) {
    this.editor = editor;
    dialog.setVisible(true);
  }

  /**
   * Close the window after an OK or Cancel.
   */
  protected void disposeFrame() {
    dialog.dispose();
  }


  /**
   * @return true if the library listing has already been downloaded
   */
  public boolean hasLibraryListing() {
    return libraryListing != null;
  }
  
  private LibraryListing getLibraryListing(JProgressBar progressBar) {
    if (libraryListing == null) {
      final LibraryListFetcher llf = new LibraryListFetcher();
      llf.setProgressMonitor(new JProgressMonitor(progressBar) {
        
        @Override
        public void finishedAction() {
          libraryListing = llf.getLibraryListing();
          synchronized (libraryListing) {
            libraryListing = llf.getLibraryListing();
            if (libraryListPane != null) {
              libraryListPane.setLibraryList(libraryListing);
            }
            updateCategoryChooser();
          }
        }
      });
      new Thread(llf).start();
      
    }
    
    return libraryListing;
  }

  public void installLibraryFromUrl(URL url,
                                    JProgressMonitor downloadProgressMonitor,
                                    JProgressMonitor installProgressMonitor) {
    
    File libDest = getTemporaryFile(url);
    
    FileDownloader downloader = new FileDownloader(url, libDest,
                                                   downloadProgressMonitor);
    
    downloader.setPostOperation(new LibraryInstaller(downloader,
                                                     installProgressMonitor));

    new Thread(downloader).start();
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
   * Installs the given library file to the active sketchbook. The contents of
   * the library are extracted to a temporary folder before being moved.
   */
  protected int installLibrary(File libFile) {
    try {
      String libName = guessLibraryName(libFile);

      File tmpFolder = Base.createTempFolder(libName, "uncompressed");
      unzip(libFile, tmpFolder);
      
      return installLibraries(Library.list(tmpFolder));
    } catch (IOException e) {
      Base.showWarning("Trouble creating temporary folder",
           "Could not create a place to store libary's uncompressed contents,\n" + 
           "so it won't be installed.", e);
    }
    
    return 0;
  }
  
  protected File getTemporaryFile(URL url) {
    try {
      File tmpFolder = Base.createTempFolder("library", "download");
      
      String[] segments = url.getFile().split("/");
      File libFile = new File(tmpFolder, segments[segments.length - 1]);
      libFile.setWritable(true);
      
      return libFile;
    } catch (IOException e) {
      Base.showError("Trouble creating temporary folder",
                     "Could not create a place to store libraries being downloaded.\n" +
                     "That's gonna prevent us from continuing.", e);
    }
    
    return null;
  }

  /**
   * Returns the presumed name of a library by looking at its filename. For
   * example,
   *   "/path/to/helpfullib.zip" -> "helpfullib"
   *   "helpfullib-0.1.1.plb" -> "helpfullib-0.1.1"
   */
  protected static String guessLibraryName(File libFile) {
    String path = libFile.getPath();
    int lastSeparator = path.lastIndexOf(File.separatorChar);
    
    String fileName;
    if (lastSeparator != -1) {
      fileName = path.substring(lastSeparator + 1);
    } else {
      fileName = path;
    }
    
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot != -1) {
      return fileName.substring(0, lastDot);
    }
    
    return fileName;
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
    
    File backupFolder = null;
    if (!libsToBeBackuped.isEmpty()) {
      backupFolder = new File(editor.getBase().getSketchbookLibrariesFolder(),
          "old");
      if (!backupFolder.exists() || !backupFolder.isDirectory()) {
        if (!backupFolder.mkdirs()) {
          Base.showWarning("Trouble creating folder to store old libraries in",
                           "Could not create folder " + backupFolder.getAbsolutePath() + ".\n"
                           + "That's gonna prevent us from replacing the library.", null);
          return 0;
        }
      }
    }
    
    // Backup libraries
    for (Library lib : libsToBeBackuped) {
      String libFolderName = lib.folder.getName();
      
      String prefix = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
      final String backupName = prefix + "_" + libFolderName;
      File backupFolderForLib = getUniqueName(backupFolder, backupName);
      
      // XXX: Windows does not like to move this folder because it is still in use
      boolean success = false;
      success = lib.folder.renameTo(backupFolderForLib);
      
      if (!success) {
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
   * Returns a file in the parent folder that does not exist yet. If
   * parent/fileName already exists, this will look for parent/fileName(2)
   * then parent/fileName(3) and so forth.
   * 
   * @return a file that does not exist yet
   */
  public static File getUniqueName(File parentFolder, String fileName) {
    File backupFolderForLib;
    int i = 1;
    do {
      String folderName = fileName;
      if (i >= 2) {
        folderName += "(" + i + ")";
      }
      i++;
      
      backupFolderForLib = new File(parentFolder, folderName);
    } while (backupFolderForLib.exists());
    
    return backupFolderForLib;
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
          currentFile.mkdirs();
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

  class FilterField extends JTextField {
    
    final static String filterHint = "Filter your search...";

    boolean isShowingHint;
    
    List<String> filters;
    
    public FilterField () {
      super(filterHint);
      
      isShowingHint = true;
      
      filters = new ArrayList<String>();
      
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
      
      getDocument().addDocumentListener(new DocumentListener() {
        
        public void removeUpdate(DocumentEvent e) {
          filter();
        }
        
        public void insertUpdate(DocumentEvent e) {
          filter();
        }
        
        public void changedUpdate(DocumentEvent e) {
          filter();
        }
        
        void filter() {
          String filter = filterField.getFilterText();
          filter = filter.toLowerCase();
          
          // Replace anything but 0-9 or a-z with a space
          filter = filter.replaceAll("[^\\x30-\\x39^\\x61-\\x7a]", " ");
          filters = Arrays.asList(filter.split(" "));
          libraryListPane.filterLibraries(category, filters);
        }
      });
    }
    
    public String getFilterText() {
      return isShowingHint ? "" : getText();
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

  class LibraryInstaller implements Runnable {
    
    ProgressMonitor progressMonitor;
    
    FileDownloader fileDownloader;
    
    public LibraryInstaller(FileDownloader downloader, ProgressMonitor pm) {
      if (pm == null) {
        progressMonitor = new NullProgressMonitor();
      } else {
        progressMonitor = pm;
      }
      fileDownloader = downloader;
    }
    
    public void run() {
  
      File libFile = fileDownloader.getFile();
      
      if (libFile != null) {
        progressMonitor.startTask("Installing", ProgressMonitor.UNKNOWN);
        
        installLibrary(libFile);
      }
      
      dialog.pack();
      
      progressMonitor.finished();
    }
  }
  
}
