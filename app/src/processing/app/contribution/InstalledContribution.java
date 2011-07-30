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

public abstract class InstalledContribution implements Contribution {

  protected String name;              // "pdf" or "PDF Export"
  protected String category;          // "Sound"
  protected String authorList;  // Ben Fry
  protected String url;               // http://processing.org
  protected String sentence;          // Write graphics to PDF files.
  protected String paragraph;         // <paragraph length description for site>
  protected int version;              // 102
  protected int latestVersion;        // 103
  protected String prettyVersion;     // "1.0.2"
  
  protected File folder;

  protected HashMap<String, String> properties;
  
  public InstalledContribution(File folder, String propertiesFileName) {
    
    this.folder = folder;
    
    File propertiesFile = new File(folder, propertiesFileName);

    properties = Base.readSettings(propertiesFile);
    category = "Unknown";

    name = properties.get("name");
    if (name == null) {
      name = folder.getName();
    }

    authorList = properties.get("authorList");

    url = properties.get("url");
    sentence = properties.get("sentence");
    paragraph = properties.get("paragraph");

    try {
      version = Integer.parseInt(properties.get("version"));
    } catch (NumberFormatException e) {
    }
    prettyVersion = properties.get("prettyVersion");
  }
  
  public File getFolder() {
    return folder;
  }
  
  public boolean isInstalled() {
    return folder != null;
  }
  
  public String getCategory() {
    return category;
  }
  
  public String getName() {
    return name;
  }
  
  public String getAuthorList() {
    return authorList;
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
  
}
