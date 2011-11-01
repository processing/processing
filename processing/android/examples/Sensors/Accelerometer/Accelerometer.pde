AccelerometerManager accel;
float ax, ay, az;


void setup() {
  accel = new AccelerometerManager(this);
  orientation(PORTRAIT);
  noLoop();
}


void draw() {
  background(0);
  fill(255);
  textSize(70);
  textAlign(CENTER, CENTER);
  text("x: " + nf(ax, 1, 2) + "\n" + 
       "y: " + nf(ay, 1, 2) + "\n" + 
       "z: " + nf(az, 1, 2), 
       0, 0, width, height);
}


public void resume() {
  if (accel != null) {
    accel.resume();
  }
}

    
public void pause() {
  if (accel != null) {
    accel.pause();
  }
}


public void shakeEvent(float force) {
  println("shake : " + force);
}


public void accelerationEvent(float x, float y, float z) {
//  println("acceleration: " + x + ", " + y + ", " + z);
  ax = x;
  ay = y;
  az = z;
  redraw();
}
