
class Module {
  int i;
  float x, y, myAngle, myRadius, dir;
  float mx = width/2;
  float my = height/2;
  float delay = 40.0;
  
  Module(int spriteNum, float xx, float yy, float deg, float rad, float pp) {
    i = spriteNum;
    x = xx;
    y = yy;
    myAngle = deg;
    myRadius = rad;
    dir = pp;
  }
  
}
