/**
 * Legs class
 * By Ira Greenberg <br />
 * Processing for Flash Developers,
 * Friends of ED, 2009
 */
 
class Legs {
  // Instance properties with default values
  float x = 0, y = 0, z = 0, w = 150, ht = 125;
  color col = #77AA22;
  // Advanced properties
  float detailW = w/6.0;
  float detailHt = ht/8.0;
  float shoeBulge = detailHt*2.0;
  float legGap = w/7.0;

  // Dynamics properties
  float velocity = .02, stepL, stepR, stepRate = random(10, 50); 
  float speedX = 1.0, speedZ, spring, damping = .5, theta;

  // Default constructor
  Legs() {
  }

  // Standard constructor
  Legs(float x, float z, float w, float ht, color col) {
    this.x = x;
    this.z = z;
    this.w = w;
    this.ht = ht;
    this.col = col;
    fill(col);
    detailW = w/6.0;
    detailHt = ht/8.0;
    shoeBulge = detailHt*2.0;
    legGap = w/7.0;
    speedX = random(-speedX, speedX);
  }

  // Advanced constructor
  Legs(float x, float z, float w, float ht, color col, float detailW, 
        float detailHt, float shoeBulge, float legGap) {
    this.x = x;
    this.z = z;
    this.w = w;
    this.ht = ht;
    this.col = col;
    this.detailW = detailW;
    this.detailHt = detailHt;
    this.shoeBulge = shoeBulge;
    this.legGap = legGap;
    speedX = random(-speedX, speedX);
  }

  // Draw legs
  void create() {
    fill(col);
    float footWidth = (w - legGap)/2;
    beginShape();
    vertex(x - w/2, y - ht, z);
    vertex(x - w/2, y - ht + detailHt, z);
    vertex(x - w/2 + detailW, y - ht + detailHt, z);
    // left foot
    vertex(x - w/2 + detailW,  y + stepL, z);
    curveVertex(x - w/2 + detailW, y + stepL, z);
    curveVertex(x - w/2 + detailW, y + stepL, z);
    curveVertex(x - w/2 + detailW - shoeBulge,  y + detailHt/2 + stepL, z);
    curveVertex(x - w/2,  y + detailHt + stepL, z);
    curveVertex(x - w/2,  y + detailHt + stepL, z);
    vertex(x - w/2 + footWidth,  y + detailHt + stepL*.9, z);
    // end left foot
    vertex(x - w/2 + footWidth + legGap/2,  y - ht + detailHt, z);
    vertex(x - w/2 + footWidth + legGap/2,  y - ht + detailHt, z);
    // right foot
    vertex(x - w/2 + footWidth + legGap,  y + detailHt + stepR*.9, z);
    vertex(x + w/2,  y + detailHt + stepR, z);
    curveVertex(x + w/2,  y + detailHt + stepR, z);
    curveVertex(x + w/2,  y + detailHt + stepR, z);
    curveVertex(x + w/2 - detailW + shoeBulge,  y + detailHt/2 + stepR, z);
    curveVertex(x + w/2 - detailW,  y + stepR, z);
    vertex(x + w/2 - detailW,  y + stepR, z);
    // end right foot
    vertex(x + w/2 - detailW,  y - ht + detailHt, z);
    vertex(x + w/2,  y - ht + detailHt, z);
    vertex(x + w/2,  y - ht, z);
    endShape(CLOSE);
  }

  // Set advanced property values
  void setDetails(float detailW, float detailHt, float shoeBulge, float legGap) {
    this.detailW = detailW;
    this.detailHt = detailHt;
    this.shoeBulge = shoeBulge;
    this.legGap = legGap;
  }

  // Make the legs step
  void step(float stepRate) {
    this.stepRate = stepRate;
    spring = ht/2.0;
    stepL = sin(theta)*spring;
    stepR = cos(theta)*spring;
    theta += radians(stepRate);
  }
  
  // Alternative overloaded step method
  void step() {
    spring = ht/2.0;
    stepL = sin(theta)*spring;
    stepR = cos(theta)*spring;
    theta += radians(stepRate);
  }


  // Moves legs along x, y, z axes
  void move() {
    // Move legs along y-axis
    y = stepR*damping;

    // Move legs along x-axis and
    // check for collision against frame edge
    x += speedX;
    if (screenX(x, y, z) > width) {
      speedX *= -1;
    }
    if (screenX(x, y, z) < 0) {
      speedX *= -1;
    }

    // Move legs along z-axis based on speed of stepping 
    // and check for collision against extremes
    speedZ = (stepRate*velocity);
    z += speedZ;
    if (z > 400) {
      z = 400;
      velocity *= -1;
    }
    if (z < -100) {
      z = -100;
      velocity *= -1;
    }
  }
  
  void setDynamics(float speedX, float spring, float damping) {
    this.speedX = speedX;
    this.spring = spring;
    this.damping = damping;
  }
}












