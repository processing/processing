package processing.mode.experimental;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;


import processing.app.Base;
import processing.app.SketchCode;
import processing.app.syntax.InputHandler.clipboard_copy;

public class ASTGenerator {

  protected ErrorCheckerService errorCheckerService;

  protected DebugEditor editor;

  public DefaultMutableTreeNode codeTree = new DefaultMutableTreeNode();

  private DefaultMutableTreeNode currentParent = null;

  private JFrame frame2, frameAutoComp;

  private JTree jtree;

  private CompilationUnit compilationUnit;

  private JTable tableAuto;

  public ASTGenerator(ErrorCheckerService ecs) {
    this.errorCheckerService = ecs;
    this.editor = ecs.getEditor();
    frame2 = new JFrame();

    jtree = new JTree();
    frame2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame2.setBounds(new Rectangle(100, 100, 460, 620));
    JScrollPane sp = new JScrollPane();
    sp.setViewportView(jtree);
    frame2.add(sp);

    frameAutoComp = new JFrame();
    frameAutoComp.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frameAutoComp.setBounds(new Rectangle(1280, 100, 460, 620));
    tableAuto = new JTable();
    JScrollPane sp2 = new JScrollPane();
    sp2.setViewportView(tableAuto);
    frameAutoComp.add(sp2);

  }

  public class ASTNodeWrapper {
    private ASTNode node;

    private String label;

    private int lineNumber;

    private int apiLevel;

    public ASTNodeWrapper(ASTNode node) {
      if (node == null)
        return;
      this.node = node;
      label = getNodeAsString(node);
      if (label == null)
        label = node.toString();
      lineNumber = compilationUnit.getLineNumber(node.getStartPosition());
      label += " | Line " + lineNumber;
      apiLevel = 0;
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
  }

  private DefaultMutableTreeNode buildAST2(String source) {
    ASTParser parser = ASTParser.newParser(AST.JLS4);
    parser.setSource(source.toCharArray());
    parser.setKind(ASTParser.K_COMPILATION_UNIT);

    Map<String, String> options = JavaCore.getOptions();

    JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
    options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
    parser.setCompilerOptions(options);
    compilationUnit = (CompilationUnit) parser.createAST(null);
//    OutlineVisitor visitor = new OutlineVisitor();
//    compilationUnit.accept(visitor);
    codeTree = new DefaultMutableTreeNode(
                                          getNodeAsString((ASTNode) compilationUnit
                                              .types().get(0)));
    visitRecur((ASTNode) compilationUnit.types().get(0), codeTree);
    SwingWorker worker = new SwingWorker() {

      @Override
      protected Object doInBackground() throws Exception {
        return null;
      }

      protected void done() {
        if (codeTree != null) {
//					if (jtree.hasFocus() || frame2.hasFocus())
//						return;
          jtree.setModel(new DefaultTreeModel(codeTree));
          ((DefaultTreeModel) jtree.getModel()).reload();
          if (!frame2.isVisible())
            frame2.setVisible(true);
          if (!frameAutoComp.isVisible())
            frameAutoComp.setVisible(true);
          jtree.validate();
        }
      }
    };
    worker.execute();

    return codeTree;
  }

  public DefaultMutableTreeNode buildAST() {
    return buildAST2(errorCheckerService.sourceCode);
  }

  public String[] checkForTypes2(ASTNode node) {

    List<VariableDeclarationFragment> vdfs = null;
    switch (node.getNodeType()) {
    case ASTNode.TYPE_DECLARATION:
      return new String[] { ((TypeDeclaration) node).getName().toString() };

    case ASTNode.METHOD_DECLARATION:
      String[] ret1 = new String[] { ((MethodDeclaration) node).getName()
          .toString() };
      return ret1;

    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      return new String[] { ((SingleVariableDeclaration) node).getName()
          .toString() };

    case ASTNode.FIELD_DECLARATION:
      vdfs = ((FieldDeclaration) node).fragments();
      break;
    case ASTNode.VARIABLE_DECLARATION_STATEMENT:
      vdfs = ((VariableDeclarationStatement) node).fragments();
      break;
    case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
      vdfs = ((VariableDeclarationExpression) node).fragments();
      break;
    default:
      break;
    }

    if (vdfs != null) {
      String ret[] = new String[vdfs.size()];
      int i = 0;
      for (VariableDeclarationFragment vdf : vdfs) {
        ret[i++] = vdf.getName().toString();
      }
      return ret;
    }

    return null;
  }

