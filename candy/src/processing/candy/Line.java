package processing.candy;

import processing.core.PGraphics;
import processing.xml.XMLElement;


public class Line extends BaseObject {

	float x1, y1, x2, y2;

	public Line(BaseObject parent, XMLElement properties) {
		super(parent, properties);
		
		kind = LINES;
		
		this.x1 = properties.getFloatAttribute("x1");
		this.y1 = properties.getFloatAttribute("y1");
		this.x2 = properties.getFloatAttribute("x2");
		this.y2 = properties.getFloatAttribute("y2");
	}

	public void drawImpl(PGraphics g) {
		g.line(x1, y1, x2, y2);
	}
}