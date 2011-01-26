/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-10 Ben Fry and Casey Reas
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

import processing.core.*;

import java.awt.*;
import java.io.*;

import javax.swing.*;


/**
 * Stores information about files in the current sketch
 */
public class Sketch {
  private Editor editor;
  private Mode mode;

  /** main pde file for this sketch. */
  private File primaryFile;

  /**
   * Name of sketch, which is the name of main file
   * (without .pde or .java extension)
   */
  private String name;

  /** true if any of the files have been modified. */
  private boolean modified;

  /** folder that contains this sketch */
  private File folder;

  /** data folder location for this sketch (may not exist yet) */
  private File dataFolder;

  /** code folder location for this sketch (may not exist yet) */
  private File codeFolder;

  private SketchCode current;
  private int currentIndex;
  /**
   * Number of sketchCode objects (tabs) in the current sketch. Note that this
   * will be the same as code.length, because the getCode() method returns
   * just the code[] array, rather than a copy of it, or an array that's been
   * resized to just the relevant files themselves.
   * http://dev.processing.org/bugs/show_bug.cgi?id=940
   */
  private int codeCount;
  private SketchCode[] code;

//  /** Class path determined during build. */
//  private String classPath;
//
//  /**
//   * This is *not* the "Processing" libraries path, this is the Java libraries
//   * path, as in java.library.path=BlahBlah, which identifies search paths for
//   * DLLs or JNILIBs. (It's Java's LD_LIBRARY_PATH, for you UNIX fans.)
//   */
//  private String javaLibraryPath;
//
//  /**
//   * List of library folders, set up in the preprocess() method.
//   */
//  private ArrayList<Library> importedLibraries;
//  //private ArrayList<File> importedLibraries;

  /** 
   * Most recent, default build path. This will contain the .java files that
   * have been preprocessed, as well as any .class files that were compiled. 
   */
//  private File buildFolder;
  
  
  /**
   * path is location of the main .pde file, because this is also
   * simplest to use when opening the file from the finder/explorer.
   */
  public Sketch(Editor editor, String path) throws IOException {
    this.editor = editor;
    this.mode = editor.getMode();

    primaryFile = new File(path);

    // get the name of the sketch by chopping .pde or .java
    // off of the main file name
    String mainFilename = primaryFile.getName();
    int suffixLength = mode.getDefaultExtension().length() + 1;
    name = mainFilename.substring(0, mainFilename.length() - suffixLength);

    // lib/build must exist when the application is started
    // it is added to the CLASSPATH by default, but if it doesn't
    // exist when the application is started, then java will remove
    // the entry from the CLASSPATH, causing Runner to fail.
    //
    /*
    tempBuildFolder = new File(TEMP_BUILD_PATH);
    if (!tempBuildFolder.exists()) {
      tempBuildFolder.mkdirs();
      Base.showError("Required folder missing",
                        "A required folder was missing from \n" +
                        "from your installation of Processing.\n" +
                        "It has now been replaced, please restart    \n" +
                        "the application to complete the repair.", null);
    }
    */
//    tempBuildFolder = Base.getBuildFolder();
    //Base.addBuildFolderToClassPath();

    folder = new File(new File(path).getParent());
    //System.out.println("sketch dir is " + folder);

    load();
  }


  /**
   * Build the list of files.
   * <P>
   * Generally this is only done once, rather than
   * each time a change is made, because otherwise it gets to be
   * a nightmare to keep track of what files went where, because
   * not all the data will be saved to disk.
   * <P>
   * This also gets called when the main sketch file is renamed,
   * because the sketch has to be reloaded from a different folder.
   * <P>
   * Another exception is when an external editor is in use,
   * in which case the load happens each time "run" is hit.
   */
  protected void load() {
    codeFolder = new File(folder, "code");
    dataFolder = new File(folder, "data");

    // get list of files in the sketch folder
    String list[] = folder.list();

    // reset these because load() may be called after an
    // external editor event. (fix for 0099)
    codeCount = 0;

    code = new SketchCode[list.length];

    String[] extensions = mode.getExtensions();

    for (String filename : list) {
      // Ignoring the dot prefix files is especially important to avoid files
      // with the ._ prefix on Mac OS X. (You'll see this with Mac files on
      // non-HFS drives, i.e. a thumb drive formatted FAT32.)
      if (filename.startsWith(".")) continue;

      // Don't let some wacko name a directory blah.pde or bling.java.
      if (new File(folder, filename).isDirectory()) continue;

      // figure out the name without any extension
      String base = filename;
      // now strip off the .pde and .java extensions
      for (String extension : extensions) {
        if (base.toLowerCase().endsWith("." + extension)) {
          base = base.substring(0, base.length() - (extension.length() + 1));

          // Don't allow people to use files with invalid names, since on load,
          // it would be otherwise possible to sneak in nasty filenames. [0116]
          if (Sketch.isSanitaryName(base)) {
            code[codeCount++] =
              new SketchCode(new File(folder, filename), extension);
          }
        }
      }
    }
    // Remove any code that wasn't proper
    code = (SketchCode[]) PApplet.subset(code, 0, codeCount);

    // move the main class to the first tab
    // start at 1, if it's at zero, don't bother
    for (int i = 1; i < codeCount; i++) {
      //if (code[i].file.getName().equals(mainFilename)) {
      if (code[i].getFile().equals(primaryFile)) {
        SketchCode temp = code[0];
        code[0] = code[i];
        code[i] = temp;
        break;
      }
    }

    // sort the entries at the top
    sortCode();

    // set the main file to be the current tab
    if (editor != null) {
      setCurrentCode(0);
    }
  }


