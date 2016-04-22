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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
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
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import processing.app.Messages;
import processing.app.ui.EditorStatus;
import processing.app.ui.Toolkit;
import processing.mode.java.JavaEditor;

import com.google.classpath.ClassPath;
import com.google.classpath.RegExpResourceFilter;

@SuppressWarnings({ "unchecked" })
public class ASTGenerator {

  public static final boolean SHOW_DEBUG_TREE = false;

  protected final ErrorCheckerService errorCheckerService;
  protected final JavaEditor editor;

  protected final GUI gui;


  public ASTGenerator(JavaEditor editor, ErrorCheckerService ecs) {
    this.editor = editor;
    this.errorCheckerService = ecs;
    gui = new GUI(editor, this);
    //addCompletionPopupListner();
    //loadJavaDoc();
  }


  public static CompletionCandidate[] checkForTypes(ASTNode node) {

    List<VariableDeclarationFragment> vdfs = null;
    switch (node.getNodeType()) {
    case ASTNode.TYPE_DECLARATION:
      return new CompletionCandidate[]{new CompletionCandidate((TypeDeclaration) node)};

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
        cand[i + 1] = new CompletionCandidate((SingleVariableDeclaration) params.get(i));
      }
      return cand;

    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      return new CompletionCandidate[]{new CompletionCandidate((SingleVariableDeclaration) node)};

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
                                 true);
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
    ClassMember scopeParent;
    SimpleType stp;
    if(astNode instanceof SimpleName){
      ASTNode decl = findDeclaration2(((SimpleName)astNode),nearestNode);
      if(decl != null){
        // see if locally defined
        log(getNodeAsString(astNode)+" found decl -> " + getNodeAsString(decl));

        {
          if (decl.getNodeType() == ASTNode.TYPE_DECLARATION) {
            TypeDeclaration td = (TypeDeclaration) decl;
            return new ClassMember(td);
          }
        }

        { // Handle "array." x "array[1]."
          Type type = extracTypeInfo2(decl);
          if (type != null && type.isArrayType() &&
              astNode.getParent().getNodeType() != ASTNode.ARRAY_ACCESS) {
            // No array access, we want members of the array itself
            Type elementType = ((ArrayType) type).getElementType();

            // Get name of the element class
            String name = "";
            if (elementType.isSimpleType()) {
              Class<?> c = findClassIfExists(elementType.toString());
              if (c != null) name = c.getName();
            } else if (elementType.isPrimitiveType()) {
              name = ((PrimitiveType) elementType).getPrimitiveTypeCode().toString();
            }

            // Convert element class to array class
            Class<?> arrayClass = getArrayClass(name);

            return arrayClass == null ? null : new ClassMember(arrayClass);
          }
        }

        return new ClassMember(extracTypeInfo(decl));
      }
      else {
        // or in a predefined class?
        Class<?> tehClass = findClassIfExists(astNode.toString());
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
            Class<?> tehClass = findClassIfExists(fa.getExpression().toString());
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

        { // Handle "array." x "array[1]."
          Type type = extracTypeInfo2(temp);
          if (type != null && type.isArrayType() &&
              astNode.getParent().getNodeType() != ASTNode.ARRAY_ACCESS) {
            // No array access, we want members of the array itself
            Type elementType = ((ArrayType) type).getElementType();

            // Get name of the element class
            String name = "";
            if (elementType.isSimpleType()) {
              Class<?> c = findClassIfExists(elementType.toString());
              if (c != null) name = c.getName();
            } else if (elementType.isPrimitiveType()) {
              name = ((PrimitiveType) elementType).getPrimitiveTypeCode().toString();
            }

            // Convert element class to array class
            Class<?> arrayClass = getArrayClass(name);

            return arrayClass == null ? null : new ClassMember(arrayClass);
          }
        }

        return new ClassMember(extracTypeInfo(temp));
      }
      if (mi.getExpression() == null) {
//        if()
        //Local code or belongs to super class
        log("MI,Not implemented.");
        return null;
      } else {
        if (mi.getExpression() instanceof SimpleName) {
          ASTNode decl = findDeclaration2((SimpleName) mi.getExpression(),
                                          nearestNode);
          if (decl != null) {
            if (decl.getNodeType() == ASTNode.TYPE_DECLARATION) {
              TypeDeclaration td = (TypeDeclaration) decl;
              return new ClassMember(td);
            }

            stp = extracTypeInfo(decl);
            if(stp == null){
            /*The type wasn't found in local code, so it might be something like
             * System.console()., or maybe belonging to super class, etc.
             */
              Class<?> tehClass = findClassIfExists(mi.getExpression().toString());
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
              Class<?> tehClass = findClassIfExists(stp.getName().toString());
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
          }
        } else {
          log("MI EXP.."+getNodeAsString(mi.getExpression()));
//          return null;
          scopeParent = resolveExpression3rdParty(nearestNode,
                                                  mi.getExpression(), noCompare);
          log("MI, ScopeParent " + scopeParent);
          return definedIn3rdPartyClass(scopeParent, mi.getName().toString());
        }

      }
      break;
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
            Class<?> tehClass = findClassIfExists(qn.getQualifier().toString());
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

            Class<?> tehClass = findClassIfExists(stp.getName().toString());
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


  public Class<?> getArrayClass(String elementClass) {
    String name;
    if (elementClass.startsWith("[")) {
      // just add a leading "["
      name = "[" + elementClass;
    } else if (elementClass.equals("boolean")) {
      name = "[Z";
    } else if (elementClass.equals("byte")) {
      name = "[B";
    } else if (elementClass.equals("char")) {
      name = "[C";
    } else if (elementClass.equals("double")) {
      name = "[D";
    } else if (elementClass.equals("float")) {
      name = "[F";
    } else if (elementClass.equals("int")) {
      name = "[I";
    } else if (elementClass.equals("long")) {
      name = "[J";
    } else if (elementClass.equals("short")) {
      name = "[S";
    } else {
      // must be an object non-array class
      name = "[L" + elementClass + ";";
    }
    return loadClass(name);
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

    ArrayList<CompletionCandidate> candidates = new ArrayList<>();
    log("In GMFT(), Looking for match " + child
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
    ArrayList<CompletionCandidate> candidates = new ArrayList<>();
    log("getMemFoType-> Looking for match " + child
        + " inside " + tehClass + " noCompare " + noCompare + " staticOnly "
        + staticOnly);
    if(tehClass == null){
      return candidates;
    }
    // tehClass will either be a TypeDecl defined locally
    if(tehClass.getDeclaringNode() instanceof TypeDeclaration){
      TypeDeclaration td = (TypeDeclaration) tehClass.getDeclaringNode();
      {
        FieldDeclaration[] fields = td.getFields();
        for (FieldDeclaration field : fields) {
          if (staticOnly && !isStatic(field.modifiers())) {
            continue;
          }
          List<VariableDeclarationFragment> vdfs = field.fragments();
          for (VariableDeclarationFragment vdf : vdfs) {
            if (noCompare) {
              candidates.add(new CompletionCandidate(vdf));
            } else if (vdf.getName().toString().toLowerCase().startsWith(child))
              candidates.add(new CompletionCandidate(vdf));
          }
        }
      }
      {
        MethodDeclaration[] methods = td.getMethods();
        for (MethodDeclaration method : methods) {
          if (staticOnly && !isStatic(method.modifiers())) {
            continue;
          }
          if (noCompare) {
            candidates.add(new CompletionCandidate(method));
          } else if (method.getName().toString().toLowerCase()
              .startsWith(child))
            candidates.add(new CompletionCandidate(method));
        }
      }

      ArrayList<CompletionCandidate> superClassCandidates;
      if(td.getSuperclassType() != null){
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

      StringBuilder label = new StringBuilder(method.getName() + "(");
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
    if (probableClass.isArray() && !staticOnly) {
      // add array members manually, they can't be fetched through code

      String className = probableClass.getSimpleName();

      if (noCompare || "clone()".startsWith(child)) {
        String methodLabel = "<html>clone() : " + className +
            " - <font color=#777777>" + className + "</font></html>";
        candidates.add(new CompletionCandidate("clone()", methodLabel, "clone()",
                                               CompletionCandidate.PREDEF_METHOD));
      }

      if ("length".startsWith(child)) {
        String fieldLabel = "<html>length : int - <font color=#777777>" +
            className + "</font></html>";
        candidates.add(new CompletionCandidate("length", fieldLabel, "length",
                                               CompletionCandidate.PREDEF_FIELD));
      }
    }
    return candidates;
  }

  private static boolean isStatic(List<org.eclipse.jdt.core.dom.Modifier> modifiers) {
    for (org.eclipse.jdt.core.dom.Modifier m : modifiers) {
      if (m.isStatic()) return true;
    }
    return false;
  }


  /**
   * Searches for the particular class in the default list of imports as well as
   * the Sketch classpath
   * @param className
   * @return
   */
  protected Class<?> findClassIfExists(String className){
    if (className == null){
      return null;
    }

    PreprocessedSketch ps = errorCheckerService.latestResult;

    if (className.indexOf('.') >= 0) {
      // Figure out what is package and what is class
      String[] parts = className.split("\\.");
      String newClassName = parts[0];
      int i = 1;
      while (i < parts.length &&
          ps.classPath.isPackage(newClassName)) {
        newClassName = newClassName + "/" + parts[i++];
      }
      while (i < parts.length) {
        newClassName = newClassName + "$" + parts[i++];
      }
      className = newClassName.replace('/', '.');
    }

    // First, see if the classname is a fully qualified name and loads straightaway
    Class<?> tehClass = loadClass(className);

    if (tehClass != null) {
      //log(tehClass.getName() + " located straightaway");
      return tehClass;
    }

    // This name is qualified and it already had its chance
    if (className.indexOf('.') >= 0) {
      return null;
    }

    log("Looking in the classloader for " + className);
    // Using ClassPath and RegExResourceFilter to find a matching class
    // and then loading the thing might be simpler and faster

    // These can be preprocessed during error check for performance
    // (collect, split into starred and not starred)
    List<ImportStatement> programImports = ps.programImports;
    List<ImportStatement> codeFolderImports = ps.codeFolderImports;
    List<ImportStatement> coreAndDefaultImports = ps.coreAndDefaultImports;

    ImportStatement javaLang = ImportStatement.wholePackage("java.lang");

    Stream<List<ImportStatement>> importListStream =
        Stream.of(Collections.singletonList(javaLang), coreAndDefaultImports,
                  programImports, codeFolderImports);

    final String finalClassName = className;

    // These streams can be made unordered parallel if it helps performance
    return importListStream
        .map(list -> list.stream()
            .map(is -> {
              if (is.getClassName().equals(finalClassName)) {
                return is.getFullClassName();
              } else if (is.isStarredImport()) {
                return is.getPackageName() + "." + finalClassName;
              }
              return null;
            })
            .filter(name -> name != null)
            .map(this::loadClass)
            .filter(cls -> cls != null)
            .findAny())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findAny()
        .orElse(null);
  }

  protected Class<?> loadClass(String className){
    Class<?> tehClass = null;
    if (className != null) {
      try {
        tehClass = Class.forName(className, false,
                                 errorCheckerService.latestResult.classLoader);
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
    if (tehClass.getDeclaringNode() instanceof TypeDeclaration) {

      TypeDeclaration td = (TypeDeclaration) tehClass.getDeclaringNode();
      for (int i = 0; i < td.getFields().length; i++) {
        List<VariableDeclarationFragment> vdfs =
          td.getFields()[i].fragments();
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
      if (td.getSuperclassType() != null) {
        log(getNodeAsString(td.getSuperclassType()) + " <-Looking into superclass of " + tehClass);
        return definedIn3rdPartyClass(new ClassMember(td
                                                     .getSuperclassType()),memberName);
      } else {
        return definedIn3rdPartyClass(new ClassMember(Object.class),memberName);
      }
    }

    Class<?> probableClass;
    if (tehClass.getClass_() != null) {
      probableClass = tehClass.getClass_();
    } else {
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


  protected static ASTNode findClosestParentNode(int lineNumber, ASTNode node) {
    // Base.loge("Props of " + node.getClass().getName());
    for (StructuralPropertyDescriptor prop : (Iterable<StructuralPropertyDescriptor>) node
        .structuralPropertiesForType()) {
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
      } else if (prop.isChildListProperty()) {
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
    List<ASTNode> nodes;
    if (parent instanceof TypeDeclaration) {
      nodes = ((TypeDeclaration) parent).bodyDeclarations();
    } else if (parent instanceof Block) {
      nodes = ((Block) parent).statements();
    } else {
      log("findClosestNode() found " + getNodeAsString(parent));
      return null;
    }

    if (nodes.size() > 0) {
      ASTNode retNode = parent;
      for (ASTNode cNode : nodes) {
        log(cNode + "|cNode " + getLineNumber(cNode) + ", lfor " + lineNumber);
        if (getLineNumber(cNode) <= lineNumber)
          retNode = cNode;
      }

      return retNode;
    }
    return parent;
  }


  /**
   * Given a word(identifier) in pde code, finds its location in the ASTNode
   * @param lineNumber
   * @param name
   * @param offset - line start nonwhitespace offset
   * @param scrollOnly
   * @return
   */
  public SimpleName getSimpleNameAt(int javaOffset) {
    return getSimpleNameAt(javaOffset, javaOffset);
  }

  public SimpleName getSimpleNameAt(int startJavaOffset, int stopJavaOffset) {
    Messages.log("* getSimpleNameAt");

    // Find node at offset
    ASTNode node = getASTNodeAt(startJavaOffset, stopJavaOffset);

    SimpleName result = null;

    if (node == null) {
      result = null;
    } else if (node.getNodeType() == ASTNode.SIMPLE_NAME) {
      result = (SimpleName) node;
    } else {
      // Return SimpleName with highest coverage
      List<SimpleName> simpleNames = getSimpleNameChildren(node);
      if (!simpleNames.isEmpty()) {
        // Compute coverage <selection x node>
        int[] coverages = simpleNames.stream()
            .mapToInt(name -> {
              int start = name.getStartPosition();
              int stop = start + name.getLength();
              return Math.min(stop, stopJavaOffset) -
                  Math.max(startJavaOffset, start);
            })
            .toArray();
        // Select node with highest coverage
        int maxIndex = IntStream.range(0, simpleNames.size())
            .filter(i -> coverages[i] >= 0)
            .reduce((i, j) -> coverages[i] > coverages[j] ? i : j)
            .orElse(-1);
        if (maxIndex == -1) return null;
        result = simpleNames.get(maxIndex);
      }
    }

    if (node == null) {
      Messages.log("no simple name found");
    } else {
      Messages.log("found " + node.toString());
    }
    return result;
  }


  public ASTNode getASTNodeAt(int startJavaOffset, int stopJavaOffset) {
    Messages.log("* getASTNodeAt");

    ASTNode root = errorCheckerService.latestResult.compilationUnit;

    int length = stopJavaOffset - startJavaOffset;

    NodeFinder f = new NodeFinder(root, startJavaOffset, length);
    ASTNode node = f.getCoveredNode();
    if (node == null) {
      node = f.getCoveringNode();
    }
    if (node == null) {
      Messages.log("no node found");
    } else {
      Messages.log("found " + node.toString());
    }
    return node;
  }


  public static List<SimpleName> getSimpleNameChildren(ASTNode node) {
    List<SimpleName> simpleNames = new ArrayList<>();
    node.accept(new ASTVisitor() {
      @Override
      public boolean visit(SimpleName simpleName) {
        simpleNames.add(simpleName);
        return super.visit(simpleName);
      }
    });
    return simpleNames;
  }


  public static IBinding resolveBinding(SimpleName node) {
    IBinding binding = node.resolveBinding();
    if (binding == null) return null;

    // Fix constructor call/declaration being resolved as type
    if (binding.getKind() == IBinding.TYPE) {
      ASTNode context = node;
      while (isNameOrType(context) &&
          !context.getLocationInParent().getId().equals("typeArguments")) {
        context = context.getParent();
      }
      switch (context.getNodeType()) {
        case ASTNode.METHOD_DECLARATION:
          MethodDeclaration decl = (MethodDeclaration) context;
          if (decl.isConstructor()) {
            binding = decl.resolveBinding();
          }
          break;
        case ASTNode.CLASS_INSTANCE_CREATION:
          ClassInstanceCreation cic = (ClassInstanceCreation) context;
          binding = cic.resolveConstructorBinding();
          break;
      }
    }

    // Normalize parametrized and raw bindings into generic bindings
    switch (binding.getKind()) {
      case IBinding.TYPE:
        ITypeBinding type = (ITypeBinding) binding;
        if (type.isParameterizedType() || type.isRawType()) {
          binding = type.getErasure();
        }
        break;
      case IBinding.METHOD:
        IMethodBinding method = (IMethodBinding) binding;
        ITypeBinding declaringClass = method.getDeclaringClass();
        if (declaringClass.isParameterizedType() ||
            declaringClass.isRawType()) {
          IMethodBinding[] methods = declaringClass.getErasure().getDeclaredMethods();
          IMethodBinding generic = Arrays.stream(methods)
              .filter(method::overrides)
              .findAny().orElse(null);
          if (generic != null) method = generic;
        }
        if (method.isParameterizedMethod() || method.isRawMethod()) {
          method = method.getMethodDeclaration();
        }
        binding = method;
        break;
    }

    return binding;
  }


  public static boolean isNameOrType(ASTNode node) {
    return node instanceof Name || node instanceof Type;
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


  protected void handleRename(IBinding binding, String newName) {

    // TODO: get this off EDT

    boolean isNewNameValid =
        Character.isUnicodeIdentifierStart(newName.charAt(0)) &&
        newName.substring(1).chars().allMatch(Character::isUnicodeIdentifierPart);
    if (!isNewNameValid) {
      JOptionPane.showMessageDialog(new JFrame(), newName
          + " isn't a valid name.", "Uh oh..", JOptionPane.PLAIN_MESSAGE);
      return;
    }

    PreprocessedSketch ps = errorCheckerService.latestResult;

    ASTNode decl = ps.compilationUnit.findDeclaringNode(binding.getKey());
    if (decl == null) return;

    List<SimpleName> occurrences = findAllOccurrences(ps.compilationUnit, binding.getKey());

    List<ShowUsageTreeNode> mappedNodes = occurrences.stream()
        .sorted(Comparator.comparing(ASTNode::getStartPosition).reversed())
        .map(node -> ShowUsageTreeNode.fromSimpleName(ps, node))
        .collect(Collectors.toList());

    editor.startCompoundEdit();
    for (ShowUsageTreeNode node : mappedNodes) {
      int tabIndex = node.tabIndex;
      int startTabOffset = node.startTabOffset;
      int stopTabOffset = node.stopTabOffset;
      errorCheckerService.highlightTabRange(tabIndex, startTabOffset, stopTabOffset);
      editor.getTextArea().setSelectedText(newName);
    }
    editor.stopCompoundEdit();

    editor.getSketch().setModified(true);

    errorCheckerService.request();
  }


  public void handleShowUsage(int tabIndex, int startTabOffset, int stopTabOffset) {
    Messages.log("* handleShowUsage");

    if (editor.hasJavaTabs()) return; // show usage disabled if java tabs

    PreprocessedSketch ps = errorCheckerService.latestResult;
    if (ps.hasSyntaxErrors) return;

    // Map offsets
    int startJavaOffset = ps.tabOffsetToJavaOffset(tabIndex, startTabOffset);
    int stopJavaOffset = ps.tabOffsetToJavaOffset(tabIndex, stopTabOffset);

    // Find the node
    SimpleName name = getSimpleNameAt(startJavaOffset, stopJavaOffset);
    if (name == null) return;

    // Find binding
    IBinding binding = resolveBinding(name);
    if (binding == null) return;

    handleShowUsage(binding);
  }


  public void handleShowUsage(IBinding binding) {
    PreprocessedSketch ps = errorCheckerService.latestResult;
    if (ps.hasSyntaxErrors) return;

    String bindingKey = binding.getKey();

    // Find occurrences of the node
    List<SimpleName> occurrences = findAllOccurrences(ps.compilationUnit, bindingKey);
    if (occurrences == null) return;

    // Send to gui
    EventQueue.invokeLater(() -> gui.handleShowUsage(binding, occurrences));
  }


  public void reloadShowUsage() {
    if (gui.showUsageBinding != null) {
      handleShowUsage(gui.showUsageBinding);
    }
  }


  protected List<SimpleName> findAllOccurrences(ASTNode root, String bindingKey) {
    List<SimpleName> occurences = new ArrayList<>();
    root.getRoot().accept(new ASTVisitor() {
      @Override
      public boolean visit(SimpleName name) {
        IBinding binding = resolveBinding(name);
        if (binding != null && bindingKey.equals(binding.getKey())) {
          occurences.add(name);
        }
        return super.visit(name);
      }
    });

    return occurences;
  }


  /*
  protected SketchOutline sketchOutline;

  public void showSketchOutline() {
    if (editor.hasJavaTabs()) return;

    sketchOutline = new SketchOutline(editor, codeTree);
    sketchOutline.show();
  }


  public void showTabOutline() {
    new TabOutline(editor).show();
  }
  */


  public void handleRename(int tabIndex, int startTabOffset, int stopTabOffset) {
    Messages.log("* handleRename");
    if (editor.hasJavaTabs()) return;  // refactoring disabled w/ java tabs

    PreprocessedSketch ps = errorCheckerService.latestResult;
    if (ps.hasSyntaxErrors) {
      editor.statusMessage("Can't perform action until syntax errors are fixed :(",
                           EditorStatus.WARNING);
      return;
    }

    // Map offsets
    int startJavaOffset = ps.tabOffsetToJavaOffset(tabIndex, startTabOffset);
    int stopJavaOffset = ps.tabOffsetToJavaOffset(tabIndex, stopTabOffset);

    // Find the node
    SimpleName name = getSimpleNameAt(startJavaOffset, stopJavaOffset);
    if (name == null) {
      editor.statusMessage("Highlight the class/function/variable name first",
                           EditorStatus.NOTICE);
      return;
    }

    // Find binding
    IBinding binding = resolveBinding(name);
    if (binding == null) {
      editor.statusMessage(name.getIdentifier() + " isn't defined in this sketch, " +
                           "so it cannot be renamed", EditorStatus.ERROR);
      return;
    }

    EventQueue.invokeLater(() -> gui.handleRename(binding));
  }


  /**
   * Give this thing a {@link Name} instance - a {@link SimpleName} from the
   * ASTNode for ex, and it tries its level best to locate its declaration in
   * the AST. It really does.
   *
   * @param findMe
   * @return
   */
  protected static ASTNode findDeclaration(Name findMe) {

    // WARNING: You're entering the Rube Goldberg territory of Experimental Mode.
    // To debug this code, thou must take the Recursive Leap of Faith.

    // log("entering --findDeclaration1 -- " + findMe.toString());
    ASTNode declaringClass;
    ASTNode parent = findMe.getParent();
    ASTNode ret;
    ArrayList<Integer> constrains = new ArrayList<>();
    if (parent.getNodeType() == ASTNode.METHOD_INVOCATION) {
      Expression exp = (Expression) parent.getStructuralProperty(MethodInvocation.EXPRESSION_PROPERTY);
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
                .getName().toString(), constrains);
          } else if (exp instanceof FieldAccess) {
            SimpleType stp = extracTypeInfo(findDeclaration(((FieldAccess) exp)
                .getName()));
            if (stp == null)
              return null;
            declaringClass = findDeclaration((stp.getName()));
            return definedIn(declaringClass, ((MethodInvocation) parent)
                .getName().toString(), constrains);
          }
          if (exp instanceof SimpleName) {
            SimpleType stp = extracTypeInfo(findDeclaration(((SimpleName) exp)));
            if (stp == null)
              return null;
            declaringClass = findDeclaration(stp.getName());
//            log("MI.SN " + getNodeAsString(declaringClass));
            constrains.add(ASTNode.METHOD_DECLARATION);
            return definedIn(declaringClass, ((MethodInvocation) parent)
                .getName().toString(), constrains);
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
                             constrains);
          } else if (exp instanceof FieldAccess) {
            SimpleType stp = extracTypeInfo(findDeclaration(((FieldAccess) exp)
                .getName()));
            if (stp == null)
              return null;
            declaringClass = findDeclaration((stp.getName()));
            constrains.add(ASTNode.TYPE_DECLARATION);
            return definedIn(declaringClass, fa.getName().toString(),
                             constrains);
          }
          if (exp instanceof SimpleName) {
            SimpleType stp = extracTypeInfo(findDeclaration(((SimpleName) exp)));
            if (stp == null)
              return null;
            declaringClass = findDeclaration(stp.getName());
//            log("FA.SN " + getNodeAsString(declaringClass));
            constrains.add(ASTNode.METHOD_DECLARATION);
            return definedIn(declaringClass, fa.getName().toString(),
                             constrains);
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
        if (stp == null) {
          return null;
        }

        declaringClass = findDeclaration(stp.getName());

//        log("QN decl class: " + getNodeAsString(declaringClass));
        constrains.clear();
        constrains.add(ASTNode.TYPE_DECLARATION);
        constrains.add(ASTNode.FIELD_DECLARATION);
        return definedIn(declaringClass, qn.getName().toString(), constrains);
      }
      else{
        if(findMe instanceof QualifiedName){
          QualifiedName qnn = (QualifiedName) findMe;
//          log("findMe is a QN, "
//              + (qnn.getQualifier().toString() + " other " + qnn.getName()
//                  .toString()));

          SimpleType stp = extracTypeInfo(findDeclaration((qnn.getQualifier())));
          if (stp == null) {
            return null;
          }
          declaringClass = findDeclaration(stp.getName());
          constrains.clear();
          constrains.add(ASTNode.TYPE_DECLARATION);
          constrains.add(ASTNode.FIELD_DECLARATION);
          return definedIn(declaringClass, qnn.getName().toString(),
                           constrains);
        }
      }
    } else if (parent.getNodeType() == ASTNode.SIMPLE_TYPE) {
      constrains.add(ASTNode.TYPE_DECLARATION);
      if (parent.getParent().getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
        constrains.add(ASTNode.CLASS_INSTANCE_CREATION);
      }
    } else if (parent.getNodeType() == ASTNode.TYPE_DECLARATION) {
      // The condition where we look up the name of a class decl
      TypeDeclaration td = (TypeDeclaration) parent;
      if (findMe.equals(td.getName())) {
        return parent;
      }

    } else if (parent instanceof Expression) {
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
                            findMe.toString(), constrains);
            if (ret != null)
              return ret;
          }
        } else if (prop.isChildListProperty()) {
//          log((prop) + " ChildList props of "
//              + getNodeAsString(parent));
          List<ASTNode> nodelist = (List<ASTNode>) parent
              .getStructuralProperty(prop);
          for (ASTNode retNode : nodelist) {
            ret = definedIn(retNode, findMe.toString(), constrains);
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
    ASTNode declaringClass;
    ASTNode parent = findMe.getParent();
    ASTNode ret;
    ArrayList<Integer> constrains = new ArrayList<>();
    if (parent.getNodeType() == ASTNode.METHOD_INVOCATION) {
      Expression exp = (Expression) parent.getStructuralProperty(MethodInvocation.EXPRESSION_PROPERTY);
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
                .getName().toString(), constrains);
          } else if (exp instanceof FieldAccess) {
            SimpleType stp = extracTypeInfo(findDeclaration2(((FieldAccess) exp)
                                                                 .getName(),
                                                             alternateParent));
            if (stp == null)
              return null;
            declaringClass = findDeclaration2((stp.getName()), alternateParent);
            return definedIn(declaringClass, ((MethodInvocation) parent)
                .getName().toString(), constrains);
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
                .getName().toString(), constrains);
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
                             constrains);
          } else if (exp instanceof FieldAccess) {
            SimpleType stp = extracTypeInfo(findDeclaration2(((FieldAccess) exp)
                                                                 .getName(),
                                                             alternateParent));
            if (stp == null)
              return null;
            declaringClass = findDeclaration2((stp.getName()), alternateParent);
            constrains.add(ASTNode.TYPE_DECLARATION);
            return definedIn(declaringClass, fa.getName().toString(),
                             constrains);
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
                             constrains);
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
        return definedIn(declaringClass, qn.getName().toString(), constrains);
      }
      else{
        if(findMe instanceof QualifiedName){
          QualifiedName qnn = (QualifiedName) findMe;
//          log("findMe is a QN, "
//              + (qnn.getQualifier().toString() + " other " + qnn.getName()
//                  .toString()));

          SimpleType stp = extracTypeInfo(findDeclaration2((qnn.getQualifier()), alternateParent));

          if (stp == null) {
            return null;
          }

//          log(qnn.getQualifier() + "->" + qnn.getName());
          declaringClass = findDeclaration2(stp.getName(), alternateParent);

//          log("QN decl class: "
//              + getNodeAsString(declaringClass));
          constrains.clear();
          constrains.add(ASTNode.TYPE_DECLARATION);
          constrains.add(ASTNode.FIELD_DECLARATION);
          return definedIn(declaringClass, qnn.getName().toString(), constrains);
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
                            findMe.toString(), constrains);
            if (ret != null)
              return ret;
          }
        } else if (prop.isChildListProperty()) {
//          log((prop) + " ChildList props of "
//              + getNodeAsString(alternateParent));
          List<ASTNode> nodelist = (List<ASTNode>) alternateParent
              .getStructuralProperty(prop);
          for (ASTNode retNode : nodelist) {
            ret = definedIn(retNode, findMe.toString(), constrains);
            if (ret != null)
              return ret;
          }
        }
      }
      alternateParent = alternateParent.getParent();
    }
    return null;
  }

  public GUI getGui() {
    return gui;
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

    private Constructor<?> cons;

    private Class<?> thisclass;

    private String stringVal;

    private String classType;

    private ASTNode astNode;

    private ASTNode declaringNode;

    public ClassMember(Class<?> m) {
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

    public ClassMember(Constructor<?> m) {
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
          thisclass = findClassIfExists(classType);
        }
        else{
          // a local type
          declaringNode = decl;
        }
      }
    }

    public Class<?> getClass_() {
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

    public Constructor<?> getCons() {
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
      log("ele type "
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


  static public Type extracTypeInfo2(ASTNode node) {
    Messages.log("* extracTypeInfo2");
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


  static protected ASTNode definedIn(ASTNode node, String name,
                                   ArrayList<Integer> constrains) {
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
      //Base.loge(getNodeAsString(node));
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
      //Base.loge(getNodeAsString(node));
      if (((MethodDeclaration) node).getName().toString().equalsIgnoreCase(name))
        return node;
      break;
    case ASTNode.SINGLE_VARIABLE_DECLARATION:
      //Base.loge(getNodeAsString(node));
      if (((SingleVariableDeclaration) node).getName().toString().equalsIgnoreCase(name))
        return node;
      break;
    case ASTNode.FIELD_DECLARATION:
      //Base.loge("FD" + node);
      vdfList = ((FieldDeclaration) node).fragments();
      break;
    case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
      //Base.loge("VDE" + node);
      vdfList = ((VariableDeclarationExpression) node).fragments();
      break;
    case ASTNode.VARIABLE_DECLARATION_STATEMENT:
      //Base.loge("VDS" + node);
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
      value = node.toString() + " FldDecl | ";
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
      value = node.toString() + " | " + className;
    value += " [" + node.getStartPosition() + ","
        + (node.getStartPosition() + node.getLength()) + "]";
    value += " Line: "
        + ((CompilationUnit) node.getRoot()).getLineNumber(node
            .getStartPosition());
    return value;
  }


//  public void jdocWindowVisible(boolean visible) {
//   // frmJavaDoc.setVisible(visible);
//  }

//  public static String readFile2(String path) {
//    BufferedReader reader = null;
//    try {
//      reader = new BufferedReader(
//                                  new InputStreamReader(
//                                                        new FileInputStream(
//                                                                            new File(
//                                                                                     path))));
//    } catch (FileNotFoundException e) {
//      e.printStackTrace();
//    }
//    try {
//      StringBuilder ret = new StringBuilder();
//      // ret.append("package " + className + ";\n");
//      String line;
//      while ((line = reader.readLine()) != null) {
//        ret.append(line);
//        ret.append("\n");
//      }
//      return ret.toString();
//    } catch (IOException e) {
//      e.printStackTrace();
//    } finally {
//      try {
//        reader.close();
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
//    }
//    return null;
//  }


  static private void log(Object object) {
    Messages.log(object == null ? "null" : object.toString());
  }


  /// Predictions --------------------------------------------------------------


  protected static List<CompletionCandidate> trimCandidates(String newWord, List<CompletionCandidate> candidates) {
    ArrayList<CompletionCandidate> newCandidate = new ArrayList<>();
    newWord = newWord.toLowerCase();
    for (CompletionCandidate comp : candidates) {
      if(comp.getNoHtmlLabel().toLowerCase().startsWith(newWord)){
        newCandidate.add(comp);
      }
    }
    return newCandidate;
  }

  protected List<CompletionCandidate> candidates;
  protected String lastPredictedPhrase = " ";

  /**
   * The main function that calculates possible code completion candidates
   *
   * @param pdePhrase
   * @param line
   * @param lineStartNonWSOffset
   */
  public List<CompletionCandidate> preparePredictions(final String pdePhrase,
                                                      final int line) {
    Messages.log("* preparePredictions");
    ASTNode astRootNode = (ASTNode) errorCheckerService.getLatestCU().types().get(0);

    // If the parsed code contains pde enhancements, take 'em out.
    // TODO: test this
    SourceMapping mapping = new SourceMapping();
    mapping.addAll(SourceUtils.replaceTypeConstructors(pdePhrase));
    mapping.addAll(SourceUtils.replaceHexLiterals(pdePhrase));
    mapping.addAll(SourceUtils.replaceColorRegex(pdePhrase));
    mapping.addAll(SourceUtils.fixFloatsRegex(pdePhrase));
    String phrase = mapping.apply(pdePhrase);

    //After typing 'arg.' all members of arg type are to be listed. This one is a flag for it
    boolean noCompare = phrase.endsWith(".");

    if (noCompare) {
      phrase = phrase.substring(0, phrase.length() - 1);
    }

    boolean incremental = !noCompare &&
        phrase.length() > lastPredictedPhrase.length() &&
        phrase.startsWith(lastPredictedPhrase);


    if (incremental) {
      log(pdePhrase + " starts with " + lastPredictedPhrase);
      log("Don't recalc");

      if (phrase.contains(".")) {
        int x = phrase.lastIndexOf('.');
        candidates = trimCandidates(phrase.substring(x + 1), candidates);
      } else {
        candidates = trimCandidates(phrase, candidates);
      }
      lastPredictedPhrase = phrase;
      return candidates;
    }

    PreprocessedSketch ps = errorCheckerService.latestResult;

    // Adjust line number for tabbed sketches
    int codeIndex = editor.getSketch().getCodeIndex(editor.getCurrentTab());
    int lineNumber = ps.tabLineToJavaLine(codeIndex, line);

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
    ASTNode nearestNode;
    ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setKind(ASTParser.K_EXPRESSION);
    parser.setSource(phrase.toCharArray());
    ASTNode testnode = parser.createAST(null);
    //Base.loge("PREDICTION PARSER PROBLEMS: " + parser);
    // Find closest ASTNode of the document to this word
    Messages.loge("Typed: " + phrase + "|" + " temp Node type: " + testnode.getClass().getSimpleName());
    if(testnode instanceof MethodInvocation){
      MethodInvocation mi = (MethodInvocation)testnode;
      log(mi.getName() + "," + mi.getExpression() + "," + mi.typeArguments().size());
    }

    // find nearest ASTNode
    nearestNode = findClosestNode(lineNumber, astRootNode);
    if (nearestNode == null) {
      // Make sure nearestNode is not NULL if couldn't find a closest node
      nearestNode = astRootNode;
    }
    Messages.loge(lineNumber + " Nearest ASTNode to PRED "
                      + getNodeAsString(nearestNode));

    candidates = new ArrayList<>();
    lastPredictedPhrase = phrase;
    // Determine the expression typed

    if (testnode instanceof SimpleName && !noCompare) {
      Messages.loge("One word expression " + getNodeAsString(testnode));
      //==> Simple one word exprssion - so is just an identifier

      // Bottom up traversal of the AST to look for possible definitions at
      // higher levels.
      //nearestNode = nearestNode.getParent();
      while (nearestNode != null) {
        // If the current class has a super class, look inside it for
        // definitions.
        if (nearestNode instanceof TypeDeclaration) {
          TypeDeclaration td = (TypeDeclaration) nearestNode;
          if (td.getStructuralProperty(TypeDeclaration.SUPERCLASS_TYPE_PROPERTY) != null) {
            SimpleType st = (SimpleType) td.getStructuralProperty(TypeDeclaration.SUPERCLASS_TYPE_PROPERTY);
            log("Superclass " + st.getName());
            ArrayList<CompletionCandidate> tempCandidates =
                getMembersForType(st.getName().toString(), phrase, false, false);
            for (CompletionCandidate can : tempCandidates) {
              candidates.add(can);
            }
            //findDeclaration(st.getName())
          }
        }
        List<StructuralPropertyDescriptor> sprops =
            nearestNode.structuralPropertiesForType();
        for (StructuralPropertyDescriptor sprop : sprops) {
          ASTNode cnode;
          if (!sprop.isChildListProperty()) {
            if (nearestNode.getStructuralProperty(sprop) instanceof ASTNode) {
              cnode = (ASTNode) nearestNode.getStructuralProperty(sprop);
              CompletionCandidate[] types = checkForTypes(cnode);
              if (types != null) {
                for (CompletionCandidate type : types) {
                  if (type.getElementName().toLowerCase().startsWith(phrase.toLowerCase()))
                    candidates.add(type);
                }
              }
            }
          } else {
            // Childlist prop
            List<ASTNode> nodelist =
                (List<ASTNode>) nearestNode.getStructuralProperty(sprop);
            for (ASTNode clnode : nodelist) {
              CompletionCandidate[] types = checkForTypes(clnode);
              if (types != null) {
                for (CompletionCandidate type : types) {
                  if (type.getElementName().toLowerCase().startsWith(phrase.toLowerCase()))
                    candidates.add(type);
                }
              }
            }
          }
        }
        nearestNode = nearestNode.getParent();
      }
      // We're seeing a simple name that's not defined locally or in
      // the parent class. So most probably a pre-defined type.
      log("Empty can. " + phrase);
      ClassPath classPath = errorCheckerService.latestResult.classPath;
      if (classPath != null) {
        RegExpResourceFilter regExpResourceFilter =
            new RegExpResourceFilter(Pattern.compile(".*"),
                                     Pattern.compile(phrase + "[a-zA-Z_0-9]*.class",
                                                     Pattern.CASE_INSENSITIVE));
        String[] resources = classPath.findResources("", regExpResourceFilter);

        for (String matchedClass2 : resources) {
          matchedClass2 = matchedClass2.replace('/', '.'); //package name
          String matchedClass = matchedClass2.substring(0, matchedClass2.length() - 6);
          int d = matchedClass.lastIndexOf('.');
          if (!errorCheckerService.ignorableSuggestionImport(matchedClass)) {
            matchedClass = matchedClass.substring(d + 1); //class name
            // display package name in grey
            String html = "<html>" + matchedClass + " : <font color=#777777>" +
                matchedClass2.substring(0, d) + "</font></html>";
            candidates.add(new CompletionCandidate(matchedClass, html,
                                                   matchedClass,
                                                   CompletionCandidate.PREDEF_CLASS));
          }
        }
      }
    } else {
      // ==> Complex expression of type blah.blah2().doIt,etc
      // Have to resolve it by carefully traversing AST of testNode
      Messages.loge("Complex expression " + getNodeAsString(testnode));
      log("candidates empty");
      ASTNode childExpr = getChildExpression(testnode);
      log("Parent expression : " + getParentExpression(testnode));
      log("Child expression : " + childExpr);
      if (!noCompare) {
        log("Original testnode " + getNodeAsString(testnode));
        testnode = getParentExpression(testnode);
        log("Corrected testnode " + getNodeAsString(testnode));
      }
      ClassMember expr =
          resolveExpression3rdParty(nearestNode, testnode, noCompare);
      if (expr == null) {
        log("Expr is null");
      } else {
        boolean isArray = expr.thisclass != null && expr.thisclass.isArray();
        boolean isSimpleType = (expr.astNode != null) &&
            expr.astNode.getNodeType() == ASTNode.SIMPLE_TYPE;
        boolean isMethod = expr.method != null;
        boolean staticOnly = !isMethod && !isArray && !isSimpleType;
        log("Expr is " + expr.toString());
        String lookFor = (noCompare || (childExpr == null)) ?
            "" : childExpr.toString();
        candidates = getMembersForType(expr, lookFor, noCompare, staticOnly);
      }
    }
    return candidates;
  }


  protected static DefaultListModel<CompletionCandidate> filterPredictions(List<CompletionCandidate> candidates) {
    Messages.log("* filterPredictions");
    DefaultListModel<CompletionCandidate> defListModel = new DefaultListModel<>();
    if (candidates.isEmpty())
      return defListModel;
    // check if first & last CompCandidate are the same methods, only then show all overloaded methods
    if (candidates.get(0).getElementName()
        .equals(candidates.get(candidates.size() - 1).getElementName())) {
      log("All CC are methods only: " + candidates.get(0).getElementName());
      for (int i = 0; i < candidates.size(); i++) {
        CompletionCandidate cc = candidates.get(i).withRegeneratedCompString();
        candidates.set(i, cc);
        defListModel.addElement(cc);
      }
    }
    else {
      boolean ignoredSome = false;
      for (int i = 0; i < candidates.size(); i++) {
        if(i > 0 && (candidates.get(i).getElementName()
            .equals(candidates.get(i - 1).getElementName()))){
          if (candidates.get(i).getType() == CompletionCandidate.LOCAL_METHOD
              || candidates.get(i).getType() == CompletionCandidate.PREDEF_METHOD) {
            CompletionCandidate cc = candidates.get(i - 1);
            String label = cc.getLabel();
            int x = label.lastIndexOf(')');
            String newLabel;
            if (candidates.get(i).getType() == CompletionCandidate.PREDEF_METHOD) {
              newLabel = (cc.getLabel().contains("<html>") ? "<html>" : "")
                  + cc.getElementName() + "(...)" + label.substring(x + 1);
            } else {
              newLabel = cc.getElementName() + "(...)" + label.substring(x + 1);
            }
            String newCompString = cc.getElementName() + "(";
            candidates.set(i - 1, cc.withLabelAndCompString(newLabel, newCompString));
            ignoredSome = true;
            continue;
          }
        }
        defListModel.addElement(candidates.get(i));
      }
      if (ignoredSome) {
        log("Some suggestions hidden");
      }
    }
    return defListModel;
  }



  /// JavaDocs -----------------------------------------------------------------


  protected TreeMap<String, String> jdocMap;


  protected void loadJavaDoc() {
    jdocMap = new TreeMap<>();

    // presently loading only p5 reference for PApplet
    // TODO: use something like ExecutorService here [jv]
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          loadJavaDoc(jdocMap, editor.getMode().getReferenceFolder());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }).start();
  }


  static void loadJavaDoc(TreeMap<String, String> jdocMap,
                          File referenceFolder) throws IOException {
    Document doc;

    FileFilter fileFilter = new FileFilter() {
      @Override
      public boolean accept(File file) {
        if(!file.getName().endsWith("_.html"))
          return false;
        int k = 0;
        for (int i = 0; i < file.getName().length(); i++) {
          if(file.getName().charAt(i)== '_')
            k++;
          if(k > 1)
            return false;
        }
        return true;
      }
    };

    for (File docFile : referenceFolder.listFiles(fileFilter)) {
      doc = Jsoup.parse(docFile, null);
      Elements elm = doc.getElementsByClass("ref-item");
      String msg = "";
      String methodName = docFile.getName().substring(0, docFile.getName().indexOf('_'));
      //System.out.println(methodName);
      for (org.jsoup.nodes.Element ele : elm) {
        msg = "<html><body> <strong><div style=\"width: 300px; text-justification: justify;\"></strong><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"ref-item\">"
            + ele.html() + "</table></div></html></body></html>";
        //mat.replaceAll("");
        msg = msg.replaceAll("img src=\"", "img src=\""
            + referenceFolder.toURI().toURL().toString() + "/");
        //System.out.println(ele.text());
      }
      jdocMap.put(methodName, msg);
    }
    //System.out.println("JDoc loaded " + jdocMap.size());
  }


/*
  public void updateJavaDoc(final CompletionCandidate candidate) {
    //TODO: Work on this later.
    return;
    String methodmatch = candidate.toString();
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
  }
*/



  /// Error checker ------------------------------------------------------------


  protected void updateDebugTree(CompilationUnit cu) {
    if (cu.types().isEmpty()){
      Messages.loge("No Type found in CU");
      return;
    }

    ASTNode type0 = (ASTNode) cu.types().get(0);
    DefaultMutableTreeNode codeTree = new DefaultMutableTreeNode(type0);
    visitRecur(type0, codeTree);
    EventQueue.invokeLater(() -> gui.updateDebugTree(codeTree));

    // TODO: figure out what this is
//    if (codeTree != null) {
//      if (!frameAutoComp.isVisible()) {
//
//        frameAutoComp.setVisible(true);
//
//      }
//      if (!frmJavaDoc.isVisible()) {
//        long t = System.currentTimeMillis();
//        loadJavaDoc();
//        log("Time taken: "
//            + (System.currentTimeMillis() - t));
//        frmJavaDoc.setBounds(new Rectangle(errorCheckerService.getEditor()
//            .getX() + errorCheckerService.getEditor().getWidth(),
//                                           errorCheckerService.getEditor()
//                                               .getY(), 450, 600));
//        frmJavaDoc.setVisible(true);
//      }
//    }
  }


  /**
   * Generates AST Swing component
   * @param node
   * @param tnode
   */
  public static void visitRecur(ASTNode node, DefaultMutableTreeNode tnode) {
    Iterator<StructuralPropertyDescriptor> it =
        node.structuralPropertiesForType().iterator();
    //Base.loge("Props of " + node.getClass().getName());
    DefaultMutableTreeNode ctnode;
    while (it.hasNext()) {
      StructuralPropertyDescriptor prop = it.next();

      if (prop.isChildProperty() || prop.isSimpleProperty()) {
        if (node.getStructuralProperty(prop) != null) {
//          System.out
//              .println(node.getStructuralProperty(prop) + " -> " + (prop));
          if (node.getStructuralProperty(prop) instanceof ASTNode) {
            ASTNode cnode = (ASTNode) node.getStructuralProperty(prop);
            if (isAddableASTNode(cnode)) {
              ctnode = new DefaultMutableTreeNode(node.getStructuralProperty(prop));
              tnode.add(ctnode);
              visitRecur(cnode, ctnode);
            }
          } else {
            tnode.add(new DefaultMutableTreeNode(node
                                                     .getStructuralProperty(prop)));
          }
        }
      } else if (prop.isChildListProperty()) {
        List<ASTNode> nodelist = (List<ASTNode>)
            node.getStructuralProperty(prop);
        for (ASTNode cnode : nodelist) {
          if (isAddableASTNode(cnode)) {
            ctnode = new DefaultMutableTreeNode(cnode);
            tnode.add(ctnode);
            visitRecur(cnode, ctnode);
          } else {
            visitRecur(cnode, tnode);
          }
        }
      }
    }
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



  /// Editor stuff -------------------------------------------------------------


  // Thread: EDT
  public void scrollToDeclaration(int tabIndex, int offset) {
    Messages.log("* scrollToDeclaration");

    // TODO: don't run the heavy lifting on EDT

    // TODO: handle comments

    PreprocessedSketch ps = errorCheckerService.latestResult;

    int javaOffset = ps.tabOffsetToJavaOffset(tabIndex, offset);

    SimpleName simpleName = getSimpleNameAt(javaOffset);

    if (simpleName == null) {
      Messages.log("nothing found");
      return;
    }

    IBinding binding = resolveBinding(simpleName);
    if (binding == null) {
      Messages.log("binding not resolved");
      return;
    }

    String key = binding.getKey();
    ASTNode decl = ps.compilationUnit.findDeclaringNode(key);
    if (decl == null) {
      Messages.log("decl not found");
      return;
    }

    SimpleName declName = null;
    switch (binding.getKind()) {
      case IBinding.TYPE: declName = ((TypeDeclaration) decl).getName(); break;
      case IBinding.METHOD: declName = ((MethodDeclaration) decl).getName(); break;
      case IBinding.VARIABLE: declName = ((VariableDeclaration) decl).getName(); break;
    }
    if (declName == null) {
      Messages.log("decl name not found " + decl);
      return;
    }

    Messages.log("found declaration, offset " + decl.getStartPosition() + ", name: " + declName);
    errorCheckerService.highlightJavaRange(declName.getStartPosition(), declName.getLength());
  }



  /// GUI ----------------------------------------------------------------------

  protected static class GUI {

    // Rename window
    protected JFrame renameWindow;
    protected JTextField renameTextField;
    protected JLabel renameOldNameLabel;
    protected JButton showUsageButton;
    protected JButton renameButton;
    protected IBinding renameBinding;

    // Show usage window
    protected JFrame showUsageWindow;
    protected JTree showUsageTree;
    protected IBinding showUsageBinding;

    protected final JavaEditor editor;
    protected final ASTGenerator astGen;


    protected GUI(JavaEditor editor, ASTGenerator astGen) {
      this.editor = editor;
      this.astGen = astGen;
      setupGUI();
      addListeners();
    }


    protected void setupGUI() {

      if (SHOW_DEBUG_TREE) initDebugWindow();

      { // Rename window
        renameWindow = new JFrame();
        renameWindow.setTitle("Enter new name:");
        renameWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        renameWindow.addComponentListener(new ComponentAdapter() {
          @Override
          public void componentHidden(ComponentEvent e) {
            renameBinding = null;
          }
        });
        renameWindow.setSize(250, 130);
        renameWindow.setLayout(new BoxLayout(renameWindow.getContentPane(), BoxLayout.Y_AXIS));
        Toolkit.setIcon(renameWindow);

        { // Top panel

          // Text field
          renameTextField = new JTextField();
          renameTextField.setPreferredSize(new Dimension(150, 60));

          // Old name label
          renameOldNameLabel = new JLabel();
          renameOldNameLabel.setText("Old Name: ");

          // Top panel
          JPanel panelTop = new JPanel();
          panelTop.setLayout(new BoxLayout(panelTop, BoxLayout.Y_AXIS));
          panelTop.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
          panelTop.add(renameTextField);
          panelTop.add(Box.createRigidArea(new Dimension(0, 10)));
          panelTop.add(renameOldNameLabel);
          renameWindow.add(panelTop);
        }

        { // Bottom panel
          showUsageButton = new JButton("Show Usage");
          renameButton = new JButton("Rename");

          JPanel panelBottom = new JPanel();
          panelBottom.setLayout(new BoxLayout(panelBottom, BoxLayout.X_AXIS));
          panelBottom.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
          panelBottom.add(Box.createHorizontalGlue());
          panelBottom.add(showUsageButton);
          panelBottom.add(Box.createRigidArea(new Dimension(15, 0)));
          panelBottom.add(renameButton);
          renameWindow.add(panelBottom);
        }

        //renameWindow.setVisible(true);
        renameWindow.setMinimumSize(renameWindow.getSize());
        renameWindow.setLocation(editor.getX()
                                     + (editor.getWidth() - renameWindow.getWidth()) / 2,
                                 editor.getY()
                                     + (editor.getHeight() - renameWindow.getHeight())
                                     / 2);
      }

      { // Show Usage window
        showUsageWindow = new JFrame();
        showUsageWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        showUsageWindow.addComponentListener(new ComponentAdapter() {
          @Override
          public void componentHidden(ComponentEvent e) {
            // Delete references to ASTNodes so that whole AST can be GC'd
            showUsageBinding = null;
            showUsageTree.setModel(null);
          }
        });
        showUsageWindow.setSize(300, 400);
        Toolkit.setIcon(showUsageWindow);
        JScrollPane sp2 = new JScrollPane();
        showUsageTree = new JTree();
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) showUsageTree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        sp2.setViewportView(showUsageTree);
        showUsageWindow.add(sp2);
      }
    }


    protected void addListeners() {

      if (SHOW_DEBUG_TREE) addDebugTreeListener();

      renameButton.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          if (renameTextField.getText().length() == 0) {
            return;
          }
          String newName = renameTextField.getText().trim();
          astGen.handleRename(renameBinding, newName);
          renameWindow.setVisible(false);
        }
      });

      showUsageButton.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          astGen.handleShowUsage(renameBinding);
        }
      });

