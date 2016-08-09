/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeKeywords - handles text coloring and links to html reference
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-12 Ben Fry and Casey Reas
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

import processing.app.ui.Editor;


public class PdeTokenMarker extends TokenMarker {
  protected KeywordMap keywordColoring;

//  protected int lastOffset;
//  protected int lastKeyword;


  /**
   * Add a keyword, and the associated coloring. KEYWORD2 and KEYWORD3
   * should only be used with functions (where parens are present).
   * This is done for the extra paren handling.
   * @param coloring one of KEYWORD1, KEYWORD2, LITERAL1, etc.
   */
  public void addColoring(String keyword, String coloring) {
    if (keywordColoring == null) {
      keywordColoring = new KeywordMap(false);
    }
    // KEYWORD1 -> 0, KEYWORD2 -> 1, etc
    int num = coloring.charAt(coloring.length() - 1) - '1';
//    byte id = (byte) ((isKey ? Token.KEYWORD1 : Token.LITERAL1) + num);
    int id = 0;
    switch (coloring.charAt(0)) {
      case 'K':
        id = Token.KEYWORD1 + num;
        keywordColoring.add(keyword, (byte) id, false);
        if (id == Token.KEYWORD6) {
          // these can be followed by parens
          keywordColoring.add(keyword, (byte) id, true);
        }
        break;
      case 'L':
        id = Token.LITERAL1 + num;
        keywordColoring.add(keyword, (byte) id, false);
        break;
      case 'F':
        id = Token.FUNCTION1 + num;
        keywordColoring.add(keyword, (byte) id, true);
        break;
    }
  }


  class MarkerState {
    int lastOffset;
    int lastKeyword;

    MarkerState(int offset) {
      lastOffset = offset;
      lastKeyword = offset;
    }
  }


  public byte markTokensImpl(byte token, Segment line, int lineIndex) {
    char[] array = line.array;
    int offset = line.offset;
//    lastOffset = offset;
//    lastKeyword = offset;
    MarkerState ms = new MarkerState(offset);
    int mlength = offset + line.count;
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
          doKeyword(ms, line, i, c);
          if (backslash)
            backslash = false;
          else {
            addToken(i - ms.lastOffset, token);
            token = Token.LITERAL1;
            ms.lastOffset = ms.lastKeyword = i;
          }
          break;
        case '\'':
          doKeyword(ms, line, i, c);
          if (backslash)
            backslash = false;
          else {
            addToken(i - ms.lastOffset, token);
            token = Token.LITERAL2;
            ms.lastOffset = ms.lastKeyword = i;
          }
          break;
        case ':':
          if (ms.lastKeyword == offset) {
            if (doKeyword(ms, line, i, c))
              break;
            backslash = false;
            addToken(i1 - ms.lastOffset, Token.LABEL);
            ms.lastOffset = ms.lastKeyword = i1;
          } else if (doKeyword(ms, line, i, c))
            break;
          break;
        case '/':
          backslash = false;
          doKeyword(ms, line, i, c);
          if (mlength - i > 1) {
            switch (array[i1]) {
            case '*':
              addToken(i - ms.lastOffset, token);
              ms.lastOffset = ms.lastKeyword = i;
              if (mlength - i > 2 && array[i + 2] == '*')
                token = Token.COMMENT2;
              else
                token = Token.COMMENT1;
              break;
            case '/':
              addToken(i - ms.lastOffset, token);
              addToken(mlength - i, Token.COMMENT1);
              ms.lastOffset = ms.lastKeyword = mlength;
              break loop;
            }
            // https://github.com/processing/processing/issues/1681
            if (array[i1] != ' ') {
              i++;  // http://processing.org/bugs/bugzilla/609.html [jdf]
            }
          }
          break;
        default:
          backslash = false;
          if (!Character.isLetterOrDigit(c) && c != '_') {
            doKeyword(ms, line, i, c);
          }
          break;
        }
        break;
      case Token.COMMENT1:
      case Token.COMMENT2:
        backslash = false;
        if (c == '*' && mlength - i > 1) {
          if (array[i1] == '/') {
            i++;
            addToken((i + 1) - ms.lastOffset, token);
            token = Token.NULL;
            ms.lastOffset = ms.lastKeyword = i + 1;
          }
        }
        break;
      case Token.LITERAL1:
        if (backslash)
          backslash = false;
        else if (c == '"') {
          addToken(i1 - ms.lastOffset, token);
          token = Token.NULL;
          ms.lastOffset = ms.lastKeyword = i1;
        }
        break;
      case Token.LITERAL2:
        if (backslash)
          backslash = false;
        else if (c == '\'') {
          addToken(i1 - ms.lastOffset, Token.LITERAL1);
          token = Token.NULL;
          ms.lastOffset = ms.lastKeyword = i1;
        }
        break;
      default:
        throw new InternalError("Invalid state: " + token);
      }
    }

    if (token == Token.NULL) {
      doKeyword(ms, line, mlength, '\0');
    }

    switch (token) {
    case Token.LITERAL1:
    case Token.LITERAL2:
      addToken(mlength - ms.lastOffset, Token.INVALID);
      token = Token.NULL;
      break;
    case Token.KEYWORD2:
      addToken(mlength - ms.lastOffset, token);
      if (!backslash)
        token = Token.NULL;
      addToken(mlength - ms.lastOffset, token);
      break;
    default:
      addToken(mlength - ms.lastOffset, token);
      break;
    }
    return token;
  }


  protected boolean doKeyword(MarkerState ms, Segment line, int i, char c) {
    int i1 = i + 1;
    int len = i - ms.lastKeyword;

    boolean paren = Editor.checkParen(line.array, i, line.array.length);

    byte id = keywordColoring.lookup(line, ms.lastKeyword, len, paren);
    if (id != Token.NULL) {
      if (ms.lastKeyword != ms.lastOffset) {
        addToken(ms.lastKeyword - ms.lastOffset, Token.NULL);
      }
      addToken(len, id);
      ms.lastOffset = i;
    }
    ms.lastKeyword = i1;
    return false;
  }
}
