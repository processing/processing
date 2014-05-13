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

  private int indentValue;
  private boolean EOF;
  private boolean a_flg, if_flg, s_flag;
  
  /* Number of ? entered without exiting at : of a?b:c structures. */
  private int questClnLvl;
  private int pos;
//  private int lineNumber;
  private int c_level;
  private int[][] sp_flg;
  private int[][] s_ind;
  private int if_lev;

  /** Number of curly brackets entered and not exited. */
  private int level;

  /** Number of paraentheses entered and not exited. */
  private int parenth_lvl;
  private int[] ind;
  private int[] p_flg;
  private char l_char;
  private int[][] s_tabs;
  private boolean jdoc_flag;
  private char cc;
  private int tabs;
  private char c;
  private char lastNonWhitespace = 0;

  private final Stack<Boolean> castFlags = new Stack<Boolean>();


  private void handleMultiLineComment() {
    final boolean saved_s_flag = s_flag;

    char ch;
    buf.append(ch = nextChar()); // extra char
    while (true) {
      buf.append(ch = nextChar());
      while (ch != '/') {
        if (ch == '\n') {
//        lineNumber++;
          writeIndentedComment();
          s_flag = true;
        }
        buf.append(ch = nextChar());
      }
      if (buf.length() >= 2 && buf.charAt(buf.length() - 2) == '*') {
        jdoc_flag = false;
        break;
      }
    }

    writeIndentedComment();
    s_flag = saved_s_flag;
    jdoc_flag = false;
    return;
  }

  private void handleSingleLineComment() {
    char ch = nextChar();
    while (ch != '\n') {
      buf.append(ch);
      ch = nextChar();
    }
//  lineNumber++;
    writeIndentedLine();
    s_flag = true;
  }

  /**
   * Transfers buf to result until a character is reached that
   * is neither \, \n, or in a string. \n is not writted, but
   * writeIndentedLine is called on finding it.
   * @return The first character not listed above.
   */
  private char get_string() {
    char ch1, ch2;
    while (true) {
      buf.append(ch1 = nextChar());
      switch (ch1) {
      case '\\':
        buf.append(nextChar());
        break;
      
      case '\'':
      case '"':
        buf.append(ch2 = nextChar());
        while (!EOF && ch2 != ch1) {
          if (ch2 == '\\') {
            // Next char is escaped, ignore.
            buf.append(nextChar());
          }
          buf.append(ch2 = nextChar());
        }
        break;
      
      case '\n':
        writeIndentedLine();
        a_flg = true;
        break;

      default:
        return ch1;
      }
    }
  }


  private void writeIndentedLine() {
    if (buf.length() == 0) {
      if (s_flag) {
        s_flag = a_flg = false;
      }
      return;
    }
    if (s_flag) {
      final boolean shouldIndent = (tabs > 0) && (buf.charAt(0) != '{')
          && a_flg;
      if (shouldIndent) {
        tabs++;
      }
      printIndentation();
      s_flag = false;
      if (shouldIndent) {
        tabs--;
      }
      a_flg = false;
    }
    result.append(buf);
    buf.setLength(0);
  }


  private void writeIndentedComment() {
    final boolean saved_s_flag = s_flag;
    if (buf.length() > 0) {
      if (s_flag) {
        printIndentation();
        s_flag = false;
      }
      int i = 0;
      while (buf.charAt(i) == ' ') {
        i++;
      }
      if (lookup_com("/**")) {
        jdoc_flag = true;
      }
      if (buf.charAt(i) == '/' && buf.charAt(i + 1) == '*') {
        if (saved_s_flag && getLastNonWhitespace() != ';') {
          result.append(buf.substring(i));
        } else {
          result.append(buf);
        }
      } else {
        if (buf.charAt(i) == '*' || !jdoc_flag) {
          result.append((" " + buf.substring(i)));
        } else {
          result.append((" * " + buf.substring(i)));
        }
      }
      buf.setLength(0);
    }
  }




  private void printIndentation() {
    if (tabs < 0) {
      tabs = 0;
    }
    if (tabs == 0) {
      return;
    }
    final int spaces = tabs * indentValue;
    for (int k = 0; k < spaces; k++) {
      result.append(' ');
    }
  }


  private char peek() {
    if (pos + 1 >= chars.length) {
      return 0;
    }
    return chars[pos + 1];
  }


  private int getLastNonWhitespace() {
    return lastNonWhitespace;
  }


  private void advanceToNonSpace() {
    if (EOF) {
      return;
    }
    do {
      pos++;
    } while (pos < chars.length && chars[pos] == ' ');
    if (pos == chars.length - 1) {
      EOF = true;
    } else {
      pos--; // reset for nextChar()
    }
  }


  private char nextChar() {
    if (EOF) {
      return '\0';
    }
    pos++;
    char retVal;
    if (pos < chars.length) {
      retVal = chars[pos];
      if (!Character.isWhitespace(retVal))
        lastNonWhitespace = retVal;
    } else {
      retVal = '\0';
    }
    if (pos == chars.length-1) {
      EOF = true;
    }
    return retVal;
  }


  /* else processing */
  private void gotelse() {
    tabs = s_tabs[c_level][if_lev];
    p_flg[level] = sp_flg[c_level][if_lev];
    ind[level] = s_ind[c_level][if_lev];
    if_flg = true;
  }


  /* read to new_line */
  private boolean getnl() {
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
//      lineNumber++;
      tabs = savedTabs;
      return true;
    }
    return false;
  }


  private boolean lookup(final String keyword) {
    return Pattern.matches("^\\s*" + keyword + "(?![a-zA-Z0-9_&]).*$", buf);
  }


  private boolean lookup_com(final String keyword) {
    final String regex = "^\\s*" + keyword.replaceAll("\\*", "\\\\*") + ".*$";
    return Pattern.matches(regex, buf);
  }


  private void trimRight(final StringBuilder sb) {
    while (sb.length() >= 1
        && Character.isWhitespace(sb.charAt(sb.length() - 1)))
      sb.setLength(sb.length() - 1);
  }


  public String format(final String source) {
    final String normalizedText = source.replaceAll("\r", "");
    final String cleanText = normalizedText
        + (normalizedText.endsWith("\n") ? "" : "\n");

    result.setLength(0);
    indentValue = Preferences.getInteger("editor.tabs.size");

//  lineNumber = 0;
    boolean forFlag = a_flg = if_flg = false;
    s_flag = true;
    int forParenthLevel = 0;
    questClnLvl = parenth_lvl = c_level = if_lev = level = 0;
    tabs = 0;
    jdoc_flag = false;

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
//    lineNumber = 1;

    EOF = false; // set in nextChar() when EOF

    while (!EOF) {
      c = nextChar();
      switch (c) {
      default:
        buf.append(c);
        l_char = c;
        break;

      case ',':
        trimRight(buf);
        buf.append(c);
        buf.append(' ');
        advanceToNonSpace();
        break;

      case ' ':
      case '\t':
        if (lookup("else")) {
          gotelse();
          if ((!s_flag) || buf.length() > 0) {
            buf.append(c);
          }
//          // issue https://github.com/processing/processing/issues/364
//          s_flag = false;
//          trimRight(result);
//          result.append(" ");

          writeIndentedLine();
          s_flag = false;
          break;
        }
        if ((!s_flag) || buf.length() > 0) {
          buf.append(c);
        }
        break;

      case '\n':
//        lineNumber++;
        if (EOF) {
          break;
        }
        boolean elseFlag = lookup("else");
        if (elseFlag)  {
          gotelse();
        }
        if (lookup_com("//")) {
          final char lastChar = buf.charAt(buf.length() - 1);
          if (lastChar == '\n') {
            buf.setLength(buf.length() - 1);
          }
        }

        writeIndentedLine();
        result.append("\n");
        s_flag = true;
        if (elseFlag) {
          p_flg[level]++;
          tabs++;
        } else if (getLastNonWhitespace() == l_char) {
          a_flg = true;
        }
        break;

      case '{':
        if (lookup("else")) {
          gotelse();
        }
        if (s_if_lev.length == c_level) {
          s_if_lev = PApplet.expand(s_if_lev);
          s_if_flg = PApplet.expand(s_if_flg);
        }
        s_if_lev[c_level] = if_lev;
        s_if_flg[c_level] = if_flg;
        if_lev = 0;
        if_flg = false;
        c_level++;
        if (s_flag && p_flg[level] != 0) {
          p_flg[level]--;
          tabs--;
        }
        trimRight(buf);
        if (buf.length() > 0 ||
          (result.length() > 0 && !Character.isWhitespace(result.charAt(result.length() - 1))))
          buf.append(" ");
        buf.append(c);
        writeIndentedLine();
        getnl();
        writeIndentedLine();
        
        result.append("\n");
        tabs++;
        s_flag = true;
        if (p_flg[level] > 0) {
          ind[level] = 1;
          level++;
          s_level[level] = c_level;
        }
        break;

      case '}':
        c_level--;
        if (c_level < 0) {
          c_level = 0;
          buf.append(c);
          writeIndentedLine();

        } else {
          if_lev = s_if_lev[c_level] - 1;
          if (if_lev < 0) {
            if_lev = 0;
          }
          if_flg = s_if_flg[c_level];
          trimRight(buf);
          writeIndentedLine();
          tabs--;

          trimRight(result);
          result.append("\n");
          printIndentation();
          result.append(c);
          if (peek() == ';') {
            result.append(nextChar());
          }

          getnl();
          writeIndentedLine();
          result.append('\n');
          s_flag = true;
          if (c_level < s_level[level]) {
            if (level > 0) {
              level--;
            }
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
        buf.append(c);
        cc = nextChar();
        while (!EOF && cc != c) {
          buf.append(cc);
          if (cc == '\\') {
            buf.append(cc = nextChar());
          }
          if (cc == '\n') {
//            lineNumber++;
            writeIndentedLine();
            s_flag = true;
          }
          cc = nextChar();
        }
        buf.append(cc);
        if (getnl()) {
          l_char = cc;
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
        getnl();
        writeIndentedLine();
        result.append("\n");
        s_flag = true;
        if (if_lev > 0) {
          if (if_flg) {
            if_lev--;
            if_flg = false;
          } else {
            if_lev = 0;
          }
        }
        break;

      case '\\':
        buf.append(c);
        buf.append(nextChar());
        break;

      case '?':
        questClnLvl++;
        buf.append(c);
        break;

      case ':':
        // Java 8 :: operator. 
        if (peek() == ':') {
          writeIndentedLine();
          result.append(c).append(nextChar());
          break;
        }

        // End a?b:c structures.
        else if (questClnLvl>0) {
          questClnLvl--;
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

        if (!lookup("default") && !lookup("case")) {
          s_flag = false;
          writeIndentedLine();
        } else {
          tabs--;
          writeIndentedLine();
          tabs++;
        }
        if (peek() == ';') {
          result.append(nextChar());
        }
        getnl();
        writeIndentedLine();
        result.append('\n');
        s_flag = true;
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

        parenth_lvl--;
	
        // If we're further back than the start of a for loop, we've
        // left it.
        if (forFlag && forParenthLevel > parenth_lvl) {
          forFlag = false;
        }

        if (parenth_lvl < 0) {
          parenth_lvl = 0;
        }

        buf.append(c);
        writeIndentedLine();
        if (getnl()) {
          chars[pos--] = '\n';
          if (parenth_lvl != 0) {
            a_flg = true;
          } else if (tabs > 0 && !isCast) {
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
        parenth_lvl++;

        // isFor says "is it the start of a for?". If it is, we set forFlag and
        // forParenthLevel. If it is not parenth_lvl was incremented above and
        // that's it.
        if (isFor) {
	        if (!forFlag) {
            forParenthLevel = parenth_lvl;
            forFlag = true;
          }
        } else if (isIf) {
          writeIndentedLine();
          s_tabs[c_level][if_lev] = tabs;
          sp_flg[c_level][if_lev] = p_flg[level];
          s_ind[c_level][if_lev] = ind[level];
          if_lev++;
          if_flg = true;
        }
      } // end switch
    } // end while not EOF

    final String formatted = result.toString();
    return formatted.equals(cleanText) ? source : formatted;
  }
}
