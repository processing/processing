/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeSketch - stores information about files in the current sketch
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry
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

import processing.core.*;

import java.awt.FileDialog;
import java.io.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.JOptionPane;

import com.oroinc.text.regex.*;


public class PdeSketch {
  static String TEMP_BUILD_PATH = "lib" + File.separator + "build";
  static File tempBuildFolder;

  PdeEditor editor;

  // name of sketch, which is the name of main file 
  // (without .pde or .java extension)
  String name;  

  // name of 'main' file, used by load(), such as sketch_04040.pde
  String mainFilename; 
  //String path;  // path to 'main' file for this sketch

  // true if any of the files have been modified
  boolean modified;

  boolean library;  // true if it's a library

  File folder; //sketchFolder;
  File dataFolder;
  File codeFolder;

  static final int PDE = 0;
  static final int JAVA = 1; 

  PdeCode current;
  int codeCount;
  PdeCode code[];

  int hiddenCount;
  PdeCode hidden[];

  // all these set each time build() is called
  String mainClassName;
  String classPath;
  String libraryPath;
  boolean externalRuntime;


  /**
   * path is location of the main .pde file, because this is also
   * simplest to use when opening the file from the finder/explorer.
   */
  public PdeSketch(PdeEditor editor, String path) throws IOException {
    this.editor = editor;

    File mainFile = new File(path);
    //System.out.println("main file is " + mainFile);

    mainFilename = mainFile.getName();
    //System.out.println("main file is " + mainFilename);

    // get the name of the sketch by chopping .pde or .java
    // off of the main file name
    if (mainFilename.endsWith(".pde")) {
      name = mainFilename.substring(0, mainFilename.length() - 4);
    } else if (mainFilename.endsWith(".java")) {
      name = mainFilename.substring(0, mainFilename.length() - 5);
    }

    // lib/build must exist when the application is started
    // it is added to the CLASSPATH by default, but if it doesn't
    // exist when the application is started, then java will remove
    // the entry from the CLASSPATH, causing PdeRuntime to fail.
    //
    tempBuildFolder = new File(TEMP_BUILD_PATH);
    if (!tempBuildFolder.exists()) {
      tempBuildFolder.mkdirs();
      PdeBase.showError("Required folder missing",
                        "A required folder was missing from \n" +
                        "from your installation of Processing.\n" +
                        "It has now been replaced, please restart    \n" + 
                        "the application to complete the repair.", null);
    }

    folder = new File(new File(path).getParent());
    //System.out.println("sketch dir is " + folder);

    codeFolder = new File(folder, "code");
    dataFolder = new File(folder, "data");

    File libraryFolder = new File(folder, "library");
    if (libraryFolder.exists()) {
      library = true;
    }

    load();
  }


  /**
   * Build the list of files. 
   *
   * Generally this is only done once, rather than
   * each time a change is made, because otherwise it gets to be 
   * a nightmare to keep track of what files went where, because 
   * not all the data will be saved to disk.
   *
   * The exception is when an external editor is in use,
   * in which case the load happens each time "run" is hit.
   */
  public void load() {
    // get list of files in the sketch folder
    String list[] = folder.list();

    for (int i = 0; i < list.length; i++) {
      if (list[i].endsWith(".pde")) codeCount++;
      else if (list[i].endsWith(".java")) codeCount++;
      else if (list[i].endsWith(".pde.x")) hiddenCount++;
      else if (list[i].endsWith(".java.x")) hiddenCount++;
    }

    code = new PdeCode[codeCount];
    hidden = new PdeCode[hiddenCount];

    int codeCounter = 0;
    int hiddenCounter = 0;

    for (int i = 0; i < list.length; i++) {
      if (list[i].endsWith(".pde")) {
        code[codeCounter++] = 
          new PdeCode(list[i].substring(0, list[i].length() - 4), 
                      new File(folder, list[i]), 
                      PDE);

      } else if (list[i].endsWith(".java")) {
        code[codeCounter++] = 
          new PdeCode(list[i].substring(0, list[i].length() - 5),
                      new File(folder, list[i]),
                      JAVA);

      } else if (list[i].endsWith(".pde.x")) {
        hidden[hiddenCounter++] = 
          new PdeCode(list[i].substring(0, list[i].length() - 6),
                      new File(folder, list[i]),
                      PDE);

      } else if (list[i].endsWith(".java.x")) {
        hidden[hiddenCounter++] = 
          new PdeCode(list[i].substring(0, list[i].length() - 7),
                      new File(folder, list[i]),
                      JAVA);
      }      
    }
    //System.out.println("code count 2 is " + codeCount);

    // remove any entries that didn't load properly
    int index = 0;
    while (index < codeCount) {
      if (code[index].program == null) {
        //hide(index);  // although will this file be hidable?
        for (int i = index+1; i < codeCount; i++) {
          code[i-1] = code[i];
        }
        codeCount--;

      } else {
        index++;
      }
    }
    //System.out.println("code count 3 is " + codeCount);

    // move the main class to the first tab
    // start at 1, if it's at zero, don't bother
    //System.out.println("looking for " + mainFilename);
    for (int i = 1; i < codeCount; i++) {
      if (code[i].file.getName().equals(mainFilename)) {
        //System.out.println("found main code at slot " + i);
        PdeCode temp = code[0];
        code[0] = code[i];
        code[i] = temp;
        break;
      }
    }

    // sort the entries at the top
    sortCode();

    // set the main file to be the current tab
    //current = code[0];
    setCurrent(0);
  }

  
  protected void insertCode(PdeCode newCode) {
    // add file to the code/codeCount list, resort the list
    if (codeCount == code.length) {
      PdeCode temp[] = new PdeCode[codeCount+1];
      System.arraycopy(code, 0, temp, 0, codeCount);
      code = temp;
    }
    code[codeCount++] = newCode;
  }


  protected void sortCode() {
    // cheap-ass sort of the rest of the files
    // it's a dumb, slow sort, but there shouldn't be more than ~5 files
    for (int i = 1; i < codeCount; i++) {
      int who = i;
      for (int j = i + 1; j < codeCount; j++) {
        if (code[j].name.compareTo(code[who].name) < 0) {
          who = j;  // this guy is earlier in the alphabet
        }
      }
      if (who != i) {  // swap with someone if changes made
        PdeCode temp = code[who];
        code[who] = code[i];
        code[i] = temp;
      }
    }
  }

