package processing.mode.experimental;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ASTNodeWrapper {
  private ASTNode Node;

  private String label;

  private int lineNumber;
  
  //private int apiLevel;
  
  /*
   * TODO: Every ASTNode object in ASTGenerator.codetree is stored as a
   * ASTNodeWrapper instance. So how resource heavy would it be to store a
   * pointer to ECS in every instance of ASTNodeWrapper? Currently I will rather
   * pass an ECS pointer in the argument when I need to access a method which
   * requires a method defined in ECS, i.e, only on demand.  
   * Bad design choice for ECS methods? IDK, yet.
   */
  
  public ASTNodeWrapper(ASTNode node) {
    if (node == null)
      return;
    this.Node = node;
    label = getNodeAsString(node);
    if (label == null)
      label = node.toString();
    lineNumber = getLineNumber(node);
    label += " | Line " + lineNumber;
    //apiLevel = 0;
  }

  /**
   * For this node, finds various offsets (java code).
   * Note that line start offset for this node is int[2] - int[1]
   * @return int[]{line number, line number start offset, node start offset,
   *         node length}
   */
  public int[] getJavaCodeOffsets() {
    int nodeOffset = Node.getStartPosition(), nodeLength = Node
        .getLength();
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
    thisNode = thisNode.getParent();
    
    Iterator<StructuralPropertyDescriptor> it = thisNode
        .structuralPropertiesForType().iterator();
    boolean flag = true;
    while (it.hasNext()) {
      StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) it
          .next();
      if (prop.isChildListProperty()) {
        List<ASTNode> nodelist = (List<ASTNode>) thisNode
            .getStructuralProperty(prop);
        for (ASTNode cnode : nodelist) {
          if (getLineNumber(cnode) == lineNumber) {
            if (flag) {
              altStartPos = cnode.getStartPosition();
              // System.out.println("multi...");

              flag = false;
            } else {
              if(cnode == Node){
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
    // System.out.println("Altspos " + altStartPos);
    return new int[] { lineNumber,altStartPos , nodeOffset, nodeLength + normalizeOffsets(Node) };
  }
  
  /**
   * Returns the difference in offsets between pde and java versions of the ast
   * node
   * 
   * @param anode
   * @return offset adjustment
   */
  private int normalizeOffsets(ASTNode anode){
    String source = anode.toString().trim();
    System.out.println("Src: " + source);
    int offset = 0;
    String dataTypeFunc[] = { "Int", "Char", "Float", "Boolean", "Byte" };

    for (String dataType : dataTypeFunc) {
      String dataTypeRegexp = "\\bPApplet.parse" + dataType + "\\s*\\(";
      Pattern pattern = Pattern.compile(dataTypeRegexp);
      Matcher matcher = pattern.matcher(source);

      while (matcher.find()) {
        System.out.print("Start index: " + matcher.start());
        System.out.println(" End index: " + matcher.end() + " ");
        System.out.println("-->" + matcher.group() + "<--");
        offset = offset - ("PApplet.parse(").length();
      }
      
      
    }
    
    // Find all #[web color] and replace with 0xff[webcolor]
    // Should be 6 digits only.
    final String webColorRegexp = "#{1}[A-F|a-f|0-9]{6}\\W";
    Pattern webPattern = Pattern.compile(webColorRegexp);
    Matcher webMatcher = webPattern.matcher(source);
    while (webMatcher.find()) {
      // System.out.println("Found at: " + webMatcher.start());
      String found = source.substring(webMatcher.start(), webMatcher.end());
      // System.out.println("-> " + found);
      source = webMatcher.replaceFirst("0xff" + found.substring(1));
      webMatcher = webPattern.matcher(source);
      offset += 3;
    }

    // Replace all color data types with int
    // Regex, Y U SO powerful?
    final String colorTypeRegex = "color(?![a-zA-Z0-9_])(?=\\[*)(?!(\\s*\\())";
    Pattern colorPattern = Pattern.compile(colorTypeRegex);
    Matcher colorMatcher = colorPattern.matcher(source);
    source = colorMatcher.replaceAll("int");
    System.out.println(source + "-Norm offset " + offset);
    return offset;
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

  private static int getLineNumber(ASTNode node) {
    return ((CompilationUnit) node.getRoot()).getLineNumber(node
        .getStartPosition());
  }

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