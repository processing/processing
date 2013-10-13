/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012 The Processing Foundation

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;


/**
 * Utility functions for base that require a java.awt.Toolkit object. These
 * are broken out from Base as we start moving toward the possibility of the
 * code running in headless mode.
 * @author fry
 */
public class Toolkit {
  static final java.awt.Toolkit awtToolkit =
    java.awt.Toolkit.getDefaultToolkit();

  /** Command on Mac OS X, Ctrl on Windows and Linux */
  static final int SHORTCUT_KEY_MASK =
    awtToolkit.getMenuShortcutKeyMask();
  /** Command-W on Mac OS X, Ctrl-W on Windows and Linux */
  public static final KeyStroke WINDOW_CLOSE_KEYSTROKE =
    KeyStroke.getKeyStroke('W', SHORTCUT_KEY_MASK);
  /** Command-Option on Mac OS X, Ctrl-Alt on Windows and Linux */
  static final int SHORTCUT_ALT_KEY_MASK = ActionEvent.ALT_MASK |
    awtToolkit.getMenuShortcutKeyMask();


  /**
   * A software engineer, somewhere, needs to have his abstraction
   * taken away. In some countries they jail or beat people for crafting
   * the sort of API that would require a five line helper function
   * just to set the shortcut key for a menu item.
   */
  static public JMenuItem newJMenuItem(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    int modifiers = awtToolkit.getMenuShortcutKeyMask();
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  /**
   * Like newJMenuItem() but adds shift as a modifier for the shortcut.
   */
  static public JMenuItem newJMenuItemShift(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    int modifiers = awtToolkit.getMenuShortcutKeyMask();
    modifiers |= ActionEvent.SHIFT_MASK;
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  /**
   * Same as newJMenuItem(), but adds the ALT (on Linux and Windows)
   * or OPTION (on Mac OS X) key as a modifier.
   */
  static public JMenuItem newJMenuItemAlt(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, SHORTCUT_ALT_KEY_MASK));
    return menuItem;
  }


