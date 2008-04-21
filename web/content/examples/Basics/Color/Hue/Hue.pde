/**
 * Hue. 
 * 
 * Hue is the color reflected from or transmitted through an object 
 * and is typically referred to as the name of the color (red, blue, yellow, etc.) 
 * Move the cursor vertically over each bar to alter its hue. 
 */
 
int barWidth = 5;
int[] hue;

void setup() 
{
  size(400, 400);
  colorMode(HSB, 360, height, height);  
  hue = new int[width/barWidth];
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
