import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Array extends PApplet {public void setup() {// Array
// by REAS <http://reas.com>

// An array is a list of data. Each piece of data in an array 
// is identified by an index number representing its position in 
// the array. Arrays are zero based, which means that the first 
// element in the array is [0], the second element is [1], and so on.
// In this example, an array named "coswav" is created and
// filled with the cosine values. This data is displayed three 
// separate ways on the screen. 

// Created 09 December 2002


size(200, 200);

float[] coswave = new float[width];

for(int i=0; i<width; i++) {
  float ratio = (float)i/(float)width;
  coswave[i] = abs( cos(ratio*PI) );
}

for(int i=0; i<width; i++) {
  stroke(coswave[i]*255);
  line(i, 0, i, width/3);
}

for(int i=0; i<width; i++) {
  stroke(coswave[i]*255/4);
  line(i, width/3, i, width/3*2);
}

for(int i=0; i<width; i++) {
  stroke(255-coswave[i]*255);
  line(i, width/3*2, i, height);
}
noLoop(); }}