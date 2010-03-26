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

import java.io.CharArrayReader;
import java.io.IOException;
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

  static final int BLOCK_MAXLEN = 64000;

  StringBuffer strOut;
  int indentValue;
  String indentChar;
  boolean EOF;
  CharArrayReader reader;
  int readCount, indexBlock, lineLength, lineNumber;
  char chars[];
  String strBlock;
  int s_level[];
  int c_level;
  int sp_flg[][];
  int s_ind[][];
  int s_if_lev[];
  int s_if_flg[];
  int if_lev, if_flg, level;
  int ind[];
  boolean e_flg;
  int paren;
  static int p_flg[];
  char l_char, p_char;
  int a_flg, q_flg, ct;
  int s_tabs[][];
  String w_if_, w_else, w_for, w_ds, w_case, w_cpp_comment, w_jdoc;
  int jdoc, j;
  char buf[];
  char cc;
  int s_flg;
  int peek;
  char peekc;
  int tabs;
  char last_char;
  char c;

  String line_feed;

  public void init(final Editor editor) {
    this.editor = editor;
  }

  public String getMenuTitle() {
    return "Auto Format";
  }

  private void comment() throws IOException {
    int save_s_flg;
    save_s_flg = s_flg;

    int done = 0;
    c = buf[j++] = getchr(); // extra char
    while (done == 0) {
      c = buf[j++] = getchr();
      while ((c != '/') && (j < buf.length)) {
        if (c == '\n' || c == '\r') {
          lineNumber++;
          putcoms();
          s_flg = 1;
        }
        c = buf[j++] = getchr();
      }
      //String tmpstr = new String(string);
      if (j > 1 && buf[j - 2] == '*') {
        done = 1;
        jdoc = 0;
      }
    }

    putcoms();
    s_flg = save_s_flg;
    jdoc = 0;
    return;
  }

  private char get_string() throws IOException {
    char ch;
    ch = '*';
    while (true) {
      switch (ch) {
      default:
        ch = buf[j++] = getchr();
        if (ch == '\\') {
          buf[j++] = getchr();
          break;
        }
        if (ch == '\'' || ch == '"') {
          cc = buf[j++] = getchr();
          while (!EOF && cc != ch) {
            if (cc == '\\') {
              buf[j++] = getchr();
            }
            cc = buf[j++] = getchr();
          }
          break;
        }
        if (ch == '\n' || ch == '\r') {
          indent_puts();
          a_flg = 1;
          break;
        } else {
          return (ch);
        }
      }
    }
  }

  private void indent_puts() {
    buf[j] = '\0';
    if (j > 0) {
      if (s_flg != 0) {
        if ((tabs > 0) && (buf[0] != '{') && (a_flg == 1)) {
          tabs++;
        }
        p_tabs();
        s_flg = 0;
        if ((tabs > 0) && (buf[0] != '{') && (a_flg == 1)) {
          tabs--;
        }
        a_flg = 0;
      }
      strOut.append(buf, 0, j);
      j = 0;

    } else {
      if (s_flg != 0) {
        s_flg = 0;
        a_flg = 0;
      }
    }
  }

  //public void fprintf(int outfil, String out_string) {
  private void fprintf(final String out_string) {
    //int out_len = out_string.length();
    //String j_string = new String(string);
    strOut.append(out_string);
  }

  /* special edition of put string for comment processing */
  private void putcoms() {
    int i = 0;
    final int sav_s_flg = s_flg;
    if (j > 0) {
      if (s_flg != 0) {
        p_tabs();
        s_flg = 0;
      }
      buf[j] = '\0';
      i = 0;
      while (buf[i] == ' ') {
        i++;
      }
      if (lookup_com(w_jdoc) == 1) {
        jdoc = 1;
      }
      final String strBuffer = new String(buf, 0, j);
      if (buf[i] == '/' && buf[i + 1] == '*') {
        if ((last_char != ';') && (sav_s_flg == 1)) {
          //fprintf(outfil, strBuffer.substring(i,j));
          fprintf(strBuffer.substring(i, j));
        } else {
          //fprintf(outfil, strBuffer);
          fprintf(strBuffer);
        }
      } else {
        if (buf[i] == '*' || jdoc == 0) {
          //fprintf (outfil, " "+strBuffer.substring(i,j));
          fprintf(" " + strBuffer.substring(i, j));
        } else {
          //fprintf (outfil, " * "+strBuffer.substring(i,j));
          fprintf(" * " + strBuffer.substring(i, j));
        }
      }
      j = 0;
      buf[0] = '\0';
    }
  }

  private void cpp_comment() throws IOException {
    c = getchr();
    while (c != '\n' && c != '\r' && j < buf.length) {
      buf[j++] = c;
      c = getchr();
    }
    lineNumber++;
    indent_puts();
    s_flg = 1;
  }

  /* expand indentValue into tabs and spaces */
  private void p_tabs() {
    int i, k;

    if (tabs < 0) {
      tabs = 0;
    }
    if (tabs == 0) {
      return;
    }
    i = tabs * indentValue; // calc number of spaces
    //j = i/8;        /* calc number of tab chars */

    for (k = 0; k < i; k++) {
      strOut.append(indentChar);
    }
  }

  private char getchr() throws IOException {
    if ((peek < 0) && (last_char != ' ') && (last_char != '\t')) {
      if ((last_char != '\n') && (last_char != '\r')) {
        p_char = last_char;
      }
    }
    if (peek > 0) /* char was read previously */
    {
      last_char = peekc;
      peek = -1;
    } else /* read next char in string */
    {
      indexBlock++;
      if (indexBlock >= lineLength) {
        lineLength = readCount = 0;
        reader.mark(1);
        if (reader.read() != -1) {
          reader.reset(); // back to the mark
          readCount = reader.read(chars);
          lineLength = readCount;
          strBlock = new String(chars);
          indexBlock = 0;
          last_char = strBlock.charAt(indexBlock);
          peek = -1;
          peekc = '`';
        } else {
          EOF = true;
          peekc = '\0';
        }
      } else {
        last_char = strBlock.charAt(indexBlock);
      }
    }
    peek = -1;
    if (last_char == '\r') {
      last_char = getchr();
    }

    return last_char;
  }

  /* else processing */
  private void gotelse() {
    tabs = s_tabs[c_level][if_lev];
    p_flg[level] = sp_flg[c_level][if_lev];
    ind[level] = s_ind[c_level][if_lev];
    if_flg = 1;
  }

  /* read to new_line */
  private int getnl() throws IOException {
    int save_s_flg;
    save_s_flg = tabs;
    peekc = getchr();
    //while ((peekc == '\t' || peekc == ' ') &&
    //     (j < string.length)) {
    while (peekc == '\t' || peekc == ' ') {
      buf[j++] = peekc;
      peek = -1;
      peekc = '`';
      peekc = getchr();
      peek = 1;
    }
    peek = 1;

    if (peekc == '/') {
      peek = -1;
      peekc = '`';
      peekc = getchr();
      if (peekc == '*') {
        buf[j++] = '/';
        buf[j++] = '*';
        peek = -1;
        peekc = '`';
        comment();
      } else if (peekc == '/') {
        buf[j++] = '/';
        buf[j++] = '/';
        peek = -1;
        peekc = '`';
        cpp_comment();
        return (1);
      } else {
        buf[j++] = '/';
        peek = 1;
      }
    }
    peekc = getchr();
    if (peekc == '\n') {
      lineNumber++;
      peek = -1;
      peekc = '`';
      tabs = save_s_flg;
      return (1);
    } else {
      peek = 1;
    }
    return 0;
  }

  private boolean lookup(final String keyword) {
    char r;
    int l, kk; //,k,i;
    if (j < 1) {
      return false;
    }
    kk = 0;
    while (buf[kk] == ' ') {
      kk++;
    }
    l = 0;
    l = new String(buf).indexOf(keyword);
    if (l < 0 || l != kk) {
      return false;
    }
    r = buf[kk + keyword.length()];
    if (r >= 'a' && r <= 'z') {
      return false;
    }
    if (r >= 'A' && r <= 'Z') {
      return false;
    }
    if (r >= '0' && r <= '9') {
      return false;
    }
    if (r == '_' || r == '&') {
      return false;
    }
    return true;
  }

  private int lookup_com(final String keyword) {
    //char r;
    int l, kk; //,k,i;
    final String j_string = new String(buf);

    if (j < 1) {
      return (0);
    }
    kk = 0;
    while (buf[kk] == ' ') {
      kk++;
    }
    l = 0;
    l = j_string.indexOf(keyword);
    if (l < 0 || l != kk) {
      return 0;
    }
    return (1);
  }

  public void run() {
    StringBuffer onechar;

    // Adding an additional newline as a hack around other errors
    final String originalText = editor.getText() + "\n";
    strOut = new StringBuffer();
    indentValue = Preferences.getInteger("editor.tabs.size");
    indentChar = new String(" ");

    lineNumber = 0;
    e_flg = false;
    c_level = if_lev = level = paren = 0;
    a_flg = q_flg = j = tabs = 0;
    if_flg = peek = -1;
    peekc = '`';
    s_flg = 1;
    jdoc = 0;

    s_level = new int[10];
    sp_flg = new int[20][10];
    s_ind = new int[20][10];
    s_if_lev = new int[10];
    s_if_flg = new int[10];
    ind = new int[10];
    p_flg = new int[10];
    s_tabs = new int[20][10];

    w_else = "else";
    w_if_ = new String("if");
    w_for = new String("for");
    w_ds = new String("default");
    w_case = new String("case");
    w_cpp_comment = new String("//");
    w_jdoc = new String("/**");
    line_feed = new String("\n");

    // read as long as there is something to read
    EOF = false; // = 1 set in getchr when EOF

    chars = new char[BLOCK_MAXLEN];
    buf = new char[BLOCK_MAXLEN];
    try { // the whole process
      // open for input
      reader = new CharArrayReader(originalText.toCharArray());
      lineLength = readCount = 0;
      // read up a block - remember how many bytes read
      readCount = reader.read(chars);
      strBlock = new String(chars);

      lineLength = readCount;
      lineNumber = 1;
      indexBlock = -1;
      j = 0;
      while (!EOF) {
        c = getchr();
        switch (c) {
        default:
          buf[j++] = c;
          if (c != ',') {
            l_char = c;
          }
          break;

        case ' ':
        case '\t':
          if (lookup(w_else)) {
            gotelse();
            if (s_flg == 0 || j > 0) {
              buf[j++] = c;
            }
            indent_puts();
            s_flg = 0;
            break;
          }
          if (s_flg == 0 || j > 0) {
            buf[j++] = c;
          }
          break;

        case '\r': // <CR> for MS Windows 95
        case '\n':
          lineNumber++;
          if (EOF) {
            break;
          }
          //String j_string = new String(string);

          e_flg = lookup(w_else);
          if (e_flg) {
            gotelse();
          }
          if (lookup_com(w_cpp_comment) == 1) {
            if (buf[j] == '\n') {
              buf[j] = '\0';
              j--;
            }
          }

          indent_puts();
          //fprintf(outfil, line_feed);
          fprintf(line_feed);
          s_flg = 1;
          if (e_flg) {
            p_flg[level]++;
            tabs++;
          } else if (p_char == l_char) {
            a_flg = 1;
          }
          break;

        case '{':
          if (lookup(w_else)) {
            gotelse();
          }
          if (s_if_lev.length == c_level) {
            s_if_lev = PApplet.expand(s_if_lev);
            s_if_flg = PApplet.expand(s_if_flg);
          }
          s_if_lev[c_level] = if_lev;
          s_if_flg[c_level] = if_flg;
          if_lev = if_flg = 0;
          c_level++;
          if (s_flg == 1 && p_flg[level] != 0) {
            p_flg[level]--;
            tabs--;
          }
          buf[j++] = c;
          indent_puts();
          getnl();
          indent_puts();
          //fprintf(outfil,"\n");
          fprintf("\n");
          tabs++;
          s_flg = 1;
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
            //EOF = true;
            //System.out.println("eof b");
            buf[j++] = c;
            indent_puts();
            break;
          }
          if ((if_lev = s_if_lev[c_level] - 1) < 0) {
            if_lev = 0;
          }
          if_flg = s_if_flg[c_level];
          indent_puts();
          tabs--;
          p_tabs();
          peekc = getchr();
          if (peekc == ';') {
            onechar = new StringBuffer();
            onechar.append(c); // the }
            onechar.append(';');
            //fprintf(outfil, onechar.toString());
            fprintf(onechar.toString());
            peek = -1;
            peekc = '`';
          } else {
            onechar = new StringBuffer();
            onechar.append(c);
            //fprintf(outfil, onechar.toString());
            fprintf(onechar.toString());
            peek = 1;
          }
          getnl();
          indent_puts();
          //fprintf(outfil,"\n");
          fprintf("\n");
          s_flg = 1;
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
          buf[j++] = c;
          cc = getchr();
          while (!EOF && cc != c) {
            // max. length of line should be 256
            buf[j++] = cc;

            if (cc == '\\') {
              cc = buf[j++] = getchr();
            }
            if (cc == '\n') {
              lineNumber++;
              indent_puts();
              s_flg = 1;
            }
            cc = getchr();

          }
          buf[j++] = cc;
          if (getnl() == 1) {
            l_char = cc;
            peek = 1;
            peekc = '\n';
          }
          break;

        case ';':
          buf[j++] = c;
          indent_puts();
          if (p_flg[level] > 0 && ind[level] == 0) {
            tabs -= p_flg[level];
            p_flg[level] = 0;
          }
          getnl();
          indent_puts();
          //fprintf(outfil,"\n");
          fprintf("\n");
          s_flg = 1;
          if (if_lev > 0) {
            if (if_flg == 1) {
              if_lev--;
              if_flg = 0;
            } else {
              if_lev = 0;
            }
          }
          break;

        case '\\':
          buf[j++] = c;
          buf[j++] = getchr();
          break;

        case '?':
          q_flg = 1;
          buf[j++] = c;
          break;

        case ':':
          buf[j++] = c;
          peekc = getchr();
          if (peekc == ':') {
            indent_puts();
            //fprintf (outfil,":");
            fprintf(":");
            peek = -1;
            peekc = '`';
            break;
          } else {
            //int double_colon = 0;
            peek = 1;
          }

          if (q_flg == 1) {
            q_flg = 0;
            break;
          }
          if (!lookup(w_ds) && !lookup(w_case)) {
            s_flg = 0;
            indent_puts();
          } else {
            tabs--;
            indent_puts();
            tabs++;
          }
          peekc = getchr();
          if (peekc == ';') {
            fprintf(";");
            peek = -1;
            peekc = '`';
          } else {
            peek = 1;
          }
          getnl();
          indent_puts();
          fprintf("\n");
          s_flg = 1;
          break;

        case '/':
          buf[j++] = c;
          peekc = getchr();

          if (peekc == '/') {
            buf[j++] = peekc;
            peekc = '`';
            peek = -1;
            cpp_comment();
            //fprintf(outfil,"\n");
            fprintf("\n");
            break;
          } else {
            peek = 1;
          }

          if (peekc != '*') {
            break;
          } else {
            if (j > 0) {
              buf[j--] = '\0';
            }
            if (j > 0) {
              indent_puts();
            }
            buf[j++] = '/';
            buf[j++] = '*';
            peek = -1;
            peekc = '`';
            comment();
            break;
          }

        case ')':
          paren--;
          if (paren < 0) {
            paren = 0;//EOF = true;
            //System.out.println("eof c");
          }
          buf[j++] = c;
          indent_puts();
          if (getnl() == 1) {
            peekc = '\n';
            peek = 1;
            if (paren != 0) {
              a_flg = 1;
            } else if (tabs > 0) {
              p_flg[level]++;
              tabs++;
              ind[level] = 0;
            }
          }
          break;

        case '(':
          buf[j++] = c;
          paren++;
          if ((lookup(w_for))) {
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
            indent_puts();
            if (getnl() == 1) {
              peekc = '\n';
              peek = 1;
              p_flg[level]++;
              tabs++;
              ind[level] = 0;
            }
            break;
          }

          if (lookup(w_if_)) {
            indent_puts();
            s_tabs[c_level][if_lev] = tabs;
            sp_flg[c_level][if_lev] = p_flg[level];
            s_ind[c_level][if_lev] = ind[level];
            if_lev++;
            if_flg = 1;
          }
        } // end switch

        //System.out.println("string len is " + string.length);
        //if (EOF == 1) System.out.println(string);
        //String j_string = new String(string);

      } // end while not EOF

      /*
      int bad;
      while ((bad = bin.read()) != -1) {
        System.out.print((char) bad);
      }
      */
      /*
      char bad;
      //while ((bad = getchr()) != 0) {
      while (true) {
        getchr();
        if (peek != -1) {
          System.out.print(last_char);
        } else {
          break;
        }
      }
      */

      // save current (rough) selection point
      int selectionEnd = editor.getSelectionStop();

      // make sure the caret would be past the end of the text
      if (strOut.length() < selectionEnd - 1) {
        selectionEnd = strOut.length() - 1;
      }

      reader.close(); // close buff

      final String formattedText = strOut.toString();
      if (formattedText.equals(originalText)) {
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
}