  boolean renamingCode;


  public void newCode() { 
    //System.out.println("new code");
    // ask for name of new file
    // maybe just popup a text area?
    renamingCode = false;
    editor.status.edit("Name for new file:", "");
  }


  public void renameCode() {
    // don't allow rename of the main code
    if (current == code[0]) return;
    // TODO maybe gray out the menu on setCurrent(0)

    // ask for new name of file (internal to window)
    // TODO maybe just popup a text area?
    renamingCode = true;
    editor.status.edit("New name for file:", current.name);
  }


  /**
   * This is called upon return from entering a new file name.
   * (that is, from either newCode or renameCode after the prompt)
   * This code is almost identical for both the newCode and renameCode
   * cases, so they're kept merged except for right in the middle 
   * where they diverge.
   */
  public void nameCode(String newName) {
    // if renaming to the same thing as before, just ignore.
    // also ignoring case here, because i don't want to write 
    // a bunch of special stuff for each platform
    // (osx is case insensitive but preserving, windows insensitive,
    // *nix is sensitive and preserving.. argh)
    if (renamingCode && newName.equalsIgnoreCase(current.name)) {
      // exit quietly for the 'rename' case.
      // if it's a 'new' then an error will occur down below
      return;
    }

    String newFilename = null;
    int newFlavor = 0;

    // add .pde to file if it has no extension
    if (newName.endsWith(".pde")) {
      newFilename = newName;
      newName = newName.substring(0, newName.length() - 4);
      newFlavor = PDE;

    } else if (newName.endsWith(".java")) {
      newFilename = newName;
      newName = newName.substring(0, newName.length() - 5);
      newFlavor = JAVA;

    } else {
      newFilename = newName + ".pde";
      newFlavor = PDE;
    }

    // dots are allowed for the .pde and .java, but not in general
    // so make sure the user didn't name things poo.time.pde
    // or something like that (nothing against poo time)
    if (newName.indexOf('.') != -1) {
      newName = PdeSketchbook.sanitizedName(newName);
      newFilename = newName + ((newFlavor == PDE) ? ".pde" : ".java");
    }

    // create the new file, new PdeCode object and load it
    File newFile = new File(folder, newFilename);
    if (newFile.exists()) {  // yay! users will try anything
      PdeBase.showMessage("Nope",
                          "A file named \"" + newFile + "\" already exists\n" +
                          "in \"" + folder.getAbsolutePath() + "\"");
      return;
    }

    if (renamingCode) {
      if (!current.file.renameTo(newFile)) {
        PdeBase.showWarning("Error",
                            "Could not rename \"" + current.file.getName() + 
                            "\" to \"" + newFile.getName() + "\"", null);
        return;
      }
      current.file = newFile;
      current.name = newName;
      current.flavor = newFlavor;

    } else {  // creating a new file
      try {
        newFile.createNewFile();  // TODO returns a boolean
      } catch (IOException e) {
        PdeBase.showWarning("Error",
                            "Could not create the file \"" + newFile + "\"\n" +
                            "in \"" + folder.getAbsolutePath() + "\"", e);
        return;
      }
      PdeCode newCode = new PdeCode(newName, newFile, newFlavor);
      insertCode(newCode);
    }

    // sort the entries
    sortCode();

    // set the new guy as current
    setCurrent(newName);

    // update the tabs
    editor.header.repaint();    
  }


  /**
   * Remove a piece of code from the sketch and from the disk.
   */
  public void deleteCode() {
    // don't allow delete of the main code
    // TODO maybe gray out the menu on setCurrent(0)
    if (current == code[0]) {
      PdeBase.showMessage("Can't do that",
                          "You cannot delete the main " + 
                          ".pde file from a sketch\n");
      return;
    }

    // confirm deletion with user, yes/no
    Object[] options = { "OK", "Cancel" };
    String prompt = 
      "Are you sure you want to delete \"" + current.name + "\"?";
    int result = JOptionPane.showOptionDialog(editor,
                                              prompt,
                                              "Delete",
                                              JOptionPane.YES_NO_OPTION,
                                              JOptionPane.QUESTION_MESSAGE,
                                              null,
                                              options, 
                                              options[0]);  
    if (result == JOptionPane.YES_OPTION) {
      // delete the file
      if (!current.file.delete()) {
        PdeBase.showMessage("Couldn't do it", 
                            "Could not delete \"" + current.name + "\".");
        return;
      }

      // remove code from the list
      removeCode(current);

      // just set current tab to the main tab
      setCurrent(0);

      // update the tabs
      editor.header.repaint();
    }
  }


  protected void removeCode(PdeCode which) {
    // remove it from the internal list of files
    // resort internal list of files
    for (int i = 0; i < codeCount; i++) {
      if (code[i] == which) {
        for (int j = i; j < codeCount-1; j++) {
          code[j] = code[j+1];
        }
        codeCount--;
        return;
      }
    }
    System.err.println("removeCode: internal error.. could not find code");
  }


  public void hideCode() {
    // don't allow hide of the main code
    // TODO maybe gray out the menu on setCurrent(0)
    if (current == code[0]) {
      PdeBase.showMessage("Can't do that",
                          "You cannot hide the main " + 
                          ".pde file from a sketch\n");
      return;
    }

    // rename the file
    File newFile = new File(current.file.getAbsolutePath() + ".x");
    if (!current.file.renameTo(newFile)) {
      PdeBase.showWarning("Error",
                          "Could not hide " + 
                          "\"" + current.file.getName() + "\".", null);
      return;
    }
    current.file = newFile;

    // move it to the hidden list
    if (hiddenCount == hidden.length) {
      PdeCode temp[] = new PdeCode[hiddenCount+1];
      System.arraycopy(hidden, 0, temp, 0, hiddenCount);
      hidden = temp;
    }
    hidden[hiddenCount++] = current;

    // remove it from the main list
    removeCode(current);

    // update the tabs
    setCurrent(0);
    editor.header.repaint();
  }


