
class ModuleA extends Module {

  ModuleA(int spriteNum, float xx, float yy, float deg, float rad, float pp) {
    super(spriteNum, xx, yy, deg, rad, pp);
  }
  
  void updateMe() {
    float mh = x - mouseX;
    float mv = y - mouseY;
    float mdif = sqrt(mh*mh+mv*mv);
    float dh = width/2 - mouseX;
    float dv = height/2 - mouseY;
    float ddif = sqrt(dh*dh+dv*dv);
    if(dir == 1){
      myAngle +=  abs(ddif - mdif)/50.0;
    }
    else{
      myAngle -=  abs(ddif - mdif)/50.0;
    }
    myRadius +=  mdif/100.00;
    if(myRadius > width){
      myRadius = random(10,40);
    }
    mx += (mouseX - mx)/delay;
    my += (mouseY - my)/delay;
    x = mx + (myRadius * cos(radians(myAngle)));
    y = my + (myRadius * sin(radians(myAngle)));
    stroke(num/(i+1), num/(i+1), num/(i+1));
    point(x,y);
  }

}
