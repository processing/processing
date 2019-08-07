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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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
    private String minimumSystemVersion = null;

    private String jvmRequired = null;
    private boolean jrePreferred = false;
    private boolean jdkPreferred = false;

    private String applicationCategory = null;

    private boolean highResolutionCapable = true;
    private boolean supportsAutomaticGraphicsSwitching = true;
    private boolean hideDockIcon = false;
    private boolean isDebug = false;
    private boolean ignorePSN = false;

    // JVM info properties
    private String mainClassName = null;
    private String jnlpLauncherName = null;
    private String jarLauncherName = null;
    private Runtime runtime = null;
    private ArrayList<FileSet> classPath = new ArrayList<>();
    private ArrayList<FileSet> libraryPath = new ArrayList<>();
    private ArrayList<Option> options = new ArrayList<>();
    private ArrayList<String> arguments = new ArrayList<>();
    private ArrayList<String> architectures = new ArrayList<>();
    private ArrayList<String> registeredProtocols = new ArrayList<>();
    private ArrayList<BundleDocument> bundleDocuments = new ArrayList<>();
    private ArrayList<TypeDeclaration> exportedTypeDeclarations = new ArrayList<>();
    private ArrayList<TypeDeclaration> importedTypeDeclarations = new ArrayList<>();
    private ArrayList<PlistEntry> plistEntries = new ArrayList<>();
    private ArrayList<Environment> environments = new ArrayList<>();

    private Reference classPathRef;
    private ArrayList<String> plistClassPaths = new ArrayList<>();

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

    public void setJVMRequired(String v){
        this.jvmRequired = v;
    }

    public void setJREPreferred(boolean preferred){
        this.jrePreferred = preferred;
    }

    public void setJDKPreferred(boolean preferred){
        this.jdkPreferred = preferred;
    }

    public void setMinimumSystemVersion(String v){
        this.minimumSystemVersion = v;
    }

    public void setApplicationCategory(String applicationCategory) {
        this.applicationCategory = applicationCategory;
    }

    public void setHighResolutionCapable(boolean highResolutionCapable) {
        this.highResolutionCapable = highResolutionCapable;
    }

    public void setHideDockIcon(boolean hideDock) {
        this.hideDockIcon = hideDock;
    }

    public void setDebug(boolean enabled) {
        this.isDebug = enabled;
    }

    public void setSupportsAutomaticGraphicsSwitching(boolean supportsAutomaticGraphicsSwitching) {
        this.supportsAutomaticGraphicsSwitching = supportsAutomaticGraphicsSwitching;
    }

    public void setIgnorePSN(boolean ignorePSN) {
        this.ignorePSN = ignorePSN;
    }

    public void setMainClassName(String mainClassName) {
        this.mainClassName = mainClassName;
    }

    public void setJnlpLauncherName(String jnlpLauncherName) {
        this.jnlpLauncherName = jnlpLauncherName;
    }

    public void setJarLauncherName(String jarLauncherName) {
        this.jarLauncherName = jarLauncherName;
    }

    public void addConfiguredRuntime(Runtime runtime) throws BuildException {
        if (this.runtime != null) {
            throw new BuildException("Runtime already specified.");
        }

        this.runtime = runtime;
    }

    public void setClasspathRef(Reference ref) {

        this.classPathRef = ref;
    }

    public void setPlistClassPaths(String plistClassPaths) {
        for (String tok : plistClassPaths.split("\\s*,\\s*")) {
            this.plistClassPaths.add(tok);
        }
    }

    public void addConfiguredClassPath(FileSet classPath) {
        this.classPath.add(classPath);
    }

    public void addConfiguredLibraryPath(FileSet libraryPath) {
        this.libraryPath.add(libraryPath);
    }

    public void addConfiguredBundleDocument(BundleDocument document) {
        if ((document.getContentTypes() == null) && (document.getExtensions() == null)) {
            throw new BuildException("Document content type or extension is required.");
        }
        this.bundleDocuments.add(document);
    }

    public void addConfiguredTypeDeclaration(TypeDeclaration typeDeclaration) {
        if (typeDeclaration.getIdentifier() == null) {
            throw new BuildException("Type declarations must have an identifier.");
        }
        if (typeDeclaration.isImported()) {
            this.importedTypeDeclarations.add(typeDeclaration);
        } else {
            this.exportedTypeDeclarations.add(typeDeclaration);
        }
    }

    public void addConfiguredPlistEntry(PlistEntry plistEntry) {
        if (plistEntry.getKey() == null) {
            throw new BuildException("Name is required.");
        }
        if (plistEntry.getValue() == null) {
            throw new BuildException("Value is required.");
        }
        if (plistEntry.getType() == null) {
            plistEntry.setType(STRING_TAG);
        }

        this.plistEntries.add(plistEntry);
    }

    public void addConfiguredEnvironment(Environment environment) {
        if (environment.getName() == null) {
            throw new BuildException("Name is required.");
        }
        if (environment.getValue() == null) {
            throw new BuildException("Value is required.");
        }

        this.environments.add(environment);
    }

    public void addConfiguredOption(Option option) throws BuildException {
        String value = option.getValue();

        if (value == null) {
            throw new BuildException("Value is required.");
        }

        options.add(option);
    }

    public void addConfiguredArgument(Argument argument) throws BuildException {
        String value = argument.getValue();

        if (value == null) {
            throw new BuildException("Value is required.");
        }

        arguments.add(value);
    }
    public void addConfiguredScheme(Argument argument) throws BuildException {
        String value = argument.getValue();

        if (value == null) {
            throw new BuildException("Value is required.");
        }

        this.registeredProtocols.add(value);
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

        if (jnlpLauncherName == null && mainClassName == null) {
            throw new IllegalStateException("Main class name or JNLP launcher name is required.");
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

            // Copy app icon to Resources folder
            copyIcon(resourcesDirectory);

            // Copy app document icons to Resources folder
            copyDocumentIcons(bundleDocuments, resourcesDirectory);
            copyDocumentIcons(exportedTypeDeclarations, resourcesDirectory);
            copyDocumentIcons(importedTypeDeclarations, resourcesDirectory);

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
            runtime.copyTo(plugInsDirectory);
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

    public void copyDocumentIcons(final ArrayList<? extends IconContainer> iconContainers,
            File resourcesDirectory) throws IOException {
        for(IconContainer iconContainer: iconContainers) {
            if(iconContainer.hasIcon()) {
                File ifile = iconContainer.getIconFile();
                if (ifile != null) {
                    copyDocumentIcon(ifile,resourcesDirectory);
                }
            }
        }
    }

    private void copyDocumentIcon(File ifile, File resourcesDirectory) throws IOException {
        if (ifile == null) {
            return;
        } else {
            copy(ifile, new File(resourcesDirectory, ifile.getName()));
        }
    }

    private void writeInfoPlist(File file) throws IOException {
        Writer out = new BufferedWriter(new FileWriter(file));
        XMLOutputFactory output = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter xout = output.createXMLStreamWriter(out);

            // Write XML declaration
            xout.writeStartDocument();
            xout.writeCharacters("\n");

            // Write plist DTD declaration
            xout.writeDTD(PLIST_DTD);
            xout.writeCharacters("\n");

            // Begin root element
            xout.writeStartElement(PLIST_TAG);
            xout.writeAttribute(PLIST_VERSION_ATTRIBUTE, "1.0");
            xout.writeCharacters("\n");

            // Begin root dictionary
            xout.writeStartElement(DICT_TAG);
            xout.writeCharacters("\n");

            // Write bundle properties
            writeProperty(xout, "CFBundleDevelopmentRegion", "English");
            writeProperty(xout, "CFBundleExecutable", executableName);
            writeProperty(xout, "CFBundleIconFile", (icon == null) ? DEFAULT_ICON_NAME : icon.getName());
            writeProperty(xout, "CFBundleIdentifier", identifier);
            writeProperty(xout, "CFBundleDisplayName", displayName);
            writeProperty(xout, "CFBundleInfoDictionaryVersion", "6.0");
            writeProperty(xout, "CFBundleName", name);
            writeProperty(xout, "CFBundlePackageType", OS_TYPE_CODE);
            writeProperty(xout, "CFBundleShortVersionString", shortVersion);
            writeProperty(xout, "CFBundleVersion", version);
            writeProperty(xout, "CFBundleSignature", signature);
            writeProperty(xout, "NSHumanReadableCopyright", copyright);
            writeProperty(xout, "LSMinimumSystemVersion", minimumSystemVersion);
            writeProperty(xout, "LSApplicationCategoryType", applicationCategory);
            writeProperty(xout, "LSUIElement",hideDockIcon);
            writeProperty(xout, "NSHighResolutionCapable",highResolutionCapable);
            writeProperty(xout, "NSSupportsAutomaticGraphicsSwitching",
                        supportsAutomaticGraphicsSwitching);
            writeProperty(xout, "IgnorePSN",ignorePSN);

            if(registeredProtocols.size() > 0){
                writeKey(xout, "CFBundleURLTypes");
                xout.writeStartElement(ARRAY_TAG);
                xout.writeCharacters("\n");
                xout.writeStartElement(DICT_TAG);
                xout.writeCharacters("\n");

                writeProperty(xout, "CFBundleURLName", identifier);
                writeStringArray(xout, "CFBundleURLSchemes",registeredProtocols);

                xout.writeEndElement();
                xout.writeCharacters("\n");
                xout.writeEndElement();
                xout.writeCharacters("\n");
            }

            // Write runtime
            if (runtime != null) {
                writeProperty(xout, "JVMRuntime", runtime.getDir().getParentFile().getParentFile().getName());
            }

            if(jvmRequired != null) {
                writeProperty(xout, "JVMVersion", jvmRequired);
            }

            writeProperty(xout, "JVMRunPrivileged", privileged);

            writeProperty(xout, "JREPreferred", jrePreferred);
            writeProperty(xout, "JDKPreferred", jdkPreferred);

            writeProperty(xout, "WorkingDirectory", workingDirectory);

            // Write jnlp launcher name - only if set
            writeProperty(xout, "JVMJNLPLauncher", jnlpLauncherName);

            // Write main class name - only if set. There should only one be set
            writeProperty(xout, "JVMMainClassName", mainClassName);

           // Write classpaths in plist, if specified
            if (!plistClassPaths.isEmpty()) {
                writeStringArray(xout,"JVMClassPath", plistClassPaths);
            }

            // Write whether launcher be verbose with debug msgs
            writeProperty(xout, "JVMDebug", isDebug);

            // Write jar launcher name
            writeProperty(xout, "JVMJARLauncher", jarLauncherName);

            // Write CFBundleDocument entries
            writeKey(xout, "CFBundleDocumentTypes");
            writeBundleDocuments(xout, bundleDocuments);

            // Write Type Declarations
            if (! exportedTypeDeclarations.isEmpty()) {
                writeKey(xout, "UTExportedTypeDeclarations");
                writeTypeDeclarations(xout, exportedTypeDeclarations);
            }
            if (! importedTypeDeclarations.isEmpty()) {
                writeKey(xout, "UTImportedTypeDeclarations");
                writeTypeDeclarations(xout, importedTypeDeclarations);
            }

            // Write architectures
            writeStringArray(xout, "LSArchitecturePriority",architectures);

            // Write Environment
            writeKey(xout, "LSEnvironment");
            xout.writeStartElement(DICT_TAG);
            xout.writeCharacters("\n");
            writeKey(xout, "LC_CTYPE");
            writeString(xout, "UTF-8");

            for (Environment environment : environments) {
                writeProperty(xout, environment.getName(), environment.getValue());
            }

            xout.writeEndElement();
            xout.writeCharacters("\n");

            // Write options
            writeKey(xout, "JVMOptions");

            xout.writeStartElement(ARRAY_TAG);
            xout.writeCharacters("\n");

            for (Option option : options) {
                if (option.getName() == null) writeString(xout, option.getValue());
            }

            xout.writeEndElement();
            xout.writeCharacters("\n");

            // Write default options
            writeKey(xout, "JVMDefaultOptions");

            xout.writeStartElement(DICT_TAG);
            xout.writeCharacters("\n");

            for (Option option : options) {
                if (option.getName() != null) {
                    writeProperty(xout, option.getName(), option.getValue());
                }
            }

            xout.writeEndElement();
            xout.writeCharacters("\n");

            // Write arguments
            writeStringArray(xout, "JVMArguments",arguments);

            // Write arbitrary key-value pairs
            for (PlistEntry item : plistEntries) {
                writeKey(xout, item.getKey());
                writeValue(xout, item.getType(), item.getValue());
            }

            // End root dictionary
            xout.writeEndElement();
            xout.writeCharacters("\n");

            // End root element
            xout.writeEndElement();
            xout.writeCharacters("\n");

            // Close document
            xout.writeEndDocument();
            xout.writeCharacters("\n");

            out.flush();
        } catch (XMLStreamException exception) {
            throw new IOException(exception);
        } finally {
            out.close();
        }
    }

    private void writeKey(XMLStreamWriter xout, String key) throws XMLStreamException {
        xout.writeStartElement(KEY_TAG);
        xout.writeCharacters(key);
        xout.writeEndElement();
        xout.writeCharacters("\n");
    }

    private void writeValue(XMLStreamWriter xout, String type, String value) throws XMLStreamException {
        if (type == null) {
            type = STRING_TAG;
        }
        if ("boolean".equals(type)) {
            writeBoolean(xout, "true".equals(value));
        } else {
            xout.writeStartElement(type);
            xout.writeCharacters(value);
            xout.writeEndElement();
            xout.writeCharacters("\n");
        }
    }

    private void writeString(XMLStreamWriter xout, String value) throws XMLStreamException {
        xout.writeStartElement(STRING_TAG);
        xout.writeCharacters(value);
        xout.writeEndElement();
        xout.writeCharacters("\n");
    }

    private void writeBoolean(XMLStreamWriter xout, boolean value) throws XMLStreamException {
        xout.writeEmptyElement(value ? "true" : "false");
        xout.writeCharacters("\n");
    }

    private void writeProperty(XMLStreamWriter xout, String key, Boolean value) throws XMLStreamException {
        if (value != null && value) {
            writeKey(xout, key);
            writeBoolean(xout, true);
        }
    }

    private void writeProperty(XMLStreamWriter xout, String key, Object value) throws XMLStreamException {
        if (value != null) {
            writeKey(xout, key);
            writeString(xout, value.toString());
        }
    }

    public void writeStringArray(XMLStreamWriter xout, final String key,
            final Iterable<String> values) throws XMLStreamException {
        if (values != null) {
            writeKey(xout, key);
            xout.writeStartElement(ARRAY_TAG);
            xout.writeCharacters("\n");
            for(String singleValue : values) {
                writeString(xout, singleValue);
            }
            xout.writeEndElement();
            xout.writeCharacters("\n");
        }
    }

    public void writeBundleDocuments(XMLStreamWriter xout,
            final ArrayList<BundleDocument> bundleDocuments) throws XMLStreamException {

        xout.writeStartElement(ARRAY_TAG);
        xout.writeCharacters("\n");

        for(BundleDocument bundleDocument: bundleDocuments) {
            xout.writeStartElement(DICT_TAG);
            xout.writeCharacters("\n");

            final List<String> contentTypes = bundleDocument.getContentTypes();
            if (contentTypes != null) {
                writeStringArray(xout, "LSItemContentTypes", contentTypes);
            } else {
                writeStringArray(xout, "CFBundleTypeExtensions", bundleDocument.getExtensions());
                writeProperty(xout, "LSTypeIsPackage", bundleDocument.isPackage());
            }
            writeStringArray(xout, "NSExportableTypes", bundleDocument.getExportableTypes());

            final File ifile = bundleDocument.getIconFile();
            writeProperty(xout, "CFBundleTypeIconFile", ifile != null ?
                        ifile.getName() : bundleDocument.getIcon());

            writeProperty(xout, "CFBundleTypeName", bundleDocument.getName());
            writeProperty(xout, "CFBundleTypeRole", bundleDocument.getRole());
            writeProperty(xout, "LSHandlerRank", bundleDocument.getHandlerRank());

            xout.writeEndElement();
            xout.writeCharacters("\n");
        }

        xout.writeEndElement();
        xout.writeCharacters("\n");
    }

    public void writeTypeDeclarations(XMLStreamWriter xout,
            final ArrayList<TypeDeclaration> typeDeclarations) throws XMLStreamException {
        xout.writeStartElement(ARRAY_TAG);
        xout.writeCharacters("\n");
        for (TypeDeclaration typeDeclaration: typeDeclarations) {

            xout.writeStartElement(DICT_TAG);
            xout.writeCharacters("\n");

            writeProperty(xout, "UTTypeIdentifier", typeDeclaration.getIdentifier());
            writeProperty(xout, "UTTypeReferenceURL", typeDeclaration.getReferenceUrl());
            writeProperty(xout, "UTTypeDescription", typeDeclaration.getDescription());

            final File ifile = typeDeclaration.getIconFile();
            writeProperty(xout, "UTTypeIconFile", ifile != null ?
                        ifile.getName() : typeDeclaration.getIcon());

            writeStringArray(xout, "UTTypeConformsTo", typeDeclaration.getConformsTo());

            writeKey(xout, "UTTypeTagSpecification");

            xout.writeStartElement(DICT_TAG);
            xout.writeCharacters("\n");

            writeStringArray(xout, "com.apple.ostype", typeDeclaration.getOsTypes());
            writeStringArray(xout, "public.filename-extension", typeDeclaration.getExtensions());
            writeStringArray(xout, "public.mime-type", typeDeclaration.getMimeTypes());

            xout.writeEndElement();
            xout.writeCharacters("\n");

            xout.writeEndElement();
            xout.writeCharacters("\n");
        }

        xout.writeEndElement();
        xout.writeCharacters("\n");
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
        catch (Exception exc)
        {
        System.err.println ("Trying to copy " + location + " to " + file);
            throw exc;
        }
    }

    static void copy(File source, File destination) throws IOException {
        Path sourcePath = source.toPath();
        Path destinationPath = destination.toPath();

        destination.getParentFile().mkdirs();

        Files.copy(sourcePath, destinationPath,
               StandardCopyOption.REPLACE_EXISTING,
               StandardCopyOption.COPY_ATTRIBUTES,
               LinkOption.NOFOLLOW_LINKS);

        if (Files.isDirectory(sourcePath)) {
            String[] files = source.list();

            for (String file : files) {
              copy(new File(source, file), new File(destination, file));
            }
        }
    }
}
