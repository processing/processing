/**
 * Camera capture pipelines. 
 * By Andres Colubri
 * 
 */

import codeanticode.gsvideo.*;

GSPipeline pipeline;

void setup() {
  size(640, 480);
  
  // The ksvideosrc element allows to select a capture device by index (0, 1, 2, etc).
  //pipeline = new GSPipeline(this, "ksvideosrc device-index=0 ! decodebin2");
  
  // DirectShow capture pipelines:
  // Uses the first availabe capture device.
  //pipeline = new GSPipeline(this, "dshowvideosrc ! decodebin2");
  // This one allows to choose the device based on its name property.
  //pipeline = new GSPipeline(this, "dshowvideosrc device-name=\"Sony Visual Communication Camera VGP-VCC7\" ! decodebin2");

  // Capture pipeline in MacOSX 64 bits. It uses the qtkitvideosrc element based on the
  // new QTkit. The input device can be set using the device-index property, which expects an
  // integer value, like ksvideosrc above.
  //pipeline = new GSPipeline(this, "qtkitvideosrc");

  // Vide4Linux2 capture pipeline.    
  pipeline = new GSPipeline(this, "v4l2src");  
  
  // The full pipeline that GSVideo passes to GStremeamer can be
  // obtained with the getPipeline() method: 
  println("Pipeline string:");
  println(pipeline.getPipeline());
  
    // Tentative dv1394 capture pipeline. This thread on the Processing's discourse:
  // http://processing.org/discourse/yabb2/YaBB.pl?num=1210072258/30
  // could be very useful to setup dv capture.
  //pipeline = new GSPipeline(this, "dv1394src port=0 ! queue ! dvdemux ! ffdec_dvvideo ! ffmpegcolorspace ! video/x-raw-yuv, width=720");
  
  // The pipeline starts in paused state, so a call to the play()
  // method is needed to get thins rolling.
  pipeline.play();
}

void draw() {
  // When the GSPipeline.available() method returns true, 
  // it means that a new frame is ready to be read.
  if (pipeline.available()) {
    pipeline.read();
    image(pipeline, 0, 0);
  }
}





