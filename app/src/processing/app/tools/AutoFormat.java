/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2005-06 Ben Fry and Casey Reas
  Copyright (c) 2003 Martin Gomez, Ateneo de Manila University

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

package processing.app.tools;

import processing.app.*;

import java.io.*;


/**
 * Alternate handler for dealing with auto format.
 * Contributed by Martin Gomez, additional bug fixes by Ben Fry.
 */
public class AutoFormat implements Tool {
  Editor editor;

  static final int BLOCK_MAXLEN = 1024;

  StringBuffer strOut;
  //String formattedText;
  int indentValue;
  String indentChar;
  //String uhOh = null;
  //String theStuff;
  int EOF;
  BufferedInputStream bin = null;
  int nBytesRead, indexBlock, lineLength, lineNumber;
  byte bArray[];
  String strBlock;
  int s_level[];
  int c_level;
  int sp_flg[][];
  int s_ind[][];
  int s_if_lev[];
  int s_if_flg[];
  int if_lev, if_flg, level;
  int ind[];
  int e_flg, paren;
  static int p_flg[];
  char l_char, p_char;
  int a_flg, q_flg, ct;
  int s_tabs[][];
  String w_if_, w_else, w_for, w_ds, w_case, w_cpp_comment, w_jdoc;
  int jdoc, j;
  char string[];
  byte bstring[];
  byte bblank;
  char cc;
  int s_flg, b_flg;
  int peek;
  char peekc;
  int tabs;
  char next_char, last_char;
  char lastc0, lastc1;
  char c, c0;
  char w_kptr;

  String line_feed;

  //static int outfil;  // temporary


  public void init(Editor editor) {
    this.editor = editor;
  }

  
  public String getMenuTitle() {
    return "Auto Format";
  }

  public void comment() throws IOException {
    int save_s_flg;
    save_s_flg = s_flg;

    int done = 0;
    c = string[j++] = getchr(); // extra char
    while (done == 0) {
      c = string[j++] = getchr();
      while ((c != '/') && (j < string.length)) {
        if(c == '\n' || c == '\r') {
          lineNumber++;
          putcoms();
          s_flg = 1;
        }
        c = string[j++] = getchr();
      }
      //String tmpstr = new String(string);
      if (j>1 && string[j-2] == '*') {
        done = 1;
        jdoc = 0;
      }
    }

    putcoms();
    s_flg = save_s_flg;
    jdoc = 0;
    return;
  }


  public char get_string() throws IOException {
    char ch;
    ch = '*';
    while (true) {
      switch (ch) {
      default:
        ch = string[j++] = getchr();
        if (ch == '\\') {
          string[j++] = getchr();
          break;
        }
        if (ch == '\'' || ch == '"') {
          cc = string[j++] = getchr();
          while (cc != ch) {
            if (cc == '\\') string[j++] = getchr();
            cc = string[j++] = getchr();
          }
          break;
        }
        if (ch == '\n' || ch == '\r') {
          indent_puts();
          a_flg = 1;
          break;
        } else {
          return(ch);
        }
      }
    }
  }


  public void indent_puts() {
    string[j] = '\0';
    if (j > 0) {
      if (s_flg != 0) {
        if((tabs > 0) && (string[0] != '{') && (a_flg == 1)) {
          tabs++;
        }
        p_tabs();
        s_flg = 0;
        if ((tabs > 0) && (string[0] != '{') && (a_flg == 1)) {
          tabs--;
        }
        a_flg = 0;
      }
      String j_string = new String(string);
      strOut.append(j_string.substring(0,j));
      for (int i=0; i<j; i++) string[i] = '\0';
      j = 0;

    } else {
      if (s_flg != 0) {
        s_flg = 0;
        a_flg = 0;
      }
    }
  }


  //public void fprintf(int outfil, String out_string) {
  public void fprintf(String out_string) {
    //int out_len = out_string.length();
    //String j_string = new String(string);
    strOut.append(out_string);
  }


