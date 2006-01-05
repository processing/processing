// Brightness
// by Rusty Robison

// Brightness is the relative lightness or darkness of a color.
// Move the cursor vertically over each bar to alter its brightness.

// Updated 26 October 2002

int barWidth = 5;
int[] brightness = new int[200/barWidth];

void setup() 
{
  size(200, 200);
  colorMode(HSB, 360, height, height);  
}

void draw() 
{
  int j = 0;
  for (int i=0; i<=(width-barWidth); i+=barWidth) {  
    noStroke();
    if ((mouseX > i) && (mouseX < i+barWidth)) {
      brightness[j] = mouseY;
    }
    fill(i, width, brightness[j]);
    rect(i, 0, barWidth, height);  
    j++;
  }
}
