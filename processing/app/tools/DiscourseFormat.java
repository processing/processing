/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2005-06 Ignacio Manuel González Moreta
  Copyright (c) 2006 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.tools;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.Segment;

import processing.app.*;
import processing.app.syntax.*;
import processing.core.PApplet;


/**
 * Format for Discourse Tool
 * <p/>
 * Original code by <A HREF="http://usuarios.iponet.es/imoreta">owd</A>.
 * Revised and updated for revision 0108 by Ben Fry (10 March 2006).
 * This code will later be removed but is included with release 0108+
 * while features for the "Tools" menu are in testing.
 * <p/>
 * Notes from the original source:
 * Discourse.java This is a dirty-mix source.
 * NOTE that: No macs and no keyboard. Unreliable source.
 * Only format processing code using fontMetrics.
 * It works under my windows XP + PentiumIV + Processing 0091.
 */
public class DiscourseFormat /*extends JPanel implements WindowListener*/ {

  //static final String WINDOW_TITLE = "Code ready: processing.org/discourse";
  static final String WINDOW_TITLE = "Format for Discourse by owd";

  // p5 icon for the window
  static Image icon;

  Editor editor;
  JEditTextArea textarea;

  // Parent editor JTextArea
  JEditTextArea parent;

  // False listener (no NullPointerException at processKeyEvent,
  // but gives other problems like the § on the Tab)
  //DiscourseListener listener;

  JFrame frame;

  // One window only (if window exists, update())
  //static boolean active = false;

  //Discourse.formatDiscourse(textarea);

  /**
   * Creates a new window with the formated (YaBB tags) sketchcode
   * from the actual Processing Tab ready to send to the processing discourse
   * web (copy & paste)
   */
  public DiscourseFormat(Editor editor) {
    //super(new GridBagLayout());

    this.editor = editor;
    this.parent = editor.textarea;

    textarea = new JEditTextArea(new PdeTextAreaDefaults());
    textarea.setRightClickPopup(new DiscourseTextAreaPopup());
    textarea.setTokenMarker(new PdeKeywords());
    textarea.setHorizontalOffset(6);

    //GridBagConstraints c = new GridBagConstraints();
    //c.fill = GridBagConstraints.BOTH;
    //c.weightx = 1.0;
    //c.weighty = 1.0;
    //add(textarea, c);

    textarea.setEditable(false);

    //frame.addWindowListener(this);
    //listener = new DiscourseListener(textarea);
    //textarea.editorListener = parent.editorListener;

    //Make sure we have nice window decorations.
    //Sure... false, false...
    //JFrame.setDefaultLookAndFeelDecorated(false);

    // Create and set up the window.
    frame = new JFrame(WINDOW_TITLE);
    frame.setSize(500, 500);

    // set the window icon
    try {
      icon = Base.getImage("icon.gif", frame);
      frame.setIconImage(icon);
    } catch (Exception e) {  } // fail silently, no big whup
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    // Create and set up the content pane.
    //JComponent newContentPane = new Discourse();
    //newContentPane.setOpaque(true); //content panes must be opaque
    //frame.setContentPane(newContentPane);
    Container pain = frame.getContentPane();
    pain.setLayout(new BorderLayout());
    pain.add(textarea, BorderLayout.CENTER);

    frame.setResizable(true);

    // Display the window
    frame.pack();
    frame.setLocation(100, 100);
    //frame.setVisible(true);
  }


  public void show() {
    // Format and render sketchcode

    // [code] tag cancels other tags, using [quote]
    StringBuffer cf = new StringBuffer("[quote] \n \n");

    // Line by line
    for (int i = 0; i < parent.getLineCount(); i++) {
      cf.append(formatCode(i));
    }

    cf.append("\n [/quote]");

    // Send the text to the textarea
    textarea.setText(cf.toString());
    textarea.select(0, 0);

    frame.show();
  }


  /*
  // Update contents
  public static void update() {
    //
    preliminars();
    frame.toFront();
  }
  */

  /** Read parent textarea */
  /*
  public static void formatDiscourse(JEditTextArea parentTxa) {
    //String code = parent.getText(0, textarea.getDocumentLength());
    parent = parentTxa;
    if (Discourse.active) {
      // Discourse window exists
      Discourse.update();
    } else {
      // Creates a new discourse window
      Discourse.createAndShowGUI();
    }
  }
  */

  /*
  // Returns a string from a char
  static String character(char ch) {
    return String.valueOf(ch);
    //Character Ch = new Character(ch);
    //return Ch.toString();
  }
  */


