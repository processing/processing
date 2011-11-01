/**
 * Composite Objects
 * 
 * An object can include several other objects. Creating such composite objects 
 * is a good way to use the principles of modularity and build higher levels of 
 * abstraction within a program.
 */

EggRing er1, er2;


void setup() {
  size(200, 200);
  smooth();
  er1 = new EggRing(66, 132, 0.1, 66);
  er2 = new EggRing(132, 180, 0.05, 132);
}


void draw() {
  background(0);
  er1.transmit();
  er2.transmit();
}
