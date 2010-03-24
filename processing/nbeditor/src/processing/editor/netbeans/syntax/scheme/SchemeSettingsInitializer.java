package processing.editor.netbeans.syntax.scheme;

import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.editor.BaseKit;
import org.netbeans.editor.Settings;
import org.netbeans.editor.SettingsNames;
import org.netbeans.editor.SettingsUtil;
import org.netbeans.editor.TokenContext;

/**
 * SchemeSettingsInitializer is responsible for setting some editor properties
 *  for rendering Scheme text.
 * @author $Author: antonio $ Antonio Vieiro (antonio@antonioshome.net)
 * @version $Revision: 1.2 $
 */
public class SchemeSettingsInitializer
  extends Settings.AbstractInitializer
{
  private static String SCHEME_PREFIX="scheme-settings-initializer"; // NOI18N

  public SchemeSettingsInitializer()
  {
    super( SCHEME_PREFIX );
  }
  
  public void updateSettingsMap(Class kitClass, Map settingsMap)
  {
    if ( kitClass == BaseKit.class )
    {
      SchemeTokenColoringInitializer colorInitializer =
        new SchemeTokenColoringInitializer();
      colorInitializer.updateSettingsMap( kitClass, settingsMap );
    }
    if ( kitClass == SchemeEditorKit.class )
    {
      // SettingsNames contains *LOTS* of settings!
      
      // Enable line numbers
      settingsMap.put( SettingsNames.LINE_NUMBER_VISIBLE, Boolean.TRUE );
      
      // Antialiased text
      HashMap hints = new HashMap();
      // TODO: Detect if JDK6.0 and, if so, update VALUE_TEXT_ANTIALIAS_ON with other settings
      hints.put( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
      settingsMap.put( SettingsNames.RENDERING_HINTS, hints );
      
      SettingsUtil.updateListSetting(
        settingsMap,
        SettingsNames.TOKEN_CONTEXT_LIST,
        new TokenContext[]
      { SchemeTokenContext.context }
      );
    }
  }
}
