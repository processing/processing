/*
This example shows how to make a simple sampler and sequencer with the Sound library. In this
sketch 5 different short samples are loaded and played back at different pitches, in this
case 5 different octaves. The sequencer triggers and event every 200-1000 mSecs randomly.
Each time a sound is played a colored rect with a random color is displayed.
*/

import processing.sound.*;

AudioDevice device;
SoundFile[] file;

// Define the number of samples 
int numsounds = 5;

int value[] = {0,0,0};

void setup(){
  size(640, 360);
  background(255);
  
  // Create a Sound renderer and an array of empty soundfiles
  device = new AudioDevice(this, 48000, 32);
  file = new SoundFile[numsounds];
  
  // Load 5 soundfiles from a folder in a for loop. By naming the files 1., 2., 3., n.aif it is easy to iterate
  // through the folder and load all files in one line of code.
  for (int i = 0; i < numsounds; i++){
    file[i] = new SoundFile(this, (i+1) + ".aif");
  }
  
}

void draw(){

    background(value[0],value[1],value[2]);

}



void keyPressed() {
  for (int i=0; i < 3; i++) {  
      value[i]=int(random(255));
  }
 
  switch(key){
  case 'a':
    file[0].play(0.5, 1.0);
    break;

  case 's':
    file[1].play(0.5, 1.0);
    break;
  
  case 'd':
    file[2].play(0.5, 1.0);
    break;
  
  case 'f':
    file[3].play(0.5, 1.0);
    break;
  
  case 'g':
    file[4].play(0.5, 1.0);
    break;
  
   case 'h':
    file[0].play(1.0, 1.0);
    break;
   
   case 'j':
    file[1].play(1.0, 1.0);
    break;

   case 'k':
    file[2].play(1.0, 1.0);
    break;
    
   case 'l':
    file[3].play(1.0, 1.0);
    break;
    
   case 'ö':
    file[4].play(1.0, 1.0);
    break;
    
   case 'ä':
    file[0].play(2.0, 1.0);
    break;
    
   case 'q':
    file[1].play(2.0, 1.0);
    break;
   
   case 'w':
    file[2].play(2.0, 1.0);
    break;    
   
   case 'e':
    file[3].play(2.0, 1.0);
   break;
   
   case 'r':
    file[4].play(2.0, 1.0);
   break; 
   
   case 't':
    file[0].play(3.0, 1.0);
    break;
    
   case 'z':
    file[1].play(3.0, 1.0);
    break;
   
   case 'u':
    file[2].play(3.0, 1.0);
    break;    
   
   case 'i':
    file[3].play(3.0, 1.0);
   break;
   
   case 'o':
    file[4].play(3.0, 1.0);
    break;
   
   case 'p':
    file[0].play(4.0, 1.0);
    break;    
   
   case 'ü':
    file[1].play(4.0, 1.0);
   break;   
  }
}
