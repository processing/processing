package processing.mode.experimental;
/**
	 * Error markers displayed on the Error Bar.
	 * 
	 * @author Manindra Moharana &lt;me@mkmoharana.com&gt;
	 * 
	 */
	public class ErrorMarker {
	  /**
	   * y co-ordinate of the marker
	   */
	  public int y;
	  /**
	   * Type of marker: Error or Warning?
	   */
	  public int type = -1;
	  /**
	   * Error Type constant
	   */
	  public static final int Error = 1;
	  /**
	   * Warning Type constant
	   */
	  public static final int Warning = 2;
	  /**
	   * Problem that the error marker represents
	   * @see Problem
	   */
	  public Problem problem;

	  public ErrorMarker(Problem problem, int y, int type) {
	    this.problem = problem;
	    this.y = y;
	    this.type = type;
	  }
	}