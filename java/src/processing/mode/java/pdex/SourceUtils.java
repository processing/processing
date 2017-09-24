package processing.mode.java.pdex;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleType;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import processing.mode.java.pdex.TextTransform.Edit;
import processing.mode.java.preproc.PdePreprocessor;

public class SourceUtils {


  public static final Pattern IMPORT_REGEX =
      Pattern.compile("(?:^|;)\\s*(import\\s+(?:(static)\\s+)?((?:\\w+\\s*\\.)*)\\s*(\\S+)\\s*;)",
                      Pattern.MULTILINE | Pattern.DOTALL);

  public static final Pattern IMPORT_REGEX_NO_KEYWORD =
      Pattern.compile("^\\s*((?:(static)\\s+)?((?:\\w+\\s*\\.)*)\\s*(\\S+))",
                      Pattern.MULTILINE | Pattern.DOTALL);

  public static List<ImportStatement> parseProgramImports(CharSequence source) {
    List<ImportStatement> result = new ArrayList<>();
    Matcher matcher = IMPORT_REGEX.matcher(source);
    while (matcher.find()) {
      ImportStatement is = ImportStatement.parse(matcher.toMatchResult());
      result.add(is);
    }
    return result;
  }

  public static List<Edit> parseProgramImports(CharSequence source,
                                               List<ImportStatement> outImports) {
    List<Edit> result = new ArrayList<>();
    Matcher matcher = IMPORT_REGEX.matcher(source);
    while (matcher.find()) {
      ImportStatement is = ImportStatement.parse(matcher.toMatchResult());
      outImports.add(is);
      int idx = matcher.start(1);
      int len = matcher.end(1) - idx;
      // Remove the import from the main program
      // Substitute with white spaces
      result.add(Edit.move(idx, len, 0));
      result.add(Edit.insert(0, "\n"));
    }
    return result;
  }



  // Positive lookahead and lookbehind are needed to match all type constructors
  // in code like `int(byte(245))` where first bracket matches as last
  // group in "^int(" but also as a first group in "(byte(". Lookahead and
  // lookbehind won't consume the shared character.
  public static final Pattern TYPE_CONSTRUCTOR_REGEX =
      Pattern.compile("(?<=^|\\W)(int|char|float|boolean|byte)(?=\\s*\\()",
                      Pattern.MULTILINE);

  public static List<Edit> replaceTypeConstructors(CharSequence source) {

    List<Edit> result = new ArrayList<>();

    Matcher matcher = TYPE_CONSTRUCTOR_REGEX.matcher(source);
    while (matcher.find()) {
      String match = matcher.group(1);
      int offset = matcher.start(1);
      int length = match.length();
      result.add(Edit.insert(offset, "PApplet."));
      String replace = "parse"
          + Character.toUpperCase(match.charAt(0)) + match.substring(1);
      result.add(Edit.replace(offset, length, replace));
    }

    return result;
  }



  public static final Pattern HEX_LITERAL_REGEX =
      Pattern.compile("(?<=^|\\W)(#[A-Fa-f0-9]{6})(?=\\W|$)");

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


  // Verifies that whole input String is floating point literal. Can't be used for searching.
  // https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-DecimalFloatingPointLiteral
  public static final Pattern FLOATING_POINT_LITERAL_VERIFIER;
  static {
    final String DIGITS = "(?:[0-9]|[0-9][0-9_]*[0-9])";
    final String EXPONENT_PART = "(?:[eE][+-]?" + DIGITS + ")";
    FLOATING_POINT_LITERAL_VERIFIER = Pattern.compile(
        "(?:^" + DIGITS + "\\." + DIGITS + "?" + EXPONENT_PART + "?[fFdD]?$)|" +
            "(?:^\\." + DIGITS + EXPONENT_PART + "?[fFdD]?$)|" +
            "(?:^" + DIGITS + EXPONENT_PART + "[fFdD]?$)|" +
            "(?:^" + DIGITS + EXPONENT_PART + "?[fFdD]$)");
  }

  // Mask to quickly resolve whether there are any access modifiers present
  private static final int ACCESS_MODIFIERS_MASK =
      Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;

  public static List<Edit> preprocessAST(CompilationUnit cu) {
    final List<Edit> edits = new ArrayList<>();

    // Walk the tree
    cu.accept(new ASTVisitor() {
      @Override
      public boolean visit(SimpleType node) {
        // replace "color" with "int"
        if ("color".equals(node.getName().toString())) {
          edits.add(Edit.replace(node.getStartPosition(), node.getLength(), "int"));
        }
        return super.visit(node);
      }

      @Override
      public boolean visit(NumberLiteral node) {
        // add 'f' to floats
        String s = node.getToken().toLowerCase();
        if (FLOATING_POINT_LITERAL_VERIFIER.matcher(s).matches() && !s.endsWith("f") && !s.endsWith("d")) {
          edits.add(Edit.insert(node.getStartPosition() + node.getLength(), "f"));
        }
        return super.visit(node);
      }

      @Override
      public boolean visit(MethodDeclaration node) {
        // add 'public' to methods with default visibility
        int accessModifiers = node.getModifiers() & ACCESS_MODIFIERS_MASK;
        if (accessModifiers == 0) {
          edits.add(Edit.insert(node.getStartPosition(), "public "));
        }
        return super.visit(node);
      }
    });

    return edits;
  }


