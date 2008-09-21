import processing.core.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class LoadingImages extends PApplet {
  public void setup() {/**
 * Loading Images. 
 * 
 * Loading a recent image from the US National Weather Service. 
 * Notice the date in the upper left corner of the image. 
 * Processing applications can only load images from the network
 * while running in the Processing environment. This example will 
 * not run in a web broswer and will only work when the computer
 * is connected to the Internet. 
 */
 
size(200, 200);
PImage img1;
img1 = loadImage("http://www.processing.org/img/processing_beta_cover.gif");
image(img1, 0, 45);


  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "LoadingImages" });
  }
}
