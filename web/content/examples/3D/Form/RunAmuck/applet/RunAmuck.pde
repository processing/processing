/**
 * Run-Amuck
 * By Ira Greenberg <br />
 * Processing for Flash Developers,
 * Friends of ED, 2009
 */

int count = 250;
Legs[] legs = new Legs[count];

void setup() {
  size(640, 360, P3D);
  noStroke();
  for (int i = 0; i < legs.length; i++) {
    legs[i] = new Legs(random(-10, 10), random(-50, 150), random(.5, 5), 
                       random(.5, 5), color(random(255), random(255), random(255)));
  }
}

void draw() {
  background(0);
  translate(width/2, height/2);
  noStroke();
  fill(35);

  // Draw ground plane
  beginShape();
  vertex(-width*2, 0, -1000);
  vertex(width*2, 0, -1000);
  vertex(width/2, height/2, 400);
  vertex(-width/2, height/2, 400);
  endShape(CLOSE);

  // Update and draw the legs
  for (int i = 0; i < legs.length; i++) {
    legs[i].create();
    // Set foot step rate
    legs[i].step(random(10, 50));
    // Move legs along x, y, z axes
    // z-movement dependent upon step rate
    legs[i].move();
  }
}


