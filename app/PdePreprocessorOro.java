// -*- Mode: JDE; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 2 -*-

// The existing ORO-based preprocessing code lives here, and can
// someday go away.

import com.oroinc.text.regex.*;
import java.io.*;

public class PdePreprocessorOro extends PdePreprocessor {
  //static final String EXTENDS = "extends BApplet ";
  //static final String EXTENDS_KJC = "extends KjcApplet ";

  static final String applet_imports[] = {
    "java.applet", "java.awt", "java.awt.image", "java.awt.event",
    "java.io", "java.net", "java.text", "java.util", "java.util.zip"
  };

  static final String application_imports[] = {
    "java.applet", "java.awt", "java.awt.image", "java.awt.event",
    "java.io", "java.net", "java.text", "java.util", "java.util.zip",
    "javax.comm",

    // if jdk14 defined, jdk13 will be as well
#ifdef JDK13
    "javax.sound.midi", "javax.sound.midi.spi",
    "javax.sound.sampled", "javax.sound.sampled.spi",
#endif

#ifdef JDK14
    "javax.xml.parsers", "javax.xml.transform", 
    "javax.xml.transform.dom", "javax.xml.transform.sax",
    "javax.xml.transform.stream", "org.xml.sax",
    "org.xml.sax.ext", "org.xml.sax.helpers"
#endif
  };

  String program;
  String buildPath;
  boolean usingExternal; // use an external process to display the applet?

  String tempClass;
  String tempFilename;
  String tempClassFilename;

  public PdePreprocessorOro(String program, String buildPath) {
    this.program = program;
    this.buildPath = buildPath;

    usingExternal = PdeBase.getBoolean("play.external", false);
  }

  static final int BEGINNER     = 0;
  static final int INTERMEDIATE = 1;
  static final int ADVANCED     = 2;

