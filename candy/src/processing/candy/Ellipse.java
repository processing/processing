package processing.candy;

import processing.core.*;
import processing.xml.XMLElement;


public class Ellipse extends BaseObject {
//	float x, y, rx, ry;

	public Ellipse(BaseObject parent, XMLElement properties) {
		super(parent, properties);
		
		x = properties.getFloatAttribute("cx");
		y = properties.getFloatAttribute("cy");
		
		float rx = properties.getFloatAttribute("rx");
		float ry = properties.getFloatAttribute("ry");
		
		x -= rx;
		y -= ry;
		
		width = rx * 2;
		height = ry * 2;
	}

	public void drawImpl(PGraphics g) {
		//g.ellipseMode(PConstants.CENTER);
		//g.ellipse(x, y, rx, ry);
		g.ellipseMode(PConstants.CORNERS);
		g.ellipse(x, y, width, height);
	}
}