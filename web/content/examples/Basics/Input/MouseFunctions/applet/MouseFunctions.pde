/**
 * Mouse Functions. 
 * 
 * Click on the box and drag it across the screen. 
 */
 
float bx;
float by;
int bs = 20;
boolean bover = false;
boolean locked = false;
float bdifx = 0.0; 
float bdify = 0.0; 


void setup() 
{
  size(200, 200);
  bx = width/2.0;
  by = height/2.0;
  rectMode(CENTER_RADIUS);  
}

void draw() 
{ 
  background(0);
  
  // Test if the cursor is over the box 
  if (mouseX > bx-bs && mouseX < bx+bs && 
      mouseY > by-bs && mouseY < by+bs) {
    bover = true;  
    if(!locked) { 
      stroke(255); 
      fill(153);
    } 
  } else {
    stroke(153);
    fill(153);
    bover = false;
  }
  
  // Draw the box
  rect(bx, by, bs, bs);
}

void mousePressed() {
  if(bover) { 
    locked = true; 
    fill(255, 255, 255);
  } else {
    locked = false;
  }
  bdifx = mouseX-bx; 
  bdify = mouseY-by; 

}

void mouseDragged() {
  if(locked) {
    bx = mouseX-bdifx; 
    by = mouseY-bdify; 
  }
}

void mouseReleased() {
  locked = false;
}

