/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Archiver - plugin tool for archiving sketches
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-2015 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas

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

package processing.app.tools;

import processing.app.*;
import processing.app.ui.Editor;
import processing.core.PApplet;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;


public class Archiver implements Tool {
  Base base;
  Editor editor;

  // someday these will be settable
  boolean useDate;
  int digits = 3;

  NumberFormat numberFormat;
  SimpleDateFormat dateFormat;


  public String getMenuTitle() {
    return Language.text("menu.tools.archive_sketch");
  }


  public void init(Base base) {
    this.base = base;

    numberFormat = NumberFormat.getInstance();
    numberFormat.setGroupingUsed(false); // no commas
    numberFormat.setMinimumIntegerDigits(digits);

    dateFormat = new SimpleDateFormat("yyMMdd");
  }


  public void run() {
    editor = base.getActiveEditor();
    Sketch sketch = editor.getSketch();

    if (sketch.isModified()) {
      Messages.showWarning("Save", "Please save the sketch before archiving.");
      return;
    }

    File location = sketch.getFolder();
    String name = location.getName();
    File parent = new File(location.getParent());

    File newbie = null;
    String namely = null;
    int index = 0;
    do {
      // only use the date if the sketch name isn't the default name
      useDate = !name.startsWith("sketch_");

      if (useDate) {
        String purty = dateFormat.format(new Date());
        String stamp = purty + ((char) ('a' + index));
        namely = name + "-" + stamp;
        newbie = new File(parent, namely + ".zip");

      } else {
        String diggie = numberFormat.format(index + 1);
        namely = name + "-" + diggie;
        newbie = new File(parent, namely + ".zip");
      }
      index++;
    } while (newbie.exists());

    // open up a prompt for where to save this fella
    PApplet.selectOutput(Language.text("archive_sketch"),
                         "fileSelected", newbie, this, editor);
  }


  public void fileSelected(File newbie) {
    if (newbie != null) {
      try {
        // Force a .zip extension
        // https://github.com/processing/processing/issues/2526
        if (!newbie.getName().toLowerCase().endsWith(".zip")) {
          newbie = new File(newbie.getAbsolutePath() + ".zip");
        }
        //System.out.println(newbie);
        FileOutputStream zipOutputFile = new FileOutputStream(newbie);
        ZipOutputStream zos = new ZipOutputStream(zipOutputFile);

        // recursively fill the zip file
        File sketchFolder = editor.getSketch().getFolder();
        buildZip(sketchFolder, sketchFolder.getName(), zos);

        // close up the jar file
        zos.flush();
        zos.close();

        final String msg =
          Language.interpolate("editor.status.archiver.create",
                               newbie.getName());
        editor.statusNotice(msg);

      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      editor.statusNotice(Language.text("editor.status.archiver.cancel"));
    }
  }


  private void buildZip(File dir, String sofar,
                       ZipOutputStream zos) throws IOException {
    String files[] = dir.list();
    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || //$NON-NLS-1$
          files[i].equals("..")) continue; //$NON-NLS-1$

      File sub = new File(dir, files[i]);
      String nowfar = (sofar == null) ?
        files[i] : (sofar + "/" + files[i]); //$NON-NLS-1$

      if (sub.isDirectory()) {
        // directories are empty entries and have / at the end
        ZipEntry entry = new ZipEntry(nowfar + "/"); //$NON-NLS-1$
        //System.out.println(entry);
        zos.putNextEntry(entry);
        zos.closeEntry();
        buildZip(sub, nowfar, zos);

      } else {
        ZipEntry entry = new ZipEntry(nowfar);
        entry.setTime(sub.lastModified());
        zos.putNextEntry(entry);
        zos.write(Util.loadBytesRaw(sub));
        zos.closeEntry();
      }
    }
  }
}
