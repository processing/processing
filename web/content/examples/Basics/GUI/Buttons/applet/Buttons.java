import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Buttons extends PApplet {// Buttons
// by REAS <http://reas.com>

// Click on one of the shapes to change
// the background color. This example
// demonstates a class for buttons.

// Created 09 February 2003

int currentcolor;

CircleButton circle1, circle2, circle3;
RectButton rect1, rect2;

boolean locked = false;

public void setup()
{
  size(200, 200);

  int baseColor = color(102, 102, 102);
  currentcolor = baseColor;
  
  // Define and create circle button
  int x = 30;
  int y = 100;
  int size = 100;
  int buttoncolor = color(153, 153, 102);
  int highlight = color(102, 102, 51);
  ellipseMode(CENTER);
  circle1 = new CircleButton(x, y, size, buttoncolor, highlight);
  
  // Define and create rectangle button
  x = 130;
  y = 110; 
  size = 24;
  buttoncolor = color(255, 153, 255);
  highlight = color(204, 153, 204); 
  circle2 = new CircleButton(x, y, size, buttoncolor, highlight);
  
  // Define and create rectangle button
  x = 130;
  y = 140; 
  size = 24;
  buttoncolor = color(204, 204, 0);
  highlight = color(153, 153, 0); 
  circle3 = new CircleButton(x, y, size, buttoncolor, highlight);
  
  // Define and create rectangle button
  x = 150;
  y = 20; 
  size = 100;
  buttoncolor = color(153, 102, 102);
  highlight = color(102, 51, 51); 
  rect1 = new RectButton(x, y, size, buttoncolor, highlight);
  
  // Define and create rectangle button
  x = 90;
  y = 20; 
  size = 50;
  buttoncolor = color(153, 153, 153);
  highlight = color(102, 102, 102); 
  rect2 = new RectButton(x, y, size, buttoncolor, highlight);
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
  } else {
    locked = false;
  }
  
  if(mousePressed) {
    if(circle1.pressed()) {
      currentcolor = circle1.basecolor;
    } else if(circle2.pressed()) {
      currentcolor = circle2.basecolor;
    } else if(circle3.pressed()) {
      currentcolor = circle3.basecolor;
    } else if(rect1.pressed()) {
      currentcolor = rect1.basecolor;
    } else if(rect2.pressed()) {
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
    } else {
      currentcolor = basecolor;
    }
  }
  
  public boolean pressed() 
  {
    if(over) {
      locked = true;
      return true;
    } else {
      locked = false;
      return false;
    }    
  }
  
  public boolean over() 
  { 
    return true; 
  }
  
  public void display() 
  { 
  
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
    } else {
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
    } else {
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

public boolean overRect(int x, int y, int width, int height) 
{
  if (mouseX >= x && mouseX <= x+width && 
      mouseY >= y && mouseY <= y+height) {
    return true;
  } else {
    return false;
  }
}

public boolean overCircle(int x, int y, int diameter) 
{
  float disX = x - mouseX;
  float disY = y - mouseY;
  if(sqrt(sq(disX) + sq(disY)) < diameter/2 ) {
    return true;
  } else {
    return false;
  }
}
}