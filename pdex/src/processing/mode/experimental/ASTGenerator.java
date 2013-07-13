package processing.mode.experimental;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import processing.app.Base;
import processing.app.SketchCode;
import processing.mode.java.preproc.PdePreprocessor;

import com.google.classpath.ClassPath;
import com.google.classpath.ClassPathFactory;
import com.google.classpath.RegExpResourceFilter;

public class ASTGenerator {

  protected ErrorCheckerService errorCheckerService;

  protected DebugEditor editor;

  public DefaultMutableTreeNode codeTree = new DefaultMutableTreeNode();

  private DefaultMutableTreeNode currentParent = null;

  /**
   * AST Window
   */
  private JFrame frame2;
  
  private JFrame frameAutoComp;

  /**
   * Swing component wrapper for AST, used for internal testing
   */
  private JTree jtree;
  
  /**
   * JTree used for testing refactoring operations
   */
  private JTree treeRename;

  private CompilationUnit compilationUnit;

  private JTable tableAuto;

  private JEditorPane javadocPane;

  private JScrollPane scrollPane;
  
  private JFrame frmRename;

  private JButton btnRename;
  
  private JButton btnListOccurrence;
  
  private JTextField txtRenameField;
  
  private JFrame frmOccurenceList;
  
  private JLabel lblRefactorOldName;
  
  public ASTGenerator(ErrorCheckerService ecs) {
    this.errorCheckerService = ecs;
    this.editor = ecs.getEditor();
    setupGUI();
    //addCompletionPopupListner();
    addListeners();
  }
  
  private void setupGUI(){
    frame2 = new JFrame();

    jtree = new JTree();
    frame2.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    frame2.setBounds(new Rectangle(680, 100, 460, 620));
    JScrollPane sp = new JScrollPane();
    sp.setViewportView(jtree);
    frame2.add(sp);

    btnRename = new JButton("Rename");
    btnListOccurrence = new JButton("Find All");
    frmRename = new JFrame();    
    frmRename.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    frmRename.setBounds(new Rectangle(680, 50, 250, 130));
    frmRename.setLayout(new BoxLayout(frmRename.getContentPane(), BoxLayout.Y_AXIS));
    JPanel panelTop = new JPanel(), panelBottom = new JPanel();
    panelTop.setLayout(new BoxLayout(panelTop, BoxLayout.Y_AXIS));
    panelTop.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    panelBottom.setLayout(new BoxLayout(panelBottom, BoxLayout.X_AXIS));
    panelBottom.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    panelBottom.add(Box.createHorizontalGlue());
    panelBottom.add(btnListOccurrence);
    panelBottom.add(Box.createRigidArea(new Dimension(15, 0)));
    panelBottom.add(btnRename);    
    frmRename.setTitle("Enter new name:");
    txtRenameField = new JTextField();
    txtRenameField.setPreferredSize(new Dimension(150, 60));
    panelTop.add(txtRenameField);
    //renameWindow.setVisible(true);
    lblRefactorOldName = new JLabel();
    lblRefactorOldName.setText("Old Name: ");
    panelTop.add(Box.createRigidArea(new Dimension(0, 10)));
    panelTop.add(lblRefactorOldName);
    frmRename.add(panelTop);
    frmRename.add(panelBottom);
    
    frmRename.setMinimumSize(frmRename.getSize());
    
    frmOccurenceList = new JFrame();
    frmOccurenceList.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    frmOccurenceList.setBounds(new Rectangle(1100, 50, 350, 500));
    
    JScrollPane sp2 = new JScrollPane();
    treeRename = new JTree();
    sp2.setViewportView(treeRename);    
    frmOccurenceList.add(sp2);    
    //occurenceListFrame.setVisible(true);
    
    frameAutoComp = new JFrame();
    frameAutoComp.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frameAutoComp.setBounds(new Rectangle(1280, 100, 460, 620));
    tableAuto = new JTable();
    JScrollPane sp3 = new JScrollPane();
    sp3.setViewportView(tableAuto);
    frameAutoComp.add(sp3);

//    jdocWindow = new JFrame();
//    jdocWindow.setTitle("P5 InstaHelp");
//    //jdocWindow.setUndecorated(true);
//    jdocWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
//    javadocPane = new JEditorPane();
//    javadocPane.setContentType("text/html");
//    javadocPane.setEditable(false);
//    scrollPane = new JScrollPane();
//    scrollPane.setViewportView(javadocPane);
//    jdocWindow.add(scrollPane);
//    jdocMap = new TreeMap<String, String>();
////    loadJars();
  }

  private DefaultMutableTreeNode buildAST(String source, CompilationUnit cu) {
    if (cu == null) {
      ASTParser parser = ASTParser.newParser(AST.JLS4);
      parser.setSource(source.toCharArray());
      parser.setKind(ASTParser.K_COMPILATION_UNIT);

      Map<String, String> options = JavaCore.getOptions();

      JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
      options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
      parser.setCompilerOptions(options);
      compilationUnit = (CompilationUnit) parser.createAST(null);
    } else {
      compilationUnit = cu;
      System.out.println("Other cu");
    }
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
					if (jtree.hasFocus() || frame2.hasFocus())
						return;
          jtree.setModel(new DefaultTreeModel(codeTree));
          ((DefaultTreeModel) jtree.getModel()).reload();
          if (!frame2.isVisible()) {
            frame2.setVisible(true);
          }
          if (!frameAutoComp.isVisible()) {

            frameAutoComp.setVisible(true);
            
          }
//          if (!jdocWindow.isVisible()) {
//            long t = System.currentTimeMillis();
//            loadJars();
//            loadJavaDoc();
//            System.out.println("Time taken: "
//                + (System.currentTimeMillis() - t));
//            jdocWindow.setBounds(new Rectangle(errorCheckerService.getEditor()
//                .getX() + errorCheckerService.getEditor().getWidth(),
//                                               errorCheckerService.getEditor()
//                                                   .getY(), 450, 600));
//            jdocWindow.setVisible(true);
//          }
          jtree.validate();
        }
      }
    };
    worker.execute();
