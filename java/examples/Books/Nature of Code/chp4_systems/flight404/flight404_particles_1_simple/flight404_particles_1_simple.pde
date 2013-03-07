// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Updated version of flight404 Particle Emitter release 1
// This works with Processing 2.0

// February 28 2011
// Daniel Shiffman

// Source Code release 1
// Particle Emitter
//
// February 11th 2008
//
// Built with Processing v.135 which you can download at http://www.processing.org/download
//
// Robert Hodgin
// flight404.com
// barbariangroup.com

// features:
//           Toxi's magnificent Vec3D library
//           perlin noise flow fields
//           ribbon trails
//           OpenGL additive blending
//           OpenGL display lists
//
// Uses the very useful Vec3D library by Karsten Schmidt (toxi)
// You can download it at http://code.google.com/p/toxiclibs/downloads/list
//
// Please post suggestions and improvements at the flight404 blog. When nicer/faster/better
// practices are suggested, I will incorporate them into the source and repost. I think that
// will be a reasonable system for now.
//
// Future additions will include:
//           Rudimentary camera movement
//           Magnetic repulsion
//           More textures means more iron
//
// UPDATES
//
// February 11th 2008
// Reorganized some of the OpenGL calls as per Simon Gelfius' suggestion.
// http://www.kinesis.be/


import toxi.geom.*;
import java.util.*;

PGL pgl;

Emitter emitter;
Vec3D gravity;
float floorLevel;

PImage particleImg;
PImage emitterImg;

int counter;


boolean ALLOWGRAVITY;    // add gravity vector?
boolean ALLOWPERLIN;     // add perlin noise flow field vector?
boolean ALLOWTRAILS;     // render particle trails?
boolean ALLOWFLOOR;      // add a floor?
                         // Turning on all of these options will make things
                         // slow down. 

void setup(){
  size( 600, 600, P3D );
  smooth(4);
  // Lately I have gotten into the habit of limiting the color range to be 
  // 0.0 to 1.0. It works this way in OpenGL so I might as well get used to it.
  colorMode( RGB, 1.0 );
  
  // Turn on 4X antialiasing
  //hint( ENABLE_OPENGL_4X_SMOOTH );
  
  // More OpenGL necessity.
  pgl         = ((PGraphicsOpenGL) g).pgl;  
  
  // Loads in a particle image from the data folder. Image size should be a power of 2.
  particleImg = loadImage( "particle.png" );
  emitterImg  = loadImage( "emitter.png" );
  
  emitter     = new Emitter();
  gravity     = new Vec3D( 0, .35, 0 );    // gravity vector
  floorLevel  = 400;
}

void draw(){
  background( 0.0 );
  perspective( PI/3.0, (float)width/(float)height, 1, 5000 );
  
  // Turns on additive blending so we can draw a bunch of glowing images without
  // needing to do any depth testing.
  pgl.depthMask(false);
  pgl.enable( PGL.BLEND );
  pgl.blendFunc(PGL.SRC_ALPHA, PGL.ONE);
  
  emitter.exist();
  
  // If the mouse button is pressed, then add 10 new particles.
  if( mousePressed ){
    if( ALLOWTRAILS && ALLOWFLOOR ){
      emitter.addParticles( 5 );
    } else {
      emitter.addParticles( 10 );
    }
  }
  
  counter ++;
}


void keyPressed(){
  if( key == 'g' || key == 'G' )
    ALLOWGRAVITY = !ALLOWGRAVITY;
    
  if( key == 'p' || key == 'P' )
    ALLOWPERLIN  = !ALLOWPERLIN;
  
  if( key == 't' || key == 'T' )
    ALLOWTRAILS  = !ALLOWTRAILS;
    
  if( key == 'f' || key == 'F' )
    ALLOWFLOOR   = !ALLOWFLOOR;

}


// This method should be nicer, but it isnt. I use getRads to get a perlin noise
// based angle in radians based on the x and y position of the object asking for it.
// Perlin noise is supposed to give you back a number between 0 and 1, but it wont
// necessarily give you numbers that range from 0 to 1.  A usual result is more like
// .25 to .75.
//
// So the point of this method is to try to normalize the values to a 
// range of 0 to 1.  It's not perfect, and I still get weird results.
// For instance, the mult variable is supposed to be the multiplier for the range.
// So if i wanted a random angle between 0 and TWO_PI, I would set the mult = TWO_PI. 
// But when I do that, I find the Perlin noise tends to give me a left-pointing angle.
// To counteract, I end up setting the mult to 10.0 in order to increase the chances
// that I get a nice range from at least 0 to TWO_PI.
float minNoise = 0.499;
float maxNoise = 0.501;
float getRads(float val1, float val2, float mult, float div){
  float rads = noise(val1/div, val2/div, counter/div);
  
  if (rads < minNoise) minNoise = rads;
  if (rads > maxNoise) maxNoise = rads;
  
  rads -= minNoise;
  rads *= 1.0/(maxNoise - minNoise);

  return rads * mult;
}