  protected void replaceCode(SketchCode newCode) {
    for (int i = 0; i < codeCount; i++) {
      if (code[i].getFileName().equals(newCode.getFileName())) {
        code[i] = newCode;
        break;
      }
    }
  }


  protected void insertCode(SketchCode newCode) {
    // make sure the user didn't hide the sketch folder
    ensureExistence();

    // add file to the code/codeCount list, resort the list
    //if (codeCount == code.length) {
    code = (SketchCode[]) PApplet.append(code, newCode);
    codeCount++;
    //}
    //code[codeCount++] = newCode;
  }


  protected void sortCode() {
    // cheap-ass sort of the rest of the files
    // it's a dumb, slow sort, but there shouldn't be more than ~5 files
    for (int i = 1; i < codeCount; i++) {
      int who = i;
      for (int j = i + 1; j < codeCount; j++) {
        if (code[j].getFileName().compareTo(code[who].getFileName()) < 0) {
          who = j;  // this guy is earlier in the alphabet
        }
      }
      if (who != i) {  // swap with someone if changes made
        SketchCode temp = code[who];
        code[who] = code[i];
        code[i] = temp;
      }
    }
  }


  boolean renamingCode;

  /**
   * Handler for the New Code menu option.
   */
  public void handleNewCode() {
    // make sure the user didn't hide the sketch folder
    ensureExistence();

    // if read-only, give an error
    if (isReadOnly()) {
      // if the files are read-only, need to first do a "save as".
      Base.showMessage("Sketch is Read-Only",
                       "Some files are marked \"read-only\", so you'll\n" +
                       "need to re-save the sketch in another location,\n" +
                       "and try again.");
      return;
    }

    renamingCode = false;
    editor.status.edit("Name for new file:", "");
  }


  /**
   * Handler for the Rename Code menu option.
   */
  public void handleRenameCode() {
    // make sure the user didn't hide the sketch folder
    ensureExistence();

    if (currentIndex == 0 && editor.untitled) {
      Base.showMessage("Sketch is Untitled",
                       "How about saving the sketch first \n" +
                       "before trying to rename it?");
      return;
    }

    // if read-only, give an error
    if (isReadOnly()) {
      // if the files are read-only, need to first do a "save as".
      Base.showMessage("Sketch is Read-Only",
                       "Some files are marked \"read-only\", so you'll\n" +
                       "need to re-save the sketch in another location,\n" +
                       "and try again.");
      return;
    }

    // ask for new name of file (internal to window)
    // TODO maybe just popup a text area?
    renamingCode = true;
    String prompt = (currentIndex == 0) ?
      "New name for sketch:" : "New name for file:";
    String oldName = (current.isExtension("pde")) ?
      current.getPrettyName() : current.getFileName();
    editor.status.edit(prompt, oldName);
  }


