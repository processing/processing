/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Archiver - plugin tool for archiving sketches
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry and
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

//package processing.app.tools;  // for 0071+

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.lang.reflect.*;
//import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;


public class Archiver {
  PdeEditor editor;

  // someday these will be settable
  boolean useDate = false;
  int digits = 3;

  NumberFormat numberFormat;
  SimpleDateFormat dateFormat;


  public void setup(PdeEditor editor) {
    this.editor = editor;

    numberFormat = NumberFormat.getInstance();
    numberFormat.setGroupingUsed(false); // no commas
    numberFormat.setMinimumIntegerDigits(digits);

    dateFormat = new SimpleDateFormat("yyMMdd");
  }


  public void show() {
    File location = editor.sketch.folder;
    String name = location.getName();
    File parent = new File(location.getParent());

    //System.out.println("loc " + location);
    //System.out.println("par " + parent);

    File newbie = null;
    int index = 0;
    do {
      if (useDate) {
	String purty = dateFormat.format(new Date());
	String stamp = purty + ((char) ('a' + index));
	newbie = new File(parent, name + "-" + stamp + ".zip");

      } else {
	String diggie = numberFormat.format(index + 1);
	newbie = new File(parent, name + "-" + diggie + ".zip");
      }
    } while (newbie.exists());

    //System.out.println(newbie);
    FileOutputStream zipOutputFile = new FileOutputStream(newbie);
    ZipOutputStream zos = new ZipOutputStream(zipOutputFile);

    // close up the jar file
    zos.flush();
    zos.close();
  }


  public void buildZip(File dir, String sofar, 
		       ZipOutputStream zos) throws IOException {
    String files[] = dir.list();
    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || 
	  files[i].equals("..")) continue;

      File sub = new File(dir, files[i]);
      String nowfar = (sofar == null) ? 
        files[i] : (sofar + "/" + files[i]);

      if (sub.isDirectory()) {
	buildZip(sub, nowfar, zos);

      } else {
	ZipEntry entry = new ZipEntry(nowfar);
	zos.putNextEntry(entry);
	zos.write(PdeBase.grabFile(sub));
	zos.closeEntry();
      }
    }
  }
}


    /*
    int index = 0;
    SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
    String purty = formatter.format(new Date());
    do {
      newbieName = "sketch_" + purty + ((char) ('a' + index));
      newbieDir = new File(newbieParentDir, newbieName);
      index++;
    } while (newbieDir.exists());
    */
