/**
 * Getting Started with Capture.
 * 
 * Reading and displaying an image from an attached Capture device. 
 */ 
 
import processing.video.*;

Capture cam;

void setup() {
  size(640, 480);

  String[] cameras = Capture.list();
  
  if (cameras.length == 0) {
    println("There are no cameras available for capture.");
    exit();
  } else {
    println("Available cameras:");
    for (int i = 0; i < cameras.length; i++) {
      println(cameras[i]);
    }
    
    cam = new Capture(this, 320, 240, cameras[0]);
    cam.start();
    
    // You can get the list of resolutions (width x height x fps)  
    // supported capture device by calling the resolutions()
    // method. It must be called after creating the capture 
    // object. 
    Resolution[] res = cam.resolutions();
    println("Supported resolutions:");
    for (int i = 0; i < res.length; i++) { 
      println(res[i].width + "x" + res[i].height + ", " + 
              res[i].fps + " fps (" + res[i].fpsString +")");  
    }        
  }      
}

void draw() {
  if (cam.available() == true) {
    cam.read();
  }
  image(cam, 0, 0);
  // The following does the same, and is faster when just drawing the image
  // without any additional resizing, transformations, or tint.
  //set(0, 0, cam);
}
