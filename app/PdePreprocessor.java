/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdePreprocessor - wrapper for default ANTLR-generated parser
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry and
  Copyright (c) 2001-03 Massachusetts Institute of Technology

  ANTLR-generated parser and several supporting classes written 
  by Dan Mosedale via funding from the Interaction Institute IVREA.

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

import processing.core.*;

import java.io.*;

import antlr.*;
import antlr.collections.*;
import antlr.collections.impl.*;

import com.oroinc.text.regex.*;


public class PdePreprocessor {

  static final int JDK11 = 0;
  static final int JDK13 = 1;
  static final int JDK14 = 2;

  static String defaultImports[][] = new String[3][];
  String extraImports[];

  static final int STATIC = 0;  // formerly BEGINNER
  static final int ACTIVE = 1;  // formerly INTERMEDIATE
  static final int JAVA   = 2;  // formerly ADVANCED
  // static to make it easier for the antlr preproc to get at it
  static int programType = -1;  

  Reader programReader;
  String buildPath;

  // used for calling the ASTFactory to get the root node
  private static final int ROOT_ID = 0;


  /**
   * These may change in-between (if the prefs panel adds this option)
   * so grab them here on construction.
   */
  public PdePreprocessor() { 
    defaultImports[JDK11] = 
      PApplet.split(PdePreferences.get("preproc.imports.jdk11"), ',');
    defaultImports[JDK13] = 
      PApplet.split(PdePreferences.get("preproc.imports.jdk13"), ',');
    defaultImports[JDK14] = 
      PApplet.split(PdePreferences.get("preproc.imports.jdk14"), ',');
  }


  /**
   * Used by PdeEmitter.dumpHiddenTokens()
   */
  public static TokenStreamCopyingHiddenTokenFilter filter;


  /**
   * preprocesses a pde file and write out a java file
   * @return the classname of the exported Java
   */
  //public String write(String program, String buildPath, String name, 
  //                  String extraImports[]) throws java.lang.Exception {
  public String write(String program, String buildPath, String name) 
    throws java.lang.Exception {
    // if the program ends with no CR or LF an OutOfMemoryError will happen.
    // not gonna track down the bug now, so here's a hack for it:
    if ((program.length() > 0) &&
        program.charAt(program.length()-1) != '\n') {
      program += "\n";
    }

    if (PdePreferences.getBoolean("preproc.substitute_unicode")) {
      // check for non-ascii chars (these will be/must be in unicode format)
      char p[] = program.toCharArray();
      int unicodeCount = 0;
      for (int i = 0; i < p.length; i++) {
        if (p[i] > 127) unicodeCount++;
      }
      // if non-ascii chars are in there, convert to unicode escapes
      if (unicodeCount != 0) {
        // add unicodeCount * 5.. replacing each unicode char 
        // with six digit uXXXX sequence (xxxx is in hex)
        // (except for nbsp chars which will be a replaced with a space)
        int index = 0;
        char p2[] = new char[p.length + unicodeCount*5];
        for (int i = 0; i < p.length; i++) {
          if (p[i] < 128) {
            p2[index++] = p[i];

          } else if (p[i] == 160) {  // unicode for non-breaking space
            p2[index++] = ' ';

          } else {
            int c = p[i];
            p2[index++] = '\\';
            p2[index++] = 'u';
            char str[] = Integer.toHexString(c).toCharArray();
            // add leading zeros, so that the length is 4
            //for (int i = 0; i < 4 - str.length; i++) p2[index++] = '0';
            for (int m = 0; m < 4 - str.length; m++) p2[index++] = '0';
            System.arraycopy(str, 0, p2, index, str.length);
            index += str.length;
          }
        }
        program = new String(p2, 0, index);
      }
    }

    // if this guy has his own imports, need to remove them 
    // just in case it's not an advanced mode sketch
    PatternMatcher matcher = new Perl5Matcher();
    PatternCompiler compiler = new Perl5Compiler();
    //String mess = "^\\s*(import\\s*[\\w\\d_\\.]+\\s*\\;)";
    //String mess = "^\\s*(import\\s*[\\w\\d\\_\\.]+\\s*\\;)";
    String mess = "^\\s*(import\\s+\\S+\\s*;)";
    java.util.Vector imports = new java.util.Vector();

    Pattern pattern = null;
    try {
      pattern = compiler.compile(mess);
    } catch (MalformedPatternException e) {
      e.printStackTrace();
      return null;
    }

    do {
      PatternMatcherInput input = new PatternMatcherInput(program);
      if (!matcher.contains(input, pattern)) break;

      MatchResult result = matcher.getMatch();
      String piece = result.group(1).toString();
      int len = piece.length();

      imports.add(piece);
      int idx = program.indexOf(piece);
      // just remove altogether?
      program = program.substring(0, idx) + program.substring(idx + len);

      //System.out.println("removing " + piece);

    } while (true);

    //if (imports.size() > 0) {
    extraImports = new String[imports.size()];
    imports.copyInto(extraImports);
    //} 

    //

    // do this after the program gets re-combobulated
    this.programReader = new StringReader(program);
    this.buildPath = buildPath;

    // create a lexer with the stream reader, and tell it to handle 
    // hidden tokens (eg whitespace, comments) since we want to pass these
    // through so that the line numbers when the compiler reports errors
    // match those that will be highlighted in the PDE IDE
    // 
    PdeLexer lexer  = new PdeLexer(programReader);
    lexer.setTokenObjectClass("antlr.CommonHiddenStreamToken");

    // create the filter for hidden tokens and specify which tokens to 
    // hide and which to copy to the hidden text
    //
    filter = new TokenStreamCopyingHiddenTokenFilter(lexer);
    filter.hide(PdeRecognizer.SL_COMMENT);
    filter.hide(PdeRecognizer.ML_COMMENT);
    filter.hide(PdeRecognizer.WS);
    filter.copy(PdeRecognizer.SEMI);
    filter.copy(PdeRecognizer.LPAREN);
    filter.copy(PdeRecognizer.RPAREN);
    filter.copy(PdeRecognizer.LCURLY);
    filter.copy(PdeRecognizer.RCURLY);
    filter.copy(PdeRecognizer.COMMA);
    filter.copy(PdeRecognizer.RBRACK);
    filter.copy(PdeRecognizer.LBRACK);
    filter.copy(PdeRecognizer.COLON);

    // create a parser and set what sort of AST should be generated
    //
    PdeRecognizer parser = new PdeRecognizer(filter);

    // use our extended AST class
    //
    parser.setASTNodeClass("antlr.ExtendedCommonASTWithHiddenTokens");

    // start parsing at the compilationUnit non-terminal
    //
    parser.pdeProgram();

    // set up the AST for traversal by PdeEmitter
    //
    ASTFactory factory = new ASTFactory();
    AST parserAST = parser.getAST();
    AST rootNode = factory.create(ROOT_ID, "AST ROOT");
    rootNode.setFirstChild(parserAST);

    // unclear if this actually works, but it's worth a shot
    //
    ((CommonAST)parserAST).setVerboseStringConversion(
      true, parser.getTokenNames());

    // if this is an advanced program, the classname is already defined.
    //
    if (programType == JAVA) {
      name = getFirstClassName(parserAST);
    }

    // if 'null' was passed in for the name, but this isn't 
    // a 'java' mode class, then there's a problem, so punt.
    // 
    if (name == null) return null;

    // output the code
    //
    PdeEmitter emitter = new PdeEmitter();
    File streamFile = new File(buildPath, name + ".java");
    PrintStream stream = new PrintStream(new FileOutputStream(streamFile));

    writeHeader(stream, extraImports, name);

    emitter.setOut(stream);
    emitter.print(rootNode);

    writeFooter(stream);
    stream.close();

    // if desired, serialize the parse tree to an XML file.  can
    // be viewed usefully with Mozilla or IE

    if (PdePreferences.getBoolean("preproc.output_parse_tree")) {

      stream = new PrintStream(new FileOutputStream("parseTree.xml"));
      stream.println("<?xml version=\"1.0\"?>");
      stream.println("<document>");
      OutputStreamWriter writer = new OutputStreamWriter(stream);
      if (parserAST != null) {
        ((CommonAST)parserAST).xmlSerialize(writer);
      }
      writer.flush();
      stream.println("</document>");
      writer.close();
    }

    return name;
  }


