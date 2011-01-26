/** 
 * Penrose Snowflake L-System 
 * by Geraldine Sarmiento (NYU ITP). 
 * 
 * This code was based on Patrick Dwyer's L-System class. 
 */

PenroseSnowflakeLSystem ps;

void setup() {
  size(640, 360);
  stroke(255);
  noFill();
  smooth();
  ps = new PenroseSnowflakeLSystem();
  ps.simulate(4);
}

void draw() {
  background(0);
  ps.render();
}


