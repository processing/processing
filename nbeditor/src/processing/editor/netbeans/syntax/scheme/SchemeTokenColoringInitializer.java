package processing.editor.netbeans.syntax.scheme;

import java.awt.Color;
import java.awt.Font;
import org.netbeans.editor.Coloring;
import org.netbeans.editor.SettingsDefaults;
import org.netbeans.editor.SettingsUtil;
import org.netbeans.editor.TokenCategory;
import org.netbeans.editor.TokenContextPath;

/**
 * SchemeTokenColoringInitializer is responsible for assigning Colorings
 *  to TokenIDs (assigning colors to tokens).
 * @author $Author: antonio $ Antonio Vieiro (antonio@antonioshome.net)
 * @version $Revision: 1.2 $
 */
public class SchemeTokenColoringInitializer
  extends SettingsUtil.TokenColoringInitializer
{
  /**
   * Creates a new instance of SchemeTokenColoringInitializer
   */
  public SchemeTokenColoringInitializer()
  {
    super( SchemeTokenContext.context );
  }
  
  public static final Font BOLD_FONT = SettingsDefaults.defaultFont.deriveFont( Font.BOLD );
  
  public static final Coloring COLOR_CONSTRUCT =
    new Coloring( BOLD_FONT, Color.BLACK, null );
  
  public static final Coloring COLOR_STD_PROC =
    new Coloring( BOLD_FONT, Color.BLUE.darker(), null );
  
  public static final Coloring COLOR_PARENTHESIS =
    new Coloring( BOLD_FONT, Color.GREEN.darker(), null );
  
  public static final Coloring COLOR_DEFAULT =
    new Coloring( null, Color.BLACK, null );
  
  public static final Coloring COLOR_COMMENT =
    new Coloring( null, Color.GREEN.darker(), null );
  
  public static final Coloring COLOR_ERROR =
    new Coloring( BOLD_FONT, Color.RED, null );
  
  public static final Coloring COLOR_CONSTANT =
    new Coloring( null, Color.RED.darker(), null );
  
  public Object getTokenColoring(TokenContextPath tokenContextPath, TokenCategory tokenIDOrCategory, boolean printingSet)
  {
    if ( printingSet )
    {
      return SettingsUtil.defaultPrintColoringEvaluator;
    }
    else
    {
      int tokenid = tokenIDOrCategory.getNumericID();
      switch( tokenid )
      {
        case SchemeTokenContext.ID_SCHEME_OPEN_PAREN:
        case SchemeTokenContext.ID_SCHEME_CLOSE_PAREN:
          return COLOR_PARENTHESIS;
        case SchemeTokenContext.ID_SCHEME_CONSTRUCT:
          return COLOR_CONSTRUCT;
        case SchemeTokenContext.ID_SCHEME_STDPROC:
          return COLOR_STD_PROC;
        case SchemeTokenContext.ID_SCHEME_CONSTANT:
          return COLOR_CONSTANT;
        case SchemeTokenContext.ID_SCHEME_COMMENT:
          return COLOR_COMMENT;
        case SchemeTokenContext.ID_SCHEME_ERROR:
          return COLOR_ERROR;
        default:
          return COLOR_DEFAULT;
      }
    }
  }
}