  // writes .java file into buildPath
  public String writeJava(String name, boolean extendsNormal,
                          boolean exporting) {
    String extendsWhat = extendsNormal ? "BApplet" : "BAppletGL";

    try {
      int programType = BEGINNER;

      // remove (encode) comments temporarily
      program = commentsCodec(program /*, true*/);

      // insert 'f' for all floats
      // shouldn't substitute f's for: "Univers76.vlw.gz";
      if (PdeBase.getBoolean("compiler.substitute_f", true)) {
        /*
          a = 0.2 * 3
          (3.)
          (.3 * 6)
          (.30*7)
          float f = 0.3; 
          fill(0.3, 0.2, 0.1);

          next to white space \s or math ops +-/*() 
          or , on either side, 
          followed by ; (might as well on either side)
   
          // allow 3. to work (also allows x.x too)
          program = substipoot(program, "(\\d+\\.\\d*)(\\D)", "$1f$2");
          program = substipoot(program, "(\\d+\\.\\d*)ff", "$1f");

          // allow .3 to work (also allows x.x)
          program = substipoot(program, "(\\d*\\.\\d+)(\\D)", "$1f$2");
          program = substipoot(program, "(\\d*\\.\\d+)ff", "$1f");
        */

        program = substipoot(program, "([\\s\\,\\;\\+\\-\\/\\*\\(\\)])(\\d+\\.\\d*)([\\s\\,\\;\\+\\-\\/\\*\\(\\)])", "$1$2f$3");
        program = substipoot(program, "([\\s\\,\\;\\+\\-\\/\\*\\(\\)])(\\d*\\.\\d+)([\\s\\,\\;\\+\\-\\/\\*\\(\\)])", "$1$2f$3");
      }

      // allow int(3.75) instead of just (int)3.75
      if (PdeBase.getBoolean("compiler.enhanced_casting", true)) {
        program = substipoot(program, "([^A-Za-z0-9_])byte\\((.*)\\)", "$1(byte)($2)");
        program = substipoot(program, "([^A-Za-z0-9_])char\\((.*)\\)", "$1(char)($2)");
        program = substipoot(program, "([^A-Za-z0-9_])int\\((.*)\\)", "$1(int)($2)");
        program = substipoot(program, "([^A-Za-z0-9_])float\\((.*)\\)", "$1(float)($2)");
      }

      if (PdeBase.getBoolean("compiler.color_datatype", true)) {
        // so that regexp works correctly in this strange edge case
        if (program.indexOf("color") == 0) program = " " + program;
        // swap 'color' with 'int' when used as a datatype
        program = substipoot(program, 
                             "([;\\s\\(])color([\\s\\[])", "$1int$2");
        // had to add ( at beginning for addPixel(color c...)
        //"([;\\s])color([\\s\\[])", "$1int$2");
        // had to add [ to that guy for color[] stuff
        //"([;\\s])color([\\s])", "$1int$2");
        //"([^A-Za-z0-9_.])color([^A-Za-z0-9_\\(.])", "$1int$2");
        // color(something) like int() and the rest is no good
        // because there is already a function called 'color' in BGraphics
        //program = substipoot(program, "([^A-Za-z0-9_])color\\((.*)\\)", "$1(int)($2)");
      }

      if (PdeBase.getBoolean("compiler.inline_web_colors", true)) {
        // convert "= #cc9988" into "= 0xffcc9988"
        //program = substipoot(program, "(=\\s*)\\#([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])", "$1 0xff$2$3$4");
        //program = substipoot(program, "#([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([;\\s])", "0xff$1$2$3$4");
        //program = substipoot(program, "#([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([;\\s])", "0x$4$1$2$3$5");
        program = substipoot(program, "#([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])", "0xff$1$2$3");
      }

      if ((program.indexOf("void setup()") != -1) ||
          (program.indexOf("void loop()") != -1) ||
          (program.indexOf("void draw()") != -1)) {
        programType = INTERMEDIATE;
      }

      int index = program.indexOf("public class");
      if (index != -1) {
        programType = ADVANCED;
        // kjc will get pissed off if i call the .java file
        // something besides the name of the class.. so here goes
        String s = program.substring(index + "public class".length()).trim();
        index = s.indexOf(' ');
        name = s.substring(0, index);
        tempClass = name;

        // and we're running inside 
        // no longer necessary, i think, since kjcapplet is gone
        /*
        if (!exporting) {
          index = program.indexOf(extendsWhat); // ...and extends BApplet
          if (index != -1) {  // just extends object
            String left = program.substring(0, index);
            String right = program.substring(index + extendsWhat.length());
            // replace with 'extends KjcApplet'
            //program = left + ((usingExternal) ? EXTENDS : EXTENDS_KJC) + right;
            program = left + extendsWhat + right;
          }
        }
        */
      }
      tempFilename = name + ".java";
      tempClassFilename = name + ".class";

      PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(buildPath + File.separator + tempFilename))));

      //String eol = System.getProperties().getProperty("line.separator");

      if (programType < ADVANCED) {
        // spew out a bunch of java imports 
        if (!exporting) {  // if running in environment, or exporting an app
          for (int i = 0; i < application_imports.length; i++) {
            writer.print("import " + application_imports[i] + ".*; ");
          }

        } else {  // exporting an applet
          for (int i = 0; i < applet_imports.length; i++) {
            writer.println("import " + applet_imports[i] + ".*; ");
            //writer.print("import " + applet_imports[i] + ".*; ");
            //if (!kjc) writer.println();
          }
        }

        // add serial if running inside pde
        //if (kjc) writer.print("import javax.comm.*;");
        if (exporting) writer.println();

        writer.print("public class " + name + " extends " +
                     extendsWhat + " {");
                     //((kjc && !usingExternal) ? 
                     //"KjcApplet" : "BApplet") + " {");
      }
      if (programType == BEGINNER) {
        if (exporting) writer.println();

        // hack so that the regexp below works
        //if (program.indexOf("size(") == 0) program = " " + program;
        if ((program.indexOf("size(") == 0) || 
            (program.indexOf("background(") == 0)) {
          program = " " + program;
        }


        PatternMatcher matcher = null;
        PatternCompiler compiler = null;
        Pattern pattern = null;
        Perl5Substitution subst = null;
        PatternMatcherInput input = null;


        ///////// grab (first) reference to size()


          matcher = new Perl5Matcher();
          compiler = new Perl5Compiler();
          try {
            pattern = 
              compiler.compile("[\\s\\;](size\\(\\s*\\d+,\\s*\\d+\\s*\\);)");
            //compiler.compile("^([^A-Za-z0-9_]+)(size\\(\\s*\\d+,\\s*\\d+\\s*\\);)");

          } catch (MalformedPatternException e){
            e.printStackTrace();
            //System.err.println("Bad pattern.");
            //System.err.println(e.getMessage());
            System.exit(1);
          }

          String sizeInfo = null;
          input = new PatternMatcherInput(program);
          if (matcher.contains(input, pattern)) {
            MatchResult result = matcher.getMatch();
            //int wide = Integer.parseInt(result.group(1).toString());
            //int high = Integer.parseInt(result.group(2).toString());
            //sizeInfo = "void setup() { " + result.group(0) + " } ";
            sizeInfo = result.group(0);

          } else {
            // no size() defined, make it default
            sizeInfo = "size(" + BApplet.DEFAULT_WIDTH + ", " + 
              BApplet.DEFAULT_HEIGHT + "); ";
          }


          // remove references to size()
          // this winds up removing every reference to size()
          // not really intended, but will help things work

          subst = new Perl5Substitution("", Perl5Substitution.INTERPOLATE_ALL);
          //subst = new Perl5Substitution("$1", Perl5Substitution.INTERPOLATE_ALL);
          program = Util.substitute(matcher, pattern, subst, program, 
                                    Util.SUBSTITUTE_ALL);


          /////////// grab (first) reference to background()

            matcher = new Perl5Matcher();
            compiler = new Perl5Compiler();
            try {
              pattern = 
                compiler.compile("[\\s\\;](background\\(.*\\);)");
              //[\\s\\;]
              //compiler.compile("([^A-Za-z0-9_]+)(background\\(.*\\);)");

            } catch (MalformedPatternException e){
              //System.err.println("Bad pattern.");
              //System.err.println(e.getMessage());
              e.printStackTrace();
              System.exit(1);
            }

            String backgroundInfo = "";
            input = new PatternMatcherInput(program);
            if (matcher.contains(input, pattern)) {
              MatchResult result = matcher.getMatch();
              //int wide = Integer.parseInt(result.group(1).toString());
              //int high = Integer.parseInt(result.group(2).toString());
              //sizeInfo = "void setup() { " + result.group(0) + " } ";
              backgroundInfo = result.group(0);

              //} else {
              // no size() defined, make it default
              //sizeInfo = "size(" + BApplet.DEFAULT_WIDTH + ", " + 
              //BApplet.DEFAULT_HEIGHT + "); ";
            }

            // remove references to size()
            // this winds up removing every reference to size()
            // not really intended, but will help things work
            subst = new Perl5Substitution("", Perl5Substitution.INTERPOLATE_ALL);
            //subst = new Perl5Substitution("$1", Perl5Substitution.INTERPOLATE_ALL);
            program = Util.substitute(matcher, pattern, subst, program, 
                                      Util.SUBSTITUTE_ALL);


            //////// spew out the size and background info

            writer.print("void setup() { ");
            writer.print(sizeInfo);
            writer.print(backgroundInfo);
            writer.print("} ");
            writer.print("void draw() {");
      }

      // decode comments to bring them back
      program = commentsCodec(program /*, false*/);

      // spew the actual program
      // this should really add extra indents, 
      // especially when not in kjc mode (!kjc == export)

      // things will be one line off if there's an error in the code
      if (exporting) writer.println();

      writer.println(program);
      //System.out.println(program);

      if (programType == BEGINNER) {
        writer.println("}");
      }
      if (programType < ADVANCED) {
        writer.print("}");
      }

      writer.flush();
      writer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
    return name;
  }

  protected String commentsCodec(String program /*, boolean encode*/) {
    // need to preprocess class to remove comments
    // so tthat they don't fool this crappy parsing below
    char p[] = program.toCharArray();
    char lastp = 0;
    boolean insideComment = false;
    boolean eolComment = false;
    boolean slash = false;
    boolean insideQuote = false;
    for (int i = 0; i < p.length; i++) {
      if (insideComment) {
        if (eolComment &&
            ((p[i] == '\r') || (p[i] == '\n'))) {
          insideComment = false;
          slash = false;

        } else if (!eolComment &&
                   (p[i] == '*') &&
                   (i != (p.length-1)) &&
                   (p[i+1] == '/')) {
          insideComment = false;
          slash = false;

        } else {
          //if ((p[i] > 32) && (p[i] < 127)) {
          if ((p[i] >= 48) && (p[i] < 128)) {
            p[i] = rotateTable[p[i]];
            //p[i] = encode ? encodeTable[p[i]] : decodeTable[p[i]];
          }
          //p[i] = ' ';
        }
      } else {  // not yet inside a comment
        if (insideQuote) {
          if ((p[i] == '\"') && (lastp != '\\')) {
            insideQuote = !insideQuote;
          } else {
            if ((p[i] >= 48) && (p[i] < 128)) {
              p[i] = rotateTable[p[i]];
              //p[i] = encode ? encodeTable[p[i]] : decodeTable[p[i]];
            }
          }

        } else {  // not inside a quote
          if (p[i] == '/') {
            if (slash) {
              insideComment = true;
              eolComment = true;
            } else {
              slash = true;
            }
          } else if (p[i] == '\"') {
            if (lastp != '\\') insideQuote = !insideQuote;

          } else if (p[i] == '*') {
            if (slash) {
              insideComment = true;
              eolComment = false;
            }
          } else {
            slash = false;
          }
        }
      }
      lastp = p[i];
    }
    //System.out.println(new String(p));
    return new String(p);
  }

  protected String substipoot(String what, String incoming, String outgoing) {
    PatternMatcher matcher = new Perl5Matcher();
    PatternCompiler compiler = new Perl5Compiler();
    Pattern pattern = null;

    try {
      pattern = compiler.compile(incoming);

    } catch (MalformedPatternException e){
      System.err.println("Bad pattern.");
      System.err.println(e.getMessage());
      System.exit(1);
    }

    Perl5Substitution subst = 
      new Perl5Substitution(outgoing, Perl5Substitution.INTERPOLATE_ALL);
    return Util.substitute(matcher, pattern, subst, what, 
                           Util.SUBSTITUTE_ALL);
  }


  //static char encodeTable[] = new char[127];
  //static char decodeTable[] = new char[127];
  static char rotateTable[] = new char[128];
  static {
    for (int i = 0; i < 80; i++) {
      rotateTable[i+48] = (char) (48 + ((i + 40) % 80));
    }

    /*
      int rot = (123 - 65) / 2;
      for (int i = 65; i < 123; i++) {
      rotateTable[i] = (char) (((i - 65 + rot) % (rot*2)) + 65); // : (char)i;
      }
    */

    //for (int i = 33; i < 127; i++) {
    //rotateTable[i] = //Character.isAlpha((char)i) ?
    //(char) (((i - 33 + rot) % 94) + 33) : (char)i;
      
    //encodeTable[i] = (char) (i+1);
    //decodeTable[i] = (char) (i-1);
    //encodeTable[i] = (char) (((i - 33 + rot) % 94) + 33);
    //decodeTable[i] = encodeTable[i];
    //encodeTable[i] = (char) (((i - 33 + rot) % 94) + 33);
    //decodeTable[i] = (char) (((i + 33 + rot) % 94) + 33);
    //System.out.println((int) decodeTable[i]);
    //}
  }
}
