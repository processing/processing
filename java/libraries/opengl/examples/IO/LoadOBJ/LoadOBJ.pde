PShape rocket;
  
public void setup() {
  size(640, 360, P3D);
    
  rocket = loadShape("rocket.obj");
}

public void draw() {
  background(0);
    
  lights();
    
  translate(width/2, height/2 + 100, -200);
  rotateX(PI);
  rotateY(frameCount * 0.01f);
  shape(rocket);
}
