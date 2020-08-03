/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-16 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.pdex;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import processing.app.SketchCode;
import processing.app.syntax.PdeTextAreaPainter;
import processing.app.syntax.TextAreaDefaults;
import processing.mode.java.JavaEditor;
import processing.mode.java.tweak.ColorControlBox;
import processing.mode.java.tweak.ColorSelector;
import processing.mode.java.tweak.Handle;
import processing.mode.java.tweak.Settings;


/**
 * Customized line painter to handle tweak mode features.
 */
public class JavaTextAreaPainter extends PdeTextAreaPainter {

  public JavaTextAreaPainter(final JavaTextArea textArea, TextAreaDefaults defaults) {
    super(textArea, defaults);

    // TweakMode code
    tweakMode = false;
    cursorType = Cursor.DEFAULT_CURSOR;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  // TWEAK MODE


	protected int horizontalAdjustment = 0;

	public boolean tweakMode = false;
	public List<List<Handle>> handles;
	public List<List<ColorControlBox>> colorBoxes;

	public Handle mouseHandle = null;
	public ColorSelector colorSelector;

	int cursorType;
	BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
  Cursor blankCursor;
  // this is a temporary workaround for the CHIP, will be removed
  {
    Dimension cursorSize = Toolkit.getDefaultToolkit().getBestCursorSize(16, 16);
    if (cursorSize.width == 0 || cursorSize.height == 0) {
      blankCursor = Cursor.getDefaultCursor();
    } else {
      blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
    }
  }

	@Override
	synchronized public void paint(Graphics gfx) {
		super.paint(gfx);

		if (tweakMode && handles != null) {
			int currentTab = getCurrentCodeIndex();
			// enable anti-aliasing
			Graphics2D g2d = (Graphics2D)gfx;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                				   RenderingHints.VALUE_ANTIALIAS_ON);

			for (Handle n : handles.get(currentTab)) {
				// update n position and width, and draw it
				int lineStartChar = textArea.getLineStartOffset(n.line);
				int x = textArea.offsetToX(n.line, n.newStartChar - lineStartChar);
				int y = textArea.lineToY(n.line) + fm.getHeight() + 1;
				int end = textArea.offsetToX(n.line, n.newEndChar - lineStartChar);
				n.setPos(x, y);
				n.setWidth(end - x);
				n.draw(g2d, n==mouseHandle);
			}

			// draw color boxes
			for (ColorControlBox cBox: colorBoxes.get(currentTab)) {
				int lineStartChar = textArea.getLineStartOffset(cBox.getLine());
				int x = textArea.offsetToX(cBox.getLine(), cBox.getCharIndex() - lineStartChar);
				int y = textArea.lineToY(cBox.getLine()) + fm.getDescent();
				cBox.setPos(x, y+1);
				cBox.draw(g2d);
			}
		}
	}


