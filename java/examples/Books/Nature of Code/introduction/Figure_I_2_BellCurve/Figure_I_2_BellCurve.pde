// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

float[] heights;

void setup() {
  size(400, 200);
  smooth();
}

void draw() {
  background(255);
  float e = 2.71828183;                       //"e", see http://mathforum.org/dr.math/faq/faq.e.html for more info
  float[] heights = new float[width];           //use an array to store all the "y" values
  float m = 0;                                 //default mean of 0 
  float sd = map(mouseX,0,width,0.4,2);     //standard deviation based on mouseX
  for (int i = 0; i < heights.length; i++) {
    float xcoord = map(i,0,width,-3,3);
    float sq2pi = sqrt(2*PI);                   //square root of 2 * PI
    float xmsq = -1*(xcoord-m)*(xcoord-m);      //-(x - mu)^2
    float sdsq = sd*sd;                         //variance (standard deviation squared)
    heights[i] = (1 / (sd * sq2pi)) * (pow(e, (xmsq/sdsq)));  //P(x) function
  }

  // a little for loop that draws a line between each point on the graph
  stroke(0);
  strokeWeight(2);
  noFill();
  beginShape();
  for (int i = 0; i < heights.length-1; i++) {
    float x = i; 
    float y = map(heights[i], 0, 1, height-2, 2);
    vertex(x, y);
  }
  endShape();
}