  public static String[] checkForTypes(ASTNode node) {

    List<VariableDeclarationFragment> vdfs = null;
    switch (node.getNodeType()) {
    case ASTNode.TYPE_DECLARATION:
      return new String[] { getNodeAsString(node) };

    case ASTNode.METHOD_DECLARATION:
      String[] ret1 = new String[] { getNodeAsString(node) };
      return ret1;

    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      return new String[] { getNodeAsString(node) };

    case ASTNode.FIELD_DECLARATION:
      vdfs = ((FieldDeclaration) node).fragments();
      break;
    case ASTNode.VARIABLE_DECLARATION_STATEMENT:
      vdfs = ((VariableDeclarationStatement) node).fragments();
      break;
    case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
      vdfs = ((VariableDeclarationExpression) node).fragments();
      break;
    default:
      break;
    }

    if (vdfs != null) {
      String ret[] = new String[vdfs.size()];
      int i = 0;
      for (VariableDeclarationFragment vdf : vdfs) {
        ret[i++] = getNodeAsString(vdf);
      }
      return ret;
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  public ArrayList<String> getNameIfType(ASTNode astnode) {

    ArrayList<String> names = new ArrayList<String>();
    List<StructuralPropertyDescriptor> sprops = astnode
        .structuralPropertiesForType();
    for (StructuralPropertyDescriptor sprop : sprops) {
      ASTNode cnode = null;
      if (!sprop.isChildListProperty()) {
        if (astnode.getStructuralProperty(sprop) instanceof ASTNode) {
          cnode = (ASTNode) astnode.getStructuralProperty(sprop);
          //if(cnode)
        }
      } else {
        // Childlist prop
        List<ASTNode> nodelist = (List<ASTNode>) astnode
            .getStructuralProperty(sprop);
        for (ASTNode clnode : nodelist) {

        }
      }
    }

    return names;
  }

  public static ASTNode resolveExpression(ASTNode nearestNode,
                                          ASTNode expression) {
//    ASTNode anode = null;
    if (expression instanceof SimpleName) {
      return findDeclaration2(((SimpleName) expression), nearestNode);
    } else if (expression instanceof MethodInvocation) {
      return findDeclaration2(((MethodInvocation) expression).getName(),
                              nearestNode);
    } else if (expression instanceof FieldAccess) {
      return findDeclaration2(((FieldAccess) expression).getName(), nearestNode);
    } else if (expression instanceof QualifiedName) {
      return findDeclaration2(((QualifiedName) expression).getName(),
                              nearestNode);
    }

//    if (anode != null) {
//      System.out.println("Expression: " + anode);
//      anode = resolveExpression(nearestNode, anode);
//    }
//    String word = anode.toString();
//    // Here expression should be a SN type hopefully.
//    System.out.println("Final Expression: " + word);
////    anode = expression.getParent();
//    
//    anode = nearestNode.getParent();
//    ASTNode matchinNode = null;
//    while (anode != null) {
//
//      List<StructuralPropertyDescriptor> sprops = anode
//          .structuralPropertiesForType();
//      for (StructuralPropertyDescriptor sprop : sprops) {
//        ASTNode cnode = null;
//        if (!sprop.isChildListProperty()) {
//          if (anode.getStructuralProperty(sprop) instanceof ASTNode) {
//            cnode = (ASTNode) anode.getStructuralProperty(sprop);
//            String[] types = checkForTypes(cnode);
//            if (types != null) {
//              for (int i = 0; i < types.length; i++) {
//                if (types[i].startsWith(word))
//                {
//                  System.out.println("match: "+types[i]);
//                }
//              }
//            }
//          }
//        } else {
//          // Childlist prop
//          List<ASTNode> nodelist = (List<ASTNode>) anode
//              .getStructuralProperty(sprop);
//          for (ASTNode clnode : nodelist) {
//            String[] types = checkForTypes(clnode);
//            if (types != null) {
//              for (int i = 0; i < types.length; i++) {
//                if (types[i].startsWith(word))
//                  System.out.println("match: "+types[i]);;
//              }
//            }
//          }
//        }
//      }
//      anode = anode.getParent();
//    }

    return null;
  }

  public static TypeDeclaration getDefiningNode(ASTNode node) {
    ASTNode parent = node.getParent();
    while (!(parent instanceof TypeDeclaration)) {
      parent = parent.getParent();
      if (parent instanceof CompilationUnit)
        return null;
    }
    return (TypeDeclaration) parent;
  }

  public void updatePredictions(final String word, final int line) {
    SwingWorker worker = new SwingWorker() {

      @Override
      protected Object doInBackground() throws Exception {
        return null;
      }

      protected void done() {
//        ArrayList<ASTNodeWrapper> candidates = new ArrayList<ASTGenerator.ASTNodeWrapper>();

        //ASTNodeWrapper[][] candi = new ASTNodeWrapper[80][1];
        int lineNumber = line;
        // Adjust line number for tabbed sketches
        if (errorCheckerService != null) {
          editor = errorCheckerService.getEditor();
          int codeIndex = editor.getSketch().getCodeIndex(editor
                                                              .getCurrentTab());
          if (codeIndex > 0)
            for (int i = 0; i < codeIndex; i++) {
              SketchCode sc = editor.getSketch().getCode(i);
              int len = Base.countLines(sc.getProgram()) + 1;
              lineNumber += len;
            }

        }

        ASTNode anode = null;
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setKind(ASTParser.K_EXPRESSION);
        parser.setSource(word.toCharArray());
        ASTNode testnode = parser.createAST(null);
        System.out.print("Typed: " + word + "|");
        anode = findClosestNode(lineNumber, (ASTNode) compilationUnit.types()
            .get(0));
        System.out.println(lineNumber + " Nearest ASTNode to PRED "
            + getNodeAsString(anode));

        ArrayList<String> candidates = new ArrayList<String>();

        if (testnode instanceof SimpleName) {
          anode = anode.getParent();
          while (anode != null) {

            List<StructuralPropertyDescriptor> sprops = anode
                .structuralPropertiesForType();
            for (StructuralPropertyDescriptor sprop : sprops) {
              ASTNode cnode = null;
              if (!sprop.isChildListProperty()) {
                if (anode.getStructuralProperty(sprop) instanceof ASTNode) {
                  cnode = (ASTNode) anode.getStructuralProperty(sprop);
                  String[] types = checkForTypes(cnode);
                  if (types != null) {
                    for (int i = 0; i < types.length; i++) {
                      if (types[i].startsWith(word))
                        candidates.add(types[i]);
                    }
                  }
                }
              } else {
                // Childlist prop
                List<ASTNode> nodelist = (List<ASTNode>) anode
                    .getStructuralProperty(sprop);
                for (ASTNode clnode : nodelist) {
                  String[] types = checkForTypes(clnode);
                  if (types != null) {
                    for (int i = 0; i < types.length; i++) {
                      if (types[i].startsWith(word))
                        candidates.add(types[i]);
                    }
                  }
                }
              }
            }
            anode = anode.getParent();
          }
        } else {

          // Complicated completion
          System.out.println("Not a SN " + getNodeAsString(testnode));
          ASTNode det = resolveExpression(anode, testnode);
          if (det != null) {
            TypeDeclaration td = null;
            if (det instanceof MethodDeclaration) {
              if (((MethodDeclaration) det).getReturnType2() instanceof SimpleType) {
                SimpleType stp = (SimpleType) (((MethodDeclaration) det)
                    .getReturnType2());
                td = (TypeDeclaration) findDeclaration(stp.getName());
              }
            } else if (det instanceof FieldDeclaration) {
              if (((FieldDeclaration) det).getType() instanceof SimpleType) {
                SimpleType stp = (SimpleType) (((FieldDeclaration) det)
                    .getType());
                td = (TypeDeclaration) findDeclaration(stp.getName());
              }
            }

            System.out.println(getNodeAsString(det) + " defined in "
                + getNodeAsString(td));
            if (td != null) {
              for (int i = 0; i < td.getFields().length; i++) {
                candidates.add(getNodeAsString(td.getFields()[i]));
              }
              for (int i = 0; i < td.getMethods().length; i++) {
                candidates.add(getNodeAsString(td.getMethods()[i]));
              }
            }

          }

        }

        String[][] candi = new String[candidates.size()][1];
        for (int i = 0; i < candi.length; i++) {
          candi[i][0] = candidates.get(i);
        }
        System.out.println("K = " + candidates.size());
        DefaultTableModel tm = new DefaultTableModel(
                                                     candi,
                                                     new String[] { "Suggestions" });
        tableAuto.setModel(tm);
        tableAuto.validate();
        tableAuto.repaint();
      }
    };

    worker.execute();

  }

  @SuppressWarnings("unchecked")
  private static ASTNode findClosestParentNode(int lineNumber, ASTNode node) {
    Iterator<StructuralPropertyDescriptor> it = node
        .structuralPropertiesForType().iterator();
    //System.err.println("Props of " + node.getClass().getName());
    while (it.hasNext()) {
      StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) it
          .next();

      if (prop.isChildProperty() || prop.isSimpleProperty()) {
        if (node.getStructuralProperty(prop) != null) {
//          System.out
//              .println(node.getStructuralProperty(prop) + " -> " + (prop));
          if (node.getStructuralProperty(prop) instanceof ASTNode) {
            ASTNode cnode = (ASTNode) node.getStructuralProperty(prop);
            System.out.println("Looking at " + getNodeAsString(cnode));
            int cLineNum = ((CompilationUnit) cnode.getRoot())
                .getLineNumber(cnode.getStartPosition() + cnode.getLength());
            if (getLineNumber(cnode) <= lineNumber && lineNumber <= cLineNum) {
              return findClosestParentNode(lineNumber, cnode);
            }
          }
        }
      }

      else if (prop.isChildListProperty()) {
        List<ASTNode> nodelist = (List<ASTNode>) node
            .getStructuralProperty(prop);
        for (ASTNode cnode : nodelist) {
          int cLineNum = ((CompilationUnit) cnode.getRoot())
              .getLineNumber(cnode.getStartPosition() + cnode.getLength());
          System.out.println("Looking at " + getNodeAsString(cnode));
          if (getLineNumber(cnode) <= lineNumber && lineNumber <= cLineNum) {
            return findClosestParentNode(lineNumber, cnode);
          }
        }
      }
    }
    return node;
  }

  @SuppressWarnings("unchecked")
  private static ASTNode findClosestNode(int lineNumber, ASTNode node) {
    ASTNode parent = findClosestParentNode(lineNumber, node);
    if (parent == null)
      return null;
    if (getLineNumber(parent) == lineNumber)
      return parent;
    List<ASTNode> nodes = null;
    if (parent instanceof TypeDeclaration) {
      nodes = (List<ASTNode>) ((TypeDeclaration) parent)
          .getStructuralProperty(TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
    } else if (parent instanceof Block) {
      nodes = (List<ASTNode>) ((Block) parent)
          .getStructuralProperty(Block.STATEMENTS_PROPERTY);
    } else {
      System.err.println("THIS CONDITION SHOULD NOT OCCUR - findClosestNode "
          + getNodeAsString(parent));
      return null;
    }

    ASTNode retNode = nodes.get(0);
    for (ASTNode cNode : nodes) {
      if (getLineNumber(cNode) <= lineNumber)
        retNode = cNode;
      else
        break;
    }

    return retNode;
  }

//  static DefaultMutableTreeNode findNodeBS(DefaultMutableTreeNode tree,
//                                           int lineNumber, String name,
//                                           int elementOffset) {
//    if (tree.getUserObject() == null)
//      return null;
//
//    ASTNodeWrapper node = ((ASTNodeWrapper) tree.getUserObject());
//
//    if (node.getLineNumber() == lineNumber) {
//      System.out.println("Located line " + lineNumber + " , " + tree);
//      if (name == null)
//        return tree;
//      else
//        return findOnLine(tree, lineNumber, name, elementOffset, node.getNode()
//            .getStartPosition());
//    }
//
//    int low = 0, high = tree.getChildCount() - 1, mid = (high + low) / 2;
//    DefaultMutableTreeNode tnode = null;
//    while (low <= high) {
//      mid = (high + low) / 2;
//      tnode = (DefaultMutableTreeNode) tree.getChildAt(mid);
//      node = ((ASTNodeWrapper) tnode.getUserObject());
//      if (node.getLineNumber() == lineNumber) {
//        System.out.println("Located line " + lineNumber + " , " + tnode);
//        if (name == null)
//          return tnode;
//        else
//          return findOnLine(tnode, lineNumber, name, elementOffset, node
//              .getNode().getStartPosition());
//      } else if (lineNumber < node.getLineNumber()) {
//        high = mid - 1;
//      } else {
//        if (high - mid <= 1) {
//          if (lineNumber > ((ASTNodeWrapper) ((DefaultMutableTreeNode) tree
//              .getChildAt(high)).getUserObject()).getLineNumber()) //high l no.
//            low = mid + 1;
//          else
//
//            high = mid - 1;
//        }
//        low = mid + 1;
//      }
//
//    }
//
//    if (!tnode.isLeaf())
//      return findNodeBS(tnode, lineNumber, name, elementOffset);
//    else
//      return tnode;
//
//      //System.out.println("visiting: " + getNodeAsString(node.getNode()) + " on  line "+node.getLineNumber());
//      if (node.getLineNumber() == lineNumber) {
//        System.err.println("Located line: " + node.toString());
//        if (name == null) // name ==null, finds any node equal to line
//        // number
//        {
//          System.out.println("Closest node at line: " + lineNumber);
//          return tree;
//        } else
//          return findOnLine(tree, lineNumber, name, elementOffset, node.getNode()
//              .getStartPosition());
//
//      } else if (!tree.isLeaf()) {
//        for (int i = 0; i < tree.getChildCount(); i++) {
//                                                      .getChildAt(i),
//                                                  lineNumber, name, elementOffset);
//          if (node2 != null)
//            return node2;
//        }
//      }
//
//    return null;
//  }

  public DefaultMutableTreeNode getAST() {
    return codeTree;
  }

  public String getLabelForASTNode(int lineNumber, String name, int offset) {
    retLabelString = "";
    getASTNodeAt(lineNumber, name, offset, false);
    return retLabelString;
  }

  public void scrollToDeclaration(int lineNumber, String name, int offset) {
    getASTNodeAt(lineNumber, name, offset, true);
  }

  String retLabelString;

  public ASTNodeWrapper getASTNodeAt(int lineNumber, String name, int offset,
                                     boolean scrollOnly) {
    System.out.println("--------");
    if (errorCheckerService != null) {
      editor = errorCheckerService.getEditor();
      int codeIndex = editor.getSketch().getCodeIndex(editor.getCurrentTab());
      if (codeIndex > 0) {
        for (int i = 0; i < codeIndex; i++) {
          SketchCode sc = editor.getSketch().getCode(i);
          int len = Base.countLines(sc.getProgram()) + 1;
          lineNumber += len;
        }
      }

    }
    System.out.println("FLON: " + lineNumber);
    ASTNode lineNode = findLineOfNode(compilationUnit, lineNumber, offset, name);
    System.out.println("+> " + lineNode);
    ASTNode decl = null;
    if (lineNode != null) {
      System.out.println("FLON2: " + lineNumber + " LN O "
          + lineNode.getStartPosition());
      ASTNode simpName = pinpointOnLine(lineNode, offset,
                                        lineNode.getStartPosition(), name);
      System.out.println("+++> " + simpName);
      if (simpName instanceof SimpleName) {
        System.out.println(getNodeAsString(simpName));
        decl = findDeclaration((SimpleName) simpName);
        if (decl != null) {
          System.err.println("DECLA: " + decl.getClass().getName());
          retLabelString = getNodeAsString(decl);
        } else
          System.err.println("null");

        System.out.println(getNodeAsString(decl));
      }
    }

//    
//    retStr = "";
    if (decl != null && scrollOnly) {
      System.err.println("FINAL String label: " + getNodeAsString(decl));

      DefaultProblem dpr = new DefaultProblem(null, null, -1, null, -1,
                                              decl.getStartPosition(),
                                              decl.getStartPosition(),
                                              getLineNumber(decl) + 1, 0);
      int[] position = errorCheckerService.calculateTabIndexAndLineNumber(dpr);
      System.out.println("Tab " + position[0] + ", Line: " + (position[1]));
      Problem p = new Problem(dpr, position[0], position[1]);
      //errorCheckerService.scrollToErrorLine(p);
    } // uncomment this one, it works

    return null;
  }

  private static int getLineNumber(ASTNode node) {
    return ((CompilationUnit) node.getRoot()).getLineNumber(node
        .getStartPosition());
  }

  public static void main(String[] args) {
    traversal2();
//    ASTParserd
  }

  public static void traversal2() {
    ASTParser parser = ASTParser.newParser(AST.JLS4);
    String source = readFile("/media/quarkninja/Work/TestStuff/low.java");
//    String source = "package decl; \npublic class ABC{\n int ret(){\n}\n}";
    parser.setSource(source.toCharArray());
    parser.setKind(ASTParser.K_COMPILATION_UNIT);

    Map<String, String> options = JavaCore.getOptions();

    JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
    options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
    parser.setCompilerOptions(options);

    CompilationUnit cu = (CompilationUnit) parser.createAST(null);
    System.out.println(CompilationUnit.propertyDescriptors(AST.JLS4).size());

    DefaultMutableTreeNode astTree = new DefaultMutableTreeNode(
                                                                "CompilationUnit");
    System.err.println("Errors: " + cu.getProblems().length);
    visitRecur(cu, astTree);
    System.out.println(astTree.getChildCount());

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      JFrame frame2 = new JFrame();
      JTree jtree = new JTree(astTree);
      frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame2.setBounds(new Rectangle(100, 100, 460, 620));
      JScrollPane sp = new JScrollPane();
      sp.setViewportView(jtree);
      frame2.add(sp);
      frame2.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }

    ASTNode found = NodeFinder.perform(cu, 468, 5);
    if (found != null) {
      System.out.println(found);
    }
  }

  @SuppressWarnings({ "unchecked" })
  public static void visitRecur(ASTNode node, DefaultMutableTreeNode tnode) {
    Iterator<StructuralPropertyDescriptor> it = node
        .structuralPropertiesForType().iterator();
    //System.err.println("Props of " + node.getClass().getName());
    DefaultMutableTreeNode ctnode = null;
    while (it.hasNext()) {
      StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) it
          .next();

      if (prop.isChildProperty() || prop.isSimpleProperty()) {
        if (node.getStructuralProperty(prop) != null) {
//          System.out
//              .println(node.getStructuralProperty(prop) + " -> " + (prop));
          if (node.getStructuralProperty(prop) instanceof ASTNode) {
            ASTNode cnode = (ASTNode) node.getStructuralProperty(prop);
            if (isAddableASTNode(cnode)) {
              ctnode = new DefaultMutableTreeNode(
                                                  getNodeAsString((ASTNode) node
                                                      .getStructuralProperty(prop)));
              tnode.add(ctnode);
              visitRecur(cnode, ctnode);
            }
          } else {
            tnode.add(new DefaultMutableTreeNode(node
                .getStructuralProperty(prop)));
          }
        }
      }

      else if (prop.isChildListProperty()) {
        List<ASTNode> nodelist = (List<ASTNode>) node
            .getStructuralProperty(prop);
        for (ASTNode cnode : nodelist) {
          if (isAddableASTNode(cnode)) {
            ctnode = new DefaultMutableTreeNode(getNodeAsString(cnode));
            tnode.add(ctnode);
            visitRecur(cnode, ctnode);
          } else
            visitRecur(cnode, tnode);
        }
      }
    }
  }

