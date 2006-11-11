package processing.svg.reader;
//package candy;
import java.util.Vector;
import processing.core.PApplet;
import processing.core.PConstants;
import proxml.InvalidDocumentException;
import proxml.XMLElement;
import proxml.XMLInOut;

//CANDY
//Processing SVG import for the common man
//author:  Flux
//with contributions by: Christian Riekoff
//email:   flux.blackcat@gmail.com
//  http://www.ghost-hack.com
//Built for Processing 118

//Using Christian Riekoff's handy proXML parser
//  http://www.texone.org/proxml
//  Hawt.

//Last 100% working for Processing 0115
//Please send bugs and annoyances to above email
//Refer to readme.txt that was part of this package for legal jargon

public class SVG{

    public String filename;

    protected static PApplet p;

    private static XMLInOut xmlio;

    private int svgWidth = 0;

    private int svgHeight = 0;

    private Vector draw = new Vector();

    private XMLElement svgData;

    //For some reason Processing was using -1 for #FFFFFFFF
    //thus we will use our own null value
    private static int transValue = -255;

    private boolean styleOverride = false;

    /**
     * Initializes a new SVG Object with the given filename.
     * @param filename, String: filename of the object
     * @param parent, PApplet: instance of the parent application
     */
    public SVG(String filename, PApplet parent){
        //Hook into PApplet
        if(p == null){
            p = parent;
            xmlio = new XMLInOut(p);
        }

        this.filename = filename;

        //Load into XML parser
        XMLElement svgDocument;
        try{
            svgDocument = xmlio.loadElementFrom(filename);
        }catch (InvalidDocumentException ide){
            PApplet.println("XML: File does not exist");
            return;
        }

        //Get the xml child node we need
        XMLElement doc = svgDocument.getChild(0);
        XMLElement entSVG = doc.getChild(0);
        XMLElement svg = entSVG.getChild(1);
        //While we're doing that, save the width and height too
        //      svgWidth = svg.getIntAttribute("width");
        //svgHeight = svg.getIntAttribute("height");

        //Catch exception when SVG doesn't have a <g> tag
        XMLElement graphics;
        String nameOfFirstChild = svg.getChild(1).toString();
        if(nameOfFirstChild.equals("<g>"))
            graphics = svg.getChild(1);
        else
            graphics = svg;

        this.svgData = svg;

        //Print SVG on construction
        //Use this for debugging
        //svg.printElementTree(" .");

        //Store vector graphics into our draw-routine
        XMLElement elements[] = graphics.getChildren();

        for (int i = 0; i < elements.length; i++){
            String name = elements[i].getElement();
            XMLElement element = elements[i];
            if (name.equals("line"))
                draw.add(new Line(element));
            else if (name.equals("circle"))
                draw.add(new Circle(element));
            else if (name.equals("ellipse"))
                draw.add(new Ellipse(element));
            else if (name.equals("rect"))
                draw.add(new Rectangle(element));
            else if (name.equals("polygon"))
                draw.add(new Polygon(element));
            else if (name.equals("path"))
                draw.add(new Path(element));
        }
    }

    /**
     * Draws the SVG document.
     *
     */
    public void draw(){
        //Maybe it would be smart to save all changes that have to made to processing modes here
        //like
        int saveEllipseMode = p.g.ellipseMode;

        boolean stroke = p.g.stroke;
        int strokeColor = p.g.strokeColor;

        boolean fill = p.g.fill;
        int fillColor = p.g.fillColor;

        for (int i = 0; i < draw.size(); i++){
            VectorObject vo = (VectorObject) draw.get(i);
            vo.basicDraw();
        }

        //set back the changed modes
        p.g.ellipseMode = saveEllipseMode;
        p.g.stroke = stroke;
        p.g.fill = fill;

        p.g.strokeColor = strokeColor;
        p.g.fillColor = fillColor;
    }

    /**
     * Overrides SVG-set styles and uses PGraphics styles and colors.
     */
    public void customStyle(){
        styleOverride = true;
    }

    /**
     * Uses SVG's styles and colors.
     */
    public void SVGStyle(){
        styleOverride = false;
    }

