/**
 * Non-orthogonal Collision with Multiple Ground Segments 
 * by Ira Greenberg. 
 * 
 * Based on Keith Peter's Solution in
 * Foundation Actionscript Animation: Making Things Move!
 */

Orb orb;
PVector velocity;
float gravity = .05, damping = 0.8;
int segments = 40;
Ground[] ground = new Ground[segments];
float[] peakHeights = new float[segments+1];

void setup(){
  size(640, 200);
  smooth();
  orb = new Orb(50, 50, 3);
  velocity = new PVector(.5, 0);

  // Calculate ground peak heights 
  for (int i=0; i<peakHeights.length; i++){
    peakHeights[i] = random(height-40, height-30);
  }

  /* Float value required for segment width (segs)
   calculations so the ground spans the entire 
   display window, regardless of segment number. */
  float segs = segments;
  for (int i=0; i<segments; i++){
    ground[i]  = new Ground(width/segs*i, peakHeights[i],
    width/segs*(i+1), peakHeights[i+1]);
  }
}


void draw(){
  // Background
  noStroke();
  fill(0, 15);
  rect(0, 0, width, height);

  // Move orb
  orb.x += velocity.x;
  velocity.y += gravity;
  orb.y += velocity.y;

  // Draw ground
  fill(127);
  beginShape();
  for (int i=0; i<segments; i++){
    vertex(ground[i].x1, ground[i].y1);
    vertex(ground[i].x2, ground[i].y2);
  }
  vertex(ground[segments-1].x2, height);
  vertex(ground[0].x1, height);
  endShape(CLOSE);

  // Draw orb
  noStroke();
  fill(200);
  ellipse(orb.x, orb.y, orb.r*2, orb.r*2);

  // Collision detection
  checkWallCollision();
  for (int i=0; i<segments; i++){
    checkGroundCollision(ground[i]);
  }
}


void checkWallCollision(){
  if (orb.x > width-orb.r){
    orb.x = width-orb.r;
    velocity.x *= -1;
    velocity.x *= damping;
  } 
  else if (orb.x < orb.r){
    orb.x = orb.r;
    velocity.x *= -1;
    velocity.x *= damping;
  }
}


void checkGroundCollision(Ground groundSegment) {

  // Get difference between orb and ground
  float deltaX = orb.x - groundSegment.x;
  float deltaY = orb.y - groundSegment.y;

  // Precalculate trig values
  float cosine = cos(groundSegment.rot);
  float sine = sin(groundSegment.rot);

  /* Rotate ground and velocity to allow 
   orthogonal collision calculations */
  float groundXTemp = cosine * deltaX + sine * deltaY;
  float groundYTemp = cosine * deltaY - sine * deltaX;
  float velocityXTemp = cosine * velocity.x + sine * velocity.y;
  float velocityYTemp = cosine * velocity.y - sine * velocity.x;

  /* Ground collision - check for surface 
   collision and also that orb is within 
   left/rights bounds of ground segment */
  if (groundYTemp > -orb.r &&
    orb.x > groundSegment.x1 &&
    orb.x < groundSegment.x2 ){
    // keep orb from going into ground
    groundYTemp = -orb.r;
    // bounce and slow down orb
    velocityYTemp *= -1.0;
    velocityYTemp *= damping;
  }

  // Reset ground, velocity and orb
  deltaX = cosine * groundXTemp - sine * groundYTemp;
  deltaY = cosine * groundYTemp + sine * groundXTemp;
  velocity.x = cosine * velocityXTemp - sine * velocityYTemp;
  velocity.y = cosine * velocityYTemp + sine * velocityXTemp;
  orb.x = groundSegment.x + deltaX;
  orb.y = groundSegment.y + deltaY;
}




