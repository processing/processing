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
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.pdex;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

import processing.mode.java.JavaEditor;


/**
 * Toggle Button displayed in the editor line status panel for toggling between
 * console and problems list. Glorified JPanel.
 *
 * @author Manindra Moharana &lt;me@mkmoharana.com&gt;
 *
 */
public class XQConsoleToggle extends JPanel implements MouseListener {
	private boolean toggleText = true;
	private boolean toggleBG = true;

	/**
	 * Height of the component
	 */
	protected int height;
	protected JavaEditor editor;
	protected String buttonName;

	public XQConsoleToggle(JavaEditor editor, String buttonName, int height) {
		this.editor = editor;
		this.height = height;
		this.buttonName = buttonName;
	}

	public Dimension getPreferredSize() {
		return new Dimension(70, height);
	}

	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
		                     RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// On mouse hover, text and background color are changed.
		if (toggleBG) {
			g.setColor(new Color(0xff9DA7B0));
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			g.setColor(new Color(0xff29333D));
			g.fillRect(0, 0, 4, this.getHeight());
			g.setColor(Color.BLACK);
		} else {
			g.setColor(Color.DARK_GRAY);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			g.setColor(new Color(0xff29333D));
			g.fillRect(0, 0, 4, this.getHeight());
			g.setColor(Color.WHITE);
		}

		g.drawString(buttonName, getWidth() / 2 + 2 // + 2 is a offset
				- getFontMetrics(getFont()).stringWidth(buttonName) / 2,
				this.getHeight() - 6);
		if (drawMarker) {
			g.setColor(markerColor);
			g.fillRect(4, 0, 2, this.getHeight());
		}
	}

	boolean drawMarker = false;
	protected Color markerColor;
	public void updateMarker(boolean value, Color color){
	  drawMarker = value;
	  markerColor = color;
	  repaint();
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {

		this.repaint();
		try {
			editor.showProblemListView(buttonName);
		} catch (Exception e) {
			System.out.println(e);
			// e.printStackTrace();
		}
		toggleText = !toggleText;
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		toggleBG = !toggleBG;
		this.repaint();
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		toggleBG = !toggleBG;
		this.repaint();
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}
}