/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeCode - data class for a single file inside a sketch
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry
  Copyright (c) 2001-03 Massachusetts Institute of Technology

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
  String preprocName;  // name of .java file after preproc
  File file;
  int flavor;

  String program;
  boolean modified;
  //History history;  // later


  public PdeCode(String name, File file, int flavor) {
    this.name = name;
    this.file = file;
    this.flavor = flavor;
  }


  public void load() throws IOException {
    program = null;

    if (file.length() != 0) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(files[i])));
      StringBuffer buffer = new StringBuffer();
      String line = null;
      while ((line = reader.readLine()) != null) {
        buffer.append(line);
        buffer.append('\n');
      }
      reader.close();
      program = buffer.toString();

    } else {
      // empty code file.. no worries, might be getting filled up
      program = "";
    }

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

    //File file = new File(directory, filename);
    //try {
    //System.out.println("handleSave: results of getText");
    //System.out.print(s);
 
    ByteArrayInputStream bis = new ByteArrayInputStream(s.getBytes());
    InputStreamReader isr = new InputStreamReader(bis);
    BufferedReader reader = new BufferedReader(isr);

    FileWriter fw = new FileWriter(file);
    PrintWriter writer = new PrintWriter(new BufferedWriter(fw));

    String line = null;
    while ((line = reader.readLine()) != null) {
      //System.out.println("w '" + line + "'");
      writer.println(line);
    }
    writer.flush();
    writer.close();

    /*
    sketchFile = file;
    setSketchModified(false);
    message("Done saving " + filename + ".");
    */
  }
}
