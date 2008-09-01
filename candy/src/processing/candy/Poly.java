package processing.candy;

import processing.core.*;
import processing.xml.XMLElement;


public class Poly extends BaseObject {

	float points[][] = null;
	/** true if polygon, false if polyline */
	boolean closed;

	public Poly(BaseObject parent, XMLElement properties, boolean closed) {
		super(parent, properties);
		String pointsBuffer[] = null;
		this.closed = closed;

		if (properties.hasAttribute("points")) {
			pointsBuffer = PApplet.splitTokens(properties.getStringAttribute("points"));
		}

		points = new float[pointsBuffer.length][2];
		for (int i = 0; i < points.length; i++) {
			String pb[] = PApplet.split(pointsBuffer[i], ',');
			points[i][0] = Float.valueOf(pb[0]).floatValue();
			points[i][1] = Float.valueOf(pb[1]).floatValue();
		}
	}

	public void drawImpl(PGraphics g) {
		if (points != null) {
			if (points.length > 0) {
				g.beginShape();
				for (int i = 0; i < points.length; i++) {
					g.vertex(points[i][0], points[i][1]);
				}
				g.endShape(closed ? PConstants.CLOSE : PConstants.OPEN);
			}
		}
	}
}