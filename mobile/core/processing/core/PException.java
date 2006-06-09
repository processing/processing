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
    
    public String getMessage() {
        return t.getMessage();
    }

    public String toString() {
        return t.toString();
    }
    
    public void printStackTrace() {
        t.printStackTrace();
    }
}
