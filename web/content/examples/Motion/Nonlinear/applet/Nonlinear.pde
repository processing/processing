// Nonlinear
// by REAS <http://reas.com>

// Each object moves according to a different simple equation.
// These are the same equations that were drawn in the "SimpleCurves" example.
// Once again, each value put into the equation is between 0 and 1 and 
// then scaled to fill the screen.

// Updated 1 September 2002


int size = 8;
int legth = 50;

int aSize;           // Height of the shape
float aPos;          // Position of shape
float aSpeed;        // Speed of the shape
int aDirection = 1;  // Left or Right

float bPos; 
float bSpeed;
int bDirection = 1;

float cPos;
float cSpeed;
int cDirection = 1;

float dPos;
float dSpeed;
int dDirection = 1;


void setup() 
{
  size(200, 200);
  noStroke();
  framerate(60);
  aPos = bPos = cPos = dPos = width/2;
}

void draw() 
{
  background(102);
  fill(255);

  aSpeed = squared(aPos/float(width-size)) * 2.0;
  aPos = aPos + ((aSpeed*2.0+0.5) * aDirection );
  if (aPos > width-size || aPos < 0) { aDirection = aDirection * -1; }
  rect(aPos, 0, size, 50);
  
  bSpeed = quad(bPos/float(width-size)) * 2.0;
  bPos = bPos + ((bSpeed*2.0+0.5) * bDirection );
  if (bPos > width-size || bPos < 0) { bDirection = bDirection * -1; }
  rect(bPos, 50, size, 50);
  
  cSpeed = hump(cPos/float(width-size)) * 2.0;
  cPos = cPos + ((cSpeed*2.0+0.5) * cDirection );
  if (cPos > width-size || cPos < 0) { cDirection = cDirection * -1; }
  rect(cPos, 100, size, 50);
  
  dSpeed = quadHump(dPos/float(width-size)) * 2.0;
  dPos = dPos + ((dSpeed*2.0+0.5) * dDirection );
  if (dPos > width-size || dPos < 0) { dDirection = dDirection * -1; }
  rect(dPos, 150, size, 50);
  
}

float quad(float sa) {
  return sa*sa*sa*sa;
}

float quadHump(float sa) {
  sa = (sa - 0.5); //scale from -2 to 2
  sa = sa*sa*sa*sa * 16;
  return sa;
}

float hump(float sa) {
  sa = (sa - 0.5) * 2; //scale from -2 to 2
  sa = sa*sa;
  if(sa > 1) { sa = 1; }
  return 1-sa;
}

float squared(float sa) {
  sa = sa*sa;
  return sa;
}
