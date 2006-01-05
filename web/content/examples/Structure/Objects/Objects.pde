// Objects
// By hbarragan

// Move the cursor across the image to change the speed and positions
// of the geometry. The class MRect defines a group of lines.

// Created 11 October 2002


MRect r1, r2, r3, r4;
 
void setup()
{
  size(200, 200);
  fill(255);
  noStroke();
  r1 = new MRect(1, 134.0, 0.532, 0.083*height, 10.0, 60.0);
  r2 = new MRect(2, 44.0, 0.166, 0.332*height, 5.0, 50.0);
  r3 = new MRect(2, 58.0, 0.332, 0.4482*height, 10.0, 35.0);
  r4 = new MRect(1, 120.0, 0.0498, 0.913*height, 15.0, 60.0);
}
 
void draw()
{
  background(0);
  
  r1.display();
  r2.display();
  r3.display();
  r4.display();
 
  r1.moveToX(mouseX-(width/2), 30);
  r2.moveToX((mouseX+(width*0.05))%width, 20);
  r3.moveToX(mouseX/4, 40);
  r4.moveToX(mouseX-(width/2), 50);
 
  r1.moveToY(mouseY+(height*0.1), 30);
  r2.moveToY(mouseY+(height*0.025), 20);
  r3.moveToY(mouseY-(height*0.025), 40);
  r4.moveToY((height-mouseY), 50);
}
 
class MRect 
{
  int w; // single bar width
  float xpos; // rect xposition
  float h; // rect height
  float ypos ; // rect yposition
  float d; // single bar distance
  float t; // number of bars
  float side; // rect width
 
  MRect(int iw, float ixp, float ih, float iyp, float id, float it) {
    w = iw;
    xpos = ixp;
    h = ih;
    ypos = iyp;
    d = id;
    t = it;
  }
 
  void moveToY (float posY, float damping) {
    float dif = ypos - posY;
    if (abs(dif) > 1) {
      ypos -= dif/damping;
    }
  }
 
  void moveToX (float posX, float damping) {
    float dif = xpos - posX;
    if (abs(dif) > 1) {
      xpos -= dif/damping;
    }
  }
 
  void display() {
    for (int i = 0; i<t; i++) {
      rect(xpos+(i*d*w), ypos, w, height*h);
    }
  }
}
