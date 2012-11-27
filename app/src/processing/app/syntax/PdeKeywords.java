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


/**
 * This class reads a keywords.txt file to get coloring put links to reference
 * locations for the set of keywords.
 */
public class PdeKeywords extends TokenMarker {
  private KeywordMap keywordColoring;

  private int lastOffset;
  private int lastKeyword;
  

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
    // text will be KEYWORD or LITERAL
    boolean isKey = (coloring.charAt(0) == 'K');
    // KEYWORD1 -> 0, KEYWORD2 -> 1, etc
    int num = coloring.charAt(coloring.length() - 1) - '1';
    byte id = (byte) ((isKey ? Token.KEYWORD1 : Token.LITERAL1) + num);
    // Making an assumption (..., you, me) that KEYWORD2 and KEYWORD3 
    // are the functions (at least that's what we're doing in P5 right now)
    keywordColoring.add(keyword, id, id == Token.KEYWORD2 || id == Token.KEYWORD3);
  }


  public byte markTokensImpl(byte token, Segment line, int lineIndex) {
    char[] array = line.array;
    int offset = line.offset;
    lastOffset = offset;
    lastKeyword = offset;
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
            i++;  // http://processing.org/bugs/bugzilla/609.html [jdf] 
          }
          break;
        default:
          backslash = false;
          if (!Character.isLetterOrDigit(c) && c != '_') {
            // if (i1 < mlength && array[i1]
//            boolean paren = false;
//            int stepper = i + 1;
//            while (stepper < mlength) {
//              if (array[stepper] == '(') {
//                paren = true;
//                break;
//              }
//              stepper++;
//            }
            doKeyword(line, i, c);
//            doKeyword(line, i, checkParen(array, i1, mlength));
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

    if (token == Token.NULL) {
      doKeyword(line, mlength, '\0');
    }

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
  
  
  private boolean checkParen(char[] array, int index, int stop) {
//    boolean paren = false;
//    int stepper = i + 1;
//    while (stepper < mlength) {
//      if (array[stepper] == '(') {
//        paren = true;
//        break;
//      }
//      stepper++;
//    }
    while (index < stop) {
//      if (array[index] == '(') {
//        return true;
//      } else if (!Character.isWhitespace(array[index])) {
//        return false;
//      }
      switch (array[index]) {
      case '(':
        return true;
        
      case ' ':
      case '\t':
      case '\n':
      case '\r':
        index++;
        break;
        
      default:
//        System.out.println("defaulting because " + array[index] + " " + PApplet.hex(array[index]));
        return false;
      }
    }
//    System.out.println("exiting " + new String(array, index, stop - index));
    return false;
  }


  private boolean doKeyword(Segment line, int i, char c) {
//    return doKeyword(line, i, false);
//  }
//  
//  
//  //private boolean doKeyword(Segment line, int i, char c) {
//  private boolean doKeyword(Segment line, int i, boolean paren) {
    int i1 = i + 1;
    int len = i - lastKeyword;
    
    boolean paren = checkParen(line.array, i, line.array.length);
//    String s = new String(line.array, lastKeyword, len);
//    if (s.equals("mousePressed")) {
//      System.out.println("found mousePressed" + (paren ? "()" : ""));
//      //new Exception().printStackTrace(System.out);
////      System.out.println("  " + i + " " + line.count + " " + 
////        //new String(line.array, line.offset + i, line.offset + line.count - i));
////        new String(line.array, i, line.array.length - i));
//    }

    byte id = keywordColoring.lookup(line, lastKeyword, len, paren);
    if (id != Token.NULL) {
      if (lastKeyword != lastOffset) {
        addToken(lastKeyword - lastOffset, Token.NULL);
      }
      if (paren && id == Token.LITERAL2) {
        id = Token.KEYWORD2;
      } else if (!paren && id == Token.KEYWORD2) {
        id = Token.LITERAL2;
      }
      addToken(len, id);
      lastOffset = i;
    }
    lastKeyword = i1;
    return false;
  }
}
