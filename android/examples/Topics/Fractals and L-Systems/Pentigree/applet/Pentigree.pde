/** 
 * Pentigree L-System 
 * by Geraldine Sarmiento (NYU ITP). 
 * 
 * This code was based on Patrick Dwyer's L-System class. 
 */


PentigreeLSystem ps;

void setup() {
  size(640, 360);
  smooth();
  ps = new PentigreeLSystem();
  ps.simulate(3);
}

void draw() {
  background(0);
  ps.render();
}