    /**
     * Prints out the SVG document usefull for parsing
     */
    public void printSVG(){
        svgData.printElementTree(" .");
    }

    //Converts a string to a float
    private float valueOf(String s){
        return Float.valueOf(s).floatValue();
    }

    //Default vector graphics class from which all others will polymorph
    protected abstract class VectorObject{

        int strokeColor = transValue;
        int fillColor = transValue;
        float strokeWeight = 1;


        boolean hasTransform = false;
        float transformation[] = null;

        //Should we keep these here even when we don't have transforms?
        float rotation = 0;
        float translateX = 0;
        float translateY = 0;

        public VectorObject(XMLElement properties){
            getColors(properties);
            getTransformation(properties);
        }


        private void getTransformation(XMLElement properties){
            String transform = "";
            if (properties.hasAttribute("transform")){
                this.hasTransform = true;
                transform = properties.getAttribute("transform");
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
                    this.rotation = -p.acos(this.transformation[3]);
                if (t[0] > 0 && t[1] < 0 && t[2] > 0 && t[3] > 0)
                    this.rotation = p.asin(this.transformation[1]);
                if (t[0] < 0 && t[1] > 0 && t[2] < 0 && t[3] < 0)
                    this.rotation = p.acos(this.transformation[0]);
                if (t[0] > 0 && t[1] > 0 && t[2] < 0 && t[3] > 0)
                    this.rotation = p.acos(this.transformation[0]);
                this.translateX = this.transformation[4];
                this.translateY = this.transformation[5];
            }
        }

        private int colorFromString(String color, String opacity){
            if (!color.equals("none")){
                color = color.substring(1, 7);
                color = opacity + color;
                return PApplet.unhex(color);
            }else{
                return transValue;
            }
        }

        //We'll need color information like stroke, fill, opacity, stroke-weight
        protected void getColors(XMLElement properties){
            //Yes. This is hacky.
            String opacity = "FF";

            if (properties.hasAttribute("opacity")){
                int o = (int) (properties.getFloatAttribute("opacity") * 255);
                opacity = PApplet.hex(o);
                opacity = opacity.substring(6, 8);
            }
            //A value of -1 = noStroke() or noFill()
            if (properties.hasAttribute("stroke")){
                strokeColor = colorFromString(properties.getAttribute("stroke"),opacity);
            }

            if (properties.hasAttribute("fill")){
                fillColor = colorFromString(properties.getAttribute("fill"),opacity);
            }

            if (properties.hasAttribute("stroke-width")){
                strokeWeight = properties.getFloatAttribute("stroke-width");
            }
        }

        protected void setColors(){
            p.colorMode(PConstants.RGB, 255, 255, 255, 255);
            if (strokeColor != transValue)
                p.stroke(strokeColor);
            else
                p.noStroke();

            if (fillColor != transValue)
                p.fill(fillColor);
            else
                p.noFill();
            p.strokeWeight(strokeWeight);
        }

        protected abstract void draw();

        //might be more efficient for all subclasses
        protected void basicDraw(){
            if(!styleOverride)
                setColors();
            if (hasTransform){
                p.pushMatrix();
                p.translate(translateX, translateY);
                p.rotate(rotation);
            }

            draw();

            if (hasTransform){
                p.popMatrix();
            }
        }
    }

    private class Line extends VectorObject{

        float x1;

        float y1;

        float x2;

        float y2;

        public Line(XMLElement properties){
            super(properties);
            this.x1 = properties.getFloatAttribute("x1");
            this.y1 = properties.getFloatAttribute("y1");
            this.x2 = properties.getFloatAttribute("x2");
            this.y2 = properties.getFloatAttribute("y2");
        }

        protected void draw(){
            p.line(x1, y1, x2, y2);
        }
    }

    private class Circle extends VectorObject{

        float x;

        float y;

        float radius;

        public Circle(XMLElement properties){
            super(properties);
            this.x = properties.getFloatAttribute("cx");
            this.y = properties.getFloatAttribute("cy");
            this.radius = properties.getFloatAttribute("r") * 2;
        }

