/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-15 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.pdex;

/**
 * Wrapper for import statements
 * 
 * @author Manindra Moharana &lt;me@mkmoharana.com&gt;
 *
 */
public class ImportStatement {
	/**
	 * Ex: processing.opengl.*, java.util.* 
	 */
	private String importName;	

  /**
	 * Which tab does it belong to?
	 */
  private int tab;

	/**
	 * Line number(pde code) of the import
	 */
  private int lineNumber;

	/**
	 * 
	 * @param importName - Ex: processing.opengl.*, java.util.* 
	 * @param tab - Which tab does it belong to?
	 * @param lineNumber - Line number(pde code) of the import
	 */
	public ImportStatement(String importName, int tab, int lineNumber) {
		this.importName = importName;
		this.tab = tab;
		this.lineNumber = lineNumber;
	}
	
	public String getImportName() {
    return importName;
  }
	
	public String getPackageName(){
	  String ret = new String(importName.trim());
	  if(ret.startsWith("import "))
	    ret = ret.substring(7);
	  if(ret.endsWith(";"))
	    ret = ret.substring(0, ret.length() - 1).trim();
	  if(ret.endsWith(".*"))
	    ret = ret.substring(0, ret.length() - 2);
	  return ret;
	}

  public int getTab() {
    return tab;
  }

  public int getLineNumber() {
    return lineNumber;
  }
}