  static public JCheckBoxMenuItem newJCheckBoxMenuItem(String title, int what) {
    JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(title);
    int modifiers = awtToolkit.getMenuShortcutKeyMask();
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  static public void addDisabledItem(JMenu menu, String title) {
    JMenuItem item = new JMenuItem(title);
    item.setEnabled(false);
    menu.add(item);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static public Dimension getScreenSize() {
    return awtToolkit.getScreenSize();
  }


  /**
   * Return an Image object from inside the Processing lib folder.
   * Moved here so that Base can stay headless.
   */
  static public Image getLibImage(String filename) {
    File file = Base.getContentFile("lib/" + filename);
    if (!file.exists()) {
      return null;
    }
    return new ImageIcon(file.getAbsolutePath()).getImage();
  }


  static ArrayList<Image> iconImages;

  /**
   * Give this Frame the Processing icon set. Ignored on OS X, because they
   * thought different and made this function set the minified image of the
   * window, not the window icon for the dock or cmd-tab.
   */
  static public void setIcon(Frame frame) {
    if (!Base.isMacOS()) {
//    // too low-res, prepping for nicer icons in 2.0 timeframe
//    Image image = awtToolkit.createImage(PApplet.ICON_IMAGE);
//    frame.setIconImage(image);

      if (iconImages == null) {
        iconImages = new ArrayList<Image>();
        final int[] sizes = { 16, 32, 48, 64, 128, 256, 512 };
        for (int sz : sizes) {
          iconImages.add(Toolkit.getLibImage("icons/pde-" + sz + ".png"));
        }
      }
      frame.setIconImages(iconImages);
    }
  }


  // someone needs to be slapped
  //static KeyStroke closeWindowKeyStroke;

  /**
   * Return true if the key event was a Ctrl-W or an ESC,
   * both indicators to close the window.
   * Use as part of a keyPressed() event handler for frames.
   */
  /*
  static public boolean isCloseWindowEvent(KeyEvent e) {
    if (closeWindowKeyStroke == null) {
      int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
      closeWindowKeyStroke = KeyStroke.getKeyStroke('W', modifiers);
    }
    return ((e.getKeyCode() == KeyEvent.VK_ESCAPE) ||
            KeyStroke.getKeyStrokeForEvent(e).equals(closeWindowKeyStroke));
  }
  */

  /**
   * Registers key events for a Ctrl-W and ESC with an ActionListener
   * that will take care of disposing the window.
   */
  static public void registerWindowCloseKeys(JRootPane root,
                                             ActionListener disposer) {
    KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    root.registerKeyboardAction(disposer, stroke,
                                JComponent.WHEN_IN_FOCUSED_WINDOW);

    int modifiers = awtToolkit.getMenuShortcutKeyMask();
    stroke = KeyStroke.getKeyStroke('W', modifiers);
    root.registerKeyboardAction(disposer, stroke,
                                JComponent.WHEN_IN_FOCUSED_WINDOW);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static public void beep() {
    awtToolkit.beep();
  }


  static public Clipboard getSystemClipboard() {
    return awtToolkit.getSystemClipboard();
  }

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static Boolean highResProp;


  static public boolean highResDisplay() {
    if (highResProp == null) {
      highResProp = checkRetina();
    }
    return highResProp;
  }
  
  
  static private boolean checkRetina() {
    if (Base.isMacOS()) {
    // This should probably be reset each time there's a display change.
    // A 5-minute search didn't turn up any such event in the Java API.
    // Also, should we use the Toolkit associated with the editor window?
//      String javaVendor = System.getProperty("java.vendor");
//      if (javaVendor.contains("Apple")) {
      if (System.getProperty("java.vendor").contains("Apple")) {
        Float prop = (Float)
          awtToolkit.getDesktopProperty("apple.awt.contentScaleFactor");
        if (prop != null) {
          return prop == 2;
        }
//      } else if (javaVendor.contains("Oracle")) {
//        String version = System.getProperty("java.version");  // 1.7.0_40
//        String[] m = PApplet.match(version, "1.(\\d).*_(\\d+)");
//        
//        // Make sure this is Oracle Java 7u40 or later
//        if (m != null && 
//            PApplet.parseInt(m[1]) >= 7 && 
//            PApplet.parseInt(m[1]) >= 40) {
      } else if (Base.isUsableOracleJava()) {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = env.getDefaultScreenDevice();

        try {
          Field field = device.getClass().getDeclaredField("scale");
          if (field != null) {
            field.setAccessible(true);
            Object scale = field.get(device);

            if (scale instanceof Integer && ((Integer)scale).intValue() == 2) {
              return true;
            }
          }
        } catch (Exception ignore) { } 
      }
    }
    return false;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


//  static Font monoFont;
//  static Font plainFont;
//  static Font boldFont;
//
//
//  static public Font getMonoFont(int size) {
//    if (monoFont == null) {
//      try {
//        monoFont = createFont("DroidSansMono.ttf", size);
//      } catch (Exception e) {
//        monoFont = new Font("Monospaced", Font.PLAIN, size);
//      }
//    }
//    return monoFont;
//  }
//
//
//  static public Font getPlainFont(int size) {
//    if (plainFont == null) {
//      try {
//        plainFont = createFont("DroidSans.ttf", size);
//      } catch (Exception e) {
//        plainFont = new Font("SansSerif", Font.PLAIN, size);
//      }
//    }
//    return plainFont;
//  }
//
//
//  static public Font getBoldFont(int size) {
//    if (boldFont == null) {
//      try {
//        boldFont = createFont("DroidSans-Bold.ttf", size);
//      } catch (Exception e) {
//        boldFont = new Font("SansSerif", Font.BOLD, size);
//      }
//    }
//    return boldFont;
//  }


  static public List<Font> getMonoFontList() {
    GraphicsEnvironment ge =
      GraphicsEnvironment.getLocalGraphicsEnvironment();
    Font[] fonts = ge.getAllFonts();
    ArrayList<Font> outgoing = new ArrayList<Font>();
    // Using AffineTransform.getScaleInstance(100, 100) doesn't change sizes
    FontRenderContext frc = 
      new FontRenderContext(new AffineTransform(),
                            Preferences.getBoolean("editor.antialias"), 
                            true);  // use fractional metrics 
    for (Font font : fonts) {
      if (font.canDisplay('i') && font.canDisplay('M') &&
          font.canDisplay(' ') && font.canDisplay('.')) {
        
        // The old method just returns 1 or 0, and using deriveFont(size)  
        // is overkill. It also causes deprecation warnings
//        @SuppressWarnings("deprecation")
//        FontMetrics fm = awtToolkit.getFontMetrics(font);
        //FontMetrics fm = awtToolkit.getFontMetrics(font.deriveFont(24));
//        System.out.println(fm.charWidth('i') + " " + fm.charWidth('M'));
//        if (fm.charWidth('i') == fm.charWidth('M') &&
//            fm.charWidth('M') == fm.charWidth(' ') && 
//            fm.charWidth(' ') == fm.charWidth('.')) {
        double w = font.getStringBounds(" ", frc).getWidth();
        if (w == font.getStringBounds("i", frc).getWidth() && 
            w == font.getStringBounds("M", frc).getWidth() &&
            w == font.getStringBounds(".", frc).getWidth()) {
          outgoing.add(font);
//          System.out.println("  good " + w);
        }
      }
    }
    return outgoing;
  }


  static Font monoFont;
  static Font monoBoldFont;
  static Font sansFont;
  static Font sansBoldFont;


  static public String getMonoFontName() {
    if (monoFont == null) {
      getMonoFont(12, Font.PLAIN);  // load a dummy version
    }
    return monoFont.getName();
  }
  
  
  static public Font getMonoFont(int size, int style) {
    if (monoFont == null) {
      try {
        monoFont = createFont("SourceCodePro-Regular.ttf", size);
        monoBoldFont = createFont("SourceCodePro-Semibold.ttf", size);
      } catch (Exception e) {
        Base.log("Could not load mono font", e);
        monoFont = new Font("Monospaced", Font.PLAIN, size);
        monoBoldFont = new Font("Monospaced", Font.BOLD, size);
      }
    }
    if (style == Font.BOLD) {
      if (size == monoBoldFont.getSize()) {
        return monoBoldFont;
      } else {
        return monoBoldFont.deriveFont((float) size);
      }
    } else {
      if (size == monoFont.getSize()) {
        return monoFont;
      } else {
        return monoFont.deriveFont((float) size);
      }
    }
  }


  static public Font getSansFont(int size, int style) {
    if (sansFont == null) {
      try {
        sansFont = createFont("SourceSansPro-Regular.ttf", size);
        sansBoldFont = createFont("SourceSansPro-Semibold.ttf", size);
      } catch (Exception e) {
        Base.log("Could not load sans font", e);
        sansFont = new Font("SansSerif", Font.PLAIN, size);
        sansBoldFont = new Font("SansSerif", Font.BOLD, size);
      }
    }
    if (style == Font.BOLD) {
      if (size == sansBoldFont.getSize()) {
        return sansBoldFont;
      } else {
        return sansBoldFont.deriveFont((float) size);
      }
    } else {
      if (size == sansFont.getSize()) {
        return sansFont;
      } else {
        return sansFont.deriveFont((float) size);
      }
    }
  }


  static private Font createFont(String filename, int size) throws IOException, FontFormatException {
    InputStream is = Base.getLibStream("fonts/" + filename);
    BufferedInputStream input = new BufferedInputStream(is);
    Font font = Font.createFont(Font.TRUETYPE_FONT, input);
    input.close();
    return font.deriveFont((float) size);
  }
  
  
  static double getAscent(Graphics g) { //, Font font) {
    Graphics2D g2 = (Graphics2D) g;
    FontRenderContext frc = g2.getFontRenderContext();
    //return new TextLayout("H", font, frc).getBounds().getHeight();
    return new TextLayout("H", g.getFont(), frc).getBounds().getHeight();
  }
}
