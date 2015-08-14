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

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import processing.app.Base;
import processing.app.Messages;


/**
 * Wrapper class for ASTNode objects
 * @author Manindra Moharana <me@mkmoharana.com>
 *
 */
public class ASTNodeWrapper {
  private ASTNode Node;
  private String label;
  private int lineNumber;


  /*
   * TODO: Every ASTNode object in ASTGenerator.codetree is stored as a
   * ASTNodeWrapper instance. So how resource heavy would it be to store a
   * pointer to ECS in every instance of ASTNodeWrapper? Currently I will rather
   * pass an ECS pointer in the argument when I need to access a method which
   * requires a method defined in ECS, i.e, only on demand.
   * Bad design choice for ECS methods? IDK, yet.
   */

  public ASTNodeWrapper(ASTNode node) {
    if (node == null){
      return;
    }
    this.Node = node;
    label = getNodeAsString(node);
    if (label == null)
      label = node.toString();
    lineNumber = getLineNumber(node);
    label += " | Line " + lineNumber;
    //apiLevel = 0;
  }

  public ASTNodeWrapper(ASTNode node, String label){
    if (node == null){
      return;
    }
    this.Node = node;
    if(label != null)
      this.label = label;
    else{
      label = getNodeAsString(node);
      if (label == null)
        label = node.toString();

      label += " | Line " + lineNumber;
    }
    lineNumber = getLineNumber(node);
  }

  /**
   * For this node, finds various offsets (java code).
   * Note that line start offset for this node is int[2] - int[1]
   * @return int[]{line number, line number start offset, node start offset,
   *         node length}
   */
  public int[] getJavaCodeOffsets(ErrorCheckerService ecs) {
    int nodeOffset = Node.getStartPosition(), nodeLength = Node
        .getLength();
    Messages.log("0.nodeOffset " + nodeOffset);
    ASTNode thisNode = Node;
    while (thisNode.getParent() != null) {
      if (getLineNumber(thisNode.getParent()) == lineNumber) {
        thisNode = thisNode.getParent();
      } else {
        break;
      }
    }
    /*
     *  There's an edge case here - multiple statements in a single line.
     *  After identifying the statement with the line number, I'll have to
     *  look at previous tree nodes in the same level for same line number.
     *  The correct line start offset would be the line start offset of
     *  the first node with this line number.
     *
     *  Using linear search for now. P.S: Eclipse AST iterators are messy.
     *  TODO: binary search might improve speed by 0.001%?
     */

    int altStartPos = thisNode.getStartPosition();
    Messages.log("1.Altspos " + altStartPos);
    thisNode = thisNode.getParent();
    Javadoc jd = null;

    /*
     * There's another case that needs to be handled. If a TD, MD or FD
     * contains javadoc comments(multi or single line) the starting position
     * of the javadoc is treated as the beginning of the declaration by the AST parser.
     * But that's clearly not what we need. The true decl begins after the javadoc ends.
     * So this offset needs to be found carefully and stored in altStartPos
     *
     */
    if (thisNode instanceof TypeDeclaration) {
      jd = ((TypeDeclaration) thisNode).getJavadoc();
      altStartPos = getJavadocOffset((TypeDeclaration) thisNode);
      Messages.log("Has t jdoc " + ((TypeDeclaration) thisNode).getJavadoc());
    } else if (thisNode instanceof MethodDeclaration) {
      altStartPos = getJavadocOffset((MethodDeclaration) thisNode);
      jd = ((MethodDeclaration) thisNode).getJavadoc();
      Messages.log("Has m jdoc " + jd);
    } else if (thisNode instanceof FieldDeclaration) {
      FieldDeclaration fd = ((FieldDeclaration) thisNode);
      jd = fd.getJavadoc();
      Messages.log("Has f jdoc " + fd.getJavadoc());
      altStartPos = getJavadocOffset(fd);
      //nodeOffset = ((VariableDeclarationFragment)(fd.fragments().get(0))).getName().getStartPosition();
    }

    if (jd == null) {
      Messages.log("Visiting children of node " + getNodeAsString(thisNode));
      @SuppressWarnings("unchecked")
      Iterator<StructuralPropertyDescriptor> it =
          thisNode.structuralPropertiesForType().iterator();
      boolean flag = true;
      while (it.hasNext()) {
        StructuralPropertyDescriptor prop = it.next();
        if (prop.isChildListProperty()) {
          @SuppressWarnings("unchecked")
          List<ASTNode> nodelist = (List<ASTNode>)
            thisNode.getStructuralProperty(prop);
          Messages.log("prop " + prop);
          for (ASTNode cnode : nodelist) {
            Messages.log("Visiting node " + getNodeAsString(cnode));
            if (getLineNumber(cnode) == lineNumber) {
              if (flag) {
                altStartPos = cnode.getStartPosition();
                // log("multi...");

                flag = false;
              } else {
                if (cnode == Node) {
                  // loop only till the current node.
                  break;
                }
                // We've located the first node in the line.
                // Now normalize offsets till Node
                //altStartPos += normalizeOffsets(cnode);

              }

            }
          }
        }
      }
      Messages.log("Altspos " + altStartPos);
    }

    int pdeoffsets[] = getPDECodeOffsets(ecs);
    String pdeCode = ecs.getPdeCodeAtLine(pdeoffsets[0],pdeoffsets[1] - 1).trim();
    int vals[] = createOffsetMapping(ecs, pdeCode,nodeOffset - altStartPos,nodeLength);
    if (vals != null)
      return new int[] {
        lineNumber, nodeOffset + vals[0] - altStartPos, vals[1] };
    else {// no offset mapping needed
      Messages.log("joff[1] = " + (nodeOffset - altStartPos));
      return new int[] { lineNumber, nodeOffset - altStartPos, nodeLength };
    }
  }

