// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

void setup() {
  size(200,200);
  background(0);
}

void draw() {
  //create an alpha blended background
  fill(0,1);
  rect(0,0,width,height);

  //get 3 gaussian random numbers w/ mean of 0 and standard deviation of 1.0
  float r = randomGaussian();
  float g = randomGaussian();
  float b = randomGaussian();

  //define standard deviation and mean
  float sd = 100; float mean = 100;
  //scale by standard deviation and mean
  //also constrain to between (0,255) since we are dealing with color
  r = constrain((r * sd) + mean,0,255);

  //repeat for g & b
  sd = 20; mean = 200;
  g = constrain((g * sd) + mean,0,255);
  sd = 50; mean = 0;
  b = constrain((b * sd) + mean,0,255);

  //get more gaussian numbers, this time for location
  float xloc = randomGaussian();
  float yloc = randomGaussian();
  sd = width/10;
  mean = width/2;
  xloc = ( xloc * sd ) + mean;
  yloc = ( yloc * sd ) + mean;

  //draw an ellipse with gaussian generated color and location
  noStroke();
  fill(r,g,b);
  ellipse(xloc,yloc,8,8);
}
