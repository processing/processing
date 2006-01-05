// Hue
// by REAS <http://reas.com>
// Modified from code by Rusty Robison

// Hue is the color reflected from or transmitted through an object 
// and is typically referred to as the name of the color (red, blue, yellow, etc.) 
// Move the cursor vertically over each bar to alter its hue.

// Updated 26 October 2002

int barWidth = 5;
int[] hue = new int[200/barWidth];

void setup() 
{
  size(200, 200);
  colorMode(HSB, 360, height, height);  
  noStroke();
}

void draw() 
{
  int j = 0;
  for (int i=0; i<=(width-barWidth); i+=barWidth) {  
    if ((mouseX > i) && (mouseX < i+barWidth)) {
      hue[j] = mouseY;
    }
    fill(hue[j], height/1.2, height/1.2);
    rect(i, 0, barWidth, height);  
    j++;
  }
}
