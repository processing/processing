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

import java.util.regex.MatchResult;
import java.util.regex.Matcher;

/**
 * Wrapper for import statements
 *
 * @author Manindra Moharana &lt;me@mkmoharana.com&gt;
 *
 */
public class ImportStatement {

  private static final String importKw = "import";
  private static final String staticKw = "static";

//  private boolean isClass;
  private boolean isStarred;
  private boolean isStatic;

  /**
   * Full class name of the import with all packages
   * Ends with star for starred imports
   */
  private String className;

  /**
   * Name of the package e.g. everything before last dot
   */
  private String packageName;

  private ImportStatement() { }

  public static ImportStatement wholePackage(String pckg) {
    ImportStatement is = new ImportStatement();
    is.packageName = pckg;
    is.className = "*";
    is.isStarred = true;
    return is;
  }

  public static ImportStatement singleClass(String cls) {
    ImportStatement is = new ImportStatement();
    int lastDot = cls.lastIndexOf('.');
    is.className = lastDot >= 0 ? cls.substring(lastDot+1) : cls;
    is.packageName = lastDot >= 0 ? cls.substring(0, lastDot) : "";
//    is.isClass = true;
    return is;
  }



  public static ImportStatement parse(String importString) {
    Matcher matcher = SourceUtils.IMPORT_REGEX_NO_KEYWORD.matcher(importString);
    if (!matcher.find()) return null;

    return parse(matcher.toMatchResult());
  }

  public static ImportStatement parse(MatchResult match) {
    ImportStatement is = new ImportStatement();

    is.isStatic = match.group(2) != null;
    String pckg = match.group(3);
    pckg = (pckg == null) ? "" : pckg.replaceAll("\\s","");
    is.packageName = pckg.endsWith(".") ?
        pckg.substring(0, pckg.length()-1) :
        pckg;

    is.className = match.group(4);
    is.isStarred = is.className.equals("*");

    return is;
  }



  public String getFullSourceLine() {
    return importKw + " " + (isStatic ? (staticKw + " ") : "") + packageName + "." + className + ";";
  }

  public String getFullClassName(){
    return packageName + "." + className;
  }

  public String getClassName(){
    return className;
  }

  public String getPackageName(){
    return packageName;
  }

  public boolean isStarredImport() {
    return isStarred;
  }

  public boolean isStaticImport() {
    return isStatic;
  }

  public boolean isSameAs(ImportStatement is) {
    return packageName.equals(is.packageName) &&
        className.equals(is.className) &&
        isStatic == is.isStatic;
  }
}