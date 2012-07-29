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
 * <a href="http://www.geocities.com/jasonpell/programs.html">here</a>.
 * Which is itself based on code from Van Di-Han Ho from
 * <a href="http://www.geocities.com/~starkville/vancbj_idx.html">here</a>.
 * [Ben Fry, August 2009]
 */
public class AutoFormat implements Formatter {
  private char[] chars;
  private final StringBuilder buf = new StringBuilder();

  private final StringBuilder result = new StringBuilder();

  private int indentValue;
  private boolean EOF;
  private boolean a_flg, e_flg, if_flg, s_flag, q_flg;
  private boolean s_if_flg[];
  private int pos;
//  private int lineNumber;
  private int s_level[];
  private int c_level;
  private int sp_flg[][];
  private int s_ind[][];
  private int s_if_lev[];
  private int if_lev, level;
  private int ind[];
  private int paren;
  private int p_flg[];
  private char l_char;
  private int ct;
  private int s_tabs[][];
  private boolean jdoc_flag;
  private char cc;
  private int tabs;
  private char c;

  private final Stack<Boolean> castFlags = new Stack<Boolean>();


  private void comment() {
    final boolean save_s_flg = s_flag;

    buf.append(c = next()); // extra char
    while (true) {
      buf.append(c = next());
      while ((c != '/')) {
        if (c == '\n') {
//          lineNumber++;
          writeIndentedComment();
          s_flag = true;
        }
        buf.append(c = next());
      }
      if (buf.length() >= 2 && buf.charAt(buf.length() - 2) == '*') {
        jdoc_flag = false;
        break;
      }
    }

    writeIndentedComment();
    s_flag = save_s_flg;
    jdoc_flag = false;
    return;
  }


  private char get_string() {
    char ch;
    while (true) {
      buf.append(ch = next());
      if (ch == '\\') {
        buf.append(next());
        continue;
      }
      if (ch == '\'' || ch == '"') {
        buf.append(cc = next());
        while (!EOF && cc != ch) {
          if (cc == '\\') {
            buf.append(next());
          }
          buf.append(cc = next());
        }
        continue;
      }
      if (ch == '\n') {
        writeIndentedLine();
        a_flg = true;
        continue;
      }
      return ch;
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
        if (saved_s_flag && prev() != ';') {
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


  private void handleSingleLineComment() {
    c = next();
    while (c != '\n') {
      buf.append(c);
      c = next();
    }
//    lineNumber++;
    writeIndentedLine();
    s_flag = true;
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
      result.append(" ");
    }
  }


  private char peek() {
    if (pos + 1 >= chars.length) {
      return 0;
    }
    return chars[pos + 1];
  }


  private char lastNonWhitespace = 0;


  private int prev() {
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
      pos--; // reset for next()
    }
  }


  private char next() {
    if (EOF) {
      return 0;
    }
    pos++;
    final char c;
    if (pos < chars.length) {
      c = chars[pos];
      if (!Character.isWhitespace(c))
        lastNonWhitespace = c;
    } else {
      c = 0;
    }
    if (pos == chars.length - 1) {
      EOF = true;
    }
    return c;
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
      buf.append(next());
      c = peek();
    }

    if (c == '/') {
      buf.append(next());
      c = peek();
      if (c == '*') {
        buf.append(next());
        comment();
      } else if (c == '/') {
        buf.append(next());
        handleSingleLineComment();
        return true;
      }
    }

    c = peek();
    if (c == '\n') {
      // eat it
      next();
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

//    lineNumber = 0;
    q_flg = e_flg = a_flg = if_flg = false;
    s_flag = true;
    c_level = if_lev = level = paren = 0;
    tabs = 0;
    jdoc_flag = false;

    s_level = new int[10];
    sp_flg = new int[20][10];
    s_ind = new int[20][10];
    s_if_lev = new int[10];
    s_if_flg = new boolean[10];
    ind = new int[10];
    p_flg = new int[10];
    s_tabs = new int[20][10];
    pos = -1;
    chars = cleanText.toCharArray();
//    lineNumber = 1;

    EOF = false; // set in next() when EOF

    while (!EOF) {
      c = next();
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
        e_flg = lookup("else");
        if (e_flg) {
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
        if (e_flg) {
          p_flg[level]++;
          tabs++;
        } else if (prev() == l_char) {
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
        //fprintf(outfil,"\n");
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
            result.append(next());
          }
          getnl();
          writeIndentedLine();
          result.append("\n");
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
        cc = next();
        while (!EOF && cc != c) {
          buf.append(cc);
          if (cc == '\\') {
            buf.append(cc = next());
          }
          if (cc == '\n') {
//            lineNumber++;
            writeIndentedLine();
            s_flag = true;
          }
          cc = next();
        }
        buf.append(cc);
        if (getnl()) {
          l_char = cc;
          // push a newline into the stream
          chars[pos--] = '\n';
        }
        break;

      case ';':
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
        buf.append(next());
        break;

      case '?':
        q_flg = true;
        buf.append(c);
        break;

      case ':':
        buf.append(c);
        if (peek() == ':') {
          writeIndentedLine();
          result.append(next());
          break;
        }

        if (q_flg) {
          q_flg = false;
          break;
        }
        if (!lookup("default") && !lookup("case")) {
          s_flag = false;
          writeIndentedLine();
        } else {
          tabs--;
          writeIndentedLine();
          tabs++;
        }
        if (peek() == ';') {
          result.append(next());
        }
        getnl();
        writeIndentedLine();
        result.append("\n");
        s_flag = true;
        break;

      case '/':
        final char la = peek();
        if (la == '/') {
          buf.append(c).append(next());
          handleSingleLineComment();
          result.append("\n");
        } else if (la == '*') {
          if (buf.length() > 0) {
            writeIndentedLine();
          }
          buf.append(c).append(next());
          comment();
        } else {
          buf.append(c);
        }
        break;

      case ')':

        final boolean isCast = castFlags.isEmpty() ? false : castFlags.pop();

        paren--;
        if (paren < 0) {
          paren = 0;
        }
        buf.append(c);
        writeIndentedLine();
        if (getnl()) {
          chars[pos--] = '\n';
          if (paren != 0) {
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
        paren++;

        if (isFor) {
          // TODO(feinberg): handle new-style for loops
          c = get_string();
          while (c != ';' && c != ':') {
            c = get_string();
          }
          ct = 0;
          int for_done = 0;
          while (for_done == 0) {
            c = get_string();
            while (c != ')') {
              if (c == '(') {
                ct++;
              }
              c = get_string();
            }
            if (ct != 0) {
              ct--;
            } else {
              for_done = 1;
            }
          } // endwhile for_done
          paren--;
          if (paren < 0) {
            paren = 0;
          }
          writeIndentedLine();
          if (getnl()) {
            chars[pos--] = '\n';
            p_flg[level]++;
            tabs++;
            ind[level] = 0;
          }
          break;
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
