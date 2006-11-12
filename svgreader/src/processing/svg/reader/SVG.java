package processing.svg.reader;

//import java.awt.Color;
//import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphicsJava2D;
import processing.xml.XMLElement;

//CANDY
//Processing SVG import for the common man
//author:  Flux
//with contributions by: Christian Riekoff
//email:   flux.blackcat@gmail.com
//http://www.ghost-hack.com
//Built for Processing 118

//Using Christian Riekoff's handy proXML parser
//http://www.texone.org/proxml
//Hawt.

//Last 100% working for Processing 0118
//Please send bugs and annoyances to above email
//Refer to readme.txt that was part of this package for legal jargon

/*
TODO
X actual linear gradients working properly
X add a table for all objects with their names, so they can be grabbed individually
_   add accessor to get items from the table
_   see if items can be named in illusfarter using the svg palette
X compound shapes (fonts) won't wind properly, so fill will be messed up
X   added hack to allow for broken shapes
X rename draw() and its buddy
X a moveto *inside* a shape will be treated as a lineto
X   had to fix this
X implement polyline
_ try enabling blending modes
_ test what happens when transparency is used with gradient fill
_ some means of centering the entire drawing (is this included already?)
_   or setting to one of the corners
_   does the svg spec just do this?
_ look into transformation issues... guessing this is probably wrong
_   this may be what's throwing off the radial radius transform
_ implement A and a
_ check for any other pieces of missing path api
_   multiple sets of coordinates after a command not supported
_   i.e. M with several coords means moveto followed by many linetos
_   also curveto with multiple sets of points is ignored
*/


public class SVG {

    public String filename;

    //protected static PApplet parent;
    //private static XMLInOut xmlio;
    protected PApplet parent;

    public float width;
    public float height;    
    //private float svgWidth = 0;
    //private float svgHeight = 0;

    //private Vector draw = new Vector();

    private Hashtable idTable = new Hashtable();  
    private XMLElement svg;
    private Group root;

    //For some reason Processing was using -1 for #FFFFFFFF
    //thus we will use our own null value
    //private static int transValue = -255;

    private boolean styleOverride = false;

    /**
     * Initializes a new SVG Object with the given filename.
     * @param filename, String: filename of the object
     * @param parent, PApplet: instance of the parent application
     */
    public SVG(String filename, PApplet parent){
        this.parent = parent;        
        this.filename = filename;

        // this will grab the root document, starting <svg ...>
        // the xml version and initial comments are ignored
        svg = new XMLElement(filename, parent);
        
        /*
        Reader reader = parent.createReader(filename);
        if (reader == null) {
            System.err.println("The file " + filename + " could not be found.");
            return;
        }
        try {
            document.parseFromReader(reader);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load SVG file");
            //throw new RuntimeException("Could not load SVG file");
            return;
        }
        */
        if (!svg.getName().equals("svg")) {
            throw new RuntimeException("root isn't svg, it's " + svg.getName());
        }

        width = svg.getFloatAttribute("width");
        height = svg.getFloatAttribute("height");
        
        /*
        PApplet.println("document has " + document.getChildCount() + " children");
        //Get the xml child node we need
        XMLElement doc = document.getChild(1);
        PApplet.println(doc);
        if (true) return;
        */
        
        /*
        //XMLElement entSVG = doc.getChild(0);
        //XMLElement svg = entSVG.getChild(1);
        //While we're doing that, save the width and height too
        //svgWidth = svg.getIntAttribute("width");
        //svgHeight = svg.getIntAttribute("height");

        //Catch exception when SVG doesn't have a <g> tag
        XMLElement graphics;	
        String nameOfFirstChild = svg.getChild(1).toString();
        if(nameOfFirstChild.equals("<g>"))
            graphics = svg.getChild(1);	
        else
            graphics = svg;	   

        this.svgData = svg;
        */
        
        //parseChildren(document);
        root = new Group(svg);
        
        /*
        XMLElement graphics = null;

        //Print SVG on construction
        //Use this for debugging
        //svg.printElementTree(" .");
        */
    }
    
    
    /**
     * Draws the SVG document.
     *
     */
    public void draw(){
        //Maybe it would be smart to save all changes that have 
        // to made to processing modes here like
        int saveEllipseMode = parent.g.ellipseMode;

        boolean stroke = parent.g.stroke;
        int strokeColor = parent.g.strokeColor;

        boolean fill = parent.g.fill;
        int fillColor = parent.g.fillColor;

        /*
        for (int i = 0; i < draw.size(); i++){
            VectorObject vo = (VectorObject) draw.get(i);
            vo.basicDraw();
        }
        */
        root.draw();

        //set back the changed modes
        parent.g.ellipseMode = saveEllipseMode;
        parent.g.stroke = stroke;
        parent.g.fill = fill;

        parent.g.strokeColor = strokeColor;
        parent.g.fillColor = fillColor;
    }

    
    /**
     * Overrides SVG-set styles and uses PGraphics styles and colors.
     */
    public void ignoreStyles(boolean state){
        styleOverride = state;
    }

    
    /**
     * Prints out the SVG document usefull for parsing
     */
    public void print(){
        PApplet.println(svg.toString());
    }
    

