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

package processing.mode.java.tweak;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JFrame;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

public class ColorSelector {
	int hue, saturation, brightness;

	public JFrame frame;
	public ColorControlBox colorBox;
	ColorSelectorBox selectorBox;
	ColorSelectorSlider selectorSlider;
	SelectorTopBar topBar;


	public ColorSelector(ColorControlBox colorBox)
	{
		this.colorBox = colorBox;
		createFrame();
	}

	public void createFrame()
	{
		frame = new JFrame();
		frame.setBackground(Color.BLACK);

		Box box = Box.createHorizontalBox();
		box.setBackground(Color.BLACK);

		selectorSlider = new ColorSelectorSlider();

		if (!colorBox.isBW) {
			selectorBox = new ColorSelectorBox();
			box.add(selectorBox.getComponent());
		}

		box.add(Box.createHorizontalGlue());
		box.add(selectorSlider.getComponent(), BorderLayout.CENTER);
		box.add(Box.createHorizontalGlue());

		frame.getContentPane().add(box, BorderLayout.CENTER);
		frame.pack();
		frame.setResizable(false);
		frame.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

    selectorBox.init();
    selectorSlider.init();
	}

	public void show(int x, int y)
	{
		frame.setLocation(x, y);
		frame.setVisible(true);
		frame.repaint();
	}

	public void hide()
	{
		this.colorBox = null;
		frame.setVisible(false);
	}

	public void refreshColor()
	{
		if (colorBox.ilegalColor) {
			return;
		}

		setColor(colorBox.color);
	}

	public void setColor(Color c)
	{
		if (selectorBox != null) {
			selectorBox.setToColor(c);
		}
		selectorSlider.setToColor(c);

		repaintSelector();
	}

	public void satBrightChanged()
	{
		repaintSelector();
	}

	public void hueChanged()
	{
		if (selectorBox != null) {
			selectorBox.renderBack();
		}
		repaintSelector();
	}

	public void repaintSelector()
	{
		if (selectorBox != null) {
			selectorBox.redraw();
		}
		selectorSlider.redraw();
	}

	/*
	 * PApplets for the interactive color fields
	 */

	public class ColorSelectorBox extends PApplet {
		int lastX, lastY;
		PImage backImg;

		public int sketchWidth() { return 255; }

		public int sketchHeight() { return 255; }

		public void setup() {
			noLoop();
			colorMode(HSB, 255, 255, 255);
			noFill();

			if (!colorBox.ilegalColor) {
				setToColor(colorBox.color);
			}

			renderBack();
		}

		public void draw() {
			image(backImg, 0, 0);

			stroke((lastY<128) ? 0 : 255);

			pushMatrix();
			translate(lastX, lastY);
			ellipse(0, 0, 5, 5);
			line(-8, 0, -6, 0);
			line(6, 0, 8, 0);
			line(0, -8, 0, -6);
			line(0, 6, 0, 8);
			popMatrix();
		}

		public void renderBack() {
			PGraphics buf = createGraphics(255, 255);
			buf.colorMode(HSB, 255, 255, 255);
			buf.beginDraw();
			buf.loadPixels();
			int index=0;
			for (int j=0; j<255; j++) {
				for (int i=0; i<255; i++) {
					buf.pixels[index++] = color(hue, i, 255-j);
				}
			}
			buf.updatePixels();
			buf.endDraw();

			backImg = buf.get();
		}

		public void setToColor(Color c) {
			// set selector color
			float hsb[] = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
			saturation = (int)(hsb[1]*255);
			brightness = (int)(hsb[2]*255);
			lastX = saturation;
			lastY = 255 - brightness;
		}

		public void mousePressed() {
			if (mouseX < 0 || mouseX > 255 ||
				mouseY < 0 || mouseY > 255) {
				return;
			}

			lastX = mouseX;
			lastY = mouseY;
			updateColor();
		}

		public void mouseDragged() {
			if (mouseX < 0 || mouseX > 255 ||
					mouseY < 0 || mouseY > 255) {
					return;
			}

			lastX = mouseX;
			lastY = mouseY;
			updateColor();
		}

