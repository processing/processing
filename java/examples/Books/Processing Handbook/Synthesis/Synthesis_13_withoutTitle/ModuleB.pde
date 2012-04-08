
class ModuleB extends Module {

  ModuleB(int spriteNum, float xx, float yy, float deg, float rad, float pp) {
    super(spriteNum, xx, yy, deg, rad, pp);    
  }

  void updateMe(){
    mx += (mouseX - mx)/delay;
    my += (mouseY - my)/delay;
    x = mx + (myRadius * cos(radians(myAngle)));
    y = my + (myRadius * sin(radians(myAngle)));
    stroke(num/2, num/2, num/2);
    point(x,y);

    // from connectMe2
    noStroke();
    fill(0, num/7.0, num/(i+1)+num/4.0, 20);
    beginShape(QUADS);
    vertex(modsA[i].x, modsA[i].y);
    vertex(modsA[i].x+1, modsA[i].y+1);
    vertex(x, y);
    vertex(x+1, y+1);
    endShape();
  }

}
