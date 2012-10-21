/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-11 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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

package processing.app.contrib;

public interface Contribution {
  // "Sound"
  String getCategory();

  // "pdf" or "PDF Export"
  String getName();

  // "[Ben Fry](http://benfry.com/)"
  String getAuthorList();

  // "http://processing.org"
  String getUrl();

  // "Write graphics to PDF files."
  String getSentence();

  // <paragraph length description for site>
  String getParagraph();

  // 102
  int getVersion();

  // "1.0.2"
  String getPrettyVersion();

  boolean isInstalled();

  Type getType();

  String getTypeName();


  public static enum Type {
    LIBRARY, LIBRARY_COMPILATION, TOOL, MODE;

    public String toString() {
      switch (this) {
      case LIBRARY:
        return "library";
      case LIBRARY_COMPILATION:
        return "compilation";
      case TOOL:
        return "tool";
      case MODE:
        return "mode";
      }
      return "contribution";
    };

    static public Type toType(String s) {
      if (s != null) {
        if ("library".equals(s.toLowerCase())) {
          return LIBRARY;
        }
        if ("compilation".equals(s.toLowerCase())) {
          return LIBRARY_COMPILATION;
        }
        if ("tool".equals(s.toLowerCase())) {
          return TOOL;
        }
        if ("mode".equals(s.toLowerCase())) {
          return MODE;
        }
      }
      return null;
    }
  }
}
