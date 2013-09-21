/*
 * Copyright 2012, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.appbundler;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.resources.FileResource;

/**
 * App bundler Ant task.
 */
public class AppBundlerTask extends Task {
  // Output folder for generated bundle
  private File outputDirectory = null;

  // General bundle properties
  private String name = null;
  private String displayName = null;
  private String identifier = null;
  private File icon = null;
  private String executableName = EXECUTABLE_NAME;

  private String shortVersion = "1.0";
  private String version = "1.0";
  private String signature = "????";
  private String copyright = "";
  private String privileged = null;
  private String workingDirectory = null;

  private String applicationCategory = null;

  private boolean highResolutionCapable = true;

  // JVM info properties
  private String mainClassName = null;
  private FileSet runtime = null;
  private ArrayList<FileSet> classPath = new ArrayList<>();
  private ArrayList<FileSet> libraryPath = new ArrayList<>();
  private ArrayList<String> options = new ArrayList<>();
  private ArrayList<String> arguments = new ArrayList<>();
  private ArrayList<String> architectures = new ArrayList<>();
  private ArrayList<BundleDocument> bundleDocuments = new ArrayList<>();

  private Reference classPathRef;

  private static final String EXECUTABLE_NAME = "JavaAppLauncher";
  private static final String DEFAULT_ICON_NAME = "GenericApp.icns";
  private static final String OS_TYPE_CODE = "APPL";

  private static final String PLIST_DTD = "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">";
  private static final String PLIST_TAG = "plist";
  private static final String PLIST_VERSION_ATTRIBUTE = "version";
  private static final String DICT_TAG = "dict";
  private static final String KEY_TAG = "key";
  private static final String ARRAY_TAG = "array";
  private static final String STRING_TAG = "string";

