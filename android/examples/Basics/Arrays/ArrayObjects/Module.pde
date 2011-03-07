class Module {
  int mx, my;
  int big;
  float x, y;
  int xdir = 1;
  int ydir = 1;
  float speed; 
  
  // Contructor (required)
  Module(int imx, int imy, int ix, int iy, float ispeed) {
    mx = imx;
    my = imy;
    x = ix;
    y = iy;
    speed = ispeed;
    big = unit;
  }
  
  // Custom method for updating the variables
  void update() {
    x = x + (speed * xdir);
    if (x >= big || x <= 0) {
      xdir *= -1;
      x = x + (1 * xdir);
      y = y + (1 * ydir);
    }
    if (y >= big || y <= 0) {
      ydir *= -1;
      y = y + (1 * ydir);
    }
  }
  
  // Custom method for drawing the object
  void draw() {
    stroke(second() * 4);
    point(mx+x-1, my+y-1);
  }
}
