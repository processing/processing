// Mouse 2D
// by REAS <http://reas.com>

// Moving the mouse changes the position and size of each box.

// Updated 21 August 2002

void setup() 
{
  size(200, 200); 
  noStroke();
  colorMode(RGB, 255, 255, 255, 100);
  rectMode(CENTER);
}

void draw() 
{   
  background(51); 
  fill(255, 80);
  rect(mouseX, height/2, mouseY/2+10, mouseY/2+10);
  fill(255, 80);
  rect(width-mouseX, height/2, ((height-mouseY)/2)+10, ((height-mouseY)/2)+10);
}