    /*
    //Converts a string to a float
    private float valueOf(String s){
        return Float.valueOf(s).floatValue();
    }
    */

    
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 
    
    
    protected abstract class BaseObject {
        String id;
        
        public BaseObject(XMLElement properties) {
            id = properties.getStringAttribute("id");
            if (id != null) {
                idTable.put(id, this);
                //System.out.println("now parsing " + id);
            }
        }

        protected abstract void drawShape();
        
        protected void draw() {
            drawShape();
        }
    }
    
    
    //Default vector graphics class from which all others will polymorph
    protected abstract class VectorObject extends BaseObject {

        boolean stroke;
        int strokeColor; // = transValue;
        float strokeWeight = Float.NaN; // none
        Gradient strokeGradient; 
        Paint strokeGradientPaint;
        String strokeName;  // id of another object, gradients only?
        
        boolean fill;
        int fillColor; // = transValue;
        Gradient fillGradient;
        Paint fillGradientPaint;
        String fillName;  // id of another object

        boolean hasTransform; //= false;
        float[] transformation; //= null;

        float opacity;
        
        //Should we keep these here even when we don't have transforms?
        float rotation = 0;
        float translateX = 0;
        float translateY = 0;


        public VectorObject(XMLElement properties) {
            super(properties);
            getColors(properties);
            getTransformation(properties);
        }


        private void getTransformation(XMLElement properties){
            String transform = "";
            if (properties.hasAttribute("transform")){
                this.hasTransform = true;
                transform = properties.getStringAttribute("transform");
                transform = transform.substring(7, transform.length() - 2);
                String tf[] = PApplet.split(transform);

                this.transformation = new float[tf.length];
                for (int i = 0; i < transformation.length; i++)
                    this.transformation[i] = Float.valueOf(tf[i]).floatValue();
            }
            //Hacky code to get rotation working
            //Done through the powers of trial and error
            if (this.hasTransform){
                float t[] = this.transformation;
                if (t[0] < 0 && t[1] < 0 && t[2] > 0 && t[3] < 0)
                    this.rotation = -parent.acos(this.transformation[3]);
                if (t[0] > 0 && t[1] < 0 && t[2] > 0 && t[3] > 0)
                    this.rotation = parent.asin(this.transformation[1]);
                if (t[0] < 0 && t[1] > 0 && t[2] < 0 && t[3] < 0)
                    this.rotation = parent.acos(this.transformation[0]);
                if (t[0] > 0 && t[1] > 0 && t[2] < 0 && t[3] > 0)
                    this.rotation = parent.acos(this.transformation[0]);
                this.translateX = this.transformation[4];
                this.translateY = this.transformation[5];
            }
        }

        
        /*
        private int colorFromString(String color, String opacity){ 
            if (!color.equals("none")){
                color = color.substring(1, 7);
                color = opacity + color;
                return PApplet.unhex(color);
            }else{
                return transValue;
            }
        }
        */
        