  /**
   * Write any required header material (eg imports, class decl stuff)
   * 
   * @param out                 PrintStream to write it to.
   * @param exporting           Is this being exported from PDE?
   * @param name                Name of the class being created.
   */
  void writeHeader(PrintStream out, String imports[], 
                   String className) {

    // must include processing.core
    out.print("import processing.core.*; ");

    // emit emports that are needed for classes from the code folder
    /*
    if (imports != null) {
      for (int i = 0; i < imports.length; i++) {
        out.print("import " + imports[i] + ".*; ");
      }      
    }
    */
    if (imports != null) {
      for (int i = 0; i < imports.length; i++) {
        out.print(imports[i]);
      }      
    }

    // emit standard imports (read from pde.properties)
    // for each language level that's being used.
    String jdkVersionStr = PdePreferences.get("preproc.jdk_version");

    int jdkVersion = JDK11;  // default
    if (jdkVersionStr.equals("1.3")) { jdkVersion = JDK13; };
    if (jdkVersionStr.equals("1.4")) { jdkVersion = JDK14; };

    for (int i = 0; i <= jdkVersion; i++) {
      for (int j = 0; j < defaultImports[i].length; j++) {
        out.print("import " + defaultImports[i][j] + ".*; ");        
      }
    }

    if (programType < JAVA) {
      // open the class definition
      out.print("public class " + className + " extends PApplet {");

      if (programType == STATIC) {
        // now that size() and background() can go inside of draw()
        // actually, use setup(), because when running externally
        // the applet size needs to be set before the window is drawn,
        // meaning that after setup() things need to be ducky.
        //out.print("public void draw() {");
        out.print("public void setup() {");
      }
    }
  }

  /**
   * Write any necessary closing text.
   * 
   * @param out         PrintStream to write it to.
   */
  void writeFooter(PrintStream out) {

    if (programType == STATIC) {
      // close off draw() definition
      out.print("}");
    }

    if (programType < JAVA) {
      // close off the class definition
      out.print("}");
    }
  }


  static String advClassName = "";

  /**
   * Find the first CLASS_DEF node in the tree, and return the name of the
   * class in question.
   *
   * XXXdmose right now, we're using a little hack to the grammar to get
   * this info.  In fact, we should be descending the AST passed in.
   */
  String getFirstClassName(AST ast) {

    String t = advClassName;
    advClassName = "";

    return t;
  }

}
