/*
 * KeywordMap.java - Fast keyword->id map
 * Copyright (C) 1998, 1999 Slava Pestov
 * Copyright (C) 1999 Mike Dillon
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package processing.app.syntax;

import javax.swing.text.Segment;

/**
 * A <code>KeywordMap</code> is similar to a hashtable in that it maps keys
 * to values. However, the `keys' are Swing segments. This allows lookups of
 * text substrings without the overhead of creating a new string object.
 * <p>
 * This class is used by <code>CTokenMarker</code> to map keywords to ids.
 *
 * @author Slava Pestov, Mike Dillon
 * @version $Id$
 */
public class KeywordMap {
//  private Keyword[] map;
//  protected int mapLength;

  private boolean ignoreCase;
  private Keyword[] literalMap;
  private Keyword[] parenMap;

  // A value of 52 will give good performance for most maps.
  static private int MAP_LENGTH = 52;
  
  /**
   * Creates a new <code>KeywordMap</code>.
   * @param ignoreCase True if keys are case insensitive
   */
  public KeywordMap(boolean ignoreCase) {
//    this(ignoreCase, 52);
    this.ignoreCase = ignoreCase;
    literalMap = new Keyword[MAP_LENGTH];
    parenMap = new Keyword[MAP_LENGTH];
  }


//  /**
//   * Creates a new <code>KeywordMap</code>.
//   * @param ignoreCase True if the keys are case insensitive
//   * @param mapLength The number of `buckets' to create.
//   * A value of 52 will give good performance for most maps.
//   */
//  public KeywordMap(boolean ignoreCase, int mapLength) {
//    this.mapLength = mapLength;
//    this.ignoreCase = ignoreCase;
//    map = new Keyword[mapLength];
//  }


  /**
   * Looks up a key.
   * @param text The text segment
   * @param offset The offset of the substring within the text segment
   * @param length The length of the substring
   */
  public byte lookup(Segment text, int offset, int length, boolean paren) {
    if (length == 0) {
      return Token.NULL;
    }
    int key = getSegmentMapKey(text, offset, length);
    Keyword k = paren ? parenMap[key] : literalMap[key];
    while (k != null) {
//      if (length != k.keyword.length) {
//        k = k.next;
//        continue;
//      }
      if (length == k.keyword.length) {
        if (SyntaxUtilities.regionMatches(ignoreCase,text,offset, k.keyword)) {
          return k.id;
        }
      }
      k = k.next;
    }
    return Token.NULL;
  }


  /**
   * Adds a key-value mapping.
   * @param keyword The key
   * @param id The value
   */
  public void add(String keyword, byte id, boolean paren) {
    int key = getStringMapKey(keyword);
    Keyword[] map = paren ? parenMap : literalMap;
    map[key] = new Keyword(keyword.toCharArray(), id, map[key]);
  }

  
  /**
   * Returns true if the keyword map is set to be case insensitive,
   * false otherwise.
   */
  public boolean getIgnoreCase() {
    return ignoreCase;
  }

  
  /**
   * Sets if the keyword map should be case insensitive.
   * @param ignoreCase True if the keyword map should be case
   * insensitive, false otherwise
   */
  public void setIgnoreCase(boolean ignoreCase) {
    this.ignoreCase = ignoreCase;
  }

  
  protected int getStringMapKey(String s) {
    return (Character.toUpperCase(s.charAt(0)) +
      Character.toUpperCase(s.charAt(s.length()-1)))
      % MAP_LENGTH;
  }

  
  protected int getSegmentMapKey(Segment s, int off, int len) {
    return (Character.toUpperCase(s.array[off]) +
      Character.toUpperCase(s.array[off + len - 1]))
      % MAP_LENGTH;
  }

  
  // private members
  private static class Keyword {
    public final char[] keyword;
    public final byte id;
    public final Keyword next;

    public Keyword(char[] keyword, byte id, Keyword next) {
      this.keyword = keyword;
      this.id = id;
      this.next = next;
    }
  }
}