  public int grabLines() {
    return lineNumber;
  }


  /* special edition of put string for comment processing */
  public void putcoms()
  {
    int i = 0;
    int sav_s_flg = s_flg;
    if(j > 0)
    {
      if(s_flg != 0)
      {
        p_tabs();
        s_flg = 0;
      }
      string[j] = '\0';
      i = 0;
      while (string[i] == ' ') i++;
      if (lookup_com(w_jdoc) == 1) jdoc = 1;
      String strBuffer = new String(string,0,j);
      if (string[i] == '/' && string[i+1]=='*')
      {
        if ((last_char != ';') && (sav_s_flg==1) )
        {
          //fprintf(outfil, strBuffer.substring(i,j));
          fprintf(strBuffer.substring(i,j));
        }
        else
        {
          //fprintf(outfil, strBuffer);
          fprintf(strBuffer);
        }
      }
      else
      {
        if (string[i]=='*' || jdoc == 0)
          //fprintf (outfil, " "+strBuffer.substring(i,j));
          fprintf (" "+strBuffer.substring(i,j));
        else
          //fprintf (outfil, " * "+strBuffer.substring(i,j));
          fprintf (" * "+strBuffer.substring(i,j));
      }
      j = 0;
      string[0] = '\0';
    }
  }

  public void cpp_comment() throws IOException
  {
    c = getchr();
    while(c != '\n' && c != '\r' && j<133)
    {
      string[j++] = c;
      c = getchr();
    }
    lineNumber++;
    indent_puts();
    s_flg = 1;
  }


  /* expand indentValue into tabs and spaces */
  public void p_tabs()
  {
    int i,k;

    if (tabs<0) tabs = 0;
    if (tabs==0) return;
    i = tabs * indentValue;  // calc number of spaces
    //j = i/8;        /* calc number of tab chars */

    for (k=0; k < i; k++) {
      strOut.append(indentChar);
    }
  }


  public char getchr() throws IOException
  {
    if((peek < 0) && (last_char != ' ') && (last_char != '\t'))
    {
      if((last_char != '\n') && (last_char != '\r'))
        p_char = last_char;
    }
    if(peek > 0)        /* char was read previously */
    {
      last_char = peekc;
      peek = -1;
    }
    else                    /* read next char in string */
    {
      indexBlock++;
      if (indexBlock >= lineLength)
      {
        for (int ib=0; ib<nBytesRead; ib++) bArray[ib] = '\0';

        lineLength = nBytesRead = 0;
        //try /* to get the next block */
        //{
          if (bin.available() > 0)
          {
            nBytesRead = bin.read(bArray);
            lineLength = nBytesRead;
            strBlock = new String(bArray);
            indexBlock = 0;
            last_char = strBlock.charAt(indexBlock);
            peek = -1;
            peekc = '`';
          }
          else
          {
            //System.out.println("eof a");
            EOF = 1;
            peekc  = '\0';
          }
          //}
          //catch(IOException ioe)
          //{
          //System.out.println(ioe.toString());
          //}
      }
      else
      {
        last_char = strBlock.charAt(indexBlock);
      }
    }
    peek = -1;
    if (last_char == '\r')
    {
      last_char = getchr();
    }

    return last_char;
  }

  /* else processing */
  public void gotelse()
  {
    tabs = s_tabs[c_level][if_lev];
    p_flg[level] = sp_flg[c_level][if_lev];
    ind[level] = s_ind[c_level][if_lev];
    if_flg = 1;
  }

