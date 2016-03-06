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

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import processing.app.Util;
import processing.data.StringList;
import processing.mode.java.preproc.PdePreprocessor;


/**
 * Implementation of PdePreprocessor that uses Eclipse JDT features
 * instead of ANTLR.
 */
public class XQPreprocessor {
  private ASTRewrite rewrite = null;
  private List<ImportStatement> extraImports;
  private ErrorCheckerService ecs;
  private String[] coreImports;
  private String[] defaultImports;


  public XQPreprocessor(ErrorCheckerService ecs) {
    this.ecs = ecs;
    
    // get parameters from the main preproc
//    PdePreprocessor p = new PdePreprocessor(null);
    PdePreprocessor p = ecs.editor.createPreprocessor(null);
    defaultImports = p.getDefaultImports();
    coreImports = p.getCoreImports();
  }


  /**
   * The main preprocessing method that converts code into compilable Java.
   */
  protected String handle(String source,
                          List<ImportStatement> programImports) {
    this.extraImports = programImports;
    Document doc = new Document(source);

    ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setSource(doc.get().toCharArray());
    parser.setKind(ASTParser.K_COMPILATION_UNIT);

    parser.setCompilerOptions(ErrorCheckerService.COMPILER_OPTIONS);
    CompilationUnit cu = (CompilationUnit) parser.createAST(null);
    cu.recordModifications();
    rewrite = ASTRewrite.create(cu.getAST());
    cu.accept(new XQVisitor());

    TextEdit edits = cu.rewrite(doc, null);
    try {
      edits.apply(doc);
    } catch (MalformedTreeException | BadLocationException e) {
      e.printStackTrace();
    }
    return doc.get();
  }


  protected String prepareImports(List<ImportStatement> programImports) {
    this.extraImports = programImports;

    StringList imports = new StringList();
    for (ImportStatement imp : extraImports) {
      imports.append(imp.getImportName());
    }
    imports.append("// Default Imports");
    for (String imp : coreImports) {
      imports.append("import " + imp + ";");
    }
    for (String imp : defaultImports) {
      imports.append("import " + imp + ";");
    }
    if (ecs.getEditor().getSketch().getCodeFolder().exists()) {
      StringList codeFolderPackages = null;
      String codeFolderClassPath = Util.contentsToClassPath(ecs.getEditor().getSketch().getCodeFolder());
      codeFolderPackages = Util.packageListFromClassPath(codeFolderClassPath);
      if (codeFolderPackages != null) {
        ecs.codeFolderImports.clear();
        for (String item : codeFolderPackages) {
          // Messages.log("CF import " + item);
          imports.append("import " + item + ".*;");
          ecs.codeFolderImports.add(new ImportStatement("import " + item + ".*;",0,0));
        }
      }
    }
    return imports.join("\n") + "\n";
  }


  /**
   * Visitor implementation that does all the substitution dirty work. <br>
   * <LI>Any function not specified as being protected or private will be made
   * 'public'. This means that <TT>void setup()</TT> becomes
   * <TT>public void setup()</TT>.
   *
   * <LI>Converts doubles into floats, i.e. 12.3 becomes 12.3f so that people
   * don't have to add f after their numbers all the time since it's confusing
   * for beginners. Also, most functions of p5 core deal with floats only.
   */
  private class XQVisitor extends ASTVisitor {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean visit(MethodDeclaration node) {
      if (node.getReturnType2() != null) {
        // if return type is color, make it int
        // if (node.getReturnType2().toString().equals("color")) {
        // System.err.println("color type detected!");
        // node.setReturnType2(rewrite.getAST().newPrimitiveType(
        // PrimitiveType.INT));
        // }

        // The return type is not void, no need to make it public
        // if (!node.getReturnType2().toString().equals("void"))
        // return true;
      }

      // Simple method, make it public
      if (node.modifiers().size() == 0 && !node.isConstructor()) {
        // rewrite.set(node, node.getModifiersProperty(),
        // Modifier.PUBLIC,
        // null);
        // rewrite.getListRewrite(node,
        // node.getModifiersProperty()).insertLast(Modifier., null)
        List newMod = rewrite.getAST().newModifiers(Modifier.PUBLIC);
        node.modifiers().add(newMod.get(0));
      }

      return true;
    }


    public boolean visit(NumberLiteral node) {
      // Need to handle both 1.0F and 1.0D cases
      // https://github.com/processing/processing/issues/3707
      String lower = node.getToken().toLowerCase();
      if (!lower.endsWith("f") && !lower.endsWith("d")) {
        for (int i = 0; i < node.getToken().length(); i++) {
          if (node.getToken().charAt(i) == '.') {
            String s = node.getToken() + "f";
            node.setToken(s);
            break;
          }
        }
      }
      return true;
    }


    // public boolean visit(FieldDeclaration node) {
    // if (node.getType().toString().equals("color")){
    // System.err.println("color type detected!");
    // node.setType(rewrite.getAST().newPrimitiveType(
    // PrimitiveType.INT));
    // }
    // return true;
    // }
    //
    // public boolean visit(VariableDeclarationStatement node) {
    // if (node.getType().toString().equals("color")){
    // System.err.println("color type detected!");
    // node.setType(rewrite.getAST().newPrimitiveType(
    // PrimitiveType.INT));
    // }
    // return true;
    // }


//    /**
//     * This is added just for debugging purposes - to make sure that all
//     * instances of color type have been substituded as in by the regex
//     * search in ErrorCheckerService.preprocessCode().
//     */
//    public boolean visit(SimpleType node) {
//      if (node.toString().equals("color")) {
//        System.err.println("Color type detected: please report as an issue.");
//      }
//      return true;
//    }
  }
}
