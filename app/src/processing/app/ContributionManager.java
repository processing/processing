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
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;

import processing.app.ContributionListPanel.*;
import processing.app.ContributionListing.ContributionListFetcher;

/**
 * 
 */
public class ContributionManager {
  
  private static final String DRAG_AND_DROP_SECONDARY =
      ".plb files usually contain contributed libraries for <br>" +
      "Processing. Click “Yes” to install this library to your<br>" +
      "sketchbook. If you wish to add this file to your<br>" +
      "sketch instead, click “No” and use <i>Sketch &gt;<br>Add File...</i>";
  
  private static final String DISCOVERY_ERROR_TITLE = "Trouble discovering libraries";

  private static final String DISCOVERY_INTERNAL_ERROR_MESSAGE =
        "An internal error occured while searching for libraries in the file.\n"
      + "This may be a one time error, so try again.";
  
  static final String ANY_CATEGORY = "Any";

  
  JFrame dialog;
  
  JProgressBar installProgressBar;
  
  FilterField filterField;
  
  ContributionListPanel contributionListPanel;
  
  JComboBox categoryChooser;
  
  // the calling editor, so updates can be applied
  Editor editor;
  
  String category;
  
  ContributionListing contributionListing;
  
  File backupFolder;
  
  public ContributionManager() {

    dialog = new JFrame("Contribution Manager");
    
    Base.setIcon(dialog);
    
    createComponents();
    
    registerDisposeListeners();
    
    dialog.pack();
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    dialog.setLocation((screen.width - dialog.getWidth()) / 2,
                       (screen.height - dialog.getHeight()) / 2);
    
    contributionListPanel.grabFocus();
    
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
   
    contributionListPanel = new ContributionListPanel(this, contributionListing);
    if (contributionListing == null) {
      JProgressBar progressBar = contributionListPanel.getSetupProgressBar();
      getContributionListing(progressBar);
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
    scrollPane.setViewportView(contributionListPanel);
    scrollPane.getViewport().setOpaque(true);
    
    scrollPane.getViewport().setBackground(contributionListPanel.getBackground());
    
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    pane.add(scrollPane, c);
    //pane.add(scrollPane, c);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
    contributionListPanel.setPreferredViewPositionListener(new PreferredViewPositionListener() {

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
        
        filterLibraries(category, filterField.filters);
      }
    });
    
    dialog.setMinimumSize(new Dimension(550, 400));
  }

