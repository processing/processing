/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

package processing.mode.java.preproc;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Stack;
import processing.app.Preferences;
import processing.app.SketchException;
import processing.mode.java.preproc.PdeTokenTypes;
import antlr.CommonASTWithHiddenTokens;
import antlr.CommonHiddenStreamToken;
import antlr.collections.AST;

/* Based on original code copyright (c) 2003 Andy Tripp <atripp@comcast.net>.
 * shipped under GPL with permission.
 */

/**
 * PDEEmitter: A class that can take an ANTLR Java AST and produce
 * reasonably formatted Java code from it. To use it, create a
 * PDEEmitter object, call setOut() if you want to print to something
 * other than System.out, and then call print(), passing the
 * AST. Typically, the AST node that you pass would be the root of a
 * tree - the ROOT_ID node that represents a Java file.
 *
 * Modified March 2010 to support Java 5 type arguments and for loops by
 * @author Jonathan Feinberg &lt;jdf@pobox.com&gt;
 */

@SuppressWarnings("serial")
public class PdeEmitter implements PdeTokenTypes {
  private final PdePreprocessor pdePreprocessor;
  private final PrintWriter out;
  private final PrintStream debug = System.err;

  private final Stack<AST> stack = new Stack<AST>();
  private final static int ROOT_ID = 0;

  public PdeEmitter(final PdePreprocessor pdePreprocessor, final PrintWriter out) {
    this.pdePreprocessor = pdePreprocessor;
    this.out = out;
  }

  /**
   * Find a child of the given AST that has the given type
   * @returns a child AST of the given type. If it can't find a child of the
   *          given type, return null.
   */
  static private AST getChild(final AST ast, final int childType) {
    AST child = ast.getFirstChild();
    while (child != null) {
      if (child.getType() == childType) {
        // debug.println("getChild: found:" + name(ast));
        return child;
      }
      child = child.getNextSibling();
    }
    return null;
  }

  /**
   * Dump the list of hidden tokens linked to after the AST node passed in.
   * Most hidden tokens are dumped from this function.
   */
  private void dumpHiddenAfter(final AST ast) {
    dumpHiddenTokens(((CommonASTWithHiddenTokens) ast).getHiddenAfter());
  }

  /**
   * Dump the list of hidden tokens linked to before the AST node passed in.
   * The only time hidden tokens need to be dumped with this function is when
   * dealing parts of the tree where automatic tree construction was
   * turned off with the ! operator in the grammar file and the nodes were
   * manually constructed in such a way that the usual tokens don't have the
   * necessary hiddenAfter links.
   */
  private void dumpHiddenBefore(final AST ast) {

    antlr.CommonHiddenStreamToken child = null, parent = ((CommonASTWithHiddenTokens) ast)
        .getHiddenBefore();

    // if there aren't any hidden tokens here, quietly return
    //
    if (parent == null) {
      return;
    }

    // traverse back to the head of the list of tokens before this node
    do {
      child = parent;
      parent = child.getHiddenBefore();
    } while (parent != null);

    // dump that list
    dumpHiddenTokens(child);
  }

  /**
   * Dump the list of hidden tokens linked to from the token passed in.
   */
  private void dumpHiddenTokens(CommonHiddenStreamToken t) {
    for (; t != null; t = pdePreprocessor.getHiddenAfter(t)) {
      out.print(t.getText());
    }
  }

  /**
   * Print the children of the given AST
   * @param ast The AST to print
   * @returns true iff anything was printed
   */
  private boolean printChildren(final AST ast) throws SketchException {
    boolean ret = false;
    AST child = ast.getFirstChild();
    while (child != null) {
      ret = true;
      print(child);
      child = child.getNextSibling();
    }
    return ret;
  }

  /**
   * Tells whether an AST has any children or not.
   * @return true iff the AST has at least one child
   */
  static private boolean hasChildren(final AST ast) {
    return (ast.getFirstChild() != null);
  }

