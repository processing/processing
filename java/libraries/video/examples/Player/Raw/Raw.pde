/**
 * Raw. 
 * 
 * Gets raw data frames from video stream, without any color conversion.
 */
 
import codeanticode.gsvideo.*;

GSPlayer video;

void setup() {
    size(100, 100);
    video = new GSPlayer(this, "station.mov", GSVideo.RAW);
    video.loop();
}

void playerEvent(GSPlayer player) {
  player.read();
}

void draw() {
  // The raw frame data is stored in video.data, which is a byte array.
  // video.dataCaps is a string containing info about the incoming data.
  
  if (video.data != null) {
    println("Data size: " + video.data.length);
    println("Data caps: " + video.dataCaps);  
  }
}
