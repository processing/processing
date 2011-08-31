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
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;

import processing.app.contribution.*;
import processing.app.contribution.Contribution.Type;

public class ContributionManager {
  
  static public final String DELETION_FLAG = "flagged_for_deletion";
  
  static private final String DOUBLE_CLICK_SECONDARY =
      "Click Ã¢â‚¬Å“YesÃ¢â‚¬Â� to install this library to your sketchbook...";
  
  static private final String DISCOVERY_INTERNAL_ERROR_MESSAGE =
        "An internal error occured while searching for contributions in the downloaded file.";
  
  static private final String DISCOVERY_NONE_FOUND_ERROR_MESSAGE =
      "Maybe it's just us, but it looks like there are no contributions in this file.";
  
  static final String ANY_CATEGORY = "Any";
  
  /** Width of each contribution icon. */
  static final int ICON_WIDTH = 25;

  /** Height of each contribution icon. */
  static final int ICON_HEIGHT = 20;

  JFrame dialog;
  
  FilterField filterField;
  
  ContributionListPanel contributionListPanel;
  
  StatusPanel statusBar;
  
  JComboBox categoryChooser;
  
  Image[] contributionIcons;
  
  // the calling editor, so updates can be applied
  Editor editor;
  
  String category;
  
  ContributionListing contribListing;
  
  /**
   * Initializes the contribution listing and fetches the advertised
   * contributions in a separate thread. This does not initialize any AWT
   * components.
   */
  public ContributionManager() {
    contribListing = new ContributionListing();
    
    contributionListPanel = new ContributionListPanel(this);
    contribListing.addContributionListener(contributionListPanel);
  }
  
