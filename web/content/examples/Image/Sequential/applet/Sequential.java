import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Sequential extends PApplet {// Sequential
// by James Patterson <http://www.presstube.com> 
// and REAS <http://reas.com>

// Displaying a sequence of images creates the illusion of motion.
// Twelve images are loaded and each is displayed individually in a loop.

// Created 13 January 2003

int numFrames = 12;  // The number of frames in the animation
int frame = 0;
PImage[] images = new PImage[numFrames];
    
public void setup()
{
  size(200, 200);
  framerate(30);
  
  images[0]  = loadImage("PT_anim0000.gif");
  images[1]  = loadImage("PT_anim0001.gif"); 
  images[2]  = loadImage("PT_anim0002.gif");
  images[3]  = loadImage("PT_anim0003.gif"); 
  images[4]  = loadImage("PT_anim0004.gif");
  images[5]  = loadImage("PT_anim0005.gif"); 
  images[6]  = loadImage("PT_anim0006.gif");
  images[7]  = loadImage("PT_anim0007.gif"); 
  images[8]  = loadImage("PT_anim0008.gif");
  images[9]  = loadImage("PT_anim0009.gif"); 
  images[10] = loadImage("PT_anim0010.gif");
  images[11] = loadImage("PT_anim0011.gif"); 
  
  // If you don't want to load each image separately
  // and you know how many frames you have, you
  // can use create the filenames as the program runs
  //for(int i=0; i<numFrames; i++) {
  //  String imageName = "PT_anim00" + ((i < 10) ? "0" : "") + i + ".gif";
  //  images[i] = loadImage(imageName);
  //}
} 
 
public void draw() 
{ 
  frame = (frame+1)%numFrames;  // Use % to cycle through frames
  image(images[frame], 0, 0);
}
}