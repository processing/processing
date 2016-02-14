/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-15 The Processing Foundation

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

package processing.app.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import processing.app.Language;
import processing.app.Messages;
import processing.app.Platform;
import processing.app.Preferences;


/**
 * Utility functions for base that require a java.awt.Toolkit object. These
 * are broken out from Base as we start moving toward the possibility of the
 * code running in headless mode.
 * @author fry
 */
public class Toolkit {
  /*
  static public final String PROMPT_YES     = Language.text("prompt.yes");
  static public final String PROMPT_NO      = Language.text("prompt.no");
  static public final String PROMPT_CANCEL  = Language.text("prompt.cancel");
  static public final String PROMPT_OK      = Language.text("prompt.ok");
  static public final String PROMPT_BROWSE  = Language.text("prompt.browse");
  */

  static final java.awt.Toolkit awtToolkit =
    java.awt.Toolkit.getDefaultToolkit();

  /** Command on Mac OS X, Ctrl on Windows and Linux */
  static final int SHORTCUT_KEY_MASK =
    awtToolkit.getMenuShortcutKeyMask();

  /** Command-W on Mac OS X, Ctrl-W on Windows and Linux */
  public static final KeyStroke WINDOW_CLOSE_KEYSTROKE =
    KeyStroke.getKeyStroke('W', SHORTCUT_KEY_MASK);
  /** Command-Option on Mac OS X, Ctrl-Alt on Windows and Linux */
  static final int SHORTCUT_ALT_KEY_MASK =
    ActionEvent.ALT_MASK | SHORTCUT_KEY_MASK;
  /** Command-Shift on Mac OS X, Ctrl-Shift on Windows and Linux */
  static final int SHORTCUT_SHIFT_KEY_MASK =
    ActionEvent.SHIFT_MASK | SHORTCUT_KEY_MASK;


  /**
   * Standardized width for buttons. Mac OS X 10.3 wants 70 as its default,
   * Windows XP needs 66, and my Ubuntu machine needs 80+, so 80 seems proper.
   * This is now stored in the languages file since this may need to be larger
   * for languages that are consistently wider than English.
   */
  static public int getButtonWidth() {
    // Made into a method so that calling Toolkit methods doesn't require
    // the languages to be loaded, and with that, Base initialized completely
    return Integer.parseInt(Language.text("preferences.button.width"));
  }