  public static void printRecur(ASTNode node) {
    Iterator<StructuralPropertyDescriptor> it = node
        .structuralPropertiesForType().iterator();
    //System.err.println("Props of " + node.getClass().getName());
    while (it.hasNext()) {
      StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) it
          .next();

      if (prop.isChildProperty() || prop.isSimpleProperty()) {
        if (node.getStructuralProperty(prop) != null) {
//          System.out
//              .println(node.getStructuralProperty(prop) + " -> " + (prop));
          if (node.getStructuralProperty(prop) instanceof ASTNode) {
            ASTNode cnode = (ASTNode) node.getStructuralProperty(prop);
            System.out.println(getNodeAsString(cnode));
            printRecur(cnode);
          }
        }
      }

      else if (prop.isChildListProperty()) {
        List<ASTNode> nodelist = (List<ASTNode>) node
            .getStructuralProperty(prop);
        for (ASTNode cnode : nodelist) {
          System.out.println(getNodeAsString(cnode));
          printRecur(cnode);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static ASTNode findLineOfNode(ASTNode node, int lineNumber,
                                        int offset, String name) {

    CompilationUnit root = (CompilationUnit) node.getRoot();
//    System.out.println("Inside "+getNodeAsString(node) + " | " + root.getLineNumber(node.getStartPosition()));
    if (root.getLineNumber(node.getStartPosition()) == lineNumber) {
      System.err
          .println(3 + getNodeAsString(node) + " len " + node.getLength());

      if (offset < node.getLength())
        return node;
      else {
        System.err.println(-11);
        return null;
      }
    }
    for (Object oprop : node.structuralPropertiesForType()) {
      StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) oprop;
      if (prop.isChildProperty() || prop.isSimpleProperty()) {
        if (node.getStructuralProperty(prop) != null) {
          if (node.getStructuralProperty(prop) instanceof ASTNode) {
            ASTNode retNode = findLineOfNode((ASTNode) node
                                                 .getStructuralProperty(prop),
                                             lineNumber, offset, name);
            if (retNode != null) {
//              System.err.println(11 + getNodeAsString(retNode));
              return retNode;
            }
          }
        }
      } else if (prop.isChildListProperty()) {
        List<ASTNode> nodelist = (List<ASTNode>) node
            .getStructuralProperty(prop);
        for (ASTNode retNode : nodelist) {

          ASTNode rr = findLineOfNode(retNode, lineNumber, offset, name);
          if (rr != null) {
//              System.err.println(12 + getNodeAsString(rr));
            return rr;
          }
        }
      }
    }
//    System.err.println("-1");
    return null;
  }

  /**
   * 
   * @param node
   * @param offset
   *          - from textarea painter
   * @param lineStartOffset
   *          - obtained from findLineOfNode
   * @param name
   * @param root
   * @return
   */
  @SuppressWarnings("unchecked")
  public static ASTNode pinpointOnLine(ASTNode node, int offset,
                                       int lineStartOffset, String name) {

    if (node instanceof SimpleName) {
      SimpleName sn = (SimpleName) node;
      if ((lineStartOffset + offset) >= sn.getStartPosition()
          && (lineStartOffset + offset) <= sn.getStartPosition()
              + sn.getLength()) {
        if (sn.toString().equals(name))
          return sn;
        else
          return null;
      } else
        return null;
    }
    for (Object oprop : node.structuralPropertiesForType()) {
      StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) oprop;
      if (prop.isChildProperty() || prop.isSimpleProperty()) {
        if (node.getStructuralProperty(prop) != null) {
          if (node.getStructuralProperty(prop) instanceof ASTNode) {
            ASTNode retNode = pinpointOnLine((ASTNode) node
                                                 .getStructuralProperty(prop),
                                             offset, lineStartOffset, name);
            if (retNode != null) {
//              System.err.println(11 + getNodeAsString(retNode));
              return retNode;
            }
          }
        }
      } else if (prop.isChildListProperty()) {
        List<ASTNode> nodelist = (List<ASTNode>) node
            .getStructuralProperty(prop);
        for (ASTNode retNode : nodelist) {

          ASTNode rr = pinpointOnLine(retNode, offset, lineStartOffset, name);
          if (rr != null) {
//              System.err.println(12 + getNodeAsString(rr));
            return rr;
          }
        }
      }
    }
//    System.err.println("-1");
    return null;
  }

  @SuppressWarnings("unchecked")
  private static ASTNode findDeclaration(Name findMe) {
    ASTNode declaringClass = null;
    ASTNode parent = findMe.getParent();
    ASTNode ret = null;
    ArrayList<Integer> constrains = new ArrayList<Integer>();
    if (parent.getNodeType() == ASTNode.METHOD_INVOCATION) {
      Expression exp = (Expression) ((MethodInvocation) parent)
          .getStructuralProperty(MethodInvocation.EXPRESSION_PROPERTY);
      //TODO: Note the imbalance of constrains.add(ASTNode.METHOD_DECLARATION);
      // Possibly a bug here. Investigate later.
      if (((MethodInvocation) parent).getName().toString()
          .equals(findMe.toString())) {
        constrains.add(ASTNode.METHOD_DECLARATION);

        if (exp != null) {
          constrains.add(ASTNode.TYPE_DECLARATION);
          System.out.println("MI EXP: " + exp.toString() + " of type "
              + exp.getClass().getName() + " parent: " + exp.getParent());
          if (exp instanceof MethodInvocation) {
            SimpleType stp = extracTypeInfo(findDeclaration(((MethodInvocation) exp)
                .getName()));
            if (stp == null)
              return null;
            declaringClass = findDeclaration(stp.getName());
            return definedIn(declaringClass, ((MethodInvocation) parent)
                .getName().toString(), constrains, declaringClass);
          } else if (exp instanceof FieldAccess) {
            SimpleType stp = extracTypeInfo(findDeclaration(((FieldAccess) exp)
                .getName()));
            if (stp == null)
              return null;
            declaringClass = findDeclaration((stp.getName()));
            return definedIn(declaringClass, ((MethodInvocation) parent)
                .getName().toString(), constrains, declaringClass);
          }
          if (exp instanceof SimpleName) {
            SimpleType stp = extracTypeInfo(findDeclaration(((SimpleName) exp)));
            if (stp == null)
              return null;
            declaringClass = findDeclaration(stp.getName());
            System.out.println("MI.SN " + getNodeAsString(declaringClass));
            constrains.add(ASTNode.METHOD_DECLARATION);
            return definedIn(declaringClass, ((MethodInvocation) parent)
                .getName().toString(), constrains, declaringClass);
          }

        }
      } else {
        parent = parent.getParent(); // Move one up the ast. V V IMP!!
      }
    } else if (parent.getNodeType() == ASTNode.FIELD_ACCESS) {
      FieldAccess fa = (FieldAccess) parent;
      Expression exp = fa.getExpression();
      if (fa.getName().toString().equals(findMe.toString())) {
        constrains.add(ASTNode.FIELD_DECLARATION);

        if (exp != null) {
          constrains.add(ASTNode.TYPE_DECLARATION);
          System.out.println("FA EXP: " + exp.toString() + " of type "
              + exp.getClass().getName() + " parent: " + exp.getParent());
          if (exp instanceof MethodInvocation) {
            SimpleType stp = extracTypeInfo(findDeclaration(((MethodInvocation) exp)
                .getName()));
            if (stp == null)
              return null;
            declaringClass = findDeclaration(stp.getName());
            return definedIn(declaringClass, fa.getName().toString(),
                             constrains, declaringClass);
          } else if (exp instanceof FieldAccess) {
            SimpleType stp = extracTypeInfo(findDeclaration(((FieldAccess) exp)
                .getName()));
            if (stp == null)
              return null;
            declaringClass = findDeclaration((stp.getName()));
            constrains.add(ASTNode.TYPE_DECLARATION);
            return definedIn(declaringClass, fa.getName().toString(),
                             constrains, declaringClass);
          }
          if (exp instanceof SimpleName) {
            SimpleType stp = extracTypeInfo(findDeclaration(((SimpleName) exp)));
            if (stp == null)
              return null;
            declaringClass = findDeclaration(stp.getName());
            System.out.println("FA.SN " + getNodeAsString(declaringClass));
            constrains.add(ASTNode.METHOD_DECLARATION);
            return definedIn(declaringClass, fa.getName().toString(),
                             constrains, declaringClass);
          }
        }

      } else {
        parent = parent.getParent(); // Move one up the ast. V V IMP!!
      }
    } else if (parent.getNodeType() == ASTNode.QUALIFIED_NAME) {

      QualifiedName qn = (QualifiedName) parent;
      if (!findMe.toString().equals(qn.getQualifier().toString())) {

        SimpleType stp = extracTypeInfo(findDeclaration((qn.getQualifier())));
        declaringClass = findDeclaration(stp.getName());
        System.out.println(qn.getQualifier() + "->" + qn.getName());
        System.out.println("QN decl class: " + getNodeAsString(declaringClass));
        constrains.clear();
        constrains.add(ASTNode.TYPE_DECLARATION);
        constrains.add(ASTNode.FIELD_DECLARATION);
        return definedIn(declaringClass, qn.getName().toString(), constrains,
                         null);
      }
    } else if (parent.getNodeType() == ASTNode.SIMPLE_TYPE) {
      constrains.add(ASTNode.TYPE_DECLARATION);
      if (parent.getParent().getNodeType() == ASTNode.CLASS_INSTANCE_CREATION)
        constrains.add(ASTNode.CLASS_INSTANCE_CREATION);
    } else if (parent instanceof Expression) {
//      constrains.add(ASTNode.TYPE_DECLARATION);
//      constrains.add(ASTNode.METHOD_DECLARATION);
//      constrains.add(ASTNode.FIELD_DECLARATION);      
    }
    while (parent != null) {
      System.out.println("findDeclaration1 -> " + getNodeAsString(parent));
      for (Object oprop : parent.structuralPropertiesForType()) {
        StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) oprop;
        if (prop.isChildProperty() || prop.isSimpleProperty()) {
          if (parent.getStructuralProperty(prop) instanceof ASTNode) {
            System.out.println(prop + " C/S Prop of -> "
                + getNodeAsString(parent));
            ret = definedIn((ASTNode) parent.getStructuralProperty(prop),
                            findMe.toString(), constrains, declaringClass);
            if (ret != null)
              return ret;
          }
        } else if (prop.isChildListProperty()) {
          System.out.println((prop) + " ChildList props of "
              + getNodeAsString(parent));
          List<ASTNode> nodelist = (List<ASTNode>) parent
              .getStructuralProperty(prop);
          for (ASTNode retNode : nodelist) {
            ret = definedIn(retNode, findMe.toString(), constrains,
                            declaringClass);
            if (ret != null)
              return ret;
          }
        }
      }
      parent = parent.getParent();
    }
    return null;
  }

