package processing.candy;

import java.awt.geom.AffineTransform;
import java.util.HashMap;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.xml.XMLElement;


abstract public class Gradient extends BaseObject {
	AffineTransform transform;

	float[] offset;
	int[] color;
	int count;

	public Gradient(BaseObject parent, XMLElement properties) {
		super(parent, properties);

		XMLElement elements[] = properties.getChildren();
		offset = new float[elements.length];
		color = new int[elements.length];

		// <stop  offset="0" style="stop-color:#967348"/>
		for (int i = 0; i < elements.length; i++) {
			XMLElement elem = elements[i];
			String name = elem.getName();
			if (name.equals("stop")) {
				offset[count] = elem.getFloatAttribute("offset");
				String style = elem.getStringAttribute("style");
				HashMap<String, String> styles = parseStyleAttributes(style);

				String colorStr = styles.get("stop-color");
				if (colorStr == null) colorStr = "#000000";
				String opacityStr = styles.get("stop-opacity");
				if (opacityStr == null) opacityStr = "1";
				int tupacity = (int) (PApplet.parseFloat(opacityStr) * 255);
				color[count] = (tupacity << 24) |
				Integer.parseInt(colorStr.substring(1), 16);
				count++;
				//System.out.println("this color is " + PApplet.hex(color[count]));
				/*
                    int idx = farbe.indexOf("#");
                    if (idx != -1) {
                        color[count] = Integer.parseInt(farbe.substring(idx+1), 16);
                        count++;
                    } else {
                        System.err.println("problem with gradient stop " + properties);
                    }
				 */
			}
		}
	}

	abstract public void drawImpl(PGraphics g);
}


