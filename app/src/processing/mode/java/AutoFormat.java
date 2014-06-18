/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Original Copyright (c) 1997, 1998 Van Di-Han HO. All Rights Reserved.
  Updates Copyright (c) 2001 Jason Pell.
  Further updates Copyright (c) 2003 Martin Gomez, Ateneo de Manila University
  Additional updates Copyright (c) 2005-10 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, version 2.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.mode.java;

import java.util.Stack;
import java.util.regex.Pattern;

import processing.app.Formatter;
import processing.app.Preferences;
import processing.core.PApplet;

/**
 * Handler for dealing with auto format.
 * Contributed by Martin Gomez, additional bug fixes by Ben Fry.
 * Additional fixes by Jonathan Feinberg in March 2010.
 * <p/>
 * After some further digging, this code in fact appears to be a modified
 * version of Jason Pell's GPLed "Java Beautifier" class found
 * <a href="http://web.archive.org/web/20091018220353/http://geocities.com/jasonpell/programs.html">here</a>,
 * which is itself based on code from Van Di-Han Ho from
 * <a href="http://web.archive.org/web/20091027043013/http://www.geocities.com/~starkville/vancbj_idx.html">here</a>.
 * [Ben Fry, August 2009]
 */
public class AutoFormat implements Formatter {
  private char[] chars;
  private final StringBuilder buf = new StringBuilder();
  private final StringBuilder result = new StringBuilder();

  /** The number of spaces in one indent. Constant. */
  private int indentValue;

  /** Set when the end of the chars array is reached. */
  private boolean EOF;

  private boolean inStatementFlag; // in a line of code
  private boolean overflowFlag;   // line overrunning?
  private boolean arrayFlag;     // in an array/after it in same statement?
  private boolean if_flg;
  private boolean startFlag;    // No buf has yet been writen to this line. 
  private boolean elseFlag;

  /** Number of ? entered without exiting at : of a?b:c structures. */
  private int conditionalLevel;

  /** chars[pos] is where we're at. */
  private int pos;
  private int level;
  private int[][] sp_flg;
  private int[][] s_ind;
  private int if_lev;

  /** Number of curly brackets entered and not exited,
      excluding arrays. */
  private int curlyLvl;

  /** Number of parentheses entered and not exited. */
  private int parenLevel;

  private int[] ind;
  private int[] p_flg;
  private int[][] s_tabs;
  private boolean jdoc_flag;

  /** The number of times to indent at a given point */
  private int tabs;

  /** The last non-space seen by nextChar(). */
  private char lastNonWhitespace = 0;

  private final Stack<Boolean> castFlags = new Stack<Boolean>();


  private void handleMultiLineComment() {
    final boolean saved_startFlag = startFlag;
    buf.append(nextChar()); // So /*/ isn't self-closing.

    for (char ch = nextChar(); !EOF; ch = nextChar()) {
      buf.append(ch);
      while (ch != '/' && !EOF) {
        if (ch == '\n') {
          writeIndentedComment();
          startFlag = true;
        }
        buf.append(ch = nextChar());
      }
      if (buf.length() >= 2 && buf.charAt(buf.length() - 2) == '*') {
        jdoc_flag = false;
        break;
      }
    }

    writeIndentedComment();
    startFlag = saved_startFlag;
    jdoc_flag = false;
    return;
  }


  /**
   * Pumps nextChar into buf until \n or EOF, then calls
   * writeIndentedLine() and sets startFlag to true.
   */
  private void handleSingleLineComment() {
    char ch = nextChar();
    while (ch != '\n' && !EOF) {
      buf.append(ch);
      ch = nextChar();
    }
    writeIndentedLine();
    startFlag = true;
  }


