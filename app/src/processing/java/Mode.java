/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2010 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.java;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import processing.app.*;


public class Mode {
  // these are static because they're used by Sketch
  static private File examplesFolder;
  static private File librariesFolder;
  static private File toolsFolder;

  ArrayList<LibraryFolder> coreLibraries;
  ArrayList<LibraryFolder> contribLibraries;

  // maps imported packages to their library folder
//  static HashMap<String, File> importToLibraryTable;
  static HashMap<String, LibraryFolder> importToLibraryTable;

  // classpath for all known libraries for p5
  // (both those in the p5/libs folder and those with lib subfolders
  // found in the sketchbook)
//  static public String librariesClassPath;


//  public Editor createEditor(Base ibase, String path, int[] location) {
//  }
}