		public void updateColor() {
			saturation = lastX;
			brightness = 255 - lastY;

			satBrightChanged();
			colorBox.selectorChanged(hue, saturation, brightness);
		}

		/*
		public Dimension getPreferredSize() {
			return new Dimension(255, 255);
		}

		public Dimension getMinimumSize() {
			return new Dimension(255, 255);
		}

		public Dimension getMaximumSize() {
			return new Dimension(255, 255);
		}
		*/
	}


	public class ColorSelectorSlider extends PApplet {
		PImage backImg;
		int lastY;

		public void setup() {
			size(30, 255);
			noLoop();
			colorMode(HSB, 255, 255, 255);
			strokeWeight(1);
			noFill();
			loadPixels();
			if (!colorBox.ilegalColor) {
				setToColor(colorBox.color);
			}

			// draw the slider background
			renderBack();
		}

		public void draw() {
			image(backImg, 0, 0);
			if (colorBox.isBW) {
				stroke(lastY<128 ? 0 : 255);
			}
			else {
				stroke(0);
			}

			pushMatrix();
			translate(0, lastY);
			// draw left bracket
			beginShape();
			vertex(5, -2);
			vertex(1, -2);
			vertex(1, 2);
			vertex(5, 2);
			endShape();

			// draw middle lines
			line(13, 0, 17, 0);
			line(15, -2, 15, 2);

			// draw right bracket
			beginShape();
			vertex(24, -2);
			vertex(28, -2);
			vertex(28, 2);
			vertex(24, 2);
			endShape();
			popMatrix();

			if (colorBox.isBW) {
				stroke(255);
				rect(0, 0, 29, 254);
			}
			else {
				stroke(0);
				line(0, 0, 0, 255);
				line(29, 0, 29, 255);
			}
		}

		public void renderBack() {
			PGraphics buf = createGraphics(30, 255);
			buf.beginDraw();
			buf.loadPixels();
			int index=0;
			for (int j=0; j<255; j++) {
				for (int i=0; i<30; i++) {
					if (colorBox.isBW) {
						buf.pixels[index++] = color(255-j);
					}
					else {
						buf.pixels[index++] = color(255-j, 255, 255);
					}
				}
			}
			buf.updatePixels();
			buf.endDraw();

			backImg = buf.get();
		}

		public void setToColor(Color c)
		{
			// set slider position
			if (colorBox.isBW) {
				hue = c.getRed();
			}
			else {
				float hsb[] = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
				hue = (int)(hsb[0]*255);
			}
			lastY = 255 - hue;
		}

		public void mousePressed()
		{
			if (mouseX < 0 || mouseX > 30 ||
					mouseY < 0 || mouseY > 255) {
				return;
			}

			lastY = mouseY;
			updateColor();
		}

		public void mouseDragged()
		{
			if (mouseX < 0 || mouseX > 30 ||
					mouseY < 0 || mouseY > 255) {
				return;
			}

			lastY = mouseY;
			updateColor();
		}

		public void updateColor()
		{
			hue = 255 - lastY;

			hueChanged();
			colorBox.selectorChanged(hue, saturation, brightness);
		}

		public Dimension getPreferredSize() {
			return new Dimension(30, 255);
		}

		public Dimension getMinimumSize() {
			return new Dimension(30, 255);
		}

		public Dimension getMaximumSize() {
			return new Dimension(30, 255);
		}
	}

	public class SelectorTopBar extends PApplet
	{
		int barWidth;
		int barHeight;

		public SelectorTopBar(int w)
		{
			super();
			barWidth = w;
			barHeight = 16;
		}

		public void setup()
		{
			size(barWidth, barHeight);
			noLoop();
		}

		public void draw()
		{
			background(128);
		}

		public Dimension getPreferredSize() {
			return new Dimension(barWidth, barHeight);
		}

		public Dimension getMinimumSize() {
			return new Dimension(barWidth, barHeight);
		}

		public Dimension getMaximumSize() {
			return new Dimension(barWidth, barHeight);
		}
	}
}

