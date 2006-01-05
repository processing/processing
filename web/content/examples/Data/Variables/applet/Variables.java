import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Variables extends PApplet {public void setup() {// Variables
// by REAS <http://reas.com>

// Variables are used for storing values.
// Changing the values of variables 'a' and 'b' significantly change the composition. 

// Created 2 September 2002

size(200, 200);
background(0);
stroke(153);

int a = 20;
int b = 50;
int c = a*8;
int d = a*9;
int e = b-a;
int f = b*2;
int g = f+e;

line(a, f, b, g);
line(b, e, b, g);
line(b, e, d, c);
line(a, e, d-e, c);
noLoop(); }}