  /**
   * When FD has javadoc attached, the beginning of FD is marked as the
   * start of the javadoc. This kind of screws things when trying to locate
   * the exact name of the FD. So, offset compensations...
   *
   * @param fd
   * @return
   */
  private int getJavadocOffset(FieldDeclaration fd){
    @SuppressWarnings("unchecked")
    List<ASTNode> list = fd.modifiers();
    SimpleName sn = (SimpleName) getNode();

    Type tp = fd.getType();
    int lineNum = getLineNumber(sn);
    Messages.log("SN "+sn + ", " + lineNum);
    for (ASTNode astNode : list) {
      if(getLineNumber(astNode) == lineNum) {
        Messages.log("first node in that line " + astNode);
        Messages.log("diff " + (sn.getStartPosition() - astNode.getStartPosition()));
        return (astNode.getStartPosition());
      }
    }
    if(getLineNumber(fd.getType()) == lineNum) {
      Messages.log("first node in that line " + tp);
      Messages.log("diff " + (sn.getStartPosition() - tp.getStartPosition()));
      return (tp.getStartPosition());
    }
    return 0;
  }

  /**
   * When MD has javadoc attached, the beginning of FD is marked as the
   * start of the javadoc. This kind of screws things when trying to locate
   * the exact name of the MD. So, offset compensations...
   *
   * @param md
   * @return
   */
  private int getJavadocOffset(MethodDeclaration md) {
    @SuppressWarnings("unchecked")
    List<ASTNode> list = md.modifiers();
    SimpleName sn = (SimpleName) getNode();
    int lineNum = getLineNumber(sn);
    Messages.log("SN " + sn + ", " + lineNum);

    for (ASTNode astNode : list) {
      if (getLineNumber(astNode) == lineNum) {
        Messages.log("first node in that line " + astNode);
        Messages.log("diff " + (sn.getStartPosition() - astNode.getStartPosition()));
        return (astNode.getStartPosition());
      }
    }

    if (!md.isConstructor()) {
      Type tp = md.getReturnType2();
      if (getLineNumber(tp) == lineNum) {
        Messages.log("first node in that line " + tp);
        Messages.log("diff " + (sn.getStartPosition() - tp.getStartPosition()));
        return (tp.getStartPosition());
      }
    }

    return 0;
  }

