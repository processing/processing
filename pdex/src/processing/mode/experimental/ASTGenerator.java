package processing.mode.experimental;

import static processing.mode.experimental.ExperimentalMode.log;
import static processing.mode.experimental.ExperimentalMode.logE;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
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
import processing.app.Library;
import processing.app.SketchCode;
import processing.app.Toolkit;
import processing.mode.java.preproc.PdePreprocessor;

import com.google.classpath.ClassPath;
import com.google.classpath.ClassPathFactory;
import com.google.classpath.RegExpResourceFilter;

public class ASTGenerator {

  protected ErrorCheckerService errorCheckerService;

  protected DebugEditor editor;

  public DefaultMutableTreeNode codeTree = new DefaultMutableTreeNode();

  protected DefaultMutableTreeNode currentParent = null;

  /**
   * AST Window
   */
  protected JFrame frmASTView;
  
  protected JFrame frameAutoComp;

  /**
   * Swing component wrapper for AST, used for internal testing
   */
  protected JTree jtree;
  
  /**
   * JTree used for testing refactoring operations
   */
  protected JTree treeRename;

  protected CompilationUnit compilationUnit;

  protected JTable tableAuto;

  protected JEditorPane javadocPane;

  protected JScrollPane scrollPane;
  
  protected JFrame frmRename;

  protected JButton btnRename;
  
  protected JButton btnListOccurrence;
  
  protected JTextField txtRenameField;
  
  protected JFrame frmOccurenceList;
  
  protected JLabel lblRefactorOldName;
  
  public ASTGenerator(ErrorCheckerService ecs) {
    this.errorCheckerService = ecs;
    this.editor = ecs.getEditor();
    setupGUI();
    //addCompletionPopupListner();
    addListeners(); 
    //loadJavaDoc();
    predictionOngoing = new AtomicBoolean(false);
  }
  
  protected void setupGUI(){
    frmASTView = new JFrame();

    jtree = new JTree();
    frmASTView.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    frmASTView.setBounds(new Rectangle(680, 100, 460, 620));
    frmASTView.setTitle("AST View - " + editor.getSketch().getName());
    JScrollPane sp = new JScrollPane();
    sp.setViewportView(jtree);
    frmASTView.add(sp);

    btnRename = new JButton("Rename");
    btnListOccurrence = new JButton("Show Usage");
    frmRename = new JFrame();    
    frmRename.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    frmRename.setSize(250, 130);
    frmRename.setLayout(new BoxLayout(frmRename.getContentPane(), BoxLayout.Y_AXIS));
    Toolkit.setIcon(frmRename);
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
    frmRename.setLocation(editor.getX()
                              + (editor.getWidth() - frmRename.getWidth()) / 2,
                          editor.getY()
                              + (editor.getHeight() - frmRename.getHeight())
                              / 2);
    
    
    frmOccurenceList = new JFrame();
    frmOccurenceList.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    frmOccurenceList.setSize(300, 400);
    Toolkit.setIcon(frmOccurenceList);
    JScrollPane sp2 = new JScrollPane();
    treeRename = new JTree();
    sp2.setViewportView(treeRename);    
    frmOccurenceList.add(sp2);    
    //occurenceListFrame.setVisible(true);
    
//    frameAutoComp = new JFrame();
//    frameAutoComp.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//    frameAutoComp.setBounds(new Rectangle(1280, 100, 460, 620));
//    Toolkit.setIcon(frameAutoComp); 
//    tableAuto = new JTable();
//    JScrollPane sp3 = new JScrollPane();
//    sp3.setViewportView(tableAuto);
//    frameAutoComp.add(sp3);

//    frmJavaDoc = new JFrame();
//    frmJavaDoc.setTitle("P5 InstaHelp");
//    //jdocWindow.setUndecorated(true);
//    frmJavaDoc.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
//    javadocPane = new JEditorPane();
//    javadocPane.setContentType("text/html");
//    javadocPane.setText("<html> </html>");
//    javadocPane.setEditable(false);
//    scrollPane = new JScrollPane();
//    scrollPane.setViewportView(javadocPane);
//    frmJavaDoc.add(scrollPane);
    //frmJavaDoc.setUndecorated(true);
    
    
  }
  
  /**
   * Toggle AST View window
   */
  public static final boolean SHOWAST = !true;

  protected DefaultMutableTreeNode buildAST(String source, CompilationUnit cu) {
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
      //log("Other cu");
    }
//    OutlineVisitor visitor = new OutlineVisitor();
//    compilationUnit.accept(visitor);
    getCodeComments();
    codeTree = new DefaultMutableTreeNode(new ASTNodeWrapper((ASTNode) compilationUnit
                                              .types().get(0)));
    //log("Total CU " + compilationUnit.types().size());
    if(compilationUnit.types() == null || compilationUnit.types().isEmpty()){
      logE("No CU found!");
    }
    visitRecur((ASTNode) compilationUnit.types().get(0), codeTree);
    SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {

      @Override
      protected Object doInBackground() throws Exception {
        return null;
      }

      protected void done() {
        if (codeTree != null) {
          if(SHOWAST){
  					if (jtree.hasFocus() || frmASTView.hasFocus())
  						return;
            jtree.setModel(new DefaultTreeModel(codeTree));
            ((DefaultTreeModel) jtree.getModel()).reload();
            jtree.validate();
            if (!frmASTView.isVisible()) {
              frmASTView.setVisible(true);
            }
          }
//          if (!frameAutoComp.isVisible()) {
//
//            frameAutoComp.setVisible(true);
//            
//          }
//          if (!frmJavaDoc.isVisible()) {
//            long t = System.currentTimeMillis();
//            loadJavaDoc();
//            log("Time taken: "
//                + (System.currentTimeMillis() - t));
//            frmJavaDoc.setBounds(new Rectangle(errorCheckerService.getEditor()
//                .getX() + errorCheckerService.getEditor().getWidth(),
//                                               errorCheckerService.getEditor()
//                                                   .getY(), 450, 600));
//            frmJavaDoc.setVisible(true);
//          }
        }
      }
    };
    worker.execute();
//    logE("++>" + System.getProperty("java.class.path"));
//    log(System.getProperty("java.class.path"));
//    log("-------------------------------");
    return codeTree;
  }

  protected ClassPathFactory factory;

  /**
   * Used for searching for package declaration of a class 
   */
  protected ClassPath classPath;

  //protected JFrame frmJavaDoc;

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
          if(Base.isMacOS()){
            // rt.jar equivalent on OS X is JAVA_HOME/bundle/Classes/classes.jar
            tehPath.append(File.pathSeparatorChar
                           + System.getProperty("java.home") + File.separator + "bundle"
                + File.separator + "Classes" + File.separator + "classes.jar"
                + File.pathSeparatorChar);
          }else{
          tehPath.append(File.pathSeparatorChar
              + System.getProperty("java.home") + File.separator + "lib"
              + File.separator + "rt.jar" + File.pathSeparatorChar);
          }
          if (errorCheckerService.classpathJars != null) {
            synchronized (errorCheckerService.classpathJars) {
              for (URL jarPath : errorCheckerService.classpathJars) {
                //log(jarPath.getPath());
                tehPath.append(jarPath.getPath() + File.pathSeparatorChar);
              }
            }
          }

//          String paths[] = tehPath.toString().split(File.separatorChar +"");
//          StringTokenizer st = new StringTokenizer(tehPath.toString(),
//                                                   File.pathSeparatorChar + "");
//          while (st.hasMoreElements()) {
//            String sstr = (String) st.nextElement();
//            log(sstr);
//          }

          classPath = factory.createFromPath(tehPath.toString());
          log("Classpath created " + (classPath != null));
