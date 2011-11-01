/**
 * Scribble Plotter  
 * by Ira Greenberg. 
 * 
 * Using 2-dimensional arrays, record end points
 * and replot scribbles between points. 
 */

int SCRIBBLE = 0;
int HATCHING = 1;

void setup(){
  size(640, 360);
  background(0);

  // Create arrays to hold x, y coords
  float[]x = new float[4];
  float[]y  = new float[4];
  // create a convenient 2-dimensional 
  // array to hold x, y arrays
  float[][]xy = {x, y};

  // Record points
  // X positions     
  xy[0][0] = 125;
  xy[0][1] = 475;
  xy[0][2] = 475;
  xy[0][3] = 125;

  // Y positions
  xy[1][0] = 100;
  xy[1][1] = 100;
  xy[1][2] = 260;
  xy[1][3] = 260;

  // Call plotting function
  makeRect(xy);
}

void makeRect(float[][]pts){
  stroke(255);
  smooth();
  
  // Scribble variables, that get passed as arguments to the scribble function
  int steps = 100;
  float scribVal = 3.0;
  for (int i = 0; i < pts[0].length; i++){
    // Plots vertices
    strokeWeight(5);
    point(pts[0][i], pts[1][i]);

    // Call scribble function
    strokeWeight(.5);
    if (i > 0){ 
      scribble(pts[0][i], pts[1][i], pts[0][i-1], pts[1][i-1], steps, scribVal, SCRIBBLE);
    } 
    if (i == pts[0].length-1){
    // Show some hatching between last 2 points
      scribble(pts[0][i], pts[1][i], pts[0][0], pts[1][0], steps, scribVal*2, HATCHING);
    }
  }
}

/* 
  Scribble function plots lines between end points, 
  determined by steps and scribVal arguments.
  two styles are available: SCRIBBLE and HATCHING, which
  are interestingly only dependent on parentheses
  placement in the line() function calls.
*/

void scribble(float x1, float y1, float x2, float y2, int steps, float scribVal, int style){

  float xStep = (x2-x1)/steps;
  float yStep = (y2-y1)/steps;
  for (int i = 0; i < steps; i++){
    if(style == SCRIBBLE){
      if (i < steps-1){
        line(x1, y1, x1+=xStep+random(-scribVal, scribVal), y1+=yStep+random(-scribVal, scribVal));
      } 
      else {
        // extra line needed to attach line back to point- not necessary in HATCHING style
        line(x1, y1, x2, y2);
      }
    }
    else if (style == HATCHING){
       line(x1, y1, (x1+=xStep)+random(-scribVal, scribVal), (y1+=yStep)+random(-scribVal, scribVal));
    }
  }
}