  /**
   * This is called upon return from entering a new file name.
   * (that is, from either newCode or renameCode after the prompt)
   * This code is almost identical for both the newCode and renameCode
   * cases, so they're kept merged except for right in the middle
   * where they diverge.
   */
  protected void nameCode(String newName) {
    // make sure the user didn't hide the sketch folder
    ensureExistence();

    // Add the extension here, this simplifies some of the logic below.
    if (newName.indexOf('.') == -1) {
      newName += "." + mode.getDefaultExtension();
    }

    // if renaming to the same thing as before, just ignore.
    // also ignoring case here, because i don't want to write
    // a bunch of special stuff for each platform
    // (osx is case insensitive but preserving, windows insensitive,
    // *nix is sensitive and preserving.. argh)
    if (renamingCode) {
      if (newName.equalsIgnoreCase(current.getFileName())) {
        // exit quietly for the 'rename' case.
        // if it's a 'new' then an error will occur down below
        return;
      }
    }

    newName = newName.trim();
    if (newName.equals("")) return;

    int dot = newName.indexOf('.');
    if (dot == 0) {
      Base.showWarning("Problem with rename",
                       "The name cannot start with a period.", null);
      return;
    }

    String newExtension = newName.substring(dot+1).toLowerCase();
    if (!mode.validExtension(newExtension)) {
      Base.showWarning("Problem with rename",
                       "\"." + newExtension + "\"" +
                       "is not a valid extension.", null);
      return;
    }

    // Don't let the user create the main tab as a .java file instead of .pde
    if (!mode.isDefaultExtension(newExtension)) {
      if (renamingCode) {  // If creating a new tab, don't show this error
        if (current == code[0]) {  // If this is the main tab, disallow
          Base.showWarning("Problem with rename",
                           "The main .pde file cannot be .java file.\n" +
                           "(It may be time for your to graduate to a\n" +
                           "\"real\" programming environment)", null);
          return;
        }
      }
    }

    // dots are allowed for the .pde and .java, but not in the name
    // make sure the user didn't name things poo.time.pde
    // or something like that (nothing against poo time)
    String shortName = newName.substring(0, dot);
    String sanitaryName = Sketch.sanitizeName(shortName);
    if (!shortName.equals(sanitaryName)) {
      newName = sanitaryName + "." + newExtension;
    }

    // Make sure no .pde *and* no .java files with the same name already exist
    // http://dev.processing.org/bugs/show_bug.cgi?id=543
    for (SketchCode c : code) {
      if (sanitaryName.equalsIgnoreCase(c.getPrettyName())) {
        Base.showMessage("Nope",
                         "A file named \"" + c.getFileName() + "\" already exists\n" +
                         "in \"" + folder.getAbsolutePath() + "\"");
        return;
      }
    }

    File newFile = new File(folder, newName);
//    if (newFile.exists()) {  // yay! users will try anything
//      Base.showMessage("Nope",
//                       "A file named \"" + newFile + "\" already exists\n" +
//                       "in \"" + folder.getAbsolutePath() + "\"");
//      return;
//    }

//    File newFileHidden = new File(folder, newName + ".x");
//    if (newFileHidden.exists()) {
//      // don't let them get away with it if they try to create something
//      // with the same name as something hidden
//      Base.showMessage("No Way",
//                       "A hidden tab with the same name already exists.\n" +
//                       "Use \"Unhide\" to bring it back.");
//      return;
//    }

    if (renamingCode) {
      if (currentIndex == 0) {
        // get the new folder name/location
        String folderName = newName.substring(0, newName.indexOf('.'));
        File newFolder = new File(folder.getParentFile(), folderName);
        if (newFolder.exists()) {
          Base.showWarning("Cannot Rename",
                           "Sorry, a sketch (or folder) named " +
                           "\"" + newName + "\" already exists.", null);
          return;
        }

        // unfortunately this can't be a "save as" because that
        // only copies the sketch files and the data folder
        // however this *will* first save the sketch, then rename

        // first get the contents of the editor text area
        if (current.isModified()) {
          current.setProgram(editor.getText());
          try {
            // save this new SketchCode
            current.save();
          } catch (Exception e) {
            Base.showWarning("Error", "Could not rename the sketch. (0)", e);
            return;
          }
        }

        if (!current.renameTo(newFile, newExtension)) {
          Base.showWarning("Error",
                           "Could not rename \"" + current.getFileName() +
                           "\" to \"" + newFile.getName() + "\"", null);
          return;
        }

        // save each of the other tabs because this is gonna be re-opened
        try {
          for (int i = 1; i < codeCount; i++) {
            code[i].save();
          }
        } catch (Exception e) {
          Base.showWarning("Error", "Could not rename the sketch. (1)", e);
          return;
        }

        // now rename the sketch folder and re-open
        boolean success = folder.renameTo(newFolder);
        if (!success) {
          Base.showWarning("Error", "Could not rename the sketch. (2)", null);
          return;
        }
        // if successful, set base properties for the sketch

        File newMainFile = new File(newFolder, newName + ".pde");
        String newMainFilePath = newMainFile.getAbsolutePath();

        // having saved everything and renamed the folder and the main .pde,
        // use the editor to re-open the sketch to re-init state
        // (unfortunately this will kill positions for carets etc)
        editor.handleOpenUnchecked(newMainFilePath,
                                   currentIndex,
                                   editor.getSelectionStart(),
                                   editor.getSelectionStop(),
                                   editor.getScrollPosition());

        // get the changes into the sketchbook menu
        // (re-enabled in 0115 to fix bug #332)
        editor.base.rebuildSketchbookMenus();

      } else {  // else if something besides code[0]
        if (!current.renameTo(newFile, newExtension)) {
          Base.showWarning("Error",
                           "Could not rename \"" + current.getFileName() +
                           "\" to \"" + newFile.getName() + "\"", null);
          return;
        }
      }

    } else {  // creating a new file
      try {
        if (!newFile.createNewFile()) {
          // Already checking for IOException, so make our own.
          throw new IOException("createNewFile() returned false");
        }
      } catch (IOException e) {
        Base.showWarning("Error",
                         "Could not create the file \"" + newFile + "\"\n" +
                         "in \"" + folder.getAbsolutePath() + "\"", e);
        return;
      }
      SketchCode newCode = new SketchCode(newFile, newExtension);
      //System.out.println("new code is named " + newCode.getPrettyName() + " " + newCode.getFile());
      insertCode(newCode);
    }

    // sort the entries
    sortCode();

    // set the new guy as current
    setCurrentCode(newName);

    // update the tabs
    editor.header.rebuild();
  }


