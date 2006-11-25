import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class DatatypeConversion extends PApplet {public void setup() {// Datatype Conversion
// by REAS <http://reas.com>

// It is sometimes beneficial to convert a value from one type of 
// data to another. Each of the conversion functions converts its parameter 
// to an equivalent representation within its datatype.
// The conversion functions include int(), float(), char(), byte(), and others

// Created 09 December 2002

size(200, 200);
background(51);
noStroke();

char c;    // Chars are used for storing typographic symbols
float f;   // Floats are decimal numbers
int i;     // Ints are values between 2,147,483,647 and -2147483648
byte b;    // Bytes are values between -128 and 128

c = 'A';
f = PApplet.toFloat(c);     // Sets f = 65.0
i = PApplet.toInt(f * 1.4f); // Sets i to 91
b = PApplet.toByte(c / 2);  // Sets b to 32

rect(f, 0, 40, 66);
fill(204);
rect(i, 67, 40, 66);
fill(255);
rect(b, 134, 40, 66);
noLoop(); }}