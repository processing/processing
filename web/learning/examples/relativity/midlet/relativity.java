import processing.core.*; public class relativity extends PMIDlet{// Relativity 
// by REAS <http://reas.com> 
//
// Each color is perceived in relation to other colors. The top and bottom 
// bars each contain the same component colors, but a different display
// order causes individual colors to appear differently.
//
// Created 23 October 2002 

int a, b, c, d, e; 
 
public void setup() { 
  noStroke(); 
  a = color(165, 167, 20); 
  b = color(77, 86, 59); 
  c = color(42, 106, 105); 
  d = color(165, 89, 20); 
  e = color(146, 150, 127); 
  noLoop(); 
} 
 
public void draw() { 
  drawBand(a, b, c, d, e, 0, 4); 
  drawBand(c, a, d, b, e, height/2, 4); 
} 
 
public void drawBand(int v, int w, int x, int y, int z, int ypos, int barWidth) { 
  int num = 5; 
  int[] colorOrder = { v, w, x, y, z }; 
  for(int i=0; i<width; i+=barWidth*num) { 
    for(int j=0; j<num; j++) { 
      fill(colorOrder[j]); 
      rect(i+j*barWidth, ypos, barWidth, height/2); 
    } 
  } 
} 
}