  /**
   * Remove a piece of code from the sketch and from the disk.
   */
  public void handleDeleteCode() {
    // make sure the user didn't hide the sketch folder
    ensureExistence();

    // if read-only, give an error
    if (isReadOnly()) {
      // if the files are read-only, need to first do a "save as".
      Base.showMessage("Sketch is Read-Only",
                       "Some files are marked \"read-only\", so you'll\n" +
                       "need to re-save the sketch in another location,\n" +
                       "and try again.");
      return;
    }

    // confirm deletion with user, yes/no
    Object[] options = { "OK", "Cancel" };
    String prompt = (currentIndex == 0) ?
      "Are you sure you want to delete this sketch?" :
      "Are you sure you want to delete \"" + current.getPrettyName() + "\"?";
    int result = JOptionPane.showOptionDialog(editor,
                                              prompt,
                                              "Delete",
                                              JOptionPane.YES_NO_OPTION,
                                              JOptionPane.QUESTION_MESSAGE,
                                              null,
                                              options,
                                              options[0]);
    if (result == JOptionPane.YES_OPTION) {
      if (currentIndex == 0) {
        // need to unset all the modified flags, otherwise tries
        // to do a save on the handleNew()

        // delete the entire sketch
        Base.removeDir(folder);

        // get the changes into the sketchbook menu
        //sketchbook.rebuildMenus();

        // make a new sketch, and i think this will rebuild the sketch menu
        //editor.handleNewUnchecked();
        //editor.handleClose2();
        editor.base.handleClose(editor);

      } else {
        // delete the file
        if (!current.deleteFile()) {
          Base.showMessage("Couldn't do it",
                           "Could not delete \"" +
                           current.getFileName() + "\".");
          return;
        }

        // remove code from the list
        removeCode(current);

        // just set current tab to the main tab
        setCurrentCode(0);

        // update the tabs
        editor.header.repaint();
      }
    }
  }


  protected void removeCode(SketchCode which) {
    // remove it from the internal list of files
    // resort internal list of files
    for (int i = 0; i < codeCount; i++) {
      if (code[i] == which) {
        for (int j = i; j < codeCount-1; j++) {
          code[j] = code[j+1];
        }
        codeCount--;
        code = (SketchCode[]) PApplet.shorten(code);
        return;
      }
    }
    System.err.println("removeCode: internal error.. could not find code");
  }


  /**
   * Move to the previous tab.
   */
  public void handlePrevCode() {
    int prev = currentIndex - 1;
    if (prev < 0) prev = codeCount-1;
    setCurrentCode(prev);
  }


  /**
   * Move to the next tab.
   */
  public void handleNextCode() {
    setCurrentCode((currentIndex + 1) % codeCount);
  }


  /**
   * Sets the modified value for the code in the frontmost tab.
   */
  public void setModified(boolean state) {
    //System.out.println("setting modified to " + state);
    //new Exception().printStackTrace();
    current.setModified(state);
    calcModified();
  }


  protected void calcModified() {
    modified = false;
    for (int i = 0; i < codeCount; i++) {
      if (code[i].isModified()) {
        modified = true;
        break;
      }
    }
    editor.header.repaint();

    if (Base.isMacOS()) {
      // http://developer.apple.com/qa/qa2001/qa1146.html
      Object modifiedParam = modified ? Boolean.TRUE : Boolean.FALSE;
      editor.getRootPane().putClientProperty("windowModified", modifiedParam);
    }
  }


  public boolean isModified() {
    return modified;
  }


  /**
   * Save all code in the current sketch.
   */
  public boolean save() throws IOException {
    // make sure the user didn't hide the sketch folder
    ensureExistence();

    // first get the contents of the editor text area
    if (current.isModified()) {
      current.setProgram(editor.getText());
    }

    // don't do anything if not actually modified
    //if (!modified) return false;

    if (isReadOnly()) {
      // if the files are read-only, need to first do a "save as".
      Base.showMessage("Sketch is read-only",
                       "Some files are marked \"read-only\", so you'll\n" +
                       "need to re-save this sketch to another location.");
      // if the user cancels, give up on the save()
      if (!saveAs()) return false;
    }

    for (int i = 0; i < codeCount; i++) {
      if (code[i].isModified()) code[i].save();
    }
    calcModified();
    return true;
  }


