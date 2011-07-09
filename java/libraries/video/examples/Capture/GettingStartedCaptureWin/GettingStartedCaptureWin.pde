/**
 * Getting Started with Capture.
 * 
 * GSVideo version by Andres Colubri. 
 * 
 * Reading and displaying an image from an attached Capture device. 
 */ 
import codeanticode.gsvideo.*;

GSCapture cam;

void setup() {
  size(640, 480);

  String[] cameras = GSCapture.list();
  
  if (cameras.length == 0)
  {
    println("There are no cameras available for capture.");
    exit();
  } else {
    println("Available cameras:");
    for (int i = 0; i < cameras.length; i++) {
      println(cameras[i]);
    }
    cam = new GSCapture(this, 640, 480, cameras[0]);
    cam.start();    

    /*
    // You can get the resolutions supported by the
    // capture device using the resolutions() method.
    // It must be called after creating the capture 
    // object. 
    int[][] res = cam.resolutions();
    for (int i = 0; i < res.length; i++) {
      println(res[i][0] + "x" + res[i][1]);
    } 
    */
  
    /*
    // You can also get the framerates supported by the
    // capture device:
    String[] fps = cam.framerates();
    for (int i = 0; i < fps.length; i++) {
      println(fps[i]);
    } 
    */   
  }
}

void draw() {
  if (cam.available() == true) {
    cam.read();
    image(cam, 0, 0);
    // The following does the same, and is faster when just drawing the image
    // without any additional resizing, transformations, or tint.
    //set(0, 0, cam);
  }
}
