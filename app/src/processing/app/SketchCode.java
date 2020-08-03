/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  SketchCode - data class for a single file inside a sketch
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

import java.io.*;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.undo.*;


/**
 * Represents a single tab of a sketch.
 */
public class SketchCode {
  /** Pretty name (no extension), not the full file name */
  private String prettyName;

  /** File object for where this code is located */
  private File file;

  /** Extension for this file (no dots, and in lowercase). */
  private String extension;

  /** Text of the program text for this tab */
  private String program;

  /** Last version of the program on disk. */
  private String savedProgram;

  /** Document object for this tab. Currently this is a SyntaxDocument. */
  private Document document;

  /** Last time this tab was visited */
  long visited;

  /** The last time this tab was saved to disk */
  private long lastModified;

  /**
   * Undo Manager for this tab, each tab keeps track of their own
   * Editor.undo will be set to this object when this code is the tab
   * that's currently the front.
   */
  private UndoManager undo = new UndoManager();

  /** What was on top of the undo stack when last saved. */
//  private UndoableEdit lastEdit;

  // saved positions from last time this tab was used
  private int selectionStart;
  private int selectionStop;
  private int scrollPosition;

  private boolean modified;

  /** name of .java file after preproc */
//  private String preprocName;
  /** where this code starts relative to the concat'd code */
  private int preprocOffset;


  public SketchCode(File file, String extension) {
    this.file = file;
    this.extension = extension;

    makePrettyName();

    try {
      load();
    } catch (IOException e) {
      System.err.println("Error while loading code " + file.getName());
    }
  }


  protected void makePrettyName() {
    prettyName = file.getName();
    int dot = prettyName.lastIndexOf('.');
    prettyName = prettyName.substring(0, dot);
  }


  public File getFile() {
    return file;
  }


  protected boolean fileExists() {
    return file.exists();
  }


  protected boolean fileReadOnly() {
    return !file.canWrite();
  }


  protected boolean deleteFile() {
    return file.delete();
  }


  protected boolean renameTo(File what, String ext) {
//    System.out.println("renaming " + file);
//    System.out.println("      to " + what);
    boolean success = file.renameTo(what);
    if (success) {
      this.file = what;  // necessary?
      this.extension = ext;
      makePrettyName();
    }
    return success;
  }


  public void copyTo(File dest) throws IOException {
    Util.saveFile(program, dest);
  }


  public String getFileName() {
    return file.getName();
  }


  public String getPrettyName() {
    return prettyName;
  }


  public String getExtension() {
    return extension;
  }


  public boolean isExtension(String what) {
    return extension.equals(what);
  }


  /** get the current text for this tab */
  public String getProgram() {
    return program;
  }


  /** set the current text for this tab */
  public void setProgram(String replacement) {
    program = replacement;
  }


  /** get the last version saved of this tab */
  public String getSavedProgram() {
    return savedProgram;
  }


  public int getLineCount() {
    return Util.countLines(program);
  }


  public void setModified(boolean modified) {
    this.modified = modified;
  }


  public boolean isModified() {
    return modified;
  }


//  public void setPreprocName(String preprocName) {
//    this.preprocName = preprocName;
//  }
//
//
//  public String getPreprocName() {
//    return preprocName;
//  }


  public void setPreprocOffset(int preprocOffset) {
    this.preprocOffset = preprocOffset;
  }


  public int getPreprocOffset() {
    return preprocOffset;
  }


  public void addPreprocOffset(int extra) {
    preprocOffset += extra;
  }


  public Document getDocument() {
    return document;
  }


  public String getDocumentText() throws BadLocationException {
    return document.getText(0, document.getLength());
  }


  public void setDocument(Document d) {
    document = d;
  }


  public UndoManager getUndo() {
    return undo;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // TODO these could probably be handled better, since it's a general state
  // issue that's read/write from only one location in Editor (on tab switch.)


  public int getSelectionStart() {
    return selectionStart;
  }


  public int getSelectionStop() {
    return selectionStop;
  }


  public int getScrollPosition() {
    return scrollPosition;
  }


  protected void setState(String p, int start, int stop, int pos) {
    program = p;
    selectionStart = start;
    selectionStop = stop;
    scrollPosition = pos;
  }


  public long lastVisited() {
    return visited;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Load this piece of code from a file.
   */
  public void load() throws IOException {
    program = Util.loadFile(file);

    if (program == null) {
      System.err.println("There was a problem loading " + file);
      System.err.println("This may happen because you don't have permissions to read the file, or the file has gone missing.");
      throw new IOException("Cannot read or access " + file);
    }

    // Remove NUL characters because they'll cause problems,
    // and their presence is very difficult to debug.
    // https://github.com/processing/processing/issues/1973
    if (program.indexOf('\0') != -1) {
      program = program.replaceAll("\0", "");
    }
    savedProgram = program;

    // This used to be the "Fix Encoding and Reload" warning, but since that
    // tool has been removed, let's ramble about text editors and encodings.
    if (program.indexOf('\uFFFD') != -1) {
      System.err.println(file.getName() + " contains unrecognized characters.");
      System.err.println("You should re-open " + file.getName() +
                         " with a text editor,");
      System.err.println("and re-save it in UTF-8 format. Otherwise, you can");
      System.err.println("delete the bad characters to get rid of this warning.");
      System.err.println();
    }

    setLastModified();
    setModified(false);
  }


  /**
   * Save this piece of code, regardless of whether the modified
   * flag is set or not.
   */
  public void save() throws IOException {
    // TODO re-enable history
    //history.record(s, SketchHistory.SAVE);

    Util.saveFile(program, file);
    savedProgram = program;
    lastModified = file.lastModified();
    setModified(false);
  }


  /**
   * Save this file to another location, used by Sketch.saveAs()
   */
  public void saveAs(File newFile) throws IOException {
    Util.saveFile(program, newFile);
    savedProgram = program;
    file = newFile;
    makePrettyName();
    setLastModified();
    setModified(false);
  }


  /**
   * Called when the sketch folder name/location has changed. Called when
   * renaming tab 0, the main code.
   */
  public void setFolder(File sketchFolder) {
    file = new File(sketchFolder, file.getName());
  }


  /**
   * Set the last known modification time, so that we're not re-firing
   * "hey, this is modified!" events incessantly.
   */
  public void setLastModified() {
    lastModified = file.lastModified();
  }


  /**
   * Used to determine whether this file was modified externally
   * @return The time the file was last modified
   */
  public long getLastModified() {
    return lastModified;
  }
}
