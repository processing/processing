package processing.core;

/** Wrapper for exceptions so that they can be thrown anywhere.
 *
 * @author Francis Li
 */
public class PException extends RuntimeException {
    /** The original exception */
    private Throwable t;
    
    public PException(Throwable t) {
        this.t = t;
    }
    
    public PException(String message, Throwable t) {
        super(message);
        this.t = t;
    }
    
    public String getMessage() {
        return super.getMessage() + ": " + t.getMessage();
    }

    public String toString() {
        return super.toString() + ": " + t.toString();
    }
    
    public void printStackTrace() {
        t.printStackTrace();
    }
}
