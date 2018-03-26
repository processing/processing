/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
	Part of the Processing project - http://processing.org

	Copyright (c) 2012-16 The Processing Foundation
	Copyright (c) 2004-12 Ben Fry and Casey Reas
	Copyright (c) 2001-04 Massachusetts Institute of Technology

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

	package processing.app.ui;

	import java.awt.Color;
	import java.awt.Cursor;
	import java.awt.Dimension;
	import java.awt.Font;
	import java.awt.FontMetrics;
	import java.awt.Graphics;
	import java.awt.Image;
	import java.awt.event.*;
	import java.awt.event.MouseAdapter;
	import java.awt.event.MouseEvent;
	import java.awt.event.MouseMotionAdapter;
	import java.awt.*;
	import java.awt.datatransfer.Clipboard;
	import java.awt.datatransfer.StringSelection;

	import javax.swing.plaf.basic.BasicSplitPaneDivider;
	import javax.swing.plaf.basic.BasicSplitPaneUI;
	import javax.swing.*;

	import processing.app.Mode;
	import processing.app.Platform;
	import processing.core.PApplet;


/**
 * Panel just below the editing area that contains status messages.
 */
public class EditorStatus extends BasicSplitPaneDivider{  //JPanel {
	static final int HIGH = Toolkit.zoom(28);
	static final int LEFT_MARGIN = Editor.LEFT_GUTTER;
	static final int RIGHT_MARGIN = Toolkit.zoom(20);

	Color urlColor;
	Color[] fgColor;
	Color[] bgColor;
	Image[] bgImage;

	@SuppressWarnings("hiding")
	static public final int ERROR   = 1;
	static public final int CURSOR_LINE_ERROR = 2;
	static public final int WARNING = 3;
	static public final int CURSOR_LINE_WARNING = 4;
	static public final int NOTICE  = 0;

	static final int YES    = 1;
	static final int NO     = 2;
	static final int CANCEL = 3;
	static final int OK     = 4;

	static final String NO_MESSAGE = "";

	Editor editor;

	int mode;
	String message;

	String url;
	int rightEdge;
	int mouseX;
	boolean urlRollover;

	Font font;
	FontMetrics metrics;
	int ascent;

	Image offscreen;
	int sizeW, sizeH;

	int response;

	boolean indeterminate;
	Thread thread;


	public EditorStatus(BasicSplitPaneUI ui, Editor editor) {
		super(ui);
		this.editor = editor;
		empty();
		updateMode();

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				updateMouse();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (urlRollover) {
					Platform.openURL(url);
				}
				//  Function call to copy message when clicked on the error
				copyMessage();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				updateMouse();
			}

		});

		addMouseMotionListener(new MouseMotionAdapter() {

			@Override
			public void mouseMoved(MouseEvent e) {
				mouseX = e.getX();
				updateMouse();
			}
		});
	}


	void updateMouse() {
		if (urlRollover) {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		} else {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}
		repaint();
	}


	static String findURL(String message) {
		String[] m = PApplet.match(message, "http\\S+");
		if (m != null) {
			return m[0];
		}
		return null;
	}


	public void updateMode() {
		Mode mode = editor.getMode();

		urlColor = mode.getColor("status.url.fgcolor");

		fgColor = new Color[] {
			mode.getColor("status.notice.fgcolor"),
			mode.getColor("status.error.fgcolor"),
			mode.getColor("status.error.fgcolor"),
			mode.getColor("status.warning.fgcolor"),
			mode.getColor("status.warning.fgcolor")
		};

		bgColor = new Color[] {
			mode.getColor("status.notice.bgcolor"),
			mode.getColor("status.error.bgcolor"),
			mode.getColor("status.error.bgcolor"),
			mode.getColor("status.warning.bgcolor"),
			mode.getColor("status.warning.bgcolor")
		};

		bgImage = new Image[] {
			mode.loadImage("/lib/status/notice.png"),
			mode.loadImage("/lib/status/error.png"),
			mode.loadImage("/lib/status/error.png"),
			mode.loadImage("/lib/status/warning.png"),
			mode.loadImage("/lib/status/warning.png")
		};

		font = mode.getFont("status.font");
		metrics = null;
	}


	public void empty() {
		mode = NOTICE;
		message = NO_MESSAGE;
		url = null;
		repaint();
	}


	public void message(String message, int mode) {
		this.message = message;
		this.mode = mode;

		url = findURL(message);
		repaint();
	}


	public void notice(String message) {
		message(message, NOTICE);
//    mode = NOTICE;
//    this.message = message;
//    url = findURL(message);
//    repaint();
	}