  public static final Pattern COLOR_TYPE_REGEX =
      Pattern.compile("(?:^|^\\p{javaJavaIdentifierPart})(color)\\s(?!\\s*\\()",
                      Pattern.MULTILINE | Pattern.UNICODE_CHARACTER_CLASS);

  public static List<Edit> replaceColorRegex(CharSequence source) {
    final List<Edit> edits = new ArrayList<>();

    Matcher matcher = COLOR_TYPE_REGEX.matcher(source);
    while (matcher.find()) {
      int offset = matcher.start(1);
      edits.add(Edit.replace(offset, 5, "int"));
    }

    return edits;
  }


  public static final Pattern NUMBER_LITERAL_REGEX =
      Pattern.compile("[-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?");

  public static List<Edit> fixFloatsRegex(CharSequence source) {
    final List<Edit> edits = new ArrayList<>();

    Matcher matcher = NUMBER_LITERAL_REGEX.matcher(source);
    while (matcher.find()) {
      int offset = matcher.start();
      int end = matcher.end();
      String group = matcher.group().toLowerCase();
      boolean isFloatingPoint = group.contains(".") || group.contains("e");
      boolean hasSuffix = end < source.length() &&
          Character.toLowerCase(source.charAt(end)) != 'f' &&
          Character.toLowerCase(source.charAt(end)) != 'd';
      if (isFloatingPoint && !hasSuffix) {
        edits.add(Edit.insert(offset, "f"));
      }
    }

    return edits;
  }


  static public String scrubCommentsAndStrings(String p) {
    StringBuilder sb = new StringBuilder(p);
    scrubCommentsAndStrings(sb);
    return sb.toString();
  }

  static public void scrubCommentsAndStrings(StringBuilder p) {

    final int length = p.length();

    final int OUT = 0;
    final int IN_BLOCK_COMMENT = 1;
    final int IN_EOL_COMMENT = 2;
    final int IN_STRING_LITERAL = 3;
    final int IN_CHAR_LITERAL = 4;

    int blockStart = -1;

    int prevState = OUT;
    int state = OUT;

    for (int i = 0; i <= length; i++) {
      char ch = (i < length) ? p.charAt(i) : 0;
      char pch = (i == 0) ? 0 : p.charAt(i-1);
      // Get rid of double backslash immediately, otherwise
      // the second backslash incorrectly triggers a new escape sequence
      if (pch == '\\' && ch == '\\') {
        p.setCharAt(i-1, ' ');
        p.setCharAt(i, ' ');
        pch = ' ';
        ch = ' ';
      }
      switch (state) {
        case OUT:
          switch (ch) {
            case '\'': state = IN_CHAR_LITERAL; break;
            case '"': state = IN_STRING_LITERAL; break;
            case '*': if (pch == '/') state = IN_BLOCK_COMMENT; break;
            case '/': if (pch == '/') state = IN_EOL_COMMENT; break;
          }
          break;
        case IN_BLOCK_COMMENT:
          if (pch == '*' && ch == '/' && (i - blockStart) > 0) {
            state = OUT;
          }
          break;
        case IN_EOL_COMMENT:
          if (ch == '\r' || ch == '\n') {
            state = OUT;
          }
          break;
        case IN_STRING_LITERAL:
          if ((pch != '\\' && ch == '"') || ch == '\r' || ch == '\n') {
            state = OUT;
          }
          break;
        case IN_CHAR_LITERAL:
          if ((pch != '\\' && ch == '\'') || ch == '\r' || ch == '\n') {
            state = OUT;
          }
          break;
      }

      // Terminate ongoing block at last char
      if (i == length) {
        state = OUT;
      }

      // Handle state changes
      if (state != prevState) {
        if (state != OUT) {
          // Entering block
          blockStart = i + 1;
        } else {
          // Exiting block
          int blockEnd = i;
          if (prevState == IN_BLOCK_COMMENT && i < length) blockEnd--; // preserve star in '*/'
          for (int j = blockStart; j < blockEnd; j++) {
            char c = p.charAt(j);
            if (c != '\n' && c != '\r') p.setCharAt(j, ' ');
          }
        }
      }

      prevState = state;
    }

  }


  // TODO: move this to a better place when JavaBuild starts using JDT and we
  //       don't need to check errors at two different places [jv 2017-09-19]
  /**
   * Checks a single code fragment (such as a tab) for non-matching braces.
   * Broken out to allow easy use in JavaBuild.
   * @param c Program code scrubbed of comments and string literals.
   * @param start Start index, inclusive.
   * @param end End index, exclusive.
   * @return {@code int[4]} Depth at which the loop stopped, followed by the
   *         line number, column, and string index (within the range) at which
   *         an error was found, if any.
   */
  static public int[] checkForMissingBraces(CharSequence c, int start, int end) {
    int depth = 0;
    int lineNumber = 0;
    int lineStart = start;
    for (int i = start; i < end; i++) {
      char ch = c.charAt(i);
      switch (ch) {
        case '{':
          depth++;
          break;
        case '}':
          depth--;
          break;
        case '\n':
          lineNumber++;
          lineStart = i;
          break;
      }
      if (depth < 0) {
        return new int[] {depth, lineNumber, i - lineStart, i - start};
      }
    }
    return new int[] {depth, lineNumber - 1, end - lineStart - 2, end - start - 2};
  }
}
