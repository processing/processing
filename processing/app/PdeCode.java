/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeCode - data class for a single file inside a sketch
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry and is
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

import java.io.*;


public class PdeCode {
  String name;  // pretty name (no extension), not the full file name
  File file;
  int flavor;

  String program;
  boolean modified;
  //PdeHistory history;  // TODO add history information

  String preprocName;  // name of .java file after preproc


  public PdeCode(String name, File file, int flavor) {
    this.name = name;
    this.file = file;
    this.flavor = flavor;

    try {
      load();
    } catch (IOException e) {
      System.err.println("error while loading code " + name);
    }
  }


  public void load() throws IOException {
    program = PdeBase.loadFile(file);

    //program = null;
    /*
    } catch (IOException e) {
      PdeBase.showWarning("Error loading file", 
                          "Error while opening the file\n" + 
                          file.getPath(), e);
      program = null;  // just in case
    */

    //if (program != null) {
    //history = new History(file);
    //}
  }


  public void save() throws IOException {
    // TODO re-enable history
    //history.record(s, PdeHistory.SAVE);

    try {
      //System.out.println("saving to " + file);
      //System.out.println("stuff to save: " + program);
      //System.out.println("-------");
      PdeBase.saveFile(program, file);

    } catch (Exception e) {
      PdeBase.showWarning("Error saving file",
                          "Could not save " + file + "\n" +
                          "because of an error.", e);
    }
    modified = false;
  }
}