  private void writeIndentedLine() {
    System.out.print(buf);
    if (buf.length() == 0) {
      if (startFlag) startFlag = elseFlag = false;
      return;
    }
    if (startFlag) {
      // Indent suppressed at eg. if<nl>{ and when  
      // buf is close-brackets only followed by ';'.
      boolean indentMore = !buf.toString().matches("[\\s\\]\\}\\)]*;") 
        && (arrayFlag || buf.charAt(0) != '{')
        && overflowFlag;
      System.out.println(indentMore + ", " + overflowFlag);
      if (indentMore) tabs++;
      printIndentation();
      startFlag = false;
      if (indentMore) tabs--;
    }
    if (elseFlag) {
      if (lastNonSpaceChar() == '}') {
        trimRight(result);
        result.append(' ');
      }
      elseFlag = false;
    }
    result.append(buf);
    buf.setLength(0);
  }


  /**
   * @return the last character in <tt>result</tt> not ' ' or '\n'.
   */
  private char lastNonSpaceChar() {
    for (int i = result.length() - 1; i >= 0; i--) {
      char chI = result.charAt(i);
      if (chI != ' ' && chI != '\n') return chI;
    }
    return 0;
  }


  /**
   * Called by handleMultilineComment.<br />
   * Sets jdoc_flag if at the start of a doc comment.
   * Sends buf to result with proper indents, then clears buf.<br />
   * Does nothing if buf is empty.
   */
  private void writeIndentedComment() {
    if (buf.length() == 0) return;

    int firstNonSpace = 0;
    while (buf.charAt(firstNonSpace) == ' ') firstNonSpace++;
    if (lookup_com("/**")) jdoc_flag = true;

    if (startFlag) printIndentation();

    if (buf.charAt(firstNonSpace) == '/' && buf.charAt(firstNonSpace+1) == '*') {
      if (startFlag && lastNonWhitespace != ';') {
        result.append(buf.substring(firstNonSpace));
      } else { 
        result.append(buf);
      }
    } else {
      if (buf.charAt(firstNonSpace) == '*' || !jdoc_flag) {
        result.append(" " + buf.substring(firstNonSpace));
      } else {
        result.append(" * " + buf.substring(firstNonSpace));
      }
    }
    buf.setLength(0);
  }


  /**
   * Makes tabs >= 0 and appends <tt>tabs*indentValue</tt>
   * spaces to result.
   */
  private void printIndentation() {
    if (tabs <= 0) {
      tabs = 0;
      return;
    }
    final int spaces = tabs * indentValue;
    for (int i = 0; i < spaces; i++) {
      result.append(' ');
    }
  }


  /**
   * @return <tt>chars[pos+1]</tt> or '\0' if out-of-bounds.
   */
  private char peek() {
    return (pos + 1 >= chars.length) ? 0 : chars[pos + 1];
  }


  /**
   * Sets pos to the position of the next character that is not ' '
   * in chars. If chars[pos] != ' ' already, it will still move on.
   * Then sets EOF if pos has reached the end, or reverses pos by 1 if it
   * has not.
   * <br/> Does nothing if EOF.
   */
  private void advanceToNonSpace() {
    if (EOF) return;

    do {
      pos++;
    } while (pos < chars.length && chars[pos] == ' ');

    if (pos == chars.length - 1) EOF = true;
    else pos--; // reset for nextChar()
  }


  /**
   * Increments pos, sets EOF if needed, and returns the new
   * chars[pos] or zero if out-of-bounds.
   * Sets lastNonWhitespace if chars[pos] isn't whitespace.
   * Does nothing and returns zero if EOF was set when it was called.
   */
  private char nextChar() {
    if (EOF) return 0;
    pos++;
    if (pos == chars.length-1) EOF = true;
    if (pos >= chars.length) return 0;

    char retVal = chars[pos];
    if (!Character.isWhitespace(retVal)) lastNonWhitespace = retVal;
    return retVal;
  }


  private void gotElse() {
    tabs = s_tabs[curlyLvl][if_lev];
    p_flg[level] = sp_flg[curlyLvl][if_lev];
    ind[level] = s_ind[curlyLvl][if_lev];
    if_flg = true;
  }


