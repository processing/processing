package processing.mode.java.preproc.util.strategy;

import java.util.Optional;

public interface SyntaxIssueMessageSimplifierStrategy {

  Optional<String> simplify(String message);

}
