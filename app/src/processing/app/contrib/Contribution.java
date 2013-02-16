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


abstract public class Contribution {
  protected String category;      // "Sound"
  protected String name;          // "pdf" or "PDF Export"
  protected String authorList;    // Ben Fry
  protected String url;           // http://processing.org
  protected String sentence;      // Write graphics to PDF files.
  protected String paragraph;     // <paragraph length description for site>
  protected int version;          // 102
  protected String prettyVersion; // "1.0.2"
  
  
  // "Sound"
  public String getCategory() {
    return category;
  }


  // "pdf" or "PDF Export"
  public String getName() {
    return name;
  }


  // "[Ben Fry](http://benfry.com/)"
  public String getAuthorList() {
    return authorList;
  }


  // "http://processing.org"
  public String getUrl() {
    return url;
  }


  // "Write graphics to PDF files."
  public String getSentence() {
    return sentence;
  }


  // <paragraph length description for site>
  public String getParagraph() {
    return paragraph;
  }


  // 102
  public int getVersion() {
    return version;
  }


  // "1.0.2"
  public String getPrettyVersion() {
    return prettyVersion;
  }


  abstract public ContributionType getType();
  
  
  public String getTypeName() {
    return getType().toString();
  }
  
  
  abstract public boolean isInstalled();
  
  
  /** 
   * Returns true if the type of contribution requires the PDE to restart
   * when being added or removed. 
   */
  public boolean requiresRestart() {
    return getType() == ContributionType.TOOL || getType() == ContributionType.MODE;
  }
  

  /** Overridden by InstalledContribution. */
  boolean isDeletionFlagged() {
    return false;
  }
}