  protected void showFrame(Editor editor) {
    this.editor = editor;
    
    if (dialog == null) {
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
    
    dialog.setVisible(true);
    
    if (!contribListing.hasDownloadedLatestList()) {
      contribListing.getAdvertisedContributions(new AbstractProgressMonitor() {
        public void startTask(String name, int maxValue) {
        }

        public void finished() {
          super.finished();

          updateContributionListing();
          updateCategoryChooser();
          if (isError()) {
            statusBar.setErrorMessage("An error occured when downloading " + 
                                      "the list of available contributions.");
          }
        }
      });
    }
    
    updateContributionListing();
    
    if (contributionIcons == null) {
      try {
        Image allButtons = ImageIO.read(Base.getLibStream("contributions.gif"));
        int count = allButtons.getHeight(dialog) / ContributionManager.ICON_HEIGHT;
        contributionIcons = new Image[count];
        contributionIcons[0]  = allButtons;
        contributionIcons[1]  = allButtons;
        contributionIcons[2]  = allButtons;
        contributionIcons[3]  = allButtons;
        
        for (int i = 0; i < count; i++) {
          Image image = dialog.createImage(
                            new FilteredImageSource(allButtons.getSource(),
                            new CropImageFilter(0, i * ContributionManager.ICON_HEIGHT,
                                                ContributionManager.ICON_WIDTH,
                                                ContributionManager.ICON_HEIGHT)));
          contributionIcons[i] = image;
        }
        
        contributionListPanel.updateColors();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  public Image getContributionIcon(Contribution.Type type) {
    
    if (contributionIcons == null)
      return null;
      
    switch (type) {
    case LIBRARY:
      return contributionIcons[0];
    case TOOL:
      return contributionIcons[1];
    case MODE:
      return contributionIcons[2];
    case LIBRARY_COMPILATION:
      return contributionIcons[3];
    }
    return null;
  }
  
  /**
   * Close the window after an OK or Cancel.
   */
  protected void disposeFrame() {
    dialog.dispose();
    editor = null;
  }
  
  /** Creates and arranges the Swing components in the dialog. */
  private void createComponents() {
    dialog.setResizable(true);
    
    Container pane = dialog.getContentPane();
    pane.setLayout(new GridBagLayout());
    
    { // The filter text area
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 2;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      filterField = new FilterField();

      pane.add(filterField, c);
    }
    
    { // The scroll area containing the contribution listing and the status bar.
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.gridx = 0;
      c.gridy = 1;
      c.gridwidth = 2;
      c.weighty = 1;
      c.weightx = 1;
      
      final JScrollPane scrollPane = new JScrollPane();
      scrollPane.setPreferredSize(new Dimension(300, 300));
      scrollPane.setViewportView(contributionListPanel);
      scrollPane.getViewport().setOpaque(true);
      scrollPane.getViewport().setBackground(contributionListPanel.getBackground());
      scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      
      statusBar = new StatusPanel();
      statusBar.setBorder(BorderFactory.createEtchedBorder());
      
      final JLayeredPane layeredPane = new JLayeredPane();
      layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);
      layeredPane.add(statusBar, JLayeredPane.PALETTE_LAYER);
      
      layeredPane.addComponentListener(new ComponentAdapter() {
        
        void resizeLayers() {
          scrollPane.setSize(layeredPane.getSize());
          scrollPane.updateUI();
        }
        
        public void componentShown(ComponentEvent e) {
          resizeLayers();
        }
        
        public void componentResized(ComponentEvent arg0) {
          resizeLayers();
        }
      });
      
      final JViewport viewport = scrollPane.getViewport();
      viewport.addComponentListener(new ComponentAdapter() {
        void resizeLayers() {
          statusBar.setLocation(0, viewport.getHeight() - 18);
          
          Dimension d = viewport.getSize();
          d.height = 20;
          d.width += 3;
          statusBar.setSize(d);
        }
        public void componentShown(ComponentEvent e) {
          resizeLayers();
        }
        public void componentResized(ComponentEvent e) {
          resizeLayers();
        }
      });
      
      pane.add(layeredPane, c);
    }
    
    { // Shows "Category:"
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 2;
      pane.add(new Label("Category:"), c);
    }
    
    { // Combo box for selecting a category
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridx = 1;
      c.gridy = 2;

      categoryChooser = new JComboBox();
      updateCategoryChooser();
      pane.add(categoryChooser, c);
      categoryChooser.addItemListener(new ItemListener() {

        public void itemStateChanged(ItemEvent e) {
          category = (String) categoryChooser.getSelectedItem();
          if (ContributionManager.ANY_CATEGORY.equals(category)) {
            category = null;
          }

          filterLibraries(category, filterField.filters);
        }
      });
    }
    
    dialog.setMinimumSize(new Dimension(550, 400));
  }

  private void updateCategoryChooser() {
    if (categoryChooser == null)
      return;
    
    ArrayList<String> categories;
    categoryChooser.removeAllItems();
    categories = new ArrayList<String>(contribListing.getCategories());
    Collections.sort(categories);
    categories.add(0, ContributionManager.ANY_CATEGORY);
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

  public void filterLibraries(String category, List<String> filters) {

    List<Contribution> filteredLibraries = contribListing
        .getFilteredLibraryList(category, filters);

    contributionListPanel.filterLibraries(filteredLibraries);
  }

  protected void updateContributionListing() {
    if (editor == null)
      return;
    
    ArrayList<Library> libraries = editor.getMode().contribLibraries;
    ArrayList<LibraryCompilation> compilations = LibraryCompilation.list(libraries);

    // Remove libraries from the list that are part of a compilations
    for (LibraryCompilation compilation : compilations) {
      Iterator<Library> it = libraries.iterator();
      while (it.hasNext()) {
        Library current = it.next();
        if (compilation.getFolder().equals(current.getFolder().getParentFile())) {
          it.remove();
        }
      }
    }
    
    ArrayList<Contribution> contributions = new ArrayList<Contribution>();
    contributions.addAll(editor.contribTools);
    contributions.addAll(libraries);
    contributions.addAll(compilations);
    
    contribListing.updateInstalledList(contributions);
  }
 
  /**
   * Non-blocking call to remove a contribution in a new thread.
   */
  public void removeContribution(final InstalledContribution contribution,
                                 ProgressMonitor pm) {
    if (contribution == null)
      return;
    
    final ProgressMonitor progressMonitor = pm != null ? pm : new NullProgressMonitor();
      
    new Thread(new Runnable() {
      
      public void run() {
        progressMonitor.startTask("Removing", ProgressMonitor.UNKNOWN);

        boolean doBackup = Preferences.getBoolean("contribution.backup.on_remove");
        if (ContributionManager.requiresRestart(contribution)) {
          
          if (!doBackup || (doBackup && backupContribution(contribution, false))) {
            if (ContributionManager.flagForDeletion(contribution)) {
              contribListing.replaceContribution(contribution, contribution);
            }
          }
        } else {
          if ((!doBackup && contribution.getFolder().delete())
              || (doBackup && backupContribution(contribution, true))) {
            Contribution advertisedVersion = contribListing
                .getAdvertisedContribution(contribution);

            if (advertisedVersion == null) {
              contribListing.removeContribution(contribution);
            } else {
              contribListing.replaceContribution(contribution,
                                                 advertisedVersion);
            }
          } else {
            if (doBackup) {
              
            } else {
              statusBar.setErrorMessage("Could not delete the contribution's files");
            }
          }
        }
        refreshInstalled();
        progressMonitor.finished();
      }
    }).start();

  }

  /**
   * Non-blocking call to download and install a contribution in a new thread.
   * 
   * @param url
   *          Direct link to the contribution.
   * @param toBeReplaced
   *          The Contribution that will be replaced by this library being
   *          installed (e.g. an advertised version of a contribution, or the
   *          old version of a contribution that is being updated). Must not be
   *          null.
   */
  public void downloadAndInstall(final URL url,
                                 final Contribution toBeReplaced,
                                 final JProgressMonitor downloadProgressMonitor,
                                 final JProgressMonitor installProgressMonitor) {

    final File libDest = getTemporaryFile(url);

    new Thread(new Runnable() {

      public void run() {

        FileDownloader.downloadFile(url, libDest, downloadProgressMonitor);
        
        
        if (!downloadProgressMonitor.isCanceled() && !downloadProgressMonitor.isError()) {
          
          installProgressMonitor.startTask("Installing", ProgressMonitor.UNKNOWN);
  
          InstalledContribution contribution = null;
          switch (toBeReplaced.getType()) {
          case LIBRARY:
            contribution = installLibrary(libDest, false);
            break;
          case LIBRARY_COMPILATION:
            contribution = installLibraryCompilation(libDest);
            break;
          case TOOL:
            contribution = installTool(libDest);
            break;
          }
  
          if (contribution != null) {
            // XXX contributionListing.getInformationFromAdvertised(contribution);
            // get the category at least
            contribListing.replaceContribution(toBeReplaced, contribution);
            refreshInstalled();
          }
  
          dialog.pack();
          installProgressMonitor.finished();
        }
      }
    }).start();

  }
  
  protected LibraryCompilation installLibraryCompilation(File f) {
    File parentDir = unzipFileToTemp(f);
    
    LibraryCompilation compilation = LibraryCompilation.create(parentDir);

    if (compilation == null) {
      statusBar.setErrorMessage(DISCOVERY_NONE_FOUND_ERROR_MESSAGE);
      return null;
    }
      
    String folderName = compilation.getName();
    
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
        return LibraryCompilation.create(dest);
      }
    }
    
    return null;
  }
  
  public Library confirmAndInstallLibrary(Editor editor, File libFile) {
    this.editor = editor;
    
    int result = Base.showYesNoQuestion(this.editor, "Install",
                             "Install libraries from " + libFile.getName() + "?",
                             ContributionManager.DOUBLE_CLICK_SECONDARY);
    
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
  File unzipFileToTemp(File libFile) {
    
    String fileName = ContributionManager.getFileName(libFile);
    File tmpFolder = null;
    
    try {
      tmpFolder = Base.createTempFolder(fileName, "uncompressed");
      tmpFolder = new File(tmpFolder, fileName);
      tmpFolder.mkdirs();
    } catch (IOException e) {
      statusBar.setErrorMessage("Could not create temp folder to uncompressed zip file.");
    }
    
    ContributionManager.unzip(libFile, tmpFolder);
    
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
      statusBar.setErrorMessage("Could not create a temp folder for download.");
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
  
  protected ToolContribution installTool(File zippedToolFile) {
    File tempDir = unzipFileToTemp(zippedToolFile);
    
    ArrayList<ToolContribution> discoveredTools = ToolContribution.list(tempDir, false);
    if (discoveredTools.isEmpty()) {
      // Sometimes tool authors place all their folders in the base
      // directory of a zip file instead of in single folder as the
      // guidelines suggest. If this is the case, we might be able to find the
      // library by stepping up a directory and searching for libraries again.
      discoveredTools = ToolContribution.list(tempDir.getParentFile(), false);
    }
    
    if (discoveredTools != null && discoveredTools.size() == 1) {
      ToolContribution discoveredTool = discoveredTools.get(0);
      return installTool(discoveredTool);
    } else {
      // Diagnose the problem and notify the user
      if (discoveredTools == null || discoveredTools.isEmpty()) {
        statusBar.setErrorMessage(DISCOVERY_INTERNAL_ERROR_MESSAGE);
      } else {
        statusBar.setErrorMessage("There were multiple tools in the file, so we're ignoring it.");
      }
    }
    
    return null;
  }
  
  protected ToolContribution installTool(ToolContribution newTool) {
    
    ArrayList<ToolContribution> oldTools = editor.contribTools;
    
    String toolFolderName = newTool.getFolder().getName();
    
    File toolDestination = editor.getBase().getSketchbookToolsFolder();
    File newToolDest = new File(toolDestination, toolFolderName);
    
    for (ToolContribution oldTool : oldTools) {
      
      // XXX: Handle other cases when installing tools.
      //   -What if a library by the same name is already installed?
      //   -What if newLibDest exists, but isn't used by an existing tools?
      if (oldTool.getFolder().exists() && oldTool.getFolder().equals(newToolDest)) {
        
        // XXX: We can't replace stuff, soooooo.... do something different
        if (!backupContribution(oldTool, false)) {
          return null;
        }
      }
    }
    
    // Move newTool to the sketchbook library folder
    if (newTool.getFolder().renameTo(newToolDest)) {
      ToolContribution movedTool = ToolContribution.getTool(newToolDest);
      try {
        movedTool.initializeToolClass();
        return movedTool;
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      statusBar.setErrorMessage("Could not move tool \"" + newTool.getName()
                                + "\" to sketchbook.");
    }
    
    return null;
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
        return installLibrary(discoveredLib, confirmReplace);
      } else {
        // Diagnose the problem and notify the user
        if (discoveredLibs == null) {
          statusBar.setErrorMessage(ContributionManager.DISCOVERY_INTERNAL_ERROR_MESSAGE);
        } else if (discoveredLibs.isEmpty()) {
          statusBar.setErrorMessage(ContributionManager.DISCOVERY_NONE_FOUND_ERROR_MESSAGE);
        } else {
          statusBar.setErrorMessage("There were multiple libraries in the file, so we're ignoring it.");
        }
      }
    } catch (IOException ioe) {
      statusBar.setErrorMessage(ContributionManager.DISCOVERY_INTERNAL_ERROR_MESSAGE);
    }
    
    return null;
  }

  /**
   * @param confirmReplace
   *          if true and the library is already installed, opens a prompt to
   *          ask the user if it's okay to replace the library. If false, the
   *          library is always replaced with the new copy.
   */
  protected Library installLibrary(Library newLib, boolean confirmReplace) {
    
    ArrayList<Library> oldLibs = editor.getMode().contribLibraries;
    
    String libFolderName = newLib.getFolder().getName();
    
    File libraryDestination = editor.getBase().getSketchbookLibrariesFolder();
    File newLibDest = new File(libraryDestination, libFolderName);
    
    for (Library oldLib : oldLibs) {
      
      // XXX: Handle other cases when installing libraries.
      //   -What if a library by the same name is already installed?
      //   -What if newLibDest exists, but isn't used by an existing library?
      if (oldLib.getFolder().exists() && oldLib.getFolder().equals(newLibDest)) {
        
        int result = 0;
        boolean doBackup = Preferences.getBoolean("contribution.backup.on_install");
        if (confirmReplace) {
          if (doBackup) {
            result = Base.showYesNoQuestion(editor, "Replace",
                   "Replace pre-existing \"" + oldLib.getName() + "\" library?",
                   "A pre-existing copy of the \"" + oldLib.getName() + "\" library<br>"+
                   "has been found in your sketchbook. Clicking “Yes”<br>"+
                   "will move the existing library to a backup folder<br>" +
                   " in <i>libraries/old</i> before replacing it.");
            if (result != JOptionPane.YES_OPTION || !backupContribution(oldLib, true)) {
              return null;
            }
          } else {
            result = Base.showYesNoQuestion(editor, "Replace",
                   "Replace pre-existing \"" + oldLib.getName() + "\" library?",
                   "A pre-existing copy of the \"" + oldLib.getName() + "\" library<br>"+
                   "has been found in your sketchbook. Clicking “Yes”<br>"+
                   "will permanently delete this library and all of its contents<br>"+
                   "before replacing it.");
            if (result != JOptionPane.YES_OPTION || !oldLib.getFolder().delete()) {
              return null;
            }
          }
        } else {
          if (doBackup && !backupContribution(oldLib, true)
              || !doBackup && !oldLib.getFolder().delete()) {
            return null;
          }
        }
      }
    }
    
    // Move newLib to the sketchbook library folder
    if (newLib.getFolder().renameTo(newLibDest)) {
      return new Library(newLibDest, null);
//      try {
//        FileUtils.copyDirectory(newLib.folder, libFolder);
//        FileUtils.deleteQuietly(newLib.folder);
//        newLib.folder = libFolder;
//      } catch (IOException e) {
    } else {
      statusBar.setErrorMessage("Could not move library \""
          + newLib.getName() + "\" to sketchbook.");
      return null;
    }
  }
  
  public void refreshInstalled() {
    editor.getMode().rebuildImportMenu();
    editor.rebuildToolMenu();
  }

  /**
   * Moves the given contribution to a backup folder.
   * @param doDeleteOriginal
   *          true if the file should be moved to the directory, false if it
   *          should instead be copied, leaving the original in place
   */
  private boolean backupContribution(InstalledContribution contribution,
                                     boolean doDeleteOriginal) {
    
    File backupFolder = null;
    
    switch (contribution.getType()) {
    case LIBRARY:
    case LIBRARY_COMPILATION:
      backupFolder = createLibraryBackupFolder();
      break;
    case MODE:
      break;
    case TOOL:
      backupFolder = createToolBackupFolder();
      break;
    }
    
    if (backupFolder == null) return false;
    
    String libFolderName = contribution.getFolder().getName();
    
    String prefix = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    final String backupName = prefix + "_" + libFolderName;
    File backupSubFolder = ContributionManager.getUniqueName(backupFolder, backupName);
    
//    try {
//      FileUtils.moveDirectory(lib.folder, backupFolderForLib);
//      return true;
    
    boolean success = false;
    if (doDeleteOriginal) {
      success = contribution.getFolder().renameTo(backupSubFolder);
    } else {
      try {
        Base.copyDir(contribution.getFolder(), backupSubFolder);
        success = true;
      } catch (IOException e) {
      }
    }
//    } catch (IOException e) {
    if (!success) {
      statusBar.setErrorMessage("Could not move contribution to backup folder.");
    }
    return success;
  }

  private File createLibraryBackupFolder() {
    
    File libraryBackupFolder = new File(editor.getBase()
        .getSketchbookLibrariesFolder(), "old");

    if (!libraryBackupFolder.exists() || !libraryBackupFolder.isDirectory()) {
      if (!libraryBackupFolder.mkdirs()) {
        statusBar.setErrorMessage("Could not create backup folder for library.");
        return null;
      }
    }
    
    return libraryBackupFolder;
  }
  
  private File createToolBackupFolder() {
    
    File toolsBackupFolder = new File(editor.getBase()
        .getSketchbookToolsFolder(), "old");

    if (!toolsBackupFolder.exists() || !toolsBackupFolder.isDirectory()) {
      if (!toolsBackupFolder.mkdirs()) {
        statusBar.setErrorMessage("Could not create backup folder for tool.");
        return null;
      }
    }
    
    return toolsBackupFolder;
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
          ContributionManager.unzipEntry(zis, currentFile);
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
  
  public void setFilterText(String filter) {
    if (filter == null || filter.isEmpty()) {
      filterField.setText("");
      filterField.isShowingHint = true;
    } else {
      filterField.setText(filter);
      filterField.isShowingHint = false;
    }
    filterField.applyFilter();
    
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
          applyFilter();
        }
        
        public void insertUpdate(DocumentEvent e) {
          applyFilter();
        }
        
        public void changedUpdate(DocumentEvent e) {
          applyFilter();
        }
      });
    }
    
    public void applyFilter() {
      String filter = filterField.getFilterText();
      filter = filter.toLowerCase();
      
      // Replace anything but 0-9, a-z, or : with a space
      filter = filter.replaceAll("[^\\x30-\\x39^\\x61-\\x7a^\\x3a]", " ");
      filters = Arrays.asList(filter.split(" "));
      filterLibraries(category, filters);
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

  public boolean hasAlreadyBeenOpened() {
    return dialog != null;
  }
  
  public ContributionListing getListing() {
    return contribListing;
  }
  
  static public boolean flagForDeletion(InstalledContribution contrib) {
    // Only returns false if the file already exists, so we can
    // ignore the return value.
    try {
      new File(contrib.getFolder(), ContributionManager.DELETION_FLAG).createNewFile();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  static public boolean removeFlagForDeletion(InstalledContribution contrib) {
    return new File(contrib.getFolder(), ContributionManager.DELETION_FLAG).delete();
  }
  
  static public boolean isFlaggedForDeletion(Contribution contrib) {
    if (contrib instanceof InstalledContribution) {
      InstalledContribution installed = (InstalledContribution) contrib;
      return new File(installed.getFolder(), ContributionManager.DELETION_FLAG).exists();
    }
    return false;
  }
  
  /** Returns true if the type of contribution requires the PDE to restart
   * when being removed. */
  static public boolean requiresRestart(Contribution contrib) {
    return contrib.getType() == Type.TOOL || contrib.getType() == Type.MODE;
  }

  class StatusPanel extends JPanel {
    
    String errorMessage;
    
    StatusPanel() {
      addMouseListener(new MouseAdapter() {
        
        public void mousePressed(MouseEvent e) {
          clearErrorMessage();
        }
      });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      
      g.setFont(new Font("SansSerif", Font.PLAIN, 10));
      int baseline = (getSize().height + g.getFontMetrics().getAscent()) / 2;
      
      if (contribListing.isDownloadingListing()) {
        g.setColor(Color.black);
        g.drawString("Downloading software listing...", 2, baseline);
        setVisible(true);
      } else if (errorMessage != null) {
        g.setColor(Color.red);
        g.drawString(errorMessage, 2, baseline);
        setVisible(true);
      } else {
        setVisible(false);
      }
    }
    
    void setErrorMessage(String message) {
      errorMessage = message;
      setVisible(true);
    }
    
    void clearErrorMessage() {
      errorMessage = null;
      repaint();
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