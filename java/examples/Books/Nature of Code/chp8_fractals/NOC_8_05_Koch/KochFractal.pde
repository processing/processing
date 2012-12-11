// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Koch Curve
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
