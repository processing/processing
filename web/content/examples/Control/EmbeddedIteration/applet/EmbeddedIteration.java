import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class EmbeddedIteration extends PApplet {public void setup() {// Embedding Iteration
// by REAS <http://reas.com>

// Embedding "for" structures allows repetition in two dimensions.

// Updated 21 August 2002

float box_size = 11; 
float box_space = 12; 
int margin = 7; 
 
size(200, 200); 
background(0); 
noStroke(); 
 
// Draw gray boxes 
 
for(int i = margin; i < width-margin; i += box_space) { 
  for(int j = margin; j < height-margin; j += box_space) { 
    fill(255 - box_size*10); 
    rect(j, i, box_size, box_size); 
  } 
  box_size = box_size - 0.6f; 
} 

		
noLoop(); }}