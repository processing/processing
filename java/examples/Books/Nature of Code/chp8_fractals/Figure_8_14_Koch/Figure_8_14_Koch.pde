// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Koch Curve

// Renders a simple fractal, the Koch snowflake
// Each recursive level drawn in sequence

ArrayList<KochLine> lines  ;   // A list to keep track of all the lines

void setup() {
  size(1820, 200);


  smooth();
}



void draw() {
  translate(10,0);
  int spacing = 10;
  int total = 5;

  background(255);
  float w = (1800-spacing*(total-1))/5;
  for (int n = 0; n < total; n++) {
    lines = new ArrayList<KochLine>();
    PVector start = new PVector(0, height*2/3);
    PVector end   = new PVector(w, height*2/3);
    lines.add(new KochLine(start, end));
    for (int i = 0; i < n; i++) {
      generate();
    }
    strokeWeight(2);
    for (KochLine l : lines) {
      l.display();
    }  
    noFill();
    strokeWeight(1);
    stroke(127);
    rect(0, 10, w,height-20);
    translate(w+spacing, 0);
  }
  save("chapter08_14.png");
  noLoop();
}

void generate() {
  ArrayList next = new ArrayList<KochLine>();    // Create emtpy list
  for (KochLine l : lines) {
    // Calculate 5 koch PVectors (done for us by the line object)
    PVector a = l.kochA();                 
    PVector b = l.kochB();
    PVector c = l.kochC();
    PVector d = l.kochD();
    PVector e = l.kochE();
    // Make line segments between all the PVectors and add them
    next.add(new KochLine(a, b));
    next.add(new KochLine(b, c));
    next.add(new KochLine(c, d));
    next.add(new KochLine(d, e));
  }
  lines = next;
}

