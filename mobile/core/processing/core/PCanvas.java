package processing.core;

import javax.microedition.lcdui.*;

import java.util.*;

/**
 *
 * @author  Francis Li
 */
public class PCanvas extends Canvas {
    private PMIDlet     midlet;
    
    private Image       buffer;
    private Graphics    bufferg;
    
    private int         width;
    private int         height;
    
    private boolean     stroke;
    private int         strokeWidth;
    private int         strokeColor;
    
    private boolean     fill;
    private int         fillColor;
    
    private int         rectMode;
    private int         ellipseMode;
    
    private int         shapeMode;
    private int[]       vertex;
    private int         vertexIndex;
    private int[]       curveVertex;
    private int         curveVertexIndex;
    
    private int         textMode;
    
    /** Creates a new instance of PCanvas */
    public PCanvas(PMIDlet midlet) {
        this.midlet = midlet;
        
        width = getWidth();
        height = getHeight();
        
        buffer = Image.createImage(width, height);
        bufferg = buffer.getGraphics();        
        
        stroke = true;
        strokeColor = 0;
        strokeWidth = 1;
        
        fill = true;
        fillColor = 0xFFFFFF;
        
        rectMode = PMIDlet.CORNER;
        ellipseMode = PMIDlet.CORNER;
        
        shapeMode = -1;
        vertex = new int[16];
        vertexIndex = 0;
        
        curveVertex = new int[8];
        curveVertexIndex = 0;
        
        textMode = Graphics.TOP | Graphics.LEFT;
        
        background(200);
    }
    
    protected void paint(Graphics g) {
        g.drawImage(buffer, 0, 0, Graphics.LEFT | Graphics.TOP);
    }
    
    protected void keyPressed(int keyCode) {
        midlet.keyPressed = true;
        
        int action = getGameAction(keyCode);
        switch (action) {
            case Canvas.UP:
                midlet.key = PMIDlet.UP;
                break;
            case Canvas.DOWN:
                midlet.key = PMIDlet.DOWN;
                break;
            case Canvas.LEFT:
                midlet.key = PMIDlet.LEFT;
                break;
            case Canvas.RIGHT:
                midlet.key = PMIDlet.RIGHT;
                break;
            case Canvas.FIRE:
                midlet.key = PMIDlet.FIRE;
                break;
            default:
                //// MIDP 1.0 says the KEY_ values map to ASCII values, but I've seen it
                //// different on some foreign (i.e. Korean) handsets
                if ((keyCode >= Canvas.KEY_NUM0) && (keyCode <= Canvas.KEY_NUM9)) {
                    midlet.key = (char) ('0' + (keyCode - Canvas.KEY_NUM0));
                } else {
                    switch (keyCode) {
                        case Canvas.KEY_POUND:
                            midlet.key = '#';
                            break;
                        case Canvas.KEY_STAR:
                            midlet.key = '*';
                            break;
                    }
                }
        }
        midlet.keyPressed();
    }
    
    protected void keyReleased(int keyCode) {
        midlet.keyPressed = false;
        midlet.keyReleased();
    }
    
    public void point(int x1, int y1) {
        if (stroke) {
            bufferg.setColor(strokeColor);
            bufferg.drawLine(x1, y1, x1, y1);
        }
    }
    
    public void line(int x1, int y1, int x2, int y2) {
        if (stroke) {
            bufferg.setColor(strokeColor);
            bufferg.drawLine(x1, y1, x2, y2);
            if (strokeWidth > 1) {
                //// to do
            }
        }
    }
    
