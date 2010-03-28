/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Original Copyright (c) 1997, 1998 Van Di-Han HO. All Rights Reserved.
  Updates Copyright (c) 2001 Jason Pell.
  Further updates Copyright (c) 2003 Martin Gomez, Ateneo de Manila University
  Bug fixes Copyright (c) 2005-09 Ben Fry and Casey Reas

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

package processing.app.tools;

import java.io.IOException;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.regex.Pattern;
import processing.app.Editor;
import processing.app.Preferences;
import processing.core.PApplet;

/**
 * Handler for dealing with auto format.
 * Contributed by Martin Gomez, additional bug fixes by Ben Fry.
 * 
 * After some further digging, this code in fact appears to be a modified 
 * version of Jason Pell's GPLed "Java Beautifier" class found here:
 * http://www.geocities.com/jasonpell/programs.html
 * Which is itself based on code from Van Di-Han Ho:
 * http://www.geocities.com/~starkville/vancbj_idx.html
 * [Ben Fry, August 2009]
 */
public class AutoFormat implements Tool {
  Editor editor;

  private char[] chars;
  private final StringBuilder buf = new StringBuilder();

  final StringBuilder result = new StringBuilder();

  int indentValue;
  boolean EOF;
  boolean a_flg, e_flg, if_flg, s_flag, q_flg;
  boolean s_if_flg[];
  int pos, lineNumber;
  int s_level[];
  int c_level;
  int sp_flg[][];
  int s_ind[][];
  int s_if_lev[];
  int if_lev, level;
  int ind[];
  int paren;
  int p_flg[];
  char l_char;
  int ct;
  int s_tabs[][];
  boolean jdoc_flag;
  char cc;
  int tabs;
  char c;

  private final Stack<Boolean> castFlags = new Stack<Boolean>();

  public void init(final Editor editor) {
    this.editor = editor;
  }

  public String getMenuTitle() {
    return "Auto Format";
  }

  private void comment() throws IOException {
    final boolean save_s_flg = s_flag;

    buf.append(c = next()); // extra char
    while (true) {
      buf.append(c = next());
      while ((c != '/')) {
        if (c == '\n') {
          lineNumber++;
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

  private char get_string() throws IOException {
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

  private void handleSingleLineComment() throws IOException {
    c = next();
    while (c != '\n') {
      buf.append(c);
      c = next();
    }
    lineNumber++;
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
  private boolean getnl() throws IOException {
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
      lineNumber++;
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

  public void run() {
    // Adding an additional newline as a hack around other errors
    final String normalizedText = editor.getText().replaceAll("\r", "");
    final String cleanText = normalizedText
        + (normalizedText.endsWith("\n") ? "" : "\n");
    result.setLength(0);
    indentValue = Preferences.getInteger("editor.tabs.size");

    lineNumber = 0;
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
    lineNumber = 1;

    EOF = false; // set in getchr when EOF

    try {
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
          lineNumber++;
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
          if (buf.length() > 0
              || (result.length() > 0 && !Character.isWhitespace(result
                  .charAt(result.length() - 1))))
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
            break;
          }
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
              lineNumber++;
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
          castFlags.push(Pattern.matches("^.*?(?:int|color|float)\\s*$",buf));
          
          buf.append(c);
          paren++;

          if ((lookup("for"))) {
            c = get_string();
            while (c != ';') {
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
              paren = 0;//EOF = true;
              //System.out.println("eof d");
            }
            writeIndentedLine();
            if (getnl()) {
              chars[pos--] = '\n';
              p_flg[level]++;
              tabs++;
              ind[level] = 0;
            }
            break;
          }

          if (lookup("if")) {
            writeIndentedLine();
            s_tabs[c_level][if_lev] = tabs;
            sp_flg[c_level][if_lev] = p_flg[level];
            s_ind[c_level][if_lev] = ind[level];
            if_lev++;
            if_flg = true;
          }
        } // end switch
      } // end while not EOF

      // save current (rough) selection point
      int selectionEnd = editor.getSelectionStop();

      // make sure the caret would be past the end of the text
      if (result.length() < selectionEnd - 1) {
        selectionEnd = result.length() - 1;
      }

      final String formattedText = result.toString();
      if (formattedText.equals(cleanText)) {
        editor.statusNotice("No changes necessary for Auto Format.");

      } else if (paren != 0) {
        // warn user if there are too many parens in either direction
        editor.statusError("Auto Format Canceled: Too many "
            + ((paren < 0) ? "right" : "left") + " parentheses.");

      } else if (c_level != 0) { // check braces only if parens are ok
        editor.statusError("Auto Format Canceled: Too many "
            + ((c_level < 0) ? "right" : "left") + " curly braces.");

      } else {
        // replace with new bootiful text
        // selectionEnd hopefully at least in the neighborhood
        editor.setText(formattedText);
        editor.setSelection(selectionEnd, selectionEnd);
        editor.getSketch().setModified(true);
        // mark as finished
        editor.statusNotice("Auto Format finished.");
      }

    } catch (final Exception e) {
      editor.statusError(e);
    }
  }

  private void skipWhitespace() {
    while (Character.isWhitespace(peek()))
      next();
  }

  private void readUntil(final String string) {
    int matched = 0;
    while (matched < string.length()) {
      final char c = next();
      if (c == string.charAt(matched))
        matched++;
      else if (c == string.charAt(0))
        matched = 1;
      else
        matched = 0;
    }
  }

  private boolean lookahead(final String s) {
    int p = pos + 1;
    while (p < chars.length && Character.isWhitespace(chars[p]))
      p++;
    if (p == chars.length)
      return false;
    for (int i = 0; i < s.length(); i++, p++) {
      if (p == chars.length)
        return false;
      if (s.charAt(i) != chars[p])
        return false;
    }
    if (p == chars.length)
      return false;
    if (Character.isLetter(chars[p]))
      return false;
    return true;
  }

  private void trimRight(final StringBuilder sb) {
    while (sb.length() >= 1
        && Character.isWhitespace(sb.charAt(sb.length() - 1)))
      sb.setLength(sb.length() - 1);
  }
}