  /**
   * When TD has javadoc attached, the beginning of FD is marked as the
   * start of the javadoc. This kind of screws things when trying to locate
   * the exact name of the TD. So, offset compensations...
   *
   * @param td
   * @return
   */
  private int getJavadocOffset(TypeDeclaration td){
    // TODO: This isn't perfect yet. Class \n \n \n className still breaks it.. :'(
    @SuppressWarnings("unchecked")
    List<ASTNode> list = td.modifiers();
    SimpleName sn = (SimpleName) getNode();

    int lineNum = getLineNumber(sn);
    Messages.log("SN "+sn + ", " + lineNum);
    for (ASTNode astNode : list) {
      if (getLineNumber(astNode) == lineNum) {
        Messages.log("first node in that line " + astNode);
        Messages.log("diff " + (sn.getStartPosition() - astNode.getStartPosition()));
        return (astNode.getStartPosition());
      }
    }

    if (td.getJavadoc() != null){
      Messages.log("diff "
          + (td.getJavadoc().getStartPosition() + td.getJavadoc().getLength() + 1));
      return (td.getJavadoc().getStartPosition() + td.getJavadoc().getLength() + 1);
    }
    Messages.log("getJavadocOffset(TypeDeclaration td) "+sn + ", found nothing. Meh.");
    return 0;
  }

  /**
   * Finds the difference in pde and java code offsets
   * @param source
   * @param inpOffset
   * @param nodeLen
   * @return int[0] - difference in start offset, int[1] - node length
   */
  private int[] createOffsetMapping(ErrorCheckerService ecs, String source, int inpOffset, int nodeLen) {

    int ret[][] = getOffsetMapping(ecs, source);
    if(ret == null){
      // no offset mapping needed
      return null;
    }
    int javaCodeMap[] = ret[0];
    int pdeCodeMap[] = ret[1];
    int pi = 1, pj = 1;
    pj = 0;
    pi = 0;
    int count = 1;
    // first find the java code index
    pj = inpOffset;

    int startIndex = javaCodeMap[pj];

    // find beginning
    while (pdeCodeMap[pi] != startIndex && pi < pdeCodeMap.length)
      pi++;
    int startoffDif = pi - pj;
    int stopindex = javaCodeMap[pj + nodeLen - 1];
    Messages.log(startIndex + "SI,St" + stopindex + "sod " + startoffDif);

    // count till stopindex
    while (pdeCodeMap[pi] < stopindex && pi < pdeCodeMap.length) {
      pi++;
      count++;
    }

//    log("PDE maps from " + pdeeCodeMap[pi]);

    Messages.log("pde len " + count);
    return new int[] { startoffDif, count };
  }

