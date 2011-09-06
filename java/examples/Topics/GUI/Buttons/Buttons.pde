/**
 * Buttons. 
 * 
 * Click on one of the shapes to change
 * the background color. This example
 * demonstates a class for buttons.
 */

color currentcolor;

CircleButton circle1, circle2, circle3;
RectButton rect1, rect2;

boolean locked = false;

void setup()
{
  size(640, 360);
  smooth();

  color baseColor = color(102);
  currentcolor = baseColor;

  // Define and create circle button
  color buttoncolor = color(204);
  color highlight = color(153);
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

void draw()
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

void update(int x, int y)
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
  color basecolor, highlightcolor;
  color currentcolor;
  boolean over = false;
  boolean pressed = false;   

  void update() 
  {
    if(over()) {
      currentcolor = highlightcolor;
    } 
    else {
      currentcolor = basecolor;
    }
  }

  boolean pressed() 
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

  boolean over() 
  { 
    return true; 
  }

  boolean overRect(int x, int y, int width, int height) 
  {
    if (mouseX >= x && mouseX <= x+width && 
      mouseY >= y && mouseY <= y+height) {
      return true;
    } 
    else {
      return false;
    }
  }

  boolean overCircle(int x, int y, int diameter) 
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
  CircleButton(int ix, int iy, int isize, color icolor, color ihighlight) 
  {
    x = ix;
    y = iy;
    size = isize;
    basecolor = icolor;
    highlightcolor = ihighlight;
    currentcolor = basecolor;
  }

  boolean over() 
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

  void display() 
  {
    stroke(255);
    fill(currentcolor);
    ellipse(x, y, size, size);
  }
}

class RectButton extends Button
{
  RectButton(int ix, int iy, int isize, color icolor, color ihighlight) 
  {
    x = ix;
    y = iy;
    size = isize;
    basecolor = icolor;
    highlightcolor = ihighlight;
    currentcolor = basecolor;
  }

  boolean over() 
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

  void display() 
  {
    stroke(255);
    fill(currentcolor);
    rect(x, y, size, size);
  }
}