//  public void unnotice(String unmessage) {
//    if (message.equals(unmessage)) empty();
//  }

  //  Function to copy the message
	public void copyMessage(){
		java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
		Clipboard clipboard = toolkit.getSystemClipboard();
		StringSelection strSel = new StringSelection(message);
		clipboard.setContents(strSel, null);
	}

	public void warning(String message) {
		message(message, WARNING);
//    this.message = message;
//    mode = WARNING;
//    url = findURL(message);
//    repaint();
	}


	public void error(String message) {
		message(message, ERROR);
//    this.message = message;
//    mode = ERROR;
//    url = findURL(message);
//    repaint();
	}


	public void startIndeterminate() {
		indeterminate = true;
		thread = new Thread() {
			public void run() {
				while (Thread.currentThread() == thread) {
					repaint();
					try {
						Thread.sleep(1000 / 10);
					} catch (InterruptedException e) { }
				}
			}
		};
		thread.setName("Editor Status");
		thread.start();
	}


	public void stopIndeterminate() {
		indeterminate = false;
		thread = null;
		repaint();
	}


	//public void paintComponent(Graphics screen) {
	public void paint(Graphics screen) {
//    if (okButton == null) setup();
		Dimension size = getSize();
		if ((size.width != sizeW) || (size.height != sizeH)) {
			// component has been resized
			offscreen = null;
		}

		if (offscreen == null) {
			sizeW = size.width;
			sizeH = size.height;

			offscreen = Toolkit.offscreenGraphics(this, sizeW, sizeH);
		}

		Graphics g = offscreen.getGraphics();
		/*Graphics2D g2 =*/ Toolkit.prepareGraphics(g);

		g.setFont(font);
		if (metrics == null) {
			metrics = g.getFontMetrics();
			ascent = metrics.getAscent();
		}

		g.drawImage(bgImage[mode], 0, 0, sizeW, sizeH, this);

		g.setColor(fgColor[mode]);
		// https://github.com/processing/processing/issues/3265
		if (message != null) {
			// needs to be set each time on osx
			g.setFont(font);
			// calculate right edge of the text for rollovers (otherwise the pane
			// cannot be resized up or down whenever a URL is being displayed)
			rightEdge = LEFT_MARGIN + g.getFontMetrics().stringWidth(message);
			// set the highlight color on rollover so that the user's not surprised
			// to see the web browser open when they click
			urlRollover = (url != null) &&
			(mouseX > LEFT_MARGIN && mouseX < rightEdge);
			if (urlRollover) {
				g.setColor(urlColor);
			}
			g.drawString(message, LEFT_MARGIN, (sizeH + ascent) / 2);
		}

		if (indeterminate) {
			//int x = cancelButton.getX();
			//int w = cancelButton.getWidth();
			int w = Toolkit.getButtonWidth();
			int x = getWidth() - RIGHT_MARGIN - w;
			int y = getHeight() / 3;
			int h = getHeight() / 3;
			g.setColor(new Color(0x80000000, true));
			g.drawRect(x, y, w, h);
			for (int i = 0; i < 10; i++) {
				int r = (int) (x + Math.random() * w);
				g.drawLine(r, y, r, y+h);

			}
		}

		screen.drawImage(offscreen, 0, 0, sizeW, sizeH, null);

	}


	public Dimension getPreferredSize() {
		return getMinimumSize();
	}


	public Dimension getMinimumSize() {
		return new Dimension(Toolkit.zoom(300), HIGH);
	}


	public Dimension getMaximumSize() {
		return new Dimension(super.getMaximumSize().width, HIGH);
	}
}
