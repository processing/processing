/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdePreprocessor - default cup-generated parser (not yet implemented)
  Part of the Processing project - http://Proce55ing.net

  Copyright (c) 2001-03 
  Ben Fry, Massachusetts Institute of Technology and 
  Casey Reas, Interaction Design Institute Ivrea

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

public class PdePreprocessor {

  static final String applet_imports[] = {
    "java.applet", "java.awt", "java.awt.image", "java.awt.event",
    "java.io", "java.net", "java.text", "java.util", "java.util.zip"
  };

  static final String application_imports[] = {
    "java.applet", "java.awt", "java.awt.image", "java.awt.event",
    "java.io", "java.net", "java.text", "java.util", "java.util.zip",
#ifndef RXTX
    "javax.comm",
#else
    "gnu.io",
#endif

    // if jdk14 defined, jdk13 will be as well
#ifdef JDK13
    "javax.sound.midi", "javax.sound.midi.spi",
    "javax.sound.sampled", "javax.sound.sampled.spi",
#endif

#ifdef JDK14
    "javax.xml.parsers", "javax.xml.transform", 
    "javax.xml.transform.dom", "javax.xml.transform.sax",
    "javax.xml.transform.stream", "org.xml.sax",
    "org.xml.sax.ext", "org.xml.sax.helpers"
#endif
  };

  static final int BEGINNER     = 0;
  static final int INTERMEDIATE = 1;
  static final int ADVANCED     = 2;

  String tempClass;
  String tempFilename;
  String tempClassFilename;

  Reader programReader;
  String buildPath;

  boolean usingExternal; // use an external process to display the applet?

  public PdePreprocessor(String program, String buildPath) {
    this.programReader = new StringReader(program);
    this.buildPath = buildPath;

    usingExternal = PdeBase.getBoolean("play.external", false);
  }

  public String writeJava(String name, boolean extendsNormal,
                          boolean exporting) throws java.lang.Exception {

    String extendsWhat = extendsNormal ? "BApplet" : "BAppletGL";

    return "";
  }
}
