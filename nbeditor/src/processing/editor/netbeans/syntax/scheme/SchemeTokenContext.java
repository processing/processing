package processing.editor.netbeans.syntax.scheme;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.editor.BaseTokenID;
import org.netbeans.editor.TokenContext;
import org.netbeans.editor.TokenContextPath;

/**
 * SchemeTokenContext is responsible for defining the tokens needed to
 *  represent the Scheme language.
 * @author $Author: antonio $ Antonio Vieiro (antonio@antonioshome.net)
 * @version $Revision: 1.2 $
 */
public class SchemeTokenContext
  extends TokenContext
{
  private SchemeTokenContext()
  {
    super("scheme-");
    
    try
    {
      addDeclaredTokenIDs();
    }
    catch (Exception e)
    {
      Logger.getLogger("SchemeTokenContext").log( Level.SEVERE, "Unexpected exception", e );
    }
  }
  
  public static final int ID_SCHEME_OPEN_PAREN  = 1;
  public static final int ID_SCHEME_CLOSE_PAREN = 2;
  public static final int ID_SCHEME_WHITESPACE  = 3;
  public static final int ID_SCHEME_CONSTRUCT   = 4;
  public static final int ID_SCHEME_STDPROC     = 5;
  public static final int ID_SCHEME_CONSTANT    = 6;
  public static final int ID_SCHEME_COMMENT     = 7;
  public static final int ID_SCHEME_VARIABLE    = 8;
  public static final int ID_SCHEME_ERROR       = 9;
  
  public static final BaseTokenID SCHEME_OPEN_PAREN  = new BaseTokenID("openparen", ID_SCHEME_OPEN_PAREN );
  public static final BaseTokenID SCHEME_CLOSE_PAREN = new BaseTokenID("closeparen", ID_SCHEME_CLOSE_PAREN );
  public static final BaseTokenID SCHEME_WHITESPACE  = new BaseTokenID("whitespace", ID_SCHEME_WHITESPACE );
  public static final BaseTokenID SCHEME_CONSTRUCT   = new BaseTokenID("construct", ID_SCHEME_CONSTRUCT );
  public static final BaseTokenID SCHEME_STDPROC     = new BaseTokenID("stdproc", ID_SCHEME_STDPROC );
  public static final BaseTokenID SCHEME_CONSTANT    = new BaseTokenID("constant", ID_SCHEME_CONSTANT );
  public static final BaseTokenID SCHEME_COMMENT     = new BaseTokenID("comment", ID_SCHEME_COMMENT );
  public static final BaseTokenID SCHEME_VARIABLE    = new BaseTokenID("variable", ID_SCHEME_VARIABLE );
  public static final BaseTokenID SCHEME_ERROR       = new BaseTokenID("error", ID_SCHEME_ERROR );
  
  public static final SchemeTokenContext context = new SchemeTokenContext();
  public static final TokenContextPath contextPath = context.getContextPath();
  
}
