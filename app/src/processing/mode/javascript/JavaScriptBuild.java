package processing.mode.javascript;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import processing.app.Base;
import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.SketchException;
import processing.app.Library;

import processing.core.PApplet;

import processing.mode.java.preproc.PdePreprocessor;


public class JavaScriptBuild
{
	public final static String TEMPLATE_FOLDER_NAME = "template";
	public final static String EXPORTED_FOLDER_NAME = "web-export";
	public final static String TEMPLATE_FILE_NAME = "template.html";

	public final static String IMPORT_REGEX =
								"^[\\s]*import[\\s]+([^\\s]+)[\\s]*";

  /**
   * Answers with the first java doc style comment in the string,
   * or an empty string if no such comment can be found.
   */
  public static String getDocString ( String s )
  {
    String[] javadoc = PApplet.match(s, "/\\*{2,}(.*?)\\*+/");

    if (javadoc != null)
	{
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
  public static void writeTemplate ( File template, File output, Map<String, String> fields )
  throws IOException
  {
    BufferedReader reader = PApplet.createReader(template);
    PrintWriter writer = PApplet.createWriter(output);

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
      writer.println(line);
    }
    writer.close();
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

  public JavaScriptBuild ( Sketch sketch )
  {
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
   * </p>
   *
   * @param bin the output folder for the built sketch
   * @return boolean whether the build was successful
   */
  public boolean build ( File bin ) throws IOException, SketchException
  {
    // make sure the user isn't playing "hide-the-sketch-folder" again
    sketch.ensureExistence();

    this.binFolder = bin;

	// we need these ..
//	JavaScriptMode jsMode = (JavaScriptMode)mode;
//	JavaScriptEditor jsEditor = (JavaScriptEditor)jsMode.getEditor();
//	BasicServer jsServer = jsEditor.getServer();

    if ( bin.exists() )
    {
      Base.removeDescendants(bin);
    } //else will be created during preprocesss

	// pass through preprocessor to catch syntax errors
    // .. exceptions bubble up.
    preprocess(bin);

    // move the data files, copies contents of sketch/data/ to web-export/
    if (sketch.hasDataFolder())
	{
      try {
        Base.copyDir(sketch.getDataFolder(), bin);

      } catch (IOException e) {
        final String msg = "An exception occured while trying to copy the data folder. " +
                           "You may have to manually move the contents of sketch/data to " +
                           "the web-export/ folder. Processing.js doesn't look for a data " +
                           "folder, so lump them together.";
        Base.showWarning("Problem building the sketch", msg, e);
      }
    }

	// as .js files are allowed now include these into the mix,
	// first find 'em ..
	String[] sketchFolderFilesRaw = sketch.getFolder().list();
	String[] sketchFolderFiles = new String[0];
	ArrayList sffList = new ArrayList();
	if ( sketchFolderFilesRaw != null )
	{
		for ( String s : sketchFolderFilesRaw )
		{
			if ( s.toLowerCase().startsWith(".") ) continue;
			if ( !s.toLowerCase().endsWith(".js") ) continue;
			sffList.add(s);
		}
		if ( sffList.size() > 0 )
			sketchFolderFiles = (String[])sffList.toArray(new String[0]);
	}
	for ( String s : sketchFolderFiles )
	{
		try {
			Base.copyFile( new File(sketch.getFolder(), s), new File(bin, s) );
		} catch ( IOException ioe ) {
			String msg = "Unable to copy file: "+s;
			Base.showWarning("Problem building the sketch", msg, ioe);
			return false;
		}
	}

	// TODO
	// Really scrub comments from code?
	// Con: larger files, PJS needs to do it later
	// Pro: being literate as we are in a script language.
    String scrubbed = PdePreprocessor.scrubComments(sketch.getCode(0).getProgram());

    // get width and height
    int wide = PApplet.DEFAULT_WIDTH;
    int high = PApplet.DEFAULT_HEIGHT;
    String[] matches = PApplet.match(scrubbed, PdePreprocessor.SIZE_REGEX);
    if (matches != null)
	{
      try
	  {
        wide = Integer.parseInt(matches[1]);
        high = Integer.parseInt(matches[2]);
        // renderer

      } catch (NumberFormatException e) {
		if ( ((JavaScriptMode)mode).showSizeWarning ) {
   	    	// found a reference to size, but it didn't seem to contain numbers
	        final String message =
	          "The size of this applet could not automatically be\n" +
	          "determined from your code. You'll have to edit the\n" +
	          "HTML file to set the size of the applet.\n" +
	          "Use only numeric values (not variables) for the size()\n" +
	          "command. See the size() reference for an explanation.";
	        Base.showWarning("Could not find applet size", message, null);
			// warn only once ..
			((JavaScriptMode)mode).showSizeWarning = false;
		}
      }
    }  // else no size() command found, defaults will be used

	// try resolve imports
	ArrayList<String> importPackages = new ArrayList<String>();
	String[] lines = scrubbed.split( "\n" );
	for ( String l : lines )
	{
		int iIndex = l.indexOf( "import" );
		if ( iIndex != -1 )
		{
			String[] iStatements = l.split(";");
			for ( String iExpression : iStatements )
			{
				matches = PApplet.match( iExpression, JavaScriptBuild.IMPORT_REGEX );
				if ( matches != null && matches.length >= 2 && matches[1] != null )
				{
					String iPackage = matches[1];
					iPackage = iPackage.trim();

					if ( iPackage.indexOf(".*") != -1 ) {
						// de.bezier.tutto.*
						iPackage = iPackage.replace( ".*", "" );
					} else {
						// de.bezier.uno.SingleClass
						iPackage = iPackage.replaceAll( "\\.[^.]+$", "" );
					}
					if ( !importPackages.contains(iPackage) ) // is this a "==" or ".equals()" ?
						importPackages.add( iPackage );
				}
			}
		}
	}
	ArrayList<String> jsImports = new ArrayList<String>();
	if ( importPackages.size() > 0 )
	{
		File libsExport = new File( bin, "libs" );
		if ( !libsExport.mkdir() )
		{
			Base.showWarning( "Error",
			 				  "Unable to create 'libs' in export folder.",
							  null );
			return false;
		}
	}
	for ( String pack : importPackages )
	{
		Library lib = mode.getLibrary( pack );
		if ( lib != null )
		{
			String libPath = lib.getJarPath();
			File libJar = new File( libPath );
			if ( libJar.exists() )
			{
				File libJS = new File( libJar.getParent(), libJar.getName().replace(".jar",".js") );
				//System.out.println( libJS.getPath() );
				if ( libJS.exists() )
				{
					String libJSDest = "libs" + File.separator + libJS.getName();
					File libJSDestFile = new File( bin, libJSDest );
					if ( libJSDestFile.exists() )
					{
						System.out.println( "Duplicate import!" );
					}
					try
					{
						Base.copyFile( libJS,
									   libJSDestFile );
						jsImports.add( libJSDest );

					} catch ( Exception se ) {
						se.printStackTrace();
					}
				}
			}
		}
	}

    // final prep and write to template.
	// getTemplateFile() is very important as it looks and preps
	// any custom templates present in the sketch folder.
    File templateFile = getTemplateFile();
    File htmlOutputFile = new File(bin, "index.html");

    Map<String, String> templateFields = new HashMap<String, String>();
    templateFields.put( "width", 		String.valueOf(wide) );
    templateFields.put( "height", 		String.valueOf(high) );
    templateFields.put( "sketch", 		sketch.getName() );
    templateFields.put( "description", 	getSketchDescription() );

	// generate an ID for the sketch to use with <canvas id="XXXX"></canvas>
	String sketchID = sketch.getName().replaceAll("[^a-zA-Z0-9]+", "").replaceAll("^[^a-zA-Z]+","");

	// add a handy method to read the generated sketchID
	String scriptFiles = "<script type=\"text/javascript\">\n";

	scriptFiles += "// convenience function to get the id attribute of generated sketch html element\n" +
				   "function getProcessingSketchId () { return '"+sketchID+"'; }\n";

//	ArrayList<String> addresses = jsServer.getInetAddresses();
//	int port = jsServer.getPort();

	// scriptFiles += "var getServerAddresses = function () {\nreturn [\n";
	// for ( String addr : addresses )
	// {
	// 	scriptFiles += "\"http://" + addr + ":" + port + "/\", \n";
	// }
	// scriptFiles += "];\n}\n";

	scriptFiles += "</script>\n";

	// add imports if any ...
	for ( String importScript : jsImports )
	{
		scriptFiles += "<script type=\"text/javascript\" src=\""+importScript+"\"></script>";
	}

	// main .pde file first
	String sourceFiles = "<a href=\"" + sketch.getName() + ".pde\">" +
                    					sketch.getName() + "</a> ";

	// add all other files (both types: .pde and .js)
	if ( sketchFolderFiles != null )
	{
		for ( String s : sketchFolderFiles )
		{
			sourceFiles += "<a href=\"" + s + "\">" + s + "</a> ";
			scriptFiles += "<script src=\""+ s +"\" type=\"text/javascript\"></script>\n";
		}
	}
	templateFields.put( "source", sourceFiles );
    templateFields.put( "scripts", scriptFiles );
	templateFields.put( "id", sketchID );

	// process template replace tokens with content
    try
	{
      writeTemplate(templateFile, htmlOutputFile, templateFields);

    } catch (IOException ioe) {
      final String msg = "There was a problem writing the html template " +
      		               "to the build folder.";
      Base.showWarning("A problem occured during the build", msg, ioe);
      return false;
    }

    // finally, add files processing.js
	String[] defaultJSFiles = new String[]{
		"processing.js" /*, "qrcode.js"*/
	};
	for ( String defaultJSFile : defaultJSFiles )
	{
	    try
		{
	      Base.copyFile( sketch.getMode().getContentFile(
							TEMPLATE_FOLDER_NAME + File.separator + defaultJSFile
						 ),
	                     new File( bin, defaultJSFile )
		  );

	    } catch (IOException ioe) {
	      final String msg = "There was a problem copying " +defaultJSFile+ " to the " +
	                         "build folder. You will have to manually add " +
	                         defaultJSFile +" to the build folder before the sketch " +
	                         "will run.";
	      Base.showWarning( "There was a problem writing to the build folder", msg, ioe);
	      //return false;
	    }
	}

    return true;
  }

  /**
   *  Find and return the template HTML file to use. This also checks for custom
   *  templates that might be living in the sketch folder. If such a "template"
   *  folder exists then it's contents will be copied over to "web-export" and
   *  it's template.html will be used as template.
   */
  private File getTemplateFile ()
  {
	File sketchFolder = sketch.getFolder();
	File customTemplateFolder = new File( sketchFolder, TEMPLATE_FOLDER_NAME );
	if ( customTemplateFolder.exists() &&
		 customTemplateFolder.isDirectory() &&
		 customTemplateFolder.canRead() )
	{
		File appletJsFolder = new File( sketchFolder, EXPORTED_FOLDER_NAME );

		try {
			//TODO: this is potentially dangerous as it might override files in "web-export"
			Base.copyDir( customTemplateFolder, appletJsFolder );
			if ( !(new File( appletJsFolder, TEMPLATE_FILE_NAME )).delete() )
			{
				// ignore?
			}
			return new File( customTemplateFolder, TEMPLATE_FILE_NAME );
		} catch ( Exception e ) {
			String msg = "";
			Base.showWarning("There was a problem copying your custom template folder", msg, e);
			return sketch.getMode().getContentFile(
				TEMPLATE_FOLDER_NAME + File.separator + TEMPLATE_FILE_NAME
			);
		}
	}
	else
    	return sketch.getMode().getContentFile(
			TEMPLATE_FOLDER_NAME + File.separator + TEMPLATE_FILE_NAME
		);
  }


  /**
   * Collects the sketch code and runs it by the Java-mode preprocessor
   * to fish for errors.
   *
   * @param bin the output folder
   *
   * @see processing.mode.java.JavaBuild#preprocess(java.io.File)
   */
  public void preprocess ( File bin ) throws IOException, SketchException
  {
	// COLLECT .pde FILES INTO ONE,
    // essentially... cat sketchFolder/*.pde > bin/sketchname.pde

    StringBuffer bigCode = new StringBuffer();
    int bigCount = 0;
    for (SketchCode sc : sketch.getCode()) {
      if (sc.isExtension("pde")) {
        sc.setPreprocOffset(bigCount);
        bigCode.append(sc.getProgram());
        bigCode.append('\n');
        bigCount += sc.getLineCount();
      }
    }

    if (!bin.exists()) {
      bin.mkdirs();
    }
    File bigFile = new File(bin, sketch.getName() + ".pde");
	String bigCodeContents = bigCode.toString();
    Base.saveFile( bigCodeContents, bigFile );

	// RUN THROUGH JAVA-MODE PREPROCESSOR,
	// some minor changes made since we are not running the result
	// but are only interested in any possible errors that may
	// surface

	PdePreprocessor preprocessor = new PdePreprocessor( sketch.getName() );
	//PreprocessorResult result;

    try
	{
      File outputFolder = sketch.makeTempFolder();
      final File java = new File( outputFolder, sketch.getName() + ".java" );
      final PrintWriter stream = new PrintWriter( new FileWriter(java) );
      try {
        /*result =*/ preprocessor.write( stream, bigCodeContents, null );
      } finally {
        stream.close();
      }

    } catch (FileNotFoundException fnfe) {
      fnfe.printStackTrace();
      String msg = "Build folder disappeared or could not be written";
      throw new SketchException(msg);

    } catch (antlr.RecognitionException re) {
      // re also returns a column that we're not bothering with for now

      // first assume that it's the main file
      int errorLine = re.getLine() - 1;

      // then search through for anyone else whose preprocName is null,
      // since they've also been combined into the main pde.
      int errorFile = findErrorFile(errorLine);
      errorLine -= sketch.getCode(errorFile).getPreprocOffset();

      String msg = re.getMessage();

      if (msg.equals("expecting RCURLY, found 'null'")) {
        // This can be a problem since the error is sometimes listed as a line
        // that's actually past the number of lines. For instance, it might
        // report "line 15" of a 14 line program. Added code to highlightLine()
        // inside Editor to deal with this situation (since that code is also
        // useful for other similar situations).
        throw new SketchException("Found one too many { characters " +
                                  "without a } to match it.",
                                  errorFile, errorLine, re.getColumn());
      }

      if (msg.indexOf("expecting RBRACK") != -1) {
        System.err.println(msg);
        throw new SketchException("Syntax error, " +
                                  "maybe a missing ] character?",
                                  errorFile, errorLine, re.getColumn());
      }

      if (msg.indexOf("expecting SEMI") != -1) {
        System.err.println(msg);
        throw new SketchException("Syntax error, " +
                                  "maybe a missing semicolon?",
                                  errorFile, errorLine, re.getColumn());
      }

      if (msg.indexOf("expecting RPAREN") != -1) {
        System.err.println(msg);
        throw new SketchException("Syntax error, " +
                                  "maybe a missing right parenthesis?",
                                  errorFile, errorLine, re.getColumn());
      }

      if (msg.indexOf("preproc.web_colors") != -1) {
        throw new SketchException("A web color (such as #ffcc00) " +
                                  "must be six digits.",
                                  errorFile, errorLine, re.getColumn(), false);
      }

      //System.out.println("msg is " + msg);
      throw new SketchException(msg, errorFile,
                                errorLine, re.getColumn());

    } catch (antlr.TokenStreamRecognitionException tsre) {
      // while this seems to store line and column internally,
      // there doesn't seem to be a method to grab it..
      // so instead it's done using a regexp

      // TODO not tested since removing ORO matcher.. ^ could be a problem
      String mess = "^line (\\d+):(\\d+):\\s";

      String[] matches = PApplet.match(tsre.toString(), mess);
      if (matches != null) {
        int errorLine = Integer.parseInt(matches[1]) - 1;
        int errorColumn = Integer.parseInt(matches[2]);

        int errorFile = 0;
        for (int i = 1; i < sketch.getCodeCount(); i++) {
          SketchCode sc = sketch.getCode(i);
          if (sc.isExtension("pde") &&
              (sc.getPreprocOffset() < errorLine)) {
            errorFile = i;
          }
        }
        errorLine -= sketch.getCode(errorFile).getPreprocOffset();

        throw new SketchException(tsre.getMessage(),
                                  errorFile, errorLine, errorColumn);

      } else {
        // this is bad, defaults to the main class.. hrm.
        String msg = tsre.toString();
        throw new SketchException(msg, 0, -1, -1);
      }

    } catch (SketchException pe) {
      // RunnerExceptions are caught here and re-thrown, so that they don't
      // get lost in the more general "Exception" handler below.
      throw pe;

    } catch (Exception ex) {
      // TODO better method for handling this?
      System.err.println("Uncaught exception type:" + ex.getClass());
      ex.printStackTrace();
      throw new SketchException(ex.toString());
    }
  }

  /**
   * Copied from JavaBuild as it is protected there.
   * @see processing.mode.java.JavaBuild#findErrorFile(int)
   */
  protected int findErrorFile(int errorLine) {
    for (int i = sketch.getCodeCount() - 1; i > 0; i--) {
      SketchCode sc = sketch.getCode(i);
      if (sc.isExtension("pde") && (sc.getPreprocOffset() < errorLine)) {
        // keep looping until the errorLine is past the offset
        return i;
      }
    }
    return 0;  // i give up
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
   * Export the sketch to the default "web-export" folder.
   * @return success of the operation
   */
  public boolean export() throws IOException, SketchException
  {
    File webExport = new File(sketch.getFolder(), EXPORTED_FOLDER_NAME);
    return build( webExport );
  }
}