  private void updateCategoryChooser() {
    ArrayList<String> categories;
    if (contributionListing != null) {
      categoryChooser.removeAllItems();
      categories = new ArrayList<String>(contributionListing.getCategories());
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
  public boolean hasContributionListing() {
    return contributionListing != null;
  }
  
  public void filterLibraries(String category, List<String> filters) {

    if (contributionListing != null) {

      List<ContributionInfo> filteredLibraries = contributionListing
          .getFilteredLibraryList(category, filters);

      contributionListPanel.filterLibraries(filteredLibraries);
    }
  }
  
  private void getContributionListing(JProgressBar progressBar) {
    if (contributionListing == null) {
      contributionListing = new ContributionListing();
      contributionListing.addContributionListener(contributionListPanel); 
    
      final ContributionListFetcher llf = new ContributionListFetcher(contributionListing);
      llf.setProgressMonitor(new JProgressMonitor(progressBar) {
        
        @Override
        public void finishedAction() {
          contributionListing = llf.getContributionListing();
          synchronized (contributionListing) {
            updateContributionListing();
            updateCategoryChooser();
            
            progressBar.setVisible(false);
          }
        }
      });
      new Thread(llf).start();
    }
  }

  protected void updateContributionListing() {
    ArrayList<Library> libraries = editor.getMode().contribLibraries;
    
    ArrayList<ContributionInfo> infoList = new ArrayList<ContributionInfo>();
    for (Library library : libraries) {
      infoList.add(library.info);
    }
    
    contributionListing.updateList(infoList);
  }

  public void removeLibrary(Library library, JProgressMonitor pm) {
    
    LibraryUninstaller libUninstaller = new LibraryUninstaller(library, pm);
    
    new Thread(libUninstaller).start();

  }
  
  interface Installer {
    public boolean install(File f);
  }
  
  private void downloadAndInstall(URL url,
                                  final JProgressMonitor downloadProgressMonitor,
                                  final JProgressMonitor installProgressMonitor,
                                  final Installer installOperation) {
    
    File libDest = getTemporaryFile(url);
    
    final FileDownloader downloader = new FileDownloader(url, libDest,
                                                         downloadProgressMonitor);

    downloader.setPostOperation(new Runnable() {

      public void run() {

        File libFile = downloader.getFile();

        if (libFile != null) {
          installProgressMonitor.startTask("Installing",
                                           ProgressMonitor.UNKNOWN);

          installOperation.install(libFile);
          refreshInstalled();
        }

        dialog.pack();

        installProgressMonitor.finished();
      }
    });

    new Thread(downloader).start();
  }
  
  public void installLibraryCompilationFromUrl(URL url,
                                final LibraryCompilationPanel libPanel,
                                JProgressMonitor downloadProgressMonitor,
                                final JProgressMonitor installProgressMonitor) {
    
    downloadAndInstall(url, downloadProgressMonitor, installProgressMonitor,
                       new Installer() {
     
      public boolean install(File f) {
        String libName = getFileName(f);
        File parentDir = unzipFileToTemp(f, libName);
        
        String folderName = libPanel.info.name;
        
        File libraryDestination = editor.getBase().getSketchbookLibrariesFolder();
        File dest = new File(libraryDestination, folderName);
        
        // XXX: Check for conflicts with other library names, etc.
        boolean errorEncountered = false;
        if (dest.exists()) {
          if (!dest.delete()) {
            // Problem
          }
        }
        
        return !errorEncountered && parentDir.renameTo(dest);
      }
    });
  }
  
  public void installLibraryFromUrl(URL url,
                                    final LibraryPanel libPanel,
                                    JProgressMonitor downloadProgressMonitor,
                                    final JProgressMonitor installProgressMonitor) {
    
    downloadAndInstall(url, downloadProgressMonitor, installProgressMonitor,
                       new Installer() {

      public boolean install(File f) {
        Library installedLib = installLibrary(f);
        
        if (installedLib != null) {
          contributionListing.replaceLibrary(libPanel.info, installedLib.info);
          libPanel.info = installedLib.info;
          return true;
        }
        
        return false;
      }
    });
  }

  public Library confirmAndInstallLibrary(Editor editor, File libFile) {
    this.editor = editor;
    
    int result = Base.showYesNoQuestion(this.editor, "Install",
                             "Install libraries from " + libFile.getName() + "?",
                             DRAG_AND_DROP_SECONDARY);
    
    if (result == JOptionPane.YES_OPTION) {
      return installLibrary(libFile);
    }
    
    return null;
  }

  /**
   * Unzips a file to a temporary folder.
   * 
   * @return the folder where the zips contents have been unzipped to.
   */
  private static File unzipFileToTemp(File libFile, String id) {
    File tmpFolder = null;
    
    try {
      tmpFolder = Base.createTempFolder(id, "uncompressed");
    } catch (IOException e) {
      Base.showWarning("Trouble creating temporary folder",
           "Could not create a place to store libary's uncompressed contents,\n" + 
           "so it won't be installed.", e);
    }
    
    unzip(libFile, tmpFolder);
    
    return tmpFolder;
  }
  
  protected File getTemporaryFile(URL url) {
    try {
      File tmpFolder = Base.createTempFolder("library", "download");
      
      String[] segments = url.getFile().split("/");
      File libFile = new File(tmpFolder, segments[segments.length - 1]);
      libFile.setWritable(true);
      
      return libFile;
    } catch (IOException e) {
      Base.showWarning("Trouble creating temporary folder",
                       "Could not create a place to store libraries being downloaded.\n", e);
    }
    
    return null;
  }

  /**
   * Returns the name of a file without its path or extension.
   * 
   * For example,
   *   "/path/to/helpfullib.zip" returns "helpfullib"
   *   "helpfullib-0.1.1.plb" returns "helpfullib-0.1.1"
   */
  protected static String getFileName(File libFile) {
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
  
  /**
   * Sometimes library authors place all their folders in the base directory of
   * a zip file instead of in single folder as the guidelines suggest. This
   * method attempts to find the library, if this is the case, by moving the
   * contents to a new subdirectory and then searching it for libraries.
   * 
   * @return A list of discovered libraries (may be empty), or null there was an
   *         error dealing with the filesystem
   */
  private ArrayList<Library> recoverLibrary(File tempDir, String libName)
      throws IOException {
    // No libraries found. It's okay though, the author might not have not
    // read the library guidelines and placed all their folders in the base
    // directory of the their zip file. If this is the case, let's help them
    // out, rather than complaining about it.
    
    File newLibFolder = getUniqueName(tempDir, libName);
    if (newLibFolder.mkdirs()) {
      for (File f : tempDir.listFiles()) {
        if (!f.equals(newLibFolder)) {
          if (!f.renameTo(new File(newLibFolder, f.getName()))) {
            // The file wasn't moved for whatever reason
            return null;
          }
//          try {
//            FileUtils.moveDirectory(f, new File(newLibFolder, f.getName()));
//          } catch (IOException e) {
//            errorEncountered = true;
//          }
        }
      }
      return Library.list(tempDir);
    } else {
      // We couldn't make the directory to move the library to
      return null;
    }
  }
  
  protected Library installLibrary(File libFile) {
    String libName = getFileName(libFile);
    File tempDir = unzipFileToTemp(libFile, libName);
    
    try {
      ArrayList<Library> discoveredLibs = Library.list(tempDir);
      if (discoveredLibs.isEmpty()) {
        discoveredLibs = recoverLibrary(tempDir, libName);
      }
      
      if (discoveredLibs != null && discoveredLibs.size() == 1) {
        Library discoveredLib = discoveredLibs.get(0);
        if (installLibrary(discoveredLib)) {
          return discoveredLib;
        } else {
          return null;
        }
      } else {
        // Diagnose the problem and notify the user
        if (discoveredLibs == null) {
          Base.showWarning(DISCOVERY_ERROR_TITLE,
                           DISCOVERY_INTERNAL_ERROR_MESSAGE, null);
        } else if (discoveredLibs.isEmpty()) {
          Base.showWarning(DISCOVERY_ERROR_TITLE,
                           "Maybe it's just us, but it looks like there are no\n"
                         + "libraries in the file we just downloaded.\n", null);
        } else {
          Base.showWarning("Too many libraries",
                           "We found more than one library in the library file\n"
                         + "we just downloaded. That shouldn't happen, so we're\n"
                         + "going to ignore this file.", null);
        }
      }
    } catch (IOException ioe) {
      Base.showWarning(DISCOVERY_ERROR_TITLE, DISCOVERY_INTERNAL_ERROR_MESSAGE,
                       ioe);
    }
    
    return null;
  }
  
  protected boolean installLibrary(Library newLib) {
    
    ArrayList<Library> oldLibs = editor.getMode().contribLibraries;
    
    String libFolderName = newLib.folder.getName();
    
    File libraryDestination = editor.getBase().getSketchbookLibrariesFolder();
    File newLibDest = new File(libraryDestination, libFolderName);
    
    boolean doInstall = true;
    
    for (Library oldLib : oldLibs) {
      
      // XXX: Handle other cases when installing libraries.
      //   -What if a library by the same name is already installed?
      //   -What if newLibDest exists, but isn't used by an existing library?
      if (oldLib.libraryFolder.exists() && oldLib.equals(newLibDest)) {
        
        int result = Base.showYesNoQuestion(editor, "Replace",
               "Replace existing \"" + oldLib.getName() + "\" library?",
               "An existing copy of the \"" + oldLib.getName() + "\" library<br>"+
               "has been found in your sketchbook. Clicking “Yes”<br>"+
               "will move the existing library to a backup folder<br>" +
               " in <i>libraries/old</i> before replacing it.");
        
        if (result == JOptionPane.YES_OPTION) {
          if (!backupLibrary(oldLib)) {
            return false;
          }
        } else {
          doInstall = false;
        }
      }
    }
    
    if (doInstall) {
      // Move newLib to the sketchbook library folder
      if (newLib.folder.renameTo(newLibDest)) {
        newLib.folder = newLibDest;
        return true;
//      try {
//        FileUtils.copyDirectory(newLib.folder, libFolder);
//        FileUtils.deleteQuietly(newLib.folder);
//        newLib.folder = libFolder;
//      } catch (IOException e) {
      } else {
        Base.showWarning("Trouble moving new library to the sketchbook",
                         "Could not move library \"" + newLib.getName() + "\" to "
                             + newLibDest.getAbsolutePath() + ".\n", null);
      }
    }
    
    return false;
  }
  
  public void refreshInstalled() {
    editor.getMode().rebuildLibraryList();
    editor.getMode().rebuildImportMenu();
  }

  /**
   * Moves the given library to a backup folder.
   */
  private boolean backupLibrary(Library lib) {
    if (!createBackupFolder()) {
      return false;
    }
    
    String libFolderName = lib.folder.getName();
    
    String prefix = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    final String backupName = prefix + "_" + libFolderName;
    File backupFolderForLib = getUniqueName(backupFolder, backupName);
    
//    try {
//      FileUtils.moveDirectory(lib.folder, backupFolderForLib);
//      return true;
    if (lib.folder.renameTo(backupFolderForLib)) {
      return true;
    } else {
//    } catch (IOException e) {
      Base.showWarning("Trouble creating backup of old \"" + lib.getName() + "\" library",
                       "Could not move library to backup folder:\n"
                           + backupFolderForLib.getAbsolutePath(), null);
      return false;
    }
  }

  /**
   * @return false if there was an error creating the backup folder, true if it
   *         already exists or was created successfully
   */
  private boolean createBackupFolder() {
    if (backupFolder != null)
      return true;
    
    backupFolder = new File(editor.getBase().getSketchbookLibrariesFolder(),
                            "old");
    if (!backupFolder.exists() || !backupFolder.isDirectory()) {
      if (!backupFolder.mkdirs()) {
        Base.showWarning("Trouble creating folder to store old libraries in",
                         "Could not create folder "
                             + backupFolder.getAbsolutePath()
                             + ".\n"
                             + "That's gonna prevent us from replacing the library.",
                         null);
        return false;
      }
    }
    
    return true;
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
          filterLibraries(category, filters);
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

  class LibraryUninstaller implements Runnable {

    Library library;

    ProgressMonitor pm;

    public LibraryUninstaller(Library library, ProgressMonitor pm) {
      this.library = library;
      this.pm = pm;
    }

    public void run() {
      pm.startTask("Removing", ProgressMonitor.UNKNOWN);
      if (library != null) {
        if (backupLibrary(library)) {
          ContributionInfo advertisedVersion = contributionListing
              .getAdvertisedContribution(library.info.name);
          
          if (advertisedVersion == null) {
            contributionListing.removeLibrary(library.info);
          } else {
            contributionListing.replaceLibrary(library.info, advertisedVersion);
          }
        }
      }
      refreshInstalled();
      pm.finished();
    }
    
  }
  
}

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
