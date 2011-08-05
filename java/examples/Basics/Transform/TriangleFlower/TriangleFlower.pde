/**
 * Triangle Flower 
 * by Ira Greenberg. 
 * 
 * Using rotate() and triangle() functions generate a pretty 
 * flower. Uncomment the line "// rotate(rot+=radians(spin));"
 * in the triBlur() function for a nice variation.
 */

PVector[] p = new PVector[3];
float shift = 0.2;
float fade = 0;
float fillCol = 0;
float rot = 0;
float spin = 0;

void setup() {
  size(640, 360);
  background(0);
  smooth();
  fade = 255.0 / (width*0.5 / shift);
  spin = 360.0 / (width*0.5 / shift);
  p[0] = new PVector(-width/2, height/2);
  p[1] = new PVector(width/2, height/2);
  p[2] = new PVector(0, -height/2);
  noStroke();
  translate(width/2, height/2);
  triBlur();
}

void triBlur() {
  fill(fillCol);
  fillCol += fade;
  rotate(spin);
  triangle(p[0].x += shift, p[0].y -= shift/2, 
           p[1].x -= shift, p[1].y -= shift/2, 
           p[2].x, p[2].y += shift); 
  if (p[0].x < 0) {
    // ecursive call
    triBlur();
  }
}