        protected void draw(){
            p.ellipseMode(PConstants.CENTER);
            p.ellipse(x, y, radius, radius);
        }
    }

    private class Ellipse extends VectorObject{

        float x;

        float y;

        float rx;

        float ry;


        public Ellipse(XMLElement properties){
            super(properties);
            this.x = properties.getFloatAttribute("cx");
            this.y = properties.getFloatAttribute("cy");
            this.rx = properties.getFloatAttribute("rx") * 2;
            this.ry = properties.getFloatAttribute("ry") * 2;
        }

        protected void draw(){
            p.ellipseMode(PConstants.CENTER);
            p.ellipse(x, y, rx, ry);
        }
    }

    private class Rectangle extends VectorObject{

        float x;

        float y;

        float w;

        float h;

        public Rectangle(XMLElement properties){
            super(properties);
            this.x = properties.getFloatAttribute("x");
            this.y = properties.getFloatAttribute("y");
            this.w = properties.getFloatAttribute("width");
            this.h = properties.getFloatAttribute("height");
        }

        protected void draw(){
            p.rectMode(PConstants.CORNER);
            p.rect(x, y, w, h);
        }
    }

    private class Polygon extends VectorObject{

        float points[][] = null;

        public Polygon(XMLElement properties){
            super(properties);
            String pointsBuffer[] = null;

            //Flux: if any spaces exist in the points list string after the last point
            //      we may have a problem

            if (properties.hasAttribute("points"))
                pointsBuffer = PApplet.split(properties.getAttribute("points"), ' ');
            points = new float[pointsBuffer.length][2];
            for (int i = 0; i < points.length; i++){
                String pb[] = PApplet.split(pointsBuffer[i], ',');
                points[i][0] = Float.valueOf(pb[0]).floatValue();
                points[i][1] = Float.valueOf(pb[1]).floatValue();
            }
        }

        protected void draw(){
            if (points != null)
                if (points.length > 0){
                    p.beginShape();
                    for (int i = 0; i < points.length; i++){
                        p.vertex(points[i][0], points[i][1]);
                    }
                    p.endShape(PConstants.CLOSE);
                }
        }
    }

    //Hang on! This is going to be meaty.
    //Big and nasty constructor coming up....
    private class Path extends VectorObject{

        Vector points = new Vector();

        boolean closed = false;