  private static ASTNode findDeclaration2(Name findMe, ASTNode alternateParent) {
    ASTNode declaringClass = null;
    ASTNode parent = findMe.getParent();
    ASTNode ret = null;
    ArrayList<Integer> constrains = new ArrayList<Integer>();
    if (parent.getNodeType() == ASTNode.METHOD_INVOCATION) {
      Expression exp = (Expression) ((MethodInvocation) parent)
          .getStructuralProperty(MethodInvocation.EXPRESSION_PROPERTY);
      //TODO: Note the imbalance of constrains.add(ASTNode.METHOD_DECLARATION);
      // Possibly a bug here. Investigate later.
      if (((MethodInvocation) parent).getName().toString()
          .equals(findMe.toString())) {
        constrains.add(ASTNode.METHOD_DECLARATION);

        if (exp != null) {
          constrains.add(ASTNode.TYPE_DECLARATION);
          System.out.println("MI EXP: " + exp.toString() + " of type "
              + exp.getClass().getName() + " parent: " + exp.getParent());
          if (exp instanceof MethodInvocation) {
            SimpleType stp = extracTypeInfo(findDeclaration2(((MethodInvocation) exp)
                                                                 .getName(),
                                                             alternateParent));
            if (stp == null)
              return null;
            declaringClass = findDeclaration2(stp.getName(), alternateParent);
            return definedIn(declaringClass, ((MethodInvocation) parent)
                .getName().toString(), constrains, declaringClass);
          } else if (exp instanceof FieldAccess) {
            SimpleType stp = extracTypeInfo(findDeclaration2(((FieldAccess) exp)
                                                                 .getName(),
                                                             alternateParent));
            if (stp == null)
              return null;
            declaringClass = findDeclaration2((stp.getName()), alternateParent);
            return definedIn(declaringClass, ((MethodInvocation) parent)
                .getName().toString(), constrains, declaringClass);
          }
          if (exp instanceof SimpleName) {
            SimpleType stp = extracTypeInfo(findDeclaration2(((SimpleName) exp),
                                                             alternateParent));
            if (stp == null)
              return null;
            declaringClass = findDeclaration2(stp.getName(), alternateParent);
            System.out.println("MI.SN " + getNodeAsString(declaringClass));
            constrains.add(ASTNode.METHOD_DECLARATION);
            return definedIn(declaringClass, ((MethodInvocation) parent)
                .getName().toString(), constrains, declaringClass);
          }

        }
      } else {
        parent = parent.getParent(); // Move one up the ast. V V IMP!!
        alternateParent = alternateParent.getParent();
      }
    } else if (parent.getNodeType() == ASTNode.FIELD_ACCESS) {
      FieldAccess fa = (FieldAccess) parent;
      Expression exp = fa.getExpression();
      if (fa.getName().toString().equals(findMe.toString())) {
        constrains.add(ASTNode.FIELD_DECLARATION);

        if (exp != null) {
          constrains.add(ASTNode.TYPE_DECLARATION);
          System.out.println("FA EXP: " + exp.toString() + " of type "
              + exp.getClass().getName() + " parent: " + exp.getParent());
          if (exp instanceof MethodInvocation) {
            SimpleType stp = extracTypeInfo(findDeclaration2(((MethodInvocation) exp)
                                                                 .getName(),
                                                             alternateParent));
            if (stp == null)
              return null;
            declaringClass = findDeclaration2(stp.getName(), alternateParent);
            return definedIn(declaringClass, fa.getName().toString(),
                             constrains, declaringClass);
          } else if (exp instanceof FieldAccess) {
            SimpleType stp = extracTypeInfo(findDeclaration2(((FieldAccess) exp)
                                                                 .getName(),
                                                             alternateParent));
            if (stp == null)
              return null;
            declaringClass = findDeclaration2((stp.getName()), alternateParent);
            constrains.add(ASTNode.TYPE_DECLARATION);
            return definedIn(declaringClass, fa.getName().toString(),
                             constrains, declaringClass);
          }
          if (exp instanceof SimpleName) {
            SimpleType stp = extracTypeInfo(findDeclaration2(((SimpleName) exp),
                                                             alternateParent));
            if (stp == null)
              return null;
            declaringClass = findDeclaration2(stp.getName(), alternateParent);
            System.out.println("FA.SN " + getNodeAsString(declaringClass));
            constrains.add(ASTNode.METHOD_DECLARATION);
            return definedIn(declaringClass, fa.getName().toString(),
                             constrains, declaringClass);
          }
        }

      } else {
        parent = parent.getParent(); // Move one up the ast. V V IMP!!
        alternateParent = alternateParent.getParent();
      }
    } else if (parent.getNodeType() == ASTNode.QUALIFIED_NAME) {

      QualifiedName qn = (QualifiedName) parent;
      if (!findMe.toString().equals(qn.getQualifier().toString())) {

        SimpleType stp = extracTypeInfo(findDeclaration2((qn.getQualifier()),
                                                         alternateParent));
        declaringClass = findDeclaration2(stp.getName(), alternateParent);
        System.out.println(qn.getQualifier() + "->" + qn.getName());
        System.out.println("QN decl class: " + getNodeAsString(declaringClass));
        constrains.clear();
        constrains.add(ASTNode.TYPE_DECLARATION);
        constrains.add(ASTNode.FIELD_DECLARATION);
        return definedIn(declaringClass, qn.getName().toString(), constrains,
                         null);
      }
    } else if (parent.getNodeType() == ASTNode.SIMPLE_TYPE) {
      constrains.add(ASTNode.TYPE_DECLARATION);
      if (parent.getParent().getNodeType() == ASTNode.CLASS_INSTANCE_CREATION)
        constrains.add(ASTNode.CLASS_INSTANCE_CREATION);
    } else if (parent instanceof Expression) {
//      constrains.add(ASTNode.TYPE_DECLARATION);
//      constrains.add(ASTNode.METHOD_DECLARATION);
//      constrains.add(ASTNode.FIELD_DECLARATION);      
    }
    System.out.println("Alternate parent: " + getNodeAsString(alternateParent));
    while (alternateParent != null) {
      System.out.println("findDeclaration2 -> "
          + getNodeAsString(alternateParent));
      for (Object oprop : alternateParent.structuralPropertiesForType()) {
        StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) oprop;
        if (prop.isChildProperty() || prop.isSimpleProperty()) {
          if (alternateParent.getStructuralProperty(prop) instanceof ASTNode) {
            System.out.println(prop + " C/S Prop of -> "
                + getNodeAsString(alternateParent));
            ret = definedIn((ASTNode) alternateParent
                                .getStructuralProperty(prop),
                            findMe.toString(), constrains, declaringClass);
            if (ret != null)
              return ret;
          }
        } else if (prop.isChildListProperty()) {
          System.out.println((prop) + " ChildList props of "
              + getNodeAsString(alternateParent));
          List<ASTNode> nodelist = (List<ASTNode>) alternateParent
              .getStructuralProperty(prop);
          for (ASTNode retNode : nodelist) {
            ret = definedIn(retNode, findMe.toString(), constrains,
                            declaringClass);
            if (ret != null)
              return ret;
          }
        }
      }
      alternateParent = alternateParent.getParent();
    }
    return null;
  }

