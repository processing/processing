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

package processing.app.contribution;

import java.util.*;

public abstract class AbstractContribution implements Contribution {
  
  public String name;             // "pdf" or "PDF Export"
  public String category;         // "Sound"
  public List<Author> authorList; // Ben Fry
  public String url;              // http://processing.org
  public String sentence;         // Write graphics to PDF files.
  public String paragraph;        // <paragraph length description for site>
  public int version;             // 102
  public int latestVersion;       // 103
  public String prettyVersion;    // "1.0.2"
  
  public String getCategory() {
    return category;
  }

  public String getName() {
    return name;
  }

  public List<Author> getAuthorList() {
    return new ArrayList<Author>(authorList);
  }

  public String getUrl() {
    return url;
  }

  public String getSentence() {
    return sentence;
  }

  public String getParagraph() {
    return paragraph;
  }

  public int getVersion() {
    return version;
  }

  public int getLatestVersion() {
    return latestVersion;
  }

  public String getPrettyVersion() {
    return prettyVersion;
  }

  public int compareTo(Contribution o) {
    return getName().toLowerCase().compareTo(o.getName().toLowerCase());
  }

  /**
   * @param string semicolin separated list of strings
   * @return List containing the trimmed elements from input string
   */
  public static List<String> toList(String string) {
    List<String> list = new ArrayList<String>();
    if (string != null) {
      String[] listAsArray = string.split(";");
      for (String element : listAsArray) {
        list.add(element);
      }
    }
    return list;
  }
  
}