  /**
   * Gets the best node in the subtree for printing.  This really means
   * the next node which could potentially have hiddenBefore data.  It's
   * usually the first printable leaf, but not always.
   *
   * @param includeThisNode     Should this node be included in the search?
   *                            If false, only descendants are searched.
   *
   * @return the first printable leaf node in an AST
   */
  private AST getBestPrintableNode(final AST ast, final boolean includeThisNode) {
    AST child;

    if (includeThisNode) {
      child = ast;
    } else {
      child = ast.getFirstChild();
    }

    if (child != null) {

      switch (child.getType()) {

      // the following node types are printing nodes that print before
      // any children, but then also recurse over children.  So they
      // may have hiddenBefore chains that need to be printed first.  Many
      // statements and all unary expression types qualify.  Return these
      // nodes directly
      case CLASS_DEF:
      case ENUM_DEF:
      case LITERAL_if:
      case LITERAL_new:
      case LITERAL_for:
      case LITERAL_while:
      case LITERAL_do:
      case LITERAL_break:
      case LITERAL_continue:
      case LITERAL_return:
      case LITERAL_switch:
      case LITERAL_try:
      case LITERAL_throw:
      case LITERAL_synchronized:
      case LITERAL_assert:
      case BNOT:
      case LNOT:
      case INC:
      case DEC:
      case UNARY_MINUS:
      case UNARY_PLUS:
        return child;

        // Some non-terminal node types (at the moment, I only know of
        // MODIFIERS, but there may be other such types), can be
        // leaves in the tree but not have any children.  If this is
        // such a node, move on to the next sibling.
      case MODIFIERS:
        if (child.getFirstChild() == null) {
          return getBestPrintableNode(child.getNextSibling(), false);
        }
        // new jikes doesn't like fallthrough, so just duplicated here:
        return getBestPrintableNode(child, false);

      default:
        return getBestPrintableNode(child, false);
      }
    }

    return ast;
  }

  // Because the meanings of <, >, >>, and >>> are overloaded to support
  // type arguments and type parameters, we have to treat them
  // as copyable to hidden text (or else the following syntax,
  // such as (); and what not gets lost under certain circumstances
  //
  // Since they are copied to the hidden stream, you don't want
  // to print them explicitly; they come out in the dumpHiddenXXX methods.
  // -- jdf
  private static final BitSet OTHER_COPIED_TOKENS = new BitSet() {
    {
      set(LT);
      set(GT);
      set(SR);
      set(BSR);
    }
  };

  /**
   * Prints a binary operator
   */
  private void printBinaryOperator(final AST ast) throws SketchException {
    print(ast.getFirstChild());
    if (!OTHER_COPIED_TOKENS.get(ast.getType())) {
      out.print(ast.getText());
      dumpHiddenAfter(ast);
    }
    print(ast.getFirstChild().getNextSibling());
  }

  private void printMethodDef(final AST ast) throws SketchException {
    final AST modifiers = ast.getFirstChild();
    final AST typeParameters, type;
    if (modifiers.getNextSibling().getType() == TYPE_PARAMETERS) {
      typeParameters = modifiers.getNextSibling();
      type = typeParameters.getNextSibling();
    } else {
      typeParameters = null;
      type = modifiers.getNextSibling();
    }
    final AST methodName = type.getNextSibling();
//    if (methodName.getText().equals("main")) {
//      pdePreprocessor.setFoundMain(true);
//    }
    pdePreprocessor.addMethod(methodName.getText());
    printChildren(ast);
  }

  private void printIfThenElse(final AST literalIf) throws SketchException {
    out.print(literalIf.getText());
    dumpHiddenAfter(literalIf);

    final AST condition = literalIf.getFirstChild();
    print(condition); // the "if" condition: an EXPR

    // the "then" clause is either an SLIST or an EXPR
    final AST thenPath = condition.getNextSibling();
    print(thenPath);

    // optional "else" clause: an SLIST or an EXPR
    // what could be simpler?
    final AST elsePath = thenPath.getNextSibling();
    if (elsePath != null) {
      out.print("else");
      final AST bestPrintableNode = getBestPrintableNode(elsePath, true);
      dumpHiddenBefore(bestPrintableNode);
      final CommonHiddenStreamToken hiddenBefore =
        ((CommonASTWithHiddenTokens) elsePath).getHiddenBefore();
      if (elsePath.getType() == PdeTokenTypes.SLIST && elsePath.getNumberOfChildren() == 0 &&
          hiddenBefore == null) {
        out.print("{");
        final CommonHiddenStreamToken hiddenAfter =
          ((CommonASTWithHiddenTokens) elsePath).getHiddenAfter();
        if (hiddenAfter == null) {
          out.print("}");
        } else {
          dumpHiddenTokens(hiddenAfter);
        }
      } else {
        print(elsePath);
      }
    }
  }