  /**
   * Generates offset mapping between java and pde code
   *
   * @param source
   * @return int[0] - java code offsets, int[1] = pde code offsets
   */
  public int[][] getOffsetMapping(ErrorCheckerService ecs, String source){

    /*
     * This is some tricky shiz. So detailed explanation follows:
     *
     * The main issue here is that pde enhancements like color vars, # literals
     * and int() type casting deviate from standard java. But I need to exact
     * index matching for pde and java versions of snippets.For ex:
     * "color col = #ffaadd;" <-PDE version
     * "int col = 0xffffaadd;" <-Converted to Java
     *
     * For exact index mapping, I need to know at which indices either is
     * deviating from the other and by what amount. Turns out, it isn't quite
     * easy.(1) First I take the pde version of the code as an argument(pde
     * version fetched from the editor directly). I then find all instances
     * which need to be converted to pure java, marking those indices and the
     * index correction needed. (2) Now all java conversions are applied after
     * marking the offsets. This ensures that the index order isn't disturbed by
     * one at a time conversions as done in preprocessCode() in ECS. Took me
     * sometime to figure out this was a bug. (3) Next I create a table(two
     * separate arrays) which allows me to look it up for matching any index
     * between pde or java version of the snippet. This also lets me find out
     * any difference in length between both versions.
     *
     * Keep in mind though, dark magic was involved in creating the final lookup
     * table.
     *
     * TODO: This is a work in progress. There may be more bugs here in hiding.
     */

    Messages.log("Src:" + source);
    // Instead of converting pde into java, how can I simply extract the same source
    // from the java code? Think. TODO
    String sourceAlt = new String(source);
    String sourceJava = ecs.astGenerator.getJavaSourceCodeLine(lineNumber);
    TreeMap<Integer, Integer> offsetmap = new TreeMap<Integer, Integer>();

    if(sourceJava.trim().startsWith("public") && !source.startsWith("public")){
      offsetmap.put(0,6);
      //TODO: This is a temp fix. You GOTTA rewrite offset matching
    }
    // Find all #[web color]
    // Should be 6 digits only.
    final String webColorRegexp = "#{1}[A-F|a-f|0-9]{6}\\W";
    Pattern webPattern = Pattern.compile(webColorRegexp);
    Matcher webMatcher = webPattern.matcher(sourceAlt);
    while (webMatcher.find()) {
      // log("Found at: " + webMatcher.start());
      // log("-> " + found);
      offsetmap.put(webMatcher.end() - 1, 3);
    }

    // Find all color data types
    final String colorTypeRegex = "color(?![a-zA-Z0-9_])(?=\\[*)(?!(\\s*\\())";
    Pattern colorPattern = Pattern.compile(colorTypeRegex);
    Matcher colorMatcher = colorPattern.matcher(sourceAlt);
    while (colorMatcher.find()) {
//      System.out.print("Start index: " + colorMatcher.start());
//      log(" End index: " + colorMatcher.end() + " ");
//      log("-->" + colorMatcher.group() + "<--");
      offsetmap.put(colorMatcher.end() - 1, -2);
    }

    // Find all int(), char()
    String dataTypeFunc[] = { "int", "char", "float", "boolean", "byte" };

    for (String dataType : dataTypeFunc) {
      String dataTypeRegexp = "\\b" + dataType + "\\s*\\(";
      Pattern pattern = Pattern.compile(dataTypeRegexp);
      Matcher matcher = pattern.matcher(sourceAlt);

      while (matcher.find()) {
//        System.out.print("Start index: " + matcher.start());
//        log(" End index: " + matcher.end() + " ");
//        log("-->" + matcher.group() + "<--");
        offsetmap.put(matcher.end() - 1, ("PApplet.parse").length());
      }
      matcher.reset();
      sourceAlt = matcher.replaceAll("PApplet.parse"
          + Character.toUpperCase(dataType.charAt(0)) + dataType.substring(1)
          + "(");

    }
    if(offsetmap.isEmpty()){
      Messages.log("No offset matching needed.");
      return null;
    }
    // replace with 0xff[webcolor] and others
    webMatcher = webPattern.matcher(sourceAlt);
    while (webMatcher.find()) {
      // log("Found at: " + webMatcher.start());
      String found = sourceAlt.substring(webMatcher.start(), webMatcher.end());
      // log("-> " + found);
      sourceAlt = webMatcher.replaceFirst("0xff" + found.substring(1));
      webMatcher = webPattern.matcher(sourceAlt);
    }

    colorMatcher = colorPattern.matcher(sourceAlt);
    sourceAlt = colorMatcher.replaceAll("int");

    Messages.log("From direct source: ");
//    sourceAlt = sourceJava;
    Messages.log(sourceAlt);


    // Create code map. Beware! Dark magic ahead.
    int javaCodeMap[] = new int[source.length() * 2];
    int pdeCodeMap[] = new int[source.length() * 2];
    int pi = 1, pj = 1;
    int keySum = 0;
    for (Integer key : offsetmap.keySet()) {
      for (; pi < key +keySum; pi++) {
        javaCodeMap[pi] = javaCodeMap[pi - 1] + 1;
      }
      for (; pj < key; pj++) {
        pdeCodeMap[pj] = pdeCodeMap[pj - 1] + 1;
      }

      Messages.log(key + ":" + offsetmap.get(key));

      int kval = offsetmap.get(key);
      if (kval > 0) {
        // repeat java offsets
        pi--;
        pj--;
        for (int i = 0; i < kval; i++, pi++, pj++) {
          if (pi > 1 && pj > 1) {
            javaCodeMap[pi] = javaCodeMap[pi - 1];
            pdeCodeMap[pj] = pdeCodeMap[pj - 1] + 1;
          }
        }
      } else {
        // repeat pde offsets
        pi--;
        pj--;
        for (int i = 0; i < -kval; i++, pi++, pj++) {
          if (pi > 1 && pj > 1) {
            javaCodeMap[pi] = javaCodeMap[pi - 1] + 1;
            pdeCodeMap[pj] = pdeCodeMap[pj - 1];
          }
        }
      }

      // after each adjustment, the key values need to keep
      // up with changed offset
      keySum += kval;
    }

    javaCodeMap[pi] = javaCodeMap[pi - 1] + 1;
    pdeCodeMap[pj] = pdeCodeMap[pj - 1] + 1;

    while (pi < sourceAlt.length()) {
      javaCodeMap[pi] = javaCodeMap[pi - 1] + 1;
      pi++;
    }
    while (pj < source.length()) {
      pdeCodeMap[pj] = pdeCodeMap[pj - 1] + 1;
      pj++;
    }

    if (Base.DEBUG) {
      // debug o/p
      for (int i = 0; i < pdeCodeMap.length; i++) {
        if (pdeCodeMap[i] > 0 || javaCodeMap[i] > 0 || i == 0) {
          if (i < source.length())
            System.out.print(source.charAt(i));
          System.out.print(pdeCodeMap[i] + " - " + javaCodeMap[i]);
          if (i < sourceAlt.length())
            System.out.print(sourceAlt.charAt(i));
          System.out.print(" <-[" + i + "]");
          System.out.println();
        }
      }
      System.out.println();
    }
    return new int[][] { javaCodeMap, pdeCodeMap };
  }


