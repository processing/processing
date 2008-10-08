package processing.candy;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import processing.core.PGraphics;
import processing.xml.XMLElement;


public class LinearGradient extends Gradient {
	float x1, y1, x2, y2;

	public LinearGradient(BaseObject parent, XMLElement properties) {
		super(parent, properties);

		this.x1 = properties.getFloatAttribute("x1");
		this.y1 = properties.getFloatAttribute("y1");
		this.x2 = properties.getFloatAttribute("x2");
		this.y2 = properties.getFloatAttribute("y2");

		String transformStr =
			properties.getStringAttribute("gradientTransform");
		
		if (transformStr != null) {
            float t[] = parseMatrix(transformStr).get(null);
            this.transform = new AffineTransform(t[0], t[3], t[1], t[4], t[2], t[5]);

			Point2D t1 = transform.transform(new Point2D.Float(x1, y1), null);
			Point2D t2 = transform.transform(new Point2D.Float(x2, y2), null);
			
			this.x1 = (float) t1.getX();
			this.y1 = (float) t1.getY();
			this.x2 = (float) t2.getX();
			this.y2 = (float) t2.getY();

		}
	}

	public void drawImpl(PGraphics g) { }
}