  public void unhideCode(String what) {
    //System.out.println("unhide " + e);
    int unhideIndex = -1;
    for (int i = 0; i < hiddenCount; i++) {
      if (hidden[i].name.equals(what)) {
        unhideIndex = i;

        // remove from the 'hidden' list
        for (int j = i; j < hiddenCount-1; j++) {
          hidden[j] = hidden[j+1];
        }
        hiddenCount--;
        break;
      }
    }
    if (unhideIndex == -1) {
      System.err.println("internal error: could find " + what + " to unhide.");
      return;
    }
    PdeCode unhideCode = hidden[unhideIndex];
    if (!unhideCode.file.exists()) {
      PdeBase.showMessage("Can't unhide",
                          "The file \"" + what + "\" no longer exists.");
      //System.out.println(unhideCode.file);
      return;
    }
    String unhidePath = unhideCode.file.getAbsolutePath();
    File unhideFile = 
      new File(unhidePath.substring(0, unhidePath.length() - 2));

    if (!unhideCode.file.renameTo(unhideFile)) {
      PdeBase.showMessage("Can't unhide",
                          "The file \"" + what + "\" could not be" + 
                          "renamed and unhidden.");
      return;
    }
    unhideCode.file = unhideFile;
    insertCode(unhideCode);
    sortCode();
    setCurrent(unhideCode.name);
    editor.header.repaint();
  }


  /**
   * Return true if this sketch is a library.
   */
  public boolean isLibrary() {
    return library;
  }


  /**
   * Sets the modified value for the code in the frontmost tab.
   */
  public void setModified() {
    current.modified = true;
    calcModified();
  }


  public void calcModified() {
    modified = false;
    for (int i = 0; i < codeCount; i++) {
      if (code[i].modified) {
        modified = true;
        break;
      }
    }
    editor.header.repaint();
  }


  /**
   * Save all code in the current sketch.
   */
  public boolean save() throws IOException {
    // first get the contents of the editor text area
    if (current.modified) {
      current.program = editor.getText();
    }

    // see if actually modified
    if (!modified) return false;

    // check if the files are read-only. 
    // if so, need to first do a "save as".
    if (isReadOnly()) {
      PdeBase.showMessage("Sketch is read-only",
                          "Some files are marked \"read-only\", so you'll\n" +
                          "need to re-save this sketch to another location.");
      // if the user cancels, give up on the save()
      if (!saveAs()) return false;
    }

    for (int i = 0; i < codeCount; i++) {
      if (code[i].modified) code[i].save();
    }
    calcModified();
    return true;
  }


  public void saveCurrent() throws IOException {
    current.save();
    calcModified();
  }


  /**
   * handles 'save as' for a sketch.. essentially duplicates
   * the current sketch folder to a new location, and then calls
   * 'save'. (needs to take the current state of the open files
   * and save them to the new folder.. but not save over the old 
   * versions for the old sketch..)
   *
   * also removes the previously-generated .class and .jar files, 
   * because they can cause trouble.
   */
  public boolean saveAs() throws IOException {
    // get new name for folder
    FileDialog fd = new FileDialog(editor, //new Frame(), 
                                   "Save sketch folder as...", 
                                   FileDialog.SAVE);
    // always default to the sketchbook folder.. 
    fd.setDirectory(PdePreferences.get("sketchbook.path"));
    // TODO or maybe this should default to the 
    //      parent dir of the old folder?

    fd.show();
    String newParentDir = fd.getDirectory();
    String newName = fd.getFile();

    // user cancelled selection
    if (newName == null) return false;
    newName = PdeSketchbook.sanitizeName(newName);

    // new sketch folder
    File newFolder = new File(newParentDir, newName);

    // make sure the paths aren't the same
    if (newFolder.equals(folder)) {
      PdeBase.showWarning("You can't fool me", 
                          "The new sketch name and location are the same\n" +
                          "as the old. I ain't not doin nuthin'.", null);
      return false;
    }

    // copy the entire contents of the sketch folder
    PdeBase.copyDir(folder, newFolder);

    // change the references to the dir location in PdeCode files
    for (int i = 0; i < codeCount; i++) {
      code[i].file = new File(newFolder, code[i].file.getName());
    }
    for (int i = 0; i < hiddenCount; i++) {
      hidden[i].file = new File(newFolder, hidden[i].file.getName());
    }

    // remove the old sketch file from the new dir
    code[0].file.delete();
    // name for the new main .pde file
    code[0].file = new File(newFolder, newName + ".pde");
    code[0].name = newName;
    // write the contents to the renamed file
    // (this may be resaved if the code is modified)
    code[0].save();

    // change the other paths
    String oldName = name;
    name = newName;
    File oldFolder = folder;
    folder = newFolder;
    dataFolder = new File(folder, "data");
    codeFolder = new File(folder, "code");

    // remove the 'applet', 'application', 'library' folders
    // from the copied version.
    // otherwise their .class and .jar files can cause conflicts.
    PdeBase.removeDir(new File(folder, "applet"));
    PdeBase.removeDir(new File(folder, "application"));
    PdeBase.removeDir(new File(folder, "library"));

    // do a "save"
    // this will take care of the unsaved changes in each of the tabs
    save();

    // get the changes into the sketchbook menu
    //sketchbook.rebuildMenu();
    // done inside PdeEditor instead

    // let PdeEditor know that the save was successful
    return true;
  }


