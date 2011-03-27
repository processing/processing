package processing.mode.javascript;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import processing.app.Base;
import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.core.PApplet;
import processing.mode.java.JavaBuild;

/** 
 * A collection of static methods to aid in building a 
 * web page with Processing.js from a Sketch object.
 */
public class JavaScriptBuild {

  // public static final String SIZE_REGEX

  // public static final String PACKAGE_REGEX
  
  /**
   * Answers with the first java doc style comment in the string,
   * or an empty string if no such comment can be found.
   */
  public static String getDocString(String s) {
    String[] javadoc = PApplet.match(s, "/\\*{2,}(.*?)\\*+/");
    if (javadoc != null) {
      StringBuffer dbuffer = new StringBuffer();
      String[] pieces = PApplet.split(javadoc[1], '\n');
      for (String line : pieces) {
        // if this line starts with * characters, remove 'em
        String[] m = PApplet.match(line, "^\\s*\\*+(.*)");
        dbuffer.append(m != null ? m[1] : line);
        // insert the new line into the html to help w/ line breaks
        dbuffer.append('\n');
      }
      return dbuffer.toString().trim();
    }
    return "";
  }

  
  /**
   * Reads in a simple template file, with fields of the form '@@somekey@@'
   * and replaces each field with the value in the map for 'somekey', writing
   * the output to the output file.
   * 
   * Keys not in the map will be replaced with empty strings.
   * 
   * @param template File object mapping to the template
   * @param output File object handle to the output
   * @param args template keys, data values to replace them
   * @throws IOException when there are problems writing to or from the files
   */
  public static void writeTemplate(File template, File output, Map<String, String> fields) throws IOException {
    BufferedReader reader = PApplet.createReader(template);
    PrintWriter theOutWriter = PApplet.createWriter(output);

    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.indexOf("@@") != -1) {
        StringBuffer sb = new StringBuffer(line);
        int start = 0, end = 0;
        while ((start = sb.indexOf("@@")) != -1) {
          if ((end = sb.indexOf("@@", start+1)) != -1) {
            String value = fields.get(sb.substring(start+2, end));
            sb.replace(start, end+2, value == null ? "" : value );
          } else {
            Base.showWarning("Problem writing file from template",
                             "The template appears to have an unterminated " +
                             "field. The output may look a little funny.",
                             null);
          }
        }
        line = sb.toString();
      }
      theOutWriter.println(line);
    }
    theOutWriter.close();
  }
  
  // -----------------------------------------------------
  

  /** 
   * The sketch this builder is working on.
   * <p>
   * Each builder instance should only work on a single sketch, so if
   * you have more than one sketch each will need a separate builder.
   */
  protected Sketch sketch;
  
  protected Mode mode;
  
  protected File binFolder;
  
  
  public JavaScriptBuild(Sketch sketch) {
    this.sketch = sketch;
    this.mode = sketch.getMode();
  }
  
  
  /**
   * Builds the sketch
   * <p>
   * The process goes a little something like this:
   * 1. rm -R bin/*
   * 2. cat *.pde > bin/sketchname.pde
   * 3. cp -r sketch/data/* bin/ (p.js doesn't recognize the data folder)
   * 4. series of greps to find height, width, name, desc
   * 5. cat template.html | sed 's/@@sketch@@/[name]/g' ... [many sed filters] > bin/index.html  
   * 
   * @param bin the output folder for the built sketch
   * @return boolean whether the build was successful
   */
  public boolean build(File bin)  {
    // make sure the user isn't playing "hide-the-sketch-folder" again
    sketch.ensureExistence();
    
    this.binFolder = bin;
    
    if (bin.exists()) {    
      Base.removeDescendants(bin);
    } //else will be created during preprocesss
    
    try {
      preprocess(bin);
    } catch (IOException e) {
      final String msg = "A problem occured while writing to the output folder.";
      Base.showWarning("Could not build the sketch", msg, e);
      return false;
    }

    // move the data files
    if (sketch.hasDataFolder()) {
      try {
        Base.copyDir(sketch.getDataFolder(), bin);
      } catch (IOException e) {
        final String msg = "An exception occured while trying to copy the data folder. " + 
                           "You may have to manually move the contents of sketch/data to " +
                           "the applet_js/ folder. Processing.js doesn't look for a data " +
                           "folder, so lump them together.";
        Base.showWarning("Problem building the sketch", msg, e);
      }
    }
    
    // TODO Code folder contents ending in .js could be moved and added as script tags?
    
    // get width and height
    int wide = PApplet.DEFAULT_WIDTH;
    int high = PApplet.DEFAULT_HEIGHT;

    String scrubbed = JavaBuild.scrubComments(sketch.getCode(0).getProgram());
    String[] matches = PApplet.match(scrubbed, JavaBuild.SIZE_REGEX);

    if (matches != null) {
      try {
        wide = Integer.parseInt(matches[1]);
        high = Integer.parseInt(matches[2]);
        // renderer
      } catch (NumberFormatException e) {
        // found a reference to size, but it didn't seem to contain numbers
        final String message =
          "The size of this applet could not automatically be\n" +
          "determined from your code. You'll have to edit the\n" +
          "HTML file to set the size of the applet.\n" +
          "Use only numeric values (not variables) for the size()\n" +
          "command. See the size() reference for an explanation.";
        Base.showWarning("Could not find applet size", message, null);
      }
    }  // else no size() command found, defaults will be used

    // final prep and write to template    
    File templateFile = sketch.getMode().getContentFile("applet_js/template.html");
    File htmlOutputFile = new File(bin, "index.html");

    Map<String, String> templateFields = new HashMap<String, String>();
    templateFields.put("width", String.valueOf(wide));
    templateFields.put("height", String.valueOf(high));
    templateFields.put("sketch", sketch.getName());
    templateFields.put("description", getSketchDescription());
    templateFields.put("source",
                       "<a href=\"" + sketch.getName() + ".pde\">" + 
                       sketch.getName() + "</a>");

    try{
      writeTemplate(templateFile, htmlOutputFile, templateFields);
    } catch (IOException ioe) {
      final String msg = "There was a problem writing the html template " +
      		               "to the build folder.";
      Base.showWarning("A problem occured during the build", msg, ioe);
      return false;
    }
    
    // finally, add Processing.js
    try {
      Base.copyFile(sketch.getMode().getContentFile("applet_js/processing.js"),
                    new File(bin, "processing.js"));
    } catch (IOException ioe) {
      final String msg = "There was a problem copying processing.js to the " +
                         "build folder. You will have to manually add " + 
                         "processing.js to the build folder before the sketch " +
                         "will run.";
      Base.showWarning("There was a problem writing to the build folder", msg, ioe);
      //return false; 
    }

    return true;
  }
  
  
  /** 
   * Prepares the sketch code objects for use with Processing.js
   * @param bin the output folder
   */
  public void preprocess(File bin) throws IOException {    
    // essentially... cat sketchFolder/*.pde > bin/sketchname.pde
    StringBuffer bigCode = new StringBuffer();
    for (SketchCode sc : sketch.getCode()){
      if (sc.isExtension("pde")) {
        bigCode.append(sc.getProgram());
        bigCode.append("\n");
      }
    }
    
    if (!bin.exists()) {
      bin.mkdirs();
    }
    File bigFile = new File(bin, sketch.getName() + ".pde");
    Base.saveFile(bigCode.toString(), bigFile);
  }
  
  
  /**
   * Parse the sketch to retrieve it's description. Answers with the first 
   * java doc style comment in the main sketch file, or an empty string if
   * no such comment exists.
   */
  public String getSketchDescription() {
    return getDocString(sketch.getCode(0).getProgram());
  }


  // -----------------------------------------------------
  // Export

  
  /** 
   * Export the sketch to the default applet_js folder.  
   * @return success of the operation 
   */
  public boolean export() throws IOException {
    File applet_js = new File(sketch.getFolder(), "applet_js");
    return exportApplet_js(applet_js);
  }

  
  /** 
   * Export the sketch to the provided folder 
   * @return success of the operation 
   */
  public boolean exportApplet_js(File appletfolder) throws IOException {
    return build(appletfolder);
  }
}