        //We'll need color information like stroke, fill, opacity, stroke-weight
        protected void getColors(XMLElement properties){
            
            // opacity for postscript-derived things like svg affects both stroke and fill
            //if (properties.hasAttribute("opacity")) {
            opacity = properties.getFloatAttribute("opacity", 1);
            //}
            int opacityMask = ((int) (opacity * 255)) << 24;
            
            String strokeText = properties.getStringAttribute("stroke", "none");
            if (strokeText.equals("none")) {

            } else if (strokeText.startsWith("#")) {
                stroke = true;
                strokeColor = opacityMask |
                    (Integer.parseInt(strokeText.substring(1), 16)) & 0xFFFFFF;
            } else if (strokeText.startsWith("url(#")) {
                strokeName = strokeText.substring(5, strokeText.length() - 1);
                Object strokeObject = idTable.get(strokeName);
                if (strokeObject instanceof Gradient) {
                    strokeGradient = (Gradient) strokeObject;
                    strokeGradientPaint = calcGradientPaint(strokeGradient);
                } else {
                    System.err.println("url " + strokeName + " refers to unexpected data");
                }
            }
            
            // fill defaults to black (though stroke defaults to "none")
            // http://www.w3.org/TR/SVG/painting.html#FillProperties
            String fillText = properties.getStringAttribute("fill", "#000000");
            if (fillText.equals("none")) {

            } else if (fillText.startsWith("#")) {
                fill = true;
                fillColor = opacityMask |
                    (Integer.parseInt(fillText.substring(1), 16)) & 0xFFFFFF;
                //System.out.println("hex for fill is " + PApplet.hex(fillColor));
            } else if (fillText.startsWith("url(#")) {
                fillName = fillText.substring(5, fillText.length() - 1);
                //PApplet.println("looking for " + fillName);
                Object fillObject = idTable.get(fillName);
                //PApplet.println("found " + fillObject);
                if (fillObject instanceof Gradient) {
                    fill = true;
                    fillGradient = (Gradient) fillObject;
                    fillGradientPaint = calcGradientPaint(fillGradient);
                    //PApplet.println("got filla " + fillObject);
                } else {
                    System.err.println("url " + fillName + " refers to unexpected data");
                }
            }

            //if (properties.hasAttribute("stroke-width")){
            //strokeWeight = properties.getFloatAttribute("stroke-width");
            //}
            strokeWeight = properties.getFloatAttribute("stroke-width", Float.NaN);
        }

        
        protected Paint calcGradientPaint(Gradient gradient) {
            if (gradient instanceof LinearGradient) {
                LinearGradient grad = (LinearGradient) gradient;
                
                /*
                Color c1 = new Color(0xFF000000 | grad.color[0]);
                Color c2 = new Color(0xFF000000 | grad.color[grad.count-1]);
                return new GradientPaint(grad.x1, grad.y1, c1,
                                         grad.x2, grad.y2, c2);
                                         */
                return new LinearGradientPaint(grad.x1, grad.y1, grad.x2, grad.y2,
                                               grad.offset, grad.color, grad.count);
                
                    
            } else if (gradient instanceof RadialGradient) {
                RadialGradient grad = (RadialGradient) gradient;
                
                //Color c1 = new Color(0xFF000000 | grad.color[0]);
                //Color c2 = new Color(0xFF000000 | grad.color[grad.count-1]);
                return new RadialGradientPaint(grad.cx, grad.cy, grad.r, 
                                               grad.offset, grad.color, grad.count);
            }
            return null;
        }
        

        protected abstract void drawShape();

        
        //might be more efficient for all subclasses
        protected void draw(){
            //PApplet.println(getClass().getName() + " " + id);

            /*
             *                     p2d.fillGradient = true;
                    p2d.fillGradientObject = paint;

             */
            if (!styleOverride) {
                parent.colorMode(PConstants.RGB, 255);
                
                if (stroke) {
                    parent.stroke(strokeColor);
                } else {
                    parent.noStroke();
                }
                if (fill) {
                    //System.out.println("filling " + PApplet.hex(fillColor));
                    parent.fill(fillColor);
                } else {
                    parent.noFill();
                }                

                if (parent.g instanceof PGraphicsJava2D) {
                    PGraphicsJava2D p2d = ((PGraphicsJava2D) parent.g);
                
                    if (strokeGradient != null) {
                        p2d.strokeGradient = true;
                        p2d.strokeGradientObject = strokeGradientPaint;
                    } else {
                        // need to shut off, in case parent object has a gradient applied
                        //p2d.strokeGradient = false;
                    }
                    if (fillGradient != null) {
                        p2d.fillGradient = true;
                        p2d.fillGradientObject = fillGradientPaint;
                    } else {
                        // need to shut off, in case parent object has a gradient applied
                        //p2d.fillGradient = false;
                    }
                }
                
                if (!Float.isNaN(strokeWeight)) {
                    parent.strokeWeight(strokeWeight);
                }
            }
            
            if (hasTransform){
                parent.pushMatrix();
                parent.translate(translateX, translateY);
                parent.rotate(rotation);
            }

            drawShape();

            if (hasTransform){
                parent.popMatrix();
            }

            if (parent.g instanceof PGraphicsJava2D) {
                PGraphicsJava2D p2d = ((PGraphicsJava2D) parent.g);
            
                if (strokeGradient != null) {
                    p2d.strokeGradient = false;
                }
                if (fillGradient != null) {
                    p2d.fillGradient = false;
                }
            }
        }
    }

    
    public class RadialGradientPaint implements Paint {
        float cx, cy, radius;
        float[] offset;
        int[] color;
        int count;
        
