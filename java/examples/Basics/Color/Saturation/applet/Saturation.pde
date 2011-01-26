/**
 * Saturation. 
 * 
 * Saturation is the strength or purity of the color and represents the 
 * amount of gray in proportion to the hue. A "saturated" color is pure 
 * and an "unsaturated" color has a large percentage of gray. 
 * Move the cursor vertically over each bar to alter its saturation. 
 */
 
int barWidth = 5;
int[] saturation;

void setup() 
{
  size(200, 200);
  colorMode(HSB, 360, height, height); 
  saturation = new int[width/barWidth];
}

void draw() 
{
  int j = 0;
  for (int i=0; i<=(width-barWidth); i+=barWidth) {  
    noStroke();
    if ((mouseX > i) && (mouseX < i+barWidth)) {
      saturation[j] = mouseY;
    }
    fill(i, saturation[j], height/1.5);
    rect(i, 0, barWidth, height);  
    j++;
  }
}