  // A terrible headache...
  public String formatCode(int line) {
    StringBuffer cf = new StringBuffer();

    // Segment
    Segment lineSegment = new Segment();

    TextAreaPainter painter = parent.getPainter();
    TokenMarker tokenMarker = parent.getTokenMarker();

    // Use painter's cached info for speed
    FontMetrics fm = painter.getFontMetrics();

    // Jump empty lines
    // if (parent.getLineLength(line) == 0) cf.concat(character('\n'));
    // return cf;

    // get line text from parent textarea
    parent.getLineText(line, lineSegment);

    char[] segmentArray = lineSegment.array;
    int limit = lineSegment.getEndIndex();
    int segmentOffset = lineSegment.offset;
    int segmentCount = lineSegment.count;
    int width = 0; //parent.getHorizontalOffset();

    int x = 0; //parent.getHorizontalOffset();

    // If syntax coloring is disabled, do simple translation
    if (tokenMarker == null) {
      for (int j = 0; j < segmentCount; j++) {
        char c = segmentArray[j + segmentOffset];
        cf = cf.append(c); //concat(character(c));
        int charWidth;
        if (c == '\t') {
          charWidth = (int) painter.nextTabStop(width, j) - width;
        } else {
          charWidth = fm.charWidth(c);
        }
        width += charWidth;
      }

    } else {
      // If syntax coloring is enabled, we have to do this
      // because tokens can vary in width
      Token tokens;
      if ((painter.getCurrentLineIndex() == line) &&
          (painter.getCurrentLineTokens() != null)) {
        tokens = painter.getCurrentLineTokens();

      } else {
        painter.setCurrentLineIndex(line);
        //painter.currentLineIndex = line;
        painter.setCurrentLineTokens(tokenMarker.markTokens(lineSegment, line));
        tokens = painter.getCurrentLineTokens();
      }

      int offset = 0;
      Toolkit toolkit = painter.getToolkit();
      Font defaultFont = painter.getFont();
      SyntaxStyle[] styles = painter.getStyles();

      for (;;) {
        byte id = tokens.id;
        if (id == Token.END) {
          char c = segmentArray[segmentOffset + offset];
          if (segmentOffset + offset < limit) {
            cf.append(c);
          } else {
            cf.append('\n');
          }
          return cf.toString();
        }
        if (id == Token.NULL) {
          fm = painter.getFontMetrics();
        } else {
          // Place open tags []
          //cf.append("[color=" + color() + "]");
          cf.append("[color=#");
          cf.append(PApplet.hex(styles[id].getColor().getRGB() & 0xFFFFFF, 6));
          cf.append("]");

          if (styles[id].isBold())
            cf.append("[b]");

          fm = styles[id].getFontMetrics(defaultFont);
        }
        int length = tokens.length;

        for (int j = 0; j < length; j++) {
          char c = segmentArray[segmentOffset + offset + j];
          cf.append(c);
          // Place close tags [/]
          if (j == (length - 1) && id != Token.NULL && styles[id].isBold())
            cf.append("[/b]");
          if (j == (length - 1) && id != Token.NULL)
            cf.append("[/color]");
          int charWidth;
          if (c == '\t') {
            charWidth = (int) painter
              .nextTabStop(width, offset + j)
              - width;
          } else {
            charWidth = fm.charWidth(c);
          }
          width += charWidth;
        }
        offset += length;
        tokens = tokens.next;
      }
    }
    return cf.toString();
  }


  /* Return a string [#rrggbb] from color */
  //public static String color(Color rgbColor) {
    /*
    int r = rgbColor.getRed();
    int g = rgbColor.getGreen();
    int b = rgbColor.getBlue();

    String rx = r >= 16 ? Integer.toHexString(r) : "0"
      + Integer.toHexString(r);
    String gx = g >= 16 ? Integer.toHexString(g) : "0"
      + Integer.toHexString(g);
    String bx = b >= 16 ? Integer.toHexString(b) : "0"
      + Integer.toHexString(b);
    rx = rx.toUpperCase();
    gx = gx.toUpperCase();
    bx = bx.toUpperCase();

    return "#" + rx + gx + bx;
    */

    /*
    return "#" +
      PApplet.hex(rgbColor.getRed(), 2) +
      PApplet.hex(rgbColor.getGreen(), 2) +
      PApplet.hex(rgbColor.getBlue(), 2);
    */
    //return "#" + PApplet.hex(rgbColor.getRGB() & 0xFFFFFF, 6);
  //}

  /*
  // M*erda... voids, voids, voids... Really needed?
  // Eclipse says YES (?)
  public void windowOpened(WindowEvent e) {
  }

  public void windowIconified(WindowEvent e) {
  }

  public void windowDeiconified(WindowEvent e) {
  }

  public void windowActivated(WindowEvent e) {
  }

  public void windowDeactivated(WindowEvent e) {
  }

  public void windowClosing(WindowEvent arg0) {
  }
  */


  /**
   * Returns the discourse popup menu. Another features can be added: format
   * selected text with a determinated tag (I'm thinking about [url]selected
   * text[/url])
   */
  class DiscourseTextAreaPopup extends JPopupMenu {
    //protected ReferenceKeys referenceItems = new ReferenceKeys();
    //String currentDir = System.getProperty("user.dir");
    //String referenceFile = null;
    //JMenuItem cutItem, copyItem;
    JMenuItem copyItem;
    //JMenuItem referenceItem;

    public DiscourseTextAreaPopup() {
      JMenuItem item;

      copyItem = new JMenuItem("Copy");
      copyItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            textarea.copy();
          }
        });
      this.add(copyItem);

      item = new JMenuItem("Select All");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            textarea.selectAll();
          }
        });
      this.add(item);
    }

    // if no text is selected, disable copy menu item
    public void show(Component component, int x, int y) {
      if (textarea.isSelectionActive()) {
        copyItem.setEnabled(true);

      } else {
        copyItem.setEnabled(false);
      }
      super.show(component, x, y);
    }
  }


  /*
  // A false listener (use the mouse)
  public class DiscourseListener {

    public DiscourseListener(JEditTextArea thisTextarea) {
      // I'm a... I know this gives peoblems, but all this code
      // is a funny hacking experiment
      thisTextarea.editorListener = parent.editorListener;
    }

    public boolean keyPressed(KeyEvent event) {
      System.out.println("Is your mouse lone some tonight...");
      return false;
    }
  }
  */
}