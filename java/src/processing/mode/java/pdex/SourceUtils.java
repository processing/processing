package processing.mode.java.pdex;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import processing.mode.java.pdex.SourceMapping.Edit;
import processing.mode.java.preproc.PdePreprocessor;

public class SourceUtils {


  public static final Pattern IMPORT_REGEX =
      Pattern.compile("(?:^|;)\\s*(import\\s+(?:(static)\\s+)?((?:\\w+\\s*\\.)*)\\s*(\\S+)\\s*;)",
                      Pattern.MULTILINE | Pattern.DOTALL);

  public static final Pattern IMPORT_REGEX_NO_KEYWORD =
      Pattern.compile("^\\s*((?:(static)\\s+)?((?:\\w+\\s*\\.)*)\\s*(\\S+))",
                      Pattern.MULTILINE | Pattern.DOTALL);

  public static List<Edit> parseProgramImports(CharSequence source,
                                               List<ImportStatement> outImports) {

    List<Edit> result = new ArrayList<>();

    Matcher matcher = IMPORT_REGEX.matcher(source);
    while (matcher.find()) {
      String piece = matcher.group(1);
      ImportStatement is = ImportStatement.parse(matcher.toMatchResult());
      outImports.add(is);
      int len = piece.length();
      int idx = matcher.start(1);
      // Remove the import from the main program
      // Substitute with white spaces
      result.add(Edit.move(idx, len, 0));
      result.add(Edit.insert(0, "\n"));
    }
    return result;
  }



  public static final Pattern TYPE_CONSTRUCTOR_REGEX =
      Pattern.compile("(?:^|\\W)(int|char|float|boolean|byte)(?:\\s*\\()",
                      Pattern.MULTILINE);

  public static List<Edit> replaceTypeConstructors(CharSequence source) {

    List<Edit> result = new ArrayList<>();

    Matcher matcher = TYPE_CONSTRUCTOR_REGEX.matcher(source);
    while (matcher.find()) {
      String match = matcher.group(1);
      int offset = matcher.start(1);
      int length = match.length();
      String replace = "PApplet.parse"
          + Character.toUpperCase(match.charAt(0))
          + match.substring(1);
      result.add(Edit.replace(offset, length, replace));
    }

    return result;
  }



  public static final Pattern HEX_LITERAL_REGEX =
      Pattern.compile("(?:^|\\W)(#[A-Fa-f0-9]{6})(?:\\W|$)");

  public static List<Edit> replaceHexLiterals(CharSequence source) {
    // Find all #[webcolor] and replace with 0xff[webcolor]
    // Should be 6 digits only.
    List<Edit> result = new ArrayList<>();

    Matcher matcher = HEX_LITERAL_REGEX.matcher(source);
    while (matcher.find()) {
      int offset = matcher.start(1);
      result.add(Edit.replace(offset, 1, "0xff"));
    }

    return result;
  }



  public static List<Edit> insertImports(List<ImportStatement> imports) {
    List<Edit> result = new ArrayList<>();
    for (ImportStatement imp : imports) {
      result.add(Edit.insert(0, imp.getFullSourceLine() + "\n"));
    }
    return result;
  }

  public static List<Edit> wrapSketch(PdePreprocessor.Mode mode, String className, int sourceLength) {

    List<Edit> edits = new ArrayList<>();

    StringBuilder b = new StringBuilder();

    // Header
    if (mode != PdePreprocessor.Mode.JAVA) {
      b.append("\npublic class ").append(className).append(" extends PApplet {\n");
      if (mode == PdePreprocessor.Mode.STATIC) {
        b.append("public void setup() {\n");
      }
    }

    edits.add(Edit.insert(0, b.toString()));

    // Reset builder
    b.setLength(0);

    // Footer
    if (mode != PdePreprocessor.Mode.JAVA) {
      if (mode == PdePreprocessor.Mode.STATIC) {
        // no noLoop() here so it does not tell you
        // "can't invoke noLoop() on obj" when you type "obj."
        b.append("\n}");
      }
      b.append("\n}\n");
    }

    edits.add(Edit.insert(sourceLength, b.toString()));

    return edits;
  }


  public static List<Edit> addPublicToTopLevelMethods(CompilationUnit cu) {
    List<Edit> edits = new ArrayList<>();

    // Add public modifier to top level methods
    for (Object node : cu.types()) {
      if (node instanceof TypeDeclaration) {
        TypeDeclaration type = (TypeDeclaration) node;
        for (MethodDeclaration method : type.getMethods()) {
          if (method.modifiers().isEmpty() && !method.isConstructor()) {
            edits.add(Edit.insert(method.getStartPosition(), "public "));
          }
        }
      }
    }

    return edits;
  }