  /**
   * Highlight the ASTNode in the editor, if it's of type
   * SimpleName
   * @param astGenerator
   * @return - true if highlighting was successful
   */
  public boolean highlightNode(ASTGenerator astGenerator){
    if (!(Node instanceof SimpleName)) {
      return false;
    }
    SimpleName nodeName = (SimpleName) Node;
    try {
      //TODO: Redundant code. See ASTGenerator.getJavaSourceCodeline()
      int javaLineNumber = getLineNumber(nodeName);
      int pdeOffs[] = astGenerator.errorCheckerService
          .calculateTabIndexAndLineNumber(javaLineNumber);
      PlainDocument javaSource = new PlainDocument();
      javaSource.insertString(0, astGenerator.errorCheckerService.sourceCode, null);
      Element lineElement = javaSource.getDefaultRootElement()
          .getElement(javaLineNumber-1);
      if(lineElement == null) {
        Messages.log(lineNumber + " line element null while highlighting " + nodeName);
        return false;
      }

      String javaLine = javaSource.getText(lineElement.getStartOffset(),
                                           lineElement.getEndOffset()
                                               - lineElement.getStartOffset());
      astGenerator.editor.getSketch().setCurrentCode(pdeOffs[0]);
      String pdeLine = astGenerator.editor.getLineText(pdeOffs[1]);
      String lookingFor = nodeName.toString();
      Messages.log(lookingFor + ", " + nodeName.getStartPosition());
      Messages.log(javaLineNumber +" JL " + javaLine + " LSO " + lineElement.getStartOffset() + ","
          + lineElement.getEndOffset());
      Messages.log(pdeOffs[1] + " PL " + pdeLine);
      if (!javaLine.contains(lookingFor) || !pdeLine.contains(lookingFor)) {
        Messages.loge("Logical error in highLightNode(). Please file a bug report.");
        return false;
      }

      OffsetMatcher ofm = new OffsetMatcher(pdeLine, javaLine);
      int highlightStart = ofm.getPdeOffForJavaOff(nodeName.getStartPosition()
                                  - lineElement.getStartOffset(),
                              nodeName.getLength());
      if (highlightStart == -1) {
        Messages.loge("Logical error in highLightNode() during offset matching. " +
        		"Please file a bug report.");
        return false;
      }
      int lso = astGenerator.editor.getTextArea().getLineStartOffset(pdeOffs[1]);
      highlightStart += lso;
      astGenerator.editor.setSelection(highlightStart, highlightStart
          + nodeName.getLength());
      /*
      // First find the name in the java line, and marks its index
      Pattern toFind = Pattern.compile("\\b" + nodeName.toString() + "\\b");
      Matcher matcher = toFind.matcher(javaLine);
      int count = 0, index = 0;
      int lsto = lineElement.getStartOffset();
      while(matcher.find()){
        count++;
        //log(matcher.start() + lsto);
        if(lsto + matcher.start() == nodeName.getStartPosition())
          break;
      }
      log("count=" + count);
      index = 0;
      // find the same name in the pde line by its index and get its offsets
      matcher = toFind.matcher(pdeLine);
      while(matcher.find()){
        count--;
        if(count == 0){
          log("Found on pde line lso: " + matcher.start());
          index = matcher.end();
          break;
        }
      }
      log("pde lso " + (index - lookingFor.length()));

      int lso = astGenerator.editor.ta.getLineStartOffset(pdeOffs[1]);
      astGenerator.editor.setSelection(lso + index - lookingFor.length(), lso
          + index);
      */
      return true;

    } catch (BadLocationException e) {
      Messages.loge("BLE in highLightNode() for " + nodeName);
      e.printStackTrace();
    }
    return false;
  }

