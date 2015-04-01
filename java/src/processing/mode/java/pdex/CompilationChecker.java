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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jface.text.Document;


/**
 * Provides compilation checking functionality
 * @author Manindra Moharana &lt;me@mkmoharana.com&gt;
 */
public class CompilationChecker {

  private class CompilationUnitImpl implements ICompilationUnit {
    private CompilationUnit unit;

    CompilationUnitImpl(CompilationUnit unit) {
      this.unit = unit;
    }

    public char[] getContents() {
      char[] contents = null;
      try {
        Document doc = new Document();
        doc.set(sourceText);
        // TextEdit edits = unit.rewrite(doc, null);
        // edits.apply(doc);
        String sourceCode = doc.get();
        if (sourceCode != null)
          contents = sourceCode.toCharArray();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return contents;
    }

    public char[] getMainTypeName() {
      TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
      return classType.getName().getFullyQualifiedName().toCharArray();
    }

    public char[][] getPackageName() {
      String[] names = getSimpleNames(this.unit.getPackage().getName()
          .getFullyQualifiedName());
      char[][] packages = new char[names.length][];
      for (int i = 0; i < names.length; ++i)
        packages[i] = names[i].toCharArray();

      return packages;
    }

    public char[] getFileName() {
      TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
      String name = classType.getName().getFullyQualifiedName() + ".java";
      return name.toCharArray();
    }

    @Override
    public boolean ignoreOptionalProblems() {
      return false;
    }
  }

  /**
   * ICompilerRequestor implementation
   */
  private class CompileRequestorImpl implements ICompilerRequestor {

    private List<IProblem> problems;

    private List<ClassFile> classes;

    public CompileRequestorImpl() {
      this.problems = new ArrayList<IProblem>();
      this.classes = new ArrayList<ClassFile>();
    }

    public void acceptResult(CompilationResult result) {
      boolean errors = false;
      if (result.hasProblems()) {
        IProblem[] problems = result.getProblems();
        for (int i = 0; i < problems.length; i++) {
          if (problems[i].isError())
            errors = true;

          this.problems.add(problems[i]);
        }
      }
      if (!errors) {
        ClassFile[] classFiles = result.getClassFiles();
        for (int i = 0; i < classFiles.length; i++)
          this.classes.add(classFiles[i]);
      }
    }

    List<IProblem> getProblems() {
      return this.problems;
    }

//    List<ClassFile> getResults() {
//      //System.out.println("Calling get results");
//      return this.classes;
//    }
  }

  /**
   * INameEnvironment implementation
   */
  private class NameEnvironmentImpl implements INameEnvironment {

    private ICompilationUnit unit;

    private String fullName;

    NameEnvironmentImpl(ICompilationUnit unit) {
      this.unit = unit;
      this.fullName = CharOperation.toString(this.unit.getPackageName()) + "."
          + new String(this.unit.getMainTypeName());
    }

    public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
      return findType(CharOperation.toString(compoundTypeName));
    }

    public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
      String fullName = CharOperation.toString(packageName);
      if (typeName != null) {
        if (fullName.length() > 0)
          fullName += ".";

        fullName += new String(typeName);
      }
      return findType(fullName);
    }

    public boolean isPackage(char[][] parentPackageName, char[] packageName) {
      String fullName = CharOperation.toString(parentPackageName);
      if (packageName != null) {
        if (fullName.length() > 0)
          fullName += ".";

        fullName += new String(packageName);
      }
      if (findType(fullName) != null)
        return false;

      try {
        return (getClassLoader().loadClass(fullName) == null);
      } catch (ClassNotFoundException e) {
        return true;
      }
    }

    public void cleanup() {
    }

