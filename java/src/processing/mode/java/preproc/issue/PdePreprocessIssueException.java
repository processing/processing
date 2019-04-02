package processing.mode.java.preproc.issue;

import processing.mode.java.preproc.issue.PdePreprocessIssue;

public class PdePreprocessIssueException extends RuntimeException {

  private final PdePreprocessIssue preprocessIssue;

  public PdePreprocessIssueException(PdePreprocessIssue newPreprocessIssue) {
    super(newPreprocessIssue.getMsg());
    preprocessIssue = newPreprocessIssue;
  }

  public PdePreprocessIssue getIssue() {
    return preprocessIssue;
  }

}