  /**
   * Handles 'Save As' for a sketch.
   * <P>
   * This basically just duplicates the current sketch folder to
   * a new location, and then calls 'Save'. (needs to take the current
   * state of the open files and save them to the new folder..
   * but not save over the old versions for the old sketch..)
   * <P>
   * Also removes the previously-generated .class and .jar files,
   * because they can cause trouble.
   */
  protected boolean saveAs() throws IOException {
    String newParentDir = null;
    String newName = null;

    /*
      JFileChooser fc = new JFileChooser();
      fc.setDialogTitle("Save sketch folder as...");
      if (isReadOnly() || isUntitled()) {
        // default to the sketchbook folder
        fc.setCurrentDirectory(new File(Preferences.get("sketchbook.path")));
      } else {
        // default to the parent folder of where this was
        fc.setCurrentDirectory(folder.getParentFile());
      }
      // can't do this, will try to save into itself by default
      //fc.setSelectedFile(folder);
      int result = fc.showSaveDialog(editor);
      if (result == JFileChooser.APPROVE_OPTION) {
        File selection = fc.getSelectedFile();
        newParentDir = selection.getParent();
        newName = selection.getName();
      }
    */

    // get new name for folder
    FileDialog fd = new FileDialog(editor,
                                   "Save sketch folder as...",
                                   FileDialog.SAVE);
    if (isReadOnly() || isUntitled()) {
      // default to the sketchbook folder
      fd.setDirectory(Preferences.get("sketchbook.path"));
    } else {
      // default to the parent folder of where this was
      fd.setDirectory(folder.getParent());
    }
    String oldName = folder.getName();
    fd.setFile(oldName);

    fd.setVisible(true);
    newParentDir = fd.getDirectory();
    newName = fd.getFile();

    // user canceled selection
    if (newName == null) return false;
    newName = Sketch.checkName(newName);

    File newFolder = new File(newParentDir, newName);
//    String newPath = newFolder.getAbsolutePath();
//    String oldPath = folder.getAbsolutePath();

//    if (newPath.equals(oldPath)) {
//      return false;  // Can't save a sketch over itself
//    }

    // make sure there doesn't exist a tab with that name already
    // but ignore this situation for the first tab, since it's probably being
    // resaved (with the same name) to another location/folder.
    for (int i = 1; i < codeCount; i++) {
      if (newName.equalsIgnoreCase(code[i].getPrettyName())) {
        Base.showMessage("Nope",
                         "You can't save the sketch as \"" + newName + "\"\n" +
                         "because the sketch already has a tab with that name.");
        return false;
      }
    }

    // check if the paths are identical
    if (newFolder.equals(folder)) {
      // just use "save" here instead, because the user will have received a
      // message (from the operating system) about "do you want to replace?"
      return save();
    }

    // check to see if the user is trying to save this sketch inside itself
    try {
      String newPath = newFolder.getCanonicalPath() + File.separator;
      String oldPath = folder.getCanonicalPath() + File.separator;

      if (newPath.indexOf(oldPath) == 0) {
        Base.showWarning("How very Borges of you",
                         "You cannot save the sketch into a folder\n" +
                         "inside itself. This would go on forever.", null);
        return false;
      }
    } catch (IOException e) { }

    // if the new folder already exists, then first remove its contents before 
    // copying everything over (user will have already been warned)
    if (newFolder.exists()) {
      Base.removeDir(newFolder);
    }
    // in fact, you can't do this on windows because the file dialog
    // will instead put you inside the folder, but it happens on osx a lot.

    // now make a fresh copy of the folder
    newFolder.mkdirs();

    // grab the contents of the current tab before saving
    // first get the contents of the editor text area
    if (current.isModified()) {
      current.setProgram(editor.getText());
    }

    File[] copyItems = folder.listFiles(new FileFilter() {
      public boolean accept(File file) {
        String name = file.getName();
        // just in case the OS likes to return these as if they're legit
        if (name.equals(".") || name.equals("..")) {
          return false;
        }
        // list of files/folders to be ignored during "save as"
        for (String ignorable : mode.getIgnorable()) {
          if (name.equals(ignorable)) {
            return false;
          }
        }
        // ignore the extensions for code, since that'll be copied below
        for (String ext : mode.getExtensions()) {
          if (name.endsWith(ext)) {
            return false;
          }
        }
        // don't do screen captures, since there might be thousands. kind of 
        // a hack, but seems harmless. hm, where have i heard that before...
        if (name.startsWith("screen-")) {
          return false;
        }
        return true;
      }
    });
    // now copy over the items that make sense
    for (File copyable : copyItems) {
      if (copyable.isDirectory()) {
        Base.copyDir(copyable, new File(newFolder, copyable.getName())); 
      } else {
        Base.copyFile(copyable, new File(newFolder, copyable.getName()));
      }
    }

    // save the other tabs to their new location
    for (int i = 1; i < codeCount; i++) {
      File newFile = new File(newFolder, code[i].getFileName());
      code[i].saveAs(newFile);
    }

    /*
    // re-copy the data folder (this may take a while.. add progress bar?)
    if (dataFolder.exists()) {
      File newDataFolder = new File(newFolder, "data");
      Base.copyDir(dataFolder, newDataFolder);
    }

    // re-copy the code folder
    if (codeFolder.exists()) {
      File newCodeFolder = new File(newFolder, "code");
      Base.copyDir(codeFolder, newCodeFolder);
    }

    // copy custom applet.html file if one exists
    // http://dev.processing.org/bugs/show_bug.cgi?id=485
    File customHtml = new File(folder, "applet.html");
    if (customHtml.exists()) {
      File newHtml = new File(newFolder, "applet.html");
      Base.copyFile(customHtml, newHtml);
    }
    */

    // save the main tab with its new name
    File newFile = new File(newFolder, newName + ".pde");
    code[0].saveAs(newFile);

    editor.handleOpenUnchecked(newFile.getPath(),
                               currentIndex,
                               editor.getSelectionStart(),
                               editor.getSelectionStop(),
                               editor.getScrollPosition());

    // Name changed, rebuild the sketch menus
    //editor.sketchbook.rebuildMenusAsync();
    editor.base.rebuildSketchbookMenus();

    // Make sure that it's not an untitled sketch
    setUntitled(false);

    // let Editor know that the save was successful
    return true;
  }