      showUsageTree.addTreeSelectionListener(new TreeSelectionListener() {

        @Override
        public void valueChanged(TreeSelectionEvent e) {
          if (showUsageTree.getLastSelectedPathComponent() == null) {
            return;
          }
          DefaultMutableTreeNode tnode = (DefaultMutableTreeNode) showUsageTree
              .getLastSelectedPathComponent();

          if (tnode.getUserObject() instanceof ShowUsageTreeNode) {
            ShowUsageTreeNode node = (ShowUsageTreeNode) tnode.getUserObject();
            astGen.errorCheckerService.highlightTabRange(node.tabIndex,
                                                         node.startTabOffset,
                                                         node.stopTabOffset);
          }
        }
      });
    }


    public void handleRename(IBinding binding) {
      if (!renameWindow.isVisible()){
        renameBinding = binding;
        renameWindow.setLocation(editor.getX()
                                     + (editor.getWidth() - renameWindow.getWidth()) / 2,
                                 editor.getY()
                                     + (editor.getHeight() - renameWindow.getHeight())
                                     / 2);
        renameOldNameLabel.setText("Current name: " + binding.getName());
        renameTextField.setText(binding.getName());
        renameTextField.requestFocus();
        renameTextField.selectAll();
        renameWindow.setVisible(true);
        renameWindow.toFront();
      }
    }


    public void handleShowUsage(IBinding binding, List<SimpleName> occurrences) {
      showUsageBinding = binding;

      PreprocessedSketch ps = astGen.errorCheckerService.latestResult;

      String bindingType = "";
      switch (binding.getKind()) {
        case IBinding.METHOD:
          IMethodBinding method = (IMethodBinding) binding;
          if (method.isConstructor()) bindingType = "Constructor";
          else bindingType = "Method";
          break;
        case IBinding.TYPE: bindingType = "Type"; break;
        case IBinding.VARIABLE:
          IVariableBinding variable = (IVariableBinding) binding;
          if (variable.isField()) bindingType = "Field";
          else if (variable.isParameter()) bindingType = "Parameter";
          else if (variable.isEnumConstant()) bindingType = "Enum constant";
          else bindingType = "Local variable";
          break;
      }

      DefaultMutableTreeNode rootNode =
          new DefaultMutableTreeNode(bindingType + ": " + binding.getName());

      Map<Integer, List<ShowUsageTreeNode>> tabGroupedTreeNodes = occurrences.stream()
          // Convert to TreeNodes
          .map(name -> ShowUsageTreeNode.fromSimpleName(ps, name))
          // TODO: this has to be fixed with better token mapping
          // remove occurrences which fall into generated header
          .filter(node -> node.tabIndex != 0 || (node.startTabOffset >= 0 && node.stopTabOffset > 0))
          // Group by tab
          .collect(Collectors.groupingBy(node -> node.tabIndex));

      tabGroupedTreeNodes.entrySet().stream()
          // Sort by tab index
          .sorted(Comparator.comparing(Map.Entry::getKey))
          .map(entry -> {
            Integer tabIndex = entry.getKey();
            List<ShowUsageTreeNode> nodes = entry.getValue();

            int count = nodes.size();
            String usageLabel = count == 1 ? "usage" : "usages";

            // Create new DefaultMutableTreeNode for this tab
            String tabLabel = "<html><font color=#222222>" +
                ps.sketch.getCode(tabIndex).getPrettyName() +
                "</font> <font color=#999999>" + count + " " + usageLabel + "</font></html>";
            DefaultMutableTreeNode tabNode = new DefaultMutableTreeNode(tabLabel);

            nodes.stream()
                // Convert TreeNodes to DefaultMutableTreeNodes
                .map(DefaultMutableTreeNode::new)
                // Add all as children of tab node
                .forEach(tabNode::add);
            return tabNode;
          })
          // Add all tab nodes as children of root node
          .forEach(rootNode::add);

      EventQueue.invokeLater(() -> {
        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        showUsageTree.setModel(model);

        // Expand all nodes
        for (int i = 0; i < showUsageTree.getRowCount(); i++) {
          showUsageTree.expandRow(i);
        }

        showUsageTree.setRootVisible(true);

        boolean setLocation = false;
        if (showUsageWindow.getState() == JFrame.ICONIFIED) {
          showUsageWindow.setState(JFrame.NORMAL);
          setLocation = true;
        }
        if (!showUsageWindow.isVisible()) {
          showUsageWindow.setVisible(true);
          setLocation = true;
        }
        if (setLocation) {
          GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
          GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
          Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
          int maxX = (int) rect.getMaxX() - showUsageWindow.getWidth();
          int x = Math.min(editor.getX() + editor.getWidth(), maxX);
          int y = (x == maxX) ? 10 : editor.getY();
          showUsageWindow.setLocation(x, y);
        }

        showUsageWindow.setTitle("Usage of \"" + binding.getName() + "\" : "
                                     + occurrences.size() + " time(s)");
      });
    }


    public void disposeAllWindows() {
      Messages.log("* disposeAllWindows");
      disposeWindow(showUsageWindow, renameWindow);

      if (debugTreeWindow != null) disposeWindow(debugTreeWindow);
    }


    public static void disposeWindow(JFrame... f) {
      for (JFrame jFrame : f) {
        if (jFrame != null)
          jFrame.dispose();
      }
    }


    /// DEBUG --------------------------------------------------------------------


    protected JFrame debugTreeWindow;

    /** Swing component wrapper for AST, used for internal testing */
    protected JTree debugTree;


    protected void initDebugWindow() {
      debugTreeWindow = new JFrame();

      debugTree = new JTree() {
        @Override
        public String convertValueToText(Object value, boolean selected,
                                         boolean expanded, boolean leaf,
                                         int row, boolean hasFocus) {
          if (value instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
            Object o = treeNode.getUserObject();
            if (o instanceof ASTNode) {
              ASTNode node = (ASTNode) o;
              return getNodeAsString(node);
            }
          }
          return super.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
        }
      };
      debugTreeWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      debugTreeWindow.setBounds(new Rectangle(680, 100, 460, 620));
      debugTreeWindow.setTitle("AST View - " + editor.getSketch().getName());
      JScrollPane sp = new JScrollPane();
      sp.setViewportView(debugTree);
      debugTreeWindow.add(sp);
    }


    protected void updateDebugTree(DefaultMutableTreeNode codeTree) {
      if (debugTree.hasFocus() || debugTreeWindow.hasFocus()) {
        return;
      }
      debugTree.setModel(new DefaultTreeModel(codeTree));
      ((DefaultTreeModel) debugTree.getModel()).reload();
      debugTree.validate();
      if (!debugTreeWindow.isVisible()) {
        debugTreeWindow.setVisible(true);
      }
    }


    protected void addDebugTreeListener() {
      debugTree.addTreeSelectionListener(new TreeSelectionListener() {

        @Override
        public void valueChanged(TreeSelectionEvent e) {
          if (debugTree.getLastSelectedPathComponent() == null) {
            return;
          }
          DefaultMutableTreeNode tnode =
              (DefaultMutableTreeNode) debugTree.getLastSelectedPathComponent();
          if (tnode.getUserObject() instanceof ASTNode) {
            ASTNode node = (ASTNode) tnode.getUserObject();
            int startOffset = node.getStartPosition();
            int length = node.getLength();
            astGen.errorCheckerService.highlightJavaRange(startOffset, length);
          }
        }

      });
    }

  }

  protected static class ShowUsageTreeNode {

    int tabIndex;
    int tabLine;
    int startTabOffset;
    int stopTabOffset;

    String text;

    public static ShowUsageTreeNode fromSimpleName(PreprocessedSketch ps, SimpleName name) {
      int startJavaOffset = name.getStartPosition();
      int stopJavaOffset = startJavaOffset + name.getLength();

      int startPdeOffset = ps.javaOffsetToPdeOffset(startJavaOffset);
      int stopPdeOffset = ps.javaOffsetToPdeOffset(stopJavaOffset);

      int tabIndex = ps.pdeOffsetToTabIndex(startPdeOffset);

      int startTabOffset = ps.pdeOffsetToTabOffset(tabIndex, startPdeOffset);
      int stopTabOffset = ps.pdeOffsetToTabOffset(tabIndex, stopPdeOffset);

      int tabLine = ps.tabOffsetToTabLine(tabIndex, startTabOffset);

      int lineStartPdeOffset = ps.pdeCode.lastIndexOf('\n', startPdeOffset) + 1;
      int lineStopPdeOffset = ps.pdeCode.indexOf('\n', stopPdeOffset);

      int snippetStartOffset = startPdeOffset - lineStartPdeOffset;
      int snippetStopOffset = stopPdeOffset - lineStartPdeOffset;

      // TODO: what a mess
      String line = ps.pdeCode.substring(lineStartPdeOffset, lineStopPdeOffset);
      String pre = line.substring(0, snippetStartOffset)
          .replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;");
      String highlight = line.substring(snippetStartOffset, snippetStopOffset)
          .replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;");
      String post = line.substring(snippetStopOffset)
          .replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;");
      line = pre + "<font color=#222222><b>" + highlight + "</b></font>" + post;
      line = line.trim();

      ShowUsageTreeNode node = new ShowUsageTreeNode();
      node.tabIndex = tabIndex;
      node.tabLine = tabLine;
      node.startTabOffset = startTabOffset;
      node.stopTabOffset = stopTabOffset;

      node.text = "<html><font color=#bbbbbb>" +
          (tabLine + 1) + "</font> <font color=#777777>" + line + "</font></html>";

      return node;
    }

    @Override
    public String toString() {
      return text;
    }
  }

}
