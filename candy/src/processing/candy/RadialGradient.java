package processing.candy;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import processing.core.PGraphics;
import processing.xml.XMLElement;


public class RadialGradient extends Gradient {
	float cx, cy, r;

	public RadialGradient(BaseObject parent, XMLElement properties) {
		super(parent, properties);

		this.cx = properties.getFloatAttribute("cx");
		this.cy = properties.getFloatAttribute("cy");
		this.r = properties.getFloatAttribute("r");

		String transformStr =
			properties.getStringAttribute("gradientTransform");
		
		if (transformStr != null) {
		    float t[] = parseMatrix(transformStr).get(null);
			this.transform = new AffineTransform(t[0], t[3], t[1], t[4], t[2], t[5]);

			Point2D t1 = transform.transform(new Point2D.Float(cx, cy), null);
			Point2D t2 = transform.transform(new Point2D.Float(cx + r, cy), null);
			
			this.cx = (float) t1.getX();
			this.cy = (float) t1.getY();
			this.r = (float) (t2.getX() - t1.getX());
		}
	}

	public void drawImpl(PGraphics g) { }
}