//          for (String packageName : classPath.listPackages("")) {
//            log(packageName);
//          }
//          RegExpResourceFilter regExpResourceFilter = new RegExpResourceFilter(
//                                                                               ".*",
//                                                                               "ArrayList.class");
//          String[] resources = classPath.findResources("", regExpResourceFilter);
//          for (String className : resources) {
//            log("-> " + className);
//          }
          log("Sketch classpath jars loaded.");
          if (Base.isMacOS()) {
            File f = new File(System.getProperty("java.home") + File.separator + "bundle"
                + File.separator + "Classes" + File.separator + "classes.jar");
            log(f.getAbsolutePath() + " | classes.jar found?"
                + f.exists());
          } else {
            File f = new File(System.getProperty("java.home") + File.separator
                + "lib" + File.separator + "rt.jar" + File.separator);
            log(f.getAbsolutePath() + " | rt.jar found?"
                + f.exists());
          }
          
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    t.start();
  }

  protected TreeMap<String, String> jdocMap;

  protected void loadJavaDoc() {
    jdocMap = new TreeMap<String, String>();
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
      return new CompletionCandidate[] { new CompletionCandidate((TypeDeclaration) node) };

    case ASTNode.METHOD_DECLARATION:
      MethodDeclaration md = (MethodDeclaration) node;
      log(getNodeAsString(md));
      List<ASTNode> params = (List<ASTNode>) md
          .getStructuralProperty(MethodDeclaration.PARAMETERS_PROPERTY);
      CompletionCandidate[] cand = new CompletionCandidate[params.size() + 1];
      cand[0] = new CompletionCandidate(md);
      for (int i = 0; i < params.size(); i++) {
//        cand[i + 1] = new CompletionCandidate(params.get(i).toString(), "", "",
//                                              CompletionCandidate.LOCAL_VAR);
        cand[i + 1] = new CompletionCandidate((SingleVariableDeclaration)params.get(i));
      }
      return cand;

    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      return new CompletionCandidate[] { new CompletionCandidate((SingleVariableDeclaration)node) };

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
//        ret[i++] = new CompletionCandidate(getNodeAsString2(vdf), "", "",
//                                           CompletionCandidate.LOCAL_VAR);
        ret[i++] = new CompletionCandidate(vdf);
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
    log("Resolving " + getNodeAsString(expression) + " noComp "
        + noCompare);
    if (expression instanceof SimpleName) {
      return findDeclaration2(((SimpleName) expression), nearestNode);
    } else if (expression instanceof MethodInvocation) {
      log("3. Method Invo "
          + ((MethodInvocation) expression).getName());
      return findDeclaration2(((MethodInvocation) expression).getName(),
                              nearestNode);
    } else if (expression instanceof FieldAccess) {
      log("2. Field access "
          + getNodeAsString(((FieldAccess) expression).getExpression()) + "|||"
          + getNodeAsString(((FieldAccess) expression).getName()));
      if (noCompare) {
        /*
         * ASTNode ret = findDeclaration2(((FieldAccess) expression).getName(),
         * nearestNode); log("Found as ->"+getNodeAsString(ret));
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
      log("1. Resolving "
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
    log("Resolve 3rdParty expr-- " + getNodeAsString(astNode)
        + " nearest node " + getNodeAsString(nearestNode));
    if(astNode == null) return null;
    ClassMember scopeParent = null;
    SimpleType stp = null;
    if(astNode instanceof SimpleName){
      ASTNode decl = findDeclaration2(((SimpleName)astNode),nearestNode);
      if(decl != null){
        // see if locally defined
        log(getNodeAsString(astNode)+" found decl -> " + getNodeAsString(decl));
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
        
        // TODO: Check for existence of 'new' keyword. Could be a ClassInstanceCreation
        
        // Local code or belongs to super class
        log("FA,Not implemented.");
        return null;
      } else { 
        if (fa.getExpression() instanceof SimpleName) {
          stp = extracTypeInfo(findDeclaration2((SimpleName) fa.getExpression(),
                                                nearestNode));
          if(stp == null){
            /*The type wasn't found in local code, so it might be something like
             * log(), or maybe belonging to super class, etc.
             */
            Class tehClass = findClassIfExists(((SimpleName)fa.getExpression()).toString());
            if (tehClass != null) {
              // Method Expression is a simple name and wasn't located locally, but found in a class
              // so look for method in this class.
              return definedIn3rdPartyClass(new ClassMember(tehClass), fa
                  .getName().toString());
            }
            log("FA resolve 3rd par, Can't resolve " + fa.getExpression());
            
            return null;
          }
          log("FA, SN Type " + getNodeAsString(stp));
          scopeParent = definedIn3rdPartyClass(stp.getName().toString(), "THIS");
          
        } else {
          scopeParent = resolveExpression3rdParty(nearestNode,
                                                  fa.getExpression(), noCompare);
        }
        log("FA, ScopeParent " + scopeParent);
        return definedIn3rdPartyClass(scopeParent, fa.getName().toString());
      }
    case ASTNode.METHOD_INVOCATION:
      MethodInvocation mi = (MethodInvocation) astNode;
      ASTNode temp = findDeclaration2(mi.getName(), nearestNode);
      if(temp instanceof MethodDeclaration){
        // method is locally defined
        log(mi.getName() + " was found locally," + getNodeAsString(extracTypeInfo(temp)));
        return new ClassMember(extracTypeInfo(temp));
      }
      if (mi.getExpression() == null) {
//        if()
        //Local code or belongs to super class
        log("MI,Not implemented.");
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
            log("MI resolve 3rd par, Can't resolve " + mi.getExpression());
            return null;
          }
          log("MI, SN Type " + getNodeAsString(stp));
          ASTNode typeDec = findDeclaration2(stp.getName(),nearestNode);
          if(typeDec == null){
            log(stp.getName() + " couldn't be found locally..");
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
          return definedIn3rdPartyClass(new ClassMember(typeDec), mi
                                        .getName().toString());
        } else {
          log("MI EXP.."+getNodeAsString(mi.getExpression()));
//          return null;
          scopeParent = resolveExpression3rdParty(nearestNode,
                                                  mi.getExpression(), noCompare);
          log("MI, ScopeParent " + scopeParent);
          return definedIn3rdPartyClass(scopeParent, mi.getName().toString());
        }
        
      }
    case ASTNode.QUALIFIED_NAME:
      QualifiedName qn = (QualifiedName) astNode;
      ASTNode temp2 = findDeclaration2(qn.getName(), nearestNode);
      if(temp2 instanceof FieldDeclaration){
        // field is locally defined
        log(qn.getName() + " was found locally," + getNodeAsString(extracTypeInfo(temp2)));
        return new ClassMember(extracTypeInfo(temp2));
      }
      if (qn.getQualifier() == null) {
        log("QN,Not implemented.");
        return null;
      } else  {
        
        if (qn.getQualifier() instanceof SimpleName) {
          stp = extracTypeInfo(findDeclaration2(qn.getQualifier(), nearestNode));
          if(stp == null){
            /*The type wasn't found in local code, so it might be something like
             * log(), or maybe belonging to super class, etc.
             */
            Class tehClass = findClassIfExists(qn.getQualifier().toString());
            if (tehClass != null) {
              // note how similar thing is called on line 690. Check check.
              return definedIn3rdPartyClass(new ClassMember(tehClass), qn
                  .getName().toString());
            }
            log("QN resolve 3rd par, Can't resolve " + qn.getQualifier());
            return null;
          }
          log("QN, SN Local Type " + getNodeAsString(stp));
          //scopeParent = definedIn3rdPartyClass(stp.getName().toString(), "THIS");
          ASTNode typeDec = findDeclaration2(stp.getName(),nearestNode);
          if(typeDec == null){
            log(stp.getName() + " couldn't be found locally..");
            
            Class tehClass = findClassIfExists(stp.getName().toString());
            if (tehClass != null) {
              // note how similar thing is called on line 690. Check check.
              return definedIn3rdPartyClass(new ClassMember(tehClass), qn
                  .getName().toString());
            }
            log("QN resolve 3rd par, Can't resolve " + qn.getQualifier());
            return null;
          }
          return definedIn3rdPartyClass(new ClassMember(typeDec), qn
                                        .getName().toString());
        } else {
          scopeParent = resolveExpression3rdParty(nearestNode,
                                                  qn.getQualifier(), noCompare);
          log("QN, ScopeParent " + scopeParent);
          return definedIn3rdPartyClass(scopeParent, qn.getName().toString());
        }
        
      }
    case ASTNode.ARRAY_ACCESS:
      ArrayAccess arac = (ArrayAccess)astNode;
      return resolveExpression3rdParty(nearestNode, arac.getArray(), noCompare);
    default:
      log("Unaccounted type " + getNodeAsString(astNode));
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
    }else if(expression instanceof ArrayAccess){
      return ((ArrayAccess)expression).getArray();
    }
    log(" getChildExpression returning NULL for "
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
    } else if (expression instanceof ArrayAccess) {
      return ((ArrayAccess) expression).getArray();
    }
    log("getParentExpression returning NULL for "
        + getNodeAsString(expression));
    return null;
  }

  protected void trimCandidates(String newWord){
    ArrayList<CompletionCandidate> newCandidate = new ArrayList<CompletionCandidate>();
    newWord = newWord.toLowerCase();
    for (CompletionCandidate comp : candidates) {
      if(comp.toString().toLowerCase().startsWith(newWord)){
        // log("Adding " + comp);
        newCandidate.add(comp);
      }
    }
    candidates = newCandidate;
  }
  
  /**
   * List of CompletionCandidates
   */
  protected ArrayList<CompletionCandidate> candidates;
  protected String lastPredictedWord = " ";
  //protected AtomicBoolean predictionsEnabled;
  protected int predictionMinLength = 2;
  
  
  private AtomicBoolean predictionOngoing;
  
  /**
   * The main function that calculates possible code completion candidates
   *  
   * @param word
   * @param line
   * @param lineStartNonWSOffset
   */
  public void preparePredictions(final String word, final int line, final int lineStartNonWSOffset) {
    if(predictionOngoing.get()) return;
        
    if(!ExperimentalMode.codeCompletionsEnabled) return;
    if(word.length() < predictionMinLength) return;
    
    predictionOngoing.set(true);
    // This method is called from TextArea.fetchPhrase, which is called via a SwingWorker instance
    // in TextArea.processKeyEvent
    if(caretWithinLineComment()){
      log("No predictions.");
      predictionOngoing.set(false);
      return;
    }
    
//    SwingWorker worker = new SwingWorker() {
//
//      @Override
//      protected Object doInBackground() throws Exception {
//        return null;
//      }
//
//      protected void done() {
    
        // If the parsed code contains pde enhancements, take 'em out.
        String word2 = ASTNodeWrapper.getJavaCode(word);

        //After typing 'arg.' all members of arg type are to be listed. This one is a flag for it        
        boolean noCompare = false;
        if (word2.endsWith(".")) {
          // return all matches
          word2 = word2.substring(0, word2.length() - 1);
          noCompare = true;
        }
        
        if (word2.length() >= predictionMinLength && !noCompare
            && word2.length() > lastPredictedWord.length()) {
          if (word2.startsWith(lastPredictedWord)) {
            log(word + " starts with " + lastPredictedWord);
            log("Don't recalc");
            if (word2.contains(".")) {
              int x = word2.lastIndexOf('.');
              trimCandidates(word2.substring(x + 1));
            } else {
              trimCandidates(word2);
            }
            showPredictions(word);
            lastPredictedWord = word2;
            predictionOngoing.set(false);
            return;
          }
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
        
        // Ensure that we're not inside a comment. TODO: Binary search
        
        /*for (Comment comm : getCodeComments()) {
          int commLineNo = PdeToJavaLineNumber(compilationUnit
              .getLineNumber(comm.getStartPosition()));
          if(commLineNo == lineNumber){
            log("Found a comment line " + comm);
            log("Comment LSO "
                + javaCodeOffsetToLineStartOffset(compilationUnit
              .getLineNumber(comm.getStartPosition()),
                                                  comm.getStartPosition()));
            break;
          }
        }*/        

        // Now parse the expression into an ASTNode object
        ASTNode nearestNode = null;
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setKind(ASTParser.K_EXPRESSION);
        parser.setSource(word2.toCharArray());
        ASTNode testnode = parser.createAST(null);
        //logE("PREDICTION PARSER PROBLEMS: " + parser);
        // Find closest ASTNode of the document to this word
        logE("Typed: " + word2 + "|" + " temp Node type: " + testnode.getClass().getSimpleName());
        if(testnode instanceof MethodInvocation){
          MethodInvocation mi = (MethodInvocation)testnode;
          log(mi.getName() + "," + mi.getExpression() + "," + mi.typeArguments().size());
        }
        
        // find nearest ASTNode
        nearestNode = findClosestNode(lineNumber, (ASTNode) errorCheckerService.getLastCorrectCU().types()
            .get(0));
        if (nearestNode == null) {
          // Make sure nearestNode is not NULL if couldn't find a closeset node
          nearestNode = (ASTNode) errorCheckerService.getLastCorrectCU().types().get(0);
        }
        logE(lineNumber + " Nearest ASTNode to PRED "
            + getNodeAsString(nearestNode));

        candidates = new ArrayList<CompletionCandidate>();
        lastPredictedWord = word2;
        // Determine the expression typed
        
        if (testnode instanceof SimpleName && !noCompare) {
          logE("One word expression " + getNodeAsString(testnode));
          //==> Simple one word exprssion - so is just an identifier
          
          // Bottom up traversal of the AST to look for possible definitions at 
          // higher levels.
          //nearestNode = nearestNode.getParent();
          while (nearestNode != null) {
            // If the current class has a super class, look inside it for
            // definitions.
            if (nearestNode instanceof TypeDeclaration) {
              TypeDeclaration td = (TypeDeclaration) nearestNode;
              if (td
                  .getStructuralProperty(TypeDeclaration.SUPERCLASS_TYPE_PROPERTY) != null) {
                SimpleType st = (SimpleType) td
                    .getStructuralProperty(TypeDeclaration.SUPERCLASS_TYPE_PROPERTY);
                log("Superclass " + st.getName());
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
                      if (types[i].getElementName().toLowerCase().startsWith(word2.toLowerCase()))
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
                      if (types[i].getElementName().toLowerCase().startsWith(word2.toLowerCase()))
                        candidates.add(types[i]);
                    }
                  }
                }
              }
            }
            nearestNode = nearestNode.getParent();
          }
          // We're seeing a simple name that's not defined locally or in
          // the parent class. So most probably a pre-defined type.
          log("Empty can. " + word2);
          if (classPath != null) {
            RegExpResourceFilter regExpResourceFilter;
            regExpResourceFilter = new RegExpResourceFilter(
                                                            Pattern
                                                                .compile(".*"),
                                                            Pattern
                                                                .compile(word2
                                                                             + "[a-zA-Z_0-9]*.class",
                                                                         Pattern.CASE_INSENSITIVE));
            String[] resources = classPath.findResources("",
                                                         regExpResourceFilter);
            for (String matchedClass2 : resources) {
              matchedClass2 = matchedClass2.replace('/', '.');
              String matchedClass = matchedClass2.substring(0, matchedClass2
                  .length() - 6);
              int d = matchedClass.lastIndexOf('.');
              if (ignorableImport(matchedClass2,matchedClass.substring(d + 1)))
                continue;
              
              matchedClass = matchedClass.substring(d + 1);
              candidates
                  .add(new CompletionCandidate(matchedClass, matchedClass
                      + " : " + matchedClass2.substring(0, d), matchedClass,
                                               CompletionCandidate.PREDEF_CLASS));
              //log("-> " + className);
            }
          }
        } else {

          // ==> Complex expression of type blah.blah2().doIt,etc
          // Have to resolve it by carefully traversing AST of testNode
          logE("Complex expression " + getNodeAsString(testnode));
          log("candidates empty");
          ASTNode childExpr = getChildExpression(testnode);
          log("Parent expression : " + getParentExpression(testnode));
          log("Child expression : " + childExpr);
          if (childExpr != null) {
            if (!noCompare) {
              log("Original testnode "
                  + getNodeAsString(testnode));
              testnode = getParentExpression(testnode);
              log("Corrected testnode "
                  + getNodeAsString(testnode));
            }
            ClassMember expr = resolveExpression3rdParty(nearestNode, testnode,
                                                         noCompare);
            if (expr == null) {
              log("Expr is null");
            } else {
              log("Expr is " + expr.toString());
              candidates = getMembersForType(expr, childExpr.toString(),
                                             noCompare, false);
            }
          }
          else
          {
            log("ChildExpr is null");
          }          
        }
        
        showPredictions(word);
        predictionOngoing.set(false);
