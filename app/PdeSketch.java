/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeSketch - stores information about files in the current sketch
  Part of the Processing project - http://Proce55ing.net

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

public class PdeSketch {
  String name; 
  File directory; 

  static final int PDE = 0;
  static final int JAVA = 1;

  int current;

  int fileCount;
  String names[];
  File files[];
  int flavor[]; 
  String program[];
  boolean modified[];

  int hiddenCount;
  String hiddenNames[];
  File hiddenFiles[];

  //String sketchName; // name of the file (w/o pde if a sketch)
  //File sketchFile;   // the .pde file itself
  //File sketchDir;    // if a sketchbook project, the parent dir
  //boolean sketchModified;


  /**
   * path is location of the main .pde file, because this is also
   * simplest to use when opening the file from the finder/explorer.
   */
  public PdeSketch(String path) {
    File mainFile = new File(path);
    System.out.println("main file is " + mainFile);
    directory = new File(path.getParent());
    System.out.println("sketch dir is " + directory);
  
    rebuild();
  }


  public void rebuild() {
    // get list of files in the folder
    String list[] = directory.list();

    for (int i = 0; i < list.length; i++) {
      if (list[i].endsWith(".pde")) fileCount++;
      else if (list[i].endsWith(".java")) fileCount++;
      else if (list[i].endsWith(".pde.x")) hiddenCount++;
      else if (list[i].endsWith(".java.x")) hiddenCount++;
    }

    names = new String[fileCount];
    files = new File[fileCount];
    modified = new boolean[fileCount];
    flavor = new int[fileCount];
    program = new String[fileCount];
    hiddenNames = new String[hiddenCount];
    hiddenFiles = new File[hiddenCount];

    int fileCounter = 0;
    int hiddenCounter = 0;

    for (int i = 0; i < list.length; i++) {
      int sub = 0;

      if (list[i].endsWith(".pde")) {
	names[fileCounter] = list[i].substring(0, list[i].length() - 4);
	files[fileCounter] = new File(directory, list[i]);
        flavor[fileCounter] = PDE;
	fileCounter++;

      } else if (list[i].endsWith(".java")) {
	names[fileCounter] = list[i].substring(0, list[i].length() - 5);
	files[fileCounter] = new File(directory, list[i]);
        flavor[fileCounter] = JAVA;
	fileCounter++;

      } else if (list[i].endsWith(".pde.x")) {
	names[hiddenCounter] = list[i].substring(0, list[i].length() - 6);
	files[hiddenCounter] = new File(directory, list[i]);
	hiddenCounter++;

      } else if (list[i].endsWith(".java.x")) {
	names[hiddenCounter] = list[i].substring(0, list[i].length() - 7);
	files[hiddenCounter] = new File(directory, list[i]);
	hiddenCounter++;
      }      
    }    
  }


  /**
   * Have the contents of the currently visible tab been modified.
   */
  /*
  public boolean isCurrentModified() {
    return modified[current];
  }
  */


  /**
   * Returns true if this is a read-only sketch. Used for the 
   * examples directory, or when sketches are loaded from read-only
   * volumes or folders without appropraite permissions.
   */
  public boolean isReadOnly() {
    return false;
  }


  /**
   * Path to the data folder of this sketch.
   */
  /*
  public File getDataDirectory() {
    File dataDir = new File(directory, "data");
    return dataDir.exists() ? dataDir : null;
  }
  */

  // copy contents of data dir
  // eventually, if the files already exist in the target, don't' bother.
  public void updateDataDirectory(File buildDir) {
    File dataDir = new File(directory, "data");
    if (dataDir.exists()) {
      PdeBase.copyDir(dataDir, buildDir);
    }
  }



  /**
   * Returns path to the main .pde file for this sketch.
   */
  public String getMainFilePath() {
    return files[0].getAbsolutePath();
  }
}