        public Path(XMLElement properties){
            super(properties);
            String pathDataBuffer = "";

            if (!properties.hasAttribute("d"))
                return;

            pathDataBuffer = properties.getAttribute("d");
            StringBuffer pathChars = new StringBuffer();

            boolean lastSeperate = false;

            for (int i = 0; i < pathDataBuffer.length(); i++){
                char c = pathDataBuffer.charAt(i);
                boolean seperate = false;

                if (
                    c == 'M' || c == 'm' ||
                    c == 'L' || c == 'l' ||
                    c == 'H' || c == 'h' ||
                    c == 'V' || c == 'v' ||
                    c == 'C' || c == 'c' ||
                    c == 'S' || c == 's' ||
                    c == ',' ||
                    c == 'Z' || c == 'z'
                    ){
                    seperate = true;
                    if (i != 0)
                        pathChars.append("|");
                }
                if (c == 'Z' || c == 'z')
                    seperate = false;
                if (c == '-' && !lastSeperate){
                    pathChars.append("|");
                }
                if (c != ',')
                    pathChars.append("" + pathDataBuffer.charAt(i));
                if (seperate && c != ',' && c != '-')
                    pathChars.append("|");
                lastSeperate = seperate;
            }

            pathDataBuffer = pathChars.toString();

            String pathDataKeys[] = PApplet.split(pathDataBuffer, '|');

            float cp[] = {0, 0};

            for (int i = 0; i < pathDataKeys.length; i++){
                char c = pathDataKeys[i].charAt(0);
                switch (c){
                    //M - move to (absolute)
                case 'M': {
                    cp[0] = valueOf(pathDataKeys[i + 1]);
                    cp[1] = valueOf(pathDataKeys[i + 2]);
                    float s[] = {cp[0], cp[1]};
                    i += 2;
                    points.add(s);
                }
                    break;
                    //m - move to (relative)
                case 'm': {
                    cp[0] = cp[0] + valueOf(pathDataKeys[i + 1]);
                    cp[1] = cp[1] + valueOf(pathDataKeys[i + 2]);
                    float s[] = {cp[0], cp[1]};
                    i += 2;
                    points.add(s);
                }
                    //C - curve to (absolute)
                case 'C': {
                    float curvePA[] = {valueOf(pathDataKeys[i + 1]), valueOf(pathDataKeys[i + 2])};
                    float curvePB[] = {valueOf(pathDataKeys[i + 3]), valueOf(pathDataKeys[i + 4])};
                    float endP[] = {valueOf(pathDataKeys[i + 5]), valueOf(pathDataKeys[i + 6])};
                    cp[0] = endP[0];
                    cp[1] = endP[1];
                    i += 6;
                    points.add(curvePA);
                    points.add(curvePB);
                    points.add(endP);
                }
                    break;
                    //c - curve to (relative)
                case 'c': {
                    float curvePA[] = {cp[0] + valueOf(pathDataKeys[i + 1]), cp[1] + valueOf(pathDataKeys[i + 2])};
                    float curvePB[] = {cp[0] + valueOf(pathDataKeys[i + 3]), cp[1] + valueOf(pathDataKeys[i + 4])};
                    float endP[] = {cp[0] + valueOf(pathDataKeys[i + 5]), cp[1] + valueOf(pathDataKeys[i + 6])};
                    cp[0] = endP[0];
                    cp[1] = endP[1];
                    i += 6;
                    points.add(curvePA);
                    points.add(curvePB);
                    points.add(endP);
                }
                    break;
                    //S - curve to shorthand (absolute)
                case 'S': {
                    float lastPoint[] = (float[]) points.get(points.size() - 1);
                    float lastLastPoint[] = (float[]) points.get(points.size() - 2);
                    float curvePA[] = {cp[0] + (lastPoint[0] - lastLastPoint[0]), cp[1] + (lastPoint[1] - lastLastPoint[1])};
                    float curvePB[] = {valueOf(pathDataKeys[i + 1]), valueOf(pathDataKeys[i + 2])};
                    float e[] = {valueOf(pathDataKeys[i + 3]), valueOf(pathDataKeys[i + 4])};
                    cp[0] = e[0];
                    cp[1] = e[1];
                    points.add(curvePA);
                    points.add(curvePB);
                    points.add(e);
                    i += 4;
                }
                    break;
                    //s - curve to shorthand (relative)
                case 's': {
                    float lastPoint[] = (float[]) points.get(points.size() - 1);
                    float lastLastPoint[] = (float[]) points.get(points.size() - 2);
                    float curvePA[] = {cp[0] + (lastPoint[0] - lastLastPoint[0]), cp[1] + (lastPoint[1] - lastLastPoint[1])};
                    float curvePB[] = {cp[0] + valueOf(pathDataKeys[i + 1]), cp[1] + valueOf(pathDataKeys[i + 2])};
                    float e[] = {cp[0] + valueOf(pathDataKeys[i + 3]), cp[1] + valueOf(pathDataKeys[i + 4])};
                    cp[0] = e[0];
                    cp[1] = e[1];
                    points.add(curvePA);
                    points.add(curvePB);
                    points.add(e);
                    i += 4;
                }
                    break;
                case 'Z':
                    closed = true;
                    break;
                case 'z':
                    closed = true;
                    break;
                }
            }
        }

        protected void draw(){
            p.beginShape();
            float start[] = (float[]) points.get(0);
            p.vertex(start[0], start[1]);
            for (int i = 1; i < points.size(); i += 3){
                float a[] = (float[]) points.get(i);
                float b[] = (float[]) points.get(i + 1);
                float e[] = (float[]) points.get(i + 2);
                p.bezierVertex(a[0], a[1], b[0], b[1], e[0], e[1]);
            }
            if (closed)
                p.endShape(PConstants.CLOSE);
            else
                p.beginShape();
            //      p.endShape();
        }

    }
    //--------------------end of class
}
