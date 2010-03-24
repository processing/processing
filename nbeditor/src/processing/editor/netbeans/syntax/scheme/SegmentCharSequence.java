package processing.editor.netbeans.syntax.scheme;

import javax.swing.text.Segment;

/**
 * SegmentCharSequence is a Segment and a CharSequence.
 * This object is used to avoid creating Strings from char arrays, so that
 *  char arrays can be used in Pattern and Matcher.
 * @author $Author: antonio $ Antonio Vieiro (antonio@antonioshome.net)
 * @version $Revision: 1.2 $
 */
public class SegmentCharSequence
  extends Segment
  implements CharSequence
{
  /**
   * Creates a new instance of SegmentCharSequence
   */
  public SegmentCharSequence( char[] buffer, int startOffset, int length )
  {
    super( buffer, startOffset, length );
  }

  public char charAt(int index)
  {
    if ( index < 0 || index > count )
      throw new StringIndexOutOfBoundsException( index );
    return array[ offset + index ];
  }

  public CharSequence subSequence(int start, int end)
  {
    if ( start < 0 || end > count || start > end )
      throw new StringIndexOutOfBoundsException( " start " + start + " end " + end );
      
    return new SegmentCharSequence( array, offset + start, end-start );
  }

  public int length()
  {
    return count;
  }
}