  /**
   * Prompt the user for a new file to the sketch. 
   * This could be .class or .jar files for the code folder,
   * .pde or .java files for the project,
   * or .dll, .jnilib, or .so files for the code folder
   */
  public void addFile() {
    // get a dialog, select a file to add to the sketch
    String prompt = 
      "Select an image or other data file to copy to your sketch";
    //FileDialog fd = new FileDialog(new Frame(), prompt, FileDialog.LOAD);
    FileDialog fd = new FileDialog(editor, prompt, FileDialog.LOAD);
    fd.show();

    String directory = fd.getDirectory();
    String filename = fd.getFile();
    if (filename == null) return;

    // copy the file into the folder. if people would rather 
    // it move instead of copy, they can do it by hand
    File sourceFile = new File(directory, filename);

    File destFile = null;
    boolean addingCode = false;

    // if the file appears to be code related, drop it 
    // into the code folder, instead of the data folder
    if (filename.toLowerCase().endsWith(".class") || 
        filename.toLowerCase().endsWith(".jar") || 
        filename.toLowerCase().endsWith(".dll") || 
        filename.toLowerCase().endsWith(".jnilib") || 
        filename.toLowerCase().endsWith(".so")) {
      //File codeFolder = new File(this.folder, "code");
      if (!codeFolder.exists()) codeFolder.mkdirs();
      destFile = new File(codeFolder, filename);

    } else if (filename.toLowerCase().endsWith(".pde") ||
               filename.toLowerCase().endsWith(".java")) {
      destFile = new File(this.folder, filename);
      addingCode = true;

    } else {
      //File dataFolder = new File(this.folder, "data");
      if (!dataFolder.exists()) dataFolder.mkdirs();
      destFile = new File(dataFolder, filename);
    }

    // make sure they aren't the same file
    if (sourceFile.equals(destFile)) {
      PdeBase.showWarning("You can't fool me", 
                          "This file has already been copied to the\n" +
                          "location where you're trying to add it.\n" +
                          "I ain't not doin nuthin'.", null);
      return;
    }

    try {
      PdeBase.copyFile(sourceFile, destFile);
    } catch (IOException e) {
      PdeBase.showWarning("Error adding file",
                          "Could not add '" + filename + 
                          "' to the sketch.", e);
    }

    // make the tabs update after this guy is added
    if (addingCode) {
      String newName = destFile.getName();
      int newFlavor = -1;
      if (newName.toLowerCase().endsWith(".pde")) {
        newName = newName.substring(0, newName.length() - 4);
        newFlavor = PDE;
      } else {
        newName = newName.substring(0, newName.length() - 5);
        newFlavor = JAVA;
      }

      // see also "nameCode" for identical situation
      PdeCode newCode = new PdeCode(newName, destFile, newFlavor);
      insertCode(newCode);
      sortCode();
      setCurrent(newName);
      editor.header.repaint();
    }
  }


