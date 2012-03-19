void setup() {
  size(400, 400, P3D); 
  smooth(2);
}

void draw() {
  background(255, 0, 0);
  ellipse(mouseX, mouseY, 100, 100);  
}

void keyPressed() {  
  if (key == '1') smooth(1);
  else if (key == '2') smooth(2);
  else if (key == '3') smooth(4);
  else if (key == '4') smooth(8);
  else if (key == '5') smooth(16);
  else if (key == '6') smooth(32); 
}

