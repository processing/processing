/*
  Part of the XQMode project - https://github.com/Manindra29/XQMode
  
  Under Google Summer of Code 2012 - 
  http://www.google-melange.com/gsoc/homepage/google/gsoc2012
  
  Copyright (C) 2012 Manindra Moharana
	
  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.mode.experimental;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import processing.app.Preferences;
import processing.core.PApplet;

/**
 * My implementation of P5 preprocessor. Uses Eclipse JDT features instead of
 * ANTLR. Performance gains mostly and full control over debug output. But needs
 * lots and lots of testing. There will always an option to switch back to PDE
 * preproc.
 * 
 * @author Manindra Moharana &lt;me@mkmoharana.com&gt;
 * 
 */
public class XQPreprocessor {

	private ASTRewrite rewrite = null;
	public int mainClassOffset = 0;
	ArrayList<String> imports;
	ArrayList<ImportStatement> extraImports;

	/**
	 * The main method that performs preprocessing. Converts code into compilable java.
	 * @param source - String
	 * @param programImports - List of import statements
	 * @return String - Compile ready java code
	 */
	public String doYourThing(String source,
			ArrayList<ImportStatement> programImports) {
		this.extraImports = programImports;
		source = prepareImports() + source;
		Document doc = new Document(source);

		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(doc.get().toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		@SuppressWarnings("unchecked")
		Map<String, String> options = JavaCore.getOptions();

		// Ben has decided to move on to 1.6. Yay!
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
		parser.setCompilerOptions(options);
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		cu.recordModifications();
		rewrite = ASTRewrite.create(cu.getAST());
		cu.accept(new XQASTVisitor());

		TextEdit edits = cu.rewrite(doc, null);
		try {
			edits.apply(doc);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		// System.out.println("------------XQPreProc-----------------");
		// System.out.println(doc.get());
		// System.out.println("------------XQPreProc End-----------------");

		// Calculate main class offset
		int position = doc.get().indexOf("{") + 1;
		int lines = 0;
		for (int i = 0; i < position; i++) {
			if (doc.get().charAt(i) == '\n')
				lines++;
		}
		lines += 2;
		// System.out.println("Lines: " + lines);
		mainClassOffset = lines;

		return doc.get();
	}

	/**
	 * Returns all import statements as lines of code
	 * 
	 * @return String - All import statements combined
	 */
	public String prepareImports() {
		imports = new ArrayList<String>();
		for (int i = 0; i < extraImports.size(); i++) {
			imports.add(new String(extraImports.get(i).importName));
		}
		imports.add(new String("// Default Imports"));
		for (int i = 0; i < getCoreImports().length; i++) {
			imports.add(new String("import " + getCoreImports()[i] + ";"));
		}
		for (int i = 0; i < getDefaultImports().length; i++) {
			imports.add(new String("import " + getDefaultImports()[i] + ";"));
		}
		String totalImports = "";
		for (int i = 0; i < imports.size(); i++) {
			totalImports += imports.get(i) + "\n";
		}
		totalImports += "\n";
		return totalImports;
	}

	public String[] getCoreImports() {
		return new String[] { "processing.core.*", "processing.data.*",
				"processing.opengl.*" };
	}

	public String[] getDefaultImports() {
		// These may change in-between (if the prefs panel adds this option)
		String prefsLine = Preferences.get("preproc.imports.list");
		return PApplet.splitTokens(prefsLine, ", ");
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
	 * 
	 * @author Manindra Moharana
	 * 
	 */
	private class XQASTVisitor extends ASTVisitor {
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
			if (!node.getToken().endsWith("f")
					&& !node.getToken().endsWith("d")) {
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

		/**
		 * This is added just for debugging purposes - to make sure that all
		 * instances of color type have been substituded as in by the regex
		 * search in ErrorCheckerService.preprocessCode().
		 */
		public boolean visit(SimpleType node) {
			if (node.toString().equals("color")) {
				System.err
						.println("color type detected! \nThis shouldn't be happening! Please report this as an issue on www.github.com/Manindra29/XQMode");
			}
			return true;

		}

	}

}
