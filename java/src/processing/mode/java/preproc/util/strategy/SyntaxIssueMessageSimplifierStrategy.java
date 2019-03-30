package processing.mode.java.preproc.util.strategy;

import processing.mode.java.preproc.util.IssueMessageSimplification;

import java.util.Optional;


/**
 * Interface for strategies that improve syntax error messages before showing them to the user.
 */
public interface SyntaxIssueMessageSimplifierStrategy {

  /**
   * Attempt to simplify an error message.
   *
   * @param message The message to be simplified.
   * @return An optional with an improved message or an empty optional if no improvements could be
   *    made by this strategy.
   */
  Optional<IssueMessageSimplification> simplify(String message);

}