  /* read to new_line */
  public int getnl() throws IOException
  {
    int save_s_flg;
    save_s_flg = tabs;
    peekc = getchr();
    //while ((peekc == '\t' || peekc == ' ') &&
    //     (j < string.length)) {
    while (peekc == '\t' || peekc == ' ') {
      string[j++] = peekc;
      peek = -1;
      peekc = '`';
      peekc = getchr();
      peek = 1;
    }
    peek = 1;

    if (peekc == '/')
    {
      peek = -1;
      peekc = '`';
      peekc = getchr();
      if (peekc == '*')
      {
        string[j++] = '/';
        string[j++] = '*';
        peek = -1;
        peekc = '`';
        comment();
      }
      else if (peekc == '/')
      {
        string[j++] = '/';
        string[j++] = '/';
        peek = -1;
        peekc = '`';
        cpp_comment();
        return (1);
      }
      else
      {
        string[j++] = '/';
        peek = 1;
      }
    }
    peekc = getchr();
    if(peekc == '\n')
    {
      lineNumber++;
      peek = -1;
      peekc = '`';
      tabs = save_s_flg;
      return(1);
    }
    else
    {
      peek = 1;
    }
    return 0;
  }

  public int lookup (String keyword)
  {
    char r;
    int  l,kk; //,k,i;
    String j_string = new String(string);

    if (j<1) return (0);
    kk=0;
    while(string[kk] == ' ')kk++;
    l=0;
    l = j_string.indexOf(keyword);
    if (l<0 || l!=kk)
    {
      return 0;
    }
    r = string[kk+keyword.length()];
    if(r >= 'a' && r <= 'z') return(0);
    if(r >= 'A' && r <= 'Z') return(0);
    if(r >= '0' && r <= '9') return(0);
    if(r == '_' || r == '&') return(0);
    return (1);
  }

  public int lookup_com (String keyword)
  {
    //char r;
    int  l,kk; //,k,i;
    String j_string = new String(string);

    if (j<1) return (0);
    kk=0;
    while(string[kk] == ' ')kk++;
    l=0;
    l = j_string.indexOf(keyword);
    if (l<0 || l!=kk)
    {
      return 0;
    }
    return (1);
  }


