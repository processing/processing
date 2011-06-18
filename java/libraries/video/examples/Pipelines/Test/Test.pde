/**
 * Test. 
 * By Andres Colubri
 * 
 * This example shows how to create GStreamer pipelines using the GSPipeline object.
 * Pipelines allow to connect different gstreamer elements (video sources, decoders, etc)
 * in order to construct a video or audio stream. The command line tool gst-launch can be used
 * to launch pipelines, and most pipelines specified with gst-launch can be used in GSPipeline,
 * as the shown in this sketch. 
 * Some online material on GStreamer:
 * http://www.cin.ufpe.br/~cinlug/wiki/index.php/Introducing_GStreamer
 * http://www.twm-kd.com/computers/software/webcam-and-linux-gstreamer-tutorial/ 	
 */

import codeanticode.gsvideo.*;

GSPipeline pipeline;

void setup() {
  size(320, 240);
  
  // VideoTestSrc pipeline. Note that there is no need to specify a 
  // video sink as the last element of the pipeline, because GSVideo
  // automatically directs the video frames of the pipeline to  
  // Processing's drawing surface.  
  pipeline = new GSPipeline(this, "videotestsrc");
  
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