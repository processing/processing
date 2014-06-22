package galsasson.mode.tweak;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Locale;

public class Handle {
	public String type;
	public String name;
	public String strValue;
	public String strNewValue;
	public int varIndex;
	int tabIndex;
	int startChar, endChar, line;
	int newStartChar, newEndChar;
	int decimalPlaces;		// number of digits after the decimal point
	float incValue;
	
	java.lang.Number value, newValue;
	String strDiff;
	
	// connect with color control box
	ColorControlBox colorBox;
	
	// interface
	int x, y, width, height;
	int xCenter, xCurrent, xLast;
	HProgressBar progBar = null;
	String textFormat;

	int oscPort;
	
	public Handle(String t, String n, int vi, String v, int ti, int l, int sc, int ec, int dp)
	{
		type = t;
		name = n;
		varIndex = vi;
		strValue = v;
		tabIndex = ti;
		line = l;
		startChar = sc;
		endChar = ec;
		decimalPlaces = dp;
		
		incValue = (float)(1/Math.pow(10, decimalPlaces));

		if (type == "int") {
			value = newValue = Integer.parseInt(strValue);
			strNewValue = strValue;
			textFormat = "%d";
		}
		else if (type == "hex") {
			Long val = Long.parseLong(strValue.substring(2, strValue.length()), 16);
			value = newValue = val.intValue();
			strNewValue = strValue;
			textFormat = "0x%x";
		}
		else if (type == "webcolor") {
			Long val = Long.parseLong(strValue.substring(1, strValue.length()), 16);
			val = val | 0xff000000;
			value = newValue = val.intValue();
			strNewValue = strValue;
			textFormat = "#%06x";
		}
		else if (type == "float") {
			value = newValue = Float.parseFloat(strValue);
			strNewValue = strValue;
			textFormat = "%.0" + decimalPlaces + "f";
		}
		
		newStartChar = startChar;
		newEndChar = endChar;
	}
	
	public void initInterface(int x, int y, int width, int height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		
		// create drag ball
		progBar = new HProgressBar(height, width);
	}
	
	public void setCenterX(int mx)
	{
		xLast = xCurrent = xCenter = mx;
	}
	
	public void setCurrentX(int mx)
	{
		xLast = xCurrent;
		xCurrent = mx;
		
		progBar.setPos(xCurrent - xCenter);
		
		updateValue();
	}
	
	public void resetProgress()
	{
		progBar.setPos(0);
	}
	
	public void updateValue()
	{
		float change = getChange();
		
		if (type == "int") {
			if (newValue.intValue() + (int)change > Integer.MAX_VALUE ||
					newValue.intValue() + (int)change < Integer.MIN_VALUE) {
				change = 0;
				return;
			}
			setValue(newValue.intValue() + (int)change);
		}
		else if (type == "hex") {
			setValue(newValue.intValue() + (int)change);
		}
		else if (type == "webcolor") {
			setValue(newValue.intValue() + (int)change);
		}
		else if (type == "float") {
			setValue(newValue.floatValue() + change);
		}

		updateColorBox();
	}
	
	public void setValue(Number value)
	{
		if (type == "int") {
			newValue = value.intValue();
			strNewValue = String.format(Locale.US,textFormat, newValue.intValue());
		}
		else if (type == "hex") {
			newValue = value.intValue();
			strNewValue = String.format(Locale.US,textFormat, newValue.intValue());
		}
		else if (type == "webcolor") {
			newValue = value.intValue();
			// keep only RGB
			int val = (newValue.intValue() & 0xffffff);
			strNewValue = String.format(Locale.US,textFormat, val);
		}
		else if (type == "float") {
			BigDecimal bd = new BigDecimal(value.floatValue());
			bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP);
			newValue = bd.floatValue();
			strNewValue = String.format(Locale.US,textFormat, newValue.floatValue());			
		}
		
		// send new data to the server in the sketch
		oscSendNewValue();
	}
	
	public void updateColorBox()
	{
		if (colorBox != null)
		{
			colorBox.colorChanged();
		}		
	}
	
	private float getChange()
	{
		int pixels = xCurrent - xLast;
		return (float)pixels*incValue;
	}
	
	public void setPos(int nx, int ny)
	{
		x = nx;
		y = ny;
	}
	
	public void setWidth(int w)
	{
		width = w;
		
		progBar.setWidth(w);
	}
	
	public void draw(Graphics2D g2d, boolean hasFocus)
	{
		AffineTransform prevTrans = g2d.getTransform();
		g2d.translate(x, y);
		
		// draw underline on the number
		g2d.setColor(ColorScheme.getInstance().progressFillColor);
		g2d.drawLine(0, 0, width, 0);
		
		if (hasFocus) {
			if (progBar != null) {
				g2d.translate(width/2, 2);
				progBar.draw(g2d);
			}
		}
		
		g2d.setTransform(prevTrans);
	}
	
	public boolean pick(int mx, int my)
	{
		return pickText(mx, my);
	}
	
	public boolean pickText(int mx, int my)
	{
		if (mx>x-2 && mx<x+width+2 && my>y-height && my<y) {
			return true;
		}
		
		return false;
	}
	
	public boolean valueChanged()
	{
		if (type == "int") {
			return (value.intValue() != newValue.intValue());
		}
		else if (type == "hex") {
			return (value.intValue() != newValue.intValue());
		}
		else if (type == "webcolor") {
			return (value.intValue() != newValue.intValue());
		}
		else {
			return (value.floatValue() != newValue.floatValue());
		}
	}
	
	public void setColorBox(ColorControlBox box)
	{
		colorBox = box;
	}
	
	public void setOscPort(int port)
	{
		oscPort = port;
	}
	
	public void oscSendNewValue()
	{
		int index = varIndex;
		try {
			if (type == "int") {
				OSCSender.sendInt(index, newValue.intValue(), oscPort);
			}
			else if (type == "hex") {
				OSCSender.sendInt(index, newValue.intValue(), oscPort);
			}
			else if (type == "webcolor") {
				OSCSender.sendInt(index, newValue.intValue(), oscPort);
			}
			else if (type == "float") {
				OSCSender.sendFloat(index, newValue.floatValue(), oscPort);
			}
		} catch (Exception e) { System.out.println("error sending OSC message!"); }
	}
	
	public String toString()
	{
		return type + " " + name + " = " + strValue + 
				" (tab: " + tabIndex + ", line: " + line + 
				", start: " + startChar + ", end: " + endChar + ")"; 
	}
}

/*
 * Used for sorting the handles by order of occurrence inside each tab 
 */
class HandleComparator implements Comparator<Handle> {
    public int compare(Handle handle1, Handle handle2) {
    	int tab = handle1.tabIndex - handle2.tabIndex;
    	if (tab == 0) {
    		return handle1.startChar - handle2.startChar;
    	}
    	else {
    		return tab;
    	}
    }
}
