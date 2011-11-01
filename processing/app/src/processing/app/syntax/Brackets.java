package processing.app.syntax;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Jonathan Feinberg &lt;jdf@pobox.com&gt;
 *
 */
public class Brackets {
  private volatile List<Integer> offsets = null;

  public void invalidate() {
    offsets = null;
  }

  public int findMatchingBracket(final String text, final int pos) {
    if (pos < 0 || pos > text.length())
      return -1;
    
    final char alpha = text.charAt(pos);
    final char beta;
    final int direction;
    switch (alpha) {
    case '(':
      beta = ')';
      direction = 1;
      break;
    case ')':
      beta = '(';
      direction = -1;
      break;
    case '[':
      beta = ']';
      direction = 1;
      break;
    case ']':
      beta = '[';
      direction = -1;
      break;
    case '{':
      beta = '}';
      direction = 1;
      break;
    case '}':
      beta = '{';
      direction = -1;
      break;
    default:
      return -1;
    }

    if (offsets == null)
      parse(text);
    
    // find this bracket
    int p;
    for (p = 0; p < offsets.size(); p++)
      if (offsets.get(p) == pos)
        break;
    if (p == offsets.size()) {
      return -1;
    }

    int depth = 1;
    for (p += direction; p >= 0 && p < offsets.size(); p += direction) {
      final int offset = offsets.get(p);
      final char c = text.charAt(offset);
      if (c == alpha)
        depth++;
      else if (c == beta)
        depth--;
      if (depth == 0)
        return offset;
    }
    return -1;
  }

  int pos;

  private void parse(final String text) {
    offsets = new ArrayList<Integer>();
    final int len = text.length();
    for (pos = 0; pos < len; pos++) {
      final char c = text.charAt(pos);
      if (c == '/' && (pos < len - 1)) {
        final char d = text.charAt(++pos);
        if (d == '/') {
          readComment(text);
        } else if (d == '*') {
          readMLComment(text);
        }
      } else if (c == '"' || c == '\'') {
        readString(text, c);
      } else if (c == '{' || c == '[' || c == '(' || c == '}' || c == ']'
          || c == ')') {
        offsets.add(pos);
      }
    }
  }

  private void readString(final String text, final char q) {
    final int len = text.length();
    for (pos++; pos < len; pos++) {
      final char c = text.charAt(pos);
      if (c == q) {
        return;
      }
      if (c == '\\') {
        pos++;
      }
    }
  }

  private void readComment(final String text) {
    final int len = text.length();
    for (pos++; pos < len; pos++)
      if (text.charAt(pos) == '\n') {
        return;
      }
  }

  private void readMLComment(final String text) {
    final int len = text.length();
    for (pos++; pos < len; pos++) {
      final char c = text.charAt(pos);
      if (c == '*' && (pos < len - 1)) {
        pos++;
        final char d = text.charAt(pos);
        if (d == '/') {
          return;
        }
      }
    }
  }
}
