public class PdeException extends Exception {
  int line = -1;

  public PdeException() { }

  public PdeException(String message) {
    super(message);
  }

  public PdeException(String message, int line) {
    super(message);
    this.line = line;
  }

#ifndef PLAYER
#ifdef DBN
  public PdeException(String message, DbnToken token) {
    super(message + ", token: " + token.toString());
  }
#endif
#endif
}

