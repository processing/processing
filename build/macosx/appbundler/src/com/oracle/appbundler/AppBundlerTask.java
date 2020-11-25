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
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
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
  private File iconFile = null;
  private String executableName = EXECUTABLE_NAME;

  private String shortVersion = null; //"1.0";
  private String version = null; //"1.0";
  private String signature = "????";
  private String copyright = null; //"";
  private String getInfo = null;
  private String privileged = null;
  private String workingDirectory = null;

  private String applicationCategory = null;
  private boolean highResolutionCapable = true;
  // Oracle Java 8 requires 10.8.3 or later, so require it here.
  private String minimumSystem = "10.8.3";
  // By default, don't embed Java FX.
  private boolean javafx = false;

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
    this.iconFile = icon;
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


  public void setGetInfo(String getInfo) {
    this.getInfo = getInfo;
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


  public void setMinimumSystem(String minimumSystem) {
    this.minimumSystem = minimumSystem;
  }


  public void setHighResolutionCapable(boolean highResolutionCapable) {
    this.highResolutionCapable = highResolutionCapable;
  }


  public void setJavaFX(boolean javafx) {
    this.javafx = javafx;
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

      // original version, removed entire bin folder
      //"jre/bin/",

      // remove everything except 'java'
      // also keep 'keytool' (needed by Android)
      "jre/bin/orbd",
      "jre/bin/pack200",
      "jre/bin/policytool",
      "jre/bin/rmid",
      "jre/bin/rmiregistry",
      "jre/bin/servertool",
      "jre/bin/tnameserv",
      "jre/bin/unpack200",

      "jre/lib/deploy/",
      "jre/lib/deploy.jar",
      "jre/lib/javaws.jar",
      "jre/lib/libdeploy.dylib",
      "jre/lib/libnpjp2.dylib",
      "jre/lib/plugin.jar",
      "jre/lib/security/javaws.policy"
    });

    if (!javafx) {
      // http://www.oracle.com/technetwork/java/javase/jdk-7-readme-429198.html
      runtime.appendExcludes(new String[] {
        "jre/THIRDPARTYLICENSEREADME-JAVAFX.txt",

        "jre/lib/javafx.properties",
        "jre/lib/jfxrt.jar",
        "jre/lib/security/javafx.policy",

        "jre/lib/fxplugins.dylib",
        "jre/lib/libdecora-sse.dylib",
        "jre/lib/libglass.dylib",
        "jre/lib/libglib-2.0.0.dylib",
        "jre/lib/libgstplugins-lite.dylib",
        "jre/lib/libgstreamer-lite.dylib",
        "jre/lib/libjavafx-font.dylib",
        "jre/lib/libjavafx-iio.dylib",
        "jre/lib/libjfxmedia.dylib",
        "jre/lib/libjfxwebkit.dylib",
        "jre/lib/libprism-es2.dylib"
      });
    }
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

    if (iconFile != null) {
      if (!iconFile.exists()) {
        throw new IllegalStateException("Icon does not exist.");
      }

      if (iconFile.isDirectory()) {
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

//      // Move back to Contents/Resources/Java instead of Contents/Java [fry]
//      File javaDirectory = new File(resourcesDirectory, "Java");
//      javaDirectory.mkdir();

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

      // Copy the bundle/document icons as well
      copyBundleIcons(resourcesDirectory);

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
          OutputStream outputStream =
            new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE);
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
          file.setLastModified(zipEntry.getTime());
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

      for (String includedFile : includedFiles) {
      //for (int i = 0; i < includedFiles.length; i++) {
        //String includedFile = includedFiles[i];
        File source = new File(runtimeHomeDirectory, includedFile);
        File destination = new File(pluginHomeDirectory, includedFile);
        copy(source, destination);
      }
    }
  }


  private void copyClassPathRefEntries(File javaDirectory) throws IOException {
    if (classPathRef != null) {
      org.apache.tools.ant.types.Path classpath =
        (org.apache.tools.ant.types.Path) classPathRef.getReferencedObject(getProject());

      Iterator<?> iter = classpath.iterator();
      while (iter.hasNext()) {
        FileResource resource = (FileResource) iter.next();
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

      for (String includedFile : includedFiles) {
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

      for (String includedFile : includedFiles) {
        File source = new File(libraryPathDirectory, includedFile);
        File destination = new File(macOSDirectory, new File(includedFile).getName());
        copy(source, destination);
      }
    }
  }


  private void copyIcon(File resourcesDirectory) throws IOException {
    if (iconFile == null) {
      copy(getClass().getResource(DEFAULT_ICON_NAME),
           new File(resourcesDirectory, DEFAULT_ICON_NAME));
    } else {
      copy(iconFile, new File(resourcesDirectory, iconFile.getName()));
    }
  }


  private void copyBundleIcons(File resourcesDirectory) throws IOException {
    for (BundleDocument bundleDocument : bundleDocuments) {
      if (bundleDocument.hasIcon()) {
        File iconFile = bundleDocument.getIconFile();
        copy(iconFile, new File(resourcesDirectory, iconFile.getName()));
      }
    }
  }


  private void writeInfoPlist(File file) throws IOException {
    FileOutputStream output = new FileOutputStream(file);
    PropertyLister plist = new PropertyLister(output);

    // Get started, write all necessary header info and open plist element
    plist.writeStartDocument();

    // Begin root dictionary
    plist.writeStartDictElement();

    // Write bundle properties
    plist.writeProperty("CFBundleDevelopmentRegion", "English");
    plist.writeProperty("CFBundleExecutable", executableName);
    plist.writeProperty("CFBundleIconFile", (iconFile == null) ? DEFAULT_ICON_NAME : iconFile.getName());
    plist.writeProperty("CFBundleIdentifier", identifier);
    plist.writeProperty("CFBundleDisplayName", displayName);
    plist.writeProperty("CFBundleInfoDictionaryVersion", "6.0");
    plist.writeProperty("CFBundleName", name);
    plist.writeProperty("CFBundlePackageType", OS_TYPE_CODE);
    plist.writeProperty("CFBundleShortVersionString", shortVersion);
    plist.writeProperty("CFBundleVersion", version);
    plist.writeProperty("CFBundleSignature", signature);
    plist.writeProperty("NSHumanReadableCopyright", copyright);

    if (getInfo != null) {
      plist.writeProperty("CFBundleGetInfoString", getInfo);
    }

    if (applicationCategory != null) {
      plist.writeProperty("LSApplicationCategoryType", applicationCategory);
    }

    if (minimumSystem != null) {
      plist.writeProperty("LSMinimumSystemVersion", minimumSystem);
    }

    if (highResolutionCapable) {
      plist.writeKey("NSHighResolutionCapable");
      plist.writeBoolean(true);
    }

    if (runtime != null) {
      plist.writeProperty("JVMRuntime", runtime.getDir().getParentFile().getParentFile().getName());
    }

    if (privileged != null) {
      plist.writeProperty("JVMRunPrivileged", privileged);
    }

    if (workingDirectory != null) {
      plist.writeProperty("WorkingDirectory", workingDirectory);
    }

    // Write main class name
    plist.writeProperty("JVMMainClassName", mainClassName);

    // Write CFBundleDocument entries
    plist.writeKey("CFBundleDocumentTypes");
    plist.writeStartArrayElement();
    for (BundleDocument bundleDocument: bundleDocuments) {
      plist.writeStartDictElement();

      plist.writeKey("CFBundleTypeExtensions");
      plist.writeStartArrayElement();
      for (String extension : bundleDocument.getExtensions()) {
        plist.writeString(extension);
      }
      plist.writeEndElement();

      if (bundleDocument.hasIcon()) {
        plist.writeKey("CFBundleTypeIconFile");
        plist.writeString(bundleDocument.getIconName());
      }

      plist.writeKey("CFBundleTypeName");
      plist.writeString(bundleDocument.getName());

      plist.writeKey("CFBundleTypeRole");
      plist.writeString(bundleDocument.getRole());

      plist.writeKey("LSTypeIsPackage");
      plist.writeBoolean(bundleDocument.isPackage());

      plist.writeEndElement();
    }
    plist.writeEndElement();

    // Write architectures
    plist.writeKey("LSArchitecturePriority");
    plist.writeStartArrayElement();
    for (String architecture : architectures) {
      plist.writeString(architecture);
    }
    plist.writeEndElement();

    // Write Environment
    plist.writeKey("LSEnvironment");
    plist.writeStartDictElement();
    plist.writeKey("LC_CTYPE");
    plist.writeString("UTF-8");
    plist.writeEndElement();

    // Write options
    plist.writeKey("JVMOptions");
    plist.writeStartArrayElement();
    for (String option : options) {
      plist.writeString(option);
    }
    plist.writeEndElement();

    // Write arguments
    plist.writeKey("JVMArguments");
    plist.writeStartArrayElement();
    for (String argument : arguments) {
      plist.writeString(argument);
    }
    plist.writeEndElement();

    // End root dictionary
    plist.writeEndElement();

    // Close out the plist
    plist.writeEndDocument();
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
      Files.copy(in, file.toPath(),
                 // can't do attributes when coming from URL
                 StandardCopyOption.REPLACE_EXISTING);
    }
  }


  private static void copy(File source, File destination) throws IOException {
    Path sourcePath = source.toPath();
    Path destinationPath = destination.toPath();

    destination.getParentFile().mkdirs();

    Files.copy(sourcePath, destinationPath,
               StandardCopyOption.REPLACE_EXISTING,
               StandardCopyOption.COPY_ATTRIBUTES,
               LinkOption.NOFOLLOW_LINKS);

    if (Files.isDirectory(sourcePath, LinkOption.NOFOLLOW_LINKS)) {
      String[] files = source.list();

      for (String file : files) {
        copy(new File(source, file), new File(destination, file));
      }
    }
  }
}