  /**
   * Prompt the user for a new file to the sketch, then call the
   * other addFile() function to actually add it.
   */
  public void handleAddFile() {
    // make sure the user didn't hide the sketch folder
    ensureExistence();

    // if read-only, give an error
    if (isReadOnly()) {
      // if the files are read-only, need to first do a "save as".
      Base.showMessage("Sketch is Read-Only",
                       "Some files are marked \"read-only\", so you'll\n" +
                       "need to re-save the sketch in another location,\n" +
                       "and try again.");
      return;
    }

    // get a dialog, select a file to add to the sketch
    String prompt =
      "Select an image or other data file to copy to your sketch";
    //FileDialog fd = new FileDialog(new Frame(), prompt, FileDialog.LOAD);
    FileDialog fd = new FileDialog(editor, prompt, FileDialog.LOAD);
    fd.setVisible(true);

    String directory = fd.getDirectory();
    String filename = fd.getFile();
    if (filename == null) return;

    // copy the file into the folder. if people would rather
    // it move instead of copy, they can do it by hand
    File sourceFile = new File(directory, filename);

    // now do the work of adding the file
    boolean result = addFile(sourceFile);

    if (result) {
      editor.statusNotice("One file added to the sketch.");
    }
  }


  /**
   * Add a file to the sketch.
   * <p/>
   * .pde or .java files will be added to the sketch folder. <br/>
   * .jar, .class, .dll, .jnilib, and .so files will all
   * be added to the "code" folder. <br/>
   * All other files will be added to the "data" folder.
   * <p/>
   * If they don't exist already, the "code" or "data" folder
   * will be created.
   * <p/>
   * @return true if successful.
   */
  public boolean addFile(File sourceFile) {
    String filename = sourceFile.getName();
    File destFile = null;
    String codeExtension = null;
    boolean replacement = false;

    // if the file appears to be code related, drop it
    // into the code folder, instead of the data folder
    if (filename.toLowerCase().endsWith(".class") ||
        filename.toLowerCase().endsWith(".jar") ||
        filename.toLowerCase().endsWith(".dll") ||
        filename.toLowerCase().endsWith(".jnilib") ||
        filename.toLowerCase().endsWith(".so")) {

      //if (!codeFolder.exists()) codeFolder.mkdirs();
      prepareCodeFolder();
      destFile = new File(codeFolder, filename);

    } else {
      for (String extension : mode.getExtensions()) {
        String lower = filename.toLowerCase();
        if (lower.endsWith("." + extension)) {
          destFile = new File(this.folder, filename);
          codeExtension = extension;
        }
      }
      if (codeExtension == null) {
        prepareDataFolder();
        destFile = new File(dataFolder, filename);
      }
    }

    // check whether this file already exists
    if (destFile.exists()) {
      Object[] options = { "OK", "Cancel" };
      String prompt = "Replace the existing version of " + filename + "?";
      int result = JOptionPane.showOptionDialog(editor,
                                                prompt,
                                                "Replace",
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null,
                                                options,
                                                options[0]);
      if (result == JOptionPane.YES_OPTION) {
        replacement = true;
      } else {
        return false;
      }
    }

    // If it's a replacement, delete the old file first,
    // otherwise case changes will not be preserved.
    // http://dev.processing.org/bugs/show_bug.cgi?id=969
    if (replacement) {
      boolean muchSuccess = destFile.delete();
      if (!muchSuccess) {
        Base.showWarning("Error adding file",
                         "Could not delete the existing '" +
                         filename + "' file.", null);
        return false;
      }
    }

    // make sure they aren't the same file
    if ((codeExtension == null) && sourceFile.equals(destFile)) {
      Base.showWarning("You can't fool me",
                       "This file has already been copied to the\n" +
                       "location from which where you're trying to add it.\n" +
                       "I ain't not doin nuthin'.", null);
      return false;
    }

    // in case the user is "adding" the code in an attempt
    // to update the sketch's tabs
    if (!sourceFile.equals(destFile)) {
      try {
        Base.copyFile(sourceFile, destFile);

      } catch (IOException e) {
        Base.showWarning("Error adding file",
                         "Could not add '" + filename + "' to the sketch.", e);
        return false;
      }
    }

    if (codeExtension != null) {
      SketchCode newCode = new SketchCode(destFile, codeExtension);

      if (replacement) {
        replaceCode(newCode);

      } else {
        insertCode(newCode);
        sortCode();
      }
      setCurrentCode(filename);
      editor.header.repaint();
      if (editor.untitled) {  // TODO probably not necessary? problematic?
        // Mark the new code as modified so that the sketch is saved
        current.setModified(true);
      }

    } else {
      if (editor.untitled) {  // TODO probably not necessary? problematic?
        // If a file has been added, mark the main code as modified so
        // that the sketch is properly saved.
        code[0].setModified(true);
      }
    }
    return true;
  }


