/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-15 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.io.*;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.*;

import processing.core.PApplet;
import processing.data.StringDict;
import processing.data.StringList;


public class Util {

  /**
   * Get the number of lines in a file by counting the number of newline
   * characters inside a String (and adding 1).
   */
  static public int countLines(String what) {
    int count = 1;
    for (char c : what.toCharArray()) {
      if (c == '\n') count++;
    }
    return count;
  }


  /**
   * Same as PApplet.loadBytes(), however never does gzip decoding.
   */
  static public byte[] loadBytesRaw(File file) throws IOException {
    int size = (int) file.length();
    FileInputStream input = new FileInputStream(file);
    byte buffer[] = new byte[size];
    int offset = 0;
    int bytesRead;
    while ((bytesRead = input.read(buffer, offset, size-offset)) != -1) {
      offset += bytesRead;
      if (bytesRead == 0) break;
    }
    input.close();  // weren't properly being closed
    input = null;
    return buffer;
  }


  /**
   * Read from a file with a bunch of attribute/value pairs
   * that are separated by = and ignore comments with #.
   * Changed in 3.x to return null (rather than empty hash) if no file,
   * and changed return type to StringDict instead of Map or HashMap.
   */
  static public StringDict readSettings(File inputFile) {
    if (!inputFile.exists()) {
      Messages.loge(inputFile + " does not exist inside readSettings()");
      return null;
    }
    String lines[] = PApplet.loadStrings(inputFile);
    if (lines == null) {
      System.err.println("Could not read " + inputFile);
      return null;
    }
    return readSettings(inputFile.toString(), lines);
  }


  /**
   * Parse a String array that contains attribute/value pairs separated
   * by = (the equals sign). The # (hash) symbol is used to denote comments.
   * Comments can be anywhere on a line. Blank lines are ignored.
   * In 3.0a6, no longer taking a blank HashMap as param; no cases in the main
   * PDE code of adding to a (Hash)Map. Also returning the Map instead of void.
   * Both changes modify the method signature, but this was only used by the
   * contrib classes.
   */
  static public StringDict readSettings(String filename, String[] lines) {
    StringDict settings = new StringDict();
    for (String line : lines) {
      // Remove comments
      int commentMarker = line.indexOf('#');
      if (commentMarker != -1) {
        line = line.substring(0, commentMarker);
      }
      // Remove extra whitespace
      line = line.trim();

      if (line.length() != 0) {
        int equals = line.indexOf('=');
        if (equals == -1) {
          if (filename != null) {
            System.err.println("Ignoring illegal line in " + filename);
            System.err.println("  " + line);
          }
        } else {
          String attr = line.substring(0, equals).trim();
          String valu = line.substring(equals + 1).trim();
          settings.set(attr, valu);
        }
      }
    }
    return settings;
  }


  static public void copyFile(File sourceFile,
                              File targetFile) throws IOException {
    BufferedInputStream from =
      new BufferedInputStream(new FileInputStream(sourceFile));
    BufferedOutputStream to =
      new BufferedOutputStream(new FileOutputStream(targetFile));
    byte[] buffer = new byte[16 * 1024];
    int bytesRead;
    while ((bytesRead = from.read(buffer)) != -1) {
      to.write(buffer, 0, bytesRead);
    }
    from.close();
    from = null;

    to.flush();
    to.close();
    to = null;

    targetFile.setLastModified(sourceFile.lastModified());
    targetFile.setExecutable(sourceFile.canExecute());
  }


  /**
   * Grab the contents of a file as a string. Connects lines with \n,
   * even if the input file used \r\n.
   */
  static public String loadFile(File file) throws IOException {
    String[] contents = PApplet.loadStrings(file);
    if (contents == null) return null;
    return PApplet.join(contents, "\n");
  }


