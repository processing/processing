package processing.app;

import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import processing.app.debug.Compiler;
import processing.core.PApplet;


public class LibraryFolder implements Comparable {
  String name;          // pdf
  String prettyName;    // PDF Export
  String author;        // Ben Fry
  String authorURL;     // http://processing.org
  String sentence;      // Write graphics to PDF files.
  String paragraph;     // <paragraph length description for site>
  int version;          // 102
  String prettyVersion; // "1.0.2"
//  String[] packages;


  // incomplete, commented out for debugging so as not to break the build
  /*
  static ArrayList<LibraryFolder> findLibraries(File folder) {
    if (!folder.isDirectory()) return null;

    String list[] = folder.list(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        // skip .DS_Store files, .svn folders, etc
        if (name.charAt(0) == '.') return false;
        return (new File(dir, name).isDirectory());
      }
    });
    // if a bad folder or inaccessible, this might come back null
    if (list == null) return null;

    // alphabetize list, since it's not always alpha order
//    Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

//    ActionListener listener = new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        activeEditor.getSketch().importLibrary(e.getActionCommand());
//      }
//    };

    boolean ifound = false;
    
    ArrayList<LibraryFolder> outgoing = new ArrayList<LibraryFolder>(); 

    for (String potentialName : list) {
      File subfolder = new File(folder, potentialName);
      File libraryFolder = new File(subfolder, "library");
      File libraryJar = new File(libraryFolder, potentialName + ".jar");
      // If a .jar file of the same prefix as the folder exists
      // inside the 'library' subfolder of the sketch
      if (libraryJar.exists()) {
        String sanityCheck = Sketch.sanitizeName(potentialName);
        if (!sanityCheck.equals(potentialName)) {
          String mess =
            "The library \"" + potentialName + "\" cannot be used.\n" +
            "Library names must contain only basic letters and numbers.\n" +
            "(ASCII only and no spaces, and it cannot start with a number)";
          Base.showMessage("Ignoring bad library name", mess);
          continue;
        }

        String libraryName = potentialName;
        File exportFile = new File(libraryFolder, "info.txt");
        //System.out.println(exportFile.getAbsolutePath());
        if (exportFile.exists()) {
          String[] exportLines = PApplet.loadStrings(exportFile);
          for (String line : exportLines) {
            String[] pieces = PApplet.trim(PApplet.split(line, '='));
            //              System.out.println(pieces);
            if (pieces[0].equals("name")) {
              libraryName = pieces[1].trim();
            }
          }
        }

        // get the path for all .jar files in this code folder
        String libraryClassPath =
          Compiler.contentsToClassPath(libraryFolder);
        // grab all jars and classes from this folder,
        // and append them to the library classpath
        librariesClassPath +=
          File.pathSeparatorChar + libraryClassPath;
        // need to associate each import with a library folder
        String[] packages =
          Compiler.packageListFromClassPath(libraryClassPath);
        for (String pkg : packages) {
          importToLibraryTable.put(pkg, libraryFolder);
        }

        JMenuItem item = new JMenuItem(libraryName);
        item.addActionListener(listener);
        item.setActionCommand(libraryJar.getAbsolutePath());
        menu.add(item);
        ifound = true;

      } else {  // not a library, but is still a folder, so recurse
        JMenu submenu = new JMenu(potentialName);
        // needs to be separate var, otherwise would set ifound to false
        boolean found = addLibraries(submenu, subfolder);
        if (found) {
          menu.add(submenu);
          ifound = true;
        }
      }
    }
    return ifound;
  }
  */
  
  
  public LibraryFolder() {

  }


  public int compareTo(Object o) {
    return prettyName.compareTo(((LibraryFolder) o).prettyName);
  }
}
