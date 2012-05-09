/** 
 * Near and Far example
 *
 * This example shows the effect of setting the near and far values
 * in the ortho() and perspective() functions. Both values are always 
 * measured from the camera eye position and along the eye-center
 * vector.
 * 
 * The spacebar switches between perspective and orthographic 
 * projections, to set the near value press 'n' and 'f' for far.
 * A mouse click switches the near/far setting on and off.
 * 'r' enables/disables rotating the object with the mouse.
 */

boolean usingPerspective = true;
boolean settingFar = true;
boolean settingEnabled = true;
boolean rotating = false;

float cameraFOV;
float cameraZ;
float cameraMaxFar;
float cameraNear;
float cameraFar;

float angleX = -PI/6;
float angleY = PI/3;

void setup() {
  size(640, 360, P3D);
  orientation(LANDSCAPE);

  cameraFOV = PI/3.0; 
  cameraZ = (height/2.0) / tan(cameraFOV/2.0);
  cameraMaxFar = cameraZ * 2.0;

  cameraNear = cameraZ / 2.0;
  cameraFar = cameraZ * 2.0;
}

void draw() {
  background(0);
  lights();

  if (settingEnabled) {
    if (settingFar) {
      float minx = map(cameraNear, 0, cameraMaxFar, 0, width);    
      cameraFar = map(mouseX, minx, width, cameraNear, cameraMaxFar);
    } else {
      float maxx = map(cameraFar, 0, cameraMaxFar, 0, width);
      cameraNear = map(mouseX, 0, maxx, 0, cameraFar);
    }
  }

  if (usingPerspective) {
    perspective(cameraFOV, float(width)/float(height), cameraNear, cameraFar);
  } else {
    ortho(0, width, 0, height, cameraNear, cameraFar);
  }

  pushMatrix();

  translate(width/2, height/2, 0);
  
  if (rotating) {
    angleX = map(mouseX, 0, width, -PI, PI);
    angleY = map(mouseY, 0, width, -PI, PI);
  }
  rotateX(angleX); 
  rotateY(angleY);

  stroke(50);
  fill(204);  
  box(160);

  popMatrix();

  // Drawing visual clues for the near and far values.
  perspective(); // Restoring to the default perspective matrix.
  noStroke();
  float near = map(cameraNear, 0, cameraMaxFar, 0, width);
  float far = map(cameraFar, 0, cameraMaxFar, 0, width);
  fill(204);
  rect(0, 0, near, 20);
  rect(far, 0, width - far, 20);  
  if (near <= far) {
    fill(160);  
  } else {
    fill(200, 50, 50);
  }
  rect(near, 0, far - near, 20);
}

void keyPressed() {
  if (key == ' ') {
    usingPerspective = !usingPerspective;
  } else if (key == 'f' || key == 'F') {
    settingFar = true;
  } else if (key == 'n' || key == 'N') {
    settingFar = false;
  } else if (key == 'r' || key == 'R') {
    if (rotating) {
      rotating = false;
    } else {
      rotating = true;
      settingEnabled = false;
    }
  }
}

void mousePressed() {
  if (!rotating) {
    settingEnabled = !settingEnabled;
  }
}

