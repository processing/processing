/*
  Part of the XQMode project - https://github.com/Manindra29/XQMode
  
  Under Google Summer of Code 2012 - 
  http://www.google-melange.com/gsoc/homepage/google/gsoc2012
  
  Copyright (C) 2012 Manindra Moharana
	
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

package processing.mode.experimental;

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
		String importName;
		/**
		 * Which tab does it belong to?
		 */
		int tab;
		
		/**
		 * Line number(pde code) of the import
		 */
		int lineNumber;

		public ImportStatement(String importName, int tab, int lineNumber) {
			this.importName = importName;
			this.tab = tab;
			this.lineNumber = lineNumber;
		}
	}