  /**
   * Spew the contents of a String object out to a file. As of 3.0 beta 2,
   * this will replace and write \r\n for newlines on Windows.
   * https://github.com/processing/processing/issues/3455
   * As of 3.3.7, this puts a newline at the end of the file,
   * per good practice/POSIX: https://stackoverflow.com/a/729795
   */
  static public void saveFile(String text, File file) throws IOException {
    String[] lines = text.split("\\r?\\n");
    File temp = File.createTempFile(file.getName(), null, file.getParentFile());
    try {
      // fix from cjwant to prevent symlinks from being destroyed.
      File canon = file.getCanonicalFile();
      // assign the var as second step since previous line may throw exception
      file = canon;
    } catch (IOException e) {
      throw new IOException("Could not resolve canonical representation of " +
                            file.getAbsolutePath());
    }
    // Could use saveStrings(), but the we wouldn't be able to checkError()
    PrintWriter writer = PApplet.createWriter(temp);
    for (String line : lines) {
      writer.println(line);
    }
    boolean error = writer.checkError();  // calls flush()
    writer.close();  // attempt to close regardless
    if (error) {
      throw new IOException("Error while trying to save " + file);
    }

    // remove the old file before renaming the temp file
    if (file.exists()) {
      boolean result = file.delete();
      if (!result) {
        throw new IOException("Could not remove old version of " +
                              file.getAbsolutePath());
      }
    }
    boolean result = temp.renameTo(file);
    if (!result) {
      throw new IOException("Could not replace " + file.getAbsolutePath() +
                            " with " + temp.getAbsolutePath());
    }
  }


  /**
   * Create a temporary folder by using the createTempFile() mechanism,
   * deleting the file it creates, and making a folder using the location
   * that was provided.
   *
   * Unlike createTempFile(), there is no minimum size for prefix. If
   * prefix is less than 3 characters, the remaining characters will be
   * filled with underscores
   */
  static public File createTempFolder(String prefix, String suffix,
                                      File directory) throws IOException {
    int fillChars = 3 - prefix.length();
    for (int i = 0; i < fillChars; i++) {
      prefix += '_';
    }
    File folder = File.createTempFile(prefix, suffix, directory);
    // Now delete that file and create a folder in its place
    folder.delete();
    folder.mkdirs();
    // And send the folder back to your friends
    return folder;
  }


  /**
   * Copy a folder from one place to another. This ignores all dot files and
   * folders found in the source directory, to avoid copying silly .DS_Store
   * files and potentially troublesome .svn folders.
   */
  static public void copyDir(File sourceDir,
                             File targetDir) throws IOException {
    if (sourceDir.equals(targetDir)) {
      final String urDum = "source and target directories are identical";
      throw new IllegalArgumentException(urDum);
    }
    targetDir.mkdirs();
    String files[] = sourceDir.list();
    for (int i = 0; i < files.length; i++) {
      // Ignore dot files (.DS_Store), dot folders (.svn) while copying
      if (files[i].charAt(0) == '.') continue;
      //if (files[i].equals(".") || files[i].equals("..")) continue;
      File source = new File(sourceDir, files[i]);
      File target = new File(targetDir, files[i]);
      if (source.isDirectory()) {
        //target.mkdirs();
        copyDir(source, target);
        target.setLastModified(source.lastModified());
      } else {
        copyFile(source, target);
      }
    }
  }


