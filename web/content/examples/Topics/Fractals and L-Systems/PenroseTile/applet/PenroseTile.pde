/** 
 * Penrose Tile L-System 
 * by Geraldine Sarmiento (NYU ITP).
 *  
 * This code was based on Patrick Dwyer's L-System class. 
 */

PenroseLSystem ds;

void setup() 
{
  size(640, 360);
  smooth();
  ds = new PenroseLSystem();
  ds.simulate(4);
}

void draw() 
{
  background(0);
  ds.render();
}






