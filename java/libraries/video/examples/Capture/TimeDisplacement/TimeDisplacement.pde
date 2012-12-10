/**
 * Time Displacement
 * by David Muth 
 * 
 * Keeps a buffer of video frames in memory and displays pixel rows
 * taken from consecutive frames distributed over the y-axis 
 */ 
 
import processing.video.*;

Capture video;
int signal = 0;

//the buffer for storing video frames
ArrayList frames = new ArrayList();

void setup() {
  size(640, 480);
  video = new Capture(this, width, height, 30);
  video.start();
}

void captureEvent(Capture camera) {
  camera.read();
  
  //copy the current video frame into an image, so it can be stored in the buffer
  PImage img = createImage(width, height, RGB);
  video.loadPixels();
  arrayCopy(video.pixels, img.pixels);
  
  frames.add(img);
  
  //once there are enough frames, remove the oldest one when adding a new one
  if(frames.size() > height/4) {
    frames.remove(0);
  }
}

void draw() {
 //set the image counter to 0
 int currentImage = 0;
 
 loadPixels();
  
  //begin a loop for displaying pixel rows of 4 pixels height
  for(int y = 0; y < video.height; y+=4) {
    //go through the frame buffer and pick an image, starting with the oldest one
    if(currentImage < frames.size()) {
      PImage img = (PImage)frames.get(currentImage);
      
      if(img != null) {
        img.loadPixels();
        
        //put 4 rows of pixels on the screen
        for(int x = 0; x < video.width; x++) {
          pixels[x + y * width] = img.pixels[x + y * video.width];
          pixels[x + (y + 1) * width] = img.pixels[x + (y + 1) * video.width];
          pixels[x + (y + 2) * width] = img.pixels[x + (y + 2) * video.width];
          pixels[x + (y + 3) * width] = img.pixels[x + (y + 3) * video.width];
        }  
      }
      
      //increase the image counter
      currentImage++;
       
    } else {
      break;
    }
  }
  
  updatePixels();
  
  //for recording an image sequence
  //saveFrame("frame-####.jpg"); 
}




