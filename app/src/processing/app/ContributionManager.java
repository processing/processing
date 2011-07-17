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

import processing.app.Contribution.ContributionInfo;
import processing.app.ContributionListing.ContributionListFetcher;

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
  
  private static final String DISCOVERY_NONE_FOUND_ERROR_MESSAGE =
      "Maybe it's just us, but it looks like there are no\n"
    + "libraries in the file we just downloaded.\n";
  
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
    ArrayList<LibraryCompilation> compilations = LibraryCompilation.list(libraries);

    for (LibraryCompilation compilation : compilations) {
      for (Library lib : compilation.libraries) {
        libraries.remove(lib);
      }
    }
    
    ArrayList<ContributionInfo> infoList = new ArrayList<ContributionInfo>();
    for (Library library : libraries) {
      infoList.add(library.info);
    }
    for (LibraryCompilation compilation : compilations) {
      infoList.add(compilation.info);
    }
    
    contributionListing.updateInstalledList(infoList);
  }
 
  public void removeContribution(final Contribution contribution,
                                 final JProgressMonitor pm) {

    new Thread(new Runnable() {
      
      public void run() {
        pm.startTask("Removing", ProgressMonitor.UNKNOWN);
        if (contribution != null) {
          if (backupContribution(contribution)) {
            ContributionInfo advertisedVersion = contributionListing
                .getAdvertisedContribution(contribution.getInfo().name, contribution.getInfo().getType());
            
            if (advertisedVersion == null) {
              contributionListing.removeContribution(contribution.getInfo());
            } else {
              contributionListing.replaceContribution(contribution.getInfo(), advertisedVersion);
            }
          }
        }
        refreshInstalled();
        pm.finished();
      }
    }).start();

  }
  
  public void downloadAndInstall(URL url,
                                  final ContributionInfo info,
                                  final ContributionInstaller installOperation,
                                  final JProgressMonitor downloadProgressMonitor,
                                  final JProgressMonitor installProgressMonitor) {
    
    File libDest = getTemporaryFile(url);
    
    final FileDownloader downloader = new FileDownloader(url, libDest,
                                                         downloadProgressMonitor);

    downloader.setPostOperation(new Runnable() {

      public void run() {

        File libFile = downloader.getFile();

        if (libFile != null) {
          installProgressMonitor.startTask("Installing",
                                           ProgressMonitor.UNKNOWN);

          Contribution contribution = installOperation.installContribution(libFile);
          
          if (contribution != null) {
            contributionListing.getInformationFromAdvertised(contribution.getInfo());
            contributionListing.replaceContribution(info, contribution.getInfo());
            refreshInstalled();
          }
          
        }

        dialog.pack();

        installProgressMonitor.finished();
      }
    });

    new Thread(downloader).start();
  }
  
  protected LibraryCompilation installLibraryCompilation(File f) {
    File parentDir = unzipFileToTemp(f);
    
    LibraryCompilation compilation = LibraryCompilation.create(parentDir);

    if (compilation == null) {
      Base.showWarning(DISCOVERY_ERROR_TITLE,
                       DISCOVERY_NONE_FOUND_ERROR_MESSAGE, null);
      return null;
    }
      
    String folderName = compilation.info.name;
    
    File libraryDestination = editor.getBase().getSketchbookLibrariesFolder();
    File dest = new File(libraryDestination, folderName);
    
    // XXX: Check for conflicts with other library names, etc.
    boolean errorEncountered = false;
    if (dest.exists()) {
      if (!dest.delete()) {
        // Problem
        errorEncountered = true;
      }
    }
    
    if (!errorEncountered) {
      // Install it, return it
      if (parentDir.renameTo(dest)) {
        compilation.folder = dest;
        return compilation;
      }
    }
    
    return null;
  }
  
  public Library confirmAndInstallLibrary(Editor editor, File libFile) {
    this.editor = editor;
    
    int result = Base.showYesNoQuestion(this.editor, "Install",
                             "Install libraries from " + libFile.getName() + "?",
                             DRAG_AND_DROP_SECONDARY);
    
    if (result == JOptionPane.YES_OPTION) {
      return installLibrary(libFile, true);
    }
    
    return null;
  }

  /**
   * Creates a temporary folder and unzips a file to a subdirectory of the temp
   * folder. The subdirectory is the only file of the tempo folder.
   * 
   * e.g. if the contents of foo.zip are /hello and /world, then the resulting
   * files will be
   *     /tmp/foo9432423uncompressed/foo/hello
   *     /tmp/foo9432423uncompress/foo/world
   * ...and "/tmp/id9432423uncompress/foo/" will be returned.
   * 
   * @return the folder where the zips contents have been unzipped to (the
   *         subdirectory of the temp folder).
   */
  private static File unzipFileToTemp(File libFile) {
    
    String fileName = getFileName(libFile);
    File tmpFolder = null;
    
    try {
      tmpFolder = Base.createTempFolder(fileName, "uncompressed");
      tmpFolder = new File(tmpFolder, fileName);
      tmpFolder.mkdirs();
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
  
  protected Library installLibrary(File libFile, boolean confirmReplace) {
    File tempDir = unzipFileToTemp(libFile);
    
    try {
      ArrayList<Library> discoveredLibs = Library.list(tempDir);
      if (discoveredLibs.isEmpty()) {
        // Sometimes library authors place all their folders in the base
        // directory of a zip file instead of in single folder as the
        // guidelines suggest. If this is the case, we might be able to find the
        // library by stepping up a directory and searching for libraries again.
        discoveredLibs = Library.list(tempDir.getParentFile());
      }
      
      if (discoveredLibs != null && discoveredLibs.size() == 1) {
        Library discoveredLib = discoveredLibs.get(0);
        if (installLibrary(discoveredLib, confirmReplace)) {
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
                           DISCOVERY_NONE_FOUND_ERROR_MESSAGE, null);
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

  /**
   * 
   * @param confirmReplace
   *          if true and the library is already installed, opens a prompt to
   *          ask the user if it's okay to replace the library. If false, the
   *          library is always replaced with the new copy.
   */
  protected boolean installLibrary(Library newLib, boolean confirmReplace) {
    
    ArrayList<Library> oldLibs = editor.getMode().contribLibraries;
    
    String libFolderName = newLib.folder.getName();
    
    File libraryDestination = editor.getBase().getSketchbookLibrariesFolder();
    File newLibDest = new File(libraryDestination, libFolderName);
    
    boolean doInstall = true;
    
    for (Library oldLib : oldLibs) {
      
      // XXX: Handle other cases when installing libraries.
      //   -What if a library by the same name is already installed?
      //   -What if newLibDest exists, but isn't used by an existing library?
      if (oldLib.folder.exists() && oldLib.folder.equals(newLibDest)) {
        
        int result = 0;
        if (confirmReplace) {
          result = Base.showYesNoQuestion(editor, "Replace",
                 "Replace existing \"" + oldLib.getName() + "\" library?",
                 "An existing copy of the \"" + oldLib.getName() + "\" library<br>"+
                 "has been found in your sketchbook. Clicking “Yes”<br>"+
                 "will move the existing library to a backup folder<br>" +
                 " in <i>libraries/old</i> before replacing it.");
        }
        if (!confirmReplace || result == JOptionPane.YES_OPTION) {
          if (!backupContribution(oldLib)) {
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
   * Moves the given contribution to a backup folder.
   */
  private boolean backupContribution(Contribution lib) {
    
    File backupFolder = null;
    
    switch (lib.getInfo().getType()) {
    case LIBRARY:
    case LIBRARY_COMPILATION:
      backupFolder = createLibraryBackupFolder();
      break;
    case MODE:
    case TOOL:
      break;
    }
    
    if (backupFolder == null) return false;
    
    String libFolderName = lib.getFolder().getName();
    
    String prefix = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    final String backupName = prefix + "_" + libFolderName;
    File backupFolderForLib = getUniqueName(backupFolder, backupName);
    
//    try {
//      FileUtils.moveDirectory(lib.folder, backupFolderForLib);
//      return true;
    if (lib.getFolder().renameTo(backupFolderForLib)) {
      return true;
    } else {
//    } catch (IOException e) {
      Base.showWarning("Trouble creating backup of old \"" + lib.getInfo().name + "\" library",
                       "Could not move library to backup folder:\n"
                           + backupFolderForLib.getAbsolutePath(), null);
      return false;
    }
  }

  /**
   * @return false if there was an error creating the backup folder, true if it
   *         already exists or was created successfully
   */
  private File createLibraryBackupFolder() {
    
    File libraryBackupFolder = new File(editor.getBase()
        .getSketchbookLibrariesFolder(), "old");

    if (!libraryBackupFolder.exists() || !libraryBackupFolder.isDirectory()) {
      if (!libraryBackupFolder.mkdirs()) {
        Base.showWarning("Trouble creating folder to store old libraries in",
                         "Could not create folder "
                             + libraryBackupFolder.getAbsolutePath()
                             + ".\n"
                             + "That's gonna prevent us from replacing the library.",
                         null);
        return null;
      }
    }
    
    return libraryBackupFolder;
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
          
          // Replace anything but 0-9, a-z, or : with a space
          filter = filter.replaceAll("[^\\x30-\\x39^\\x61-\\x7a^\\x3a]", " ");
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

  interface ContributionInstaller {
    /**
     * Installs a contribution contained in the given zipped file.
     * 
     * @param zippedFile
     *          zip file containing a contribution. Never null.
     * @return the contribution if it was installed. Null otherwise.
     */
    public Contribution installContribution(File zippedFile);
    
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
