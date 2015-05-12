/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-15 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.pdex;

import java.util.ArrayList;


/**
 * Performs offset matching between PDE and Java code (one line of code only)
 * @author Manindra Moharana <me@mkmoharana.com>
 */
public class OffsetMatcher {
  public ArrayList<OffsetMatcher.OffsetPair> offsetMatch;
  String pdeCodeLine, javaCodeLine;
  boolean matchingNeeded = false;


  public OffsetMatcher(String pdeCode, String javaCode) {
    this.pdeCodeLine = pdeCode;
    this.javaCodeLine = javaCode;
    if(pdeCodeLine.trim().equals(javaCodeLine.trim())){ //TODO: trim() needed here?
      matchingNeeded = false;
      offsetMatch = new ArrayList<OffsetMatcher.OffsetPair>();
      //log("Offset Matching not needed");
    } else {
      matchingNeeded = true;
      minDistance();
    }
  }


  public int getPdeOffForJavaOff(int start, int length) {
//    log("PDE :" + pdeCodeLine + "\nJAVA:" + javaCodeLine);
//    log("getPdeOffForJavaOff() start:" + start + ", len " + length);
    if(!matchingNeeded) return start;
    int ans = getPdeOffForJavaOff(start);
    int end = getPdeOffForJavaOff(start + length - 1);
    if(ans == -1 || end == -1){
//      log("ans: " + ans + " end: " + end);
    } else {
//      log(start + " java start off, pde start off "
//          + ans);
//      log((start + length - 1) + " java end off, pde end off "
//          + end);
//      log("J: " + javaCodeLine.substring(start, start + length) + "\nP: "
//          + pdeCodeLine.substring(ans, end + 1));
    }
    return ans;
  }


  public int getJavaOffForPdeOff(int start, int length) {
    if(!matchingNeeded) return start;
    int ans = getJavaOffForPdeOff(start);
//    log(start + " pde start off, java start off "
//        + getJavaOffForPdeOff(start));
//    log((start + length - 1) + " pde end off, java end off "
//        + getJavaOffForPdeOff(start + length - 1));
    return ans;
  }


  public int getPdeOffForJavaOff(int javaOff) {
    if (!matchingNeeded)
      return javaOff;
    for (int i = offsetMatch.size() - 1; i >= 0; i--) {
      if (offsetMatch.get(i).javaOffset < javaOff) {
        continue;
      } else if (offsetMatch.get(i).javaOffset == javaOff) {
//        int j = i;

        // sometimes there are multiple repeated j offsets for a single pde offset
        // so go to the last one, with bound check
        while (i > 0 && offsetMatch.get(--i).javaOffset == javaOff) {
//          log("MP " + offsetMatch.get(i).javaOffset + " "
//              + offsetMatch.get(i).pdeOffset);
        }
        if (i + 1 < offsetMatch.size()) { // bounds check, see #2664
          int pdeOff = offsetMatch.get(++i).pdeOffset;
          while (i > 0 && offsetMatch.get(--i).pdeOffset == pdeOff) {
          }
        }
        int j = i + 1;
        if (j > -1 && j < offsetMatch.size())
          return offsetMatch.get(j).pdeOffset;
      }

    }
    return -1;
  }


  public int getJavaOffForPdeOff(int pdeOff) {
    if(!matchingNeeded) return pdeOff;
    for (int i = offsetMatch.size() - 1; i >= 0; i--) {
      if (offsetMatch.get(i).pdeOffset < pdeOff) {
        continue;
      } else if (offsetMatch.get(i).pdeOffset == pdeOff) {
//        int j = i;
        while (i > 0 && offsetMatch.get(--i).pdeOffset == pdeOff) {
//          log("MP " + offsetMatch.get(i).javaOffset + " "
//              + offsetMatch.get(i).pdeOffset);
        }
        if (i + 1 < offsetMatch.size()) { // bounds check, see #2664
          int javaOff = offsetMatch.get(++i).javaOffset;
          while (i > 0 && offsetMatch.get(--i).javaOffset == javaOff) {
          }
        }
        int j = i + 1;
        if (j > -1 && j < offsetMatch.size())
          return offsetMatch.get(j).javaOffset;
      }

    }
    return -1;
  }


