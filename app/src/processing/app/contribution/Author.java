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

public class Author {

  String name;

  String url;

  public Author(String text) {
    
    text = text.trim();
    name = "";
    url = "";
    
    if (isUrlSyntax(text)) {
      // Use link syntax
      name = getText(text).trim();
      url = getUrl(text);
    } else {
      name = text;
    }
      
  }

  public Author(String name, String url) {
    this.name = name;
    this.url = url;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }
  
  static public boolean isUrlSyntax(String text) {
    return text.matches("\\[.*\\]\\(.*\\)");
  }
  
  static public String getText(String text) {
    if (isUrlSyntax(text))
      return text.substring(1, text.indexOf(']'));
    return null;
  }
  
  static public String getUrl(String text) {
    if (isUrlSyntax(text))
      return text.substring(text.indexOf('(') + 1, text.length() - 1).trim();
    return null;
  }
  
}