  public void run() {
    StringBuffer onechar;

    // Adding an additional newline as a hack around other errors
    String originalText = editor.getText() + "\n";
    strOut = new StringBuffer();
    indentValue = Preferences.getInteger("editor.tabs.size");
    indentChar = new String(" ");

    lineNumber = 0;
    //BLOCK_MAXLEN = 256;
    c_level = if_lev = level = e_flg = paren = 0;
    a_flg = q_flg = j = b_flg = tabs = 0;
    if_flg = peek = -1;
    peekc = '`';
    s_flg = 1;
    bblank = ' ';
    jdoc = 0;

    s_level  = new int[10];
    sp_flg   = new int[20][10];
    s_ind    = new int[20][10];
    s_if_lev = new int[10];
    s_if_flg = new int[10];
    ind      = new int[10];
    p_flg    = new int[10];
    s_tabs   = new int[20][10];

    w_else = new String ("else");
    w_if_ = new String ("if");
    w_for = new String ("for");
    w_ds  = new String ("default");
    w_case  = new String ("case");
    w_cpp_comment = new String ("//");
    w_jdoc = new String ("/**");
    line_feed = new String ("\n");

    // read as long as there is something to read
    EOF = 0;  // = 1 set in getchr when EOF

    bArray = new byte[BLOCK_MAXLEN];
    string = new char[BLOCK_MAXLEN];
    try {  // the whole process
      // open for input
      ByteArrayInputStream in =
        new ByteArrayInputStream(originalText.getBytes());

      // add buffering to that InputStream
      bin = new BufferedInputStream(in);

      for (int ib = 0; ib < BLOCK_MAXLEN; ib++) bArray[ib] = '\0';

      lineLength = nBytesRead = 0;
      // read up a block - remember how many bytes read
      nBytesRead = bin.read(bArray);
      strBlock = new String(bArray);

      lineLength = nBytesRead;
      lineNumber  = 1;
      indexBlock = -1;
      j = 0;
      while (EOF == 0)
      {
        c = getchr();
        switch(c)
        {
        default:
          string[j++] = c;
          if(c != ',')
          {
            l_char = c;
          }
          break;

        case ' ':
        case '\t':
          if(lookup(w_else) == 1)
          {
            gotelse();
            if(s_flg == 0 || j > 0)string[j++] = c;
            indent_puts();
            s_flg = 0;
            break;
          }
          if(s_flg == 0 || j > 0)string[j++] = c;
          break;

        case '\r':  // <CR> for MS Windows 95
        case '\n':
          lineNumber++;
          if (EOF==1)
          {
            break;
          }
          //String j_string = new String(string);

          e_flg = lookup(w_else);
          if(e_flg == 1) gotelse();
          if (lookup_com(w_cpp_comment) == 1)
          {
            if (string[j] == '\n')
            {
              string[j] = '\0';
              j--;
            }
          }

          indent_puts();
          //fprintf(outfil, line_feed);
          fprintf(line_feed);
          s_flg = 1;
          if(e_flg == 1)
          {
            p_flg[level]++;
            tabs++;
          }
          else
            if(p_char == l_char)
            {
              a_flg = 1;
            }
          break;

        case '{':
          if(lookup(w_else) == 1)gotelse();
          s_if_lev[c_level] = if_lev;
          s_if_flg[c_level] = if_flg;
          if_lev = if_flg = 0;
          c_level++;
          if(s_flg == 1 && p_flg[level] != 0)
          {
            p_flg[level]--;
            tabs--;
          }
          string[j++] = c;
          indent_puts();
          getnl() ;
          indent_puts();
          //fprintf(outfil,"\n");
          fprintf("\n");
          tabs++;
          s_flg = 1;
          if(p_flg[level] > 0)
          {
            ind[level] = 1;
            level++;
            s_level[level] = c_level;
          }
          break;

        case '}':
          c_level--;
          if (c_level < 0)
          {
            EOF = 1;
            //System.out.println("eof b");
            string[j++] = c;
            indent_puts();
            break;
          }
          if ((if_lev = s_if_lev[c_level]-1) < 0)
            if_lev = 0;
          if_flg = s_if_flg[c_level];
          indent_puts();
          tabs--;
          p_tabs();
          peekc = getchr();
          if( peekc == ';')
          {
            onechar = new StringBuffer();
            onechar.append(c);   // the }
            onechar.append(';');
            //fprintf(outfil, onechar.toString());
            fprintf(onechar.toString());
            peek = -1;
            peekc = '`';
          }
          else
          {
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
          if(c_level < s_level[level])
            if(level > 0) level--;
          if(ind[level] != 0)
          {
            tabs -= p_flg[level];
            p_flg[level] = 0;
            ind[level] = 0;
          }
          break;

        case '"':
        case '\'':
          string[j++] = c;
          cc = getchr();
          while(cc != c)
          {
            // max. length of line should be 256
            string[j++] = cc;

            if(cc == '\\')
            {
              cc = string[j++] = getchr();
            }
            if(cc == '\n')
            {
              lineNumber++;
              indent_puts();
              s_flg = 1;
            }
            cc = getchr();

          }
          string[j++] = cc;
          if(getnl() == 1)
          {
            l_char = cc;
            peek = 1;
            peekc = '\n';
          }
          break;

        case ';':
          string[j++] = c;
          indent_puts();
          if(p_flg[level] > 0 && ind[level] == 0)
          {
            tabs -= p_flg[level];
            p_flg[level] = 0;
          }
          getnl();
          indent_puts();
          //fprintf(outfil,"\n");
          fprintf("\n");
          s_flg = 1;
          if(if_lev > 0)
            if(if_flg == 1)
            {
              if_lev--;
              if_flg = 0;
            }
            else if_lev = 0;
          break;

        case '\\':
          string[j++] = c;
          string[j++] = getchr();
          break;

        case '?':
          q_flg = 1;
          string[j++] = c;
          break;

        case ':':
          string[j++] = c;
          peekc = getchr();
          if(peekc == ':')
          {
            indent_puts();
            //fprintf (outfil,":");
            fprintf(":");
            peek = -1;
            peekc = '`';
            break;
          }
          else
          {
            //int double_colon = 0;
            peek = 1;
          }

          if(q_flg == 1)
          {
            q_flg = 0;
            break;
          }
          if(lookup(w_ds) == 0 && lookup(w_case) == 0)
          {
            s_flg = 0;
            indent_puts();
          }
          else
          {
            tabs--;
            indent_puts();
            tabs++;
          }
          peekc = getchr();
          if(peekc == ';')
          {
            //fprintf(outfil,";");
            fprintf(";");
            peek = -1;
            peekc = '`';
          }
          else
          {
            peek = 1;
          }
          getnl();
          indent_puts();
          //fprintf(outfil,"\n");
          fprintf("\n");
          s_flg = 1;
          break;

        case '/':
          c0 = string[j];
          string[j++] = c;
          peekc = getchr();

          if(peekc == '/')
          {
            string[j++] = peekc;
            peekc = '`';
            peek = -1;
            cpp_comment();
            //fprintf(outfil,"\n");
            fprintf("\n");
            break;
          }
          else
          {
            peek = 1;
          }

          if(peekc != '*') {
            break;
          }
          else
          {
            if (j > 0) string[j--] = '\0';
            if (j > 0) indent_puts();
            string[j++] = '/';
            string[j++] = '*';
            peek = -1;
            peekc = '`';
            comment();
            break;
          }

        case '#':
          string[j++] = c;
          cc = getchr();
          while(cc != '\n')
          {
            string[j++] = cc;
            cc = getchr();
          }
          string[j++] = cc;
          s_flg = 0;
          indent_puts();
          s_flg = 1;
          break;

        case ')':
          paren--;
          if (paren < 0)
          {
            EOF = 1;
            //System.out.println("eof c");
          }
          string[j++] = c;
          indent_puts();
          if(getnl() == 1)
          {
            peekc = '\n';
            peek = 1;
            if(paren != 0)
            {
              a_flg = 1;
            }
            else if(tabs > 0)
            {
              p_flg[level]++;
              tabs++;
              ind[level] = 0;
            }
          }
          break;

        case '(':
          string[j++] = c;
          paren++;
          if ((lookup(w_for) == 1))
          {
            c = get_string();
            while(c != ';') c = get_string();
            ct=0;
            int for_done = 0;
            while (for_done==0)
            {
              c = get_string();
              while(c != ')')
              {
                if(c == '(') ct++;
                c = get_string();
              }
              if(ct != 0)
              {
                ct--;
              }
              else for_done = 1;
            }                        // endwhile for_done
            paren--;
            if (paren < 0)
            {
              EOF = 1;
              //System.out.println("eof d");
            }
            indent_puts();
            if(getnl() == 1)
            {
              peekc = '\n';
              peek = 1;
              p_flg[level]++;
              tabs++;
              ind[level] = 0;
            }
            break;
          }

          if(lookup(w_if_) == 1)
          {
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

      bin.close(); // close buff

      String formattedText = strOut.toString();
      if (formattedText.equals(originalText)) {
        editor.statusNotice("No changes necessary for Auto Format.");

      } else if (paren != 0) {
        // warn user if there are too many parens in either direction
        editor.statusError("Auto Format Canceled: Too many " +
                     ((paren < 0) ? "right" : "left") +
                     " parentheses.");

      } else if (c_level != 0) {  // check braces only if parens are ok
        editor.statusError("Auto Format Canceled: Too many " +
                     ((c_level < 0) ? "right" : "left") +
                     " curly braces.");

      } else {
        // replace with new bootiful text
        // selectionEnd hopefully at least in the neighborhood
        editor.setText(formattedText);
        editor.setSelection(selectionEnd, selectionEnd);
        editor.getSketch().setModified(true);
        // mark as finished
        editor.statusNotice("Auto Format finished.");
      }

    } catch (Exception e) {
      editor.statusError(e);
    }
  }
}