  /**
   * Finds 'distance' between two Strings.
   * See Edit Distance Problem
   * https://secweb.cs.odu.edu/~zeil/cs361/web/website/Lectures/styles/pages/editdistance.html
   * http://www.stanford.edu/class/cs124/lec/med.pdf
   */
  private int minDistance() {

//    word1 = reverse(word1);
//    word2 = reverse(word2);
    int len1 = pdeCodeLine.length();
    int len2 = javaCodeLine.length();
    // log(pdeCodeLine + " len: " + len1);
    // log(javaCodeLine + " len: " + len2);
    // len1+1, len2+1, because finally return dp[len1][len2]
    int[][] dp = new int[len1 + 1][len2 + 1];

    for (int i = 0; i <= len1; i++) {
      dp[i][0] = i;
    }

    for (int j = 0; j <= len2; j++) {
      dp[0][j] = j;
    }

    //iterate though, and check last char
    for (int i = 0; i < len1; i++) {
      char c1 = pdeCodeLine.charAt(i);
      for (int j = 0; j < len2; j++) {
        char c2 = javaCodeLine.charAt(j);
        //System.out.print(c1 + "<->" + c2);
        //if last two chars equal
        if (c1 == c2) {
          //update dp value for +1 length
          dp[i + 1][j + 1] = dp[i][j];
//          log();
        } else {
          int replace = dp[i][j] + 1;
          int insert = dp[i][j + 1] + 1;
          int delete = dp[i + 1][j] + 1;
//          if (replace < delete) {
//            log(" --- D");
//          } else
//            log(" --- R");
          int min = replace > insert ? insert : replace;
          min = delete > min ? min : delete;
          dp[i + 1][j + 1] = min;
        }
      }
    }

    ArrayList<OffsetPair> alist = new ArrayList<OffsetMatcher.OffsetPair>();
    offsetMatch = alist;
    minDistInGrid(dp, len1, len2, 0, 0, pdeCodeLine.toCharArray(),
                  javaCodeLine.toCharArray(), alist);
    return dp[len1][len2];
  }


  private void minDistInGrid(int g[][], int i, int j, int fi, int fj,
                             char s1[], char s2[], ArrayList<OffsetPair> set) {
//    if(i < s1.length)System.out.print(s1[i] + " <->");
//    if(j < s2.length)System.out.print(s2[j]);
    if (i < s1.length && j < s2.length) {
//      pdeCodeMap[k] = i;
//      javaCodeMap[k] = j;
      //System.out.print(s1[i] + " " + i + " <-> " + j + " " + s2[j]);
      set.add(new OffsetPair(i, j));
//      if (s1[i] != s2[j])
//        System.out.println("--");
    }
    //System.out.println();
    if (i == fi && j == fj) {
      //System.out.println("Reached end.");
    } else {
      int a = Integer.MAX_VALUE, b = a, c = a;
      if (i > 0)
        a = g[i - 1][j];
      if (j > 0)
        b = g[i][j - 1];
      if (i > 0 && j > 0)
        c = g[i - 1][j - 1];
      int mini = Math.min(a, Math.min(b, c));
      if (mini == a) {
        //System.out.println(s1[i + 1] + " " + s2[j]);
        minDistInGrid(g, i - 1, j, fi, fj, s1, s2, set);
      } else if (mini == b) {
        //System.out.println(s1[i] + " " + s2[j + 1]);
        minDistInGrid(g, i, j - 1, fi, fj, s1, s2, set);
      } else if (mini == c) {
        //System.out.println(s1[i + 1] + " " + s2[j + 1]);
        minDistInGrid(g, i - 1, j - 1, fi, fj, s1, s2, set);
      }
    }
  }


  private class OffsetPair {
    public final int pdeOffset, javaOffset;

    public OffsetPair(int pde, int java) {
      pdeOffset = pde;
      javaOffset = java;
    }
  }


  /*
  public static void main(String[] args) {
//    minDistance("c = #qwerty;", "c = 0xffqwerty;");
    OffsetMatcher a;

//    a = new OffsetMatcher("int a = int(can); int ball;",
//                          "int a = PApplet.parseInt(can); int ball;");
//    a.getPdeOffForJavaOff(25, 3);
//    a.getJavaOffForPdeOff(12, 3);
//    minDistance("static void main(){;", "public static void main(){;");
//      minDistance("#bb00aa", "0xffbb00aa");
//    a = new OffsetMatcher("void test(ArrayList<Boid> boids){",
//    "public void test(ArrayList<Boid> boids){");
//    a.getJavaOffForPdeOff(20,4);
    a = new OffsetMatcher("}", "\n");
    a.getPdeOffForJavaOff(0,1);
    a = new OffsetMatcher("color abc = #qwerty;", "int abc = 0xffqwerty;");
    a.getPdeOffForJavaOff(4, 3);
//    a.getJavaOffForPdeOff(6, 3);
//    distance("c = #bb00aa;", "c = 0xffbb00aa;");
  }
  */
}

