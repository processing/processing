package processing.candy;

import processing.core.*;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.xml.XMLElement;


public class Path extends BaseObject {

	public int count = 0;
	public float[] x = new float[4];
	public float[] y = new float[4];

	static public final int MOVETO = 0;
	static public final int LINETO = 1;
	static public final int CURVETO = 2;
	static public final int QCURVETO = 3;
	public int[] kind = new int[4];

	public boolean closed = false;


	public Path(BaseObject parent, XMLElement properties) {
		super(parent, properties);
		String pathDataBuffer = "";

		if (!properties.hasAttribute("d"))
			return;

		pathDataBuffer = properties.getStringAttribute("d");
		StringBuffer pathChars = new StringBuffer();

		boolean lastSeparate = false;

		for (int i = 0; i < pathDataBuffer.length(); i++) {
			char c = pathDataBuffer.charAt(i);
			boolean separate = false;

			if (c == 'M' || c == 'm' ||
				c == 'L' || c == 'l' ||
				c == 'H' || c == 'h' ||
				c == 'V' || c == 'v' ||
				c == 'C' || c == 'c' ||  // beziers
				c == 'S' || c == 's' ||
				c == 'Q' || c == 'q' ||  // quadratic beziers
				c == 'T' || c == 't' ||
				c == 'Z' || c == 'z' ||  // closepath 
				c == ',') {
				separate = true;
				if (i != 0) {
					pathChars.append("|");
				}
			}
			if (c == 'Z' || c == 'z') {
				separate = false;
			}
			if (c == '-' && !lastSeparate) {
				pathChars.append("|");
			}
			if (c != ',') {
				pathChars.append("" + pathDataBuffer.charAt(i));
			}
			if (separate && c != ',' && c != '-') {
				pathChars.append("|");
			}
			lastSeparate = separate;
		}

		pathDataBuffer = pathChars.toString();

		//String pathDataKeys[] = PApplet.split(pathDataBuffer, '|');
		// use whitespace constant to get rid of extra spaces and CR or LF
		String pathDataKeys[] =
			PApplet.splitTokens(pathDataBuffer, "|" + PConstants.WHITESPACE);
		//for (int j = 0; j < pathDataKeys.length; j++) {
		//    PApplet.println(j + "\t" + pathDataKeys[j]);
		//}
		//PApplet.println(pathDataKeys);
		//PApplet.println();

		//float cp[] = {0, 0};
		float cx = 0;
		float cy = 0;

		int i = 0;
		//for (int i = 0; i < pathDataKeys.length; i++) {
		while (i < pathDataKeys.length) {
			char c = pathDataKeys[i].charAt(0);
			switch (c) {

			//M - move to (absolute)
			case 'M':
				/*
                    cp[0] = PApplet.parseFloat(pathDataKeys[i + 1]);
                    cp[1] = PApplet.parseFloat(pathDataKeys[i + 2]);
                    float s[] = {cp[0], cp[1]};
                    i += 2;
                    points.add(s);
				 */
				cx = PApplet.parseFloat(pathDataKeys[i + 1]);
				cy = PApplet.parseFloat(pathDataKeys[i + 2]);
				moveto(cx, cy);
				i += 3;
				break;


				//m - move to (relative)
			case 'm':
				/*
                    cp[0] = cp[0] + PApplet.parseFloat(pathDataKeys[i + 1]);
                    cp[1] = cp[1] + PApplet.parseFloat(pathDataKeys[i + 2]);
                    float s[] = {cp[0], cp[1]};
                    i += 2;
                    points.add(s);
				 */
				cx = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
				cy = cy + PApplet.parseFloat(pathDataKeys[i + 2]);
				moveto(cx, cy);
				i += 3;
				break;


			case 'L':
				cx = PApplet.parseFloat(pathDataKeys[i + 1]);
				cy = PApplet.parseFloat(pathDataKeys[i + 2]);
				lineto(cx, cy);
				i += 3;
				break;


			case 'l':
				cx = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
				cy = cy + PApplet.parseFloat(pathDataKeys[i + 2]);
				lineto(cx, cy);
				i += 3;
				break;


				// horizontal lineto absolute
			case 'H':
				cx = PApplet.parseFloat(pathDataKeys[i + 1]);
				lineto(cx, cy);
				i += 2;
				break;


				// horizontal lineto relative
			case 'h':
				cx = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
				lineto(cx, cy);
				i += 2;
				break;


			case 'V':
				cy = PApplet.parseFloat(pathDataKeys[i + 1]);
				lineto(cx, cy);
				i += 2;
				break;


			case 'v':
				cy = cy + PApplet.parseFloat(pathDataKeys[i + 1]);
				lineto(cx, cy);
				i += 2;
				break;


				// C - curve to (absolute)
			case 'C': {
				float ctrlX1 = PApplet.parseFloat(pathDataKeys[i + 1]);
				float ctrlY1 = PApplet.parseFloat(pathDataKeys[i + 2]);
				float ctrlX2 = PApplet.parseFloat(pathDataKeys[i + 3]);
				float ctrlY2 = PApplet.parseFloat(pathDataKeys[i + 4]);
				float endX = PApplet.parseFloat(pathDataKeys[i + 5]);
				float endY = PApplet.parseFloat(pathDataKeys[i + 6]);
				curveto(ctrlX1, ctrlY1, ctrlX2, ctrlY2, endX, endY);
				cx = endX;
				cy = endY;
				i += 7;
			}
			break;

			// c - curve to (relative)
			case 'c': {
				float ctrlX1 = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
				float ctrlY1 = cy + PApplet.parseFloat(pathDataKeys[i + 2]);
				float ctrlX2 = cx + PApplet.parseFloat(pathDataKeys[i + 3]);
				float ctrlY2 = cy + PApplet.parseFloat(pathDataKeys[i + 4]);
				float endX = cx + PApplet.parseFloat(pathDataKeys[i + 5]);
				float endY = cy + PApplet.parseFloat(pathDataKeys[i + 6]);
				curveto(ctrlX1, ctrlY1, ctrlX2, ctrlY2, endX, endY);
				cx = endX;
				cy = endY;
				i += 7;
			}
			break;

			// S - curve to shorthand (absolute)
			case 'S': {
				float ppx = x[count-2];
				float ppy = y[count-2];
				float px = x[count-1];
				float py = y[count-1];
				float ctrlX1 = px + (px - ppx);
				float ctrlY1 = py + (py - ppy);
				float ctrlX2 = PApplet.parseFloat(pathDataKeys[i + 1]);
				float ctrlY2 = PApplet.parseFloat(pathDataKeys[i + 2]);
				float endX = PApplet.parseFloat(pathDataKeys[i + 3]);
				float endY = PApplet.parseFloat(pathDataKeys[i + 4]);
				curveto(ctrlX1, ctrlY1, ctrlX2, ctrlY2, endX, endY);
				cx = endX;
				cy = endY;
				i += 5;
			}
			break;

			// s - curve to shorthand (relative)
			case 's': {
				float ppx = x[count-2];
				float ppy = y[count-2];
				float px = x[count-1];
				float py = y[count-1];
				float ctrlX1 = px + (px - ppx);
				float ctrlY1 = py + (py - ppy);
				float ctrlX2 = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
				float ctrlY2 = cy + PApplet.parseFloat(pathDataKeys[i + 2]);
				float endX = cx + PApplet.parseFloat(pathDataKeys[i + 3]);
				float endY = cy + PApplet.parseFloat(pathDataKeys[i + 4]);
				curveto(ctrlX1, ctrlY1, ctrlX2, ctrlY2, endX, endY);
				cx = endX;
				cy = endY;
				i += 5;
			}
			break;

			// Q - quadratic curve to (absolute)
			case 'Q': {
				float ctrlX = PApplet.parseFloat(pathDataKeys[i + 1]);
				float ctrlY = PApplet.parseFloat(pathDataKeys[i + 2]);
				float endX = PApplet.parseFloat(pathDataKeys[i + 3]);
				float endY = PApplet.parseFloat(pathDataKeys[i + 4]);
				curveto(ctrlX, ctrlY, endX, endY);
				cx = endX;
				cy = endY;
				i += 5;
			}
			break;

			// q - quadratic curve to (relative)
			case 'q': {
				float ctrlX = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
				float ctrlY = cy + PApplet.parseFloat(pathDataKeys[i + 2]);
				float endX = cx + PApplet.parseFloat(pathDataKeys[i + 3]);
				float endY = cy + PApplet.parseFloat(pathDataKeys[i + 4]);
				curveto(ctrlX, ctrlY, endX, endY);
				cx = endX;
				cy = endY;
				i += 5;
			}
			break;

			// T - quadratic curve to shorthand (absolute)
			// The control point is assumed to be the reflection of the 
			// control point on the previous command relative to the 
			// current point. (If there is no previous command or if the 
			// previous command was not a Q, q, T or t, assume the control 
			// point is coincident with the current point.) 
			case 'T': {
				float ppx = x[count-2];
				float ppy = y[count-2];
				float px = x[count-1];
				float py = y[count-1];
				float ctrlX = px + (px - ppx);
				float ctrlY = py + (py - ppy);
				float endX = PApplet.parseFloat(pathDataKeys[i + 1]);
				float endY = PApplet.parseFloat(pathDataKeys[i + 2]);
				curveto(ctrlX, ctrlY, endX, endY);
				cx = endX;
				cy = endY;
				i += 3;
			}
			break;

			// t - quadratic curve to shorthand (relative)
			case 't': {
				float ppx = x[count-2];
				float ppy = y[count-2];
				float px = x[count-1];
				float py = y[count-1];
				float ctrlX = px + (px - ppx);
				float ctrlY = py + (py - ppy);
				float endX = cx + PApplet.parseFloat(pathDataKeys[i + 1]);
				float endY = cy + PApplet.parseFloat(pathDataKeys[i + 2]);
				curveto(ctrlX, ctrlY, endX, endY);
				cx = endX;
				cy = endY;
				i += 3;
			}
			break;

			case 'Z':
			case 'z':
				closed = true;
				i++;
				break;

			default:
				String parsed = 
					PApplet.join(PApplet.subset(pathDataKeys, 0, i), ",");
			String unparsed = 
				PApplet.join(PApplet.subset(pathDataKeys, i), ",");
			System.err.println("parsed: " + parsed);
			System.err.println("unparsed: " + unparsed);
			throw new RuntimeException("shape command not handled: " + pathDataKeys[i]);
			}
		}
	}


