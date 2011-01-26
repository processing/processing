
// Based on code 31-03 (p. 281)


Spot[] sps = new Spot[36];

void setup() {
  size(750, 2775);
  //size(200, 200);
  smooth();
  noStroke();
  ellipseMode(CENTER_RADIUS);
  randomSeed(0);
  for(int i=0; i<sps.length; i++) {
    sps[i] = new Spot();  
  }
}

void draw() {
  fill(255, 12);
  rect(0, 0, width, height);
  fill(0);
  for(int i=0; i<sps.length; i++) {
    sps[i].updateDisplay();  
  }
}

void mousePressed() {
  save("page_278.tif"); 
}


class Spot {

  float radius = 15.0;  // Radius of the circle
  float x = random(radius, width-radius);       // X-coordinate
  float y = random(radius, height-radius);       // Y-coordinate
  float speedX = random(0.4, 1.0);   // Speed of motion on the x-axis
  float speedY = random(0.4, 1.0);   // Speed of motion on the y-axis

  int directionX = 1;   // Direction of motion on the x-axis
  int directionY = -1;  // Direction of motion on the y-axis

  Spot() {
    float r = random(1.0);
    if (r < .25) {
      directionX = 1;
      directionY = 1;
    } 
    else if (r < .5) {
      directionX = -1;
      directionY = 1;    
    } 
    else if (r < .75) {
      directionX = 1;
      directionY = -1;    
    } 
    else {
      directionX = -1;
      directionY = -1;    
    }
  }


  void updateDisplay() {

    ellipse(x, y, radius, radius);
    x += speedX * directionX;
    if ((x > width-radius) || (x < radius)) {
      directionX = -directionX;  // Change direction
    }
    y += speedY * directionY;
    if ((y > height-radius) || (y < radius)) {
      directionY = -directionY;  // Change direction
    }
  }

}