  private boolean readForNewLine() {
    final int savedTabs = tabs;
    char c = peek();
    while (!EOF && (c == '\t' || c == ' ')) {
      buf.append(nextChar());
      c = peek();
    }

    if (c == '/') {
      buf.append(nextChar());
      c = peek();
      if (c == '*') {
        buf.append(nextChar());
        handleMultiLineComment();
      } else if (c == '/') {
        buf.append(nextChar());
        handleSingleLineComment();
        return true;
      }
    }

    c = peek();
    if (c == '\n') {
      // eat it
      nextChar();
      tabs = savedTabs;
      return true;
    }
    return false;
  }

  /**
   * Called at '\n' in the main loop
   * @returns whether the statement (or whatever) had ended.
   */
  private boolean isOverflowing() {
    if (lastNonWhitespace == 0 || lastNonWhitespace == ';') {
       return false; //start of file or end of statement
    }
    if (lastNonWhitespace != '{' && lastNonWhitespace != '}') {
      return true; // evidently in a statement;
    }
    return arrayFlag; // there was an array, {} aren't blocks.
    // If {} were blocks for an odd, valid reason... this will
    // fail, but I reckon that anyone putting blocks in arrays
    // is in a small minority.
  }


  /**
   * @returns last non-wsp in result+buf, or 0 on error.
   */
  private char prevNonWhitespace() {
    StringBuffer tot = new StringBuffer();
    tot.append(result);
    tot.append(buf);
    for (int i = tot.length()-1; i >= 0; i--) {
      if (!Character.isWhitespace(tot.charAt(i)))
        return tot.charAt(i);
    }
    return 0;
  }
  

  /**
   * Sees if buf is of the form [optional whitespace][keyword][optional anything].
   * It won't allow keyword to be directly followed by an alphanumeric, _, or &.
   * Will be different if keyword contains regex codes.
   */
  private boolean lookup(final String keyword) {
    return Pattern.matches("^\\s*" + keyword + "(?![a-zA-Z0-9_&]).*$", buf);
  }


  /**
   * Sees if buf is of the form [optional whitespace][keyword][optional anything].
   * It *will* allow keyword to be directly followed by an alphanumeric, _, or &.
   * Will be different if keyword contains regex codes (except *, which is fine).
   */
  private boolean lookup_com(final String keyword) {
    final String regex = "^\\s*" + keyword.replace("*", "\\*") + ".*$";
    return Pattern.matches(regex, buf);
  }


  /**
   * Takes all whitespace off the end of its argument.
   */
  private void trimRight(final StringBuilder sb) {
    while (sb.length() >= 1 && Character.isWhitespace(sb.charAt(sb.length() - 1)))
      sb.setLength(sb.length() - 1);
  }


