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

import java.io.File;
import java.util.*;

import processing.app.*;

public abstract class InstalledContribution extends AbstractContribution {

  protected File folder;

  protected HashMap<String, String> properties;
  
  public InstalledContribution(File folder) {
    
    this.folder = folder;
    
    if (folder != null) {
      File propertiesFile = new File(folder, "contribution.properties");
  
      properties = Base.readSettings(propertiesFile);
      category = "Unknown";
  
      name = properties.get("name");
      if (name == null) {
        name = folder.getName();
      }
  
      String authors = properties.get("authorList");
      authorList = new ArrayList<Author>();
      for (String authorName : toList(authors)) {
        Author author = new Author();
        author.name = authorName.trim();
  
        authorList.add(author);
      }
  
      url = properties.get("url");
      sentence = properties.get("sentence");
      paragraph = properties.get("paragraph");
  
      try {
        version = Integer.parseInt(properties.get("version"));
      } catch (NumberFormatException e) {
      }
      prettyVersion = properties.get("prettyVersion");
    }
  }
  
  public File getFolder() {
    return folder;
  }
  
  public boolean isInstalled() {
    return folder != null;
  }
  
}
