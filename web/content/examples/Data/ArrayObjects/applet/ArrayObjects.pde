// Array Objects
// by REAS <http://reas.com>

// Demonstrates the syntax for creating an array of custom objects.

// Updated 10 March 2003
// Created 20 October 2002

int unit = 40;
int num;
Module[] mods;

void setup() 
{
  size(200, 200);
  background(176);
  noStroke();
  framerate(60);
  
  num = width/unit * width/unit;
  mods = new Module[num];
  
  for (int i=0; i<height/unit; i++) {
    for(int j=0; j<height/unit; j++) {
      int index = i*height/unit + j;
      mods[index] = new Module(j*unit, i*unit, unit/2, unit/2, random(0.05, 0.8));  
    }
  }
}

void draw() 
{
  for(int i=0; i<num; i++) {
    mods[i].update();
    mods[i].draw();
  }
}

class Module {
  float mx, my;
  int size = unit;
  float xxx, yyy = 0;
  int xdir = 1;
  int ydir = 1;
  float speed; 
  
  // Contructor (required)
  Module(float imx, float imy, float ixxx, float iyyy, float ispeed) {
    mx = imy;
    my = imx;
    xxx = int(ixxx);
    yyy = int(iyyy);
    speed = ispeed;
  }
  
  // Custom method for updating the variables
  void update() {
    xxx = xxx + (speed * xdir);
    if (xxx >= size || xxx <= 0) {
      xdir *= -1;
      xxx = xxx + (1 * xdir);
      yyy = yyy + (1 * ydir);
    }
    if (yyy >= size || yyy <= 0) {
      ydir *= -1;
      yyy = yyy + (1 * ydir);
    }
  }
  
  // Custom method for drawing the object
  void draw() {
    stroke(second()*4);
    point(mx+xxx-1, my+yyy-1);
  }
}