//    System.err.println("++>" + System.getProperty("java.class.path"));
//    System.out.println(System.getProperty("java.class.path"));
//    System.out.println("-------------------------------");
    return codeTree;
  }

  private ClassPathFactory factory;

  /**
   * Used for searching for package declaration of a class 
   */
  private ClassPath classPath;

  private JFrame jdocWindow;

  /**
   * Loads up .jar files and classes defined in it for completion lookup
   */
  protected void loadJars() {
//    SwingWorker worker = new SwingWorker() {
//      protected void done(){        
//      }
//      protected Object doInBackground() throws Exception {
//        return null;        
//      }
//    };    
//    worker.execute();
    
    Thread t = new Thread(new Runnable() {

      public void run() {
        try {
          factory = new ClassPathFactory();

          StringBuffer tehPath = new StringBuffer(System
              .getProperty("java.class.path"));
          tehPath.append(File.pathSeparatorChar
              + System.getProperty("java.home") + "/lib/rt.jar"
              + File.pathSeparatorChar);
          if (errorCheckerService.classpathJars != null) {
            for (URL jarPath : errorCheckerService.classpathJars) {
              //System.out.println(jarPath.getPath());
              tehPath.append(jarPath.getPath() + File.pathSeparatorChar);
            }
          }

//          String paths[] = tehPath.toString().split(File.separatorChar +"");
//          StringTokenizer st = new StringTokenizer(tehPath.toString(),
//                                                   File.pathSeparatorChar + "");
//          while (st.hasMoreElements()) {
//            String sstr = (String) st.nextElement();
//            System.out.println(sstr);
//          }

          classPath = factory.createFromPath(tehPath.toString());
//          for (String packageName : classPath.listPackages("")) {
//            System.out.println(packageName);
//          }
          RegExpResourceFilter regExpResourceFilter = new RegExpResourceFilter(
                                                                               ".*",
                                                                               "ArrayList.class");
          String[] resources = classPath.findResources("", regExpResourceFilter);
          for (String className : resources) {
            System.out.println("-> " + className);
          }
          System.out.println("jars loaded.");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    t.start();
  }

  private TreeMap<String, String> jdocMap;

  private void loadJavaDoc() {

    // presently loading only p5 reference for PApplet
    Thread t = new Thread(new Runnable() {

      @Override
      public void run() {
        JavadocHelper.loadJavaDoc(jdocMap, editor.mode().getReferenceFolder());
      }
    });
    t.start();

  }

  public DefaultMutableTreeNode buildAST(CompilationUnit cu) {
    return buildAST(errorCheckerService.sourceCode, cu);
  }
  
  public static CompletionCandidate[] checkForTypes(ASTNode node) {

    List<VariableDeclarationFragment> vdfs = null;
    switch (node.getNodeType()) {
    case ASTNode.TYPE_DECLARATION:
      return new CompletionCandidate[] { new CompletionCandidate(
                                                                 getNodeAsString2(node),
                                                                 ((TypeDeclaration) node)
                                                                     .getName()
                                                                     .toString()) };

    case ASTNode.METHOD_DECLARATION:
      MethodDeclaration md = (MethodDeclaration) node;
      System.out.println(getNodeAsString(md));
      List<ASTNode> params = (List<ASTNode>) md
          .getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY);
      CompletionCandidate[] cand = new CompletionCandidate[params.size() + 1];
      cand[0] = new CompletionCandidate(md);
      for (int i = 0; i < params.size(); i++) {
        cand[i + 1] = new CompletionCandidate(params.get(i).toString(), "", "",
                                              CompletionCandidate.LOCAL_VAR);
      }
      return cand;

    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      return new CompletionCandidate[] { new CompletionCandidate(
                                                                 getNodeAsString2(node),
                                                                 "",
                                                                 "",
                                                                 CompletionCandidate.LOCAL_VAR) };

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
      CompletionCandidate ret[] = new CompletionCandidate[vdfs.size()];
      int i = 0;
      for (VariableDeclarationFragment vdf : vdfs) {
        ret[i++] = new CompletionCandidate(getNodeAsString2(vdf), "", "",
                                           CompletionCandidate.LOCAL_VAR);
      }
      return ret;
    }

    return null;
  }

  /**
   * Find the parent of the expression in a().b, this would give me the return
   * type of a(), so that we can find all children of a() begininng with b
   * 
   * @param nearestNode
   * @param expression
   * @return
   */
  public static ASTNode resolveExpression(ASTNode nearestNode,
                                          ASTNode expression, boolean noCompare) {
    System.out.println("Resolving " + getNodeAsString(expression) + " noComp "
        + noCompare);
    if (expression instanceof SimpleName) {
      return findDeclaration2(((SimpleName) expression), nearestNode);
    } else if (expression instanceof MethodInvocation) {
      System.out.println("3. Method Invo "
          + ((MethodInvocation) expression).getName());
      return findDeclaration2(((MethodInvocation) expression).getName(),
                              nearestNode);
    } else if (expression instanceof FieldAccess) {
      System.out.println("2. Field access "
          + getNodeAsString(((FieldAccess) expression).getExpression()) + "|||"
          + getNodeAsString(((FieldAccess) expression).getName()));
      if (noCompare) {
        /*
         * ASTNode ret = findDeclaration2(((FieldAccess) expression).getName(),
         * nearestNode); System.out.println("Found as ->"+getNodeAsString(ret));
         * return ret;
         */
        return findDeclaration2(((FieldAccess) expression).getName(),
                                nearestNode);
      } else {

        /*
         * Note how for the next recursion, noCompare is reversed. Let's say
         * I've typed getABC().quark.nin where nin is incomplete(ninja being the
         * field), when execution first enters here, it calls resolveExpr again
         * for "getABC().quark" where we know that quark field must be complete,
         * so we toggle noCompare. And kaboom.
         */
        return resolveExpression(nearestNode,
                                 ((FieldAccess) expression).getExpression(),
                                 !noCompare);
      }
      //return findDeclaration2(((FieldAccess) expression).getExpression(), nearestNode);
    } else if (expression instanceof QualifiedName) {
      System.out.println("1. Resolving "
          + ((QualifiedName) expression).getQualifier() + " ||| "
          + ((QualifiedName) expression).getName());
      if (noCompare) { // no compare, as in "abc.hello." need to resolve hello here
        return findDeclaration2(((QualifiedName) expression).getName(),
                                nearestNode);
      } else { 
        //User typed "abc.hello.by" (bye being complete), so need to resolve "abc.hello." only
        return findDeclaration2(((QualifiedName) expression).getQualifier(),
                          nearestNode);
      }
    }

    return null;
  }
  
  /**
   * Finds the type of the expression in foo.bar().a().b, this would give me the
   * type of b if it exists in return type of a(). If noCompare is true,
   * it'll return type of a()
   * @param nearestNode
   * @param astNode
   * @return
   */
  public ClassMember resolveExpression3rdParty(ASTNode nearestNode,
                                          ASTNode astNode, boolean noCompare) {
    System.out.println("Resolve 3rdParty expr-- " + getNodeAsString(astNode)
        + " nearest node " + getNodeAsString(nearestNode));
    
    ClassMember scopeParent = null;
    SimpleType stp = null;
    if(astNode instanceof SimpleName){
      ASTNode decl = findDeclaration2(((SimpleName)astNode),nearestNode);
      if(decl != null){
        // see if locally defined
        System.out.println(getNodeAsString(astNode)+" found decl -> " + getNodeAsString(decl));
        return new ClassMember(extracTypeInfo(decl));
      }
      else {
        // or in a predefined class?
        Class tehClass = findClassIfExists(((SimpleName) astNode).toString());
        if (tehClass != null) {
          return new ClassMember(tehClass);
        }
      }
      astNode = astNode.getParent();
    }
    switch (astNode.getNodeType()) {
    //TODO: Notice the redundancy in the 3 cases, you can simplify things even more.
    case ASTNode.FIELD_ACCESS:
      FieldAccess fa = (FieldAccess) astNode;
      if (fa.getExpression() == null) {
        // Local code or belongs to super class
        System.out.println("FA,Not implemented.");
        return null;
      } else { 
        if (fa.getExpression() instanceof SimpleName) {
          stp = extracTypeInfo(findDeclaration2((SimpleName) fa.getExpression(),
                                                nearestNode));
          if(stp == null){
            /*The type wasn't found in local code, so it might be something like
             * System.out.println(), or maybe belonging to super class, etc.
             */
            Class tehClass = findClassIfExists(((SimpleName)fa.getExpression()).toString());
            if (tehClass != null) {
              // Method Expression is a simple name and wasn't located locally, but found in a class
              // so look for method in this class.
              return definedIn3rdPartyClass(new ClassMember(tehClass), fa
                  .getName().toString());
            }
            System.out.println("FA resolve 3rd par, Can't resolve " + fa.getExpression());
            
            return null;
          }
          System.out.println("FA, SN Type " + getNodeAsString(stp));
          scopeParent = definedIn3rdPartyClass(stp.getName().toString(), "THIS");
          
        } else {
          scopeParent = resolveExpression3rdParty(nearestNode,
                                                  fa.getExpression(), noCompare);
        }
        System.out.println("FA, ScopeParent " + scopeParent);
        return definedIn3rdPartyClass(scopeParent, fa.getName().toString());
      }
    case ASTNode.METHOD_INVOCATION:
      MethodInvocation mi = (MethodInvocation) astNode;
      ASTNode temp = findDeclaration2(mi.getName(), nearestNode);
      if(temp instanceof MethodDeclaration){
        // method is locally defined
        System.out.println(mi.getName() + " was found locally," + getNodeAsString(extracTypeInfo(temp)));
        return new ClassMember(extracTypeInfo(temp));
      }
      if (mi.getExpression() == null) {
        //Local code or belongs to super class
        System.out.println("MI,Not implemented.");
        return null;
      } else { 
        if (mi.getExpression() instanceof SimpleName) {
          stp = extracTypeInfo(findDeclaration2((SimpleName) mi.getExpression(),
                                                nearestNode));
          if(stp == null){
            /*The type wasn't found in local code, so it might be something like
             * System.console()., or maybe belonging to super class, etc.
             */
            Class tehClass = findClassIfExists(((SimpleName)mi.getExpression()).toString());
            if (tehClass != null) {
              // Method Expression is a simple name and wasn't located locally, but found in a class
              // so look for method in this class.
              return definedIn3rdPartyClass(new ClassMember(tehClass), mi
                  .getName().toString());
            }
            System.out.println("MI resolve 3rd par, Can't resolve " + mi.getExpression());
            return null;
          }
          System.out.println("MI, SN Type " + getNodeAsString(stp));
          ASTNode typeDec = findDeclaration2(stp.getName(),nearestNode);
          if(typeDec == null){
            System.out.println(stp.getName() + " couldn't be found locally..");
            Class tehClass = findClassIfExists(stp.getName().toString());
            if (tehClass != null) {
              // Method Expression is a simple name and wasn't located locally, but found in a class
              // so look for method in this class.
              return definedIn3rdPartyClass(new ClassMember(tehClass), mi
                  .getName().toString());
            }
            //return new ClassMember(findClassIfExists(stp.getName().toString()));
          }
          //scopeParent = definedIn3rdPartyClass(stp.getName().toString(), "THIS");
          return new ClassMember(typeDec);
        } else {
          System.out.println("MI EXP.."+getNodeAsString(mi.getExpression()));
//          return null;
          scopeParent = resolveExpression3rdParty(nearestNode,
                                                  mi.getExpression(), noCompare);
          System.out.println("MI, ScopeParent " + scopeParent);
          return definedIn3rdPartyClass(scopeParent, mi.getName().toString());
        }
        
      }
    case ASTNode.QUALIFIED_NAME:
      QualifiedName qn = (QualifiedName) astNode;
      ASTNode temp2 = findDeclaration2(qn.getName(), nearestNode);
      if(temp2 instanceof FieldDeclaration){
        // field is locally defined
        System.out.println(qn.getName() + " was found locally," + getNodeAsString(extracTypeInfo(temp2)));
        return new ClassMember(extracTypeInfo(temp2));
      }
      if (qn.getQualifier() == null) {
        System.out.println("MI,Not implemented.");
        return null;
      } else  {
        
        if (qn.getQualifier() instanceof SimpleName) {
          stp = extracTypeInfo(findDeclaration2(qn.getQualifier(), nearestNode));
          if(stp == null){
            /*The type wasn't found in local code, so it might be something like
             * System.out.println(), or maybe belonging to super class, etc.
             */
            Class tehClass = findClassIfExists(qn.getQualifier().toString());
            if (tehClass != null) {
              // note how similar thing is called on line 690. Check check.
              return definedIn3rdPartyClass(new ClassMember(tehClass), qn
                  .getName().toString());
            }
            System.out.println("QN resolve 3rd par, Can't resolve " + qn.getQualifier());
            return null;
          }
          System.out.println("QN, SN Local Type " + getNodeAsString(stp));
          //scopeParent = definedIn3rdPartyClass(stp.getName().toString(), "THIS");
          ASTNode typeDec = findDeclaration2(stp.getName(),nearestNode);
          if(typeDec == null){
            System.out.println(stp.getName() + " couldn't be found locally..");
            return new ClassMember(findClassIfExists(stp.getName().toString()));
          }
          return new ClassMember(typeDec);
        } else {
          scopeParent = resolveExpression3rdParty(nearestNode,
                                                  qn.getQualifier(), noCompare);
          System.out.println("QN, ScopeParent " + scopeParent);
          return definedIn3rdPartyClass(scopeParent, qn.getName().toString());
        }
        
      }
    default:
      System.out.println("Unaccounted type " + getNodeAsString(astNode));
      break;
    }
    
    return null;
  }
  
  /**
   * For a().abc.a123 this would return a123
   * 
   * @param expression
   * @return
   */
  public static ASTNode getChildExpression(ASTNode expression) {
//    ASTNode anode = null;
    if (expression instanceof SimpleName) {
      return expression;
    } else if (expression instanceof FieldAccess) {
      return ((FieldAccess) expression).getName();
    } else if (expression instanceof QualifiedName) {
      return ((QualifiedName) expression).getName();
    }else if (expression instanceof MethodInvocation) {
      return ((MethodInvocation) expression).getName();
    }
    System.out.println(" getChildExpression returning NULL for "
        + getNodeAsString(expression));
    return null;
  }
  
  public static ASTNode getParentExpression(ASTNode expression) {
//  ASTNode anode = null;
  if (expression instanceof SimpleName) {
    return expression;
  } else if (expression instanceof FieldAccess) {
    return ((FieldAccess) expression).getExpression();
  } else if (expression instanceof QualifiedName) {
    return ((QualifiedName) expression).getQualifier();
  } else if (expression instanceof MethodInvocation) {
    return ((MethodInvocation) expression).getExpression();
  }
  System.out.println("getParentExpression returning NULL for "
      + getNodeAsString(expression));
  return null;
}

  public void updatePredictions(final String word, final int line, final int lineStartNonWSOffset) {
    SwingWorker worker = new SwingWorker() {

      @Override
      protected Object doInBackground() throws Exception {
        return null;
      }

      protected void done() {
        // If the parsed code contains pde enhancements, take 'em out.
        String word2 = ASTNodeWrapper.getJavaCode(word);

        //After typing 'arg.' all members of arg type are to be listed. This one is a flag for it        
        boolean noCompare = false;
        if (word2.endsWith(".")) {
          // return all matches
          word2 = word2.substring(0, word2.length() - 1);
          noCompare = true;
        }
        
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

        // Now parse the expression into an ASTNode object
        ASTNode nearestNode = null;
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setKind(ASTParser.K_EXPRESSION);
        parser.setSource(word2.toCharArray());
        ASTNode testnode = parser.createAST(null);
        //System.err.println("PREDICTION PARSER PROBLEMS: " + parser);
        // Find closest ASTNode of the document to this word
        System.err.print("Typed: " + word2 + "|");
        nearestNode = findClosestNode(lineNumber, (ASTNode) compilationUnit.types()
            .get(0));
        if (nearestNode == null)
          //Make sure nearestNode is not NULL if couldn't find a closeset node
          nearestNode = (ASTNode) compilationUnit.types().get(0);
        System.err.println(lineNumber + " Nearest ASTNode to PRED "
            + getNodeAsString(nearestNode));

        ArrayList<CompletionCandidate> candidates = new ArrayList<CompletionCandidate>();

        // Determine the expression typed
        
        if (testnode instanceof SimpleName && !noCompare) {
          System.err
              .println("One word expression " + getNodeAsString(testnode));
          //==> Simple one word exprssion - so is just an identifier
          
          // Bottom up traversal of the AST to look for possible definitions at 
          // higher levels.
          nearestNode = nearestNode.getParent();
          while (nearestNode != null) {
            // If the current class has a super class, look inside it for
            // definitions.
            if (nearestNode instanceof TypeDeclaration) {
              TypeDeclaration td = (TypeDeclaration) nearestNode;
              if (td
                  .getStructuralProperty(TypeDeclaration.SUPERCLASS_TYPE_PROPERTY) != null) {
                SimpleType st = (SimpleType) td
                    .getStructuralProperty(TypeDeclaration.SUPERCLASS_TYPE_PROPERTY);
                System.out.println("Superclass " + st.getName());
                for (CompletionCandidate can : getMembersForType(st.getName()
                    .toString(), word2, noCompare, false)) {
                  candidates.add(can);
                }
                //findDeclaration(st.getName())

              }
            }
            List<StructuralPropertyDescriptor> sprops = nearestNode
                .structuralPropertiesForType();
            for (StructuralPropertyDescriptor sprop : sprops) {
              ASTNode cnode = null;
              if (!sprop.isChildListProperty()) {
                if (nearestNode.getStructuralProperty(sprop) instanceof ASTNode) {
                  cnode = (ASTNode) nearestNode.getStructuralProperty(sprop);
                  CompletionCandidate[] types = checkForTypes(cnode);
                  if (types != null) {
                    for (int i = 0; i < types.length; i++) {
                      if (types[i].getElementName().startsWith(word2))
                        candidates.add(types[i]);
                    }
                  }
                }
              } else {
                // Childlist prop
                List<ASTNode> nodelist = (List<ASTNode>) nearestNode
                    .getStructuralProperty(sprop);
                for (ASTNode clnode : nodelist) {
                  CompletionCandidate[] types = checkForTypes(clnode);
                  if (types != null) {
                    for (int i = 0; i < types.length; i++) {
                      if (types[i].getElementName().startsWith(word2))
                        candidates.add(types[i]);
                    }
                  }
                }
              }
            }
            nearestNode = nearestNode.getParent();
          }
          if(candidates.isEmpty()){
            // We're seeing a simple name that's not defined locally or in
            // the parent class. So most probably a pre-defined type.
            System.out.println("Empty can. " + word2);
            RegExpResourceFilter regExpResourceFilter;
            regExpResourceFilter = new RegExpResourceFilter(".*", word2 + "[a-zA-Z_0-9]*.class");
            String[] resources = classPath.findResources("", regExpResourceFilter);
            for (String matchedClass : resources) {
              matchedClass = matchedClass.substring(0,
                                                    matchedClass.length() - 6);
              matchedClass = matchedClass.replace('/', '.');
              int d = matchedClass.lastIndexOf('.');
              matchedClass = matchedClass.substring(d + 1);
              candidates.add(new CompletionCandidate(matchedClass));
              //System.out.println("-> " + className);
            }
          }

        } else {

          // ==> Complex expression of type blah.blah2().doIt,etc
          // Have to resolve it by carefully traversing AST of testNode
          System.err.println("Complex expression " + getNodeAsString(testnode));
          System.out.println("candidates empty");
          String childExpr = getChildExpression(testnode)
              .toString();
          System.out.println("Parent expression : " + getParentExpression(testnode));
          System.out.println("Child expression : " + childExpr);
          
          if(!noCompare){
            System.out.println("Original testnode " + getNodeAsString(testnode));
            testnode = getParentExpression(testnode);
            System.out.println("Corrected testnode " + getNodeAsString(testnode));
          }
          ClassMember expr = resolveExpression3rdParty(nearestNode, testnode, noCompare);
          if(expr == null){
            System.out.println("Expr is null");
          }else {
            System.out.println("Expr is " + expr.toString());
            candidates = getMembersForType(expr,
                                         childExpr, noCompare, false);
          }
          /*ASTNode det = resolveExpression(nearestNode, testnode,noCompare);
          // Find the parent of the expression
          // in a().b, this would give me the return type of a(), so that we can 
          // find all children of a() begininng with b
          System.err.println("DET " + getNodeAsString(det));
          if (det != null) {
            TypeDeclaration td = null;
            SimpleType stp = extracTypeInfo(det);
            if (det instanceof MethodDeclaration) {
              if (((MethodDeclaration) det).getReturnType2() instanceof SimpleType) {
                stp = (SimpleType) (((MethodDeclaration) det).getReturnType2());
                td = (TypeDeclaration) findDeclaration(stp.getName());
              }
            } else if (det instanceof FieldDeclaration) {
              if (((FieldDeclaration) det).getType() instanceof SimpleType) {
                stp = (SimpleType) (((FieldDeclaration) det).getType());
                td = (TypeDeclaration) findDeclaration(stp.getName());
              }
            } else if (det instanceof VariableDeclarationStatement) {
              stp = (SimpleType) (((VariableDeclarationStatement) det)
                  .getType());
              td = (TypeDeclaration) findDeclaration(stp.getName());
            }
            System.out.println("ST is " + stp.getName());
            // Now td contains the type returned by a()
            System.err.println(getNodeAsString(det) + " defined in "
                + getNodeAsString(td));
            ASTNode child = resolveChildExpression(testnode);
            if (td != null) {

              System.out.println("Completion candidate: "
                  + getNodeAsString(child));
              for (int i = 0; i < td.getFields().length; i++) {
                List<VariableDeclarationFragment> vdfs = td.getFields()[i]
                    .fragments();
                for (VariableDeclarationFragment vdf : vdfs) {
                  if (noCompare) {
                    candidates
                        .add(new CompletionCandidate(getNodeAsString2(vdf)));
                  } else if (vdf.getName().toString()
                      .startsWith(child.toString()))
                    candidates
                        .add(new CompletionCandidate(getNodeAsString2(vdf)));
                }

              }
              for (int i = 0; i < td.getMethods().length; i++) {
                if (noCompare) {
                  candidates
                      .add(new CompletionCandidate(getNodeAsString2(td
                          .getMethods()[i]), td.getName().toString(), "",
                                                   CompletionCandidate.METHOD));
                } else if (td.getMethods()[i].getName().toString()
                    .startsWith(child.toString()))
                  candidates
                      .add(new CompletionCandidate(getNodeAsString2(td
                          .getMethods()[i]), td.getName().toString(), "",
                                                   CompletionCandidate.METHOD));
              }
            } else {
              if (stp != null) {
                candidates = getMembersForType(stp.getName().toString(),
                                               child.toString(), noCompare,
                                               false);
              }
            }

          } else if (word.length() - word2.length() == 1) {
            System.out.println(word + " w2 " + word2);
//            int dC = 0;
//            for (int i = 0; i < word.length(); i++) {
//              if(word.charAt(i) == '.')
//                dC++;              
//            }
//            if(dC == 1 && word.charAt(word.length() - 1) == '.'){
            System.out.println("All members of " + word2);
            candidates = getMembersForType(word2, "", true, true);
//            }
          } else {
            System.out.println("Some members of " + word2);
            int x = word2.indexOf('.');
            if (x != -1) {
              candidates = getMembersForType(word2.substring(0, x),
                                             word2.substring(x + 1), false,
                                             true);
            }
          }
          if(candidates.size() == 0){
            System.out.println("candidates empty");
            String childExpr = resolveChildExpression(testnode)
                .toString();
            System.out.println("Parent expression : " + resolveParentExpression(testnode));
            System.out.println("Child expression : " + childExpr);
            
            if(!noCompare){
              System.out.println("Original testnode " + getNodeAsString(testnode));
              testnode = resolveParentExpression(testnode);
              System.out.println("Corrected testnode " + getNodeAsString(testnode));
            }
            ClassMember expr = resolveExpression3rdParty(nearestNode, testnode, noCompare);
            if(expr == null){
              System.out.println("Expr is null");
            }else {
              System.out.println("Expr is " + expr.toString());
              candidates = getMembersForType(expr,
                                           childExpr, noCompare, false);
            }
          }*/
        }
        
       
        
        Collections.sort(candidates);
        CompletionCandidate[][] candi = new CompletionCandidate[candidates
            .size()][1];
        
        DefaultListModel defListModel = new DefaultListModel();

        for (int i = 0; i < candi.length; i++) {
          candi[i][0] = candidates.get(i);
          defListModel.addElement(candidates.get(i));
        }
        System.out.println("K = " + candidates.size());
        DefaultTableModel tm = new DefaultTableModel(
                                                     candi,
                                                     new String[] { "Suggestions" });
        tableAuto.setModel(tm);
        tableAuto.validate();
        tableAuto.repaint();
//        CompletionCandidate[] candidatesArray = candidates
//            .toArray(new CompletionCandidate[candidates.size()]);
        errorCheckerService.getEditor().textArea()
            .showSuggestion(defListModel,word);
      }
    };

    worker.execute();

  }

  /**
   * Loads classes from .jar files in sketch classpath
   * 
   * @param typeName
   * @param child
   * @param noCompare
   * @return
   */
  public ArrayList<CompletionCandidate> getMembersForType(String typeName,
                                                          String child,
                                                          boolean noCompare,
                                                          boolean staticOnly) {
    
    ArrayList<CompletionCandidate> candidates = new ArrayList<CompletionCandidate>();
    System.out.println("In GMFT(), Looking for match " + child.toString()
        + " in class " + typeName + " noCompare " + noCompare + " staticOnly "
        + staticOnly);
    Class<?> probableClass = findClassIfExists(typeName);
    if(probableClass == null){
      System.out.println("In GMFT(), class not found.");
      return candidates;
    }
   return getMembersForType(new ClassMember(probableClass), child, noCompare, staticOnly);
   
  }
  
  public ArrayList<CompletionCandidate> getMembersForType(ClassMember tehClass,
                                                          String child,
                                                          boolean noCompare,
                                                          boolean staticOnly) {
    ArrayList<CompletionCandidate> candidates = new ArrayList<CompletionCandidate>();
    System.out.println("getMemFoType-> Looking for match " + child.toString()
        + " inside " + tehClass + " noCompare " + noCompare + " staticOnly "
        + staticOnly);

    // tehClass will either be a TypeDecl defined locally
    if(tehClass.getDeclaringNode() instanceof TypeDeclaration){
      
      TypeDeclaration td = (TypeDeclaration) tehClass.getDeclaringNode();
      for (int i = 0; i < td.getFields().length; i++) {
        List<VariableDeclarationFragment> vdfs = td.getFields()[i]
            .fragments();
        for (VariableDeclarationFragment vdf : vdfs) {
          if (noCompare) {
            candidates
                .add(new CompletionCandidate(getNodeAsString2(vdf)));
          } else if (vdf.getName().toString()
              .startsWith(child.toString()))
            candidates
                .add(new CompletionCandidate(getNodeAsString2(vdf)));
        }

      }
      for (int i = 0; i < td.getMethods().length; i++) {
        if (noCompare) {
          candidates
              .add(new CompletionCandidate(getNodeAsString2(td
                  .getMethods()[i]), td.getName().toString(), "",
                                           CompletionCandidate.METHOD));
        } else if (td.getMethods()[i].getName().toString()
            .startsWith(child.toString()))
          candidates
              .add(new CompletionCandidate(getNodeAsString2(td
                  .getMethods()[i]), td.getName().toString(), "",
                                           CompletionCandidate.METHOD));
      }
      
      ArrayList<CompletionCandidate> superClassCandidates = new ArrayList<CompletionCandidate>();
      if(td.getSuperclassType() instanceof Type){
        System.out.println(getNodeAsString(td.getSuperclassType()) + " <-Looking into superclass of " + tehClass);
        superClassCandidates = getMembersForType(new ClassMember(td
                                                     .getSuperclassType()),
                                                 child, noCompare, staticOnly);        
      }
      else
      {
        superClassCandidates = getMembersForType(new ClassMember(Object.class),
                                                 child, noCompare, staticOnly);
      }
      for (CompletionCandidate cc : superClassCandidates) {
        candidates.add(cc);
      }
      return candidates;
    }
    
    // Or tehClass will be a predefined class
    try {
      Class<?> probableClass;
      if(tehClass.getClass_() != null){
        probableClass = tehClass.getClass_();
      }
      else
      {
        probableClass = findClassIfExists(tehClass.getTypeAsString());
        System.out.println("Loaded " + probableClass.toString());
      }
      for (Method method : probableClass.getMethods()) {
        if (!Modifier.isStatic(method.getModifiers()) && staticOnly) {
          continue;
        }
        
        StringBuffer label = new StringBuffer(method.getName() + "(");
        for (int i = 0; i < method.getParameterTypes().length; i++) {
          label.append(method.getParameterTypes()[i].getSimpleName());
          if (i < method.getParameterTypes().length - 1)
            label.append(",");
        }
        label.append(")");
        if (noCompare) {
          candidates.add(new CompletionCandidate(method));
        }
        else if (label.toString().startsWith(child.toString())) {
          candidates.add(new CompletionCandidate(method));
        }
      }
      for (Field field : probableClass.getFields()) {
        if (!Modifier.isStatic(field.getModifiers()) && staticOnly) {
          continue;
        }
        if (noCompare) {
          candidates.add(new CompletionCandidate(field));
        }
        else if (field.getName().startsWith(child.toString())) {
          candidates.add(new CompletionCandidate(field));
        }
      }
      return candidates;
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Couldn't load " + tehClass);
    }
    return null;
  }
  
  /**
   * Searches for the particular class in the default list of imports as well as
   * the Sketch classpath
   * @param className
   * @return
   */
  private Class findClassIfExists(String className){
    Class tehClass = null;
    // First, see if the classname is a fully qualified name and loads straightaway
    tehClass = loadClass(className);
    if(tehClass instanceof Class){
      System.out.println(tehClass.getName() + " located straightaway");
      return tehClass;
    }
    
    System.out.println("Looking in the classloader for " + className);
    ArrayList<ImportStatement> imports = errorCheckerService
        .getProgramImports();

    for (ImportStatement impS : imports) {
      String temp = impS.getPackageName();

      if (temp.endsWith("*")) {
        temp = temp.substring(0, temp.length() - 1) + className;
      } else {
        int x = temp.lastIndexOf('.');
        if (temp.substring(x).equals(className)) {
          // Well, we've found the class.
        }
      }
      tehClass = loadClass(temp);
      if (tehClass instanceof Class) {
        System.out.println(tehClass.getName() + " located.");
        return tehClass;
      }

      System.out.println("Doesn't exist in package: " + impS.getImportName());

    }
    
    PdePreprocessor p = new PdePreprocessor(null);
    for (String impS : p.getCoreImports()) {
      tehClass = loadClass(impS.substring(0,impS.length()-1) + className);
      if (tehClass instanceof Class) {
        System.out.println(tehClass.getName() + " located.");
        return tehClass;
      }
      System.out.println("Doesn't exist in package: " + impS);
    }
    
    for (String impS : p.getDefaultImports()) {
      if(className.equals(impS) || impS.endsWith(className)){
        tehClass = loadClass(impS);                    
        if (tehClass instanceof Class) {
          System.out.println(tehClass.getName() + " located.");
          return tehClass;
        }
        System.out.println("Doesn't exist in package: " + impS);
      }
    }
    
    // And finally, the daddy
    String daddy = "java.lang." + className;
    tehClass = loadClass(daddy);                    
    if (tehClass instanceof Class) {
      System.out.println(tehClass.getName() + " located.");
      return tehClass;
    }
    System.out.println("Doesn't exist in java.lang");
    
    return tehClass;
  }
  
  private Class loadClass(String className){
    Class tehClass = null;
    try {      
      tehClass = Class.forName(className, false,
                                     errorCheckerService
                                         .getSketchClassLoader());
    } catch (ClassNotFoundException e) {
      //System.out.println("Doesn't exist in package: ");
    }
    return tehClass;
  }
  
  public ClassMember definedIn3rdPartyClass(String className,String memberName){
    Class<?> probableClass = findClassIfExists(className);
    if (probableClass == null) {
      System.out.println("Couldn't load " + className);
      return null;
    }
    if (memberName.equals("THIS")) {
      return new ClassMember(probableClass);
    } else {
      return definedIn3rdPartyClass(new ClassMember(probableClass), memberName);
    }
  }
  
  public ClassMember definedIn3rdPartyClass(ClassMember tehClass,String memberName){
    if(tehClass == null)
      return null;
    System.out.println("definedIn3rdPartyClass-> Looking for " + memberName
        + " in " + tehClass);
    if(tehClass.getDeclaringNode() instanceof TypeDeclaration){
      
      TypeDeclaration td = (TypeDeclaration) tehClass.getDeclaringNode();
      for (int i = 0; i < td.getFields().length; i++) {
        List<VariableDeclarationFragment> vdfs = td.getFields()[i]
            .fragments();
        for (VariableDeclarationFragment vdf : vdfs) {
          if (vdf.getName().toString()
              .startsWith(memberName))
            return new ClassMember(vdf);
        }

      }
      for (int i = 0; i < td.getMethods().length; i++) {
       if (td.getMethods()[i].getName().toString()
            .startsWith(memberName))
         return new ClassMember(td.getMethods()[i]);
      }
      return null;
    }
    
    Class probableClass = null;
    if(tehClass.getClass_() != null){
      probableClass = tehClass.getClass_();
    }
    else
    {
      probableClass = findClassIfExists(tehClass.getTypeAsString());
      System.out.println("Loaded " + probableClass.toString());
    }
    for (Method method : probableClass.getMethods()) {        
      if (method.getName().equals(memberName)) {
        return new ClassMember(method);
      }
    }
    for (Field field : probableClass.getFields()) {
      if (field.getName().equals(memberName)) {
        return new ClassMember(field);
      }
    }
    return null;
  }
  
//  private ClassMember loadClass(String className){
//    RegExpResourceFilter regExpResourceFilter;
//    regExpResourceFilter = new RegExpResourceFilter(".*", className + ".class");
//    String[] resources = classPath.findResources("", regExpResourceFilter);
//    for (String cn : resources) {
//      System.out.println("-> " + cn);
//    }
//    if (resources.length == 0) {
//      System.out.println("In defIn3rdPar(), couldn't find class: " + className);
//      return null;
//    }
//    //TODO: Multiple matched classes? What about 'em?
//    String matchedClass = resources[0];
//    matchedClass = matchedClass.substring(0, matchedClass.length() - 6);
//    matchedClass = matchedClass.replace('/', '.');
//    System.out.println("In defIn3rdPar(), Matched class: " + matchedClass);
//    
//    System.out.println("Trying to load class " + className);
//    try {
//      Class<?> probableClass = Class.forName(className, false,
//                                             errorCheckerService.classLoader);
//        return new ClassMember(probableClass); 
//    } catch (ClassNotFoundException e) {
//      
//      System.out.println("Couldn't load " + className);
//      e.printStackTrace();
//    }
//    return null;
//  }

  public void updateJavaDoc(final CompletionCandidate candidate) {
    //TODO: Work on this later.
    return;
    /*String methodmatch = candidate.toString();
    if (methodmatch.indexOf('(') != -1) {
      methodmatch = methodmatch.substring(0, methodmatch.indexOf('('));
    }

    //System.out.println("jdoc match " + methodmatch);
    for (final String key : jdocMap.keySet()) {
      if (key.startsWith(methodmatch) && key.length() > 3) {
        System.out.println("Matched jdoc " + key);

        //visitRecur((ASTNode) compilationUnit.types().get(0), codeTree);
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {

            System.out.println("Class: " + candidate.getDefiningClass());
            if (candidate.getDefiningClass().equals("processing.core.PApplet")) {
              javadocPane.setText(jdocMap.get(key));
              //jdocWindow.setVisible(true);
              //editor.textArea().requestFocus()
            } else
              javadocPane.setText("");
            javadocPane.setCaretPosition(0);
          }
        });
        break;
      }
    }*/
    //jdocWindow.setVisible(false);

  }

  @SuppressWarnings("unchecked")
  private static ASTNode findClosestParentNode(int lineNumber, ASTNode node) {
    Iterator<StructuralPropertyDescriptor> it = node
        .structuralPropertiesForType().iterator();
    System.err.println("Props of " + node.getClass().getName());
    while (it.hasNext()) {
      StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) it
          .next();

      if (prop.isChildProperty() || prop.isSimpleProperty()) {
        if (node.getStructuralProperty(prop) != null) {
//          System.out
//              .println(node.getStructuralProperty(prop) + " -> " + (prop));
          if (node.getStructuralProperty(prop) instanceof ASTNode) {
            ASTNode cnode = (ASTNode) node.getStructuralProperty(prop);
//            System.out.println("Looking at " + getNodeAsString(cnode)+ " for line num " + lineNumber);
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
//          System.out.println("Looking at " + getNodeAsString(cnode)+ " for line num " + lineNumber);
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

    if (nodes.size() > 0) {
      ASTNode retNode = nodes.get(0);
      for (ASTNode cNode : nodes) {
        if (getLineNumber(cNode) <= lineNumber)
          retNode = cNode;
        else
          break;
      }

      return retNode;
    }
    return null;
  }

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

  /**
   * 
   * @param lineNumber
   * @param name
   * @param offset - line start nonwhitespace offset
   * @param scrollOnly
   * @return
   */
  public ASTNodeWrapper getASTNodeAt(int lineNumber, String name, int offset,
                                     boolean scrollOnly) {
    
    System.out.println("----getASTNodeAt----");
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
    String nameOfNode = null; // The node name which is to be scrolled to
    if (lineNode != null) {
      
      // Some delicate offset handling follows.
      ASTNodeWrapper lineNodeWrap = new ASTNodeWrapper(lineNode);
      int altOff = offset;
      int ret[][] = lineNodeWrap.getOffsetMapping(errorCheckerService);
      if(ret != null){
        altOff = 0;
        int javaCodeMap[] = ret[0], pdeCodeMap[] = ret[1];

        for (; altOff < javaCodeMap.length; altOff++) {
          if (javaCodeMap[altOff] == pdeCodeMap[offset]) {
            break;
          }
        }
      }
      System.out.println("FLON2: " + lineNumber + " LN spos "
          + lineNode.getStartPosition() + " off " + offset + " alt off" + altOff);
      /* 
       * Now I need to see if multiple statements exist with this same line number
       * If that's the case, I need to ensure the offset is right. 
       */
      ASTNode parLineNode = lineNode.getParent(); 
      
      Iterator<StructuralPropertyDescriptor> it = parLineNode
          .structuralPropertiesForType().iterator();
      boolean flag = true;
      int offAdjust = 0;
      while (it.hasNext() && flag) {
        StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) it
            .next();
        if (prop.isChildListProperty()) {
          List<ASTNode> nodelist = (List<ASTNode>) parLineNode
              .getStructuralProperty(prop);
          for (ASTNode cnode : nodelist) {
            if (getLineNumber(cnode) == lineNumber) {
              if (cnode.getStartPosition() <= lineNode.getStartPosition()
                  + altOff
                  && cnode.getStartPosition() + cnode.getLength() > lineNode
                      .getStartPosition() + altOff) {
                System.out.println(cnode);
                offAdjust = cnode.getStartPosition() - lineNode.getStartPosition();
                lineNode = cnode;
                altOff -= offAdjust;
                flag = false;
                break;
              }
              
            }
          }
        }
      }
      System.out.println("FLON3 "+lineNode.getStartPosition() + " off " + offset + " alt off" + altOff);
      ASTNode simpName = pinpointOnLine(lineNode, altOff,
                                        lineNode.getStartPosition(), name);
      System.out.println("+++> " + simpName);
      if (simpName instanceof SimpleName) {
        nameOfNode = simpName.toString();
        System.out.println(getNodeAsString(simpName));
        decl = findDeclaration((SimpleName) simpName);
        if (decl != null) {
          System.err.println("DECLA: " + decl.getClass().getName());
          retLabelString = getNodeAsString(decl);
        } else
          System.err.println("null");

        System.out.println(getNodeAsString(decl));
        
        // - findDecl3 testing
        
        ASTNode nearestNode = findClosestNode(lineNumber, (ASTNode) compilationUnit.types()
                                              .get(0));
        ClassMember cmem = resolveExpression3rdParty(nearestNode,
                                                     (SimpleName) simpName, false);
        if(cmem != null){
          System.out.println("CMEM-> "+cmem);
        }
        else
          System.out.println("CMEM-> null");
      }
    }

    if (decl != null && scrollOnly) {
      /*
       * For scrolling, we highlight just the name of the node, 
       * i.e., a SimpleName instance. But the declared node always 
       * points to the declared node itself, like TypeDecl, MethodDecl, etc.
       * This is important since it contains all the properties.
       */
      ASTNode simpName2 = getNodeName(decl,nameOfNode);
      System.err.println("FINAL String decl: " + getNodeAsString(decl));
      System.err.println("FINAL String label: " + getNodeAsString(simpName2));
      errorCheckerService.highlightNode(simpName2);
    } 

    return new ASTNodeWrapper(decl);
  }
  
  /**
   * Given a declaration type astnode, returns the SimpleName peroperty
   * of that node.
   * @param node
   * @param name - The name we're looking for.
   * @return SimpleName
   */
  private static ASTNode getNodeName(ASTNode node, String name){
    List<VariableDeclarationFragment> vdfs = null;
    switch (node.getNodeType()) {
    case ASTNode.TYPE_DECLARATION:
      return ((TypeDeclaration) node).getName();
    case ASTNode.METHOD_DECLARATION:
      return ((MethodDeclaration) node).getName();
    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      return ((SingleVariableDeclaration) node).getName();
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
      for (VariableDeclarationFragment vdf : vdfs) {
        if (vdf.getName().toString().equals(name)) {
          return vdf.getName();
        }
      }

    }
    return null;
  }

  /**
   * Fetches line number of the node in its CompilationUnit.
   * @param node
   * @return
   */
  private static int getLineNumber(ASTNode node) {
    return ((CompilationUnit) node.getRoot()).getLineNumber(node
        .getStartPosition());
  }

  public static void main(String[] args) {
    traversal2();
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
  
  private void addListeners(){
    jtree.addTreeSelectionListener(new TreeSelectionListener() {
      
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        System.out.println(e);
        SwingWorker worker = new SwingWorker() {

          @Override
          protected Object doInBackground() throws Exception {
            return null;
          }

          protected void done() {
            if(jtree
                .getLastSelectedPathComponent() == null){
              return;
            }
            DefaultMutableTreeNode tnode = (DefaultMutableTreeNode) jtree
                .getLastSelectedPathComponent();    
            if(tnode.getUserObject() == null){
              return;
            }
            
            if (tnode.getUserObject() instanceof ASTNodeWrapper) {
              ASTNodeWrapper awrap = (ASTNodeWrapper) tnode.getUserObject();
              errorCheckerService.highlightNode(awrap);
            }
          }
        };
        worker.execute();
      }
    });
    
    btnRename.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {       
        if(txtRenameField.getText().length() == 0)
          return;
        SwingWorker worker = new SwingWorker() {

          @Override
          protected Object doInBackground() throws Exception {
            return null;
          }

          protected void done() {
            
            String newName = txtRenameField.getText();
            DefaultMutableTreeNode defCU = findAllOccurrences();
            treeRename.setModel(new DefaultTreeModel(defCU));
            ((DefaultTreeModel) treeRename.getModel()).reload();
            frmOccurenceList.setVisible(true);
            int lineOffsetDisplacementConst = newName.length()
                - editor.ta.getSelectedText().length();
            HashMap<Integer, Integer> lineOffsetDisplacement = new HashMap<Integer, Integer>();

            // I need to store the pde and java offsets beforehand because once
            // the replace starts, all offsets returned are affected
            int offsetsMap[][][] = new int[defCU.getChildCount()][2][];
            for (int i = defCU.getChildCount() - 1; i >= 0; i--) {
              ASTNodeWrapper awrap = (ASTNodeWrapper) ((DefaultMutableTreeNode) (defCU
                  .getChildAt(i))).getUserObject();
              offsetsMap[i][0] = awrap.getPDECodeOffsets(errorCheckerService);
              offsetsMap[i][1] = awrap.getJavaCodeOffsets(errorCheckerService);
            }
            
            for (int i = defCU.getChildCount() - 1; i >= 0; i--) {
              int pdeoffsets[] = offsetsMap[i][0];
              int javaoffsets[] = offsetsMap[i][1];
              // correction for pde enhancements related displacement on a line
              int off = 0;
              if (lineOffsetDisplacement.get(javaoffsets[0]) != null) {
                off = lineOffsetDisplacement.get(javaoffsets[0]);

                lineOffsetDisplacement.put(javaoffsets[0],
                                           lineOffsetDisplacementConst + off);
              } else {
                lineOffsetDisplacement.put(javaoffsets[0],
                                           lineOffsetDisplacementConst);
              }

              ErrorCheckerService.scrollToErrorLine(editor, pdeoffsets[0],
                                                    pdeoffsets[1],
                                                    javaoffsets[1] + off,
                                                    javaoffsets[2]);
              editor.ta.setSelectedText(newName);
            }
            for (Integer lineNum : lineOffsetDisplacement.keySet()) {
              System.out.println(lineNum + "line, disp"
                  + lineOffsetDisplacement.get(lineNum));
            }
            editor.getSketch().setModified(true);
            errorCheckerService.runManualErrorCheck();
            frmOccurenceList.setVisible(false);
            frmRename.setVisible(false);
          }
        };
        worker.execute();
      }
    });

    btnListOccurrence.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        SwingWorker worker = new SwingWorker() {

          @Override
          protected Object doInBackground() throws Exception {
            return null;
          }

          protected void done() {
            DefaultMutableTreeNode defCU = findAllOccurrences();            
            treeRename.setModel(new DefaultTreeModel(defCU));
            ((DefaultTreeModel) treeRename.getModel()).reload();
            frmOccurenceList.setVisible(true);
          }
        };
        worker.execute();
      }
    });
        
    treeRename.addTreeSelectionListener(new TreeSelectionListener() {
      
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        System.out.println(e);
        SwingWorker worker = new SwingWorker() {

          @Override
          protected Object doInBackground() throws Exception {
            return null;
          }

          protected void done() {
            if(treeRename
                .getLastSelectedPathComponent() == null){
              return;
            }
            DefaultMutableTreeNode tnode = (DefaultMutableTreeNode) treeRename
                .getLastSelectedPathComponent();    
            if(tnode.getUserObject() == null){
              return;
            }
            
            if (tnode.getUserObject() instanceof ASTNodeWrapper) {
              ASTNodeWrapper awrap = (ASTNodeWrapper) tnode.getUserObject();
              errorCheckerService.highlightNode(awrap);
            }
          }
        };
        worker.execute();
      }
    });
  }
  
  private DefaultMutableTreeNode findAllOccurrences(){
    String selText = editor.ta.getSelectedText();
    int line = editor.ta.getSelectionStartLine();
    System.out.println(editor.ta.getSelectedText()
        + "<- offsets "
        + (line)
        + ", "
        + (editor.ta.getSelectionStart() - editor.ta
            .getLineStartOffset(line))
        + ", "
        + (editor.ta.getSelectionStop() - editor.ta
            .getLineStartOffset(line)));
    int offwhitespace = editor.ta
        .getLineStartNonWhiteSpaceOffset(line);
    ASTNodeWrapper wnode = getASTNodeAt(line
                                            + errorCheckerService.mainClassOffset,
                                        selText,
                                        editor.ta.getSelectionStart()
                                        - offwhitespace, false);
    System.err.println("Gonna find all occurrences of "
        + getNodeAsString(wnode.getNode()));
    
    //If wnode is a constructor, find the TD instead.
    if (wnode.getNodeType() == ASTNode.METHOD_DECLARATION) {
      MethodDeclaration md = (MethodDeclaration) wnode.getNode();
      ASTNode node = md.getParent();
      while (node != null) {
        if (node instanceof TypeDeclaration) {
          // System.out.println("Parent class " + getNodeAsString(node));
          break;
        }
        node = node.getParent();
      }
      if(node != null && node instanceof TypeDeclaration){
        TypeDeclaration td = (TypeDeclaration) node;
        if(td.getName().toString().equals(md.getName().toString())){
          System.err.println("Renaming constructor of " + getNodeAsString(td));
          wnode = new ASTNodeWrapper(td);
        }
      }
    }
    
    DefaultMutableTreeNode defCU = new DefaultMutableTreeNode(wnode);
    dfsNameOnly(defCU, wnode.getNode(), selText);
    System.out.println(wnode);
    return defCU;
  }

  @SuppressWarnings({ "unchecked" })
  /**
   * Generates AST Swing component 
   * @param node
   * @param tnode
   */
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
                                                  new ASTNodeWrapper((ASTNode) node
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
            ctnode = new DefaultMutableTreeNode(new ASTNodeWrapper(cnode));
            tnode.add(ctnode);
            visitRecur(cnode, ctnode);
          } else
            visitRecur(cnode, tnode);
        }
      }
    }
  }
  
  public void dfsNameOnly(DefaultMutableTreeNode tnode,ASTNode decl, String name) {
    Stack temp = new Stack<DefaultMutableTreeNode>();
    temp.push(codeTree);
    
    while(!temp.isEmpty()){
      DefaultMutableTreeNode cnode = (DefaultMutableTreeNode) temp.pop();
      for (int i = 0; i < cnode.getChildCount(); i++) {
        temp.push(cnode.getChildAt(i));
      }
      
      if(!(cnode.getUserObject() instanceof ASTNodeWrapper))
        continue;
      ASTNodeWrapper awnode = (ASTNodeWrapper) cnode.getUserObject();
//      System.out.println("Visiting: " + getNodeAsString(awnode.getNode()));
      if(isInstanceOfType(awnode.getNode(), decl, name)){
        tnode.add(new DefaultMutableTreeNode(awnode));
      }
      
    }
  }
  
  private boolean isInstanceOfType(ASTNode node,ASTNode decl, String name){
    if(node instanceof SimpleName){
      SimpleName sn = (SimpleName) node;
      
      if (sn.toString().equals(name)) {
        ArrayList<ASTNode> nodesToBeMatched = new ArrayList<ASTNode>();
        nodesToBeMatched.add(decl);
        if(decl instanceof TypeDeclaration){
          System.out.println("decl is a TD");
          TypeDeclaration td = (TypeDeclaration)decl;
          MethodDeclaration[] mlist = td.getMethods();
          for (MethodDeclaration md : mlist) {
            if(md.getName().toString().equals(name)){
              nodesToBeMatched.add(md);
            }
          }
        }
        System.out.println("Visiting: " + getNodeAsString(node));
        ASTNode decl2 = findDeclaration(sn);
        System.err.println("It's decl: " + getNodeAsString(decl2));
        System.out.println("But we need: "+getNodeAsString(decl));
        for (ASTNode astNode : nodesToBeMatched) {
          if(astNode.equals(decl2)){
            return true;
          }
        }
      }
    }
    return false;
  }
  
  public void handleRefactor(){
    if(editor.ta.getSelectedText() == null){
      editor.statusError("Highlight the class/function/variable name first");
      return;
    }
    
    if(errorCheckerService.hasSyntaxErrors()){
      editor.statusError("Can't rename until syntax errors are fixed :(");
      return;
    }
    if (!frmRename.isVisible()){
      frmRename.setVisible(true);
      SwingUtilities.invokeLater(new Runnable() {          
        @Override
        public void run() {
          frmOccurenceList.setTitle("All occurrences of "
              + editor.ta.getSelectedText());
          lblRefactorOldName.setText("Current name: "
              + editor.ta.getSelectedText());
          txtRenameField.requestFocus();
        }
      });
    }
    frmRename.toFront();
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
      return node;
//      if (offset < node.getLength())
//        return node;
//      else {
//        System.err.println(-11);
//        return null;
//      }
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
      System.out.println(offset+ "off,pol " + getNodeAsString(sn));
      if ((lineStartOffset + offset) >= sn.getStartPosition()
          && (lineStartOffset + offset) <= sn.getStartPosition()
              + sn.getLength()) {
        if (sn.toString().equals(name)) {
          return sn;
        }
        else {
          return null;
        } 
      } else {
        return null;
      }
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

  /**
   * Give this thing a {@link Name} instance - a {@link SimpleName} from the
   * ASTNode for ex, and it tries its level best to locate its declaration in
   * the AST. It really does.
   * 
   * @param findMe
   * @return
   */
  @SuppressWarnings("unchecked")
  private static ASTNode findDeclaration(Name findMe) {
    
    // WARNING: You're entering the Rube Goldberg territory of Experimental Mode.
    // To debug this code, thou must take the Recursive Leap of Faith.
    
    System.out.println("entering --findDeclaration1 -- " + findMe.toString());
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
        System.out.println(qn.getQualifier() + "->" + qn.getName());
        declaringClass = findDeclaration(stp.getName());
        
        System.out.println("QN decl class: " + getNodeAsString(declaringClass));
        constrains.clear();
        constrains.add(ASTNode.TYPE_DECLARATION);
        constrains.add(ASTNode.FIELD_DECLARATION);
        return definedIn(declaringClass, qn.getName().toString(), constrains,
                         null);
      }
      else{
        if(findMe instanceof QualifiedName){
          QualifiedName qnn = (QualifiedName) findMe;
          System.out.println("findMe is a QN, "
              + (qnn.getQualifier().toString() + " other " + qnn.getName()
                  .toString()));

          SimpleType stp = extracTypeInfo(findDeclaration((qnn.getQualifier())));
          System.out.println(qnn.getQualifier() + "->" + qnn.getName());
          declaringClass = findDeclaration(stp.getName());

          System.out.println("QN decl class: "
              + getNodeAsString(declaringClass));
          constrains.clear();
          constrains.add(ASTNode.TYPE_DECLARATION);
          constrains.add(ASTNode.FIELD_DECLARATION);
          return definedIn(declaringClass, qnn.getName().toString(), constrains,
                           null);
        }
      }
    } else if (parent.getNodeType() == ASTNode.SIMPLE_TYPE) {
      constrains.add(ASTNode.TYPE_DECLARATION);
      if (parent.getParent().getNodeType() == ASTNode.CLASS_INSTANCE_CREATION)
        constrains.add(ASTNode.CLASS_INSTANCE_CREATION);
    } else if(parent.getNodeType() == ASTNode.TYPE_DECLARATION){
      // The condition where we look up the name of a class decl
      TypeDeclaration td = (TypeDeclaration) parent;
      if(findMe.equals(td.getName()))
      {
        return parent; 
      }
    }
    else if (parent instanceof Expression) {
//      constrains.add(ASTNode.TYPE_DECLARATION);
//      constrains.add(ASTNode.METHOD_DECLARATION);
//      constrains.add(ASTNode.FIELD_DECLARATION);      
    }
