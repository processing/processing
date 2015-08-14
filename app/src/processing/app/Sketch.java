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

import processing.app.ui.Editor;
import processing.app.ui.ProgressFrame;
import processing.app.ui.Recent;
import processing.app.ui.Toolkit;
import processing.core.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.List;

import javax.swing.*;


/**
 * Stores information about files in the current sketch.
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

  /** Moved out of Editor and into here for cleaner access. */
  private boolean untitled;


  /**
   * Used by the command-line version to create a sketch object.
   * @param path location of the main .pde file
   * @param mode what flavor of sketch we're dealing with.
   */
  public Sketch(String path, Mode mode) {
    this.editor = null;
    this.mode = mode;
    load(path);
  }


  /**
   * path is location of the main .pde file, because this is also
   * simplest to use when opening the file from the finder/explorer.
   */
  public Sketch(String path, Editor editor) throws IOException {
    this.editor = editor;
    this.mode = editor.getMode();
    load(path);
  }


  protected void load(String path) {
    primaryFile = new File(path);
    // get the name of the sketch by chopping .pde or .java
    // off of the main file name
    String mainFilename = primaryFile.getName();
    int suffixLength = mode.getDefaultExtension().length() + 1;
    name = mainFilename.substring(0, mainFilename.length() - suffixLength);
    folder = new File(new File(path).getParent());
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
          if (isSanitaryName(base)) {
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


  /**
   * Reload the current sketch. Used to update the text area when
   * an external editor is in use.
   */
  public void reload() {
    // set current to null so that the tab gets updated
    // http://dev.processing.org/bugs/show_bug.cgi?id=515
    current = null;
    // nuke previous files and settings
    load();
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

        // We also need to update the current tab
        if (currentIndex == i) {
          currentIndex = who;
        } else if (currentIndex == who) {
          currentIndex = i;
        }
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
      Base.showMessage(Language.text("new.messages.is_read_only"),
                       Language.text("new.messages.is_read_only.description"));
      return;
    }

    renamingCode = false;
    // editor.status.edit("Name for new file:", "");
    promptForTabName(Language.text("editor.tab.rename.description")+":", "");
  }


  /**
   * Handler for the Rename Code menu option.
   */
  public void handleRenameCode() {
    // make sure the user didn't hide the sketch folder
    ensureExistence();

    if (currentIndex == 0 && isUntitled()) {
      Base.showMessage(Language.text("rename.messages.is_untitled"),
                       Language.text("rename.messages.is_untitled.description"));
      return;
    }

    if (isModified()) {
      Base.showMessage(Language.text("menu.file.save"),
                       Language.text("rename.messages.is_modified"));
      return;
    }

    // if read-only, give an error
    if (isReadOnly()) {
      // if the files are read-only, need to first do a "save as".
      Base.showMessage(Language.text("rename.messages.is_read_only"),
                       Language.text("rename.messages.is_read_only.description"));
      return;
    }

    // ask for new name of file (internal to window)
    // TODO maybe just popup a text area?
    renamingCode = true;
    String prompt = (currentIndex == 0) ?
      Language.text("editor.sketch.rename.description") :
      Language.text("editor.tab.rename.description");
    String oldName = (current.isExtension(mode.getDefaultExtension())) ?
      current.getPrettyName() : current.getFileName();
    promptForTabName(prompt + ":", oldName);
  }


  /**
   * Displays a dialog for renaming or creating a new tab
   */
  protected void promptForTabName(String prompt, String oldName) {
    final JTextField field = new JTextField(oldName);

    field.addKeyListener(new KeyAdapter() {
      // Forget ESC, the JDialog should handle it.
      // Use keyTyped to catch when the feller is actually added to the text
      // field. With keyTyped, as opposed to keyPressed, the keyCode will be
      // zero, even if it's enter or backspace or whatever, so the keychar
      // should be used instead. Grr.
      public void keyTyped(KeyEvent event) {
        //System.out.println("got event " + event);
        char ch = event.getKeyChar();
        if ((ch == '_') || (ch == '.') || // allow.pde and .java
            (('A' <= ch) && (ch <= 'Z')) || (('a' <= ch) && (ch <= 'z'))) {
          // These events are allowed straight through.
        } else if (ch == ' ') {
          String t = field.getText();
          int start = field.getSelectionStart();
          int end = field.getSelectionEnd();
          field.setText(t.substring(0, start) + "_" + t.substring(end));
          field.setCaretPosition(start + 1);
          event.consume();
        } else if ((ch >= '0') && (ch <= '9')) {
          // getCaretPosition == 0 means that it's the first char
          // and the field is empty.
          // getSelectionStart means that it *will be* the first
          // char, because the selection is about to be replaced
          // with whatever is typed.
          if (field.getCaretPosition() == 0 ||
              field.getSelectionStart() == 0) {
            // number not allowed as first digit
            event.consume();
          }
        } else if (ch == KeyEvent.VK_ENTER) {
          // Slightly ugly hack that ensures OK button of the dialog consumes
          // the Enter key event. Since the text field is the default component
          // in the dialog, OK doesn't consume Enter key event, by default.
          Container parent = field.getParent();
          while (!(parent instanceof JOptionPane)) {
            parent = parent.getParent();
          }
          JOptionPane pane = (JOptionPane) parent;
          final JPanel pnlBottom = (JPanel)
            pane.getComponent(pane.getComponentCount() - 1);
          for (int i = 0; i < pnlBottom.getComponents().length; i++) {
            Component component = pnlBottom.getComponents()[i];
            if (component instanceof JButton) {
              final JButton okButton = (JButton) component;
              if (okButton.getText().equalsIgnoreCase("OK")) {
                ActionListener[] actionListeners =
                  okButton.getActionListeners();
                if (actionListeners.length > 0) {
                  actionListeners[0].actionPerformed(null);
                  event.consume();
                }
              }
            }
          }
        } else {
          event.consume();
        }
      }
    });

    int userReply = JOptionPane.showOptionDialog(editor, new Object[] {
                                                 prompt, field },
                                                 Language.text("editor.tab.new"),
                                                 JOptionPane.OK_CANCEL_OPTION,
                                                 JOptionPane.PLAIN_MESSAGE,
                                                 null, new Object[] {
                                                 Toolkit.PROMPT_OK,
                                                 Toolkit.PROMPT_CANCEL },
                                                 field);

    if (userReply == JOptionPane.OK_OPTION) {
      nameCode(field.getText());
    }
  }



  /**
   * This is called upon return from entering a new file name.
   * (that is, from either newCode or renameCode after the prompt)
   */
  protected void nameCode(String newName) {
    newName = newName.trim();
    if (newName.length() == 0) {
      return;
    }

    // make sure the user didn't hide the sketch folder
    ensureExistence();

    // Add the extension here, this simplifies some of the logic below.
    if (newName.indexOf('.') == -1) {
      newName += "." + (renamingCode ? mode.getDefaultExtension() : mode.getModuleExtension());
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

    if (newName.startsWith(".")) {
      Base.showWarning(Language.text("name.messages.problem_renaming"),
                       Language.text("name.messages.starts_with_dot.description"));
      return;
    }

    int dot = newName.lastIndexOf('.');
    String newExtension = newName.substring(dot+1).toLowerCase();
    if (!mode.validExtension(newExtension)) {
      Base.showWarning(Language.text("name.messages.problem_renaming"),
                       Language.interpolate("name.messages.invalid_extension.description",
                        newExtension));
      return;
    }

    // Don't let the user create the main tab as a .java file instead of .pde
    if (!mode.isDefaultExtension(newExtension)) {
      if (renamingCode) {  // If creating a new tab, don't show this error
        if (current == code[0]) {  // If this is the main tab, disallow
          Base.showWarning(Language.text("name.messages.problem_renaming"),
                           Language.interpolate("name.messages.main_java_extension.description",
                            newExtension));
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

    // If changing the extension of a file from .pde to .java, then it's ok.
    // http://code.google.com/p/processing/issues/detail?id=776
    // A regression introduced by Florian's bug report (below) years earlier.
    if (!(renamingCode && sanitaryName.equals(current.getPrettyName()))) {
      // Make sure no .pde *and* no .java files with the same name already exist
      // (other than the one we are currently attempting to rename)
      // http://processing.org/bugs/bugzilla/543.html
      for (SketchCode c : code) {
        if (c != current && sanitaryName.equalsIgnoreCase(c.getPrettyName())) {
          Base.showMessage(Language.text("name.messages.new_sketch_exists"),
                           Language.interpolate("name.messages.new_sketch_exists.description",
                            c.getFileName(), folder.getAbsolutePath()));
          return;
        }
      }
    }

    File newFile = new File(folder, newName);

    if (renamingCode) {
      if (currentIndex == 0) {
        // get the new folder name/location
        String folderName = newName.substring(0, newName.indexOf('.'));
        File newFolder = new File(folder.getParentFile(), folderName);
        if (newFolder.exists()) {
          Base.showWarning(Language.text("name.messages.new_folder_exists"),
                           Language.interpolate("name.messages.new_folder_exists.description",
                            newName));
          return;
        }

        // renaming the containing sketch folder
        boolean success = folder.renameTo(newFolder);
        if (!success) {
          Base.showWarning(Language.text("name.messages.error"),
            Language.text("name.messages.no_rename_folder.description"));
          return;
        }
        // let this guy know where he's living (at least for a split second)
        current.setFolder(newFolder);
        // folder will be set to newFolder by updateInternal()

        // unfortunately this can't be a "save as" because that
        // only copies the sketch files and the data folder
        // however this *will* first save the sketch, then rename

        // moved this further up in the process (before prompting for the name)
//        if (isModified()) {
//          Base.showMessage("Save", "Please save the sketch before renaming.");
//          return;
//        }

        // This isn't changing folders, just changes the name
        newFile = new File(newFolder, newName);
        if (!current.renameTo(newFile, newExtension)) {
          Base.showWarning(Language.text("name.messages.error"),
                           Language.interpolate("name.messages.no_rename_file.description",
                            current.getFileName(), newFile.getName()));
          return;
        }

        // Tell each code file the good news about their new home.
        // current.renameTo() above already took care of the main tab.
        for (int i = 1; i < codeCount; i++) {
          code[i].setFolder(newFolder);
        }
       // Update internal state to reflect the new location
        updateInternal(sanitaryName, newFolder);

//        File newMainFile = new File(newFolder, newName + ".pde");
//        String newMainFilePath = newMainFile.getAbsolutePath();
//
//        // having saved everything and renamed the folder and the main .pde,
//        // use the editor to re-open the sketch to re-init state
//        // (unfortunately this will kill positions for carets etc)
//        editor.handleOpenUnchecked(newMainFilePath,
//                                   currentIndex,
//                                   editor.getSelectionStart(),
//                                   editor.getSelectionStop(),
//                                   editor.getScrollPosition());
//
//        // get the changes into the sketchbook menu
//        // (re-enabled in 0115 to fix bug #332)
//        editor.base.rebuildSketchbookMenusAsync();

      } else {  // else if something besides code[0]
        if (!current.renameTo(newFile, newExtension)) {
          Base.showWarning(Language.text("name.messages.error"),
                           Language.interpolate("name.messages.no_rename_file.description",
                            current.getFileName(), newFile.getName()));
          return;
        }
      }

    } else {  // not renaming, creating a new file
      try {
        if (!newFile.createNewFile()) {
          // Already checking for IOException, so make our own.
          throw new IOException("createNewFile() returned false");
        }
      } catch (IOException e) {
        Base.showWarning(Language.text("name.messages.error"),
                         Language.interpolate("name.messages.no_create_file.description",
                          newFile, folder.getAbsolutePath()),
                         e);
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
    editor.rebuildHeader();
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
      Base.showMessage(Language.text("delete.messages.is_read_only"),
                       Language.text("delete.messages.is_read_only.description"));
      return;
    }

    // don't allow if untitled
    if (currentIndex == 0 && isUntitled()) {
      Base.showMessage(Language.text("delete.messages.cannot_delete"),
                       Language.text("delete.messages.cannot_delete.description"));
      return;
    }

    // confirm deletion with user, yes/no
    Object[] options = { Language.text("prompt.ok"), Language.text("prompt.cancel") };
    String prompt = (currentIndex == 0) ?
      Language.text("warn.delete.sketch") :
      Language.interpolate("warn.delete.file", current.getPrettyName());
    int result = JOptionPane.showOptionDialog(editor,
                                              prompt,
                                              Language.text("warn.delete"),
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
        Util.removeDir(folder);

        // get the changes into the sketchbook menu
        //sketchbook.rebuildMenus();

        // make a new sketch, and i think this will rebuild the sketch menu
        //editor.handleNewUnchecked();
        //editor.handleClose2();
        editor.getBase().handleClose(editor, false);

      } else {
        // delete the file
        if (!current.deleteFile()) {
          Base.showMessage(Language.text("delete.messages.cannot_delete.file"),
                           Language.text("delete.messages.cannot_delete.file.description")+" \"" +
                           current.getFileName() + "\".");
          return;
        }

        // remove code from the list
        removeCode(current);

        // just set current tab to the main tab
        setCurrentCode(0);

        // update the tabs
        editor.rebuildHeader();
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
    //new Exception().printStackTrace(System.out);
    if (current.isModified() != state) {
      current.setModified(state);
      calcModified();
    }
  }


  protected void calcModified() {
    modified = false;
    for (int i = 0; i < codeCount; i++) {
      if (code[i].isModified()) {
        modified = true;
        break;
      }
    }
    editor.repaintHeader();

    if (Platform.isMacOS()) {
      // http://developer.apple.com/qa/qa2001/qa1146.html
      Object modifiedParam = modified ? Boolean.TRUE : Boolean.FALSE;
      // https://developer.apple.com/library/mac/technotes/tn2007/tn2196.html#WINDOW_DOCUMENTMODIFIED
      editor.getRootPane().putClientProperty("Window.documentModified", modifiedParam);
    }
  }


  public boolean isModified() {
    return modified;
  }


  /**
   * Save all code in the current sketch. This just forces the files to save
   * in place, so if it's an untitled (un-saved) sketch, saveAs() should be
   * called instead. (This is handled inside Editor.handleSave()).
   */
  public boolean save() throws IOException {
    // make sure the user didn't hide the sketch folder
    ensureExistence();

    // first get the contents of the editor text area
//    if (current.isModified()) {
    current.setProgram(editor.getText());
//    }

    // don't do anything if not actually modified
    //if (!modified) return false;

    if (isReadOnly()) {
      // if the files are read-only, need to first do a "save as".
      Base.showMessage(Language.text("save_file.messages.is_read_only"),
                       Language.text("save_file.messages.is_read_only.description"));
      // if the user cancels, give up on the save()
      if (!saveAs()) return false;
    }

    for (SketchCode sc : code) {
      if (sc.isModified()) {
        sc.save();
      }
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
  public boolean saveAs() throws IOException {
    String newParentDir = null;
    String newName = null;

    final String oldName2 = folder.getName();
    // TODO rewrite this to use shared version from PApplet
    final String PROMPT = Language.text("save");
    if (Preferences.getBoolean("chooser.files.native")) {
      // get new name for folder
      FileDialog fd = new FileDialog(editor, PROMPT, FileDialog.SAVE);
      if (isReadOnly() || isUntitled()) {
        // default to the sketchbook folder
        fd.setDirectory(Preferences.getSketchbookPath());
      } else {
        // default to the parent folder of where this was
        fd.setDirectory(folder.getParent());
      }
      String oldName = folder.getName();
      fd.setFile(oldName);
      fd.setVisible(true);
      newParentDir = fd.getDirectory();
      newName = fd.getFile();
    } else {
      JFileChooser fc = new JFileChooser();
      fc.setDialogTitle(PROMPT);
      if (isReadOnly() || isUntitled()) {
        // default to the sketchbook folder
        fc.setCurrentDirectory(new File(Preferences.getSketchbookPath()));
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
    }

    // user canceled selection
    if (newName == null) return false;

    // check on the sanity of the name
    String sanitaryName = Sketch.checkName(newName);
    File newFolder = new File(newParentDir, sanitaryName);
    if (!sanitaryName.equals(newName) && newFolder.exists()) {
      Base.showMessage(Language.text("save_file.messages.sketch_exists"),
                       Language.interpolate("save_file.messages.sketch_exists.description",
                        sanitaryName));
      return false;
    }
    newName = sanitaryName;

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
        Base.showMessage(Language.text("save_file.messages.tab_exists"),
                         Language.interpolate("save_file.messages.tab_exists.description",
                          newName));
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
        Base.showWarning(Language.text("save_file.messages.recursive_save"),
                         Language.text("save_file.messages.recursive_save.description"),
                         null);
        return false;
      }
    } catch (IOException e) { }

    // if the new folder already exists, then first remove its contents before
    // copying everything over (user will have already been warned).
    if (newFolder.exists()) {
      Util.removeDir(newFolder);
    }
    // in fact, you can't do this on Windows because the file dialog
    // will instead put you inside the folder, but it happens on OS X a lot.

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


    final File newFolder2 = newFolder;
    final File[] copyItems2 = copyItems;
    final String newName2 = newName;

    // Create a new event dispatch thread- to display ProgressBar
    // while Saving As
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new ProgressFrame(copyItems2, newFolder2, oldName2, newName2, editor);
      }
    });


    // save the other tabs to their new location
    for (int i = 1; i < codeCount; i++) {
      File newFile = new File(newFolder, code[i].getFileName());
      code[i].saveAs(newFile);
    }

    // While the old path to the main .pde is still set, remove the entry from
    // the Recent menu so that it's not sticking around after the rename.
    // If untitled, it won't be in the menu, so there's no point.
    if (!isUntitled()) {
      Recent.remove(editor);
    }

    // save the main tab with its new name
    File newFile = new File(newFolder, newName + "." + mode.getDefaultExtension());
    code[0].saveAs(newFile);

    updateInternal(newName, newFolder);

    // Make sure that it's not an untitled sketch
    setUntitled(false);

    // Add this sketch back using the new name
    Recent.append(editor);

    // let Editor know that the save was successful
    return true;
  }



  /**
   * Update internal state for new sketch name or folder location.
   */
  protected void updateInternal(String sketchName, File sketchFolder) {
    // reset all the state information for the sketch object
    String oldPath = getMainFilePath();
    primaryFile = code[0].getFile();
//    String newPath = getMainFilePath();
//    editor.base.renameRecent(oldPath, newPath);

    name = sketchName;
    folder = sketchFolder;
    codeFolder = new File(folder, "code");
    dataFolder = new File(folder, "data");

    // set the main file to be the current tab
    //setCurrentCode(0);
    // nah, this might just annoy people

    // Name changed, rebuild the sketch menus
    calcModified();
//    System.out.println("modified is now " + modified);
    editor.updateTitle();
    editor.getBase().rebuildSketchbookMenus();
    Recent.rename(editor, oldPath);
//    editor.header.rebuild();
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
      Base.showMessage(Language.text("add_file.messages.is_read_only"),
                       Language.text("add_file.messages.is_read_only.description"));
      return;
    }

    // get a dialog, select a file to add to the sketch
    String prompt = Language.text("file");
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
//      editor.statusNotice("One file added to the sketch.");
    	//Done from within TaskAddFile inner class when copying is completed
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
      Object[] options = { Language.text("prompt.ok"), Language.text("prompt.cancel") };
      String prompt = Language.interpolate("add_file.messages.confirm_replace",
                                           filename);
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
        Base.showWarning(Language.text("add_file.messages.error_adding"),
                         Language.interpolate("add_file.messages.cannot_delete.description",
                          filename),
                         null);
        return false;
      }
    }

    // make sure they aren't the same file
    if ((codeExtension == null) && sourceFile.equals(destFile)) {
      Base.showWarning(Language.text("add_file.messages.same_file"),
                       Language.text("add_file.messages.same_file.description"),
                       null);
      return false;
    }

    // Handles "Add File" when a .pde is used. For beta 1, this no longer runs
    // on a separate thread because it's totally unnecessary (a .pde file is
    // not going to be so large that it's ever required) and otherwise we have
    // to introduce a threading block here.
    // https://github.com/processing/processing/issues/3383
    if (!sourceFile.equals(destFile)) {
      try {
        Util.copyFile(sourceFile, destFile);

      } catch (IOException e) {
        Base.showWarning(Language.text("add_file.messages.error_adding"),
                         Language.interpolate("add_file.messages.cannot_add.description",
                          filename),
                         e);
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
      editor.repaintHeader();
      if (isUntitled()) {  // TODO probably not necessary? problematic?
        // Mark the new code as modified so that the sketch is saved
        current.setModified(true);
      }

    } else {
      if (isUntitled()) {  // TODO probably not necessary? problematic?
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
//    // for the tab sizing
//    if (current != null) {
//      current.visited = System.currentTimeMillis();
//      System.out.println(current.visited);
//    }
    // if current is null, then this is the first setCurrent(0)
    if (((currentIndex == which) && (current != null))
      || which >= codeCount || which < 0) {
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
    current.visited = System.currentTimeMillis();

    editor.setCode(current);
    editor.repaintHeader();
  }


  /**
   * Internal helper function to set the current tab based on a name.
   * @param findName the file name (not pretty name) to be shown
   */
  public void setCurrentCode(String findName) {
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
      File buildFolder = Base.createTempFolder(name, "temp", null);
//      if (buildFolder.mkdirs()) {
      return buildFolder;

//      } else {
//        Base.showWarning("Build folder bad",
//                         "Could not create a place to build the sketch.", null);
//      }
    } catch (IOException e) {
      Base.showWarning(Language.text("temp_dir.messages.bad_build_folder"),
                       Language.text("temp_dir.messages.bad_build_folder.description"),
                       e);
    }
    return null;
  }


  /**
   * When running from the editor, take care of preparations before running
   * a build or an export. Also erases and/or creates 'targetFolder' if it's
   * not null, and if preferences say to do so when exporting.
   * @param targetFolder is something like applet, application, android...
   */
  /*
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
  */


  /**
   * Make sure the sketch hasn't been moved or deleted by some
   * nefarious user. If they did, try to re-create it and save.
   * Only checks to see if the main folder is still around,
   * but not its contents.
   */
  public void ensureExistence() {
    if (!folder.exists()) {
      // Disaster recovery, try to salvage what's there already.
      Base.showWarning(Language.text("ensure_exist.messages.missing_sketch"),
                       Language.text("ensure_exist.messages.missing_sketch.description"),
                       null);
      try {
        folder.mkdirs();
        modified = true;

        for (int i = 0; i < codeCount; i++) {
          code[i].save();  // this will force a save
        }
        calcModified();

      } catch (Exception e) {
        Base.showWarning(Language.text("ensure_exist.messages.unrecoverable"),
                         Language.text("ensure_exist.messages.unrecoverable.description"),
                         e);
      }
    }
  }


  /**
   * Returns true if this is a read-only sketch. Used for the
   * examples directory, or when sketches are loaded from read-only
   * volumes or folders without appropriate permissions.
   */
  public boolean isReadOnly() {
    String apath = folder.getAbsolutePath();
    List<Mode> modes = editor.getBase().getModeList();
    // Make sure it's not read-only for another Mode besides this one
    // https://github.com/processing/processing/issues/773
    for (Mode mode : modes) {
      if (apath.startsWith(mode.getExamplesFolder().getAbsolutePath()) ||
          apath.startsWith(mode.getLibrariesFolder().getAbsolutePath())) {
        return true;
      }
    }

    // check to see if each modified code file can be written to
    // canWrite() doesn't work on directories
    for (int i = 0; i < codeCount; i++) {
      if (code[i].isModified() &&
        code[i].fileReadOnly() &&
        code[i].fileExists()) {
        //System.err.println("found a read-only file " + code[i].file);
        return true;
      }
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
   * Returns a File object for the main .pde file for this sketch.
   */
  public File getMainFile() {
    return primaryFile;
  }


  /**
   * Returns path to the main .pde file for this sketch.
   */
  public String getMainFilePath() {
    return primaryFile.getAbsolutePath();
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


  public int getCurrentCodeIndex() {
    return currentIndex;
  }


  public String getMainProgram() {
    return getCode(0).getProgram();
  }


  public void setUntitled(boolean untitled) {
//    editor.untitled = u;
    this.untitled = untitled;
    editor.updateTitle();
  }


  public boolean isUntitled() {
//    return editor.untitled;
    return untitled;
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
        Language.text("check_name.messages.is_name_modified");
      System.out.println(msg);
    }
    return newName;
  }


  /**
   * Return true if the name is valid for a Processing sketch. Extensions of the form .foo are
   * ignored.
   */
  public static boolean isSanitaryName(String name) {
    final int dot = name.lastIndexOf('.');
    if (dot >= 0) {
      name = name.substring(0, dot);
    }
    return sanitizeName(name).equals(name);
  }


  static final boolean asciiLetter(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
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
   * underscores. Also disallows starting the sketch name with a digit
   * or underscore.
   * <p/>
   * In Processing 2.0, sketches can no longer begin with an underscore,
   * because these aren't valid class names on Android.
   */
  static public String sanitizeName(String origName) {
    char orig[] = origName.toCharArray();
    StringBuilder sb = new StringBuilder();

    // Can't lead with a digit (or anything besides a letter), so prefix with
    // "sketch_". In 1.x this prefixed with an underscore, but those get shaved
    // off later, since you can't start a sketch name with underscore anymore.
    if (!asciiLetter(orig[0])) {
      sb.append("sketch_");
    }
//    for (int i = 0; i < orig.length; i++) {
    for (char c : orig) {
      if (asciiLetter(c) || (c >= '0' && c <= '9')) {
        sb.append(c);

      } else {
        // Tempting to only add if prev char is not underscore, but that
        // might be more confusing if lots of chars are converted and the
        // result is a very short string thats nothing like the original.
        sb.append('_');
      }
    }
    // Let's not be ridiculous about the length of filenames.
    // in fact, Mac OS 9 can handle 255 chars, though it can't really
    // deal with filenames longer than 31 chars in the Finder.
    // Limiting to that for sketches would mean setting the
    // upper-bound on the character limit here to 25 characters
    // (to handle the base name + ".class")
    if (sb.length() > 63) {
      sb.setLength(63);
    }
    // Remove underscores from the beginning, these seem to be a reserved
    // thing on Android, plus it sometimes causes trouble elsewhere.
    int underscore = 0;
    while (underscore < sb.length() && sb.charAt(underscore) == '_') {
      underscore++;
    }
    if (underscore == sb.length()) {
      return "bad_sketch_name_please_fix";

    } else if (underscore != 0) {
      return sb.substring(underscore);
    }
    return sb.toString();
  }


  public Mode getMode() {
    return mode;
  }
}