  /**
   * A software engineer, somewhere, needs to have their abstraction
   * taken away. Who crafts the sort of API that would require a
   * five-line helper function just to set the shortcut key for a
   * menu item?
   */
  static public JMenuItem newJMenuItem(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    int modifiers = awtToolkit.getMenuShortcutKeyMask();
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  /**
   * @param action: use an Action, which sets the title, reaction
   *                and enabled-ness all by itself.
   */
  static public JMenuItem newJMenuItem(Action action, int what) {
    JMenuItem menuItem = new JMenuItem(action);
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
   * Like newJMenuItem() but adds shift as a modifier for the shortcut.
   */
  static public JMenuItem newJMenuItemShift(Action action, int what) {
    JMenuItem menuItem = new JMenuItem(action);
    int modifiers = awtToolkit.getMenuShortcutKeyMask();
    modifiers |= ActionEvent.SHIFT_MASK;
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  /**
   * Same as newJMenuItem(), but adds the ALT (on Linux and Windows)
   * or OPTION (on Mac OS X) key as a modifier. This function should almost
   * never be used, because it's bad for non-US keyboards that use ALT in
   * strange and wondrous ways.
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


  /**
   * Removes all mnemonics, then sets a mnemonic for each menu and menu item
   * recursively by these rules:
   * <ol>
   * <li> It tries to assign one of <a href="http://techbase.kde.org/Projects/Usability/HIG/Keyboard_Accelerators">
   * KDE's defaults</a>.</li>
   * <li> Failing that, it loops through the first letter of each word, where a word
   *  is a block of Unicode "alphabetical" chars, looking for an upper-case ASCII mnemonic
   *  that is not taken. This is to try to be relevant, by using a letter well-associated
   *  with the command. (MS guidelines) </li>
   * <li> Ditto, but with lowercase. </li>
   * <li> Next, it tries the second ASCII character, if its width &gt;= half the width of
   *  'A'. </li>
   * <li> If the first letters are all taken/non-ASCII, then it loops through the
   *  ASCII letters in the item, widest to narrowest, seeing if any of them is not taken.
   *  To improve readability, it discriminates against decenders (qypgj), imagining they
   *  have 2/3 their actual width. (MS guidelines: avoid decenders). It also discriminates
   *  against vowels, imagining they have 2/3 their actual width. (MS and Gnome guidelines:
   *  avoid vowels.) </li>
   * <li>Failing that, it will loop left-to-right for an available digit. This is a last
   *  resort because the normal setMnemonic dislikes them.</li>
   * <li> If that doesn't work, it doesn't assign a mnemonic. </li>
   * </ol>
   *
   * As a special case, strings starting "sketchbook \u2192 " have that bit ignored
   * because otherwise the Recent menu looks awful. However, the name <tt>"sketchbook \u2192
   * Sketch"</tt>, for example, will have the 'S' of "Sketch" chosen, but the 's' of 'sketchbook
   * will get underlined.
   * No letter by an underscore will be assigned.
   * Disabled on Mac, per Apple guidelines.
   * <tt>menu</tt> may contain nulls.
   *
   * Author: George Bateman. Initial work Myer Nore.
   * @param menu
   *          A menu, a list of menus or an array of menu items to set mnemonics for.
   */
  static public void setMenuMnemonics(JMenuItem... menu) {
    if (Platform.isMacOS()) return;
    if (menu.length == 0) return;

    // The English is http://techbase.kde.org/Projects/Usability/HIG/Keyboard_Accelerators,
    // made lowercase.
    // Nothing but [a-z] except for '&' before mnemonics and regexes for changable text.
    final String[] kdePreDefStrs = { "&file", "&new", "&open", "open&recent",
      "&save", "save&as", "saveacop&y", "saveas&template", "savea&ll", "reloa&d",
      "&print", "printpre&view", "&import", "e&xport", "&closefile",
      "clos&eallfiles", "&quit", "&edit", "&undo", "re&do", "cu&t", "&copy",
      "&paste", "&delete", "select&all", "dese&lect", "&find", "find&next",
      "findpre&vious", "&replace", "&gotoline", "&view", "&newview",
      "close&allviews", "&splitview", "&removeview", "splitter&orientation",
      "&horizontal", "&vertical", "view&mode", "&fullscreenmode", "&zoom",
      "zoom&in", "zoom&out", "zoomtopage&width", "zoomwhole&page", "zoom&factor",
      "&insert", "&format", "&go", "&up", "&back", "&forward", "&home", "&go",
      "&previouspage", "&nextpage", "&firstpage", "&lastpage", "read&updocument",
      "read&downdocument", "&back", "&forward", "&gotopage", "&bookmarks",
      "&addbookmark", "bookmark&tabsasfolder", "&editbookmarks",
      "&newbookmarksfolder", "&tools", "&settings", "&toolbars",
      "configure&shortcuts", "configuretool&bars", "&configure.*", "&help",
      ".+&handbook", "&whatsthis", "report&bug", "&aboutprocessing", "about&kde",
      "&beenden", "&suchen",   // de
      "&preferncias", "&sair", // Preferências; pt
      "&rechercher" };         // fr
    Pattern[] kdePreDefPats = new Pattern[kdePreDefStrs.length];
    for (int i = 0; i < kdePreDefStrs.length; i++) {
      kdePreDefPats[i] = Pattern.compile(kdePreDefStrs[i].replace("&",""));
    }

    final Pattern nonAAlpha = Pattern.compile("[^A-Za-z]");
    FontMetrics fmTmp = null;
    for (JMenuItem m : menu) {
      if (m != null) {
        fmTmp = m.getFontMetrics(m.getFont());
        break;
      }
    }
    if (fmTmp == null) return; // All null menuitems; would fail.
    final FontMetrics fm = fmTmp; // Hack for accessing variable in comparator.

    final Comparator<Character> charComparator = new Comparator<Character>() {
      char[] baddies = "qypgjaeiouQAEIOU".toCharArray();
      public int compare(Character ch1, Character ch2) {
        // Discriminates against descenders for readability, per MS
        // Human Interface Guide, and vowels per MS and Gnome.
        float w1 = fm.charWidth(ch1), w2 = fm.charWidth(ch2);
        for (char bad : baddies) {
          if (bad == ch1) w1 *= 0.66f;
          if (bad == ch2) w2 *= 0.66f;
        }
        return (int)Math.signum(w2 - w1);
      }
    };

    // Holds only [0-9a-z], not uppercase.
    // Prevents X != x, so "Save" and "Save As" aren't both given 'a'.
    final List<Character> taken = new ArrayList<Character>(menu.length);
    char firstChar;
    char[] cleanChars;
    Character[] cleanCharas;

    // METHOD 1: attempt to assign KDE defaults.
    for (JMenuItem jmi : menu) {
      if (jmi == null) continue;
      if (jmi.getText() == null) continue;
      jmi.setMnemonic(0); // Reset all mnemonics.
      String asciiName = nonAAlpha.matcher(jmi.getText()).replaceAll("");
      String lAsciiName = asciiName.toLowerCase();
      for (int i = 0; i < kdePreDefStrs.length; i++) {
        if (kdePreDefPats[i].matcher(lAsciiName).matches()) {
          char mnem = asciiName.charAt(kdePreDefStrs[i].indexOf("&"));
          jmi.setMnemonic(mnem);
          jmi.setDisplayedMnemonicIndex(jmi.getText().indexOf(mnem));
          taken.add((char)(mnem | 32)); // to lowercase
          break;
        }
      }
    }

    // Where KDE defaults fail, use an algorithm.
    algorithmicAssignment:
    for (JMenuItem jmi : menu) {
      if (jmi == null) continue;
      if (jmi.getText() == null) continue;
      if (jmi.getMnemonic() != 0) continue; // Already assigned.

      // The string can't be made lower-case as that would spoil
      // the width comparison.
      String cleanString = jmi.getText();
      if (cleanString.startsWith("sketchbook \u2192 "))
        cleanString = cleanString.substring(13);

      if (cleanString.length() == 0) continue;

      // First, ban letters by underscores.
      final List<Character> banned = new ArrayList<Character>();
      for (int i = 0; i < cleanString.length(); i++) {
        if (cleanString.charAt(i) == '_') {
          if (i > 0)
            banned.add(Character.toLowerCase(cleanString.charAt(i - 1)));
          if (i + 1 < cleanString.length())
            banned.add(Character.toLowerCase(cleanString.charAt(i + 1)));
        }
      }

      // METHOD 2: Uppercase starts of words.
      // Splitting into blocks of ASCII letters wouldn't work
      // because there could be non-ASCII letters in a word.
      for (String wd : cleanString.split("[^\\p{IsAlphabetic}]")) {
        if (wd.length() == 0) continue;
        firstChar = wd.charAt(0);
        if (taken.contains(Character.toLowerCase(firstChar))) continue;
        if (banned.contains(Character.toLowerCase(firstChar))) continue;
        if ('A' <= firstChar && firstChar <= 'Z') {
          jmi.setMnemonic(firstChar);
          jmi.setDisplayedMnemonicIndex(jmi.getText().indexOf(firstChar));
          taken.add((char)(firstChar | 32)); // tolowercase
          continue algorithmicAssignment;
        }
      }

      // METHOD 3: Lowercase starts of words.
      for (String wd : cleanString.split("[^\\p{IsAlphabetic}]")) {
        if (wd.length() == 0) continue;
        firstChar = wd.charAt(0);
        if (taken.contains(Character.toLowerCase(firstChar))) continue;
        if (banned.contains(Character.toLowerCase(firstChar))) continue;
        if ('a' <= firstChar && firstChar <= 'z') {
          jmi.setMnemonic(firstChar);
          jmi.setDisplayedMnemonicIndex(jmi.getText().indexOf(firstChar));
          taken.add(firstChar); // is lowercase
          continue algorithmicAssignment;
        }
      }

      // METHOD 4: Second wide-enough ASCII letter.
      cleanString = nonAAlpha.matcher(jmi.getText()).replaceAll("");
      if (cleanString.length() >= 2) {
        char ascii2nd = cleanString.charAt(1);
        if (!taken.contains((char)(ascii2nd|32)) &&
            !banned.contains((char)(ascii2nd|32)) &&
            fm.charWidth('A') <= 2*fm.charWidth(ascii2nd)) {
          jmi.setMnemonic(ascii2nd);
          jmi.setDisplayedMnemonicIndex(jmi.getText().indexOf(ascii2nd));
          taken.add((char)(ascii2nd|32));
          continue algorithmicAssignment;
        }
      }

      // METHOD 5: charComparator over all ASCII letters.
      cleanChars  = cleanString.toCharArray();
      cleanCharas = new Character[cleanChars.length];
      for (int i = 0; i < cleanChars.length; i++) {
        cleanCharas[i] = new Character(cleanChars[i]);
      }
      Arrays.sort(cleanCharas, charComparator); // sorts in increasing order
      for (char mnem : cleanCharas) {
        if (taken.contains(Character.toLowerCase(mnem))) continue;
        if (banned.contains(Character.toLowerCase(mnem))) continue;

        // NB: setMnemonic(char) doesn't want [^A-Za-z]
        jmi.setMnemonic(mnem);
        jmi.setDisplayedMnemonicIndex(jmi.getText().indexOf(mnem));
        taken.add(Character.toLowerCase(mnem));
        continue algorithmicAssignment;
      }

      // METHOD 6: Digits as last resort.
      for (char digit : jmi.getText().replaceAll("[^0-9]", "").toCharArray()) {
        if (taken.contains(digit)) continue;
        if (banned.contains(digit)) continue;
        jmi.setMnemonic(KeyEvent.VK_0 + digit - '0');
        // setDisplayedMnemonicIndex() unneeded: no case issues.
        taken.add(digit);
        continue algorithmicAssignment;
      }
    }

    // Finally, RECURSION.
    for (JMenuItem jmi : menu) {
      if (jmi instanceof JMenu) setMenuMnemsInside((JMenu) jmi);
    }
  }


  /**
   * As setMenuMnemonics(JMenuItem...).
   */
  static public void setMenuMnemonics(JMenuBar menubar) {
    JMenuItem[] items = new JMenuItem[menubar.getMenuCount()];
    for (int i = 0; i < items.length; i++) {
      items[i] = menubar.getMenu(i);
    }
    setMenuMnemonics(items);
  }


  /**
   * As setMenuMnemonics(JMenuItem...).
   */
  static public void setMenuMnemonics(JPopupMenu menu) {
    ArrayList<JMenuItem> items = new ArrayList<JMenuItem>();

    for (Component c : menu.getComponents()) {
      if (c instanceof JMenuItem) items.add((JMenuItem)c);
    }
    setMenuMnemonics(items.toArray(new JMenuItem[items.size()]));
  }


  /**
   * Calls setMenuMnemonics(JMenuItem...) on the sub-elements only.
   */
  static public void setMenuMnemsInside(JMenu menu) {
    JMenuItem[] items = new JMenuItem[menu.getItemCount()];
    for (int i = 0; i < items.length; i++) {
      items[i] = menu.getItem(i);
    }
    setMenuMnemonics(items);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static public Dimension getScreenSize() {
    return awtToolkit.getScreenSize();
  }


  /**
   * Return an Image object from inside the Processing 'lib' folder.
   * Moved here so that Base can stay headless.
   */
  static public Image getLibImage(String filename) {
    ImageIcon icon = getLibIcon(filename);
    return (icon == null) ? null : icon.getImage();
  }


  /**
   * Get an ImageIcon from the Processing 'lib' folder.
   * @since 3.0a6
   */
  static public ImageIcon getLibIcon(String filename) {
    File file = Platform.getContentFile("lib/" + filename);
    if (!file.exists()) {
//      System.err.println("does not exist: " + file);
      return null;
    }
    return new ImageIcon(file.getAbsolutePath());
  }


  static public ImageIcon getIconX(File dir, String base) {
    return getIconX(dir, base, 0);
  }


  /**
   * Get an icon of the format base-NN.png where NN is the size, but if it's
   * a hidpi display, get the NN*2 version automatically, sized at NN
   */
  static public ImageIcon getIconX(File dir, String base, int size) {
    final int scale = Toolkit.highResDisplay() ? 2 : 1;
    String filename = (size == 0) ?
      (base + "-" + scale + "x.png") :
      (base + "-" + (size*scale) + ".png");
//    File file = Platform.getContentFile("lib/" + filename);
    File file = new File(dir, filename);
    if (!file.exists()) {
//      System.err.println("does not exist: " + file);
      return null;
    }
    ImageIcon outgoing = new ImageIcon(file.getAbsolutePath()) {
      @Override
      public int getIconWidth() {
        return super.getIconWidth() / scale;
      }

      @Override
      public int getIconHeight() {
        return super.getIconHeight() / scale;
      }

      @Override
      public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        ImageObserver imageObserver = getImageObserver();
        if (imageObserver == null) {
          imageObserver = c;
        }
        g.drawImage(getImage(), x, y, getIconWidth(), getIconHeight(), imageObserver);
      }
    };
    return outgoing;
  }


  /**
   * Get an image icon with hi-dpi support. Pulls 1x or 2x versions of the
   * file depending on the display type, but sizes them based on 1x.
   */
  static public ImageIcon getLibIconX(String base) {
    return getLibIconX(base, 0);
  }


  static public ImageIcon getLibIconX(String base, int size) {
    return getIconX(Platform.getContentFile("lib"), base, size);
  }


  static List<Image> iconImages;


  // Deprecated version of the function, but can't get rid of it without
  // breaking tools and modes (they'd only require a recompile, but they would
  // no longer be backwards compatible.
  static public void setIcon(Frame frame) {
    setIcon((Window) frame);
  }


  /**
   * Give this Frame the Processing icon set. Ignored on OS X, because they
   * thought different and made this function set the minified image of the
   * window, not the window icon for the dock or cmd-tab.
   */
  static public void setIcon(Window window) {
    if (!Platform.isMacOS()) {
      if (iconImages == null) {
        iconImages = new ArrayList<Image>();
        final int[] sizes = { 16, 32, 48, 64, 128, 256, 512 };
        for (int sz : sizes) {
          iconImages.add(Toolkit.getLibImage("icons/pde-" + sz + ".png"));
        }
      }
      window.setIconImages(iconImages);
    }
  }


  static public Shape createRoundRect(float x1, float y1, float x2, float y2,
                                      float tl, float tr, float br, float bl) {
    GeneralPath path = new GeneralPath();
//    vertex(x1+tl, y1);

    if (tr != 0) {
      path.moveTo(x2-tr, y1);
      path.quadTo(x2, y1, x2, y1+tr);
    } else {
      path.moveTo(x2, y1);
    }
    if (br != 0) {
      path.lineTo(x2, y2-br);
      path.quadTo(x2, y2, x2-br, y2);
    } else {
      path.lineTo(x2, y2);
    }
    if (bl != 0) {
      path.lineTo(x1+bl, y2);
      path.quadTo(x1, y2, x1, y2-bl);
    } else {
      path.lineTo(x1, y2);
    }
    if (tl != 0) {
      path.lineTo(x1, y1+tl);
      path.quadTo(x1, y1, x1+tl, y1);
    } else {
      path.lineTo(x1, y1);
    }
    path.closePath();
    return path;
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


  /**
   * Handles scaling for high-res displays, also sets text anti-aliasing
   * options to be far less ugly than the defaults.
   * Moved to a utility function because it's used in several classes.
   * @return a Graphics2D object, as a bit o sugar
   */
  static public Graphics2D prepareGraphics(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;

    if (Toolkit.highResDisplay()) {
      // scale everything 2x, will be scaled down when drawn to the screen
      g2.scale(2, 2);
    }
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    if (Toolkit.highResDisplay()) {
      // Looks great on retina, not so great (with our font) on 1x
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                          RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
    }
    return g2;
  }


//  /**
//   * Prepare and offscreen image that's sized for this Component, 1x or 2x
//   * depending on whether this is a retina display or not.
//   * @param comp
//   * @param image
//   * @return
//   */
//  static public Image prepareOffscreen(Component comp, Image image) {
//    Dimension size = comp.getSize();
//    Image offscreen = image;
//    if (image == null ||
//        image.getWidth(null) != size.width ||
//        image.getHeight(null) != size.height) {
//      if (Toolkit.highResDisplay()) {
//        offscreen = comp.createImage(size.width*2, size.height*2);
//      } else {
//        offscreen = comp.createImage(size.width, size.height);
//      }
//    }
//    return offscreen;
//  }


//  static final Color CLEAR_COLOR = new Color(0, true);
//
//  static public void clearGraphics(Graphics g, int width, int height) {
//    g.setColor(CLEAR_COLOR);
//    g.fillRect(0, 0, width, height);
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static Boolean highResProp;


  static public boolean highResDisplay() {
    if (highResProp == null) {
      highResProp = checkRetina();
    }
    return highResProp;
  }


  // This should probably be reset each time there's a display change.
  // A 5-minute search didn't turn up any such event in the Java API.
  // Also, should we use the Toolkit associated with the editor window?
  static private boolean checkRetina() {
    if (Platform.isMacOS()) {
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


  static final char GREEK_SMALL_LETTER_ALPHA = '\u03B1';  // α
  static final char GREEK_CAPITAL_LETTER_OMEGA = '\u03A9';  // ω


  // Gets the plain (not bold, not italic) version of each
  static private List<Font> getMonoFontList() {
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
      if (font.getStyle() == Font.PLAIN &&
          font.canDisplay('i') && font.canDisplay('M') &&
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

//          //PApplet.printArray(font.getAvailableAttributes());
//          Map<TextAttribute,?> attr = font.getAttributes();
//          System.out.println(font.getFamily() + " > " + font.getName());
//          System.out.println(font.getAttributes());
//          System.out.println("  " + attr.get(TextAttribute.WEIGHT));
//          System.out.println("  " + attr.get(TextAttribute.POSTURE));

          outgoing.add(font);
//          System.out.println("  good " + w);
        }
      }
    }
    return outgoing;
  }


  static public String[] getMonoFontFamilies() {
    HashSet<String> families = new HashSet<String>();
    for (Font font : getMonoFontList()) {
      families.add(font.getFamily());
    }
    String[] names = families.toArray(new String[0]);
    Arrays.sort(names);
    return names;
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
        //monoBoldFont = createFont("SourceCodePro-Semibold.ttf", size);
        monoBoldFont = createFont("SourceCodePro-Bold.ttf", size);

        // additional language constraints
        if ("el".equals(Language.getLanguage())) {
          if (!monoFont.canDisplay(GREEK_SMALL_LETTER_ALPHA) ||
              !monoFont.canDisplay(GREEK_CAPITAL_LETTER_OMEGA)) {
            monoFont = createFont("AnonymousPro-Regular.ttf", size);
            monoBoldFont = createFont("AnonymousPro-Bold.ttf", size);
          }
        }
      } catch (Exception e) {
        Messages.loge("Could not load mono font", e);
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

        // additional language constraints
        if ("el".equals(Language.getLanguage())) {
          if (!sansFont.canDisplay(GREEK_SMALL_LETTER_ALPHA) ||
              !sansFont.canDisplay(GREEK_CAPITAL_LETTER_OMEGA)) {
            sansFont = createFont("Carlito-Regular.ttf", size);
            sansBoldFont = createFont("Carlito-Bold.ttf", size);
          }
        }
      } catch (Exception e) {
        Messages.loge("Could not load sans font", e);
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


  /**
   * Get a font from the JRE lib/fonts folder. Our default fonts are also
   * installed there so that the monospace (and others) can be used by other
   * font listing calls (i.e. it appears in the list of monospace fonts in
   * the Preferences window, and can be used by HTMLEditorKit for WebFrame).
   */
  static private Font createFont(String filename, int size) throws IOException, FontFormatException {
    // Can't use Base.getJavaHome(), because if we're not using our local JRE,
    // we likely have bigger problems with how things are running.
    File fontFile = new File(System.getProperty("java.home"), "lib/fonts/" + filename);
    if (!fontFile.exists()) {
      // if we're debugging from Eclipse, grab it from the work folder (user.dir is /app)
      fontFile = new File(System.getProperty("user.dir"), "../build/shared/lib/fonts/" + filename);
    }
    if (!fontFile.exists()) {
      // if we're debugging the new Java Mode from Eclipse, paths are different
      fontFile = new File(System.getProperty("user.dir"), "../../shared/lib/fonts/" + filename);
    }
    if (!fontFile.exists()) {
      String msg = "Could not find required fonts. ";
      // This gets the JAVA_HOME for the *local* copy of the JRE installed with
      // Processing. If it's not using the local JRE, it may be because of this
      // launch4j bug: https://github.com/processing/processing/issues/3543
      if (hasNonAsciiChars(Platform.getJavaHome().getAbsolutePath())) {
        msg += "Trying moving Processing\n" +
          "to a location with only ASCII characters in the path.";
      } else {
        msg += "Please reinstall Processing.";
      }
      Messages.showError("Font Sadness", msg, null);
    }

    BufferedInputStream input = new BufferedInputStream(new FileInputStream(fontFile));
    Font font = Font.createFont(Font.TRUETYPE_FONT, input);
    input.close();
    return font.deriveFont((float) size);
  }


  static private final boolean hasNonAsciiChars(String what) {
    for (char c : what.toCharArray()) {
      if (c < 32 || c > 127) return true;
    }
    return false;
  }


  /**
   * Synthesized replacement for FontMetrics.getAscent(), which is dreadfully
   * inaccurate and inconsistent across platforms.
   */
  static public double getAscent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    FontRenderContext frc = g2.getFontRenderContext();
    //return new TextLayout("H", font, frc).getBounds().getHeight();
    return new TextLayout("H", g.getFont(), frc).getBounds().getHeight();
  }


//  /** Do not use or rely upon presence of this method: not approved as final API. */
//  static public void debugOpacity(Component comp) {
//    //Component parent = comp.getParent();
//    while (comp != null) {
//      //EditorConsole.systemOut.println("parent is " + parent + " " + parent.isOpaque());
//      //EditorConsole.systemOut.println(parent.getClass().getName() + " " + (parent.isOpaque() ? "OPAQUE" : ""));
//      System.out.println(comp.getClass().getName() + " " + (comp.isOpaque() ? "OPAQUE" : ""));
//      comp = comp.getParent();
//    }
//    //EditorConsole.systemOut.println();
//    System.out.println();
//  }


  static public int getMenuItemIndex(JMenu menu, JMenuItem item) {
    int index = 0;
    for (Component comp : menu.getMenuComponents()) {
      if (comp == item) {
        return index;
      }
      index++;
    }
    return -1;
  }
  
  public static String sanitizeHtmlTags(String stringIn) {
    stringIn = stringIn.replaceAll("<", "&lt;");
    stringIn = stringIn.replaceAll(">", "&gt;");
    return stringIn;
  }


  /**
   * This has a [link](http://example.com/) in [it](http://example.org/).
   *
   * Becomes...
   *
   * This has a <a href="http://example.com/">link</a> in <a
   * href="http://example.org/">it</a>.
   */
  public static String toHtmlLinks(String stringIn) {
    Pattern p = Pattern.compile("\\[(.*?)\\] ?\\((.*?)\\)");
    Matcher m = p.matcher(stringIn);

    StringBuilder sb = new StringBuilder();

    int start = 0;
    while (m.find(start)) {
      sb.append(stringIn.substring(start, m.start()));

      String text = m.group(1);
      String url = m.group(2);

      sb.append("<a href=\"");
      sb.append(url);
      sb.append("\">");
      sb.append(text);
      sb.append("</a>");

      start = m.end();
    }
    sb.append(stringIn.substring(start));
    return sb.toString();
  }
  /**
   * This has a [link](http://example.com/) in [it](http://example.org/).
   *
   * Becomes...
   *
   * This has a link in it.
   */
  public static String toPlainText(String stringIn) {
    Pattern p = Pattern.compile("\\[(.*?)\\] ?\\((.*?)\\)");
    Matcher m = p.matcher(stringIn);

    StringBuilder sb = new StringBuilder();

    int start = 0;
    while (m.find(start)) {
      sb.append(stringIn.substring(start, m.start()));

      String text = m.group(1);

      sb.append(text);

      start = m.end();
    }
    sb.append(stringIn.substring(start));
    return sb.toString();
  }
}
