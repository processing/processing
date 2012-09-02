// Koch Curve
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code, Chapter 8

// Renders a simple fractal, the Koch snowflake
// Each recursive level drawn in sequence

KochFractal k;

void setup() {
  size(800,250);
  background(255);
  //frameRate(1);  // Animate slowly
  k = new KochFractal();
  smooth();
}

void draw() {
  background(255);
  // Draws the snowflake!
  k.render();
  // Iterate
  k.nextLevel();
  // Let's not do it more than 5 times. . .
  if (k.getCount() > 5) {
    k.restart();
  }
  println(frameCount);
}

// Koch Curve
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code, Chapter 8

// A class to manage the list of line segments in the snowflake pattern

class KochFractal {
  PVector start;       // A PVector for the start
  PVector end;         // A PVector for the end
  ArrayList<KochLine> lines;   // A list to keep track of all the lines
  int count;
  
  public KochFractal() {
    start = new PVector(0,height-20);
    end = new PVector(width,height-20);
    lines = new ArrayList<KochLine>();
    restart();
  }

  void nextLevel() {  
    // For every line that is in the arraylist
    // create 4 more lines in a new arraylist
    lines = iterate(lines);
    count++;
  }

  void restart() { 
    count = 0;      // Reset count
    lines.clear();  // Empty the array list
    lines.add(new KochLine(start,end));  // Add the initial line (from one end PVector to the other)
  }
  
  int getCount() {
    return count;
  }
  
  // This is easy, just draw all the lines
  void render() {
    for(KochLine l : lines) {
      l.display();
    }
  }

  // This is where the **MAGIC** happens
  // Step 1: Create an empty arraylist
  // Step 2: For every line currently in the arraylist
  //   - calculate 4 line segments based on Koch algorithm
  //   - add all 4 line segments into the new arraylist
  // Step 3: Return the new arraylist and it becomes the list of line segments for the structure
  
  // As we do this over and over again, each line gets broken into 4 lines, which gets broken into 4 lines, and so on. . . 
  ArrayList iterate(ArrayList<KochLine> before) {
    ArrayList now = new ArrayList<KochLine>();    // Create emtpy list
    for(KochLine l : before) {
      // Calculate 5 koch PVectors (done for us by the line object)
      PVector a = l.start();                 
      PVector b = l.kochleft();
      PVector c = l.kochmiddle();
      PVector d = l.kochright();
      PVector e = l.end();
      // Make line segments between all the PVectors and add them
      now.add(new KochLine(a,b));
      now.add(new KochLine(b,c));
      now.add(new KochLine(c,d));
      now.add(new KochLine(d,e));
    }
    return now;
  }

}
// Koch Curve
// Daniel Shiffman <http://www.shiffman.net>
// Nature of Code, Chapter 8

// A class to describe one line segment in the fractal
// Includes methods to calculate midPVectors along the line according to the Koch algorithm

class KochLine {

  // Two PVectors,
  // a is the "left" PVector and 
  // b is the "right PVector
  PVector a;
  PVector b;

  KochLine(PVector start, PVector end) {
    a = start.get();
    b = end.get();
  }

  void display() {
    stroke(0);
    line(a.x, a.y, b.x, b.y);
  }

  PVector start() {
    return a.get();
  }

  PVector end() {
    return b.get();
  }

  // This is easy, just 1/3 of the way
  PVector kochleft() {
    PVector v = PVector.sub(b, a);
    v.div(3);
    v.add(a);
    return v;
  }    

  // More complicated, have to use a little trig to figure out where this PVector is!
  PVector kochmiddle() {
    PVector v = PVector.sub(b, a);
    v.div(3);
    
    PVector p = a.get();
    p.add(v);
    
    rotate(v,-radians(60));
    p.add(v);
    
    return p;
  }    


  // Easy, just 2/3 of the way
  PVector kochright() {
    PVector v = PVector.sub(a, b);
    v.div(3);
    v.add(b);
    return v;
  }
}

  public void rotate(PVector v, float theta) {
    float xTemp = v.x;
    // Might need to check for rounding errors like with angleBetween function?
    v.x = v.x*cos(theta) - v.y*sin(theta);
    v.y = xTemp*sin(theta) + v.y*cos(theta);
  }



