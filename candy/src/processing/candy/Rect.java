package processing.candy;

import processing.core.*;
import processing.xml.XMLElement;


public class Rect extends BaseObject {
//	float x, y, w, h;

	public Rect(BaseObject parent, XMLElement properties) {
		super(parent, properties);
		x = properties.getFloatAttribute("x");
		y = properties.getFloatAttribute("y");
		width = properties.getFloatAttribute("width");
		height = properties.getFloatAttribute("height");
	}

	public void drawImpl(PGraphics g) {
		g.rectMode(PConstants.CORNER);
		g.rect(x, y, width, height);
	}
}


