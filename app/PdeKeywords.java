/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeKeywords - handles text coloring and links to html reference
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
import java.util.*;


public class PdeKeywords extends CTokenMarker {

  // lookup table for the TokenMarker subclass, handles coloring
  static KeywordMap keywordColoring;

  // lookup table that maps keywords to their html reference pages
  static Hashtable keywordToReference;


  public PdeKeywords() {
    super(false, getKeywords());
  }


  /**
   * Handles loading of keywords file. uses getKeywords() method 
   * because that's part of the TokenMarker classes.
   */
  static public KeywordMap getKeywords() {
    if (keywordColoring == null) {
      try {
        keywordColoring = new KeywordMap(false);
        keywordToReference = new Hashtable();

        InputStream input = PdeBase.getStream("keywords.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        String line = null;
        while ((line = reader.readLine()) != null) {
          int tab = line.indexOf('\t');
          if (tab == -1) continue;

          String keyword = line.substring(0, tab);
          String second = line.substring(tab + 1);
          tab = second.indexOf('\t');
          String coloring = second.substring(0, tab);
          String htmlFilename = second.substring(tab + 1);

          if (coloring.length() > 0) {
            // text will be KEYWORD or LITERAL
            boolean isKey = (coloring.charAt(0) == 'K');
            // KEYWORD1 -> 0, KEYWORD2 -> 1, etc
            int num = coloring.charAt(coloring.length() - 1) - '1';
            byte id = (byte) ((isKey ? Token.KEYWORD1 : Token.LITERAL1) + num);
            keywordColoring.add(keyword, id);
          }

          if (htmlFilename.length() > 0) {
            keywordToReference.put(keyword, htmlFilename);
          }
        }
        reader.close();

      } catch (Exception e) {
        PdeBase.showError("Problem loading keywords", 
                          "Could not find keywords.txt,\n" + 
                          "please re-install Processing.", e);
        System.exit(1);
      }
    }
    return keywordColoring;
  }


  static public String getReference(String keyword) {
    return (String) keywordToReference.get(keyword);
  }
}
