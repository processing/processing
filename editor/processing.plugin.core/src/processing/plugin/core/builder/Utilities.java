package processing.plugin.core.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;

import processing.plugin.core.ProcessingLog;

/**
 * General container for static utility methods related to the build
 * process that should not be grouped with the ProcessingCore class.
 * 
 * Many of these are taken from PApplet or Base or some other class
 * that invokes UI. They have been placed here and sometimes modified
 * slightly to avoid invoking any UI components which cause warnings 
 * and other scary messages in Eclipse that might spook users.
 * 
 * All the UI stuff hasn't quite been removed yet, but if code is in here
 * it is not part of the problem, so that's something.
 */
public class Utilities {

	public static final String PACKAGE_REGEX = "(?:^|\\s|;)package\\s+(\\S+)\\;";

	/**
	 * Read in a file and return it as a string
	 * 
	 * @param file a resource handler for a file
	 * @return contents of the file
	 */
	public static String readFile(IFile file) {
		if (!file.exists())
			return "";
		InputStream stream = null;
		try{
			stream = file.getContents();
			Reader reader = new BufferedReader(new InputStreamReader(stream));
			StringBuffer result = new StringBuffer(2048);
			char[] buf = new char[2048];
			while (true){
				int count = reader.read(buf);
				if (count < 0)
					break;
				result.append(buf, 0, count);
			}
			return result.toString();
		} catch (Exception e){ // IOException and CoreException
			ProcessingLog.logError(e);
			return "";
		} finally {
			try{
				if (stream != null)
					stream.close();
			} catch (IOException e){
				ProcessingLog.logError(e);
				return "";
			}
		}
	}

	/**
	 * Count newlines in a string
	 * 
	 * @param what
	 * @return number of newline statements
	 */
	public static int getLineCount(String what){
		int count = 1;
		for (char c : what.toCharArray()) {
			if (c == '\n') count++;
		}
		return count;
	}

