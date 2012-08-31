/**
 * Save Frames
 * by Daniel Shiffman.  
 * 
 * This example demonstrates how to use saveFrame() to render
 * out an image sequence that you can assemble into a movie
 * using the MovieMaker tool.
 */

// A boolean to track whether we are recording are not
boolean recording = false;

void setup() {
  size(640, 360);
  smooth();
}

void draw() {
  background(0);
  
  // An arbitrary oscillating rotating animation 
  // so that we have something to render
  for (float a = 0; a < TWO_PI; a+= 0.2) {
    pushMatrix();
    translate(width/2, height/2);
    rotate(a+sin(frameCount*0.004*a));
    stroke(255);
    line(-100, 0, 100, 0);
    popMatrix();
  }
  
  // If we are recording call saveFrame!
  // The number signs (#) indicate to Processing to 
  // number the files automatically
  if (recording) {
    saveFrame("output/frames####.png");
  }
   
  // Let's draw some stuff to tell us what is happening
  // It's important to note that none of this will show up in the
  // rendered files b/c it is drawn *after* saveFrame()
  textAlign(CENTER);
  fill(255);
  if (!recording) {
    text("Press r to start recording.", width/2, height-24);
  } 
  else {
    text("Press r to stop recording.", width/2, height-24);
  }
  
  // A red dot for when we are recording
  stroke(255);
  if (recording) {
    fill(255, 0, 0);
  } else { 
    noFill();
  }
  ellipse(width/2, height-48, 16, 16);
}

void keyPressed() {
  
  // If we press r, start or stop recording!
  if (key == 'r' || key == 'R') {
    recording = !recording;
  }
}


