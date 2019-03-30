package processing.mode.java.preproc.util;


import processing.mode.java.preproc.util.strategy.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Facade that tries to create a better error message for syntax issues in input source.
 *
 * <p>
 * Facade that interprets error messages from ANTLR in an attempt to generate an improved error
 * message when describing grammatically incorrect input. This is distinct from compiler errors
 * caused after generating an AST.
 * </p>
 */
public class SyntaxIssueMessageSimplifier {

  private static AtomicReference<SyntaxIssueMessageSimplifier> instance = new AtomicReference<>();

  private List<SyntaxIssueMessageSimplifierStrategy> strategies;

  /**
   * Get a shared instance of this singleton.
   *
   * @return Shared instance of this singleton, creating that shared instance if one did not exist
   *    previously.
   */
  public static SyntaxIssueMessageSimplifier get() {
    instance.compareAndSet(null, new SyntaxIssueMessageSimplifier());
    return instance.get();
  }

  /**
   * Create a new syntax issue message simplifier with the default simplifier strategies.
   */
  private SyntaxIssueMessageSimplifier() {
    strategies = new ArrayList<>();
    strategies.add(new MissingIdentifierMessageSimplifierStrategy());
    strategies.add(new KnownMissingMessageSimplifierStrategy());
    strategies.add(new ExtraneousInputMessageSimplifierStrategy());
    strategies.add(new MismatchedInputMessageSimplifierStrategy());
    strategies.add(new MissingVariableNameMessageSimplifierStrategy());
    strategies.add(new AssignmentMessageSimplifierStrategy());
    strategies.add(new BadIdentifierMessageSimplifierStrategy());
    strategies.add(new MissingClassNameMessageSimplifierStrategy());
    strategies.add(new MissingMethodNameMessageSimplifierStrategy());
    strategies.add(new BadParamMessageSimplifierStrategy());
    strategies.add(new NonTermMessageSimplifierStrategy());
    strategies.add(new MissingDoubleQuoteMessageSimplifierStrategy());
    strategies.add(new MissingSingleQuoteMessageSimplifierStrategy());
    strategies.add(new MissingParenMessageSimplifierStrategy());
    strategies.add(new MissingChevMessageSimplifierStrategy());
    strategies.add(new MissingCurlyMessageSimplifierStrategy());
    strategies.add(new DefaultMessageSimplifier());
  }

  /**
   * Attempt to improve an error message.
   *
   * @param originalMessage Error message generated from ANTLR.
   * @return An improved error message or the originalMessage if no improvements could be made.
   */
  public IssueMessageSimplification simplify(String originalMessage) {
    //System.err.println(originalMessage);
    Optional<IssueMessageSimplification> matching = strategies.stream()
        .map((x) -> x.simplify(originalMessage))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();

    return matching.orElse(new IssueMessageSimplification(originalMessage, 0));
  }

}
