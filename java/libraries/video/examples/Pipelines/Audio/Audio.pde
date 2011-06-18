/**
 * Audio pipeline. 
 * By Andres Colubri
 * 
 */

import codeanticode.gsvideo.*;

GSPipeline pipeline;

void setup() {
  size(100, 100);
  
  // An audio-only pipeline can be specified by setting the type parameter to GSVideo.AUDIO.
  // In this way, GSVideo doesn't try to copy the stream to the Processing window.
  // The other two possible types are GSVideo.VIDEO (default) and GSVideo.DATA.
  // Linux:
  pipeline = new GSPipeline(this, "audiotestsrc ! audioconvert ! alsasink", GSVideo.AUDIO);
  // Windows:
  //pipeline = new GSPipeline(this, "audiotestsrc ! audioconvert ! directsoundsink", GSVideo.AUDIO);
  
  // The pipeline starts in paused state, so a call to the play()
  // method is needed to get thins rolling.
  pipeline.play();
}

void draw() {
  // No need to draw anything on the screen. The audio gets 
  // automatically directed to the sound card.
}