  /**
   * Change what file is currently being edited. Changes the current tab index.
   * <OL>
   * <LI> store the String for the text of the current file.
   * <LI> retrieve the String for the text of the new file.
   * <LI> change the text that's visible in the text area
   * </OL>
   */
  public void setCurrentCode(int which) {
    // if current is null, then this is the first setCurrent(0)
    if ((currentIndex == which) && (current != null)) {
      return;
    }

    // get the text currently being edited
    if (current != null) {
      current.setState(editor.getText(),
                       editor.getSelectionStart(),
                       editor.getSelectionStop(),
                       editor.getScrollPosition());
    }

    current = code[which];
    currentIndex = which;

    editor.setCode(current);
    editor.header.rebuild();
  }


  /**
   * Internal helper function to set the current tab based on a name.
   * @param findName the file name (not pretty name) to be shown
   */
  protected void setCurrentCode(String findName) {
    for (int i = 0; i < codeCount; i++) {
      if (findName.equals(code[i].getFileName()) ||
          findName.equals(code[i].getPrettyName())) {
        setCurrentCode(i);
        return;
      }
    }
  }


  /** 
   * Create a temporary folder that includes the sketch's name in its title.
   */
  public File makeTempFolder() {
    try {
      File buildFolder = Base.createTempFolder(name, "temp");
//      if (buildFolder.mkdirs()) {
      return buildFolder;

//      } else {
//        Base.showWarning("Build folder bad", 
//                         "Could not create a place to build the sketch.", null);
//      }
    } catch (IOException e) {
      Base.showWarning("Build folder bad", 
                       "Could not find a place to build the sketch.", e);
    }
    return null;
  }


  /**
   * When running from the editor, take care of preparations before running
   * a build or an export. Also erases and/or creates 'targetFolder' if it's
   * not null, and if preferences say to do so when exporting.
   * @param targetFolder is something like applet, application, android...
   */
  public void prepareBuild(File targetFolder) throws SketchException {
    // make sure the user didn't hide the sketch folder
    ensureExistence();

    // don't do from the command line
    if (editor != null) {
      // make sure any edits have been stored
      current.setProgram(editor.getText());

      // if an external editor is being used, need to grab the
      // latest version of the code from the file.
      if (Preferences.getBoolean("editor.external")) {
        // set current to null so that the tab gets updated
        // http://dev.processing.org/bugs/show_bug.cgi?id=515
        current = null;
        // nuke previous files and settings
        load();
      }
    }

    if (targetFolder != null) {
      // Nuke the old applet/application folder because it can cause trouble
      if (Preferences.getBoolean("export.delete_target_folder")) {
        System.out.println("temporarily skipping deletion of " + targetFolder);
//        Base.removeDir(targetFolder);
//        targetFolder.renameTo(dest);
      }
      // Create a fresh output folder (needed before preproc is run next)
      targetFolder.mkdirs();
    }
  }


  /**
   * Make sure the sketch hasn't been moved or deleted by some
   * nefarious user. If they did, try to re-create it and save.
   * Only checks to see if the main folder is still around,
   * but not its contents.
   */
  public void ensureExistence() {
    if (folder.exists()) return;

    Base.showWarning("Sketch Disappeared",
                     "The sketch folder has disappeared.\n " +
                     "Will attempt to re-save in the same location,\n" +
                     "but anything besides the code will be lost.", null);
    try {
      folder.mkdirs();
      modified = true;

      for (int i = 0; i < codeCount; i++) {
        code[i].save();  // this will force a save
      }
      calcModified();

    } catch (Exception e) {
      Base.showWarning("Could not re-save sketch",
                       "Could not properly re-save the sketch. " +
                       "You may be in trouble at this point,\n" +
                       "and it might be time to copy and paste " +
                       "your code to another text editor.", e);
    }
  }


