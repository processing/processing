// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// The old way to do intersection tests, look how slow!!

int totalThings = 2000;

ArrayList<Thing> a;            // ArrayList for all "things"

void setup() {
  size(640,360);
  a = new ArrayList<Thing>();  // Create the list

  // Put 2000 Things in the system
  for (int i = 0; i < totalThings; i++) {
    a.add(new Thing(random(width),random(height)));
  }

}

void draw() {
  background(0);
  fill(255);
  noStroke();
  // Run through the Grid
  stroke(255);
  for (Thing t : a) {
    t.highlight = false;
    for (Thing other : a) {
      // As long as its not the same one
      if (t != other) {
        // Check to see if they are touching
        // (We could do many other things here besides just intersection tests, such
        // as apply forces, etc.)
        float d = dist(t.x,t.y,other.x,other.y);
        if (d < t.r/2 + other.r/2) {
          t.highlight = true;
        }
      }
    }
  }

  // Display and move all Things
  for (Thing t : a) {
    t.render();
    t.move();
  }
  
  fill(0);
  rect(0,height-20,width,20);
  fill(255);
  text("Framerate: " + int(frameRate),10,height-6);
  
  

}