	protected void moveto(float px, float py) {
		if (count == x.length) {
			x = PApplet.expand(x);
			y = PApplet.expand(y);
			kind = PApplet.expand(kind);
		}
		kind[count] = MOVETO;
		x[count] = px;
		y[count] = py;
		count++;
	}


	protected void lineto(float px, float py) {
		if (count == x.length) {
			x = PApplet.expand(x);
			y = PApplet.expand(y);
			kind = PApplet.expand(kind);
		}
		kind[count] = LINETO;
		x[count] = px;
		y[count] = py;
		count++;
	}


	/** Quadratic curveto command. */
	protected void curveto(float x1, float y1, float x2, float y2) {
		if (count + 2 >= x.length) {
			x = PApplet.expand(x);
			y = PApplet.expand(y);
			kind = PApplet.expand(kind);
		}
		kind[count] = QCURVETO;
		x[count] = x1;
		y[count] = y1;
		count++;
		x[count] = x2;
		y[count] = y2;
		count++;
	}


	/** Cubic curveto command. */
	protected void curveto(float x1, float y1, float x2, float y2, float x3, float y3) {
		if (count + 2 >= x.length) {
			x = PApplet.expand(x);
			y = PApplet.expand(y);
			kind = PApplet.expand(kind);
		}
		kind[count] = CURVETO;
		x[count] = x1;
		y[count] = y1;
		count++;
		x[count] = x2;
		y[count] = y2;
		count++;
		x[count] = x3;
		y[count] = y3;
		count++;
	}


	public void drawImpl(PGraphics g) {
		g.beginShape();

		g.vertex(x[0], y[0]);
		int i = 1;  // moveto has the first point
		while (i < count) {
			switch (kind[i]) {
			case MOVETO:
				g.breakShape();
				g.vertex(x[i], y[i]);
				i++;
				break;

			case LINETO:
				g.vertex(x[i], y[i]);
				i++;
				break;

			case QCURVETO:  // doubles the control point
				g.bezierVertex(x[i], y[i], x[i+1], y[i+1], x[i+1], y[i+1]);
				i += 2;
				break;

			case CURVETO:
				g.bezierVertex(x[i], y[i], x[i+1], y[i+1], x[i+2], y[i+2]);
				i += 3;
				break;
			}
		}
		g.endShape(closed ? PConstants.CLOSE : PConstants.OPEN);
	}
}