//      }
//    };
//
//    worker.execute();
  }
  
  protected void showPredictions(final String word) {
    if (sketchOutline != null)
      if (sketchOutline.isVisible()) return;
    Collections.sort(candidates);
//    CompletionCandidate[][] candi = new CompletionCandidate[candidates.size()][1];
    DefaultListModel<CompletionCandidate> defListModel = new DefaultListModel<CompletionCandidate>();

    for (int i = 0; i < candidates.size(); i++) {
//      candi[i][0] = candidates.get(i);
      defListModel.addElement(candidates.get(i));
    }
    log("Total preds = " + candidates.size());
//    DefaultTableModel tm = new DefaultTableModel(candi,
//                                                 new String[] { "Suggestions" });
//    if (tableAuto.isVisible()) {
//      tableAuto.setModel(tm);
//      tableAuto.validate();
//      tableAuto.repaint();
//    }
    errorCheckerService.getEditor().textArea()
        .showSuggestion(defListModel, word);
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
    log("In GMFT(), Looking for match " + child.toString()
        + " in class " + typeName + " noCompare " + noCompare + " staticOnly "
        + staticOnly);
    Class<?> probableClass = findClassIfExists(typeName);
    if(probableClass == null){
      log("In GMFT(), class not found.");
      return candidates;
    }
   return getMembersForType(new ClassMember(probableClass), child, noCompare, staticOnly);
   
  }
  
  public ArrayList<CompletionCandidate> getMembersForType(ClassMember tehClass,
                                                          String childToLookFor,
                                                          boolean noCompare,
                                                          boolean staticOnly) {
    String child = childToLookFor.toLowerCase();
    ArrayList<CompletionCandidate> candidates = new ArrayList<CompletionCandidate>();
    log("getMemFoType-> Looking for match " + child.toString()
        + " inside " + tehClass + " noCompare " + noCompare + " staticOnly "
        + staticOnly);
    if(tehClass == null){
      return candidates;
    }
    // tehClass will either be a TypeDecl defined locally
    if(tehClass.getDeclaringNode() instanceof TypeDeclaration){
      
      TypeDeclaration td = (TypeDeclaration) tehClass.getDeclaringNode();
      for (int i = 0; i < td.getFields().length; i++) {
        List<VariableDeclarationFragment> vdfs = td.getFields()[i]
            .fragments();
        for (VariableDeclarationFragment vdf : vdfs) {
          if (noCompare) {
            candidates
                .add(new CompletionCandidate(vdf));
          } else if (vdf.getName().toString().toLowerCase()
              .startsWith(child))
            candidates
                .add(new CompletionCandidate(vdf));
        }

      }
      for (int i = 0; i < td.getMethods().length; i++) {
        if (noCompare) {
          candidates.add(new CompletionCandidate(td.getMethods()[i]));
        } else if (td.getMethods()[i].getName().toString().toLowerCase()
            .startsWith(child))
          candidates.add(new CompletionCandidate(td.getMethods()[i]));
      }
      
      ArrayList<CompletionCandidate> superClassCandidates = new ArrayList<CompletionCandidate>();
      if(td.getSuperclassType() instanceof Type){
        log(getNodeAsString(td.getSuperclassType()) + " <-Looking into superclass of " + tehClass);
        superClassCandidates = getMembersForType(new ClassMember(td
                                                     .getSuperclassType()),
                                                 childToLookFor, noCompare, staticOnly);        
      }
      else
      {
        superClassCandidates = getMembersForType(new ClassMember(Object.class),
                                                 childToLookFor, noCompare, staticOnly);
      }
      for (CompletionCandidate cc : superClassCandidates) {
        candidates.add(cc);
      }
      return candidates;
    }
    
    // Or tehClass will be a predefined class
   
    Class<?> probableClass;
    if (tehClass.getClass_() != null) {
      probableClass = tehClass.getClass_();
    } else {
      probableClass = findClassIfExists(tehClass.getTypeAsString());
      if (probableClass == null) {
        log("Couldn't find class " + tehClass.getTypeAsString());
        return candidates;
      }
      log("Loaded " + probableClass.toString());
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
      } else if (label.toString().toLowerCase().startsWith(child)) {
        candidates.add(new CompletionCandidate(method));
      }
    }
    for (Field field : probableClass.getFields()) {
      if (!Modifier.isStatic(field.getModifiers()) && staticOnly) {
        continue;
      }
      if (noCompare) {
        candidates.add(new CompletionCandidate(field));
      } else if (field.getName().toLowerCase().startsWith(child)) {
        candidates.add(new CompletionCandidate(field));
      }
    }
    return candidates;
  }
  
  public String getPDESourceCodeLine(int javaLineNumber) {
    int res[] = errorCheckerService
        .calculateTabIndexAndLineNumber(javaLineNumber);
    if (res != null) {
      return errorCheckerService.getPDECodeAtLine(res[0], res[1]);
    }
    return null;
  }
  
  /**
   * Returns the java source code line at the given line number
   * @param javaLineNumber
   * @return
   */
  public String getJavaSourceCodeLine(int javaLineNumber) {
    try {
      PlainDocument javaSource = new PlainDocument();
      javaSource.insertString(0, errorCheckerService.sourceCode, null);
      Element lineElement = javaSource.getDefaultRootElement()
          .getElement(javaLineNumber - 1);
      if (lineElement == null) {
        log("Couldn't fetch jlinenum " + javaLineNumber);
        return null;
      }
      String javaLine = javaSource.getText(lineElement.getStartOffset(),
                                           lineElement.getEndOffset()
                                               - lineElement.getStartOffset());
      return javaLine;
    } catch (BadLocationException e) {
      logE(e + " in getJavaSourceCodeline() for jinenum: " + javaLineNumber);
    }
    return null;
  }

  /**
   * Returns the java source code line Element at the given line number.
   * The Element object stores the offset data, but not the actual line
   * of code.
   * @param javaLineNumber
   * @return
   */
  public Element getJavaSourceCodeElement(int javaLineNumber) {
    try {
      PlainDocument javaSource = new PlainDocument();
      javaSource.insertString(0, errorCheckerService.sourceCode, null);
      Element lineElement = javaSource.getDefaultRootElement()
          .getElement(javaLineNumber - 1);
      if (lineElement == null) {
        log("Couldn't fetch jlinenum " + javaLineNumber);
        return null;
      }
//      String javaLine = javaSource.getText(lineElement.getStartOffset(),
//                                           lineElement.getEndOffset()
//                                               - lineElement.getStartOffset());
      return lineElement;
    } catch (BadLocationException e) {
      logE(e + " in getJavaSourceCodeline() for jinenum: " + javaLineNumber);
    }
    return null;
  }
  
  /**
   * Searches for the particular class in the default list of imports as well as
   * the Sketch classpath
   * @param className
   * @return
   */
  protected Class findClassIfExists(String className){
    if(className == null){
      return null;
    }
    Class tehClass = null;
    // First, see if the classname is a fully qualified name and loads straightaway
    tehClass = loadClass(className);
    
    if (tehClass != null) {    
      //log(tehClass.getName() + " located straightaway");
      return tehClass;
    }
    
    log("Looking in the classloader for " + className);
    ArrayList<ImportStatement> imports = errorCheckerService
        .getProgramImports();

    for (ImportStatement impS : imports) {
      String temp = impS.getPackageName();

      if (temp.endsWith("*")) {
        temp = temp.substring(0, temp.length() - 1) + className;
      } else {
        int x = temp.lastIndexOf('.');
        //log("fclife " + temp.substring(x + 1));
        if (!temp.substring(x + 1).equals(className)) {
          continue;
        }
      }
      tehClass = loadClass(temp);
      if (tehClass != null) {
        log(tehClass.getName() + " located.");
        return tehClass;
      }

      //log("Doesn't exist in package: " + impS.getImportName());

    }
    
    PdePreprocessor p = new PdePreprocessor(null);
    for (String impS : p.getCoreImports()) {
      tehClass = loadClass(impS.substring(0,impS.length()-1) + className);
      if (tehClass != null) {
        log(tehClass.getName() + " located.");
        return tehClass;
      }
      //log("Doesn't exist in package: " + impS);
    }
    
    for (String impS : p.getDefaultImports()) {
      if(className.equals(impS) || impS.endsWith(className)){
        tehClass = loadClass(impS);                    
        if (tehClass != null) {
          log(tehClass.getName() + " located.");
          return tehClass;
        }
       // log("Doesn't exist in package: " + impS);
      }
    }
    
    // And finally, the daddy
    String daddy = "java.lang." + className;
    tehClass = loadClass(daddy);                    
    if (tehClass != null) {
      log(tehClass.getName() + " located.");
      return tehClass;
    }
    //log("Doesn't exist in java.lang");
    
    return tehClass;
  }
  
  protected Class loadClass(String className){
    Class tehClass = null;
    if(className instanceof String){
      try {
        tehClass = Class.forName(className, false,
                                 errorCheckerService.getSketchClassLoader());
      } catch (ClassNotFoundException e) {
        //log("Doesn't exist in package: ");
      }
    }
    return tehClass;
  }
  
  public ClassMember definedIn3rdPartyClass(String className,String memberName){
    Class<?> probableClass = findClassIfExists(className);
    if (probableClass == null) {
      log("Couldn't load " + className);
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
    log("definedIn3rdPartyClass-> Looking for " + memberName
        + " in " + tehClass);
    String memberNameL = memberName.toLowerCase();
    if(tehClass.getDeclaringNode() instanceof TypeDeclaration){
      
      TypeDeclaration td = (TypeDeclaration) tehClass.getDeclaringNode();
      for (int i = 0; i < td.getFields().length; i++) {
        List<VariableDeclarationFragment> vdfs = td.getFields()[i]
            .fragments();
        for (VariableDeclarationFragment vdf : vdfs) {
          if (vdf.getName().toString().toLowerCase()
              .startsWith(memberNameL))
            return new ClassMember(vdf);
        }

      }
      for (int i = 0; i < td.getMethods().length; i++) {
       if (td.getMethods()[i].getName().toString().toLowerCase()
            .startsWith(memberNameL))
         return new ClassMember(td.getMethods()[i]);
      }
      if(td.getSuperclassType() instanceof Type){
        log(getNodeAsString(td.getSuperclassType()) + " <-Looking into superclass of " + tehClass);
        return definedIn3rdPartyClass(new ClassMember(td
                                                     .getSuperclassType()),memberName);        
      }
      else
      {
        return definedIn3rdPartyClass(new ClassMember(Object.class),memberName);
      }
    }
    
    Class probableClass = null;
    if(tehClass.getClass_() != null){
      probableClass = tehClass.getClass_();
    }
    else
    {
      probableClass = findClassIfExists(tehClass.getTypeAsString());
      log("Loaded " + probableClass.toString());
    }
    for (Method method : probableClass.getMethods()) {        
      if (method.getName().equalsIgnoreCase(memberName)) {
        return new ClassMember(method);
      }
    }
    for (Field field : probableClass.getFields()) {
      if (field.getName().equalsIgnoreCase(memberName)) {
        return new ClassMember(field);
      }
    }
    return null;
  }
  
  public void updateJavaDoc(final CompletionCandidate candidate) {
    //TODO: Work on this later.
      return;
  /*  String methodmatch = candidate.toString();
    if (methodmatch.indexOf('(') != -1) {
      methodmatch = methodmatch.substring(0, methodmatch.indexOf('('));
    }

    //log("jdoc match " + methodmatch);
    String temp = "<html> </html>";
    for (final String key : jdocMap.keySet()) {
      if (key.startsWith(methodmatch) && key.length() > 3) {
        log("Matched jdoc " + key);
        if (candidate.getWrappedObject() != null) {
          String definingClass = "";
          if (candidate.getWrappedObject() instanceof Field)
            definingClass = ((Field) candidate.getWrappedObject())
                .getDeclaringClass().getName();
          else if (candidate.getWrappedObject() instanceof Method)
            definingClass = ((Method) candidate.getWrappedObject())
                .getDeclaringClass().getName();
          if (definingClass.equals("processing.core.PApplet")) {
            temp = (jdocMap.get(key));
            break;
          }
        } 
      }
    }
    
    final String jdocString = temp;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        javadocPane.setText(jdocString);
        scrollPane.getVerticalScrollBar().setValue(0);
        //frmJavaDoc.setVisible(!jdocString.equals("<html> </html>"));
        editor.toFront();
        editor.ta.requestFocus();
      }
    });
*/
  }

  @SuppressWarnings("unchecked")
  protected static ASTNode findClosestParentNode(int lineNumber, ASTNode node) {
    Iterator<StructuralPropertyDescriptor> it = node
        .structuralPropertiesForType().iterator();
    // logE("Props of " + node.getClass().getName());
    while (it.hasNext()) {
      StructuralPropertyDescriptor prop = it.next();

      if (prop.isChildProperty() || prop.isSimpleProperty()) {
        if (node.getStructuralProperty(prop) != null) {
//          System.out
//              .println(node.getStructuralProperty(prop) + " -> " + (prop));
          if (node.getStructuralProperty(prop) instanceof ASTNode) {
            ASTNode cnode = (ASTNode) node.getStructuralProperty(prop);
//            log("Looking at " + getNodeAsString(cnode)+ " for line num " + lineNumber);
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
//          log("Looking at " + getNodeAsString(cnode)+ " for line num " + lineNumber);
          if (getLineNumber(cnode) <= lineNumber && lineNumber <= cLineNum) {
            return findClosestParentNode(lineNumber, cnode);
          }
        }
      }
    }
    return node;
  }

  protected static ASTNode findClosestNode(int lineNumber, ASTNode node) {
    log("findClosestNode to line " + lineNumber);
    ASTNode parent = findClosestParentNode(lineNumber, node);
    log("findClosestParentNode returned " + getNodeAsString(parent));
    if (parent == null)
      return null;
    if (getLineNumber(parent) == lineNumber){
      log(parent + "|PNode " + getLineNumber(parent) + ", lfor " + lineNumber );
      return parent;
    }
    List<ASTNode> nodes = null;
    if (parent instanceof TypeDeclaration) {
      nodes = ((TypeDeclaration) parent).bodyDeclarations();
    } else if (parent instanceof Block) {
      nodes = ((Block) parent).statements();
    } else {
      System.err.println("THIS CONDITION SHOULD NOT OCCUR - findClosestNode "
          + getNodeAsString(parent));
      return null;
    }

    if (nodes.size() > 0) {
      ASTNode retNode = parent;
      for (int i = 0; i < nodes.size(); i++) {
        ASTNode cNode = nodes.get(i);        
        log(cNode + "|cNode " + getLineNumber(cNode) + ", lfor " + lineNumber );
        if (getLineNumber(cNode) <= lineNumber)          
          retNode = cNode;
      }

      return retNode;
    }
    return parent;
  }

  public DefaultMutableTreeNode getAST() {
    return codeTree;
  }

  public String getLabelForASTNode(int lineNumber, String name, int offset) {
    return getASTNodeAt(lineNumber, name, offset, false).getLabel();
    //return "";
  }
  
  protected String getLabelIfType(ASTNodeWrapper node, SimpleName sn){
    ASTNode current = node.getNode().getParent();
    String type = "";
    StringBuffer fullName = new StringBuffer();
    Stack<String> parents = new Stack<String>();
    String simpleName = (sn == null) ? node.getNode().toString() : sn.toString();
    switch (node.getNodeType()) {
    case ASTNode.TYPE_DECLARATION:
    case ASTNode.METHOD_DECLARATION:
    case ASTNode.FIELD_DECLARATION:
      while (current != null) {
        if (current instanceof TypeDeclaration) {
          parents.push(((TypeDeclaration) current).getName().toString());
        }
        current = current.getParent();
      }
      while (parents.size() > 0) {
        fullName.append(parents.pop() + ".");
      }
      fullName.append(simpleName);
      if (node.getNode() instanceof MethodDeclaration) {
        MethodDeclaration md = (MethodDeclaration) node.getNode();
        if (!md.isConstructor())
          type = md.getReturnType2().toString();
        fullName.append('(');
        if (!md.parameters().isEmpty()) {
          List<ASTNode> params = md.parameters();
          for (ASTNode par : params) {
            if (par instanceof SingleVariableDeclaration) {
              SingleVariableDeclaration svd = (SingleVariableDeclaration) par;
              fullName.append(svd.getType() + " " + svd.getName() + ",");
            }
          }
        }
        if(fullName.charAt(fullName.length() - 1) == ',')
          fullName.deleteCharAt(fullName.length() - 1);
        fullName.append(')');
      }
      else if(node.getNode() instanceof FieldDeclaration){
        type = ((FieldDeclaration) node.getNode()).getType().toString();
      }
      int x = fullName.indexOf(".");
      fullName.delete(0, x + 1);
      return type + " " + fullName;
    
    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      SingleVariableDeclaration svd = (SingleVariableDeclaration)node.getNode();
      return svd.getType() + " " + svd.getName();
      
    case ASTNode.VARIABLE_DECLARATION_STATEMENT:
      return ((VariableDeclarationStatement) node.getNode()).getType() + " "
          + simpleName;
    case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
      return ((VariableDeclarationExpression) node.getNode()).getType() + " "
          + simpleName;
    default:
      break;
    }
    
    
    return "";
  }

  public void scrollToDeclaration(int lineNumber, String name, int offset) {
    getASTNodeAt(lineNumber, name, offset, true);
  }


  /**
   * Given a word(identifier) in pde code, finds its location in the ASTNode
   * @param lineNumber
   * @param name
   * @param offset - line start nonwhitespace offset
   * @param scrollOnly
   * @return
   */
  public ASTNodeWrapper getASTNodeAt(int lineNumber, String name, int offset,
                                     boolean scrollOnly) {
    
    // Convert tab based pde line number to actual line number
    int pdeLineNumber = lineNumber + errorCheckerService.mainClassOffset;
//    log("----getASTNodeAt---- CU State: "
//        + errorCheckerService.compilationUnitState);
    if (errorCheckerService != null) {
      editor = errorCheckerService.getEditor();
      int codeIndex = editor.getSketch().getCodeIndex(editor.getCurrentTab());
      if (codeIndex > 0) {
        for (int i = 0; i < codeIndex; i++) {
          SketchCode sc = editor.getSketch().getCode(i);
          int len = Base.countLines(sc.getProgram()) + 1;
          pdeLineNumber += len;
        }
      }

    }

    // Find closest ASTNode to the linenumber
//    log("getASTNodeAt: Node line number " + pdeLineNumber);
    ASTNode lineNode = findLineOfNode(compilationUnit, pdeLineNumber, offset,
                                      name);

//    log("Node text +> " + lineNode);
    ASTNode decl = null;
    String nodeLabel = null;
    String nameOfNode = null; // The node name which is to be scrolled to

    // Obtain correspondin java code at that line, match offsets
    if (lineNode != null) {
      String pdeCodeLine = errorCheckerService.getPDECodeAtLine(editor
          .getSketch().getCurrentCodeIndex(), lineNumber);
      String javaCodeLine = getJavaSourceCodeLine(pdeLineNumber);

//      log(lineNumber + " Original Line num.\nPDE :" + pdeCodeLine);
//      log("JAVA:" + javaCodeLine);
//      log("Clicked on: " + name + " start offset: " + offset);
      // Calculate expected java offset based on the pde line
      OffsetMatcher ofm = new OffsetMatcher(pdeCodeLine, javaCodeLine);
      int javaOffset = ofm.getJavaOffForPdeOff(offset, name.length())
          + lineNode.getStartPosition();
//      log("JAVA ast offset: " + (javaOffset));
      
      // Find the corresponding node in the AST
      ASTNode simpName = dfsLookForASTNode(errorCheckerService.getLatestCU(),
                                           name, javaOffset,
                                           javaOffset + name.length());

      // If node wasn't found in the AST, lineNode may contain something
      if (simpName == null && lineNode instanceof SimpleName) {
        switch (lineNode.getParent().getNodeType()) {
        case ASTNode.TYPE_DECLARATION:

        case ASTNode.METHOD_DECLARATION:

        case ASTNode.FIELD_DECLARATION:

        case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
          decl = lineNode.getParent();
          return new ASTNodeWrapper(decl, "");
        default:
          break;
        }
      }

      // SimpleName instance found, now find its declaration in code
      if (simpName instanceof SimpleName) {
        nameOfNode = simpName.toString();
        // log(getNodeAsString(simpName));
        decl = findDeclaration((SimpleName) simpName);
        if (decl != null) {
//          logE("DECLA: " + decl.getClass().getName());
          nodeLabel = getLabelIfType(new ASTNodeWrapper(decl),
                                     (SimpleName) simpName);
          //retLabelString = getNodeAsString(decl);
        } else {
//          logE("null");
          if (scrollOnly) {
            editor.statusMessage(simpName + " is not defined in this sketch",
                                 DebugEditor.STATUS_ERR);
          }
        }

//        log(getNodeAsString(decl));

        /*
        // - findDecl3 testing

        ASTNode nearestNode = findClosestNode(lineNumber,
                                              (ASTNode) compilationUnit.types()
                                                  .get(0));
        ClassMember cmem = resolveExpression3rdParty(nearestNode,
                                                     (SimpleName) simpName,
                                                     false);
        if (cmem != null) {
          log("CMEM-> " + cmem);
        } else
          log("CMEM-> null");
        */
      }
    }

    if (decl != null && scrollOnly) {
      /*
       * For scrolling, we highlight just the name of the node, i.e., a
       * SimpleName instance. But the declared node always points to the
       * declared node itself, like TypeDecl, MethodDecl, etc. This is important
       * since it contains all the properties.
       */
      ASTNode simpName2 = getNodeName(decl, nameOfNode);
//      logE("FINAL String decl: " + getNodeAsString(decl));
//      logE("FINAL String label: " + getNodeAsString(simpName2));
      //errorCheckerService.highlightNode(simpName2);
      ASTNodeWrapper declWrap = new ASTNodeWrapper(simpName2, nodeLabel);
      //errorCheckerService.highlightNode(declWrap);
      if (!declWrap.highlightNode(this)) {
        logE("Highlighting failed.");
      }
    }

    // Return the declaration wrapped as ASTNodeWrapper
    return new ASTNodeWrapper(decl, nodeLabel);
  }

  /**
   * Given a declaration type astnode, returns the SimpleName peroperty
   * of that node.
   * @param node
   * @param name - The name we're looking for.
   * @return SimpleName
   */
  protected static ASTNode getNodeName(ASTNode node, String name){
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
  public static int getLineNumber(ASTNode node) {
    return ((CompilationUnit) node.getRoot()).getLineNumber(node
        .getStartPosition());
  }
  
  public static int getLineNumber(ASTNode node, int pos) {
    return ((CompilationUnit) node.getRoot()).getLineNumber(pos);
  }

  public static void main(String[] args) {
    //traversal2();
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
    log(CompilationUnit.propertyDescriptors(AST.JLS4).size());

    DefaultMutableTreeNode astTree = new DefaultMutableTreeNode(
                                                                "CompilationUnit");
    logE("Errors: " + cu.getProblems().length);
    visitRecur(cu, astTree);
    log(astTree.getChildCount());

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
      log(found);
    }
  }
  
  final ASTGenerator thisASTGenerator = this;
  
  protected void addListeners(){
    jtree.addTreeSelectionListener(new TreeSelectionListener() {
      
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        log(e);
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
            if (tnode.getUserObject() instanceof ASTNodeWrapper) {
              ASTNodeWrapper awrap = (ASTNodeWrapper) tnode.getUserObject();
              awrap.highlightNode(thisASTGenerator);
              // errorCheckerService.highlightNode(awrap);
              
              //--
              try {
                int javaLineNumber = getLineNumber(awrap.getNode());
                int pdeOffs[] = errorCheckerService
                    .calculateTabIndexAndLineNumber(javaLineNumber);
                PlainDocument javaSource = new PlainDocument();
                javaSource.insertString(0, errorCheckerService.sourceCode, null);
                Element lineElement = javaSource.getDefaultRootElement()
                    .getElement(javaLineNumber-1);
                if(lineElement == null) {
                  return;
                }
                
                String javaLine = javaSource.getText(lineElement.getStartOffset(),
                                                     lineElement.getEndOffset()
                                                         - lineElement.getStartOffset());
                editor.getSketch().setCurrentCode(pdeOffs[0]);
                String pdeLine = editor.getLineText(pdeOffs[1]);
                //String lookingFor = nodeName.toString();
                //log(lookingFor + ", " + nodeName.getStartPosition());
                log("JL " + javaLine + " LSO " + lineElement.getStartOffset() + ","
                    + lineElement.getEndOffset());
                log("PL " + pdeLine);
              } catch (BadLocationException e) {
                e.printStackTrace();
              }
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
           refactorIt();
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
            handleShowUsage();
          }
        };
        worker.execute();
      }
    });
        
    treeRename.addTreeSelectionListener(new TreeSelectionListener() {
      
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        log(e);
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
            
            if (tnode.getUserObject() instanceof ASTNodeWrapper) {
              ASTNodeWrapper awrap = (ASTNodeWrapper) tnode.getUserObject();
              //errorCheckerService.highlightNode(awrap);
              awrap.highlightNode(thisASTGenerator);
            }
          }
        };
        worker.execute();
      }
    });
  }
  
  protected void refactorIt(){
    String newName = txtRenameField.getText().trim();
    String selText = lastClickedWord == null ? editor.ta.getSelectedText()
        : lastClickedWord;
    // Find all occurrences of last clicked word
    DefaultMutableTreeNode defCU = findAllOccurrences(); //TODO: Repetition here
    if(defCU == null){
      editor.statusMessage("Can't locate definition of " + selText, 
                           DebugEditor.STATUS_ERR);
      return;
    }
    
    // Verify if the new name is a valid java identifier
    if(!newName.matches("([a-zA-Z][a-zA-Z0-9_]*)|([_][a-zA-Z0-9_]+)"))
    {
      JOptionPane.showConfirmDialog(new JFrame(), newName
          + " isn't a valid name.", "Uh oh..", JOptionPane.PLAIN_MESSAGE);
      return;
    }
    //else log("New name looks K.");
    
    errorCheckerService.pauseThread();
    if(treeRename.isVisible()){
      treeRename.setModel(new DefaultTreeModel(defCU));
      ((DefaultTreeModel) treeRename.getModel()).reload();
    }
//    frmOccurenceList.setTitle("Usage of \"" + selText + "\" : "
//        + defCU.getChildCount() + " time(s)");
//    frmOccurenceList.setLocation(editor.getX() + editor.getWidth(),editor.getY());
//    frmOccurenceList.setVisible(true);
    int lineOffsetDisplacementConst = newName.length()
        - selText.length();
    HashMap<Integer, Integer> lineOffsetDisplacement = new HashMap<Integer, Integer>();

    // I need to store the pde and java offsets beforehand because once
    // the replace starts, all offsets returned are affected
    //int offsetsMap[][][] = new int[defCU.getChildCount()][2][];
    int pdeOffsets[][] = new int[defCU.getChildCount()][3];
    for (int i = 0; i < defCU.getChildCount(); i++) {
      ASTNodeWrapper awrap = (ASTNodeWrapper) ((DefaultMutableTreeNode) (defCU
          .getChildAt(i))).getUserObject();
      int ans[] = errorCheckerService.calculateTabIndexAndLineNumber(awrap
          .getLineNumber());
      pdeOffsets[i][0] = ans[0];
      pdeOffsets[i][1] = ans[1];
      pdeOffsets[i][2] = awrap.getPDECodeOffsetForSN(this);
    }
    
    editor.startCompoundEdit();
    for (int i = 0; i < defCU.getChildCount(); i++) {
      ASTNodeWrapper awrap = (ASTNodeWrapper) ((DefaultMutableTreeNode) (defCU
          .getChildAt(i))).getUserObject();
      // correction for pde enhancements related displacement on a line
      int off = 0;
      if (lineOffsetDisplacement.get(awrap.getLineNumber()) != null) {
        off = lineOffsetDisplacement.get(awrap.getLineNumber());

        lineOffsetDisplacement.put(awrap.getLineNumber(),
                                   lineOffsetDisplacementConst + off);
      } else {
        lineOffsetDisplacement.put(awrap.getLineNumber(),
                                   lineOffsetDisplacementConst);
      }
//      logE(getNodeAsString(awrap.getNode()) + ", T:" + pdeOffsets[i][0]
//          + ", L:" + pdeOffsets[i][1] + ", O:" + pdeOffsets[i][2]);
      highlightPDECode(pdeOffsets[i][0],
                       pdeOffsets[i][1], pdeOffsets[i][2]
                           + off, awrap.getNode()
                           .toString().length());
      //int k = JOptionPane.showConfirmDialog(new JFrame(), "Rename?","", JOptionPane.INFORMATION_MESSAGE);
      editor.ta.setSelectedText(newName);
    }
    editor.stopCompoundEdit();
    errorCheckerService.resumeThread();
    editor.getSketch().setModified(true);
    errorCheckerService.runManualErrorCheck();
//    frmOccurenceList.setVisible(false);
    frmRename.setVisible(false);
    lastClickedWord = null;
    lastClickedWordNode = null;
  }
  
  /**
   * Highlights text in the editor
   * @param tab
   * @param lineNumber
   * @param lineStartWSOffset - line start offset including initial white space
   * @param length
   */
  public void highlightPDECode(int tab, int lineNumber, int lineStartWSOffset,
                               int length) {
//    log("ASTGen.highlightPDECode: T " + tab + ",L: " + lineNumber + ",LSO: "
//        + lineStartWSOffset + ",Len: " + length);
    editor.toFront();
    editor.getSketch().setCurrentCode(tab);
    lineStartWSOffset += editor.ta.getLineStartOffset(lineNumber);
    editor.ta.select(lineStartWSOffset, lineStartWSOffset + length);
  }
  
  public void handleShowUsage(){
    if(editor.hasJavaTabs) return; // show usage disabled if java tabs
    log("Last clicked word:" + lastClickedWord);
    if(lastClickedWord == null && editor.ta.getSelectedText() == null){
      editor.statusMessage("Highlight the class/function/variable name first"
                           , DebugEditor.STATUS_INFO);
      return;
    }
    
    if(errorCheckerService.hasSyntaxErrors()){
      editor.statusMessage("Can't perform action until syntax errors are " +
      		"fixed :(", DebugEditor.STATUS_WARNING);
      return;
    }
    DefaultMutableTreeNode defCU = findAllOccurrences();   
    String selText = lastClickedWord == null ? editor.ta.getSelectedText()
        : lastClickedWord;
    if(defCU == null){
      editor.statusMessage("Can't locate definition of " + selText, 
                           DebugEditor.STATUS_ERR);
      return;
    }
    if(defCU.getChildCount() == 0)
      return;
    treeRename.setModel(new DefaultTreeModel(defCU));
    ((DefaultTreeModel) treeRename.getModel()).reload();
    treeRename.setRootVisible(false);
    frmOccurenceList.setTitle("Usage of \"" + selText + "\" : "
        + defCU.getChildCount() + " time(s)");
    frmOccurenceList.setLocation(editor.getX() + editor.getWidth(),editor.getY());
    frmOccurenceList.setVisible(true);
    lastClickedWord = null;
    lastClickedWordNode = null;
  }
  
  protected String lastClickedWord = null;
  protected ASTNodeWrapper lastClickedWordNode = null;
  
  public String getLastClickedWord() {
    return lastClickedWord;
  }

  public void setLastClickedWord(int lineNumber, String lastClickedWord, int offset) {
    this.lastClickedWord = lastClickedWord;
    lastClickedWordNode = getASTNodeAt(lineNumber, lastClickedWord, offset, false);
    log("Last clicked node: " + lastClickedWordNode);
  }

  protected DefaultMutableTreeNode findAllOccurrences(){
    log("Last clicked word:" + lastClickedWord);
    String selText = lastClickedWord == null ? editor.ta.getSelectedText()
        : lastClickedWord;
    int line = editor.ta.getSelectionStartLine();
    log(selText
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
    ASTNodeWrapper wnode;
    if (lastClickedWord == null || lastClickedWordNode.getNode() == null) {
      wnode = getASTNodeAt(line + errorCheckerService.mainClassOffset, selText,
                           editor.ta.getSelectionStart() - offwhitespace, false);
    }
    else{
      wnode = lastClickedWordNode;
    }
    if(wnode.getNode() == null){
      return null;
    }
    logE("Gonna find all occurrences of "
        + getNodeAsString(wnode.getNode()));
    
    //If wnode is a constructor, find the TD instead.
    if (wnode.getNodeType() == ASTNode.METHOD_DECLARATION) {
      MethodDeclaration md = (MethodDeclaration) wnode.getNode();
      ASTNode node = md.getParent();
      while (node != null) {
        if (node instanceof TypeDeclaration) {
          // log("Parent class " + getNodeAsString(node));
          break;
        }
        node = node.getParent();
      }
      if(node != null && node instanceof TypeDeclaration){
        TypeDeclaration td = (TypeDeclaration) node;
        if(td.getName().toString().equals(md.getName().toString())){
          logE("Renaming constructor of " + getNodeAsString(td));
          wnode = new ASTNodeWrapper(td);
        }
      }
    }
    
    DefaultMutableTreeNode defCU = new DefaultMutableTreeNode(
                                                              new ASTNodeWrapper(
                                                                                 wnode
                                                                                     .getNode(),
                                                                                 selText));
    dfsNameOnly(defCU, wnode.getNode(), selText);
    
    // Reverse the list obtained via dfs
    Stack<Object> tempS = new Stack<Object>();
    for (int i = 0; i < defCU.getChildCount(); i++) {
      tempS.push(defCU.getChildAt(i));
    }
    defCU.removeAllChildren();
    while (!tempS.isEmpty()) {
      defCU.add((MutableTreeNode) tempS.pop());
    }
    log(wnode);
    
    return defCU;
  }

  @SuppressWarnings({ "unchecked" })
  /**
   * Generates AST Swing component 
   * @param node
   * @param tnode
   */
  public static void visitRecur(ASTNode node, DefaultMutableTreeNode tnode) {
    Iterator<StructuralPropertyDescriptor> it = 
        node.structuralPropertiesForType().iterator();
    //logE("Props of " + node.getClass().getName());
    DefaultMutableTreeNode ctnode = null;
    while (it.hasNext()) {
      StructuralPropertyDescriptor prop = it.next();

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
//      log("Visiting: " + getNodeAsString(awnode.getNode()));
      if(isInstanceOfType(awnode.getNode(), decl, name)){
        int val[] = errorCheckerService
            .JavaToPdeOffsets(awnode.getLineNumber(), 0);
        tnode.add(new DefaultMutableTreeNode(new ASTNodeWrapper(awnode
            .getNode(), "Line " + (val[1] + 1) + " | Tab: "
            + editor.getSketch().getCode(val[0]).getPrettyName())));
      }
      
    }
  }
  
  public ASTNode dfsLookForASTNode(ASTNode root, String name, int startOffset,
                                   int endOffset) {
//    log("dfsLookForASTNode() lookin for " + name + " Offsets: " + startOffset
//        + "," + endOffset);
    Stack<ASTNode> stack = new Stack<ASTNode>();
    stack.push(root);

    while (!stack.isEmpty()) {
      ASTNode node = stack.pop();
      //log("Popped from stack: " + getNodeAsString(node));
      Iterator<StructuralPropertyDescriptor> it = 
          node.structuralPropertiesForType().iterator();
      while (it.hasNext()) {
        StructuralPropertyDescriptor prop = it.next();

        if (prop.isChildProperty() || prop.isSimpleProperty()) {
          if (node.getStructuralProperty(prop) instanceof ASTNode) {
            ASTNode temp = (ASTNode) node.getStructuralProperty(prop);
            if (temp.getStartPosition() <= startOffset
                && (temp.getStartPosition() + temp.getLength()) >= endOffset) {
              if(temp instanceof SimpleName){
                if(name.equals(temp.toString())){
//                  log("Found simplename: " + getNodeAsString(temp));
                  return temp;
                }
//                log("Bummer, didn't match");
              }
              else
                stack.push(temp);
                //log("Pushed onto stack: " + getNodeAsString(temp));
            }
          }
        }
        else if (prop.isChildListProperty()) {
          List<ASTNode> nodelist = (List<ASTNode>) node
              .getStructuralProperty(prop);
          for (ASTNode temp : nodelist) {
            if (temp.getStartPosition() <= startOffset
                && (temp.getStartPosition() + temp.getLength()) >= endOffset) {
                stack.push(temp);
//                log("Pushed onto stack: " + getNodeAsString(temp));
                if(temp instanceof SimpleName){
                  if(name.equals(temp.toString())){
//                    log("Found simplename: " + getNodeAsString(temp));
                    return temp;
                  }
//                  log("Bummer, didn't match");
                }
                else
                  stack.push(temp);
                  //log("Pushed onto stack: " + getNodeAsString(temp));
            }
          }
        }
      }
    }
//    log("dfsLookForASTNode() not found " + name);
    return null;
  }
  
  protected SketchOutline sketchOutline;
  protected void showSketchOutline(){
    if(editor.hasJavaTabs) return; // sketch outline disabled if java tabs
    sketchOutline = new SketchOutline(codeTree, errorCheckerService);
    sketchOutline.show();
  }
  
  protected void showTabOutline(){
    new TabOutline(errorCheckerService).show();
  }
  
  public int javaCodeOffsetToLineStartOffset(int line, int jOffset){
    // Find the first node with this line number, return its offset - jOffset
    line = pdeLineNumToJavaLineNum(line);
    log("Looking for line: " + line + ", jOff " + jOffset);
    Stack temp = new Stack<DefaultMutableTreeNode>();
    temp.push(codeTree);

    while (!temp.isEmpty()) {
      DefaultMutableTreeNode cnode = (DefaultMutableTreeNode) temp.pop();
      for (int i = 0; i < cnode.getChildCount(); i++) {
        temp.push(cnode.getChildAt(i));
      }

      if (!(cnode.getUserObject() instanceof ASTNodeWrapper))
        continue;
      ASTNodeWrapper awnode = (ASTNodeWrapper) cnode.getUserObject();
//      log("Visiting: " + getNodeAsString(awnode.getNode()));
      if (awnode.getLineNumber() == line) {
        log("First element with this line no is: " + awnode
            + "LSO: " + (jOffset - awnode.getNode().getStartPosition()));
        return (jOffset - awnode.getNode().getStartPosition());
      }
    }    
    return -1;
  }
  
  /**
   * Converts pde line number to java line number
   * @param pdeLineNum - pde line number
   * @return
   */
  protected int pdeLineNumToJavaLineNum(int pdeLineNum){
    int javaLineNumber = pdeLineNum + errorCheckerService.getPdeImportsCount();
    // Adjust line number for tabbed sketches
    int codeIndex = editor.getSketch().getCodeIndex(editor.getCurrentTab());
    if (codeIndex > 0)
      for (int i = 0; i < codeIndex; i++) {
        SketchCode sc = editor.getSketch().getCode(i);
        int len = Base.countLines(sc.getProgram()) + 1;
        javaLineNumber += len;
      }
    return javaLineNumber;
  }
  
  protected boolean isInstanceOfType(ASTNode node,ASTNode decl, String name){
    if(node instanceof SimpleName){
      SimpleName sn = (SimpleName) node;
      
      if (sn.toString().equals(name)) {
        ArrayList<ASTNode> nodesToBeMatched = new ArrayList<ASTNode>();
        nodesToBeMatched.add(decl);
        if(decl instanceof TypeDeclaration){
          log("decl is a TD");
          TypeDeclaration td = (TypeDeclaration)decl;
          MethodDeclaration[] mlist = td.getMethods();
          for (MethodDeclaration md : mlist) {
            if(md.getName().toString().equals(name)){
              nodesToBeMatched.add(md);
            }
          }
        }
        log("Visiting: " + getNodeAsString(node));
        ASTNode decl2 = findDeclaration(sn);
        logE("It's decl: " + getNodeAsString(decl2));
        log("But we need: "+getNodeAsString(decl));
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
    if(editor.hasJavaTabs) return; // refactoring disabled if java tabs
    log("Last clicked word:" + lastClickedWord);
    if(lastClickedWord == null && editor.ta.getSelectedText() == null){
      editor.statusMessage("Highlight the class/function/variable name first",
                           DebugEditor.STATUS_INFO);
      return;
    }
    
    if(errorCheckerService.hasSyntaxErrors()){
      editor
          .statusMessage("Can't perform action until syntax errors are fixed :(",
                         DebugEditor.STATUS_WARNING);
      return;
    }
    
    DefaultMutableTreeNode defCU = findAllOccurrences();   
    String selText = lastClickedWord == null ? editor.ta.getSelectedText()
        : lastClickedWord;
    if(defCU == null){
      editor.statusMessage(selText + " isn't defined in this sketch, so it can't" +
      		" be renamed", DebugEditor.STATUS_ERR);
      return;
    }
    if (!frmRename.isVisible()){
      frmRename.setLocation(editor.getX()
                            + (editor.getWidth() - frmRename.getWidth()) / 2,
                        editor.getY()
                            + (editor.getHeight() - frmRename.getHeight())
                            / 2);
      frmRename.setVisible(true);
      SwingUtilities.invokeLater(new Runnable() {          
        @Override
        public void run() {
          String selText = lastClickedWord == null ? editor.ta.getSelectedText()
              : lastClickedWord;
          frmOccurenceList.setTitle("All occurrences of "
              + selText);
          lblRefactorOldName.setText("Current name: "
              + selText);
          txtRenameField.setText("");
          txtRenameField.requestFocus();
        }
      });
    }
    frmRename.toFront();
  }
  

  public static void printRecur(ASTNode node) {
    Iterator<StructuralPropertyDescriptor> it = node
        .structuralPropertiesForType().iterator();
    //logE("Props of " + node.getClass().getName());
    while (it.hasNext()) {
      StructuralPropertyDescriptor prop = it.next();

      if (prop.isChildProperty() || prop.isSimpleProperty()) {
        if (node.getStructuralProperty(prop) != null) {
//          System.out
//              .println(node.getStructuralProperty(prop) + " -> " + (prop));
          if (node.getStructuralProperty(prop) instanceof ASTNode) {
            ASTNode cnode = (ASTNode) node.getStructuralProperty(prop);
            log(getNodeAsString(cnode));
            printRecur(cnode);
          }
        }
      }

      else if (prop.isChildListProperty()) {
        List<ASTNode> nodelist = (List<ASTNode>) node
            .getStructuralProperty(prop);
        for (ASTNode cnode : nodelist) {
          log(getNodeAsString(cnode));
          printRecur(cnode);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  protected static ASTNode findLineOfNode(ASTNode node, int lineNumber,
                                        int offset, String name) {

    CompilationUnit root = (CompilationUnit) node.getRoot();
//    log("Inside "+getNodeAsString(node) + " | " + root.getLineNumber(node.getStartPosition()));
    if (root.getLineNumber(node.getStartPosition()) == lineNumber) {
      // logE(3 + getNodeAsString(node) + " len " + node.getLength());      
      return node;
//      if (offset < node.getLength())
//        return node;
//      else {
//        logE(-11);
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
//              logE(11 + getNodeAsString(retNode));
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
//              logE(12 + getNodeAsString(rr));
            return rr;
          }
        }
      }
    }
//    logE("-1");
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
    //log("pinpointOnLine node class: " + node.getClass().getSimpleName());
    if (node instanceof SimpleName) {
      SimpleName sn = (SimpleName) node;
      //log(offset+ "off,pol " + getNodeAsString(sn));
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
//              logE(11 + getNodeAsString(retNode));
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
//              logE(12 + getNodeAsString(rr));
            return rr;
          }
        }
      }
    }
//    logE("-1");
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
  protected static ASTNode findDeclaration(Name findMe) {
    
    // WARNING: You're entering the Rube Goldberg territory of Experimental Mode.
    // To debug this code, thou must take the Recursive Leap of Faith.
    
    // log("entering --findDeclaration1 -- " + findMe.toString());
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
//          log("MI EXP: " + exp.toString() + " of type "
//              + exp.getClass().getName() + " parent: " + exp.getParent());
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
//            log("MI.SN " + getNodeAsString(declaringClass));
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
//          log("FA EXP: " + exp.toString() + " of type "
//              + exp.getClass().getName() + " parent: " + exp.getParent());
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
//            log("FA.SN " + getNodeAsString(declaringClass));
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
//        log(qn.getQualifier() + "->" + qn.getName());
        declaringClass = findDeclaration(stp.getName());
        
//        log("QN decl class: " + getNodeAsString(declaringClass));
        constrains.clear();
        constrains.add(ASTNode.TYPE_DECLARATION);
        constrains.add(ASTNode.FIELD_DECLARATION);
        return definedIn(declaringClass, qn.getName().toString(), constrains,
                         null);
      }
      else{
        if(findMe instanceof QualifiedName){
          QualifiedName qnn = (QualifiedName) findMe;
//          log("findMe is a QN, "
//              + (qnn.getQualifier().toString() + " other " + qnn.getName()
//                  .toString()));

          SimpleType stp = extracTypeInfo(findDeclaration((qnn.getQualifier())));
//          log(qnn.getQualifier() + "->" + qnn.getName());
          declaringClass = findDeclaration(stp.getName());

//          log("QN decl class: "
//              + getNodeAsString(declaringClass));
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
//      log("findDeclaration1 -> " + getNodeAsString(parent));
      for (Object oprop : parent.structuralPropertiesForType()) {
        StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) oprop;
        if (prop.isChildProperty() || prop.isSimpleProperty()) {
          if (parent.getStructuralProperty(prop) instanceof ASTNode) {
//            log(prop + " C/S Prop of -> "
//                + getNodeAsString(parent));
            ret = definedIn((ASTNode) parent.getStructuralProperty(prop),
                            findMe.toString(), constrains, declaringClass);
            if (ret != null)
              return ret;
          }
        } else if (prop.isChildListProperty()) {
//          log((prop) + " ChildList props of "
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
  protected static ASTNode findDeclaration2(Name findMe, ASTNode alternateParent) {
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
//          log("MI EXP: " + exp.toString() + " of type "
//              + exp.getClass().getName() + " parent: " + exp.getParent());
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
//            log("MI.SN " + getNodeAsString(declaringClass));
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
//          log("FA EXP: " + exp.toString() + " of type "
//              + exp.getClass().getName() + " parent: " + exp.getParent());
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
//            log("FA.SN " + getNodeAsString(declaringClass));
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
//        log(qn.getQualifier() + "->" + qn.getName());
//        log("QN decl class: " + getNodeAsString(declaringClass));
        constrains.clear();
        constrains.add(ASTNode.TYPE_DECLARATION);
        constrains.add(ASTNode.FIELD_DECLARATION);
        return definedIn(declaringClass, qn.getName().toString(), constrains,
                         null);
      }
      else{
        if(findMe instanceof QualifiedName){
          QualifiedName qnn = (QualifiedName) findMe;
//          log("findMe is a QN, "
//              + (qnn.getQualifier().toString() + " other " + qnn.getName()
//                  .toString()));

          SimpleType stp = extracTypeInfo(findDeclaration2((qnn.getQualifier()), alternateParent));
//          log(qnn.getQualifier() + "->" + qnn.getName());
          declaringClass = findDeclaration2(stp.getName(), alternateParent);

//          log("QN decl class: "
//              + getNodeAsString(declaringClass));
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
//    log("Alternate parent: " + getNodeAsString(alternateParent));
    while (alternateParent != null) {
//      log("findDeclaration2 -> "
//          + getNodeAsString(alternateParent));
      for (Object oprop : alternateParent.structuralPropertiesForType()) {
        StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) oprop;
        if (prop.isChildProperty() || prop.isSimpleProperty()) {
          if (alternateParent.getStructuralProperty(prop) instanceof ASTNode) {
//            log(prop + " C/S Prop of -> "
//                + getNodeAsString(alternateParent));
            ret = definedIn((ASTNode) alternateParent
                                .getStructuralProperty(prop),
                            findMe.toString(), constrains, declaringClass);
            if (ret != null)
              return ret;
          }
        } else if (prop.isChildListProperty()) {
//          log((prop) + " ChildList props of "
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
  
  
  protected List<Comment> getCodeComments(){
    List<Comment> commentList = compilationUnit.getCommentList();
//    log("Total comments: " + commentList.size());
//    int i = 0;
//    for (Comment comment : commentList) {
//      log(++i + ": "+comment + " Line:"
//          + compilationUnit.getLineNumber(comment.getStartPosition()) + ", "
//          + comment.getLength());
//    }
    return commentList;
  }
  
  protected boolean caretWithinLineComment(){
    String pdeLine = editor.getLineText(editor.textArea().getCaretLine()).trim();
    int caretPos = editor.textArea().getCaretPosition()
        - editor.textArea()
            .getLineStartNonWhiteSpaceOffset(editor.textArea().getCaretLine());
    int x = pdeLine.indexOf("//");
//    log(x + " , " + caretPos + ", Checking line for comment " + pdeLine);
    //lineStartOffset = editor.textArea().
    
    if (x >= 0 && caretPos > x) {
//      log("INSIDE a comment");
      return true;
    }
//    log("not within comment");
    return false;
  }
  
  /**
   * A wrapper for java.lang.reflect types.
   * Will have to see if the usage turns out to be internal only here or not
   * and then accordingly decide where to place this class.
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
      if(node instanceof TypeDeclaration){
        declaringNode = node;
      }
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
  public static SimpleType extracTypeInfo(ASTNode node) {
    if (node == null) {
      return null;
    }
    Type t = extracTypeInfo2(node);
    if (t instanceof PrimitiveType) {
      return null;
    } else if (t instanceof ArrayType) {
      ArrayType at = (ArrayType) t;
      log(at.getComponentType() + " <-comp type, ele type-> "
          + at.getElementType() + ", "
          + at.getElementType().getClass().getName());
      if (at.getElementType() instanceof PrimitiveType) {
        return null;
      } else if (at.getElementType() instanceof SimpleType) {
        return (SimpleType) at.getElementType();
      } else
        return null;
    } else if (t instanceof ParameterizedType) {
      ParameterizedType pmt = (ParameterizedType) t;
      log(pmt.getType() + ", " + pmt.getType().getClass());
      if (pmt.getType() instanceof SimpleType) {
        return (SimpleType) pmt.getType();
      } else
        return null;
    }
    return (SimpleType) t;
  }
  
  public static Type extracTypeInfo2(ASTNode node) {
    if (node == null)
      return null;
    switch (node.getNodeType()) {
    case ASTNode.METHOD_DECLARATION:
      return ((MethodDeclaration) node).getReturnType2();
    case ASTNode.FIELD_DECLARATION:
      return ((FieldDeclaration) node).getType();
    case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
      return  ((VariableDeclarationExpression) node).getType();
    case ASTNode.VARIABLE_DECLARATION_STATEMENT:
      return  ((VariableDeclarationStatement) node).getType();
    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      return  ((SingleVariableDeclaration) node).getType();
    case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
      return extracTypeInfo2(node.getParent());
    }
    log("Unknown type info request " + getNodeAsString(node));
    return null;
  }

  @SuppressWarnings("unchecked")
  protected static ASTNode definedIn(ASTNode node, String name,
                                   ArrayList<Integer> constrains,
                                   ASTNode declaringClass) {
    if (node == null)
      return null;
    if (constrains != null) {
//      log("Looking at " + getNodeAsString(node) + " for " + name
//          + " in definedIn");
      if (!constrains.contains(node.getNodeType()) && constrains.size() > 0) {
//        System.err.print("definedIn -1 " + " But constrain was ");
//        for (Integer integer : constrains) {
//          System.out.print(ASTNode.nodeClassForType(integer) + ",");
//        }
//        log();
        return null;
      }
    }

    List<VariableDeclarationFragment> vdfList = null;
    switch (node.getNodeType()) {

    case ASTNode.TYPE_DECLARATION:
      //logE(getNodeAsString(node));
      TypeDeclaration td = (TypeDeclaration) node;
      if (td.getName().toString().equals(name)) {
        if (constrains.contains(ASTNode.CLASS_INSTANCE_CREATION)) {
          // look for constructor;
          MethodDeclaration[] methods = td.getMethods();
          for (MethodDeclaration md : methods) {
            if (md.getName().toString().equalsIgnoreCase(name)) {
              log("Found a constructor.");
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
              if (vdf.getName().toString().equalsIgnoreCase(name))
                return fd;
            }
          }
        } else if (constrains.contains(ASTNode.METHOD_DECLARATION)) {
          // look for methods
          MethodDeclaration[] methods = td.getMethods();
          for (MethodDeclaration md : methods) {
            if (md.getName().toString().equalsIgnoreCase(name)) {
              return md;
            }
          }
        }
      }
      break;
    case ASTNode.METHOD_DECLARATION:
      //logE(getNodeAsString(node));
      if (((MethodDeclaration) node).getName().toString().equalsIgnoreCase(name))
        return node;
      break;
    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      //logE(getNodeAsString(node));
      if (((SingleVariableDeclaration) node).getName().toString().equalsIgnoreCase(name))
        return node;
      break;
    case ASTNode.FIELD_DECLARATION:
      //logE("FD" + node);
      vdfList = ((FieldDeclaration) node).fragments();
      break;
    case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
      //logE("VDE" + node);
      vdfList = ((VariableDeclarationExpression) node).fragments();
      break;
    case ASTNode.VARIABLE_DECLARATION_STATEMENT:
      //logE("VDS" + node);
      vdfList = ((VariableDeclarationStatement) node).fragments();
      break;

    default:

    }
    if (vdfList != null) {
      for (VariableDeclarationFragment vdf : vdfList) {
        if (vdf.getName().toString().equalsIgnoreCase(name))
          return node;
      }
    }
    return null;
  }
  protected JFrame frmImportSuggest;
  public void suggestImports(final String className){
    if(frmImportSuggest != null)
      if(frmImportSuggest.isVisible())
      return;
    log("Looking for class " + className);
    RegExpResourceFilter regf = new RegExpResourceFilter(
                                                         Pattern.compile(".*"),
                                                         Pattern
                                                             .compile(className
                                                                          + ".class",
                                                                      Pattern.CASE_INSENSITIVE));
    String[] resources = classPath
        .findResources("", regf);
    ArrayList<String> candidates = new ArrayList<String>();
    for (String res : resources) {
      candidates.add(res);
    }
    
    // log("Couldn't find import for class " + className);

    for (Library lib : editor.dmode.contribLibraries) {
      ClassPath cp = factory.createFromPath(lib.getClassPath());
      resources = cp.findResources("", regf);
      for (String res : resources) {
        candidates.add(res);
        log("Res: " + res);
      }
    }
    
    if (editor.getSketch().hasCodeFolder()) {
      File codeFolder = editor.getSketch().getCodeFolder();
      // get a list of .jar files in the "code" folder
      // (class files in subfolders should also be picked up)
      ClassPath cp = factory.createFromPath(Base
                                            .contentsToClassPath(codeFolder));
      resources = cp.findResources("", regf);
      for (String res : resources) {
        candidates.add(res);
        log("Res: " + res);
      }
    }

    resources = new String[candidates.size()];
    for (int i = 0; i < resources.length; i++) {
      resources[i] = candidates.get(i).replace('/', '.')
          .substring(0, candidates.get(i).length() - 6);
    }
    if (resources.length >= 1) {
      final JList classList = new JList(resources);
      classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      frmImportSuggest = new JFrame();
      frmImportSuggest.setSize(350, 200);
      Toolkit.setIcon(frmImportSuggest);
      frmImportSuggest.setLayout(new BoxLayout(frmImportSuggest
          .getContentPane(), BoxLayout.Y_AXIS));
      ((JComponent) frmImportSuggest.getContentPane()).setBorder(BorderFactory
          .createEmptyBorder(5, 5, 5, 5));
      JLabel lbl = new JLabel("<html>The class \"" + className
          + "\" couldn't be determined. You are probably missing one of the following imports:</html>");
      JScrollPane jsp = new JScrollPane();
      jsp.setViewportView(classList);
      JButton btnInsertImport = new JButton("Insert import");
      btnInsertImport.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          if (classList.getSelectedValue() != null) {
            try {
              String impString = "import " + classList.getSelectedValue()
                  + ";\n";
              int ct = editor.getSketch().getCurrentCodeIndex();
              editor.getSketch().setCurrentCode(0);
              editor.textArea().getDocument().insertString(0, impString, null);
              editor.getSketch().setCurrentCode(ct);
              errorCheckerService.runManualErrorCheck();
              frmImportSuggest.setVisible(false);
              frmImportSuggest = null;
            } catch (BadLocationException e) {
              log("Failed to insert import for " + className);
              e.printStackTrace();
            }
          }
        }
      });
      
      JButton btnCancel = new JButton("Cancel");
      btnCancel.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          frmImportSuggest.setVisible(false);
        }
      });
      
      JPanel panelTop = new JPanel(), panelBottom = new JPanel(), panelLabel = new JPanel();
      panelTop.setLayout(new BoxLayout(panelTop, BoxLayout.Y_AXIS));
      panelTop.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      panelLabel.setLayout(new BorderLayout());
      panelLabel.add(lbl,BorderLayout.CENTER);
      panelTop.add(panelLabel);
      panelTop.add(Box.createRigidArea(new Dimension(1, 5)));
      panelTop.add(jsp);
      panelBottom.setLayout(new BoxLayout(panelBottom, BoxLayout.X_AXIS));
      panelBottom.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      panelBottom .setLayout(new BoxLayout(panelBottom, BoxLayout.X_AXIS));
      panelBottom.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      panelBottom.add(Box.createHorizontalGlue());
      panelBottom.add(btnInsertImport);
      panelBottom.add(Box.createRigidArea(new Dimension(15, 0)));
      panelBottom.add(btnCancel);    

//      frmImportSuggest.add(lbl);
//      frmImportSuggest.add(jsp);
//      frmImportSuggest.add(btnInsertImport);
      frmImportSuggest.add(panelTop);
      frmImportSuggest.add(panelBottom);
      frmImportSuggest.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      frmImportSuggest.setTitle("Import Suggestion");
      frmImportSuggest.setLocation(editor.getX()
                            + (editor.getWidth() - frmImportSuggest.getWidth()) / 2,
                        editor.getY()
                            + (editor.getHeight() - frmImportSuggest.getHeight())
                            / 2);
      editor.ta.hideSuggestion();
      frmImportSuggest.setVisible(true);
    }

  }
  
  public void disposeAllWindows() {
    disposeWindow(frmASTView, frameAutoComp, frmImportSuggest,
                  frmOccurenceList, frmRename);
  }
  
  public static void disposeWindow(JFrame... f) {
    for (JFrame jFrame : f) {
      if(jFrame != null)
        jFrame.dispose();
    }
  }

  public static final String ignoredImports[] = {
    "com.oracle.", "sun.", "sunw.", "com.sun.", "javax.", "sunw.", "org.ietf.",
    "org.jcp.", "org.omg.", "org.w3c.", "org.xml.", "org.eclipse.", "com.ibm.",
    "org.netbeans.", "org.jsoup.", "org.junit.", "org.apache.", "antlr." };
  public static final String allowedImports[] = {"java.lang.", "java.util.", "java.io.", 
    "java.math.", "processing.core.", "processing.data.", "processing.event.", "processing.opengl."};
  protected boolean ignorableImport(String impName, String className) {
    //TODO: Trie man.
    for (ImportStatement impS : errorCheckerService.getProgramImports()) {
      if(impName.startsWith(impS.getPackageName()))
        return false;
    }
    for (String impS : allowedImports) {
      if(impName.startsWith(impS) && className.indexOf('.') == -1)
        return false;
    }
    return true;
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
  
  

  static protected String getNodeAsString(ASTNode node) {
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
  static protected String getNodeAsString2(ASTNode node) {
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
   // frmJavaDoc.setVisible(visible);
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
