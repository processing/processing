/**
 * Constrain. 
 * 
 * Move the mouse across the screen to move the circle. 
 * The program constrains the circle to its box. 
 */
 
float mx;
float my;
float easing = 0.05;
float esize = 25.0;
int box = 30;

void setup() 
{
  size(200, 200);
  noStroke(); 
  smooth();
  ellipseMode(RADIUS);  
}

void draw() 
{ 
  background(51);
  
  if(abs(mouseX - mx) > 0.1) {
    mx = mx + (mouseX - mx) * easing;
  }
  if(abs(mouseY - my) > 0.1) {
    my = my + (mouseY- my) * easing;
  }
  
  float distance = esize * 2;
  mx = constrain(mx, box+distance, width-box-distance);
  my = constrain(my, box+distance, height-box-distance);
  fill(76);
  rect(box+esize, box+esize, box*3, box*3);
  fill(255);  
  ellipse(mx, my, esize, esize);
}