  static public void copyDirNative(File sourceDir,
                                   File targetDir) throws IOException {
    Process process = null;
    if (Platform.isMacOS() || Platform.isLinux()) {
      process = Runtime.getRuntime().exec(new String[] {
        "cp", "-a", sourceDir.getAbsolutePath(), targetDir.getAbsolutePath()
      });
    } else {
      // TODO implement version that uses XCOPY here on Windows
      throw new RuntimeException("Not yet implemented on Windows");
    }
    try {
      int result = process.waitFor();
      if (result != 0) {
        throw new IOException("Error while copying (result " + result + ")");
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


//  /**
//   * Delete a file or directory in a platform-specific manner. Removes a File
//   * object (a file or directory) from the system by placing it in the Trash
//   * or Recycle Bin (if available) or simply deleting it (if not).
//   *
//   * When the file/folder is on another file system, it may simply be removed
//   * immediately, without additional warning. So only use this if you want to,
//   * you know, "delete" the subject in question.
//   *
//   * NOTE: Not yet tested nor ready for prime-time.
//   *
//   * @param file the victim (a directory or individual file)
//   * @return true if all ends well
//   * @throws IOException what went wrong
//   */
//  static public boolean platformDelete(File file) throws IOException {
//    return Base.getPlatform().deleteFile(file);
//  }


  /**
   * Remove all files in a directory and the directory itself.
   * Prints error messages with failed filenames. Does not follow symlinks.
   */
  static public boolean removeDir(File dir) {
    return removeDir(dir, true);
  }

  /**
   * Remove all files in a directory and the directory itself.
   * Optinally prints error messages with failed filenames.
   * Does not follow symlinks.
   */
  static public boolean removeDir(File dir, boolean printErrorMessages) {
    if (!dir.exists()) return true;

    boolean result = true;
    if (!Files.isSymbolicLink(dir.toPath())) {
      File[] files = dir.listFiles();
      if (files != null) {
        for (File child : files) {
          if (child.isFile()) {
            boolean deleted = child.delete();
            if (!deleted && printErrorMessages) {
              System.err.println("Could not delete " + child.getAbsolutePath());
            }
            result &= deleted;
          } else if (child.isDirectory()) {
            result &= removeDir(child, printErrorMessages);
          }
        }
      }
    }
    boolean deleted = dir.delete();
    if (!deleted && printErrorMessages) {
      System.err.println("Could not delete " + dir.getAbsolutePath());
    }
    result &= deleted;
    return result;
  }


  /**
   * Function to return the length of the file, or entire directory, including
   * the component files and sub-folders if passed.
   * @param file The file or folder to calculate
   */
  static public long calcSize(File file) {
    return file.isFile() ? file.length() : Util.calcFolderSize(file);
  }


  /**
   * Calculate the size of the contents of a folder.
   * Used to determine whether sketches are empty or not.
   * Note that the function calls itself recursively.
   */
  static public long calcFolderSize(File folder) {
    int size = 0;

    String files[] = folder.list();
    // null if folder doesn't exist, happens when deleting sketch
    if (files == null) return -1;

    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") ||
          files[i].equals("..") ||
          files[i].equals(".DS_Store")) continue;
      File fella = new File(folder, files[i]);
      if (fella.isDirectory()) {
        size += calcFolderSize(fella);
      } else {
        size += (int) fella.length();
      }
    }
    return size;
  }


  /**
   * Recursively creates a list of all files within the specified folder,
   * and returns a list of their relative paths.
   * Ignores any files/folders prefixed with a dot.
   * @param relative true return relative paths instead of absolute paths
   */
  static public String[] listFiles(File folder, boolean relative) {
    return listFiles(folder, relative, null);
  }


  static public String[] listFiles(File folder, boolean relative,
                                   String extension) {
    if (extension != null) {
      if (!extension.startsWith(".")) {
        extension = "." + extension;
      }
    }

    StringList list = new StringList();
    listFilesImpl(folder, relative, extension, list);

    if (relative) {
      String[] outgoing = new String[list.size()];
      // remove the slash (or backslash) as well
      int prefixLength = folder.getAbsolutePath().length() + 1;
      for (int i = 0; i < outgoing.length; i++) {
        outgoing[i] = list.get(i).substring(prefixLength);
      }
      return outgoing;
    }
    return list.array();
  }


  static void listFilesImpl(File folder, boolean relative,
                            String extension, StringList list) {
    File[] items = folder.listFiles();
    if (items != null) {
      for (File item : items) {
        String name = item.getName();
        if (name.charAt(0) != '.') {
          if (item.isDirectory()) {
            listFilesImpl(item, relative, extension, list);

          } else {  // a file
            if (extension == null || name.endsWith(extension)) {
              list.append(item.getAbsolutePath());
            }
          }
        }
      }
    }
  }


  /**
   * @param folder source folder to search
   * @return a list of .jar and .zip files in that folder
   */
  static public File[] listJarFiles(File folder) {
    return folder.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return (!name.startsWith(".") &&
                (name.toLowerCase().endsWith(".jar") ||
                 name.toLowerCase().endsWith(".zip")));
      }
    });
  }


  /////////////////////////////////////////////////////////////////////////////


  /**
   * Given a folder, return a list of absolute paths to all jar or zip files
   * inside that folder, separated by pathSeparatorChar.
   *
   * This will prepend a colon (or whatever the path separator is)
   * so that it can be directly appended to another path string.
   *
   * As of 0136, this will no longer add the root folder as well.
   *
   * This function doesn't bother checking to see if there are any .class
   * files in the folder or within a subfolder.
   */
  static public String contentsToClassPath(File folder) {
    if (folder == null) return "";

    StringBuilder sb = new StringBuilder();
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
          sb.append(sep);
          sb.append(path);
          sb.append(list[i]);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();  // this would be odd
    }
    return sb.toString();
  }


  /**
   * A classpath, separated by the path separator, will contain
   * a series of .jar/.zip files or directories containing .class
   * files, or containing subdirectories that have .class files.
   *
   * @param path the input classpath
   * @return array of possible package names
   */
  static public StringList packageListFromClassPath(String path) {
//    Map<String, Object> map = new HashMap<String, Object>();
    StringList list = new StringList();
    String pieces[] =
      PApplet.split(path, File.pathSeparatorChar);

    for (int i = 0; i < pieces.length; i++) {
      //System.out.println("checking piece '" + pieces[i] + "'");
      if (pieces[i].length() == 0) continue;

      if (pieces[i].toLowerCase().endsWith(".jar") ||
          pieces[i].toLowerCase().endsWith(".zip")) {
        //System.out.println("checking " + pieces[i]);
        packageListFromZip(pieces[i], list);

      } else {  // it's another type of file or directory
        File dir = new File(pieces[i]);
        if (dir.exists() && dir.isDirectory()) {
          packageListFromFolder(dir, null, list);
          //importCount = magicImportsRecursive(dir, null,
          //                                  map);
                                              //imports, importCount);
        }
      }
    }
//    int mapCount = map.size();
//    String output[] = new String[mapCount];
//    int index = 0;
//    Set<String> set = map.keySet();
//    for (String s : set) {
//      output[index++] = s.replace('/', '.');
//    }
//    return output;
    StringList outgoing = new StringList(list.size());
    for (String item : list) {
      outgoing.append(item.replace('/', '.'));
    }
    return outgoing;
  }


  static private void packageListFromZip(String filename, StringList list) {
    try {
      ZipFile file = new ZipFile(filename);
      Enumeration<?> entries = file.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) entries.nextElement();

        if (!entry.isDirectory()) {
          String name = entry.getName();

          // Avoid META-INF because some jokers but .class files in there
          // https://github.com/processing/processing/issues/5778
          if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
            int slash = name.lastIndexOf('/');
            if (slash != -1) {
              String packageName = name.substring(0, slash);
              list.appendUnique(packageName);
            }
          }
        }
      }
      file.close();
    } catch (IOException e) {
      System.err.println("Ignoring " + filename + " (" + e.getMessage() + ")");
      //e.printStackTrace();
    }
  }


  /**
   * Make list of package names by traversing a directory hierarchy.
   * Each time a class is found in a folder, add its containing set
   * of folders to the package list. If another folder is found,
   * walk down into that folder and continue.
   */
  static private void packageListFromFolder(File dir, String sofar,
                                            StringList list) {
//                                            Map<String, Object> map) {
    boolean foundClass = false;
    String files[] = dir.list();

    for (int i = 0; i < files.length; i++) {
      if (files[i].equals(".") || files[i].equals("..")) continue;

      File sub = new File(dir, files[i]);
      if (sub.isDirectory()) {
        String nowfar =
          (sofar == null) ? files[i] : (sofar + "." + files[i]);
        packageListFromFolder(sub, nowfar, list);
        //System.out.println(nowfar);
        //imports[importCount++] = nowfar;
        //importCount = magicImportsRecursive(sub, nowfar,
        //                                  imports, importCount);
      } else if (!foundClass) {  // if no classes found in this folder yet
        if (files[i].endsWith(".class")) {
          //System.out.println("unique class: " + files[i] + " for " + sofar);
//          map.put(sofar, new Object());
          list.appendUnique(sofar);
          foundClass = true;
        }
      }
    }
  }


  static public void unzip(File zipFile, File dest) {
    try {
      FileInputStream fis = new FileInputStream(zipFile);
      CheckedInputStream checksum = new CheckedInputStream(fis, new Adler32());
      ZipInputStream zis = new ZipInputStream(new BufferedInputStream(checksum));
      ZipEntry next = null;
      while ((next = zis.getNextEntry()) != null) {
        File currentFile = new File(dest, next.getName());
        if (next.isDirectory()) {
          currentFile.mkdirs();
        } else {
          File parentDir = currentFile.getParentFile();
          // Sometimes the directory entries aren't already created
          if (!parentDir.exists()) {
            parentDir.mkdirs();
          }
          currentFile.createNewFile();
          unzipEntry(zis, currentFile);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  static protected void unzipEntry(ZipInputStream zin, File f) throws IOException {
    FileOutputStream out = new FileOutputStream(f);
    byte[] b = new byte[512];
    int len = 0;
    while ((len = zin.read(b)) != -1) {
      out.write(b, 0, len);
    }
    out.flush();
    out.close();
  }


  static public byte[] gzipEncode(byte[] what) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    GZIPOutputStream output = new GZIPOutputStream(baos);
    PApplet.saveStream(output, new ByteArrayInputStream(what));
    output.close();
    return baos.toByteArray();
  }


  static public final boolean containsNonASCII(String what) {
    for (char c : what.toCharArray()) {
      if (c < 32 || c > 127) return true;
    }
    return false;
  }
}