  /**
   * Find the SimpleType from FD, SVD, VDS, etc
   * 
   * @param node
   * @return
   */
  private static SimpleType extracTypeInfo(ASTNode node) {
    if (node == null)
      return null;
    switch (node.getNodeType()) {
    case ASTNode.METHOD_DECLARATION:
      return (SimpleType) ((MethodDeclaration) node)
          .getStructuralProperty(MethodDeclaration.RETURN_TYPE2_PROPERTY);
    case ASTNode.FIELD_DECLARATION:
      return (SimpleType) ((FieldDeclaration) node)
          .getStructuralProperty(FieldDeclaration.TYPE_PROPERTY);
    case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
      return (SimpleType) ((VariableDeclarationExpression) node)
          .getStructuralProperty(VariableDeclarationExpression.TYPE_PROPERTY);
    case ASTNode.VARIABLE_DECLARATION_STATEMENT:
      return (SimpleType) ((VariableDeclarationStatement) node)
          .getStructuralProperty(VariableDeclarationStatement.TYPE_PROPERTY);
    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      return (SimpleType) ((SingleVariableDeclaration) node)
          .getStructuralProperty(SingleVariableDeclaration.TYPE_PROPERTY);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static ASTNode definedIn(ASTNode node, String name,
                                   ArrayList<Integer> constrains,
                                   ASTNode declaringClass) {
    if (node == null)
      return null;
    if (constrains != null) {
      System.out.println("Looking at " + getNodeAsString(node) + " for " + name
          + " in definedIn");
      if (!constrains.contains(node.getNodeType()) && constrains.size() > 0) {
        System.err.print("definedIn -1 " + " But constrain was ");
        for (Integer integer : constrains) {
          System.out.print(ASTNode.nodeClassForType(integer) + ",");
        }
        System.out.println();
        return null;
      }
    }

    List<VariableDeclarationFragment> vdfList = null;
    switch (node.getNodeType()) {

    case ASTNode.TYPE_DECLARATION:
      System.err.println(getNodeAsString(node));
      TypeDeclaration td = (TypeDeclaration) node;
      if (td.getName().toString().equals(name)) {
        if (constrains.contains(ASTNode.CLASS_INSTANCE_CREATION)) {
          // look for constructor;
          MethodDeclaration[] methods = td.getMethods();
          for (MethodDeclaration md : methods) {
            if (md.getName().toString().equals(name)) {
              return md;
            }
          }
        } else {
          // it's just the TD we're lookin for
          return node;
        }
      } else {
        if (constrains.contains(ASTNode.FIELD_DECLARATION)) {
          // look for fields
          FieldDeclaration[] fields = td.getFields();
          for (FieldDeclaration fd : fields) {
            List<VariableDeclarationFragment> fragments = fd.fragments();
            for (VariableDeclarationFragment vdf : fragments) {
              if (vdf.getName().toString().equals(name))
                return fd;
            }
          }
        } else if (constrains.contains(ASTNode.METHOD_DECLARATION)) {
          // look for methods
          MethodDeclaration[] methods = td.getMethods();
          for (MethodDeclaration md : methods) {
            if (md.getName().toString().equals(name)) {
              return md;
            }
          }
        }
      }
      break;
    case ASTNode.METHOD_DECLARATION:
      System.err.println(getNodeAsString(node));
      if (((MethodDeclaration) node).getName().toString().equals(name))
        return node;
      break;
    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      System.err.println(getNodeAsString(node));
      if (((SingleVariableDeclaration) node).getName().toString().equals(name))
        return node;
      break;
    case ASTNode.FIELD_DECLARATION:
      System.err.println("FD" + node);
      vdfList = ((FieldDeclaration) node).fragments();
      break;
    case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
      System.err.println("VDE" + node);
      vdfList = ((VariableDeclarationExpression) node).fragments();
      break;
    case ASTNode.VARIABLE_DECLARATION_STATEMENT:
      System.err.println("VDS" + node);
      vdfList = ((VariableDeclarationStatement) node).fragments();
      break;

    default:

    }
    if (vdfList != null) {
      for (VariableDeclarationFragment vdf : vdfList) {
        if (vdf.getName().toString().equals(name))
          return node;
      }
    }
    return null;
  }

  public static boolean isAddableASTNode(ASTNode node) {
    return true;
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

  public static String readFile(String path) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(
                                  new InputStreamReader(
                                                        new FileInputStream(
                                                                            new File(
                                                                                     path))));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    try {
      StringBuilder ret = new StringBuilder();
      // ret.append("package " + className + ";\n");
      String line;
      while ((line = reader.readLine()) != null) {
        ret.append(line);
        ret.append("\n");
      }
      return ret.toString();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

}
