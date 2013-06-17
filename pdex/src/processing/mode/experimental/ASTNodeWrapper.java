package processing.mode.experimental;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ASTNodeWrapper {
  private ASTNode node;

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
    this.node = node;
    label = getNodeAsString(node);
    if (label == null)
      label = node.toString();
    lineNumber = getLineNumber(node);
    label += " | Line " + lineNumber;
    //apiLevel = 0;
  }

  /**
   * For this node, finds various offsets (java code).
   * 
   * @return int[]{line number, line number start offset, node start offset,
   *         node length}
   */
  public int[] getJavaCodeOffsets() {
    int nodeLineNo = getLineNumber(node), nodeOffset = node.getStartPosition(), nodeLength = node
        .getLength();
    ASTNode thisNode = node;
    while (thisNode.getParent() != null) {
      if (getLineNumber(node.getParent()) == nodeLineNo) {
        node = node.getParent();
      } else {
        break;
      }
    }
    return new int[] {
      nodeLineNo, node.getStartPosition(), nodeOffset, nodeLength };
  }

  /**
   * 
   * @param ecs
   *          - ErrorCheckerService instance
   */
  public void getPDECodeOffsets(ErrorCheckerService ecs) {

  }

  public String toString() {
    return label;
  }

  public ASTNode getNode() {
    return node;
  }

  public String getLabel() {
    return label;
  }

  public int getNodeType() {
    return node.getNodeType();
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