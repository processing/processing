public class PdeException extends Exception {
  int line = -1;

  public PdeException() { }

  public PdeException(String message) {
    super(massage(message));
    //System.out.println("message for this error is " + message);
  }

  public PdeException(String message, int line) {
    super(massage(message));
    this.line = line;
  }

  // make static so that super() can call it
  static public final String massage(String msg) {
    if (msg.indexOf("java.lang.") == 0) {
      //int dot = msg.lastIndexOf('.');
      msg = msg.substring("java.lang.".length());
    }
    return msg;
    //return (dot == -1) ? msg : msg.substring(dot+1);
  }
}
