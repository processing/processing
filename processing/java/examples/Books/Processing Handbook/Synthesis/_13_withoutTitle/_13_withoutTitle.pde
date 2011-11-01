/** 
 * Synthesis 4: Structure and Interface
 * WithoutTitle by Lia (http://lia.sil.at)
 * p. 496
 * 
 * Move the mouse to create the drawing. Click to refresh the window.
 */
 

int num = 50;
ModuleA[] modsA;
ModuleB[] modsB;
boolean initialized = false;

void setup() { 
  size(600, 600);
  colorMode(RGB, num);
  background(num);
  smooth();
  
  modsA = new ModuleA[num];
  modsB = new ModuleB[num];
  
  for (int i=0; i<num; i++) {
    int qq;
    if(random(1) > 0.5) {
      qq = 1;
    } else {
      qq = -1;
    }
    float x = random(width);
    float y = random(height);
    float angle = random(360);
    float direction = random(10, 40);
    modsA[i] = new ModuleA(i, x, y, angle, direction, qq);
    modsB[i] = new ModuleB(i, x, y, angle, direction, qq);
  }

}

void draw() {
  if(initialized == true){
    for (int i=0; i<num; i++){
      modsA[i].updateMe();
      for (int j=0; j<num; j++){
        modsB[j].myAngle = modsA[j].myAngle;
        modsB[j].myRadius = modsA[j].myRadius+i;
      }
      modsB[i].updateMe();
      if ( (modsA[i].x < 0) || (modsA[i].x > width) || (modsA[i].y < 0) || (modsA[i].y > height) ){
        modsA[i].x = mouseX;
        modsA[i].y = mouseY;
        modsB[i].x = mouseX;
        modsB[i].y = mouseY;
        float a = random(360);
        modsA[i].myAngle = a;
        modsB[i].myAngle = a;
        float r = random(10, 40);
        modsA[i].myRadius = r;
        modsB[i].myRadius = r+i*i;
      }
    }
  }
}

void mousePressed() {
  background(num); 
}

void mouseMoved() {
  initialized = true; 
}

void keyPressed() {
  // saveFrame("withouttitle-####.tif"); 
}


