/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package processing.mode.experimental;
import static processing.mode.experimental.ExperimentalMode.log;
import galsasson.mode.tweak.ColorControlBox;
import galsasson.mode.tweak.ColorSelector;
import galsasson.mode.tweak.Handle;
import galsasson.mode.tweak.Settings;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;

import processing.app.SketchCode;
import processing.app.syntax.TextAreaDefaults;
import processing.app.syntax.TokenMarker;

/**
 * Customized line painter. Adds support for background colors, left hand gutter
 * area with background color and text.
 * 
 * @author Martin Leopold <m@martinleopold.com>
 */
public class TextAreaPainter extends processing.app.syntax.TextAreaPainter
	implements MouseListener, MouseMotionListener {

  protected TextArea ta; // we need the subclassed textarea

  protected ErrorCheckerService errorCheckerService;

  /**
   * Error line underline color
   */
  public Color errorColor = new Color(0xED2630);

  /**
   * Warning line underline color
   */

  public Color warningColor = new Color(0xFFC30E);

  /**
   * Color of Error Marker
   */
  public Color errorMarkerColor = new Color(0xED2630);

  /**
   * Color of Warning Marker
   */
  public Color warningMarkerColor = new Color(0xFFC30E);

  static int ctrlMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  public TextAreaPainter(TextArea textArea, TextAreaDefaults defaults) {
    super(textArea, defaults);
    ta = textArea;
    addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent evt) {
        if(ta.editor.hasJavaTabs) return; // Ctrl + Click disabled for java tabs
        if (evt.getButton() == MouseEvent.BUTTON1) {
          if (evt.isControlDown() || evt.isMetaDown())
            handleCtrlClick(evt);
        }
      }
    });

    // TweakMode code
	interactiveMode = false;
	cursorType = Cursor.DEFAULT_CURSOR;
  }