//    else if(findMe instanceof QualifiedName){
//      QualifiedName qn = (QualifiedName) findMe;
//      System.out
//          .println("findMe is a QN, "
//              + (qn.getQualifier().toString() + " other " + qn.getName()
//                  .toString()));
//    }
    while (parent != null) {
      System.out.println("findDeclaration1 -> " + getNodeAsString(parent));
      for (Object oprop : parent.structuralPropertiesForType()) {
        StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) oprop;
        if (prop.isChildProperty() || prop.isSimpleProperty()) {
          if (parent.getStructuralProperty(prop) instanceof ASTNode) {
//            System.out.println(prop + " C/S Prop of -> "
//                + getNodeAsString(parent));
            ret = definedIn((ASTNode) parent.getStructuralProperty(prop),
                            findMe.toString(), constrains, declaringClass);
            if (ret != null)
              return ret;
          }
        } else if (prop.isChildListProperty()) {
//          System.out.println((prop) + " ChildList props of "
//              + getNodeAsString(parent));
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

  /**
   * A variation of findDeclaration() but accepts an alternate parent ASTNode
   * @param findMe
   * @param alternateParent
   * @return
   */
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
        if(stp == null)
          return null;
        declaringClass = findDeclaration2(stp.getName(), alternateParent);
        System.out.println(qn.getQualifier() + "->" + qn.getName());
        System.out.println("QN decl class: " + getNodeAsString(declaringClass));
        constrains.clear();
        constrains.add(ASTNode.TYPE_DECLARATION);
        constrains.add(ASTNode.FIELD_DECLARATION);
        return definedIn(declaringClass, qn.getName().toString(), constrains,
                         null);
      }
      else{
        if(findMe instanceof QualifiedName){
          QualifiedName qnn = (QualifiedName) findMe;
          System.out.println("findMe is a QN, "
              + (qnn.getQualifier().toString() + " other " + qnn.getName()
                  .toString()));

          SimpleType stp = extracTypeInfo(findDeclaration2((qnn.getQualifier()), alternateParent));
          System.out.println(qnn.getQualifier() + "->" + qnn.getName());
          declaringClass = findDeclaration2(stp.getName(), alternateParent);

          System.out.println("QN decl class: "
              + getNodeAsString(declaringClass));
          constrains.clear();
          constrains.add(ASTNode.TYPE_DECLARATION);
          constrains.add(ASTNode.FIELD_DECLARATION);
          return definedIn(declaringClass, qnn.getName().toString(), constrains,
                           null);
        }
      }
    } else if (parent.getNodeType() == ASTNode.SIMPLE_TYPE) {
      constrains.add(ASTNode.TYPE_DECLARATION);
      if (parent.getParent().getNodeType() == ASTNode.CLASS_INSTANCE_CREATION)
        constrains.add(ASTNode.CLASS_INSTANCE_CREATION);
    } else if (parent instanceof Expression) {
//      constrains.add(ASTNode.TYPE_DECLARATION);
//      constrains.add(ASTNode.METHOD_DECLARATION);
//      constrains.add(ASTNode.FIELD_DECLARATION);      
    } // TODO: in findDec, we also have a case where parent of type TD is handled.
      // Figure out if needed here as well.
    System.out.println("Alternate parent: " + getNodeAsString(alternateParent));
    while (alternateParent != null) {
//      System.out.println("findDeclaration2 -> "
//          + getNodeAsString(alternateParent));
      for (Object oprop : alternateParent.structuralPropertiesForType()) {
        StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) oprop;
        if (prop.isChildProperty() || prop.isSimpleProperty()) {
          if (alternateParent.getStructuralProperty(prop) instanceof ASTNode) {
//            System.out.println(prop + " C/S Prop of -> "
//                + getNodeAsString(alternateParent));
            ret = definedIn((ASTNode) alternateParent
                                .getStructuralProperty(prop),
                            findMe.toString(), constrains, declaringClass);
            if (ret != null)
              return ret;
          }
        } else if (prop.isChildListProperty()) {
//          System.out.println((prop) + " ChildList props of "
//              + getNodeAsString(alternateParent));
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
   * A wrapper for java.lang.reflect types.
   * Will have to see if the usage turns out to be internal only here or not
   * and then accordingly decide where to place this class. TODO
   * @author quarkninja
   *
   */
  public class ClassMember {
    private Field field;

    private Method method;

    private Constructor cons;

    private Class thisclass;

    private String stringVal;
    
    private String classType;
    
    private ASTNode astNode;
    
    private ASTNode declaringNode;

    public ClassMember(Class m) {
      thisclass = m;
      stringVal = "Predefined Class " + m.getName();
      classType = m.getName();
    }

    public ClassMember(Method m) {
      method = m;
      stringVal = "Method " + m.getReturnType().getName() + " | " + m.getName()
          + " defined in " + m.getDeclaringClass().getName();
      classType = m.getReturnType().getName();
    }

    public ClassMember(Field m) {
      field = m;
      stringVal = "Field " + m.getType().getName() + " | " + m.getName()
          + " defined in " + m.getDeclaringClass().getName();
      classType = m.getType().getName();
    }

    public ClassMember(Constructor m) {
      cons = m;
      stringVal = "Cons " + " " + m.getName() + " defined in "
          + m.getDeclaringClass().getName();
    }
    
    public ClassMember(ASTNode node){
      astNode = node;
      stringVal = getNodeAsString(node);
      if(node instanceof SimpleType){
        classType = ((SimpleType)node).getName().toString();
      }
      SimpleType stp = (node instanceof SimpleType) ? (SimpleType) node
          : extracTypeInfo(node);
      if(stp != null){
        ASTNode decl =findDeclaration(stp.getName());
        // Czech out teh mutation
        if(decl == null){
          // a predefined type
          classType = stp.getName().toString();
          Class<?> probableClass = findClassIfExists(classType);
          thisclass = probableClass; 
        }
        else{
          // a local type
          declaringNode = decl;
        }
      }
    }

    public Class getClass_() {
      return thisclass;
    }
    
    public ASTNode getDeclaringNode(){
      return declaringNode;
    }

    public Field getField() {
      return field;
    }

    public Method getMethod() {
      return method;
    }

    public Constructor getCons() {
      return cons;
    }
    
    public ASTNode getASTNode(){
      return astNode;
    }

    public String toString() {
      return stringVal;
    }
    
    public String getTypeAsString(){
      return classType;
    }
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
      return (SimpleType) ((MethodDeclaration) node).getReturnType2();
    case ASTNode.FIELD_DECLARATION:
      return (SimpleType) ((FieldDeclaration) node).getType();
    case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
      return (SimpleType) ((VariableDeclarationExpression) node).getType();
    case ASTNode.VARIABLE_DECLARATION_STATEMENT:
      return (SimpleType) ((VariableDeclarationStatement) node).getType();
    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      return (SimpleType) ((SingleVariableDeclaration) node).getType();
    case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
      return extracTypeInfo(node.getParent());
    }
    System.out.println("Unknown type info request " + getNodeAsString(node));
    return null;
  }

  @SuppressWarnings("unchecked")
  private static ASTNode definedIn(ASTNode node, String name,
                                   ArrayList<Integer> constrains,
                                   ASTNode declaringClass) {
    if (node == null)
      return null;
    if (constrains != null) {
//      System.out.println("Looking at " + getNodeAsString(node) + " for " + name
//          + " in definedIn");
      if (!constrains.contains(node.getNodeType()) && constrains.size() > 0) {
//        System.err.print("definedIn -1 " + " But constrain was ");
//        for (Integer integer : constrains) {
//          System.out.print(ASTNode.nodeClassForType(integer) + ",");
//        }
//        System.out.println();
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
              System.out.println("Found a constructor.");
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
    switch (node.getNodeType()) {
//    case ASTNode.STRING_LITERAL:
//    case ASTNode.NUMBER_LITERAL:
//    case ASTNode.BOOLEAN_LITERAL:
//    case ASTNode.NULL_LITERAL:
//      return false;
    default:
      return true;
    }
  }
  
  /**
   * For any line or expression, finds the line start offset(java code).
   * @param node
   * @return
   */
  public int getASTNodeLineStartOffset(ASTNode node){
    int nodeLineNo = getLineNumber(node);
    while(node.getParent() != null){
      if (getLineNumber(node.getParent()) == nodeLineNo) {
        node = node.getParent();
      } else {
        break;
      }
    }    
    return node.getStartPosition();
  }
  
  /**
   * For any node, finds various offsets (java code).
   * 
   * @param node
   * @return int[]{line number, line number start offset, node start offset,
   *         node length}
   */
  public int[] getASTNodeAllOffsets(ASTNode node){
    int nodeLineNo = getLineNumber(node), nodeOffset = node.getStartPosition(), nodeLength = node
        .getLength();
    while(node.getParent() != null){
      if (getLineNumber(node.getParent()) == nodeLineNo) {
        node = node.getParent();
      } else {
        break;
      }
    }    
    return new int[]{nodeLineNo, node.getStartPosition(), nodeOffset,nodeLength};
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
    else if(node instanceof FieldAccess)
      value = node.toString() + " | ";
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

  /**
   * CompletionPanel name
   * 
   * @param node
   * @return
   */
  static private String getNodeAsString2(ASTNode node) {
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
      value = ((TypeDeclaration) node).getName().toString();
    else if (node instanceof MethodDeclaration)
      value = ((MethodDeclaration) node).getName().toString();
    else if (node instanceof MethodInvocation)
      value = ((MethodInvocation) node).getName().toString() + " | "
          + className;
    else if (node instanceof FieldDeclaration)
      value = ((FieldDeclaration) node).toString();
    else if (node instanceof SingleVariableDeclaration)
      value = ((SingleVariableDeclaration) node).getName().toString();
    else if (node instanceof ExpressionStatement)
      value = node.toString() + className;
    else if (node instanceof SimpleName)
      value = ((SimpleName) node).getFullyQualifiedName() + " | " + className;
    else if (node instanceof QualifiedName)
      value = node.toString();
    else if (node instanceof VariableDeclarationFragment)
      value = ((VariableDeclarationFragment) node).getName().toString();
    else if (className.startsWith("Variable"))
      value = node.toString();
    else if (node instanceof VariableDeclarationStatement)
      value = ((VariableDeclarationStatement) node).toString();
    else if (className.endsWith("Type"))
      value = node.toString();
//    value += " [" + node.getStartPosition() + ","
//        + (node.getStartPosition() + node.getLength()) + "]";
//    value += " Line: "
//        + ((CompilationUnit) node.getRoot()).getLineNumber(node
//            .getStartPosition());
    return value;
  }

  public void jdocWindowVisible(boolean visible) {
    jdocWindow.setVisible(visible);
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
