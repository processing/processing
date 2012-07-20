import processing.dxf.*;

PShape rocket;
  
boolean record;  
  
public void setup() {
  size(640, 360, P3D);
    
  rocket = loadShape("rocket.obj");
}

public void draw() {
  if (record) {
    beginRaw(DXF, "rocket.dxf");
  }  
  
  background(0);
    
  lights();
    
  translate(width/2, height/2 + 100, -200);
  rotateX(PI);
  rotateY(frameCount * 0.01f);
  shape(rocket);
  
  if (record) { 
    endRaw();
    record = false;
  }  
}

void keyPressed() {
  if (key == 'r') record = true;
}  
