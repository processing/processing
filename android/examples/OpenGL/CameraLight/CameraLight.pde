// CameraLight, by Andres Colubri
// Simple example showing a lit rotating cube. The projection
// is set to orthographic if the screen is tapped.

float spin = 0.0;

void setup() {
  size(480, 800, A3D);
  orientation(PORTRAIT);
  
  noStroke();
}

void draw() {
  background(51);
  lights();
  
  if (mousePressed) {
    // The arguments of ortho are specified in screen coordinates, where (0,0)
    // is the upper left corner of the screen
    ortho(0, 0, width, height);     
  } else {

    float fov = PI/3.0; 
    float cameraZ = (height/2.0) / tan(fov/2.0); 
    perspective(fov, float(width)/float(height), 
                cameraZ/2.0, cameraZ*2.0); 
  }  
  
  spin += 0.01;
  
  pushMatrix();
  translate(width/2, height/2, 100);
  rotateX(PI/9);
  rotateY(PI/5 + spin);
  box(150);
  popMatrix();
}