  /**
   * Gets offset mapping between java and pde code
   * int[0][x] stores the java code offset and
   * int[1][x] is the corresponding offset in pde code
   * @param ecs
   * @return int[0] - java code offset, int[1] - pde code offset
   */
  public int[][] getOffsetMapping(ErrorCheckerService ecs){
    int pdeoffsets[] = getPDECodeOffsets(ecs);
    String pdeCode = ecs.getPdeCodeAtLine(pdeoffsets[0],pdeoffsets[1] - 1).trim();
    return getOffsetMapping(ecs, pdeCode);
  }

  /**
   *
   * @param ecs
   *          - ErrorCheckerService instance
   * @return int[0] - tab number, int[1] - line number in the int[0] tab, int[2]
   *         - line start offset, int[3] - offset from line start int[2] and
   *         int[3] are on TODO
   */
  public int[] getPDECodeOffsets(ErrorCheckerService ecs) {
    return ecs.JavaToPdeOffsets(lineNumber + 1, Node.getStartPosition());
  }

  public int getPDECodeOffsetForSN(ASTGenerator astGen){
    if (Node instanceof SimpleName) {
      Element lineElement = astGen.getJavaSourceCodeElement(lineNumber);
      Messages.log("Line element off " + lineElement.getStartOffset());
      OffsetMatcher ofm = new OffsetMatcher(astGen.getPDESourceCodeLine(lineNumber),
                                            astGen.getJavaSourceCodeLine(lineNumber));
      //log("");
      int pdeOffset = ofm.getPdeOffForJavaOff(Node.getStartPosition()
          - lineElement.getStartOffset(), Node.toString().length());
      return pdeOffset;
    }
    return -1;
  }

  public String toString() {
    return label;
  }

  public ASTNode getNode() {
    return Node;
  }

  public String getLabel() {
    return label;
  }

  public int getNodeType() {
    return Node.getNodeType();
  }

  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Applies pde enhancements to code.
   * TODO: Code reuse happening here. :\
   * @param source
   * @return
   */
  public static String getJavaCode(String source){
    Messages.log("Src:" + source);
    String sourceAlt = new String(source);

    // Find all #[web color]
    // Should be 6 digits only.
    final String webColorRegexp = "#{1}[A-F|a-f|0-9]{6}\\W";
    Pattern webPattern = Pattern.compile(webColorRegexp);
    Matcher webMatcher = webPattern.matcher(sourceAlt);
    while (webMatcher.find()) {
      // log("Found at: " + webMatcher.start());
      // log("-> " + found);
    }

    // Find all color data types
    final String colorTypeRegex = "color(?![a-zA-Z0-9_])(?=\\[*)(?!(\\s*\\())";
    Pattern colorPattern = Pattern.compile(colorTypeRegex);
    Matcher colorMatcher = colorPattern.matcher(sourceAlt);
    while (colorMatcher.find()) {
//      System.out.print("Start index: " + colorMatcher.start());
//      log(" End index: " + colorMatcher.end() + " ");
//      log("-->" + colorMatcher.group() + "<--");
    }

    // Find all int(), char()
    String dataTypeFunc[] = { "int", "char", "float", "boolean", "byte" };

    for (String dataType : dataTypeFunc) {
      String dataTypeRegexp = "\\b" + dataType + "\\s*\\(";
      Pattern pattern = Pattern.compile(dataTypeRegexp);
      Matcher matcher = pattern.matcher(sourceAlt);

      while (matcher.find()) {
//        System.out.print("Start index: " + matcher.start());
//        log(" End index: " + matcher.end() + " ");
//        log("-->" + matcher.group() + "<--");
      }
      matcher.reset();
      sourceAlt = matcher.replaceAll("PApplet.parse"
          + Character.toUpperCase(dataType.charAt(0)) + dataType.substring(1)
          + "(");

    }
    // replace with 0xff[webcolor] and others
    webMatcher = webPattern.matcher(sourceAlt);
    while (webMatcher.find()) {
      // log("Found at: " + webMatcher.start());
      String found = sourceAlt.substring(webMatcher.start(), webMatcher.end());
      // log("-> " + found);
      sourceAlt = webMatcher.replaceFirst("0xff" + found.substring(1));
      webMatcher = webPattern.matcher(sourceAlt);
    }

    colorMatcher = colorPattern.matcher(sourceAlt);
    sourceAlt = colorMatcher.replaceAll("int");

    Messages.log("Converted:"+sourceAlt);
    return sourceAlt;
  }

