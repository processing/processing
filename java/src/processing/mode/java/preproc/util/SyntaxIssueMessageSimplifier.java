package processing.mode.java.preproc.util;


import processing.mode.java.preproc.util.strategy.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class SyntaxIssueMessageSimplifier {

  private static AtomicReference<SyntaxIssueMessageSimplifier> instance = new AtomicReference<>();

  private List<SyntaxIssueMessageSimplifierStrategy> strategies;

  public static SyntaxIssueMessageSimplifier get() {
    instance.compareAndSet(null, new SyntaxIssueMessageSimplifier());
    return instance.get();
  }

  private SyntaxIssueMessageSimplifier() {
    strategies = new ArrayList<>();
    strategies.add(new MissingDoubleQuoteMessageSimplifierStrategy());
    strategies.add(new MissingSingleQuoteMessageSimplifierStrategy());
    strategies.add(new MissingVariableNameMessageSimplifierStrategy());
    strategies.add(new AssignmentMessageSimplifierStrategy());
    strategies.add(new MissingClassNameMessageSimplifierStrategy());
    strategies.add(new MissingMethodNameMessageSimplifierStrategy());
    strategies.add(new BadIdentifierMessageSimplifierStrategy());
    strategies.add(new BadParamMessageSimplifierStrategy());
    strategies.add(new MissingParenMessageSimplifierStrategy());
    strategies.add(new MissingCaretMessageSimplifierStrategy());
    strategies.add(new MissingCurlyMessageSimplifierStrategy());
    strategies.add(new SemicolonMessageSimplifierStrategy());
    strategies.add(new DefaultMessageSimplifier());
  }

  public String simplify(String originalMessage) {
    System.err.println(originalMessage);
    Optional<String> matching = strategies.stream()
        .map((x) -> x.simplify(originalMessage))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();

    return matching.orElse(originalMessage);
  }

}