        public RadialGradientPaint(float cx, float cy, float radius, 
                                   float[] offset, int[] color, int count) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.offset = offset;
            this.color = color;
            this.count = count;
        }

        public PaintContext createContext(ColorModel cm,
                                          Rectangle deviceBounds, Rectangle2D userBounds,
                                          AffineTransform xform, RenderingHints hints) {
            Point2D transformedPoint = 
                xform.transform(new Point2D.Float(cx, cy), null);
            // this causes problems
            //Point2D transformedRadius = 
            //    xform.deltaTransform(new Point2D.Float(radius, radius), null);
            return new RadialGradientContext((float) transformedPoint.getX(), 
                                             (float) transformedPoint.getY(), 
                                             radius, //(float) transformedRadius.distance(0, 0), 
                                             offset, color, count);
        }

        public int getTransparency() {
            /*
            int a1 = mPointColor.getAlpha();
            int a2 = mBackgroundColor.getAlpha();
            return (((a1 & a2) == 0xff) ? OPAQUE : TRANSLUCENT);
            */
            return OPAQUE;
        }
    }          
    
    
    public class RadialGradientContext implements PaintContext {
        float cx, cy, radius;
        float[] offset;
        int[] color;
        int count;
        
        public RadialGradientContext(float cx, float cy, float radius, 
                                     float[] offset, int[] color, int count) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.offset = offset;
            this.color = color;
            this.count = count;
        }

        public void dispose() {}

        public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }

        int ACCURACY = 5;
        
        public Raster getRaster(int x, int y, int w, int h) {
            WritableRaster raster =
                getColorModel().createCompatibleWritableRaster(w, h);

            //System.out.println("radius here is " + radius);
            int span = (int) radius * ACCURACY; 
            int[][] interp = new int[span][3];
            int prev = 0;
            for (int i = 1; i < count; i++) {
                int c0 = color[i-1];
                int c1 = color[i];
                int last = (int) (offset[i] * (span - 1));
                for (int j = prev; j <= last; j++) {
                    float btwn = PApplet.norm(j, prev, last);
                    interp[j][0] = (int) PApplet.lerp((c0 >> 16) & 0xff, (c1 >> 16) & 0xff, btwn);
                    interp[j][1] = (int) PApplet.lerp((c0 >> 8) & 0xff, (c1 >> 8) & 0xff, btwn);
                    interp[j][2] = (int) PApplet.lerp(c0 & 0xff, c1 & 0xff, btwn);
                }
                prev = last;
            }
                
            int[] data = new int[w * h * 4];
            int index = 0;
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    float distance = PApplet.dist(cx, cy, x + i, y + j);
                    int which = PApplet.min((int) (distance * ACCURACY), interp.length-1);
                    
                    data[index++] = interp[which][0];
                    data[index++] = interp[which][1];
                    data[index++] = interp[which][2];
                    data[index++] = 255;
                }
            }
            raster.setPixels(0, 0, w, h, data);

            return raster;
        }
    }

    
    public class LinearGradientPaint implements Paint {
        float x1, y1, x2, y2;
        float[] offset;
        int[] color;
        int count;
        
        public LinearGradientPaint(float x1, float y1, float x2, float y2,
                                   float[] offset, int[] color, int count) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.offset = offset;
            this.color = color;
            this.count = count;
        }

        public PaintContext createContext(ColorModel cm,
                                          Rectangle deviceBounds, Rectangle2D userBounds,
                                          AffineTransform xform, RenderingHints hints) {
            Point2D t1 = xform.transform(new Point2D.Float(x1, y1), null);
            Point2D t2 = xform.transform(new Point2D.Float(x2, y2), null);
            return new LinearGradientContext((float) t1.getX(), (float) t1.getY(), 
                                             (float) t2.getX(), (float) t2.getY(), 
                                             offset, color, count);
        }

        public int getTransparency() {
            /*
            int a1 = mPointColor.getAlpha();
            int a2 = mBackgroundColor.getAlpha();
            return (((a1 & a2) == 0xff) ? OPAQUE : TRANSLUCENT);
            */
            return OPAQUE;
        }
    }          
    
    
    public class LinearGradientContext implements PaintContext {
        float x1, y1, x2, y2;
        float[] offset;
        int[] color;
        int count;
        
        public LinearGradientContext(float x1, float y1, float x2, float y2,
                                     float[] offset, int[] color, int count) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.offset = offset;
            this.color = color;
            this.count = count;
        }

        public void dispose() { }

        public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }

        int ACCURACY = 1;
        
        public Raster getRaster(int x, int y, int w, int h) {
            WritableRaster raster =
                getColorModel().createCompatibleWritableRaster(w, h);

            // make normalized version of base vector
            float nx = x2 - x1;
            float ny = y2 - y1;
            float len = (float) Math.sqrt(nx*nx + ny*ny);
            if (len != 0) {
                nx /= len;
                ny /= len;
            }
            
            int span = (int) PApplet.dist(x1, y1, x2, y2) * ACCURACY;
            int[][] interp = new int[span][3];
            int prev = 0;
            for (int i = 1; i < count; i++) {
                int c0 = color[i-1];
                int c1 = color[i];
                int last = (int) (offset[i] * (span-1));
                //System.out.println("last is " + last);
                for (int j = prev; j <= last; j++) {
                    float btwn = PApplet.norm(j, prev, last);
                    interp[j][0] = (int) PApplet.lerp((c0 >> 16) & 0xff, (c1 >> 16) & 0xff, btwn);
                    interp[j][1] = (int) PApplet.lerp((c0 >> 8) & 0xff, (c1 >> 8) & 0xff, btwn);
                    interp[j][2] = (int) PApplet.lerp(c0 & 0xff, c1 & 0xff, btwn);
                    //System.out.println(j + " " + interp[j][0] + " " + interp[j][1] + " " + interp[j][2]);
                }
                prev = last;
            }

            int[] data = new int[w * h * 4];
            int index = 0;
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    //float distance = 0; //PApplet.dist(cx, cy, x + i, y + j);
                    //int which = PApplet.min((int) (distance * ACCURACY), interp.length-1);
                    float px = (x + i) - x1;
                    float py = (y + j) - y1;
                    // distance up the line is the dot product of the normalized
                    // vector of the gradient start/stop by the point being tested
                    int which = (int) ((px*nx + py*ny) * ACCURACY);
                    if (which < 0) which = 0;
                    if (which > interp.length-1) which = interp.length-1; 
                    //if (which > 138) System.out.println("grabbing " + which);
                    
                    data[index++] = interp[which][0];
                    data[index++] = interp[which][1];
                    data[index++] = interp[which][2];
                    data[index++] = 255;
                }
            }
            raster.setPixels(0, 0, w, h, data);

            return raster;
        }
    }
    
    
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 
    
    
    private class Group extends BaseObject {
        BaseObject[] objects;
        int objectCount;
        
        
        public Group(XMLElement graphics) {
            super(graphics);
            
            XMLElement elements[] = graphics.getChildren();
            objects = new BaseObject[elements.length];
            
            for (int i = 0; i < elements.length; i++){
                String name = elements[i].getName(); //getElement();
                XMLElement element = elements[i];
                
                if (name.equals("g")) {
                    objects[objectCount++] = new Group(element);
                    
                } else if (name.equals("line")) {
                    objects[objectCount++] = new Line(element);
                    
                } else if (name.equals("circle")) {
                    objects[objectCount++] = new Circle(element);
                    
                } else if (name.equals("ellipse")) {
                    objects[objectCount++] = new Ellipse(element);
                    
                } else if (name.equals("rect")) {
                    objects[objectCount++] = new Rect(element);
                    
                } else if (name.equals("polygon")) {
                    objects[objectCount++] = new Poly(element, true);
                    
                } else if (name.equals("polyline")) {
                    objects[objectCount++] = new Poly(element, false);
                    
                } else if (name.equals("path")) {
                    objects[objectCount++] = new Path(element);
                    
                } else if (name.equals("radialGradient")) {
                    objects[objectCount++] = new RadialGradient(element);
                    
                } else if (name.equals("linearGradient")) {
                    objects[objectCount++] = new LinearGradient(element);                    
                    
                } else {
                    PApplet.println("not handled " + name);
                }
            }
        }
        
        
        public void drawShape() {
            for (int i = 0; i < objectCount; i++) {
                objects[i].draw();
            }
        }
    }

    
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 
    
    
    abstract private class Gradient extends BaseObject {
        float[] offset;
        int[] color;
        int count;
        
        public Gradient(XMLElement properties) {
            super(properties);
            
            XMLElement elements[] = properties.getChildren();
            offset = new float[elements.length];
            color = new int[elements.length];
            
            // <stop  offset="0" style="stop-color:#967348"/>
            for (int i = 0; i < elements.length; i++){
                XMLElement element = elements[i];
                String name = element.getName();
                if (name.equals("stop")) {
                    offset[count] = element.getFloatAttribute("offset");
                    String farbe = element.getStringAttribute("style");
                    int idx = farbe.indexOf("#");
                    if (idx != -1) {
                        color[count] = Integer.parseInt(farbe.substring(idx+1), 16);
                        count++;
                    } else {
                        System.err.println("problem with gradient stop " + properties);
                    }
                }
            }
        }
        
        abstract protected void drawShape();
    }
    
    
    private class LinearGradient extends Gradient {
        float x1, y1, x2, y2;

        public LinearGradient(XMLElement properties) {
            super(properties);
            
            this.x1 = properties.getFloatAttribute("x1");
            this.y1 = properties.getFloatAttribute("y1");
            this.x2 = properties.getFloatAttribute("x2");
            this.y2 = properties.getFloatAttribute("y2");
        }

        protected void drawShape(){
        }
    }

    
    private class RadialGradient extends Gradient {
        float cx, cy, r;
        
        
        public RadialGradient(XMLElement properties) {            
            super(properties);
            
            this.cx = properties.getFloatAttribute("cx");
            this.cy = properties.getFloatAttribute("cy");
            this.r = properties.getFloatAttribute("r");
        }
        
        protected void drawShape() {
        }
    }    
    
    
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 
    
    
    private class Line extends VectorObject{

        float x1, y1, x2, y2;

        public Line(XMLElement properties){
            super(properties);
            this.x1 = properties.getFloatAttribute("x1");
            this.y1 = properties.getFloatAttribute("y1");
            this.x2 = properties.getFloatAttribute("x2");
            this.y2 = properties.getFloatAttribute("y2");
        }

        protected void drawShape(){
            parent.line(x1, y1, x2, y2);
        }
    }

    
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 

    
    private class Circle extends VectorObject{

        float x, y, radius;

        public Circle(XMLElement properties){
            super(properties);
            this.x = properties.getFloatAttribute("cx");
            this.y = properties.getFloatAttribute("cy");
            this.radius = properties.getFloatAttribute("r") * 2;
        }

        protected void drawShape(){
            parent.ellipseMode(PConstants.CENTER);
            parent.ellipse(x, y, radius, radius);
        }
    }

    
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 

    
    private class Ellipse extends VectorObject{

        float x, y, rx, ry;


        public Ellipse(XMLElement properties){
            super(properties);
            this.x = properties.getFloatAttribute("cx");
            this.y = properties.getFloatAttribute("cy");
            this.rx = properties.getFloatAttribute("rx") * 2;
            this.ry = properties.getFloatAttribute("ry") * 2; 
        }

        protected void drawShape(){
            parent.ellipseMode(PConstants.CENTER); 
            parent.ellipse(x, y, rx, ry);
        }
    }

    
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 

    
    private class Rect extends VectorObject{

        float x, y, w, h;

        public Rect(XMLElement properties){
            super(properties);
            this.x = properties.getFloatAttribute("x");
            this.y = properties.getFloatAttribute("y");
            this.w = properties.getFloatAttribute("width");
            this.h = properties.getFloatAttribute("height");
        }

        protected void drawShape(){
            parent.rectMode(PConstants.CORNER);
            parent.rect(x, y, w, h);
        }
    }

    
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 

    
    private class Poly extends VectorObject {

        float points[][] = null;
        /** true if polygon, false if polyline */
        boolean closed;

        public Poly(XMLElement properties, boolean closed){
            super(properties);
            String pointsBuffer[] = null;
            this.closed = closed;

            if (properties.hasAttribute("points")) { 
                //pointsBuffer = PApplet.split(properties.getStringAttribute("points"), ' ');
                pointsBuffer = PApplet.split(properties.getStringAttribute("points"));
            }
            
            points = new float[pointsBuffer.length][2];
            for (int i = 0; i < points.length; i++){
                String pb[] = PApplet.split(pointsBuffer[i], ',');
                points[i][0] = Float.valueOf(pb[0]).floatValue();
                points[i][1] = Float.valueOf(pb[1]).floatValue();
            }
        }

        protected void drawShape(){
            if (points != null)
                if (points.length > 0){
                    parent.beginShape();
                    for (int i = 0; i < points.length; i++){
                        parent.vertex(points[i][0], points[i][1]);
                    }
                    parent.endShape(closed ? PConstants.CLOSE : PConstants.OPEN);
                }
        }
    }

    
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 
    
    
    private class Path extends VectorObject {

        //Vector points = new Vector();
        boolean closed = false;
        
        int count = 0;
        float[] x = new float[4];
        float[] y = new float[4];
        //boolean[] bezier = new boolean[4];
        
        static final int MOVETO = 0;
        static final int LINETO = 1;
        static final int CURVETO = 2;
        int[] kind = new int[4];
        

        //Hang on! This is going to be meaty.   
        //Big and nasty constructor coming up....
        public Path(XMLElement properties){
            super(properties);
            String pathDataBuffer = "";

            if (!properties.hasAttribute("d"))
                return;

            pathDataBuffer = properties.getStringAttribute("d");
            StringBuffer pathChars = new StringBuffer();

            boolean lastSeparate = false;

            for (int i = 0; i < pathDataBuffer.length(); i++){
                char c = pathDataBuffer.charAt(i);
                boolean separate = false;

                if (c == 'M' || c == 'm' ||
                    c == 'L' || c == 'l' ||
                    c == 'H' || c == 'h' ||
                    c == 'V' || c == 'v' ||
                    c == 'C' || c == 'c' ||
                    c == 'S' || c == 's' ||
                    c == 'Z' || c == 'z' ||
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
                PApplet.split(pathDataBuffer, "|" + PConstants.WHITESPACE);
            //for (int j = 0; j < pathDataKeys.length; j++) {
            //    PApplet.println(j + "\t" + pathDataKeys[j]);
            //}
            //PApplet.println(pathDataKeys);
            //PApplet.println();

            //float cp[] = {0, 0};
            float cx = 0;
            float cy = 0;

            int i = 0;
            //for (int i = 0; i < pathDataKeys.length; i++){
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

                
                //C - curve to (absolute)
                case 'C': {
                    /*
                    float curvePA[] = {PApplet.parseFloat(pathDataKeys[i + 1]), PApplet.parseFloat(pathDataKeys[i + 2])};
                    float curvePB[] = {PApplet.parseFloat(pathDataKeys[i + 3]), PApplet.parseFloat(pathDataKeys[i + 4])};
                    float endP[] = {PApplet.parseFloat(pathDataKeys[i + 5]), PApplet.parseFloat(pathDataKeys[i + 6])};
                    cp[0] = endP[0];
                    cp[1] = endP[1];
                    i += 6;
                    points.add(curvePA);
                    points.add(curvePB);
                    points.add(endP);
                    */
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
                
                //c - curve to (relative)
                case 'c': {
                    /*
                    float curvePA[] = {cp[0] + PApplet.parseFloat(pathDataKeys[i + 1]), cp[1] + PApplet.parseFloat(pathDataKeys[i + 2])};
                    float curvePB[] = {cp[0] + PApplet.parseFloat(pathDataKeys[i + 3]), cp[1] + PApplet.parseFloat(pathDataKeys[i + 4])};
                    float endP[] = {cp[0] + PApplet.parseFloat(pathDataKeys[i + 5]), cp[1] + PApplet.parseFloat(pathDataKeys[i + 6])};
                    cp[0] = endP[0];
                    cp[1] = endP[1];
                    i += 6;
                    points.add(curvePA);
                    points.add(curvePB);
                    points.add(endP);
                    */
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
                
                //S - curve to shorthand (absolute)
                case 'S': {
                    /*
                    float lastPoint[] = (float[]) points.get(points.size() - 1);
                    float lastLastPoint[] = (float[]) points.get(points.size() - 2);
                    float curvePA[] = {cp[0] + (lastPoint[0] - lastLastPoint[0]), 
                                       cp[1] + (lastPoint[1] - lastLastPoint[1])};
                    float curvePB[] = {PApplet.parseFloat(pathDataKeys[i + 1]), 
                                       PApplet.parseFloat(pathDataKeys[i + 2])};
                    float e[] = {PApplet.parseFloat(pathDataKeys[i + 3]), PApplet.parseFloat(pathDataKeys[i + 4])};
                    cp[0] = e[0];
                    cp[1] = e[1];
                    points.add(curvePA);
                    points.add(curvePB);
                    points.add(e);
                    i += 4;
                    */
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
                
                //s - curve to shorthand (relative)
                case 's': {
                    /*
                    float lastPoint[] = (float[]) points.get(points.size() - 1);
                    float lastLastPoint[] = (float[]) points.get(points.size() - 2);
                    float curvePA[] = {cp[0] + (lastPoint[0] - lastLastPoint[0]), cp[1] + (lastPoint[1] - lastLastPoint[1])};
                    float curvePB[] = {cp[0] + PApplet.parseFloat(pathDataKeys[i + 1]), cp[1] + PApplet.parseFloat(pathDataKeys[i + 2])};
                    float e[] = {cp[0] + PApplet.parseFloat(pathDataKeys[i + 3]), cp[1] + PApplet.parseFloat(pathDataKeys[i + 4])};
                    cp[0] = e[0];
                    cp[1] = e[1];
                    points.add(curvePA);
                    points.add(curvePB);
                    points.add(e);
                    i += 4;
                    */
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
                
                case 'Z':
                case 'z':
                    closed = true;
                    i++;
                    break;
                    
                default:
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

        
        protected void drawShape(){
            parent.beginShape();
            /*
            float start[] = (float[]) points.get(0);
            parent.vertex(start[0], start[1]);
            for (int i = 1; i < points.size(); i += 3){
                float a[] = (float[]) points.get(i);
                float b[] = (float[]) points.get(i + 1);
                float e[] = (float[]) points.get(i + 2);
                parent.bezierVertex(a[0], a[1], b[0], b[1], e[0], e[1]);
            }
            */
            
            /*
            for (int i = 0; i < count; i++) {
                PApplet.println(i + "\t" + x[i] + "\t" + y[i] + "\t" + bezier[i]);
            }
            PApplet.println();
            */
            
            parent.vertex(x[0], y[0]);
            int i = 1;  // moveto has the first point
            while (i < count) {
                switch (kind[i]) {
                case MOVETO:
                    /*
                    //if (i != 0) {
                    parent.endShape(closed ? PConstants.CLOSE : PConstants.OPEN);
                    closed = false;
                    parent.beginShape();
                    //}
                     */
                    parent.breakShape();
                    parent.vertex(x[i], y[i]);
                    i++;
                    break;
                    
                case LINETO:
                    parent.vertex(x[i], y[i]);
                    i++;
                    break;
                    
                case CURVETO:
                    parent.bezierVertex(x[i], y[i], x[i+1], y[i+1], x[i+2], y[i+2]);
                    i += 3;
                    break;
                }
            }
            parent.endShape(closed ? PConstants.CLOSE : PConstants.OPEN);
            /*
            if (closed)
                parent.endShape(PConstants.CLOSE);
            else
                parent.beginShape();
            //	    p.endShape();
             */
        }
    }
}