  public static List<Edit> replaceColorAndFixFloats(CompilationUnit cu) {
    final List<Edit> edits = new ArrayList<>();

    // Walk the tree, replace "color" with "int" and add 'f' to floats
    cu.accept(new ASTVisitor() {
      @Override
      public boolean visit(SimpleType node) {
        if ("color".equals(node.getName().toString())) {
          edits.add(Edit.replace(node.getStartPosition(), node.getLength(), "int"));
        }
        return super.visit(node);
      }

      @Override
      public boolean visit(NumberLiteral node) {
        String s = node.getToken().toLowerCase();
        if (!s.endsWith("f") && !s.endsWith("d") && (s.contains(".") || s.contains("e"))) {
          edits.add(Edit.insert(node.getStartPosition() + node.getLength(), "f"));
        }
        return super.visit(node);
      }
    });

    return edits;
  }

  /**
   * Replaces non-ascii characters with their unicode escape sequences and
   * stuff. Used as it is from
   * processing.src.processing.mode.java.preproc.PdePreprocessor
   *
   * @param program
   *            - Input String containing non ascii characters
   * @return String - Converted String
   */
  public static String substituteUnicode(String program) {
    StringBuilder sb = new StringBuilder(program);
    substituteUnicode(sb);
    return sb.toString();
  }

  public static void substituteUnicode(StringBuilder p) {
    // check for non-ascii chars (these will be/must be in unicode format)
    int unicodeCount = 0;
    for (int i = 0; i < p.length(); i++) {
      if (p.charAt(i) > 127) {
        unicodeCount++;
      }
    }
    if (unicodeCount == 0) {
      return;
    }

    StringBuilder p2 = new StringBuilder(p.length() + 4 * unicodeCount);

    // if non-ascii chars are in there, convert to unicode escapes
    // add unicodeCount * 5.. replacing each unicode char
    // with six digit uXXXX sequence (xxxx is in hex)
    // (except for nbsp chars which will be a replaced with a space)
    for (int i = 0; i < p.length(); i++) {
      int c = p.charAt(i);
      if (c < 128) {
        p2.append(c);
      } else if (c == 160) { // unicode for non-breaking space
        p2.append(' ');
      } else if (c >= 128){
        p2.append("\\u").append(String.format("%04X", c));
      }
    }

    p.setLength(0);
    p.append(p2);
  }


  static public String scrubCommentsAndStrings(String p) {
    StringBuilder sb = new StringBuilder(p);
    scrubCommentsAndStrings(sb);
    return sb.toString();
  }

  /**
   * Replace all commented portions of a given String as spaces.
   * Utility function used here and in the preprocessor.
   */
  static public void scrubCommentsAndStrings(StringBuilder p) {
    // Track quotes to avoid problems with code like: String t = "*/*";
    // http://code.google.com/p/processing/issues/detail?id=1435
    boolean insideQuote = false;

    int length = p.length();

    int index = 0;
    while (index < length) {
      // for any double slash comments, ignore until the end of the line
      if (!insideQuote &&
          (p.charAt(index) == '/') &&
          (index < length - 1) &&
          (p.charAt(index+1) == '/')) {
        p.setCharAt(index++, ' ');
        p.setCharAt(index++, ' ');
        while ((index < length) &&
            (p.charAt(index) != '\n')) {
          p.setCharAt(index++, ' ');
        }

        // check to see if this is the start of a new multiline comment.
        // if it is, then make sure it's actually terminated somewhere.
      } else if (!insideQuote &&
          (p.charAt(index) == '/') &&
          (index < length - 1) &&
          (p.charAt(index+1) == '*')) {
        p.setCharAt(index++, ' ');
        p.setCharAt(index++, ' ');
        boolean endOfRainbow = false;
        while (index < length - 1) {
          if ((p.charAt(index) == '*') && (p.charAt(index+1) == '/')) {
            p.setCharAt(index++, ' ');
            p.setCharAt(index++, ' ');
            endOfRainbow = true;
            break;

          } else {
            // continue blanking this area
            p.setCharAt(index++, ' ');
          }
        }
        if (!endOfRainbow) {
          throw new RuntimeException("Missing the */ from the end of a " +
                                         "/* comment */");
        }
      } else {
        boolean isChar = index > 0 && p.charAt(index-1) == '\\';
        if ((insideQuote && p.charAt(index) == '\n') ||
            (p.charAt(index) == '"' && !isChar)) {
          insideQuote = !insideQuote;
          index++;
        } else if (insideQuote && index < p.length() - 1 &&
            p.charAt(index) == '\\' && p.charAt(index+1) == '"') {
          p.setCharAt(index, ' ');
          p.setCharAt(index + 1, ' ');
          index++;
        } else if (insideQuote) {
          p.setCharAt(index, ' ');
          index++;
        } else {  // any old character, move along
          index++;
        }
      }
    }
  }
}