/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along 
  with this program; if not, write to the Free Software Foundation, Inc.
  59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package processing.app.contrib;

import java.io.File;

import processing.app.Base;

public enum ContributionType {
//    LIBRARY, LIBRARY_COMPILATION, TOOL, MODE;
    LIBRARY, TOOL, MODE;

    
    public String toString() {
      switch (this) {
      case LIBRARY:
        return "library";
//      case LIBRARY_COMPILATION:
//        return "compilation";
      case TOOL:
        return "tool";
      case MODE:
        return "mode";
      }
      return null;  // should be unreachable
    };
    
    
    public String getFolderName() {
      switch (this) {
      case LIBRARY:
        return "libraries";
//      case LIBRARY_COMPILATION:
//        return "libraries";
      case TOOL:
        return "tools";
      case MODE:
        return "modes";
      }
      return null;  // should be unreachable
    }
    
    
//    public String getPropertiesName() {
//      return toString() + ".properties";
//    }

    
    static public ContributionType fromName(String s) {
      if (s != null) {
        if ("library".equals(s.toLowerCase())) {
          return LIBRARY;
        }
//        if ("compilation".equals(s.toLowerCase())) {
//          return LIBRARY_COMPILATION;
//        }
        if ("tool".equals(s.toLowerCase())) {
          return TOOL;
        }
        if ("mode".equals(s.toLowerCase())) {
          return MODE;
        }
      }
      return null;
    }
    
    
    public File getSketchbookContribFolder() {
      switch (this) {
      case LIBRARY:
        return Base.getSketchbookLibrariesFolder();
      case TOOL:
        return Base.getSketchbookToolsFolder();
      case MODE:
        return Base.getSketchbookModesFolder();
      }
      return null;
    }

    
//    static public boolean validName(String s) {
//      return "library".equals(s) || "tool".equals(s) || "mode".equals(s); 
//    }
  }