  public void addLibrary(String jarPath) {
    String list[] = PdeCompiler.packageListFromClassPath(jarPath);

    // import statements into the main sketch file (code[0])
    // if the current code is a .java file, insert into current
    if (current.flavor == PDE) {
      setCurrent(0);
    }
    // could also scan the text in the file to see if each import
    // statement is already in there, but if the user has the import
    // commented out, then this will be a problem.
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < list.length; i++) {
      buffer.append("import ");
      buffer.append(list[i]);
      buffer.append(".*;\n");
    }
    buffer.append('\n');
    buffer.append(editor.getText());
    editor.setText(buffer.toString(), false);
    setModified();
  }


  /**
   * Change what file is currently being edited. 
   * 1. store the String for the text of the current file.
   * 2. retrieve the String for the text of the new file.
   * 3. change the text that's visible in the text area
   */
  public void setCurrent(int which) {
    // get the text currently being edited
    //program[current] = editor.getText();
    if (current != null) {
      current.program = editor.getText();
    }

    current = code[which];

    // set to the text for this file
    // 'true' means to wipe out the undo buffer
    // (so they don't undo back to the other file.. whups!)
    editor.setText(current.program, true); 

    // and i'll personally make a note of the change
    //current = which;

    editor.header.rebuild();
  }


  /**
   * Internal helper function to set the current tab
   * based on a name (used by codeNew and codeRename).
   */
  protected void setCurrent(String findName) {
    for (int i = 0; i < codeCount; i++) {
      if (findName.equals(code[i].name)) {
        setCurrent(i);
        return;
      }
    }
  }


  /**
   * Cleanup temporary files used during a build/run.
   */
  protected void cleanup() {
    // if the java runtime is holding onto any files in the build dir, we
    // won't be able to delete them, so we need to force a gc here
    //
    System.gc();

    // note that we can't remove the builddir itself, otherwise
    // the next time we start up, internal runs using PdeRuntime won't
    // work because the build dir won't exist at startup, so the classloader
    // will ignore the fact that that dir is in the CLASSPATH in run.sh
    //
    //File dirObject = new File(TEMP_BUILD_PATH);
    //PdeBase.removeDescendants(dirObject);
    PdeBase.removeDescendants(tempBuildFolder);
  }


  /**
   * Preprocess, Compile, and Run the current code.
   * This is not Runnable.run(), but a handler for the run() command.
   *
   * There are three main parts to this process:
   *
   *   (0. if not java, then use another 'engine'.. i.e. python)
   *
   *    1. do the p5 language preprocessing
   *       this creates a working .java file in a specific location
   *       better yet, just takes a chunk of java code and returns a 
   *       new/better string editor can take care of saving this to a 
   *       file location
   *
   *    2. compile the code from that location
   *       catching errors along the way
   *       placing it in a ready classpath, or .. ?
   *
   *    3. run the code 
   *       needs to communicate location for window 
   *       and maybe setup presentation space as well
   *       run externally if a code folder exists, 
   *       or if more than one file is in the project
   *
   *    X. afterwards, some of these steps need a cleanup function
   */
  //public void run() throws PdeException {
  public boolean handleRun() throws PdeException {
    current.program = editor.getText();

    // TODO record history here
    //current.history.record(program, PdeHistory.RUN);

    // if an external editor is being used, need to grab the
    // latest version of the code from the file.
    if (PdePreferences.getBoolean("editor.external")) {
      // history gets screwed by the open..
      //String historySaved = history.lastRecorded;
      //handleOpen(sketch);
      //history.lastRecorded = historySaved;

      // nuke previous files and settings, just get things loaded
      load();
    }

    // in case there were any boogers left behind
    // do this here instead of after exiting, since the exit
    // can happen so many different ways.. and this will be
    // better connected to the dataFolder stuff below.
    cleanup();

    // make up a temporary class name to suggest.
    // name will only be used if the code is not in ADVANCED mode.
    String suggestedClassName = 
      ("Temporary_" + String.valueOf((int) (Math.random() * 10000)) +
       "_" + String.valueOf((int) (Math.random() * 10000)));

    // handle preprocessing the main file's code
    mainClassName = build(TEMP_BUILD_PATH, suggestedClassName);
    // externalPaths is magically set by build()

    if (!externalRuntime) {  // only if not running externally already
      // copy contents of data dir into lib/build
      if (dataFolder.exists()) {
        // just drop the files in the build folder (pre-68)
        //PdeBase.copyDir(dataDir, buildDir);
        // drop the files into a 'data' subfolder of the build dir
        try {
          PdeBase.copyDir(dataFolder, new File(tempBuildFolder, "data"));
        } catch (IOException e) {
          e.printStackTrace();
          throw new PdeException("Problem copying files from data folder");
        }
      }
    }

    // if the compilation worked, run the applet
//    if (mainClassName != null) {

      /*
      if (externalPaths == null) {
        externalPaths = 
          PdeCompiler.calcClassPath(null) + File.pathSeparator + 
          tempBuildPath;
      } else {
        externalPaths = 
          tempBuildPath + File.pathSeparator +
          PdeCompiler.calcClassPath(null) + File.pathSeparator +
          externalPaths;
      }
      */

      // get a useful folder name for the 'code' folder
      // so that it can be included in the java.library.path
      /*
      String libraryPath = "";
      if (externalCode != null) {
        libraryPath = externalCode.getCanonicalPath();
      }
      */

      // create a runtime object
//      runtime = new PdeRuntime(this, editor); 

      // if programType is ADVANCED
      //   or the code/ folder is not empty -> or just exists (simpler)
      // then set boolean for external to true
      // include path to build in front, then path for code folder
      //   when passing the classpath through
      //   actually, build will already be in there, just prepend code

      // use the runtime object to consume the errors now
      // no need to bother recycling the old guy
      //PdeMessageStream messageStream = new PdeMessageStream(runtime);

      // start the applet
//      runtime.start(presenting ? presentLocation : appletLocation); //,
      //new PrintStream(messageStream));

      // spawn a thread to update PDE GUI state
//      watcher = new RunButtonWatcher();

//    } else {
      // [dmose] throw an exception here?
      // [fry] iirc the exception will have already been thrown
//      cleanup();
//    }
    return (mainClassName != null);
  }


  /**
   * Have the contents of the currently visible tab been modified?
   */
  /*
  public boolean isCurrentModified() {
    return modified[current];
  }
  */


  /**
   * Build all the code for this sketch.
   *
   * In an advanced program, the returned classname could be different,
   * which is why the className is set based on the return value.
   * A compilation error will burp up a PdeException.
   *
   * @return null if compilation failed, main class name if not
   */
  protected String build(String buildPath, String suggestedClassName)
    throws PdeException {

    String importPackageList[] = null;

    String javaClassPath = System.getProperty("java.class.path");
    // remove quotes if any.. this is an annoying thing on windows
    if (javaClassPath.startsWith("\"") && javaClassPath.endsWith("\"")) {
      javaClassPath = javaClassPath.substring(1, javaClassPath.length() - 1);
    }

    classPath = buildPath + File.pathSeparator + javaClassPath;
    //System.out.println("cp = " + classPath);

    // figure out the contents of the code folder to see if there
    // are files that need to be added to the imports
    //File codeFolder = new File(folder, "code");
    if (codeFolder.exists()) {
      externalRuntime = true;
      classPath += File.pathSeparator + 
        PdeCompiler.contentsToClassPath(codeFolder);
      importPackageList = PdeCompiler.packageListFromClassPath(classPath);
      //libraryPath = codeFolder.getCanonicalPath();
      libraryPath = codeFolder.getAbsolutePath();
    } else {
      externalRuntime = (codeCount > 1);  // may still be set true later
      importPackageList = null;
      libraryPath = "";
    }

    // if 'data' folder is large, set to external runtime
    if (dataFolder.exists() &&
        PdeBase.calcFolderSize(dataFolder) > 768 * 1024) {  // if > 768k
      externalRuntime = true;
    }


    // 1. concatenate all .pde files to the 'main' pde
    //    store line number for starting point of each code bit

    StringBuffer bigCode = new StringBuffer(code[0].program);
    int bigCount = countLines(code[0].program);

    for (int i = 1; i < codeCount; i++) {
      if (code[i].flavor == PDE) {
        code[i].lineOffset = ++bigCount;
        bigCode.append('\n');
        bigCode.append(code[i].program);
        bigCount += countLines(code[i].program);
        code[i].preprocName = null;  // don't compile me
      }
    }


    // 2. run preproc on that code using the sugg class name
    //    to create a single .java file and write to buildpath

    String primaryClassName = null;

    PdePreprocessor preprocessor = new PdePreprocessor();
    try {
      // if (i != 0) preproc will fail if a pde file is not 
      // java mode, since that's required
      String className = 
        preprocessor.write(bigCode.toString(), buildPath,
                           suggestedClassName);
      //preprocessor.write(bigCode.toString(), buildPath,
      //                   suggestedClassName, importPackageList);
      if (className == null) {
        throw new PdeException("Could not find main class");
        // this situation might be perfectly fine, 
        // (i.e. if the file is empty)
        //System.out.println("No class found in " + code[i].name);
        //System.out.println("(any code in that file will be ignored)");
        //System.out.println();

      } else {
        code[0].preprocName = className + ".java";
      }

      // store this for the compiler and the runtime
      primaryClassName = className;
      //System.out.println("primary class " + primaryClassName);

      // check if the 'main' file is in java mode
      if (PdePreprocessor.programType == PdePreprocessor.JAVA) {
        externalRuntime = true; // we in advanced mode now, boy
      }

    } catch (antlr.RecognitionException re) {
      // this even returns a column
      int errorFile = 0;
      int errorLine = re.getLine() - 1;
      for (int i = 1; i < codeCount; i++) {
        if ((code[i].flavor == PDE) && 
            (code[i].lineOffset < errorLine)) {
          errorFile = i;
        }
      }
      errorLine -= code[errorFile].lineOffset;

      throw new PdeException(re.getMessage(), errorFile,
                             errorLine, re.getColumn());

    } catch (antlr.TokenStreamRecognitionException tsre) {
      // while this seems to store line and column internally,
      // there doesn't seem to be a method to grab it.. 
      // so instead it's done using a regexp
      PatternMatcher matcher = new Perl5Matcher();
      PatternCompiler compiler = new Perl5Compiler();
      // line 3:1: unexpected char: 0xA0
      String mess = "^line (\\d+):(\\d+):\\s";

      Pattern pattern = null;
      try {
        pattern = compiler.compile(mess);
      } catch (MalformedPatternException e) {
        PdeBase.showWarning("Internal Problem",
                            "An internal error occurred while trying\n" + 
                            "to compile the sketch. Please report\n" +
                            "this online at http://processing.org/bugs", e);
      }

      PatternMatcherInput input = 
        new PatternMatcherInput(tsre.toString());
      if (matcher.contains(input, pattern)) {
        MatchResult result = matcher.getMatch();

        int errorLine = Integer.parseInt(result.group(1).toString()) - 1;
        int errorColumn = Integer.parseInt(result.group(2).toString());
        int errorFile = 0;
        for (int i = 1; i < codeCount; i++) {
          if ((code[i].flavor == PDE) && 
              (code[i].lineOffset < errorLine)) {
            errorFile = i;
          }
        }
        errorLine -= code[errorFile].lineOffset;

        throw new PdeException(tsre.getMessage(), 
                               errorFile, errorLine, errorColumn);

      } else {
        // this is bad, defaults to the main class.. hrm.
        throw new PdeException(tsre.toString(), 0, -1, -1);
      }

    } catch (PdeException pe) {
      // PdeExceptions are caught here and re-thrown, so that they don't 
      // get lost in the more general "Exception" handler below. 
      throw pe;

    } catch (Exception ex) {
      // TODO better method for handling this? 
      System.err.println("Uncaught exception type:" + ex.getClass());
      ex.printStackTrace();
      throw new PdeException(ex.toString());
    }


    // 3. then loop over the code[] and save each .java file

    for (int i = 0; i < codeCount; i++) {
      if (code[i].flavor == JAVA) {
        // no pre-processing services necessary for java files
        // just write the the contents of 'program' to a .java file 
        // into the build directory. uses byte stream and reader/writer
        // shtuff so that unicode bunk is properly handled
        String filename = code[i].name + ".java";
        try {
          PdeBase.saveFile(code[i].program, new File(buildPath, filename));
        } catch (IOException e) {
          e.printStackTrace();
          throw new PdeException("Problem moving " + filename + 
                                 " to the build folder");
        }
        code[i].preprocName = filename;
      }
    }

    // compile the program. errors will happen as a PdeException
    // that will bubble up to whomever called build().
    //
    PdeCompiler compiler = new PdeCompiler();
    boolean success = compiler.compile(this, buildPath);
    //System.out.println("success = " + success + " ... " + primaryClassName);
    return success ? primaryClassName : null;
  }


  protected int countLines(String what) {
    char c[] = what.toCharArray();
    int count = 0;
    for (int i = 0; i < c.length; i++) {
      if (c[i] == '\n') count++;
    }
    return count;
  }


  /**
   * Called by PdeEditor to handle someone having selected 'export'. 
   * Pops up a dialog box for export options, and then calls the
   * necessary function with the parameters from the window.
   *
   * +-------------------------------------------------------+
   * +                                                       +
   * + Export to:  [ Applet (for the web)   + ]    [  OK  ]  +
   * +                                                       +
   * + > Advanced                                            +
   * +                                                       +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Version: [ Java 1.1   + ]                           +
   * +                                                       +
   * +   Recommended version of Java when exporting applets. +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Version: [ Java 1.3   + ]                           +
   * +                                                       +
   * +   Java 1.3 is not recommended for applets,            +
   * +   unless you are using features that require it.      +
   * +   Using a version of Java other than 1.1 will require +
   * +   your Windows users to install the Java Plug-In,     +
   * +   and your Macintosh users to be running OS X.        +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Version: [ Java 1.4   + ]                           +
   * +                                                       +
   * +   identical message as 1.3 above...                   +
   * +                                                       +
   * +-------------------------------------------------------+
   * 
   * +-------------------------------------------------------+
   * +                                                       +
   * + Export to:  [ Application            + ]    [  OK  ]  + 
   * +                                                       +
   * + > Advanced                                            +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Version: [ Java 1.1   + ]                           +
   * +                                                       +
   * +   Not much point to using Java 1.1 for applications.  +
   * +   To run applications, all users will have to         + 
   * +   install Java, in which case they'll most likely     +
   * +   have version 1.3 or later.                          +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Version: [ Java 1.3   + ]                           +
   * +                                                       +
   * +   Java 1.3 is the recommended setting for exporting   +
   * +   applications. Applications will run on any Windows  +
   * +   or Unix machine with Java installed. Mac OS X has   +
   * +   Java installed with the operation system, so there  +
   * +   is no additional installation will be required.     +
   * +                                                       +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +                                                       +
   * +   Platform: [ Mac OS X   + ]    <-- defaults to current platform
   * +                                                       +
   * +   Exports the application as a double-clickable       + 
   * +   .app package, compatible with Mac OS X.             +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Platform: [ Windows    + ]                          +
   * +                                                       +
   * +   Exports the application as a double-clickable       +
   * +   .exe and a handful of supporting files.             +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Platform: [ jar file   + ]                          +
   * +                                                       +
   * +   A jar file can be used on any platform that has     +
   * +   Java installed. Simply doube-click the jar (or type +
   * +   "java -jar sketch.jar" at a command prompt) to run  +
   * +   the application. It is the least fancy method for   +
   * +   exporting.                                          +
   * +                                                       +
   * +-------------------------------------------------------+
   *

   * +-------------------------------------------------------+
   * +                                                       +
   * + Export to:  [ Library                + ]    [  OK  ]  +
   * +                                                       +
   * +-------------------------------------------------------+
   */
  //public boolean export() throws Exception {
  //return exportApplet(true);
  //}


  public boolean exportApplet(/*boolean replaceHtml*/) throws Exception {
    boolean replaceHtml = true;
    //File appletDir, String exportSketchName, File dataDir) {
    //String program = textarea.getText();

    // create the project directory
    // pass null for datapath because the files shouldn't be 
    // copied to the build dir.. that's only for the temp stuff
    File appletDir = new File(folder, "applet");

    boolean writeHtml = true;
    if (appletDir.exists()) {
      File htmlFile = new File(appletDir, "index.html");
      if (htmlFile.exists() && !replaceHtml) {
        writeHtml = false;
      }
    } else {
      appletDir.mkdirs();
    }

    // build the sketch 
    String foundName = build(appletDir.getPath(), name);

    // (already reported) error during export, exit this function
    if (foundName == null) return false;

    // if name != exportSketchName, then that's weirdness
    // BUG unfortunately, that can also be a bug in the preproc :(
    if (!name.equals(foundName)) {
      PdeBase.showWarning("Error during export", 
                          "Sketch name is " + name + " but the sketch\n" +
                          "name in the code was " + foundName, null);
      return false;
    }

    if (writeHtml) {
      int wide = PApplet.DEFAULT_WIDTH;
      int high = PApplet.DEFAULT_HEIGHT;

      //try {
      PatternMatcher matcher = new Perl5Matcher();
      PatternCompiler compiler = new Perl5Compiler();

      // this matches against any uses of the size() function, 
      // whether they contain numbers of variables or whatever. 
      // this way, no warning is shown if size() isn't actually 
      // used in the applet, which is the case especially for 
      // beginners that are cutting/pasting from the reference.
      String sizing = 
        "[\\s\\;]size\\s*\\(\\s*(\\S+)\\s*,\\s*(\\S+)\\s*\\);";
      Pattern pattern = compiler.compile(sizing);

      // adds a space at the beginning, in case size() is the very 
      // first thing in the program (very common), since the regexp 
      // needs to check for things in front of it.
      PatternMatcherInput input = 
        new PatternMatcherInput(" " + code[0].program);
      if (matcher.contains(input, pattern)) {
        MatchResult result = matcher.getMatch();
        try {
          wide = Integer.parseInt(result.group(1).toString());
          high = Integer.parseInt(result.group(2).toString());

        } catch (NumberFormatException e) {
          // found a reference to size, but it didn't 
          // seem to contain numbers
          final String message = 
            "The size of this applet could not automatically be\n" +
            "determined from your code. You'll have to edit the\n" + 
            "HTML file to set the size of the applet.";

          PdeBase.showWarning("Could not find applet size", message, null);
        }
      }  // else no size() command found

      // handle this in editor instead, rare or nonexistant
      //} catch (MalformedPatternException e) {
      //PdeBase.showWarning("Internal Problem",
      //                    "An internal error occurred while trying\n" + 
      //                    "to export the sketch. Please report this.", e);
      //return false;
      //}

      StringBuffer sources = new StringBuffer();
      for (int i = 0; i < codeCount; i++) {
        sources.append("<a href=\"" + code[i].file.getName() + "\">" + 
                       code[i].name + "</a> ");
      }

      File htmlOutputFile = new File(appletDir, "index.html");
      FileOutputStream fos = new FileOutputStream(htmlOutputFile);
      PrintStream ps = new PrintStream(fos);

      // @@sketch@@, @@width@@, @@height@@, @@archive@@, @@source@@

      InputStream is = null; 
      // if there is an applet.html file in the sketch folder, use that
      File customHtml = new File(folder, "applet.html");
      if (customHtml.exists()) {
        is = new FileInputStream(customHtml);
      }
      if (is == null) {
        is = PdeBase.getStream("applet.html");
      }
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));

      String line = null;
      while ((line = reader.readLine()) != null) {
        if (line.indexOf("@@") != -1) {
          StringBuffer sb = new StringBuffer(line);
          int index = 0;
          while ((index = sb.indexOf("@@sketch@@")) != -1) {
            sb.replace(index, index + "@@sketch@@".length(), 
                       name);
          }
          while ((index = sb.indexOf("@@source@@")) != -1) {
            sb.replace(index, index + "@@source@@".length(), 
                       sources.toString());
          }
          while ((index = sb.indexOf("@@archive@@")) != -1) {
            sb.replace(index, index + "@@archive@@".length(), 
                       name + ".jar");
          }
          while ((index = sb.indexOf("@@width@@")) != -1) {
            sb.replace(index, index + "@@width@@".length(), 
                       String.valueOf(wide));
          }
          while ((index = sb.indexOf("@@height@@")) != -1) {
            sb.replace(index, index + "@@height@@".length(), 
                       String.valueOf(wide));
          }
          line = sb.toString();
        }
        ps.println(line);
      }

      reader.close();
      ps.flush();
      ps.close();
    }

    // copy the source files to the target, since we like
    // to encourage people to share their code
    for (int i = 0; i < codeCount; i++) {
      try {
        PdeBase.copyFile(code[i].file, 
                         new File(appletDir, code[i].file.getName()));
      } catch (IOException e) {
        
      }
    }

    // create new .jar file
    FileOutputStream zipOutputFile = 
      new FileOutputStream(new File(appletDir, name + ".jar"));
    ZipOutputStream zos = new ZipOutputStream(zipOutputFile);
    ZipEntry entry;

    // add the contents of the code folder to the jar
    // unpacks all jar files 
    //File codeFolder = new File(folder, "code");
    if (codeFolder.exists()) {
      String includes = PdeCompiler.contentsToClassPath(codeFolder);
      packClassPathIntoZipFile(includes, zos);
    }

    // add the appropriate bagel to the classpath
    /*
    String jdkVersion = PdePreferences.get("compiler.jdk_version");
    String bagelJar = "lib/export11.jar";  // default
    if (jdkVersion.equals("1.3") || jdkVersion.equals("1.4")) {
      bagelJar = "lib/export13.jar";
    }
    */
    String bagelJar = "lib/core.jar";

    //if (jdkVersionStr.equals("1.3")) { bagelJar = "export13.jar" };
    //if (jdkVersionStr.equals("1.4")) { bagelJar = "export14.jar" };
    packClassPathIntoZipFile(bagelJar, zos);

    /*
      // add the contents of lib/export to the jar file
      // these are the jdk11-only bagel classes
      String exportDir = ("lib" + File.separator + 
                          "export" + File.separator);
      String bagelClasses[] = new File(exportDir).list();

      for (int i = 0; i < bagelClasses.length; i++) {
        if (!bagelClasses[i].endsWith(".class")) continue;
        entry = new ZipEntry(bagelClasses[i]);
        zos.putNextEntry(entry);
        zos.write(PdeBase.grabFile(new File(exportDir + bagelClasses[i])));
        zos.closeEntry();
      }
    */

    // TODO these two loops are insufficient.
    // should instead recursively add entire contents of build folder
    // the data folder will already be a subdirectory
    // and the classes may be buried in subfolders if a package name was used

    // files to include from data directory
    //if ((dataDir != null) && (dataDir.exists())) {
    if (dataFolder.exists()) {
      String dataFiles[] = dataFolder.list();
      for (int i = 0; i < dataFiles.length; i++) {
        // don't export hidden files
        // skipping dot prefix removes all: . .. .DS_Store
        if (dataFiles[i].charAt(0) == '.') continue;

        entry = new ZipEntry(dataFiles[i]);
        zos.putNextEntry(entry);
        zos.write(PdeBase.grabFile(new File(dataFolder, dataFiles[i])));
        zos.closeEntry();
      }
    }

    // add the project's .class files to the jar
    // just grabs everything from the build directory
    // since there may be some inner classes
    // (add any .class files from the applet dir, then delete them)
    String classfiles[] = appletDir.list();
    for (int i = 0; i < classfiles.length; i++) {
      if (classfiles[i].endsWith(".class")) {
        entry = new ZipEntry(classfiles[i]);
        zos.putNextEntry(entry);
        zos.write(PdeBase.grabFile(new File(appletDir, classfiles[i])));
        zos.closeEntry();
      }
    }

    // remove the .class files from the applet folder. if they're not 
    // removed, the msjvm will complain about an illegal access error, 
    // since the classes are outside the jar file.
    for (int i = 0; i < classfiles.length; i++) {
      if (classfiles[i].endsWith(".class")) {
        File deadguy = new File(appletDir, classfiles[i]);
        if (!deadguy.delete()) {
          PdeBase.showWarning("Could not delete", 
                              classfiles[i] + " could not \n" +
                              "be deleted from the applet folder.  \n" + 
                              "You'll need to remove it by hand.", null);
        }
      }
    }

    // close up the jar file
    zos.flush();
    zos.close();

    PdeBase.openFolder(appletDir);

    //} catch (Exception e) {
    //e.printStackTrace();
    //}
    return true;
  }


  public boolean exportApplication() {
    return true;
  }


  public boolean exportLibrary() {
    return true;
  }


  /**
   * Slurps up .class files from a colon (or semicolon on windows) 
   * separated list of paths and adds them to a ZipOutputStream.
   */
  static public void packClassPathIntoZipFile(String path, 
                                              ZipOutputStream zos) 
    throws IOException {
    String pieces[] = PApplet.split(path, File.pathSeparatorChar);

    for (int i = 0; i < pieces.length; i++) {
      if (pieces[i].length() == 0) continue;
      //System.out.println("checking piece " + pieces[i]);

      // is it a jar file or directory?
      if (pieces[i].toLowerCase().endsWith(".jar") || 
          pieces[i].toLowerCase().endsWith(".zip")) {
        try {
          ZipFile file = new ZipFile(pieces[i]);
          Enumeration entries = file.entries();
          while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
              // actually 'continue's for all dir entries

            } else {
              String name = entry.getName();
              // ignore contents of the META-INF folders
              if (name.indexOf("META-INF") == 0) continue;
              ZipEntry entree = new ZipEntry(name);

              zos.putNextEntry(entree);
              byte buffer[] = new byte[(int) entry.getSize()];
              InputStream is = file.getInputStream(entry);

              int offset = 0;
              int remaining = buffer.length; 
              while (remaining > 0) {
                int count = is.read(buffer, offset, remaining);
                offset += count;
                remaining -= count;
              }

              zos.write(buffer);
              zos.flush();
              zos.closeEntry();
            }
          }
        } catch (IOException e) {
          System.err.println("Error in file " + pieces[i]);
          e.printStackTrace();
        }
      } else {  // not a .jar or .zip, prolly a directory
        File dir = new File(pieces[i]);
        // but must be a dir, since it's one of several paths
        // just need to check if it exists
        if (dir.exists()) {
          packClassPathIntoZipFileRecursive(dir, null, zos);
        }
      }
    }
  }


  /**
   * Continue the process of magical exporting. This function
   * can be called recursively to walk through folders looking
   * for more goodies that will be added to the ZipOutputStream.
   */
  static public void packClassPathIntoZipFileRecursive(File dir, 
                                                       String sofar, 
                                                       ZipOutputStream zos) 
    throws IOException {
    String files[] = dir.list();
    for (int i = 0; i < files.length; i++) {
      //if (files[i].equals(".") || files[i].equals("..")) continue;
      // ignore . .. and .DS_Store
      if (files[i].charAt(0) == '.') continue;

      File sub = new File(dir, files[i]);
      String nowfar = (sofar == null) ? 
        files[i] : (sofar + "/" + files[i]);

      if (sub.isDirectory()) {
        packClassPathIntoZipFileRecursive(sub, nowfar, zos);

      } else {
        // don't add .jar and .zip files, since they only work
        // inside the root, and they're unpacked
        if (!files[i].toLowerCase().endsWith(".jar") &&
            !files[i].toLowerCase().endsWith(".zip") &&
            files[i].charAt(0) != '.') {
          ZipEntry entry = new ZipEntry(nowfar);
          zos.putNextEntry(entry);
          zos.write(PdeBase.grabFile(sub));
          zos.closeEntry();
        }
      }
    }
  }


  /**
   * Returns true if this is a read-only sketch. Used for the 
   * examples directory, or when sketches are loaded from read-only
   * volumes or folders without appropraite permissions.
   */
  public boolean isReadOnly() {
    String apath = folder.getAbsolutePath();
    if (apath.startsWith(PdeSketchbook.examplesPath) || 
        apath.startsWith(PdeSketchbook.librariesPath)) {
      return true;

      // this doesn't work on directories
      //} else if (!folder.canWrite()) {
    } else {
      // check to see if each modified code file can be written to
      for (int i = 0; i < codeCount; i++) {
        if (code[i].modified && !code[i].file.canWrite()) {
          //System.err.println("found a read-only file " + code[i].file);
          return true;
        }
      }
      //return true;
    }
    return false;
  }


  /**
   * Returns path to the main .pde file for this sketch.
   */
  public String getMainFilePath() {
    return code[0].file.getAbsolutePath();
  }
}