  /**
   * Entry point
   */
  public String format(final String source) {
    final String normalizedText = source.replaceAll("\r", "");
    final String cleanText =
      normalizedText + (normalizedText.endsWith("\n") ? "" : "\n");

    result.setLength(0);
    indentValue = Preferences.getInteger("editor.tabs.size");

    boolean forFlag = if_flg = false;
    startFlag = true;
    int forParenthLevel = 0;
    conditionalLevel = parenLevel = curlyLvl = if_lev = level = 0;
    tabs = 0;
    jdoc_flag = overflowFlag = inStatementFlag = false;
    char cc;

    int[] s_level = new int[10];
    sp_flg = new int[20][10];
    s_ind = new int[20][10];
    int[] s_if_lev = new int[10];
    boolean[] s_if_flg = new boolean[10];
    ind = new int[10];
    p_flg = new int[10];
    s_tabs = new int[20][10];
    pos = -1;
    chars = cleanText.toCharArray();

    EOF = false; // set in nextChar() when EOF

    while (!EOF) {
      char c = nextChar();
      switch (c) {
      default:
        inStatementFlag = true;
        buf.append(c);
        break;

      case ',':
        inStatementFlag = true;
        trimRight(buf);
        buf.append(", ");
        advanceToNonSpace();
        break;

      case ' ':
      case '\t':
        elseFlag = lookup("else");
        if (elseFlag) {
          gotElse();
          if (!startFlag || buf.length() > 0) buf.append(c);
          writeIndentedLine();
          startFlag = false;
          break;
        }
        if (!startFlag || buf.length() > 0) buf.append(c);
        break;

      case '\n':
        if (EOF) break;
        
        elseFlag = lookup("else");
        if (elseFlag) gotElse();

        if (lookup_com("//")) {
          if (buf.charAt(buf.length() - 1) == '\n') {
            buf.setLength(buf.length() - 1);
          }
        }

        writeIndentedLine();
        result.append("\n");
        startFlag = true;
        if (elseFlag) {
          p_flg[level]++;
          tabs++;
        } else overflowFlag = inStatementFlag;
        break;

      case '{':
        char prevChar = prevNonWhitespace();
        if (prevChar == '=' || prevChar == ']') arrayFlag = true;
        if (arrayFlag) {
          buf.append(c);
          break; // No special treatment. TODO: indents.
        } else inStatementFlag = false; // eg class declaration ends

        elseFlag = lookup("else");
        if (elseFlag) gotElse();
        if (s_if_lev.length == curlyLvl) {
          s_if_lev = PApplet.expand(s_if_lev);
          s_if_flg = PApplet.expand(s_if_flg);
        }

        s_if_lev[curlyLvl] = if_lev;
        s_if_flg[curlyLvl] = if_flg;
        if_lev = 0;
        if_flg = false;
        curlyLvl++;
        if (startFlag && p_flg[level] != 0) {
          p_flg[level]--;
          tabs--;
        }
        trimRight(buf);
        if (buf.length() > 0 || (result.length() > 0 &&
              !Character.isWhitespace(result.charAt(result.length() - 1)))) {
          buf.append(" ");
        }
        buf.append(c);
        writeIndentedLine();
        readForNewLine();
        writeIndentedLine();

        result.append("\n");
        tabs++;
        startFlag = true;
        if (p_flg[level] > 0) {
          ind[level] = 1;
          level++;
          s_level[level] = curlyLvl;
        }
        break;

      case '}':
        if (arrayFlag) {
          buf.append(c);
          break;
        } else inStatementFlag = false;

        curlyLvl--;
        if (curlyLvl < 0) {
          curlyLvl = 0;
          buf.append(c);
          writeIndentedLine();
        } else {
          if_lev = s_if_lev[curlyLvl] - 1;
          if (if_lev < 0) {
            if_lev = 0;
          }
          if_flg = s_if_flg[curlyLvl];
          trimRight(buf);
          writeIndentedLine();
          tabs--;

          trimRight(result);
          result.append("\n");
          printIndentation();
          result.append(c);
          if (peek() == ';' || arrayFlag) {
            //Now does commas etc.
            if (peek() == ' ') advanceToNonSpace();
            result.append(nextChar());
          }

          readForNewLine();
          writeIndentedLine();
          result.append('\n');
          startFlag = true;
          if (curlyLvl < s_level[level] && level > 0) {
            level--;
          }
          if (ind[level] != 0) {
            tabs -= p_flg[level];
            p_flg[level] = 0;
            ind[level] = 0;
          }
        }
        break;

      case '"':
      case '\'':
        inStatementFlag = true;
        buf.append(c);
        cc = nextChar();
        while (!EOF && cc != c) {
          buf.append(cc);
          if (cc == '\\') {
            buf.append(cc = nextChar());
          }
          if (cc == '\n') {
            writeIndentedLine();
            startFlag = true;
          }
          cc = nextChar();
        }
        buf.append(cc);
        if (readForNewLine()) {
          // push a newline into the stream
          chars[pos--] = '\n';
        }
        break;

      case ';':
        if (forFlag) {
          // This is like a comma.
          trimRight(buf);
          buf.append("; ");
          // Not non-whitespace: allow \n.
          advanceToNonSpace();
          break;
        }
        buf.append(c);
        writeIndentedLine();
        if (p_flg[level] > 0 && ind[level] == 0) {
          tabs -= p_flg[level];
          p_flg[level] = 0;
        }
        readForNewLine();
        writeIndentedLine();
        result.append("\n");
        startFlag = true;
        if (if_lev > 0) {
          if (if_flg) {
            if_lev--;
            if_flg = false;
          } else {
            if_lev = 0;
          }
        }
        // Array behaviour ends at the end
        // of a statement.
        arrayFlag = false;
        inStatementFlag = false;
        break;

      case '\\':
        buf.append(c);
        buf.append(nextChar());
        break;

      case '?':
        conditionalLevel++;
        buf.append(c);
        break;

      case ':':
        // Java 8 :: operator.
        if (peek() == ':') {
          result.append(c).append(nextChar());
          break;
        }

        // End a?b:c structures.
        else if (conditionalLevel > 0) {
          conditionalLevel--;
          buf.append(c);
          break;
        }

        else if (forFlag) {
          trimRight(buf);
          buf.append(" : ");
          // Not to non-whitespace: allow \n.
          advanceToNonSpace();
          break;
        }

        buf.append(c);
        inStatementFlag = false;

        if (!lookup("default") && !lookup("case")) {
          startFlag = false;
          writeIndentedLine();
        } else {
          tabs--;
          writeIndentedLine();
          tabs++;
        }
        if (peek() == ';') {
          result.append(nextChar());
        }
        readForNewLine();
        writeIndentedLine();
        result.append('\n');
        startFlag = true;
        break;

      case '/':
        final char next = peek();
        if (next == '/') {
          // call nextChar to move on.
          buf.append(c).append(nextChar());
          handleSingleLineComment();
          result.append("\n");
        } else if (next == '*') {
          if (buf.length() > 0) {
            writeIndentedLine();
          }
          buf.append(c).append(nextChar());
          handleMultiLineComment();
        } else {
          buf.append(c);
        }
        break;

      case ')':
        final boolean isCast = castFlags.isEmpty() ? false : castFlags.pop();
        parenLevel--;

        // If we're further back than the start of a for loop, we've
        // left it.
        if (forFlag && forParenthLevel > parenLevel) forFlag = false;

        if (parenLevel < 0) parenLevel = 0;

        buf.append(c);
        writeIndentedLine();
        if (readForNewLine()) {
          chars[pos--] = '\n';
          if (parenLevel == 0 && tabs > 0 && !isCast) {
            p_flg[level]++;
            tabs++;
            ind[level] = 0;
          }
        }
        break;

      case '(':
        castFlags.push(Pattern.matches("^.*?(?:int|color|float)\\s*$", buf));

        final boolean isFor = lookup("for");
        final boolean isIf = lookup("if");

        if (isFor || isIf || lookup("while")) {
          if (!Character.isWhitespace(buf.charAt(buf.length() - 1))) {
            buf.append(' ');
          }
        }

        buf.append(c);
        parenLevel++;

        // isFor says "Is it the start of a for?". If it is, we set forFlag and
        // forParenthLevel. If it is not parenth_lvl was incremented above and
        // that's it.
        if (isFor && !forFlag) {
          forParenthLevel = parenLevel;
          forFlag = true;
        } else if (isIf) {
          writeIndentedLine();
          s_tabs[curlyLvl][if_lev] = tabs;
          sp_flg[curlyLvl][if_lev] = p_flg[level];
          s_ind[curlyLvl][if_lev] = ind[level];
          if_lev++;
          if_flg = true;
        }
      } // end switch
    } // end while not EOF

    if (buf.length() > 0) writeIndentedLine();

    final String formatted = result.toString();
    return formatted.equals(cleanText) ? source : formatted;
  }
}