	/**
	 * Given a folder, return a list of absolute paths to all .jar and .zip
	 * (but not .class) files inside that folder, separated by the system's
	 * path separator character.
	 *
	 * This will prepend the system's path separator so that it can be directly 
	 * appended to another path string.
	 *
	 * This function doesn't bother checking to see if there are any .class
	 * files in the folder or within a subfolder.
	 */
	static public String contentsToClassPath(File folder) {
		if (folder == null) return "";

		StringBuffer abuffer = new StringBuffer();
		String sep = System.getProperty("path.separator");

		try {
			String path = folder.getCanonicalPath();

			// When getting the name of this folder, make sure it has a slash
			// after it, so that the names of sub-items can be added.
			if (!path.endsWith(File.separator)) {
				path += File.separator;
			}

			String list[] = folder.list();
			for (int i = 0; i < list.length; i++) {
				// Skip . and ._ files. Prior to 0125p3, .jar files that had
				// OS X AppleDouble files associated would cause trouble.
				if (list[i].startsWith(".")) continue;

				if (list[i].toLowerCase().endsWith(".jar") ||
						list[i].toLowerCase().endsWith(".zip")) {
					abuffer.append(sep);
					abuffer.append(path);
					abuffer.append(list[i]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();  // this would be odd
		}
		return abuffer.toString();
	}

	/**
	 * A classpath, separated by the path separator, will contain
	 * a series of .jar/.zip files or directories containing .class
	 * files, or containing subdirectories that have .class files.
	 *
	 * @param path the input classpath
	 * @return array of possible package names
	 */
	static public String[] packageListFromClassPath(String path) {
		Hashtable table = new Hashtable();
		String pieces[] =
			split(path, File.pathSeparatorChar);

		for (int i = 0; i < pieces.length; i++) {
			//System.out.println("checking piece '" + pieces[i] + "'");
			if (pieces[i].length() == 0) continue;

			if (pieces[i].toLowerCase().endsWith(".jar") ||
					pieces[i].toLowerCase().endsWith(".zip")) {
				//System.out.println("checking " + pieces[i]);
				packageListFromZip(pieces[i], table);

			} else {  // it's another type of file or directory
				File dir = new File(pieces[i]);
				if (dir.exists() && dir.isDirectory()) {
					packageListFromFolder(dir, null, table);
				}
			}
		}
		int tableCount = table.size();
		String output[] = new String[tableCount];
		int index = 0;
		Enumeration e = table.keys();
		while (e.hasMoreElements()) {
			output[index++] = ((String) e.nextElement()).replace('/', '.');
		}
		//System.arraycopy(imports, 0, output, 0, importCount);
		//PApplet.printarr(output);
		return output;
	}

	/**
	 * Get a list of packages contained in a .zip file
	 */
	static private void packageListFromZip(String filename, Hashtable table) {
		try {
			ZipFile file = new ZipFile(filename);
			Enumeration entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();

				if (!entry.isDirectory()) {
					String name = entry.getName();

					if (name.endsWith(".class")) {
						int slash = name.lastIndexOf('/');
						if (slash == -1) continue;

						String pname = name.substring(0, slash);
						if (table.get(pname) == null) {
							table.put(pname, new Object());
						}
					}
				}
			}
		} catch (IOException e) {
			ProcessingLog.logInfo("Ignoring " + filename + " (" + e.getMessage() + ")");
			//e.printStackTrace();
		}
	}

	/**
	 * Make list of package names by traversing a directory hierarchy. Each time a 
	 * class is found in a folder, add its containing set of folders to the package 
	 * list. If another folder is found, walk down into that folder and continue.
	 */
	static private void packageListFromFolder(File dir, String sofar, Hashtable table) {

		boolean foundClass = false;
		String files[] = dir.list();

		for (int i = 0; i < files.length; i++) {
			if (files[i].equals(".") || files[i].equals("..")) continue;

			File sub = new File(dir, files[i]);
			if (sub.isDirectory()) {
				String nowfar =
					(sofar == null) ? files[i] : (sofar + "." + files[i]);
					packageListFromFolder(sub, nowfar, table);
			} else if (!foundClass) {  // if no classes found in this folder yet
				if (files[i].endsWith(".class")) {
					table.put(sofar, new Object());
					foundClass = true;
				}
			}
		}
	}

	  /**
	   * Produce a sanitized name that fits our standards for likely to work.
	   * <p/>
	   * Java classes have a wider range of names that are technically allowed
	   * (supposedly any Unicode name) than what we support. The reason for
	   * going more narrow is to avoid situations with text encodings and
	   * converting during the process of moving files between operating
	   * systems, i.e. uploading from a Windows machine to a Linux server,
	   * or reading a FAT32 partition in OS X and using a thumb drive.
	   * <p/>
	   * This helper function replaces everything but A-Z, a-z, and 0-9 with
	   * underscores. Also disallows starting the sketch name with a digit.
	   */
	  static public String sanitizeName(String origName) {
	    char c[] = origName.toCharArray();
	    StringBuffer buffer = new StringBuffer();

	    // can't lead with a digit, so start with an underscore
	    if ((c[0] >= '0') && (c[0] <= '9')) {
	      buffer.append('_');
	    }
	    for (int i = 0; i < c.length; i++) {
	      if (((c[i] >= '0') && (c[i] <= '9')) ||
	          ((c[i] >= 'a') && (c[i] <= 'z')) ||
	          ((c[i] >= 'A') && (c[i] <= 'Z'))) {
	        buffer.append(c[i]);

	      } else {
	        buffer.append('_');
	      }
	    }
	    // let's not be ridiculous about the length of filenames.
	    // in fact, Mac OS 9 can handle 255 chars, though it can't really
	    // deal with filenames longer than 31 chars in the Finder.
	    // but limiting to that for sketches would mean setting the
	    // upper-bound on the character limit here to 25 characters
	    // (to handle the base name + ".class")
	    if (buffer.length() > 63) {
	      buffer.setLength(63);
	    }
	    return buffer.toString();
	  }
	
	  /**
	   * Match a string with a regular expression, and returns the match as an
	   * array. The first index is the matching expression, and array elements
	   * [1] and higher represent each of the groups (sequences found in parens).
	   *
	   * This uses multiline matching (Pattern.MULTILINE) and dotall mode
	   * (Pattern.DOTALL) by default, so that ^ and $ match the beginning and
	   * end of any lines found in the source, and the . operator will also
	   * pick up newline characters.
	   */
	  static public String[] match(String what, String regexp) {
	    Pattern p = matchPattern(regexp);
	    Matcher m = p.matcher(what);
	    if (m.find()) {
	      int count = m.groupCount() + 1;
	      String[] groups = new String[count];
	      for (int i = 0; i < count; i++) {
	        groups[i] = m.group(i);
	      }
	      return groups;
	    }
	    return null;
	  }
	  
	  static protected HashMap<String, Pattern> matchPatterns;
	  
	  static Pattern matchPattern(String regexp) {
		    Pattern p = null;
		    if (matchPatterns == null) {
		      matchPatterns = new HashMap<String, Pattern>();
		    } else {
		      p = matchPatterns.get(regexp);
		    }
		    if (p == null) {
		      if (matchPatterns.size() == 10) {
		        // Just clear out the match patterns here if more than 10 are being
		        // used. It's not terribly efficient, but changes that you have >10
		        // different match patterns are very slim, unless you're doing
		        // something really tricky (like custom match() methods), in which
		        // case match() won't be efficient anyway. (And you should just be
		        // using your own Java code.) The alternative is using a queue here,
		        // but that's a silly amount of work for negligible benefit.
		        matchPatterns.clear();
		      }
		      p = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
		      matchPatterns.put(regexp, p);
		    }
		    return p;
		  }

	  /**
	   * Split a String on a specific delimiter. Unlike Java's String.split()
	   * method, this does not parse the delimiter as a regexp because it's more
	   * confusing than necessary, and String.split() is always available for
	   * those who want regexp.
	   */
	  static public String[] split(String what, String delim) {
	    ArrayList<String> items = new ArrayList<String>();
	    int index;
	    int offset = 0;
	    while ((index = what.indexOf(delim, offset)) != -1) {
	      items.add(what.substring(offset, index));
	      offset = index + delim.length();
	    }
	    items.add(what.substring(offset));
	    String[] outgoing = new String[items.size()];
	    items.toArray(outgoing);
	    return outgoing;
	  }
	  
	  /**
	   * Split a string into pieces along a specific character.
	   * Most commonly used to break up a String along a space or a tab
	   * character.
	   * <P>
	   * This operates differently than the others, where the
	   * single delimeter is the only breaking point, and consecutive
	   * delimeters will produce an empty string (""). This way,
	   * one can split on tab characters, but maintain the column
	   * alignments (of say an excel file) where there are empty columns.
	   */
	  static public String[] split(String what, char delim) {
	    // do this so that the exception occurs inside the user's
	    // program, rather than appearing to be a bug inside split()
	    if (what == null) return null;
	    //return split(what, String.valueOf(delim));  // huh

	    char chars[] = what.toCharArray();
	    int splitCount = 0; //1;
	    for (int i = 0; i < chars.length; i++) {
	      if (chars[i] == delim) splitCount++;
	    }
	    // make sure that there is something in the input string
	    //if (chars.length > 0) {
	      // if the last char is a delimeter, get rid of it..
	      //if (chars[chars.length-1] == delim) splitCount--;
	      // on second thought, i don't agree with this, will disable
	    //}
	    if (splitCount == 0) {
	      String splits[] = new String[1];
	      splits[0] = new String(what);
	      return splits;
	    }
	    //int pieceCount = splitCount + 1;
	    String splits[] = new String[splitCount + 1];
	    int splitIndex = 0;
	    int startIndex = 0;
	    for (int i = 0; i < chars.length; i++) {
	      if (chars[i] == delim) {
	        splits[splitIndex++] =
	          new String(chars, startIndex, i-startIndex);
	        startIndex = i + 1;
	      }
	    }
	    //if (startIndex != chars.length) {
	      splits[splitIndex] =
	        new String(chars, startIndex, chars.length-startIndex);
	    //}
	    return splits;
	  }

}