  /**
   * Print the given AST. Call this function to print your PDE code.
   *
   * It works by making recursive calls to print children.
   * So the code below is one big "switch" statement on the passed AST type.
   */
  public void print(final AST ast) throws SketchException {
    if (ast == null) {
      return;
    }

    stack.push(ast);

    final AST child1 = ast.getFirstChild();
    AST child2 = null;
    AST child3 = null;
    if (child1 != null) {
      child2 = child1.getNextSibling();
      if (child2 != null) {
        child3 = child2.getNextSibling();
      }
    }

    switch (ast.getType()) {
    // The top of the tree looks like this:
    //  ROOT_ID  "Whatever.java"
    //   package
    //   imports
    //   class definition
    case ROOT_ID:
      dumpHiddenTokens(pdePreprocessor.getInitialHiddenToken());
      printChildren(ast);
      break;

    // supporting a "package" statement in a PDE program has
    // a bunch of issues with it that need to dealt in the compilation
    // code too, so this isn't actually tested.
    case PACKAGE_DEF:
      out.print("package");
      dumpHiddenAfter(ast);
      print(ast.getFirstChild());
      break;

    // IMPORT has exactly one child
    case IMPORT:
      out.print("import");
      dumpHiddenAfter(ast);
      print(ast.getFirstChild());
      break;

    case STATIC_IMPORT:
      out.print("import static");
      dumpHiddenAfter(ast);
      print(ast.getFirstChild());
      break;

    case CLASS_DEF:
    case ENUM_DEF:
    case INTERFACE_DEF:
      print(getChild(ast, MODIFIERS));
      if (ast.getType() == CLASS_DEF) {
        out.print("class");
      } else if (ast.getType() == ENUM_DEF) {
        out.print("enum");
      } else {
        out.print("interface");
      }
      dumpHiddenBefore(getChild(ast, IDENT));
      print(getChild(ast, IDENT));
      print(getChild(ast, TYPE_PARAMETERS));
      print(getChild(ast, EXTENDS_CLAUSE));
      print(getChild(ast, IMPLEMENTS_CLAUSE));
      print(getChild(ast, OBJBLOCK));
      break;

    case EXTENDS_CLAUSE:
      if (hasChildren(ast)) {
        out.print("extends");
        dumpHiddenBefore(getBestPrintableNode(ast, false));
        printChildren(ast);
      }
      break;

    case IMPLEMENTS_CLAUSE:
      if (hasChildren(ast)) {
        out.print("implements");
        dumpHiddenBefore(getBestPrintableNode(ast, false));
        printChildren(ast);
      }
      break;

    // DOT
    case DOT:
      print(child1);
      out.print(".");
      dumpHiddenAfter(ast);
      print(child2);
      if (child3 != null) {
        print(child3);
      }
      break;

    case MODIFIERS:
    case OBJBLOCK:
    case CTOR_DEF:
      //case METHOD_DEF:
    case PARAMETERS:
    case PARAMETER_DEF:
    case VARIABLE_PARAMETER_DEF:
    case VARIABLE_DEF:
    case ENUM_CONSTANT_DEF:
    case TYPE:
    case SLIST:
    case ELIST:
    case ARRAY_DECLARATOR:
    case TYPECAST:
    case EXPR:
    case ARRAY_INIT:
    case FOR_INIT:
    case FOR_CONDITION:
    case FOR_ITERATOR:
    case METHOD_CALL:
    case INSTANCE_INIT:
    case INDEX_OP:
    case SUPER_CTOR_CALL:
    case CTOR_CALL:
      printChildren(ast);
      break;

    case METHOD_DEF:
      printMethodDef(ast);
      break;

    // if we have two children, it's of the form "a=0"
    // if just one child, it's of the form "=0" (where the
    // lhs is above this AST).
    case ASSIGN:
      if (child2 != null) {
        print(child1);
        out.print("=");
        dumpHiddenAfter(ast);
        print(child2);
      } else {
        out.print("=");
        dumpHiddenAfter(ast);
        print(child1);
      }
      break;

    // binary operators:
    case PLUS:
    case MINUS:
    case DIV:
    case MOD:
    case NOT_EQUAL:
    case EQUAL:
    case LE:
    case GE:
    case LOR:
    case LAND:
    case BOR:
    case BXOR:
    case BAND:
    case SL:
    case SR:
    case BSR:
    case LITERAL_instanceof:
    case PLUS_ASSIGN:
    case MINUS_ASSIGN:
    case STAR_ASSIGN:
    case DIV_ASSIGN:
    case MOD_ASSIGN:
    case SR_ASSIGN:
    case BSR_ASSIGN:
    case SL_ASSIGN:
    case BAND_ASSIGN:
    case BXOR_ASSIGN:
    case BOR_ASSIGN:

    case LT:
    case GT:
      printBinaryOperator(ast);
      break;

    case LITERAL_for:
      out.print(ast.getText());
      dumpHiddenAfter(ast);
      if (child1.getType() == FOR_EACH_CLAUSE) {
        printChildren(child1);
        print(child2);
      } else {
        printChildren(ast);
      }
      break;

    case POST_INC:
    case POST_DEC:
      print(child1);
      out.print(ast.getText());
      dumpHiddenAfter(ast);
      break;

    // unary operators:
    case BNOT:
    case LNOT:
    case INC:
    case DEC:
    case UNARY_MINUS:
    case UNARY_PLUS:
      out.print(ast.getText());
      dumpHiddenAfter(ast);
      print(child1);
      break;

    case LITERAL_new:
      out.print("new");
      dumpHiddenAfter(ast);
      printChildren(ast);
      break;

    case LITERAL_return:
      out.print("return");
      dumpHiddenAfter(ast);
      print(child1);
      break;

    case STATIC_INIT:
      out.print("static");
      dumpHiddenBefore(getBestPrintableNode(ast, false));
      print(child1);
      break;

    case LITERAL_switch:
      out.print("switch");
      dumpHiddenAfter(ast);
      printChildren(ast);
      break;

    case LABELED_STAT:
    case CASE_GROUP:
      printChildren(ast);
      break;

    case LITERAL_case:
      out.print("case");
      dumpHiddenAfter(ast);
      printChildren(ast);
      break;

    case LITERAL_default:
      out.print("default");
      dumpHiddenAfter(ast);
      printChildren(ast);
      break;

    case NUM_INT:
    case CHAR_LITERAL:
    case STRING_LITERAL:
    case NUM_FLOAT:
    case NUM_LONG:
      out.print(ast.getText());
      dumpHiddenAfter(ast);
      break;

    case LITERAL_synchronized: // 0137 to fix bug #136
    case LITERAL_assert:
      out.print(ast.getText());
      dumpHiddenAfter(ast);
      printChildren(ast);
      break;

    case LITERAL_private:
    case LITERAL_public:
    case LITERAL_protected:
    case LITERAL_static:
    case LITERAL_transient:
    case LITERAL_native:
    case LITERAL_threadsafe:
      //case LITERAL_synchronized:  // 0137 to fix bug #136
    case LITERAL_volatile:
    case LITERAL_class: // 0176 to fix bug #1466
    case FINAL:
    case ABSTRACT:
    case LITERAL_package:
    case LITERAL_void:
    case LITERAL_boolean:
    case LITERAL_byte:
    case LITERAL_char:
    case LITERAL_short:
    case LITERAL_int:
    case LITERAL_float:
    case LITERAL_long:
    case LITERAL_double:
    case LITERAL_true:
    case LITERAL_false:
    case LITERAL_null:
    case SEMI:
    case LITERAL_this:
    case LITERAL_super:
      out.print(ast.getText());
      dumpHiddenAfter(ast);
      break;

    case EMPTY_STAT:
    case EMPTY_FIELD:
      break;

    case LITERAL_continue:
    case LITERAL_break:
      out.print(ast.getText());
      dumpHiddenAfter(ast);
      if (child1 != null) {// maybe label
        print(child1);
      }
      break;

    // yuck:  Distinguish between "import x.y.*" and "x = 1 * 3"
    case STAR:
      if (hasChildren(ast)) { // the binary mult. operator
        printBinaryOperator(ast);
      } else { // the special "*" in import:
        out.print("*");
        dumpHiddenAfter(ast);
      }
      break;

    case LITERAL_throws:
      out.print("throws");
      dumpHiddenAfter(ast);
      printChildren(ast);
      break;

    case LITERAL_if:
      printIfThenElse(ast);
      break;

    case LITERAL_while:
      out.print("while");
      dumpHiddenAfter(ast);
      printChildren(ast);
      break;

    case LITERAL_do:
      out.print("do");
      dumpHiddenAfter(ast);
      print(child1); // an SLIST
      out.print("while");
      dumpHiddenBefore(getBestPrintableNode(child2, false));
      print(child2); // an EXPR
      break;

    case LITERAL_try:
      out.print("try");
      dumpHiddenAfter(ast);
      printChildren(ast);
      break;

    case LITERAL_catch:
      out.print("catch");
      dumpHiddenAfter(ast);
      printChildren(ast);
      break;

    // the first child is the "try" and the second is the SLIST
    case LITERAL_finally:
      out.print("finally");
      dumpHiddenAfter(ast);
      printChildren(ast);
      break;

    case LITERAL_throw:
      out.print("throw");
      dumpHiddenAfter(ast);
      print(child1);
      break;

    // the dreaded trinary operator
    case QUESTION:
      print(child1);
      out.print("?");
      dumpHiddenAfter(ast);
      print(child2);
      print(child3);
      break;

    // pde specific or modified tokens start here

    // Image -> BImage, Font -> BFont as appropriate
    case IDENT:
      /*
      if (ast.getText().equals("Image") &&
          Preferences.getBoolean("preproc.substitute_image")) { //, true)) {
        out.print("BImage");
      } else if (ast.getText().equals("Font") &&
                 Preferences.getBoolean("preproc.substitute_font")) { //, true)) {
        out.print("BFont");
      } else {
      */
      out.print(ast.getText());
      //}
      dumpHiddenAfter(ast);
      break;

    // the color datatype is just an alias for int
    case LITERAL_color:
      out.print("int");
      dumpHiddenAfter(ast);
      break;

    case WEBCOLOR_LITERAL:
      if (ast.getText().length() != 6) {
        System.err.println("Internal error: incorrect length of webcolor "
            + "literal should have been detected sooner.");
        break;
      }
      out.print("0xff" + ast.getText());
      dumpHiddenAfter(ast);
      break;

    // allow for stuff like int(43.2).
    case CONSTRUCTOR_CAST:
      final AST terminalTypeNode = child1.getFirstChild();
      final AST exprToCast = child2;
      final String pooType = terminalTypeNode.getText();
      out.print("PApplet.parse" + Character.toUpperCase(pooType.charAt(0))
          + pooType.substring(1));
      dumpHiddenAfter(terminalTypeNode); // the left paren
      print(exprToCast);
      break;

    // making floating point literals default to floats, not doubles
    case NUM_DOUBLE:
      final String literalDouble = ast.getText().toLowerCase();
      out.print(literalDouble);
      if (Preferences.getBoolean("preproc.substitute_floats")
          && literalDouble.indexOf('d') == -1) { // permit literal doubles
        out.print("f");
      }
      dumpHiddenAfter(ast);
      break;

    case TYPE_ARGUMENTS:
    case TYPE_PARAMETERS:
      printChildren(ast);
      break;

    case TYPE_ARGUMENT:
    case TYPE_PARAMETER:
      printChildren(ast);
      break;

    case WILDCARD_TYPE:
      out.print(ast.getText());
      dumpHiddenAfter(ast);
      print(ast.getFirstChild());
      break;

    case TYPE_LOWER_BOUNDS:
    case TYPE_UPPER_BOUNDS:
      out.print(ast.getType() == TYPE_LOWER_BOUNDS ? "super" : "extends");
      dumpHiddenBefore(getBestPrintableNode(ast, false));
      printChildren(ast);
      break;

    case ANNOTATION:
      out.print("@");
      printChildren(ast);
      break;

    case ANNOTATIONS:
    case ANNOTATION_ARRAY_INIT:
      printChildren(ast);
      break;

    case ANNOTATION_MEMBER_VALUE_PAIR:
      print(ast.getFirstChild());
      out.print("=");
      dumpHiddenBefore(getBestPrintableNode(ast.getFirstChild().getNextSibling(), false));
      print(ast.getFirstChild().getNextSibling());
      break;

    default:
      debug.println("Unrecognized type:" + ast.getType() + " ("
          + TokenUtil.nameOf(ast) + ")");
      break;
    }

    stack.pop();
  }

}
