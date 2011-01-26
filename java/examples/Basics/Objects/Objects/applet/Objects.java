import processing.core.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class Objects extends PApplet {

/**
 * Objects
 * by hbarragan. 
 * 
 * Move the cursor across the image to change the speed and positions
 * of the geometry. The class MRect defines a group of lines.
 */

MRect r1, r2, r3, r4;
 
public void setup()
{
  size(200, 200);
  fill(255, 204);
  noStroke();
  r1 = new MRect(1, 134.0f, 0.532f, 0.083f*height, 10.0f, 60.0f);
  r2 = new MRect(2, 44.0f, 0.166f, 0.332f*height, 5.0f, 50.0f);
  r3 = new MRect(2, 58.0f, 0.332f, 0.4482f*height, 10.0f, 35.0f);
  r4 = new MRect(1, 120.0f, 0.0498f, 0.913f*height, 15.0f, 60.0f);
}
 
public void draw()
{
  background(0);
  
  r1.display();
  r2.display();
  r3.display();
  r4.display();
 
  r1.move(mouseX-(width/2), mouseY+(height*0.1f), 30);
  r2.move((mouseX+(width*0.05f))%width, mouseY+(height*0.025f), 20);
  r3.move(mouseX/4, mouseY-(height*0.025f), 40);
  r4.move(mouseX-(width/2), (height-mouseY), 50);
}
 
class MRect 
{
  int w; // single bar width
  float xpos; // rect xposition
  float h; // rect height
  float ypos ; // rect yposition
  float d; // single bar distance
  float t; // number of bars
 
  MRect(int iw, float ixp, float ih, float iyp, float id, float it) {
    w = iw;
    xpos = ixp;
    h = ih;
    ypos = iyp;
    d = id;
    t = it;
  }
 
  public void move (float posX, float posY, float damping) {
    float dif = ypos - posY;
    if (abs(dif) > 1) {
      ypos -= dif/damping;
    }
    dif = xpos - posX;
    if (abs(dif) > 1) {
      xpos -= dif/damping;
    }
  }
 
  public void display() {
    for (int i=0; i<t; i++) {
      rect(xpos+(i*(d+w)), ypos, w, height*h);
    }
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "Objects" });
  }
}
