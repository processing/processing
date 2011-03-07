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

public class Buttons extends PApplet {

/**
 * Buttons. 
 * 
 * Click on one of the shapes to change
 * the background color. This example
 * demonstates a class for buttons.
 */

int currentcolor;

CircleButton circle1, circle2, circle3;
RectButton rect1, rect2;

boolean locked = false;

public void setup()
{
  size(200, 200);
  smooth();

  int baseColor = color(102);
  currentcolor = baseColor;

  // Define and create circle button
  int buttoncolor = color(204);
  int highlight = color(153);
  ellipseMode(CENTER);
  circle1 = new CircleButton(30, 100, 100, buttoncolor, highlight);

  // Define and create circle button
  buttoncolor = color(204);
  highlight = color(153); 
  circle2 = new CircleButton(130, 110, 24, buttoncolor, highlight);

  // Define and create circle button
  buttoncolor = color(153);
  highlight = color(102); 
  circle3 = new CircleButton(130, 140, 24, buttoncolor, highlight);

  // Define and create rectangle button
  buttoncolor = color(102);
  highlight = color(51); 
  rect1 = new RectButton(150, 20, 100, buttoncolor, highlight);

  // Define and create rectangle button
  buttoncolor = color(51);
  highlight = color(0); 
  rect2 = new RectButton(90, 20, 50, buttoncolor, highlight);
}

public void draw()
{
  background(currentcolor);
  stroke(255);
  update(mouseX, mouseY);
  circle1.display();
  circle2.display();
  circle3.display();
  rect1.display();
  rect2.display();
}

public void update(int x, int y)
{
  if(locked == false) {
    circle1.update();
    circle2.update();
    circle3.update();
    rect1.update();
    rect2.update();
  } 
  else {
    locked = false;
  }

  if(mousePressed) {
    if(circle1.pressed()) {
      currentcolor = circle1.basecolor;
    } 
    else if(circle2.pressed()) {
      currentcolor = circle2.basecolor;
    } 
    else if(circle3.pressed()) {
      currentcolor = circle3.basecolor;
    } 
    else if(rect1.pressed()) {
      currentcolor = rect1.basecolor;
    } 
    else if(rect2.pressed()) {
      currentcolor = rect2.basecolor;
    }
  }
}


class Button
{
  int x, y;
  int size;
  int basecolor, highlightcolor;
  int currentcolor;
  boolean over = false;
  boolean pressed = false;   

  public void update() 
  {
    if(over()) {
      currentcolor = highlightcolor;
    } 
    else {
      currentcolor = basecolor;
    }
  }

  public boolean pressed() 
  {
    if(over) {
      locked = true;
      return true;
    } 
    else {
      locked = false;
      return false;
    }    
  }

  public boolean over() 
  { 
    return true; 
  }

  public boolean overRect(int x, int y, int width, int height) 
  {
    if (mouseX >= x && mouseX <= x+width && 
      mouseY >= y && mouseY <= y+height) {
      return true;
    } 
    else {
      return false;
    }
  }

  public boolean overCircle(int x, int y, int diameter) 
  {
    float disX = x - mouseX;
    float disY = y - mouseY;
    if(sqrt(sq(disX) + sq(disY)) < diameter/2 ) {
      return true;
    } 
    else {
      return false;
    }
  }

}

class CircleButton extends Button
{ 
  CircleButton(int ix, int iy, int isize, int icolor, int ihighlight) 
  {
    x = ix;
    y = iy;
    size = isize;
    basecolor = icolor;
    highlightcolor = ihighlight;
    currentcolor = basecolor;
  }

  public boolean over() 
  {
    if( overCircle(x, y, size) ) {
      over = true;
      return true;
    } 
    else {
      over = false;
      return false;
    }
  }

  public void display() 
  {
    stroke(255);
    fill(currentcolor);
    ellipse(x, y, size, size);
  }
}

class RectButton extends Button
{
  RectButton(int ix, int iy, int isize, int icolor, int ihighlight) 
  {
    x = ix;
    y = iy;
    size = isize;
    basecolor = icolor;
    highlightcolor = ihighlight;
    currentcolor = basecolor;
  }

  public boolean over() 
  {
    if( overRect(x, y, size, size) ) {
      over = true;
      return true;
    } 
    else {
      over = false;
      return false;
    }
  }

  public void display() 
  {
    stroke(255);
    fill(currentcolor);
    rect(x, y, size, size);
  }
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "Buttons" });
  }
}