    private NameEnvironmentAnswer findType(String fullName) {

      if (this.fullName.equals(fullName))
        return new NameEnvironmentAnswer(unit, null);

      try {
        InputStream is = getClassLoader().getResourceAsStream(fullName
                                                                  .replace('.',
                                                                           '/')
                                                                  + ".class");
        if (is != null) {
          // System.out.println("Find type: " + fullName);
          byte[] buffer = new byte[8192];
          int bytes = 0;
          ByteArrayOutputStream os = new ByteArrayOutputStream(buffer.length);
          while ((bytes = is.read(buffer, 0, buffer.length)) > 0)
            os.write(buffer, 0, bytes);

          os.flush();
          ClassFileReader classFileReader = new ClassFileReader(
                                                                os.toByteArray(),
                                                                fullName
                                                                    .toCharArray(),
                                                                true);
          return new NameEnvironmentAnswer(classFileReader, null);
        }
        return null;
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (ClassFormatException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private URLClassLoader urlClassLoader;

  private ClassLoader getClassLoader() {
    if (urlClassLoader != null) {
      return urlClassLoader;
    } else {
      return getClass().getClassLoader();
    }
  }

  private void prepareClassLoader(ArrayList<File> jarList) {
    URL urls[] = new URL[jarList.size()];
    for (int i = 0; i < urls.length; i++) {
      try {
        urls[i] = jarList.get(i).toURI().toURL();
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
    }
    urlClassLoader = new URLClassLoader(urls);
    //System.out.println("URL Classloader ready");
  }

  /**
   * ClassLoader implementation
   */
  /*
  private class CustomClassLoader extends ClassLoader {

    private Map classMap;

    CustomClassLoader(ClassLoader parent, List classesList) {
      this.classMap = new HashMap();
      for (int i = 0; i < classesList.size(); i++) {
        ClassFile classFile = (ClassFile) classesList.get(i);
        String className = CharOperation.toString(classFile.getCompoundName());
        this.classMap.put(className, classFile.getBytes());
      }
    }

    public Class findClass(String name) throws ClassNotFoundException {
      byte[] bytes = (byte[]) this.classMap.get(name);
      if (bytes != null)
        return defineClass(name, bytes, 0, bytes.length);

      return super.findClass(name);
    }
  };
  */

  @SuppressWarnings("unchecked")
  private ICompilationUnit generateCompilationUnit() {
    ASTParser parser = ASTParser.newParser(AST.JLS8);
    try {
      parser.setSource("".toCharArray());
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Map<String, String> options = JavaCore.getOptions();

    JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
    parser.setCompilerOptions(options);
    CompilationUnit unit = (CompilationUnit) parser.createAST(null);
    unit.recordModifications();

    AST ast = unit.getAST();

    // Package statement
    // package astexplorer;

    PackageDeclaration packageDeclaration = ast.newPackageDeclaration();
    unit.setPackage(packageDeclaration);
    // unit.se
    packageDeclaration.setName(ast.newSimpleName(fileName));
    // System.out.println("Filename: " + fileName);
    // class declaration
    // public class SampleComposite extends Composite {

    TypeDeclaration classType = ast.newTypeDeclaration();
    classType.setInterface(false);
    // classType.s
    classType.setName(ast.newSimpleName(fileName));
    unit.types().add(classType);
    // classType.setSuperclass(ast.newSimpleName("Composite"));
    return new CompilationUnitImpl(unit);
  }

  static private String fileName = null;  //"HelloPeasy";



  private void compileMeQuitely(ICompilationUnit unit, Map<String, String> compilerSettings) {

    Map<String, String> settings;
    if (compilerSettings == null) {
      settings = new HashMap<>();

      settings.put(CompilerOptions.OPTION_LineNumberAttribute,
                   CompilerOptions.GENERATE);
      settings.put(CompilerOptions.OPTION_SourceFileAttribute,
                   CompilerOptions.GENERATE);
      settings.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_6);
      settings.put(CompilerOptions.OPTION_SuppressWarnings,
                   CompilerOptions.DISABLED);
      // settings.put(CompilerOptions.OPTION_ReportUnusedImport,
      // CompilerOptions.IGNORE);
      // settings.put(CompilerOptions.OPTION_ReportMissingSerialVersion,
      // CompilerOptions.IGNORE);
      // settings.put(CompilerOptions.OPTION_ReportRawTypeReference,
      // CompilerOptions.IGNORE);
      // settings.put(CompilerOptions.OPTION_ReportUncheckedTypeOperation,
      // CompilerOptions.IGNORE);
    } else {
      settings = compilerSettings;
    }

//    CompilerOptions cop = new CompilerOptions();
//    cop.set(settings);
    CompileRequestorImpl requestor = new CompileRequestorImpl();
    Compiler compiler = new Compiler(new NameEnvironmentImpl(unit),
                                     DefaultErrorHandlingPolicies
                                         .proceedWithAllProblems(),
                                         new CompilerOptions(settings), requestor,
                                     new DefaultProblemFactory(Locale
                                         .getDefault()));
    compiler.compile(new ICompilationUnit[] { unit });

    List<IProblem> problems = requestor.getProblems();
    prob = new IProblem[problems.size()];
    int count = 0;
    for (Iterator<IProblem> it = problems.iterator(); it.hasNext();) {
      IProblem problem = it.next();
      prob[count++] = problem;
    }
  }

  private void compileMeQuitely(ICompilationUnit unit) {
    compileMeQuitely(unit, null);
  }

  static private String[] getSimpleNames(String qualifiedName) {
    StringTokenizer st = new StringTokenizer(qualifiedName, ".");
    ArrayList<String> list = new ArrayList<String>();
    while (st.hasMoreTokens()) {
      String name = st.nextToken().trim();
      if (!name.equals("*"))
        list.add(name);
    }
    return list.toArray(new String[0]);
  }

  public static void main(String[] args) {
    ArrayList<File> fl = new ArrayList<File>();
    fl.add(new File(
                    "/home/quarkninja/Workspaces/processing_workspace/processing/core/library/core.jar"));
    CompilationChecker cc = new CompilationChecker(fl);
    cc.getErrors("Brightness");
    cc.display();
  }

  public void display() {
    boolean error = false;
    int errorCount = 0, warningCount = 0, count = 0;
    for (int i = 0; i < prob.length; i++) {
      IProblem problem = prob[i];
      if (problem == null)
        continue;
      StringBuilder sb = new StringBuilder();
      sb.append(problem.getMessage());
      sb.append(" | line: ");
      sb.append(problem.getSourceLineNumber());
      String msg = sb.toString();
      if (problem.isError()) {
        error = true;
        msg = "Error: " + msg;
        errorCount++;
      } else if (problem.isWarning()) {
        msg = "Warning: " + msg;
        warningCount++;
      }
      System.out.println(msg);
      prob[count++] = problem;
    }

    if (!error) {
      System.out.println("====================================");
      System.out.println("    Compiled without any errors.    ");
      System.out.println("====================================");
    } else {
      System.out.println("====================================");
      System.out.println(" Compilation failed. You erred man! ");
      System.out.println("====================================");

    }
    System.out.print("Total warnings: " + warningCount);
    System.out.println(", Total errors: " + errorCount);
  }

  IProblem[] prob;

  public IProblem[] getErrors(String name) {
    fileName = name;
    compileMeQuitely(generateCompilationUnit());
    // System.out.println("getErrors()");

    return prob;
  }

  /**
   * Performs compiler error check.
   * @param sourceName - name of the class 
   * @param source - source code
   * @param settings - compiler options
   * @param classLoader - custom classloader which can load all dependencies
   * @return IProblem[] - list of compiler errors and warnings
   */
  public IProblem[] getErrors(String sourceName, String source, Map<String, String> settings,
                              URLClassLoader classLoader) {
    fileName = sourceName;
    sourceText = "package " + fileName + ";\n" + source;
    if (classLoader != null)
      this.urlClassLoader = classLoader;
    compileMeQuitely(generateCompilationUnit(), settings);
    // System.out.println("getErrors(), Done.");

    return prob;
  }

  String sourceText = null;  //"";

  
  public IProblem[] getErrors(String sourceName, String source) {
    return getErrors(sourceName, source, null);
  }

  
  public IProblem[] getErrors(String sourceName, String source, Map<String, String> settings) {
    fileName = sourceName;
    sourceText = "package " + fileName + ";\n" + source;

    compileMeQuitely(generateCompilationUnit(), settings);
    // System.out.println("getErrors(), Done.");
    return prob;
  }

  
  public CompilationChecker() {
    // System.out.println("Compilation Checker initialized.");
  }

  
  public CompilationChecker(ArrayList<File> fileList) {
    prepareClassLoader(fileList);
    // System.out.println("Compilation Checker initialized.");
  }
}