    public void triangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        shapeMode = PMIDlet.POLYGON;
        vertex[0] = x1;
        vertex[1] = y1;
        vertex[2] = x2;
        vertex[3] = y2;
        vertex[4] = x3;
        vertex[5] = y3;
        vertexIndex = 6;
        endShape();
    }
    
    public void quad(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        shapeMode = PMIDlet.POLYGON;
        vertex[0] = x1;
        vertex[1] = y1;
        vertex[2] = x2;
        vertex[3] = y2;
        vertex[4] = x3;
        vertex[5] = y3;
        vertex[6] = x4;
        vertex[7] = y4;
        vertexIndex = 8;
        endShape();
    }
    
    public void rect(int x, int y, int width, int height) {
        int temp;
        switch (rectMode) {
            case PMIDlet.CORNERS:
                temp = x;
                x = Math.min(x, width);
                width = Math.abs(x - temp);
                temp = y;
                y = Math.min(y, height);
                height = Math.abs(y - temp);
                break;
            case PMIDlet.CENTER:
                x -= width / 2;
                y -= height / 2;
                break;
        }
        if (fill) {
            bufferg.setColor(fillColor);
            bufferg.fillRect(x, y, width, height);
        }
        if (stroke) {
            bufferg.setColor(strokeColor);
            bufferg.drawRect(x, y, width, height);
        }
    }
    
    public void rectMode(int MODE) {
        if ((MODE >= PMIDlet.CORNER) && (MODE <= PMIDlet.CENTER)) {
            rectMode = MODE;
        }
    }
    
    public void ellipse(int x, int y, int width, int height) {
        int temp;
        switch (ellipseMode) {
            case PMIDlet.CORNERS:
                temp = x;
                x = Math.min(x, width);
                width = Math.abs(x - temp);
                temp = y;
                y = Math.min(y, height);
                height = Math.abs(y - temp);
                break;
            case PMIDlet.CENTER:
                x -= width / 2;
                y -= height / 2;
                break;
            case PMIDlet.CENTER_RADIUS:
                x -= width;
                y -= height;
                width *= 2;
                height *= 2;
                break;
        }
        if (fill) {
            bufferg.setColor(fillColor);
            bufferg.fillArc(x, y, width, height, 0, 360);
        }
        if (stroke) {
            bufferg.setColor(strokeColor);
            bufferg.drawArc(x, y, width, height, 0, 360);
        }
    }
    
    public void ellipseMode(int MODE) {
        if ((MODE >= PMIDlet.CORNER) && (MODE <= PMIDlet.CENTER_RADIUS)) {
            ellipseMode = MODE;
        }
    }
    
    public void curve(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        beginShape(PMIDlet.LINE_STRIP);
        curveVertex(x1, y1);
        curveVertex(x1, y1);
        curveVertex(x2, y2);
        curveVertex(x3, y3);
        curveVertex(x4, y4);
        curveVertex(x4, y4);
        endShape();
    }
    
    public void bezier(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        beginShape(PMIDlet.LINE_STRIP);
        bezierVertex(x1, y1);
        bezierVertex(x2, y2);
        bezierVertex(x3, y3);
        bezierVertex(x4, y4);
        endShape();
    }
    
    public void strokeWeight(int width) {
        strokeWidth = width;
    }
    
    public void beginShape(int MODE) {
        if ((MODE >= PMIDlet.POINTS) && (MODE <= PMIDlet.POLYGON)) {
            shapeMode = MODE;
        } else {
            shapeMode = PMIDlet.POINTS;
        }
        vertexIndex = 0;
        curveVertexIndex = 0;
    }
    
    public void endShape() {
        int i;
        int step;
        switch (shapeMode) {
            case PMIDlet.POINTS:
                i = 0;
                step = 2;
                break;
            case PMIDlet.LINES:
                i = 2;
                step = 4;
                break;
            case PMIDlet.LINE_STRIP:
            case PMIDlet.LINE_LOOP:
                i = 2;
                step = 2;
                break;
            case PMIDlet.TRIANGLES:
                i = 4;
                step = 6;
                break;
            case PMIDlet.TRIANGLE_STRIP:
                i = 4;
                step = 2;
                break;
            case PMIDlet.QUADS:
                i = 6;
                step = 8;
                break;
            case PMIDlet.QUAD_STRIP:
                i = 6;
                step = 4;
                break;
            case PMIDlet.POLYGON:
                polygon(0, vertexIndex - 2);
                return;
            default:
                return;
        }
        
        for (; i < vertexIndex; i += step) {
            switch (shapeMode) {
                case PMIDlet.POINTS:
                    point(vertex[i], vertex[i + 1]);
                    break;
                case PMIDlet.LINES:
                case PMIDlet.LINE_STRIP:
                case PMIDlet.LINE_LOOP:
                    line(vertex[i - 2], vertex[i - 1], vertex[i], vertex[i + 1]);
                    break;
                case PMIDlet.TRIANGLES:
                case PMIDlet.TRIANGLE_STRIP:
                    polygon(i - 4, i);
                    break;
                case PMIDlet.QUADS:
                case PMIDlet.QUAD_STRIP:
                    polygon(i - 6, i);
                    break;
            }
        }
        //// handle loop closing
        if (shapeMode == PMIDlet.LINE_LOOP) {
            if (vertexIndex >= 2) {
                line(vertex[vertexIndex - 2], vertex[vertexIndex - 1], vertex[0], vertex[1]);
            }
        }
        
        vertexIndex = 0;
    }
    
    private void polygon(int startIndex, int endIndex) {
        //// make sure at least 2 vertices
        if (endIndex >= (startIndex + 2)) {
            //// make sure at least 3 vertices for fill
            if (endIndex >= (startIndex + 4)) {
                if (fill) {
                    bufferg.setColor(fillColor);
                    
                    //// insertion sort of edges from top-left to bottom right
                    Vector edges = new Vector();
                    int edgeCount = 0;
                    Edge e1, e2;
                    int i, j;
                    int yMin = Integer.MAX_VALUE;
                    int yMax = Integer.MIN_VALUE;
                    for (i = startIndex; i <= endIndex; i += 2) {
                        e1 = new Edge();
                        if (i == startIndex) {
                            //// handle connecting line between start and endpoints
                            if (vertex[startIndex + 1] < vertex[endIndex + 1]) {
                                e1.x1 = vertex[startIndex];
                                e1.y1 = vertex[startIndex + 1];
                                e1.x2 = vertex[endIndex];
                                e1.y2 = vertex[endIndex + 1];
                            } else {
                                e1.x1 = vertex[endIndex];
                                e1.y1 = vertex[endIndex + 1];
                                e1.x2 = vertex[startIndex];
                                e1.y2 = vertex[startIndex + 1];
                            }                            
                        } else if (vertex[i - 1] < vertex[i + 1]) {
                            e1.x1 = vertex[i - 2];
                            e1.y1 = vertex[i - 1];
                            e1.x2 = vertex[i];
                            e1.y2 = vertex[i + 1];
                        } else {
                            e1.x1 = vertex[i];
                            e1.y1 = vertex[i + 1];
                            e1.x2 = vertex[i - 2];
                            e1.y2 = vertex[i - 1];
                        }                            
                        e1.x = e1.x1;
                        e1.dx = e1.x2 - e1.x1;
                        e1.dy = e1.y2 - e1.y1;
                        
                        yMin = Math.min(e1.y1, yMin);
                        yMax = Math.max(e1.y2, yMax);
                        
                        for (j = 0; j < edgeCount; j++) {
                            e2 = (Edge) edges.elementAt(j);
                            if (e1.y1 < e2.y1) {
                                edges.insertElementAt(e1, j);
                                e1 = null;
                                break;
                            } else if (e1.y1 == e2.y1) {
                                if (e1.x1 < e2.x1) {
                                    edges.insertElementAt(e1, j);
                                    e1 = null;
                                    break;
                                }
                            }
                        }
                        if (e1 != null) {
                            edges.addElement(e1);
                        }
                        edgeCount += 1;
                    }
                    
                    //// draw scanlines
                    Vector active = new Vector();
                    int activeCount = 0;
                    for (int y = yMin; y <= yMax; y++) {
                        //// update currently active edges
                        for (i = activeCount - 1; i >= 0; i--) {
                            e1 = (Edge) active.elementAt(i);
                            if (e1.y2 <= y) {
                                //// remove edges not intersecting current scan line
                                active.removeElementAt(i);
                                activeCount--;
                            } else {
                                //// update x coordinate
                                e1.x = (y - e1.y1) * e1.dx / e1.dy + e1.x1;
                            }
                        }
                        
                        //// re-sort active edge list
                        Vector newActive = new Vector();
                        for (i = 0; i < activeCount; i++) {
                            e1 = (Edge) active.elementAt(i);
                            
                            for (j = 0; j < i; j++) {
                                e2 = (Edge) newActive.elementAt(j);
                                if (e1.x < e2.x) {
                                    newActive.insertElementAt(e1, j);
                                    e1 = null;
                                    break;
                                }
                            }
                            if (e1 != null) {
                                newActive.addElement(e1);
                            }
                        }
                        active = newActive;
                        
                        //// insertion sort any new intersecting edges into active list
                        for (i = 0; i < edgeCount; i++) {
                            e1 = (Edge) edges.elementAt(i);
                            if (e1.y1 == y) {
                                for (j = 0; j < activeCount; j++) {
                                    e2 = (Edge) active.elementAt(j);
                                    if (e1.x < e2.x) {
                                        active.insertElementAt(e1, j);
                                        e1 = null;
                                        break;
                                    }
                                }
                                if (e1 != null) {
                                    active.addElement(e1);
                                }
                                activeCount++;
                                
                                //// remove from edges list
                                edges.removeElementAt(i);
                                edgeCount--;
                                //// indices are shifted down one
                                i--;
                            } else {
                                break;
                            }
                        }
                        //// draw line segments between pairs of edges
                        for (i = 1; i < activeCount; i += 2) {
                            e1 = (Edge) active.elementAt(i - 1);
                            e2 = (Edge) active.elementAt(i);
                            
                            bufferg.drawLine(e1.x, y, e2.x, y);
                        }
                    }
                }
            }
            if (stroke) {
                for (int i = startIndex + 2; i <= endIndex; i += 2) {
                    line(vertex[i - 2], vertex[i - 1], vertex[i], vertex[i + 1]);
                }
                line(vertex[endIndex], vertex[endIndex + 1], vertex[startIndex], vertex[startIndex + 1]);
            }
        }
    }
    
    public void vertex(int x, int y) {
        vertex[vertexIndex] = x;
        vertexIndex++;
        vertex[vertexIndex] = y;
        vertexIndex++;
        
        int length = vertex.length;
        if (vertexIndex == length) {
            int[] old = vertex;
            vertex = new int[length * 2];
            System.arraycopy(old, 0, vertex, 0, length);
        }        
    }
    
    public void curveVertex(int x, int y) {
        //// use fixed point, 8-bit precision
        curveVertex[curveVertexIndex] = x << 8;
        curveVertexIndex++;
        curveVertex[curveVertexIndex] = y << 8;
        curveVertexIndex++;
        
        if (curveVertexIndex == 8) {
            int tension = 128 /* 0.5f */;
            
            int dx0 = ((curveVertex[4] - curveVertex[0]) * tension) >> 8;
            int dx1 = ((curveVertex[6] - curveVertex[2]) * tension) >> 8;
            int dy0 = ((curveVertex[5] - curveVertex[1]) * tension) >> 8;
            int dy1 = ((curveVertex[7] - curveVertex[3]) * tension) >> 8;
            
            plotCurveVertices(curveVertex[2], curveVertex[3],
                              curveVertex[4], curveVertex[5],
                              dx0, dx1, dy0, dy1);
            
            for (int i = 0; i < 6; i++) {
                curveVertex[i] = curveVertex[i + 2];
            }
            curveVertexIndex = 6;
        }
    }
    
    public void bezierVertex(int x, int y) {
        //// use fixed point, 8-bit precision
        curveVertex[curveVertexIndex] = x << 8;
        curveVertexIndex++;
        curveVertex[curveVertexIndex] = y << 8;
        curveVertexIndex++;
        
        if (curveVertexIndex == 8) {
            //// use fixed point, 8-bit precision
            int tension = 768 /* 3.0f */;
            
            int dx0 = ((curveVertex[2] - curveVertex[0]) * tension) >> 8;
            int dx1 = ((curveVertex[6] - curveVertex[4]) * tension) >> 8;
            int dy0 = ((curveVertex[3] - curveVertex[1]) * tension) >> 8;
            int dy1 = ((curveVertex[7] - curveVertex[5]) * tension) >> 8;
            
            plotCurveVertices(curveVertex[0], curveVertex[1],
                              curveVertex[6], curveVertex[7],
                              dx0, dx1, dy0, dy1);
            
            curveVertexIndex = 0;
        }
    }
    
    private void plotCurveVertices(int x0, int y0, int x1, int y1, int dx0, int dx1, int dy0, int dy1) {
        int x, y, t, t2, t3, h0, h1, h2, h3;
        vertex(x0 >> 8, y0 >> 8);
        for (t = 0; t < 256 /* 1.0f */; t += 26 /* 0.1f */) {
            t2 = (t * t) >> 8;
            t3 = (t * t2) >> 8;

            h0 = ((512 /* 2.0f */ * t3) >> 8) - ((768 /* 3.0f */ * t2) >> 8) + 256 /* 1.0f */;
            h1 = ((-512 /* -2.0f */ * t3) >> 8) + ((768 /* 3.0f */ * t2) >> 8);
            h2 = t3 - ((512 /* 2.0f */ * t2) >> 8) + t;
            h3 = t3 - t2;

            x = ((h0 * x0) >> 8) + ((h1 * x1) >> 8) + ((h2 * dx0) >> 8) + ((h3 * dx1) >> 8);
            y = ((h0 * y0) >> 8) + ((h1 * y1) >> 8) + ((h2 * dy0) >> 8) + ((h3 * dy1) >> 8);
            vertex(x >> 8, y >> 8);
        }
        vertex(x1 >> 8, y1 >> 8);
    }
    
    public void translate(int x, int y) {
        bufferg.translate(x, y);
    }
    
    public void background(int gray) {
        background(gray, gray, gray);
    }
    
    public void background(int value1, int value2, int value3) {
        bufferg.setColor(value1, value2, value3);
        bufferg.fillRect(0, 0, width, height);
    }
    
    public void stroke(int gray) {
        stroke(gray, gray, gray);
    }
    
    public void stroke(int value1, int value2, int value3) {
        stroke = true;
        strokeColor = ((value1 & 0xFF) << 16) | ((value2 & 0xFF) << 8) | (value3 & 0xFF);
    }
    
    public void noStroke() {
        stroke = false;
    }
    
    public void fill(int gray) {
        fill(gray, gray, gray);
    }
    
    public void fill(int value1, int value2, int value3) {
        fill = true;
        fillColor = ((value1 & 0xFF) << 16) | ((value2 & 0xFF) << 8) | (value3 & 0xFF);
    }
    
    public void noFill() {
        fill = false;
    }
    
    public void text(String data, int x, int y) {
        bufferg.drawString(data, x, y, textMode);
    }
    
    public void textMode(int MODE) {
        switch (MODE) {
            case PMIDlet.ALIGN_LEFT:
                textMode = Graphics.TOP | Graphics.LEFT;
                break;
            case PMIDlet.ALIGN_RIGHT:
                textMode = Graphics.TOP | Graphics.RIGHT;
                break;
            case PMIDlet.ALIGN_CENTER:
                textMode = Graphics.TOP | Graphics.HCENTER;
                break;
        }
    }
    
    private static class Edge {
        public int x1, y1, x2, y2;
        public int x, dx, dy;
    }
}