  private static int getLineNumber(ASTNode node) {
    return ((CompilationUnit) node.getRoot()).getLineNumber(node
        .getStartPosition());
  }

  /*private static int getLineNumber2(ASTNode thisNode) {
    int jdocOffset = 0; Javadoc jd = null;
    if(thisNode instanceof TypeDeclaration){
       jd = ((TypeDeclaration)thisNode).getJavadoc();
      log("Has t jdoc " + ((TypeDeclaration)thisNode).getJavadoc());
    } else if(thisNode instanceof MethodDeclaration){
      jd = ((MethodDeclaration)thisNode).getJavadoc();
      log("Has m jdoc " + jd);
    } else if(thisNode instanceof FieldDeclaration){
      jd = ((FieldDeclaration)thisNode).getJavadoc();
      log("Has f jdoc " + ((FieldDeclaration)thisNode).getJavadoc());
    }
    if(jd != null){
      jdocOffset = 1+jd.getLength();
    }
    log("ln 2 = " + ((CompilationUnit) thisNode.getRoot()).getLineNumber(thisNode
                                                                         .getStartPosition() + jdocOffset));
    return ((CompilationUnit) thisNode.getRoot()).getLineNumber(thisNode
        .getStartPosition() + jdocOffset);
  }*/

  static private String getNodeAsString(ASTNode node) {
    if (node == null)
      return "NULL";
    String className = node.getClass().getName();
    int index = className.lastIndexOf(".");
    if (index > 0)
      className = className.substring(index + 1);

    // if(node instanceof BodyDeclaration)
    // return className;

    String value = className;

    if (node instanceof TypeDeclaration)
      value = ((TypeDeclaration) node).getName().toString() + " | " + className;
    else if (node instanceof MethodDeclaration)
      value = ((MethodDeclaration) node).getName().toString() + " | "
          + className;
    else if (node instanceof MethodInvocation)
      value = ((MethodInvocation) node).getName().toString() + " | "
          + className;
    else if (node instanceof FieldDeclaration)
      value = ((FieldDeclaration) node).toString() + " FldDecl| ";
    else if (node instanceof SingleVariableDeclaration)
      value = ((SingleVariableDeclaration) node).getName() + " - "
          + ((SingleVariableDeclaration) node).getType() + " | SVD ";
    else if (node instanceof ExpressionStatement)
      value = node.toString() + className;
    else if (node instanceof SimpleName)
      value = ((SimpleName) node).getFullyQualifiedName() + " | " + className;
    else if (node instanceof QualifiedName)
      value = node.toString() + " | " + className;
    else if (className.startsWith("Variable"))
      value = node.toString() + " | " + className;
    else if (className.endsWith("Type"))
      value = node.toString() + " |" + className;
    value += " [" + node.getStartPosition() + ","
        + (node.getStartPosition() + node.getLength()) + "]";
    value += " Line: "
        + ((CompilationUnit) node.getRoot()).getLineNumber(node
            .getStartPosition());
    return value;
  }
}