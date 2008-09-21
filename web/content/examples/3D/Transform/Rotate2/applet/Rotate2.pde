/**
 * Rotate 2. 
 * 
 * The push() and pop() functions allow for more control over transformations.
 * The push function saves the current coordinate system to the stack 
 * and pop() restores the prior coordinate system. 
 */
 
float a;                          // Angle of rotation
float offset = PI/24.0;             // Angle offset between boxes
int num = 12;                     // Number of boxes
color[] colors = new color[num];  // Colors of each box
color safecolor;

boolean pink = true;

void setup() 
{ 
  size(640, 360, P3D);
  noStroke();  
  for(int i=0; i<num; i++) {
    colors[i] = color(255 * (i+1)/num);
  }
  lights();
} 
 

void draw() 
{     
  background(0, 0, 26);
  translate(width/2, height/2);
  a += 0.01;   
  
  for(int i = 0; i < num; i++) {
    pushMatrix();
    fill(colors[i]);
    rotateY(a + offset*i);
    rotateX(a/2 + offset*i);
    box(200);
    popMatrix();
  }
} 
