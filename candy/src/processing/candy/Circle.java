package processing.candy;

import processing.core.*;
import processing.xml.XMLElement;


public class Circle extends BaseObject {
	//float x, y, radius;
	
	public Circle(BaseObject parent, XMLElement properties) {
		super(parent, properties);
		this.x = properties.getFloatAttribute("cx");
		this.y = properties.getFloatAttribute("cy");
		float radius = properties.getFloatAttribute("r");
		width = height = radius * 2;
	}


	public void drawImpl(PGraphics g) {
		g.ellipseMode(PConstants.CENTER);
		g.ellipse(x, y, width, height);
	}
}