	protected void startTweakMode() {
	  addMouseListener(new MouseListener() {

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
	    public void mousePressed(MouseEvent e) {
	      int currentTab = getCurrentCodeIndex();
	      // check for clicks on number handles
	      for (Handle n : handles.get(currentTab)) {
	        if (n.pick(e.getX(), e.getY())) {
	          cursorType = -1;
	          JavaTextAreaPainter.this.setCursor(blankCursor);
	          mouseHandle = n;
	          mouseHandle.setCenterX(e.getX());
	          repaint();
	          return;
	        }
	      }

	      // check for clicks on color boxes
	      for (ColorControlBox box : colorBoxes.get(currentTab)) {
	        if (box.pick(e.getX(), e.getY())) {
	          if (colorSelector != null) {
	            // we already show a color selector, close it
	            colorSelector.frame.dispatchEvent(new WindowEvent(colorSelector.frame, WindowEvent.WINDOW_CLOSING));
	          }

	          colorSelector = new ColorSelector(box);
	          colorSelector.frame.addWindowListener(new WindowAdapter() {
	            public void windowClosing(WindowEvent e) {
	              colorSelector.frame.setVisible(false);
	              colorSelector = null;
	            }
	          });
	          colorSelector.show(getLocationOnScreen().x + e.getX() + 30,
	                             getLocationOnScreen().y + e.getY() - 130);
	        }
	      }
	    }

      @Override
      public void mouseExited(MouseEvent e) { }

      @Override
      public void mouseEntered(MouseEvent e) { }

      @Override
      public void mouseClicked(MouseEvent e) { }
    });

	  addMouseMotionListener(new MouseMotionListener() {

	    @Override
	    public void mouseMoved(MouseEvent e) {
	      updateCursor(e.getX(), e.getY());

	      if (!Settings.alwaysShowColorBoxes) {
	        showHideColorBoxes(e.getY());
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
    });
	  tweakMode = true;
	  setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		repaint();
	}


	protected void stopTweakMode() {
		tweakMode = false;

		if (colorSelector != null) {
			colorSelector.hide();
			WindowEvent windowEvent =
			  new WindowEvent(colorSelector.frame, WindowEvent.WINDOW_CLOSING);
			colorSelector.frame.dispatchEvent(windowEvent);
		}

		setCursor(new Cursor(Cursor.TEXT_CURSOR));
		repaint();
	}


	protected void updateTweakInterface(List<List<Handle>> handles,
	                                    List<List<ColorControlBox>> colorBoxes) {
		this.handles = handles;
		this.colorBoxes = colorBoxes;

		updateTweakInterfacePositions();
		repaint();
	}


	/**
	* Initialize all the number changing interfaces.
	* synchronize this method to prevent the execution of 'paint' in the middle.
	* (don't paint while we make changes to the text of the editor)
	*/
	private synchronized void updateTweakInterfacePositions() {
		SketchCode[] code = getEditor().getSketch().getCode();
		int prevScroll = textArea.getVerticalScrollPosition();
		String prevText = textArea.getText();

		for (int tab=0; tab<code.length; tab++) {
			String tabCode = getJavaEditor().baseCode[tab];
			textArea.setText(tabCode);
			for (Handle n : handles.get(tab)) {
				int lineStartChar = textArea.getLineStartOffset(n.line);
				int x = textArea.offsetToX(n.line, n.newStartChar - lineStartChar);
				int end = textArea.offsetToX(n.line, n.newEndChar - lineStartChar);
				int y = textArea.lineToY(n.line) + fm.getHeight() + 1;
				n.initInterface(x, y, end-x, fm.getHeight());
			}

			for (ColorControlBox cBox : colorBoxes.get(tab)) {
				int lineStartChar = textArea.getLineStartOffset(cBox.getLine());
				int x = textArea.offsetToX(cBox.getLine(), cBox.getCharIndex() - lineStartChar);
				int y = textArea.lineToY(cBox.getLine()) + fm.getDescent();
				cBox.initInterface(this, x, y+1, fm.getHeight()-2, fm.getHeight()-2);
			}
		}

		textArea.setText(prevText);
		textArea.scrollTo(prevScroll, 0);
	}


	/**
	 * Take the saved code of the current tab and replace
	 * all numbers with their current values.
	 * Update TextArea with the new code.
	 */
	public void updateCodeText() {
		int charInc = 0;
		int currentTab = getCurrentCodeIndex();
		SketchCode sc = getEditor().getSketch().getCode(currentTab);
		String code = getJavaEditor().baseCode[currentTab];

		for (Handle n : handles.get(currentTab)) {
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


	// don't paint while we do the stuff below
	private synchronized void replaceTextAreaCode(String code) {
	  // by default setText will scroll all the way to the end
	  // remember current scroll position
	  int scrollLine = textArea.getVerticalScrollPosition();
	  int scrollHor = textArea.getHorizontalScrollPosition();
	  textArea.setText(code);
	  textArea.setOrigin(scrollLine, -scrollHor);
	}


	static private String replaceString(String str, int start, int end, String put) {
		return str.substring(0, start) + put + str.substring(end, str.length());
	}


	private void updateCursor(int mouseX, int mouseY) {
		int currentTab = getCurrentCodeIndex();
		for (Handle n : handles.get(currentTab)) {
			if (n.pick(mouseX, mouseY)) {
				cursorType = Cursor.W_RESIZE_CURSOR;
				setCursor(new Cursor(cursorType));
				return;
			}
		}

		for (ColorControlBox colorBox : colorBoxes.get(currentTab)) {
			if (colorBox.pick(mouseX, mouseY)) {
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


	private void showHideColorBoxes(int y) {
	  // display the box if the mouse if in the same line.
	  // always keep the color box of the color selector.
		int currentTab = getCurrentCodeIndex();

		boolean change = false;
		for (ColorControlBox box : colorBoxes.get(currentTab)) {
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


	// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


	private JavaEditor getJavaEditor() {
	  return (JavaEditor) getEditor();
	}


	private int getCurrentCodeIndex() {
	  return getEditor().getSketch().getCurrentCodeIndex();
	}
}
