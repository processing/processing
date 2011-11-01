/**
 * Spring. 
 * 
 * Click, drag, and release the horizontal bar to start the spring. 
 */
 
// Spring drawing constants for top bar
int springHeight = 32;  // Height
int left;               // Left position
int right;              // Right position
int max = 200;          // Maximum Y value
int min = 100;          // Minimum Y value
boolean over = false;   // If mouse over
boolean move = false;   // If mouse down and over

// Spring simulation constants
float M = 0.8;   // Mass
float K = 0.2;   // Spring constant
float D = 0.92;  // Damping
float R = 150;   // Rest position

// Spring simulation variables
float ps = R;    // Position
float vs = 0.0;  // Velocity
float as = 0;    // Acceleration
float f = 0;     // Force


void setup() {
  size(640, 360);
  rectMode(CORNERS);
  noStroke();
  left = width/2 - 100;
  right = width/2 + 100;
}

void draw() {
  background(102);
  updateSpring();
  drawSpring();
}

void drawSpring() {
  
  // Draw base
  fill(0.2);
  float baseWidth = 0.5 * ps + -8;
  rect(width/2 - baseWidth, ps + springHeight, width/2 + baseWidth, height);

  // Set color and draw top bar
  if(over || move) { 
    fill(255);
  } else { 
    fill(204);
  }
  rect(left, ps, right, ps + springHeight);
}


void updateSpring() {
  // Update the spring position
  if(!move) {
    f = -K * (ps - R);    // f=-ky
    as = f / M;           // Set the acceleration, f=ma == a=f/m
    vs = D * (vs + as);   // Set the velocity
    ps = ps + vs;         // Updated position
  }
  if(abs(vs) < 0.1) {
    vs = 0.0;
  }

  // Test if mouse is over the top bar
  if(mouseX > left && mouseX < right && mouseY > ps && mouseY < ps + springHeight) {
    over = true;
  } else {
    over = false;
  }
  
  // Set and constrain the position of top bar
  if(move) {
    ps = mouseY - springHeight/2;
    ps = constrain(ps, min, max);
  }
}

void mousePressed() {
  if(over) {
    move = true;
  }
}

void mouseReleased() {
  move = false;
}
