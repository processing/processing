/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeKeywords - handles text coloring and links to html reference
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-10 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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

package processing.app.syntax;

import javax.swing.text.Segment;


/**
 * This class reads a keywords.txt file to get coloring put links to reference
 * locations for the set of keywords.
 */
public class PdeKeywords extends TokenMarker {
  private KeywordMap keywordColoring;

  // lookup table for the TokenMarker subclass, handles coloring
//  private static final KeywordMap keywordColoring;
  // lookup table that maps keywords to their html reference pages
//  private static final Hashtable keywordToReference;
//  private HashMap<String,String> keywordToReference;

  // used internally
  private int lastOffset;
  private int lastKeyword;
  

  /*
  public PdeKeywords(File file) throws IOException {
    //super(false, getKeywords());
//    this.cpp = cpp;
//    this.keywordColoring = keywordColoring;
//    try {
    BufferedReader reader = PApplet.createReader(file);

    keywordColoring = new KeywordMap(false);
    keywordToReference = new HashMap<String, String>();

    //      InputStream input = Base.getLibStream("keywords.txt");
    //      InputStreamReader isr = new InputStreamReader(input);
    //      BufferedReader reader = new BufferedReader(isr);

    String line = null;
    while ((line = reader.readLine()) != null) {
      String pieces[] = processing.core.PApplet.split(line, '\t');
      if (pieces.length >= 2) {
        String keyword = pieces[0].trim();
        String coloring = pieces[1].trim();

        if (coloring.length() > 0) {
          // text will be KEYWORD or LITERAL
          boolean isKey = (coloring.charAt(0) == 'K');
          // KEYWORD1 -> 0, KEYWORD2 -> 1, etc
          int num = coloring.charAt(coloring.length() - 1) - '1';
          byte id = (byte) ((isKey ? Token.KEYWORD1 : Token.LITERAL1) + num);
          //System.out.println("got " + (isKey ? "keyword" : "literal") +
          //                 (num+1) + " for " + keyword);
          keywordColoring.add(keyword, id);
        }
        if (pieces.length >= 3) {
          String htmlFilename = pieces[2].trim();
          if (htmlFilename.length() > 0) {
            keywordToReference.put(keyword, htmlFilename);
          }
        }
      }
    }
    reader.close();

//    } catch (Exception e) {
//      Base.showError("Problem loading keywords",
//                     "Could not load keywords.txt,\n" + 
//                     "please re-install Processing.", e);
//    }
  }
  */
  

  /**
   * Add a keyword, and the associated coloring.
   * @param coloring one of KEYWORD1, KEYWORD2, LITERAL1, etc.
   */
  public void addColoring(String keyword, String coloring) {
    // text will be KEYWORD or LITERAL
    boolean isKey = (coloring.charAt(0) == 'K');
    // KEYWORD1 -> 0, KEYWORD2 -> 1, etc
    int num = coloring.charAt(coloring.length() - 1) - '1';
    byte id = (byte) ((isKey ? Token.KEYWORD1 : Token.LITERAL1) + num);
    keywordColoring.add(keyword, id);
  }


  public byte markTokensImpl(byte token, Segment line, int lineIndex) {
    char[] array = line.array;
    int offset = line.offset;
    lastOffset = offset;
    lastKeyword = offset;
    int mlength = line.count + offset;
    boolean backslash = false;

    loop: for (int i = offset; i < mlength; i++) {
      int i1 = (i + 1);

      char c = array[i];
      if (c == '\\') {
        backslash = !backslash;
        continue;
      }

      switch (token) {
      case Token.NULL:
        switch (c) {
        case '#':
          if (backslash)
            backslash = false;
          break;
        case '"':
          doKeyword(line, i, c);
          if (backslash)
            backslash = false;
          else {
            addToken(i - lastOffset, token);
            token = Token.LITERAL1;
            lastOffset = lastKeyword = i;
          }
          break;
        case '\'':
          doKeyword(line, i, c);
          if (backslash)
            backslash = false;
          else {
            addToken(i - lastOffset, token);
            token = Token.LITERAL2;
            lastOffset = lastKeyword = i;
          }
          break;
        case ':':
          if (lastKeyword == offset) {
            if (doKeyword(line, i, c))
              break;
            backslash = false;
            addToken(i1 - lastOffset, Token.LABEL);
            lastOffset = lastKeyword = i1;
          } else if (doKeyword(line, i, c))
            break;
          break;
        case '/':
          backslash = false;
          doKeyword(line, i, c);
          if (mlength - i > 1) {
            switch (array[i1]) {
            case '*':
              addToken(i - lastOffset, token);
              lastOffset = lastKeyword = i;
              if (mlength - i > 2 && array[i + 2] == '*')
                token = Token.COMMENT2;
              else
                token = Token.COMMENT1;
              break;
            case '/':
              addToken(i - lastOffset, token);
              addToken(mlength - i, Token.COMMENT1);
              lastOffset = lastKeyword = mlength;
              break loop;
            }
            i++; // jdf- fix http://dev.processing.org/bugs/show_bug.cgi?id=609
          }
          break;
        default:
          backslash = false;
          if (!Character.isLetterOrDigit(c) && c != '_')
            doKeyword(line, i, c);
          break;
        }
        break;
      case Token.COMMENT1:
      case Token.COMMENT2:
        backslash = false;
        if (c == '*' && mlength - i > 1) {
          if (array[i1] == '/') {
            i++;
            addToken((i + 1) - lastOffset, token);
            token = Token.NULL;
            lastOffset = lastKeyword = i + 1;
          }
        }
        break;
      case Token.LITERAL1:
        if (backslash)
          backslash = false;
        else if (c == '"') {
          addToken(i1 - lastOffset, token);
          token = Token.NULL;
          lastOffset = lastKeyword = i1;
        }
        break;
      case Token.LITERAL2:
        if (backslash)
          backslash = false;
        else if (c == '\'') {
          addToken(i1 - lastOffset, Token.LITERAL1);
          token = Token.NULL;
          lastOffset = lastKeyword = i1;
        }
        break;
      default:
        throw new InternalError("Invalid state: " + token);
      }
    }

    if (token == Token.NULL)
      doKeyword(line, mlength, '\0');

    switch (token) {
    case Token.LITERAL1:
    case Token.LITERAL2:
      addToken(mlength - lastOffset, Token.INVALID);
      token = Token.NULL;
      break;
    case Token.KEYWORD2:
      addToken(mlength - lastOffset, token);
      if (!backslash)
        token = Token.NULL;
      addToken(mlength - lastOffset, token);
      break;
    default:
      addToken(mlength - lastOffset, token);
      break;
    }
    return token;
  }


  private boolean doKeyword(Segment line, int i, char c) {
    int i1 = i + 1;

    int len = i - lastKeyword;
    byte id = keywordColoring.lookup(line, lastKeyword, len);
    if (id != Token.NULL) {
      if (lastKeyword != lastOffset)
        addToken(lastKeyword - lastOffset, Token.NULL);
      addToken(len, id);
      lastOffset = i;
    }
    lastKeyword = i1;
    return false;
  }
}