  /**
   * Returns true if this is a read-only sketch. Used for the
   * examples directory, or when sketches are loaded from read-only
   * volumes or folders without appropriate permissions.
   */
  public boolean isReadOnly() {
    String apath = folder.getAbsolutePath();
    Mode mode = editor.getMode();
    if (apath.startsWith(mode.getExamplesFolder().getAbsolutePath()) ||
        apath.startsWith(mode.getLibrariesFolder().getAbsolutePath())) {
      return true;

      // canWrite() doesn't work on directories
      //} else if (!folder.canWrite()) {
    } else {
      // check to see if each modified code file can be written to
      for (int i = 0; i < codeCount; i++) {
        if (code[i].isModified() &&
            code[i].fileReadOnly() &&
            code[i].fileExists()) {
          //System.err.println("found a read-only file " + code[i].file);
          return true;
        }
      }
      //return true;
    }
    return false;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  // Additional accessors added in 0136 because of package work.
  // These will also be helpful for tool developers.


  /**
   * Returns the name of this sketch. (The pretty name of the main tab.)
   */
  public String getName() {
    return name;
  }


  /**
   * Returns a file object for the primary .pde of this sketch.
   */
  public File getPrimaryFile() {
    return primaryFile;
  }


  /**
   * Returns path to the main .pde file for this sketch.
   */
  public String getMainFilePath() {
    return primaryFile.getAbsolutePath();
    //return code[0].file.getAbsolutePath();
  }


  /**
   * Returns the sketch folder.
   */
  public File getFolder() {
    return folder;
  }


  /**
   * Returns the location of the sketch's data folder. (It may not exist yet.)
   */
  public File getDataFolder() {
    return dataFolder;
  }

  
  public boolean hasDataFolder() {
    return dataFolder.exists();
  }


  /**
   * Create the data folder if it does not exist already. As a convenience,
   * it also returns the data folder, since it's likely about to be used.
   */
  public File prepareDataFolder() {
    if (!dataFolder.exists()) {
      dataFolder.mkdirs();
    }
    return dataFolder;
  }


  /**
   * Returns the location of the sketch's code folder. (It may not exist yet.)
   */
  public File getCodeFolder() {
    return codeFolder;
  }


  public boolean hasCodeFolder() {
    return (codeFolder != null) && codeFolder.exists();
  }


  /**
   * Create the code folder if it does not exist already. As a convenience,
   * it also returns the code folder, since it's likely about to be used.
   */
  public File prepareCodeFolder() {
    if (!codeFolder.exists()) {
      codeFolder.mkdirs();
    }
    return codeFolder;
  }


//  public String getClassPath() {
//    return classPath;
//  }


//  public String getLibraryPath() {
//    return javaLibraryPath;
//  }


  public SketchCode[] getCode() {
    return code;
  }


  public int getCodeCount() {
    return codeCount;
  }


  public SketchCode getCode(int index) {
    return code[index];
  }


  public int getCodeIndex(SketchCode who) {
    for (int i = 0; i < codeCount; i++) {
      if (who == code[i]) {
        return i;
      }
    }
    return -1;
  }


  public SketchCode getCurrentCode() {
    return current;
  }


  public void setUntitled(boolean u) {
    editor.untitled = u;
  }


  public boolean isUntitled() {
    return editor.untitled;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Convert to sanitized name and alert the user
   * if changes were made.
   */
  static public String checkName(String origName) {
    String newName = sanitizeName(origName);

    if (!newName.equals(origName)) {
      String msg =
        "The sketch name had to be modified. Sketch names can only consist\n" +
        "of ASCII characters and numbers (but cannot start with a number).\n" +
        "They should also be less less than 64 characters long.";
      System.out.println(msg);
    }
    return newName;
  }


  /**
   * Return true if the name is valid for a Processing sketch.
   */
  static public boolean isSanitaryName(String name) {
    if (name.toLowerCase().endsWith(".pde")) {
      name = name.substring(0, name.length() - 4);
    }
    return sanitizeName(name).equals(name);
  }


  /**
   * Produce a sanitized name that fits our standards for likely to work.
   * <p/>
   * Java classes have a wider range of names that are technically allowed
   * (supposedly any Unicode name) than what we support. The reason for
   * going more narrow is to avoid situations with text encodings and
   * converting during the process of moving files between operating
   * systems, i.e. uploading from a Windows machine to a Linux server,
   * or reading a FAT32 partition in OS X and using a thumb drive.
   * <p/>
   * This helper function replaces everything but A-Z, a-z, and 0-9 with
   * underscores. Also disallows starting the sketch name with a digit.
   */
  static public String sanitizeName(String origName) {
    char c[] = origName.toCharArray();
    StringBuffer buffer = new StringBuffer();

    // can't lead with a digit, so start with an underscore
    if ((c[0] >= '0') && (c[0] <= '9')) {
      buffer.append('_');
    }
    for (int i = 0; i < c.length; i++) {
      if (((c[i] >= '0') && (c[i] <= '9')) ||
          ((c[i] >= 'a') && (c[i] <= 'z')) ||
          ((c[i] >= 'A') && (c[i] <= 'Z'))) {
        buffer.append(c[i]);

      } else {
        buffer.append('_');
      }
    }
    // let's not be ridiculous about the length of filenames.
    // in fact, Mac OS 9 can handle 255 chars, though it can't really
    // deal with filenames longer than 31 chars in the Finder.
    // but limiting to that for sketches would mean setting the
    // upper-bound on the character limit here to 25 characters
    // (to handle the base name + ".class")
    if (buffer.length() > 63) {
      buffer.setLength(63);
    }
    return buffer.toString();
  }
  
  
  public Mode getMode() {
    return mode;
  }
}
