import processing.jogl.*;

void setup() {
  size(4000, 2000, JOGL.P3D);
}
  
void draw() {
  background(255, 0, 0);
  line(0, 0, width, height);
  line(0, height, width, 0);    
  println(mouseX, mouseY);
  ellipse(mouseX, mouseY, 50, 50);
}