/**
 * Copyright (c) 2010 Chris Lonnen. All rights reserved.
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * Contributors:
 *     Chris Lonnen - initial API and implementation
 */
package processing.plugin.core.model;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import processing.core.PConstants;
import processing.plugin.core.ProcessingLog;
import processing.plugin.core.ProcessingUtilities;

/** 
 * Provides a model of a Processing library folder for easy extraction of
 * files required for different exports on different platforms.
 * <p> 
 * This is a rather unimaginative rewrite of the Processing.app.LibraryFolder 
 * class to work with the LibraryModel instead of Processing.app.Base.
 */
public class LibraryFolder implements PConstants {

	File folder;		  // root folder
	File libraryFolder;   // name/library
	File examplesFolder;  // name/examples
	String name;          // "pdf" or "PDF Export"
	String author;        // Ben Fry
	String authorURL;     // http://processing.org
	String sentence;      // Write graphics to PDF files.
	String paragraph;     // <paragraph length description for site>
	int version;          // 102
	String prettyVersion; // "1.0.2"

	HashMap<String,String[]> exportList;
	String[] appletExportList;
	boolean[] multipleArch = new boolean[platformNames.length];

	/**
	 * For runtime, the native library path for this platform. e.g. on Windows 64, 
	 * this might be the windows64 subfolder with the library.
	 */
	String nativeLibraryPath;

	/** How many bits this machine is */
	static int nativeBits;
	static {
		nativeBits = 32;  // perhaps start with 32
		String bits = System.getProperty("sun.arch.data.model");
		if (bits != null) {
			if (bits.equals("64")) {
				nativeBits = 64;
			}
		} else {
			// if some other strange vm, maybe try this instead
			if (System.getProperty("java.vm.name").contains("64")) {
				nativeBits = 64;
			}
		}
	}  