  private static final int BUFFER_SIZE = 2048;


  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }


  public void setName(String name) {
    this.name = name;
  }


  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }


  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }


  public void setIcon(File icon) {
    this.icon = icon;
  }


  public void setExecutableName(String executable) {
    this.executableName = executable;
  }


  public void setShortVersion(String shortVersion) {
    this.shortVersion = shortVersion;
  }


  public void setVersion(String version) {
    this.version = version;
  }


  public void setSignature(String signature) {
    this.signature = signature;
  }


  public void setCopyright(String copyright) {
    this.copyright = copyright;
  }


  public void setPrivileged(String privileged) {
    this.privileged = privileged;
  }


  public void setWorkingDirectory(String workingDirectory) {
    this.workingDirectory = workingDirectory;
  }


  public void setApplicationCategory(String applicationCategory) {
    this.applicationCategory = applicationCategory;
  }


  public void setHighResolutionCapable(boolean highResolutionCapable) {
    this.highResolutionCapable = highResolutionCapable;
  }


  public void setMainClassName(String mainClassName) {
    this.mainClassName = mainClassName;
  }


  public void addConfiguredRuntime(FileSet runtime) throws BuildException {
    if (this.runtime != null) {
      throw new BuildException("Runtime already specified.");
    }

    this.runtime = runtime;

    runtime.appendIncludes(new String[] {
      "jre/",
    });

    runtime.appendExcludes(new String[] {
      "bin/",
      "jre/bin/",
      "jre/lib/deploy/",
      "jre/lib/deploy.jar",
      "jre/lib/javaws.jar",
      "jre/lib/libdeploy.dylib",
      "jre/lib/libnpjp2.dylib",
      "jre/lib/plugin.jar",
      "jre/lib/security/javaws.policy"
    });
  }


  public void setClasspathRef(Reference ref) {
    this.classPathRef = ref;
  }


  public void addConfiguredClassPath(FileSet classPath) {
    this.classPath.add(classPath);
  }


  public void addConfiguredLibraryPath(FileSet libraryPath) {
    this.libraryPath.add(libraryPath);
  }


  public void addConfiguredBundleDocument(BundleDocument document) {
    this.bundleDocuments.add(document);
  }


  public void addConfiguredOption(Option option) throws BuildException {
    String value = option.getValue();

    if (value == null) {
      throw new BuildException("Value is required.");
    }

    options.add(value);
  }


  public void addConfiguredArgument(Argument argument) throws BuildException {
    String value = argument.getValue();

    if (value == null) {
      throw new BuildException("Value is required.");
    }

    arguments.add(value);
  }


  public void addConfiguredArch(Architecture architecture) throws BuildException {
    String name = architecture.getName();

    if (name == null) {
      throw new BuildException("Name is required.");
    }

    architectures.add(name);
  }


  @Override
  public void execute() throws BuildException {
    // Validate required properties
    if (outputDirectory == null) {
      throw new IllegalStateException("Output directory is required.");
    }

    if (!outputDirectory.exists()) {
      throw new IllegalStateException("Output directory does not exist.");
    }

    if (!outputDirectory.isDirectory()) {
      throw new IllegalStateException("Invalid output directory.");
    }

    if (name == null) {
      throw new IllegalStateException("Name is required.");
    }

    if (displayName == null) {
      throw new IllegalStateException("Display name is required.");
    }

    if (identifier == null) {
      throw new IllegalStateException("Identifier is required.");
    }

    if (icon != null) {
      if (!icon.exists()) {
        throw new IllegalStateException("Icon does not exist.");
      }

      if (icon.isDirectory()) {
        throw new IllegalStateException("Invalid icon.");
      }
    }

    if (shortVersion == null) {
      throw new IllegalStateException("Short version is required.");
    }

    if (signature == null) {
      throw new IllegalStateException("Signature is required.");
    }

    if (signature.length() != 4) {
      throw new IllegalStateException("Invalid signature.");
    }

    if (copyright == null) {
      throw new IllegalStateException("Copyright is required.");
    }

    if (mainClassName == null) {
      throw new IllegalStateException("Main class name is required.");
    }

    // Create the app bundle
    try {
      System.out.println("Creating app bundle: " + name);

      // Create directory structure
      File rootDirectory = new File(outputDirectory, name + ".app");
      delete(rootDirectory);
      rootDirectory.mkdir();

      File contentsDirectory = new File(rootDirectory, "Contents");
      contentsDirectory.mkdir();

      File macOSDirectory = new File(contentsDirectory, "MacOS");
      macOSDirectory.mkdir();

      File javaDirectory = new File(contentsDirectory, "Java");
      javaDirectory.mkdir();

      File plugInsDirectory = new File(contentsDirectory, "PlugIns");
      plugInsDirectory.mkdir();

      File resourcesDirectory = new File(contentsDirectory, "Resources");
      resourcesDirectory.mkdir();

      // Generate Info.plist
      File infoPlistFile = new File(contentsDirectory, "Info.plist");
      infoPlistFile.createNewFile();
      writeInfoPlist(infoPlistFile);

      // Generate PkgInfo
      File pkgInfoFile = new File(contentsDirectory, "PkgInfo");
      pkgInfoFile.createNewFile();
      writePkgInfo(pkgInfoFile);

      // Copy executable to MacOS folder
      File executableFile = new File(macOSDirectory, executableName);
      copy(getClass().getResource(EXECUTABLE_NAME), executableFile);

      executableFile.setExecutable(true, false);

      // Copy localized resources to Resources folder
      copyResources(resourcesDirectory);

      // Copy runtime to PlugIns folder
      copyRuntime(plugInsDirectory);

      // Copy class path entries to Java folder
      copyClassPathEntries(javaDirectory);

      // Copy class path ref entries to Java folder
      copyClassPathRefEntries(javaDirectory);

      // Copy library path entries to MacOS folder
      copyLibraryPathEntries(macOSDirectory);

      // Copy icon to Resources folder
      copyIcon(resourcesDirectory);

    } catch (IOException exception) {
      throw new BuildException(exception);
    }
  }


  private void copyResources(File resourcesDirectory) throws IOException {
    // Unzip res.zip into resources directory
    InputStream inputStream = getClass().getResourceAsStream("res.zip");
    ZipInputStream zipInputStream = new ZipInputStream(inputStream);

    try {
      ZipEntry zipEntry = zipInputStream.getNextEntry();
      while (zipEntry != null) {
        File file = new File(resourcesDirectory, zipEntry.getName());

        if (zipEntry.isDirectory()) {
          file.mkdir();
        } else {
          OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE);

          try {
            int b = zipInputStream.read();
            while (b != -1) {
              outputStream.write(b);
              b = zipInputStream.read();
            }

            outputStream.flush();
          } finally {
            outputStream.close();
          }

        }

        zipEntry = zipInputStream.getNextEntry();
      }
    } finally {
      zipInputStream.close();
    }
  }


  private void copyRuntime(File plugInsDirectory) throws IOException {
    if (runtime != null) {
      File runtimeHomeDirectory = runtime.getDir();
      File runtimeContentsDirectory = runtimeHomeDirectory.getParentFile();
      File runtimeDirectory = runtimeContentsDirectory.getParentFile();

      // Create root plug-in directory
      File pluginDirectory = new File(plugInsDirectory, runtimeDirectory.getName());
      pluginDirectory.mkdir();

      // Create Contents directory
      File pluginContentsDirectory = new File(pluginDirectory, runtimeContentsDirectory.getName());
      pluginContentsDirectory.mkdir();

      // Copy MacOS directory
      File runtimeMacOSDirectory = new File(runtimeContentsDirectory, "MacOS");
      copy(runtimeMacOSDirectory, new File(pluginContentsDirectory, runtimeMacOSDirectory.getName()));

      // Copy Info.plist file
      File runtimeInfoPlistFile = new File(runtimeContentsDirectory, "Info.plist");
      copy(runtimeInfoPlistFile, new File(pluginContentsDirectory, runtimeInfoPlistFile.getName()));

      // Copy included contents of Home directory
      File pluginHomeDirectory = new File(pluginContentsDirectory, runtimeHomeDirectory.getName());

      DirectoryScanner directoryScanner = runtime.getDirectoryScanner(getProject());
      String[] includedFiles = directoryScanner.getIncludedFiles();

      for (int i = 0; i < includedFiles.length; i++) {
        String includedFile = includedFiles[i];
        File source = new File(runtimeHomeDirectory, includedFile);
        File destination = new File(pluginHomeDirectory, includedFile);
        copy(source, destination);
      }
    }
  }


  private void copyClassPathRefEntries(File javaDirectory) throws IOException {
    if(classPathRef != null) {
      org.apache.tools.ant.types.Path classpath =
          (org.apache.tools.ant.types.Path) classPathRef.getReferencedObject(getProject());

      Iterator<FileResource> iter = (Iterator<FileResource>)(Object)classpath.iterator();
      while(iter.hasNext()) {
        FileResource resource = iter.next();
        File source = resource.getFile();
        File destination = new File(javaDirectory, source.getName());
        copy(source, destination);
      }
    }
  }


  private void copyClassPathEntries(File javaDirectory) throws IOException {
    for (FileSet fileSet : classPath) {
      File classPathDirectory = fileSet.getDir();
      DirectoryScanner directoryScanner = fileSet.getDirectoryScanner(getProject());
      String[] includedFiles = directoryScanner.getIncludedFiles();

      for (int i = 0; i < includedFiles.length; i++) {
        String includedFile = includedFiles[i];
        File source = new File(classPathDirectory, includedFile);
        File destination = new File(javaDirectory, new File(includedFile).getName());
        copy(source, destination);
      }
    }
  }


  private void copyLibraryPathEntries(File macOSDirectory) throws IOException {
    for (FileSet fileSet : libraryPath) {
      File libraryPathDirectory = fileSet.getDir();
      DirectoryScanner directoryScanner = fileSet.getDirectoryScanner(getProject());
      String[] includedFiles = directoryScanner.getIncludedFiles();

      for (int i = 0; i < includedFiles.length; i++) {
        String includedFile = includedFiles[i];
        File source = new File(libraryPathDirectory, includedFile);
        File destination = new File(macOSDirectory, new File(includedFile).getName());
        copy(source, destination);
      }
    }
  }


  private void copyIcon(File resourcesDirectory) throws IOException {
    if (icon == null) {
      copy(getClass().getResource(DEFAULT_ICON_NAME), new File(resourcesDirectory, DEFAULT_ICON_NAME));
    } else {
      copy(icon, new File(resourcesDirectory, icon.getName()));
    }
  }


  class PropertyLister {
    PrintWriter writer;
    int indentSpaces = 2;
//    int indentLevel;
    Stack<String> elements = new Stack<>();
    
    public PropertyLister(OutputStream output) throws UnsupportedEncodingException {
      OutputStreamWriter osw = new OutputStreamWriter(output, "UTF-8");
      writer = new PrintWriter(osw);
    }
    
    void writeStartDocument() {
      writer.println("<?xml version=\"1.0\" ?>");
    }
    
    void writeEndDocument() {
      writer.flush();
      writer.close();
    }
    
    void println(String line) {
      writer.println(line);
    }
    
    void writeStartElement(String element, String... args) {
      emitIndent();
      writer.print("<" + element);
      
      for (int i = 0; i < args.length; i += 2) {
        String attr = args[i];
        String value = args[i+1];
        writer.print(" " + attr + "=\"" + value + "\"");
      }
      writer.println(">");
//      indentLevel++;
      elements.push(element);
    }
    
    void writeStartElement(String element) {
      writer.println("<" + element + ">");
//      indentLevel++;
      elements.push(element);
    }
    
    void writeEndElement() {
      emitIndent();
      writer.println("</" + elements.pop() + ">");
    }
    
    void writeSingle(String tag, String content) {
      emitIndent();
      writer.println("<" + tag + ">" + content + "</" + tag + ">");
    }
    
    void writeKey(String key) {
      writeSingle(KEY_TAG, key);
//      emitIndent();
//      writer.println("<key>" + key + "</key>");
    }
    
    void writeString(String value) {
//      emitIndent();
//      writer.println("<string>" + value + "</string>");
      writeSingle(STRING_TAG, value);
    }
    
    void writeBoolean(boolean value) {
      emitIndent();
      writer.println("<" + (value ? "true" : "false") + "/>");
    }
    
    void writeProperty(String property, String value) {
      writeKey(property);
      writeString(value);
    }
    
    void emitIndent() {
      for (int i = 0; i < elements.size(); i++) {
        for (int j = 0; j < indentSpaces; j++) {
          writer.print(' ');
        }
      }
    }
  }
  
  
  private void writeInfoPlist(File file) throws IOException {
    FileOutputStream output = new FileOutputStream(file);
    PropertyLister xout = new PropertyLister(output);

    xout.println(PLIST_DTD);

    // Begin root element
    xout.writeStartElement(PLIST_TAG, PLIST_VERSION_ATTRIBUTE, "1.0");

    // Begin root dictionary
    xout.writeStartElement(DICT_TAG);

    // Write bundle properties
    xout.writeProperty("CFBundleDevelopmentRegion", "English");
    xout.writeProperty("CFBundleExecutable", executableName);
    xout.writeProperty("CFBundleIconFile", (icon == null) ? DEFAULT_ICON_NAME : icon.getName());
    xout.writeProperty("CFBundleIdentifier", identifier);
    xout.writeProperty("CFBundleDisplayName", displayName);
    xout.writeProperty("CFBundleInfoDictionaryVersion", "6.0");
    xout.writeProperty("CFBundleName", name);
    xout.writeProperty("CFBundlePackageType", OS_TYPE_CODE);
    xout.writeProperty("CFBundleShortVersionString", shortVersion);
    xout.writeProperty("CFBundleVersion", version);
    xout.writeProperty("CFBundleSignature", signature);
    xout.writeProperty("NSHumanReadableCopyright", copyright);

    if (applicationCategory != null) {
      xout.writeProperty("LSApplicationCategoryType", applicationCategory);
    }

    if (highResolutionCapable) {
      xout.writeKey("NSHighResolutionCapable");
      xout.writeBoolean(true);
    }

    // Write runtime
    if (runtime != null) {
      xout.writeProperty("JVMRuntime", runtime.getDir().getParentFile().getParentFile().getName());
    }

    if (privileged != null) {
      xout.writeProperty("JVMRunPrivileged", privileged);
    }

    if (workingDirectory != null) {
      xout.writeProperty("WorkingDirectory", workingDirectory);
    }

    // Write main class name
    xout.writeProperty("JVMMainClassName", mainClassName);


    // Write CFBundleDocument entries
    xout.writeKey("CFBundleDocumentTypes");

    xout.writeStartElement(ARRAY_TAG);

    for (BundleDocument bundleDocument: bundleDocuments) {
      xout.writeStartElement(DICT_TAG);

      xout.writeKey("CFBundleTypeExtensions");
      xout.writeStartElement(ARRAY_TAG);
      for (String extension : bundleDocument.getExtensions()) {
        xout.writeString(extension);
      }
      xout.writeEndElement();

      if (bundleDocument.hasIcon()) {
        xout.writeKey("CFBundleTypeIconFile");
        xout.writeString(bundleDocument.getIcon());
      }

      xout.writeKey("CFBundleTypeName");
      xout.writeString(bundleDocument.getName());

      xout.writeKey("CFBundleTypeRole");
      xout.writeString(bundleDocument.getRole());

      xout.writeKey("LSTypeIsPackage");
      xout.writeBoolean(bundleDocument.isPackage());

      xout.writeEndElement();
    }

    xout.writeEndElement();

    // Write architectures
    xout.writeKey("LSArchitecturePriority");

    xout.writeStartElement(ARRAY_TAG);

    for (String architecture : architectures) {
      xout.writeString(architecture);
    }

    xout.writeEndElement();

    // Write Environment
    xout.writeKey("LSEnvironment");
    xout.writeStartElement(DICT_TAG);
    xout.writeKey("LC_CTYPE");
    xout.writeString("UTF-8");
    xout.writeEndElement();

    // Write options
    xout.writeKey("JVMOptions");

    xout.writeStartElement(ARRAY_TAG);

    for (String option : options) {
      xout.writeString(option);
    }

    xout.writeEndElement();

    // Write arguments
    xout.writeKey("JVMArguments");

    xout.writeStartElement(ARRAY_TAG);

    for (String argument : arguments) {
      xout.writeString(argument);
    }

    xout.writeEndElement();

    // End root dictionary
    xout.writeEndElement();

    // End root element
    xout.writeEndElement();

    // Close document
    xout.writeEndDocument();
  }


  private void writePkgInfo(File file) throws IOException {
    Writer out = new BufferedWriter(new FileWriter(file));

    try {
      out.write(OS_TYPE_CODE + signature);
      out.flush();
    } finally {
      out.close();
    }
  }


  private static void delete(File file) throws IOException {
    Path filePath = file.toPath();

    if (Files.exists(filePath, LinkOption.NOFOLLOW_LINKS)) {
      if (Files.isDirectory(filePath, LinkOption.NOFOLLOW_LINKS)) {
        File[] files = file.listFiles();

        for (int i = 0; i < files.length; i++) {
          delete(files[i]);
        }
      }

      Files.delete(filePath);
    }
  }


  private static void copy(URL location, File file) throws IOException {
    try (InputStream in = location.openStream()) {
      Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }


  private static void copy(File source, File destination) throws IOException {
    Path sourcePath = source.toPath();
    Path destinationPath = destination.toPath();

    destination.getParentFile().mkdirs();

    Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);

    if (Files.isDirectory(sourcePath, LinkOption.NOFOLLOW_LINKS)) {
      String[] files = source.list();

      for (int i = 0; i < files.length; i++) {
        String file = files[i];
        copy(new File(source, file), new File(destination, file));
      }
    }
  }
}
