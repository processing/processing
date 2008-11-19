/**
 * Subtractive Color Wheel 
 * by Ira Greenberg. 
 * 
 * The primaries are red, yellow, and blue. The
 * secondaries are green, purple, and orange. The 
 * tertiaries are  yellow-orange, red-orange, red-purple, 
 * blue-purple, blue-green, and yellow-green.
 * 
 * Create a shade or tint of the 
 * subtractive color wheel using
 * SHADE or TINT parameters.
 */

int segs = 12;
int steps = 6;
float rotAdjust = radians(360.0/segs/2.0);
float radius = 95.0;
float segWidth = radius/steps;
float interval = TWO_PI/segs;
int SHADE = 0;
int TINT = 1;

void setup(){
  size(200, 200);
  background(127);
  smooth();
  ellipseMode(RADIUS);
  noStroke();
 // you can substitue TINT for SHADE argument
 createWheel(width/2, height/2, SHADE);
}

void createWheel(int x, int y, int valueShift){
  if (valueShift == SHADE){
    for (int j=0; j<steps; j++){
      color[]cols = { 
        color(255-(255/steps)*j, 255-(255/steps)*j, 0), 
        color(255-(255/steps)*j, (255/1.5)-((255/1.5)/steps)*j, 0), 
        color(255-(255/steps)*j, (255/2)-((255/2)/steps)*j, 0), 
        color(255-(255/steps)*j, (255/2.5)-((255/2.5)/steps)*j, 0), 
        color(255-(255/steps)*j, 0, 0), 
        color(255-(255/steps)*j, 0, (255/2)-((255/2)/steps)*j), 
        color(255-(255/steps)*j, 0, 255-(255/steps)*j), 
        color((255/2)-((255/2)/steps)*j, 0, 255-(255/steps)*j), 
        color(0, 0, 255-(255/steps)*j),
        color(0, 255-(255/steps)*j, (255/2.5)-((255/2.5)/steps)*j), 
        color(0, 255-(255/steps)*j, 0), 
        color((255/2)-((255/2)/steps)*j, 255-(255/steps)*j, 0) };
      for (int i=0; i< segs; i++){
        fill(cols[i]);
        arc(x, y, radius, radius, interval*i+rotAdjust, interval*(i+1)+rotAdjust);
      }
      radius -= segWidth;
    }
  } else  if (valueShift == TINT){
    for (int j=0; j<steps; j++){
      color[]cols = { 
        color((255/steps)*j, (255/steps)*j, 0), 
        color((255/steps)*j, ((255/1.5)/steps)*j, 0), 
        color((255/steps)*j, ((255/2)/steps)*j, 0), 
        color((255/steps)*j, ((255/2.5)/steps)*j, 0), 
        color((255/steps)*j, 0, 0), 
        color((255/steps)*j, 0, ((255/2)/steps)*j), 
        color((255/steps)*j, 0, (255/steps)*j), 
        color(((255/2)/steps)*j, 0, (255/steps)*j), 
        color(0, 0, (255/steps)*j),
        color(0, (255/steps)*j, ((255/2.5)/steps)*j), 
        color(0, (255/steps)*j, 0), 
        color(((255/2)/steps)*j, (255/steps)*j, 0) };
      for (int i=0; i< segs; i++){
        fill(cols[i]);
        arc(x, y, radius, radius, interval*i+rotAdjust, interval*(i+1)+rotAdjust);
      }
      radius -= segWidth;
    }
  } 
}