	/** Filter to pull out just files and no directories */
	FilenameFilter simpleFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			// skip .DS_Store files, .svn folders, etc
			if (name.charAt(0) == '.') return false;
			if (name.equals("CVS")) return false;
			File file = new File(dir, name); 
			return (!file.isDirectory());
		}
	};

	/** Filter to pull out just jars*/
	FilenameFilter jarFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			if (name.charAt(0) == '.') return false;  // skip ._blah.jar crap on OS X
			if (new File(dir, name).isDirectory()) return false;
			String lc = name.toLowerCase();
			return lc.endsWith(".jar") || lc.endsWith(".zip"); 
		}
	};

	/** Returns an ArrayList of LibraryFolders for each valid library in the folder. */
	static protected ArrayList<LibraryFolder> list(File folder) throws IOException {
		ArrayList<LibraryFolder> libraries = new ArrayList<LibraryFolder>();
		list(folder, libraries);
		return libraries;
	}

	/** Loads the provided ArrayList with the libraries in the directory. */
	static protected void list(File folder, ArrayList<LibraryFolder> libraries) throws IOException {
		if (folder == null) return;
		if (folder.isDirectory()) {
			String[] list = folder.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					// skip .DS_Store files, .svn folders, etc
					if (name.charAt(0) == '.') return false;
					if (name.equals("CVS")) return false;
					return (new File(dir, name).isDirectory());
				}
			});
			// if a bad folder or something like that, this might come back null
			if (list != null) {
				for (String potentialName : list) {
					File baseFolder = new File(folder, potentialName);
					File libraryFolder = new File(baseFolder, "library");
					File libraryJar = new File(libraryFolder, potentialName + ".jar");
					// If a .jar file of the same prefix as the folder exists
					// inside the 'library' subfolder of the sketch
					if (libraryJar.exists()) {
						String sanityCheck = ProcessingUtilities.sanitizeName(potentialName);
						if (sanityCheck.equals(potentialName)) {
							libraries.add(new LibraryFolder(baseFolder));
						} else {
							ProcessingLog.logInfo(
									"The library \"" + potentialName + "\" cannot be used.\n" +
									"Library names must contain only basic letters and numbers.\n" +
									"(ASCII only and no spaces, and it cannot start with a number)"
									);
							continue;
						}
					}
				}
			}
		}
	}

	/** 
	 * Create a Library Folder from a file folder. 
	 * <p>
	 * The constructor assumes that the provided folder is the root of a valid library
	 * and does very little checking to ensure that. Invalid libraries may be logged and
	 * ignored, but more likely will cause a null pointer or something nasty to that end.
	 * <p> 
	 * Creating a library this way updates the LibraryModel's lookup table as a side effect.
	 */
	public LibraryFolder(File folder) {
		this.folder = folder;
		libraryFolder = new File(folder, "library");
		examplesFolder = new File(folder, "examples");

		File exportSettings = new File(libraryFolder, "export.txt");
		HashMap<String,String> exportTable = ProcessingUtilities.readSettings(exportSettings);

		name = exportTable.get("name");
		if (name == null) name = folder.getName();
		
		exportList = new HashMap<String, String[]>();

		// get the list of files just in the library root
		String[] baseList = folder.list(simpleFilter);

		String appletExportStr = exportTable.get("applet");
		if (appletExportStr != null) {
			appletExportList = ProcessingUtilities.splitTokens(appletExportStr, ", ");
		} else {
			appletExportList = baseList;
		}

		// for the host platform, need to figure out what's available
		File nativeLibraryFolder = libraryFolder;
		String hostPlatform = platformNames[ProcessingUtilities.platform];
		// see if there's a 'windows', 'macosx', or 'linux' folder
		File hostLibrary = new File(libraryFolder, hostPlatform);
		if (hostLibrary.exists()) nativeLibraryFolder = hostLibrary;
		// check for bit-specific version, e.g. on windows, check if there  
		// is a window32 or windows64 folder (on windows)
		hostLibrary = new File(libraryFolder, hostPlatform + nativeBits);
		if (hostLibrary.exists()) nativeLibraryFolder = hostLibrary;
		// save that folder for later use
		nativeLibraryPath = nativeLibraryFolder.getAbsolutePath();

		// for each individual platform that this library supports, figure out what's around
		for (int i = 1; i < platformNames.length; i++) {
			String platformName = platformNames[i];
			String platformName32 = platformName + "32";
			String platformName64 = platformName + "64";

			String platformAll = exportTable.get("application." + platformName);
			String[] platformList = platformAll == null ? null : ProcessingUtilities.splitTokens(platformAll, ", ");

			String platform32 = exportTable.get("application." + platformName + "32");
			String[] platformList32 = platform32 == null ? null : ProcessingUtilities.splitTokens(platform32, ", ");

			String platform64 = exportTable.get("application." + platformName + "64");
			String[] platformList64 = platform64 == null ? null : ProcessingUtilities.splitTokens(platform64, ", ");

			if (platformAll == null) {
				File folderAll = new File(libraryFolder, platformName);
				if (folderAll.exists())
					platformList = ProcessingUtilities.concat(baseList, folderAll.list(simpleFilter));
			}
			if (platform32 == null) {
				File folder32 = new File(libraryFolder, platformName32);
				if (folder32.exists())
					platformList32 = ProcessingUtilities.concat(baseList, folder32.list(simpleFilter));
			}
			if (platform64 == null) {
				File folder64 = new File(libraryFolder, platformName64);
				if (folder64.exists()) 
					platformList64 = ProcessingUtilities.concat(baseList, folder64.list(simpleFilter));
			}

			if (platformList32 != null || platformList64 != null) multipleArch[i] = true;

			// if there aren't any relevant imports specified or in their own folders, 
			// then use the baseList (root of the library folder) as the default. 
			if (platformList == null && platformList32 == null && platformList64 == null) {
				exportList.put(platformName, baseList);
			} else {      
				// once we've figured out which side our bread is buttered on, save it.
				// (also concatenate the list of files in the root folder as well
				if (platformList != null) exportList.put(platformName, platformList);
				if (platformList32 != null) exportList.put(platformName32, platformList32);
				if (platformList64 != null) exportList.put(platformName64, platformList64);
			}
		}

		// get the path for all .jar files in this code folder
		String[] packages = ProcessingUtilities.packageListFromClassPath(getClassPath());
		for (String pkg : packages) {
			LibraryFolder library = LibraryModel.importToLibraryTable.get(pkg);
			if (library != null) {
				ProcessingLog.logInfo(
						"The library found in " + getPath()
						+ "conflicts with " + library.getPath()
						+ "which already defines the package " + pkg + " -- "
						+ "the original library will be used."
				);
			} else {
				LibraryModel.importToLibraryTable.put(pkg, this);
			}
		}
	}

	/** Answers the library name */
	public String getName() {
		return name;
	}

	/** Answers the absolute path to the root of the 'library' folder */
	public String getPath() {
		return folder.getAbsolutePath();
	}

	/** Answers the absolute path to the 'library' folder in the root of the library */
	public String getLibraryPath() {
		return libraryFolder.getAbsolutePath();
	}

	/** Answers the absolute path to the library jar file */
	public String getJarPath() {
		return new File(folder, "library" + File.separatorChar + name + ".jar").getAbsolutePath(); 
	}


	// this prepends a colon so that it can be appended to other paths safely
	public String getClassPath() {
		StringBuilder cp = new StringBuilder();

		String[] jarHeads = libraryFolder.list(jarFilter);
		for (String jar : jarHeads) {
			cp.append(File.pathSeparatorChar);
			cp.append(new File(libraryFolder, jar).getAbsolutePath());
		}
		jarHeads = new File(nativeLibraryPath).list(jarFilter);
		for (String jar : jarHeads) {
			cp.append(File.pathSeparatorChar);
			cp.append(new File(nativeLibraryPath, jar).getAbsolutePath());
		}
		return cp.toString();
	}

	public String getNativePath() { return nativeLibraryPath; }

	protected File[] wrapFiles(String[] list) {
		File[] outgoing = new File[list.length];
		for (int i = 0; i < list.length; i++) {
			outgoing[i] = new File(libraryFolder, list[i]);
		}
		return outgoing;
	}

	public File[] getAppletExports() {
		return wrapFiles(appletExportList);
	}

	public File[] getApplicationExports(int platform, int bits) {
		String[] list = getApplicationExportList(platform, bits);
		return wrapFiles(list);
	}

	/**
	 * Returns the necessary exports for the specified platform. 
	 * If no 32 or 64-bit version of the exports exists, it returns the version
	 * that doesn't specify bit depth. 
	 */
	public String[] getApplicationExportList(int platform, int bits) {
		String platformName = platformNames[platform];
		if (bits == 32) {
			String[] pieces = exportList.get(platformName + "32");
			if (pieces != null) return pieces;
		} else if (bits == 64) {
			String[] pieces = exportList.get(platformName + "64");
			if (pieces != null) return pieces;
		}
		return exportList.get(platformName);
	}

	public boolean hasMultipleArch(int platform) {
		return multipleArch[platform];
	}

	static boolean hasMultipleArch(int platform, ArrayList<LibraryFolder> libraries) {
		for (LibraryFolder library : libraries) {
			if (library.hasMultipleArch(platform)) {
				return true;
			}
		}
		return false;
	}
}
