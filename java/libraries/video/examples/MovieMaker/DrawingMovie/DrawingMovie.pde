/**
 * GSVideo drawing movie example.
 *
 * Adapted from Daniel Shiffman's original Drawing Movie 
 * example by Andres Colubri 
 * Makes a movie of a line drawn by the mouse. Press
 * the spacebar to finish and save the movie. 
 */

import codeanticode.gsvideo.*;

GSMovieMaker mm;
int fps = 30;

void setup() {
  size(320, 240);
  frameRate(fps);
  
  PFont font = createFont("Courier", 24);
  textFont(font, 24);

  // Save as THEORA in a OGG file as MEDIUM quality (all quality settings are WORST, LOW,
  // MEDIUM, HIGH and BEST):
  mm = new GSMovieMaker(this, width, height, "drawing.ogg", GSMovieMaker.THEORA, GSMovieMaker.MEDIUM, fps);
  
  // Available codecs are: 
  // THEORA
  // XVID
  // X264
  // DIRAC
  // MJPEG
  // MJPEG2K
  // As for the file formats, the following are autodetected from the filename extension:
  // .ogg: OGG
  // .avi: Microsoft's AVI
  // .mov: Quicktime's MOV
  // .flv: Flash Video
  // .mkv: Matroska container
  // .mp4: MPEG-4
  // .3gp: 3GGP video
  // .mpg: MPEG-1
  // .mj2: Motion JPEG 2000
  // Please note that some of the codecs/containers might not work as expected, depending
  // on which gstreamer plugins are installed. Also, some codec/container combinations 
  // don't seem to be compatible, for example THEORA+AVI or X264+OGG.
  
  // Encoding with DIRAC codec into an avi file:
  //mm = new GSMovieMaker(this, width, height, "drawing.avi", GSMovieMaker.DIRAC, GSMovieMaker.BEST, fps);  
  
  // Important: Be sure of using the same framerate as the one set with frameRate().
  // If the sketch's framerate is higher than the speed with which GSMovieMaker 
  // can compress frames and save them to file, then the computer's RAM will start to become 
  // clogged with unprocessed frames waiting on the gstreamer's queue. If all the physical RAM 
  // is exhausted, then the whole system might become extremely slow and unresponsive.
  // Using the same framerate as in the frameRate() function seems to be a reasonable choice,
  // assuming that CPU can keep up with encoding at the same pace with which Processing sends
  // frames (which might not be the case is the CPU is slow). As the resolution increases, 
  // encoding becomes more costly and the risk of clogging the computer's RAM increases.
    
  // The movie maker can also be initialized by explicitly specifying the name of the desired gstreamer's
  // encoder and muxer elements. Also, arrays with property names and values for the encoder can be passed.
  // In the following code, the DIRAC encoder (schroenc) and the Matroska muxer (matroskamux) are selected,
  // with an encoding quality of 9.0 (schroenc accepts quality values between 0 and 10). The property arrays
  // can be set to null in order to use default property values.
  //String[] propName = { "quality" };
  //Float f = 9.0f;
  //Object[] propValue = { f };
  //mm = new GSMovieMaker(this, width, height, "drawing.ogg", "schroenc", "oggmux", propName, propValue, fps);
  
  // There are two queues in the movie recording process: a pre-encoding queue and an encoding 
  // queue. The former is stored in the Java side and the later inside gstreamer. When the 
  // encoding queue is full, frames start to accumulate in the pre-encoding queue until its
  // maximum size is reached. After that point, new frames are dropped. To have no limit in the
  // size of the pre-encoding queue, set it to zero.
  // The size of both is set with the following function (first argument is the size of pre-
  // encoding queue):
  mm.setQueueSize(50, 10);
  
  mm.start();
  
  background(160, 32, 32);
}

void draw() {
  stroke(7, 146, 168);
  strokeWeight(4);
  
  // Draw if mouse is pressed
  if (mousePressed && pmouseX != 0 && mouseY != 0) {
    line(pmouseX, pmouseY, mouseX, mouseY);
  }
  
  // Drawing framecount.
  String s = "Frame " + frameCount;
  fill(160, 32, 32);
  noStroke();
  rect(10, 6, textWidth(s), 24);
  fill(255);
  text(s, 10, 30);

  loadPixels();
  // Add window's pixels to movie
  mm.addFrame(pixels);
  
  println("Number of queued frames : " + mm.getQueuedFrames());
  println("Number of dropped frames: " + mm.getDroppedFrames());
}

void keyPressed() {
  if (key == ' ') {
    // Finish the movie if space bar is pressed
    mm.finish();
    // Quit running the sketch once the file is written
    exit();
  }
}