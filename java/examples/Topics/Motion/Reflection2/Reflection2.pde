/**
 * Non-orthogonal Collision with Multiple Ground Segments 
 * by Ira Greenberg. 
 * 
 * Based on Keith Peter's Solution in
 * Foundation Actionscript Animation: Making Things Move!
 */

Orb orb;

PVector gravity = new PVector(0,0.05);
int segments = 40;
Ground[] ground = new Ground[segments];
float[] peakHeights = new float[segments+1];

void setup(){
  size(640, 360);
  orb = new Orb(50, 50, 3);


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
  
  orb.move();
  orb.display();
  orb.checkWallCollision();
  
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


  for (int i=0; i<segments; i++){
    orb.checkGroundCollision(ground[i]);
  }
}