//    public void processKeyEvent(KeyEvent evt) {
//    	log(evt);
//    }

  void handleCtrlClick(MouseEvent evt) {
    log("--handleCtrlClick--");
    int off = ta.xyToOffset(evt.getX(), evt.getY());
    if (off < 0)
      return;
    int line = ta.getLineOfOffset(off);
    if (line < 0)
      return;
    String s = ta.getLineText(line);
    if (s == null)
      return;
    else if (s.length() == 0)
      return;
    else {
      int x = ta.xToOffset(line, evt.getX()), x2 = x + 1, x1 = x - 1;
      log("x="+x);
      int xLS = off - ta.getLineStartNonWhiteSpaceOffset(line);
      if (x < 0 || x >= s.length())
        return;
      String word = s.charAt(x) + "";
      if (s.charAt(x) == ' ')
        return;
      if (!(Character.isLetterOrDigit(s.charAt(x)) || s.charAt(x) == '_' || s
          .charAt(x) == '$'))
        return;
      int i = 0;
      while (true) {
        i++;
        if (x1 >= 0 && x1 < s.length()) {
          if (Character.isLetter(s.charAt(x1)) || s.charAt(x1) == '_') {
            word = s.charAt(x1--) + word;
            xLS--;
          } else
            x1 = -1;
        } else
          x1 = -1;

        if (x2 >= 0 && x2 < s.length()) {
          if (Character.isLetterOrDigit(s.charAt(x2)) || s.charAt(x2) == '_'
              || s.charAt(x2) == '$')
            word = word + s.charAt(x2++);
          else
            x2 = -1;
        } else
          x2 = -1;

        if (x1 < 0 && x2 < 0)
          break;
        if (i > 200) {
          // time out!
          // System.err.println("Whoopsy! :P");
          break;
        }
      }
      if (Character.isDigit(word.charAt(0)))
        return;
      
      log(errorCheckerService.mainClassOffset + line +
      "|" + line + "| offset " + xLS + word + " <= \n");
      errorCheckerService.getASTGenerator()
          .scrollToDeclaration(line, word, xLS);
    }
  }

  private void loadTheme(ExperimentalMode mode) {
    errorColor = mode.getThemeColor("editor.errorcolor", errorColor);
    warningColor = mode.getThemeColor("editor.warningcolor", warningColor);
    errorMarkerColor = mode.getThemeColor("editor.errormarkercolor",
                                          errorMarkerColor);
    warningMarkerColor = mode.getThemeColor("editor.warningmarkercolor",
                                            warningMarkerColor);
  }

  /**
   * Paint a line. Paints the gutter (with background color and text) then the
   * line (background color and text).
   * 
   * @param gfx
   *          the graphics context
   * @param tokenMarker
   * @param line
   *          0-based line number
   * @param x
   *          horizontal position
   */
  @Override
  protected void paintLine(Graphics gfx, TokenMarker tokenMarker, int line,
                           int x) {
    try {
      //TODO: This line is causing NPE's randomly ever since I added the toggle for 
      //Java Mode/Debugger toolbar.
      super.paintLine(gfx, tokenMarker, line, x + ta.getGutterWidth());
    } catch (Exception e) {
      log(e.getMessage());
    }
    if(ta.editor.debugToolbarEnabled != null && ta.editor.debugToolbarEnabled.get()){
      // paint gutter
      paintGutterBg(gfx, line, x);
  
      // disabled line background after P5 2.1, since it adds highlight by default
      //paintLineBgColor(gfx, line, x + ta.getGutterWidth()); 
  
      paintGutterLine(gfx, line, x);
  
      // paint gutter symbol
      paintGutterText(gfx, line, x);
      
    }   
    paintErrorLine(gfx, line, x);
  }

  /**
   * Paint the gutter background (solid color).
   * 
   * @param gfx
   *          the graphics context
   * @param line
   *          0-based line number
   * @param x
   *          horizontal position
   */
  protected void paintGutterBg(Graphics gfx, int line, int x) {
    gfx.setColor(ta.gutterBgColor);
    int y = ta.lineToY(line) + fm.getLeading() + fm.getMaxDescent();
    gfx.fillRect(0, y, ta.getGutterWidth(), fm.getHeight());
  }

  /**
   * Paint the vertical gutter separator line.
   * 
   * @param gfx
   *          the graphics context
   * @param line
   *          0-based line number
   * @param x
   *          horizontal position
   */
  protected void paintGutterLine(Graphics gfx, int line, int x) {
    int y = ta.lineToY(line) + fm.getLeading() + fm.getMaxDescent();
    gfx.setColor(ta.gutterLineColor);
    gfx.drawLine(ta.getGutterWidth(), y, ta.getGutterWidth(),
                 y + fm.getHeight());
  }

  /**
   * Paint the gutter text.
   * 
   * @param gfx
   *          the graphics context
   * @param line
   *          0-based line number
   * @param x
   *          horizontal position
   */
  protected void paintGutterText(Graphics gfx, int line, int x) {
    String text = ta.getGutterText(line);
    if (text == null) {
      return;
    }

    gfx.setFont(getFont());
    Color textColor = ta.getGutterTextColor(line);
    if (textColor == null) {
      gfx.setColor(getForeground());
    } else {
      gfx.setColor(textColor);
    }
    int y = ta.lineToY(line) + fm.getHeight();

    // draw 4 times to make it appear bold, displaced 1px to the right, to the bottom and bottom right.
    //int len = text.length() > ta.gutterChars ? ta.gutterChars : text.length();
    Utilities.drawTabbedText(new Segment(text.toCharArray(), 0, text.length()),
                             ta.getGutterMargins(), y, gfx, this, 0);
    Utilities.drawTabbedText(new Segment(text.toCharArray(), 0, text.length()),
                             ta.getGutterMargins() + 1, y, gfx, this, 0);
    Utilities.drawTabbedText(new Segment(text.toCharArray(), 0, text.length()),
                             ta.getGutterMargins(), y + 1, gfx, this, 0);
    Utilities.drawTabbedText(new Segment(text.toCharArray(), 0, text.length()),
                             ta.getGutterMargins() + 1, y + 1, gfx, this, 0);
  }

  /**
   * Paint the background color of a line.
   * 
   * @param gfx
   *          the graphics context
   * @param line
   *          0-based line number
   * @param x
   */
  protected void paintLineBgColor(Graphics gfx, int line, int x) {
    int y = ta.lineToY(line);
    y += fm.getLeading() + fm.getMaxDescent();
    int height = fm.getHeight();

    // get the color
    Color col = ta.getLineBgColor(line);
    //System.out.print("bg line " + line + ": ");
    // no need to paint anything
    if (col == null) {
      //log("none");
      return;
    }
    // paint line background
    gfx.setColor(col);
    gfx.fillRect(0, y, getWidth(), height);
  }

  /**
   * Paints the underline for an error/warning line
   * 
   * @param gfx
   *          the graphics context
   * @param tokenMarker
   * @param line
   *          0-based line number: NOTE
   * @param x
   */
  protected void paintErrorLine(Graphics gfx, int line, int x) {
    if (errorCheckerService == null) {
      return;
    }

    if (errorCheckerService.problemsList == null) {
      return;
    }

    boolean notFound = true;
    boolean isWarning = false;
    Problem problem = null;
    
    // Check if current line contains an error. If it does, find if it's an
    // error or warning
    for (ErrorMarker emarker : errorCheckerService.getEditor().errorBar.errorPoints) {
      if (emarker.getProblem().getLineNumber() == line) {
        notFound = false;
        if (emarker.getType() == ErrorMarker.Warning) {
          isWarning = true;
        }
        problem = emarker.getProblem();
        //log(problem.toString());
        break;
      }
    }

    if (notFound) {
      return;
    }

    // Determine co-ordinates
    // log("Hoff " + ta.getHorizontalOffset() + ", " +
    // horizontalAdjustment);
    int y = ta.lineToY(line);
    y += fm.getLeading() + fm.getMaxDescent();
//    int height = fm.getHeight();
    int start = ta.getLineStartOffset(line) + problem.getPDELineStartOffset();
    int pLength = problem.getPDELineStopOffset() + 1
        - problem.getPDELineStartOffset();
    
    try {
      String badCode = null;
      String goodCode = null;
      try {
        badCode = ta.getDocument().getText(start, pLength);
        goodCode = ta.getDocument().getText(ta.getLineStartOffset(line),
                                            problem.getPDELineStartOffset());
        //log("paintErrorLine() LineText GC: " + goodCode);
        //log("paintErrorLine() LineText BC: " + badCode);
      } catch (BadLocationException bl) {
        // Error in the import statements or end of code.
        // System.out.print("BL caught. " + ta.getLineCount() + " ,"
        // + line + " ,");
        // log((ta.getLineStopOffset(line) - start - 1));
        return;
      }

      // Take care of offsets
      int aw = fm.stringWidth(trimRight(badCode)) + ta.getHorizontalOffset(); // apparent width. Whitespaces
      // to the left of line + text
      // width
      int rw = fm.stringWidth(badCode.trim()); // real width
      int x1 = fm.stringWidth(goodCode) + (aw - rw), y1 = y + fm.getHeight()
          - 2, x2 = x1 + rw;
      // Adding offsets for the gutter
      x1 += ta.getGutterWidth();
      x2 += ta.getGutterWidth();

      // gfx.fillRect(x1, y, rw, height);

      // Let the painting begin!
      
      // Little rect at starting of a line containing errors - disabling it for now
//      gfx.setColor(errorMarkerColor);
//      if (isWarning) {
//        gfx.setColor(warningMarkerColor);
//      }
//      gfx.fillRect(1, y + 2, 3, height - 2);

      
      gfx.setColor(errorColor);
      if (isWarning) {
        gfx.setColor(warningColor);
      }
      int xx = x1;

      // Draw the jagged lines
      while (xx < x2) {
        gfx.drawLine(xx, y1, xx + 2, y1 + 1);
        xx += 2;
        gfx.drawLine(xx, y1 + 1, xx + 2, y1);
        xx += 2;
      }
    } catch (Exception e) {
      System.out
          .println("Looks like I messed up! XQTextAreaPainter.paintLine() : "
              + e);
      //e.printStackTrace();
    }

    // Won't highlight the line. Select the text instead.
    // gfx.setColor(Color.RED);
    // gfx.fillRect(2, y, 3, height);
  }

  /**
   * Trims out trailing whitespaces (to the right)
   * 
   * @param string
   * @return - String
   */
  private String trimRight(String string) {
    String newString = "";
    for (int i = 0; i < string.length(); i++) {
      if (string.charAt(i) != ' ') {
        newString = string.substring(0, i) + string.trim();
        break;
      }
    }
    return newString;
  }

  /**
   * Sets ErrorCheckerService and loads theme for TextAreaPainter(XQMode)
   * 
   * @param ecs
   * @param mode
   */
  public void setECSandTheme(ErrorCheckerService ecs, ExperimentalMode mode) {
    this.errorCheckerService = ecs;
    loadTheme(mode);
  }

  public String getToolTipText(java.awt.event.MouseEvent evt) {
    if(ta.editor.hasJavaTabs) return null; // disabled for java tabs
    int off = ta.xyToOffset(evt.getX(), evt.getY());
    if (off < 0)
      return null;
    int line = ta.getLineOfOffset(off);
    if (line < 0)
      return null;
    String s = ta.getLineText(line);
    if (s == null)
      return evt.toString();
    else if (s.length() == 0)
      return null;
    else {
      int x = ta.xToOffset(line, evt.getX()), x2 = x + 1, x1 = x - 1;
      int xLS = off - ta.getLineStartNonWhiteSpaceOffset(line);
      if (x < 0 || x >= s.length())
        return null;
      String word = s.charAt(x) + "";
      if (s.charAt(x) == ' ')
        return null;
      if (!(Character.isLetterOrDigit(s.charAt(x)) || s.charAt(x) == '_' || s
          .charAt(x) == '$'))
        return null;
      int i = 0;
      while (true) {
        i++;
        if (x1 >= 0 && x1 < s.length()) {
          if (Character.isLetter(s.charAt(x1)) || s.charAt(x1) == '_') {
            word = s.charAt(x1--) + word;
            xLS--;
          } else
            x1 = -1;
        } else
          x1 = -1;

        if (x2 >= 0 && x2 < s.length()) {
          if (Character.isLetterOrDigit(s.charAt(x2)) || s.charAt(x2) == '_'
              || s.charAt(x2) == '$')
            word = word + s.charAt(x2++);
          else
            x2 = -1;
        } else
          x2 = -1;

        if (x1 < 0 && x2 < 0)
          break;
        if (i > 200) {
          // time out!
          // System.err.println("Whoopsy! :P");
          break;
        }
      }
      if (Character.isDigit(word.charAt(0)))
        return null;
      String tooltipText = errorCheckerService.getASTGenerator()
          .getLabelForASTNode(line, word, xLS);

//      log(errorCheckerService.mainClassOffset + " MCO "
//      + "|" + line + "| offset " + xLS + word + " <= offf: "+off+ "\n");
      if (tooltipText != null)
        return tooltipText;
      return word;
    }

  }

  // TweakMode code
	protected int horizontalAdjustment = 0;

	public boolean interactiveMode = false;
	public ArrayList<Handle> handles[];
	public ArrayList<ColorControlBox> colorBoxes[];

	public Handle mouseHandle = null;
	public ColorSelector colorSelector;

	int cursorType;
	BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

	// Create a new blank cursor.
	Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
	    cursorImg, new Point(0, 0), "blank cursor");


	/**
	* Repaints the text.
	* @param gfx The graphics context
	*/
	@Override
	public synchronized void paint(Graphics gfx)
	{
		super.paint(gfx);

		if (interactiveMode && handles!=null)
		{
			int currentTab = ta.editor.getSketch().getCurrentCodeIndex();
			// enable anti-aliasing
			Graphics2D g2d = (Graphics2D)gfx;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                				RenderingHints.VALUE_ANTIALIAS_ON);

			for (Handle n : handles[currentTab])
			{
				// update n position and width, and draw it
				int lineStartChar = ta.getLineStartOffset(n.line);
				int x = ta.offsetToX(n.line, n.newStartChar - lineStartChar);
				int y = ta.lineToY(n.line) + fm.getHeight() + 1;
				int end = ta.offsetToX(n.line, n.newEndChar - lineStartChar);
				n.setPos(x, y);
				n.setWidth(end - x);
				n.draw(g2d, n==mouseHandle);
			}

			// draw color boxes
			for (ColorControlBox cBox: colorBoxes[currentTab])
			{
				int lineStartChar = ta.getLineStartOffset(cBox.getLine());
				int x = ta.offsetToX(cBox.getLine(), cBox.getCharIndex() - lineStartChar);
				int y = ta.lineToY(cBox.getLine()) + fm.getDescent();
				cBox.setPos(x, y+1);
				cBox.draw(g2d);
			}
		}
	}

	public void startInterativeMode()
	{
		interactiveMode = true;
		repaint();
	}

	public void stopInteractiveMode()
	{
		interactiveMode = false;

		if (colorSelector != null) {
			// close color selector
			colorSelector.hide();
			colorSelector.frame.dispatchEvent(new WindowEvent(colorSelector.frame, WindowEvent.WINDOW_CLOSING));
		}

		repaint();
	}

	// Update the interface
	public void updateInterface(ArrayList<Handle> handles[], ArrayList<ColorControlBox> colorBoxes[])
	{
		this.handles = handles;
		this.colorBoxes = colorBoxes;

		initInterfacePositions();
		repaint();
	}

	/**
	* Initialize all the number changing interfaces.
	* synchronize this method to prevent the execution of 'paint' in the middle.
	* (don't paint while we make changes to the text of the editor)
	*/
	public synchronized void initInterfacePositions()
	{
		SketchCode[] code = ta.editor.getSketch().getCode();
		int prevScroll = ta.getVerticalScrollPosition();
		String prevText = ta.getText();

		for (int tab=0; tab<code.length; tab++)
		{
			String tabCode = ta.editor.baseCode[tab];
			ta.setText(tabCode);
			for (Handle n : handles[tab])
			{
				int lineStartChar = ta.getLineStartOffset(n.line);
				int x = ta.offsetToX(n.line, n.newStartChar - lineStartChar);
				int end = ta.offsetToX(n.line, n.newEndChar - lineStartChar);
				int y = ta.lineToY(n.line) + fm.getHeight() + 1;
				n.initInterface(x, y, end-x, fm.getHeight());
			}

			for (ColorControlBox cBox : colorBoxes[tab])
			{
				int lineStartChar = ta.getLineStartOffset(cBox.getLine());
				int x = ta.offsetToX(cBox.getLine(), cBox.getCharIndex() - lineStartChar);
				int y = ta.lineToY(cBox.getLine()) + fm.getDescent();
				cBox.initInterface(this, x, y+1, fm.getHeight()-2, fm.getHeight()-2);
			}
		}

		ta.setText(prevText);
		ta.scrollTo(prevScroll, 0);
	}

	/**
	 * Take the saved code of the current tab and replace
	 * all numbers with their current values.
	 * Update TextArea with the new code.
	 */
	public void updateCodeText()
	{
		int charInc = 0;
		int currentTab = ta.editor.getSketch().getCurrentCodeIndex();
		SketchCode sc = ta.editor.getSketch().getCode(currentTab);
		String code = ta.editor.baseCode[currentTab];

		for (Handle n : handles[currentTab])
		{
			int s = n.startChar + charInc;
			int e = n.endChar + charInc;
			code = replaceString(code, s, e, n.strNewValue);
			n.newStartChar = n.startChar + charInc;
			charInc += n.strNewValue.length() - n.strValue.length();
			n.newEndChar = n.endChar + charInc;
		}

		replaceTextAreaCode(code);
		// update also the sketch code for later
		sc.setProgram(code);
	}

	private synchronized void replaceTextAreaCode(String code)
	{
			// don't paint while we do the stuff below
			/* by default setText will scroll all the way to the end
			 * remember current scroll position */
			int scrollLine = ta.getVerticalScrollPosition();
			int scrollHor = ta.getHorizontalScroll();
			ta.setText(code);
			ta.setOrigin(scrollLine, -scrollHor);
	}

	public String replaceString(String str, int start, int end, String put)
	{
		return str.substring(0, start) + put + str.substring(end, str.length());
	}

	public void updateCursor(int mouseX, int mouseY)
	{
		int currentTab = ta.editor.getSketch().getCurrentCodeIndex();
		for (Handle n : handles[currentTab])
		{
			if (n.pick(mouseX, mouseY))
			{
				cursorType = Cursor.W_RESIZE_CURSOR;
				setCursor(new Cursor(cursorType));
				return;
			}
		}

		for (ColorControlBox colorBox : colorBoxes[currentTab])
		{
			if (colorBox.pick(mouseX, mouseY))
			{
				cursorType = Cursor.HAND_CURSOR;
				setCursor(new Cursor(cursorType));
				return;
			}
		}

		if (cursorType == Cursor.W_RESIZE_CURSOR ||
			cursorType == Cursor.HAND_CURSOR ||
			cursorType == -1) {
			cursorType = Cursor.DEFAULT_CURSOR;
			setCursor(new Cursor(cursorType));
		}
	}

	/* Handle color boxes show/hide
	 *
	 * display the box if the mouse if in the same line.
	 * always keep the color box of the color selector.
	 */
	private void showHideColorBoxes(int y)
	{
		int currentTab = ta.editor.getSketch().getCurrentCodeIndex();

		boolean change = false;
		for (ColorControlBox box : colorBoxes[currentTab]) {
			if (box.setMouseY(y)) {
				change = true;
			}
		}

		if (colorSelector != null) {
			colorSelector.colorBox.visible = true;
		}

		if (change) {
			repaint();
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (mouseHandle != null) {
			// set the current drag amount of the arrows
			mouseHandle.setCurrentX(e.getX());

			// update code text with the new value
			updateCodeText();

			if (colorSelector != null) {
				colorSelector.refreshColor();
			}

			repaint();
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		int currentTab = ta.editor.getSketch().getCurrentCodeIndex();
		// check for clicks on number handles
		for (Handle n : handles[currentTab])
		{
			if (n.pick(e.getX(), e.getY()))
			{
				cursorType = -1;
				this.setCursor(blankCursor);
				mouseHandle = n;
				mouseHandle.setCenterX(e.getX());
				repaint();
				return;
			}
		}

		// check for clicks on color boxes
		for (ColorControlBox box : colorBoxes[currentTab])
		{
			if (box.pick(e.getX(), e.getY()))
			{
				if (colorSelector != null) {
					// we already show a color selector, close it
					colorSelector.frame.dispatchEvent(
							new WindowEvent(colorSelector.frame, WindowEvent.WINDOW_CLOSING));
				}

				colorSelector = new ColorSelector(box);
				colorSelector.frame.addWindowListener(new WindowAdapter() {
				        public void windowClosing(WindowEvent e) {
				        	colorSelector.frame.setVisible(false);
				        	colorSelector = null;
				        }
				      });
				colorSelector.show(this.getLocationOnScreen().x + e.getX() + 30,
						this.getLocationOnScreen().y + e.getY() - 130);
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (mouseHandle != null) {
			mouseHandle.resetProgress();
			mouseHandle = null;

			updateCursor(e.getX(), e.getY());
			repaint();
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		updateCursor(e.getX(), e.getY());

		if (!Settings.alwaysShowColorBoxes) {
			showHideColorBoxes(e.getY());
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

}
