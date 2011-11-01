/** 
 * Synthesis 3: Motion and Arrays
 * Mr. Roboto by Leon Hong
 * p. 375
 *
 * The program loads a series images. They are assembled with the code
 * to create the robot and its motion. The position of the robot is controlled
 * with the mouse. Move the robot over the battery to turn on its lights.
 */


PImage batt, battalpha, batteat, batteatalpha, body, body2, head, head2, headalpha;
float mx = -100;
float my = -10;
float delay = 100.0;
float sinval;
float angle;
float speed = -.9;

void setup() {
  size(600, 600);
  batt = loadImage("batt.png");
  battalpha = loadImage("battalpha.gif");
  batt.mask(battalpha);
  batteat = loadImage("batteat.gif");
  batteatalpha = loadImage("batteatalpha.gif");
  batteat.mask(batteatalpha);
  body = loadImage("body.png");
  body2 = loadImage("body2.jpg");
  head = loadImage("head.png");
  head2 = loadImage("head2.jpg");
  headalpha = loadImage("headalpha.gif");
  head.mask(headalpha);
  head2.mask(headalpha);
  frameRate(30);
}

void draw() {
  noCursor();
  background (172);
  pushMatrix();
  interpolate();
  translate (mx-100,my-150);
  angle = angle + speed;
  sinval = sin(angle);
  if (abs(my-mouseY)>20 && abs(mx-mouseX)>5){
    image (body,0,135);
    image (head,0,sinval*20-40);
  }
  else{
    image (body2,0,135);
    image (head2,0,sinval*20-40);
  }
  interpolate();
  popMatrix();
  pushMatrix();
  if (abs(my-mouseY)>20 && abs(mx-mouseX)>5){
    battery(true);
  }
  else{
    battery (false);
  }
  popMatrix();
}

void battery(boolean test){
  if (test==true){
    image (batt, mouseX-50, mouseY-20);
  }
  else{
    image (batteat, mouseX-50+random(-20,20), mouseY-20+random(-20,20));
  }
}

void interpolate(){
  float diffx = mouseX-mx;
  if(abs(diffx) > 1) {
    mx = mx + diffx/delay;
  }
  float diffy = mouseY-my;
  if(abs(diffy) > 1) {
    my = my + diffy/delay;
  }
}

void keyPressed() {
  // saveFrame("robot-####.tif"); 
}
