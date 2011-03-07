/**
 * Characters Strings. 
 * 
 * Click on the image to give it focus and then type letters to
 * shift the location of the image. 
 * Characters are typographic symbols such as A, d, and %.  
 * The character datatype, abbreviated as char, stores letters and 
 * symbols in the Unicode format, a coding system developed to support 
 * a variety of world languages. Characters are distinguished from other
 * symbols by putting them between single quotes ('P'). 
 * A string is a sequence of characters. A string is noted by surrounding 
 * a group of letters with double quotes ("Processing"). 
 * Chars and strings are most often used with the keyboard methods, 
 * to display text to the screen, and to load images or files. 
 */
 
PImage frog;
PFont font;
int xoffset;
char letter;

void setup() 
{
  size(200, 200, P2D);

  font = loadFont("Eureka-90.vlw"); 
  textFont(font); 
  // Draw text more accurately and efficiently.
  textMode(SCREEN);
  textAlign(CENTER);
  
  // The String datatype must be capitalized because it is a complex datatype.
  // A String is actually a class with its own methods, some of which are
  // featured below.
  String name = "rathausFrog";
  String extension = ".jpg";
  int nameLength = name.length();
  println("The length of " + name + " is " + nameLength + ".");
  name = name.concat(extension);
  nameLength = name.length();
  println("The length of " + name + " is " + nameLength + ".");

  // The parameter for the loadImage() method must be a string
  // This line could also be written "frog = loadImage("rathausFrog.jpg");
  frog = loadImage(name);
}

void draw() 
{
  background(51); // Set background to dark gray

  // Same as image(frog, xoffset, 0), but more efficient 
  // because no transformations or tint() or smooth() are used.
  set(xoffset, 0, frog);

  // Draw an X
  line(0, 0, width, height);  
  line(0, height, width, 0); 

//  // Get the width of the letter
//  float letterWidth = textWidth(letter);
//
  // Draw the letter to the center of the screen
  text(letter, width/2, height/2);
}

void keyPressed()
{
  // The variable "key" always contains the value of the most recent key pressed.
  // If the key is an upper or lowercase letter between 'A' and 'z'
  // the image is shifted to the corresponding value of that key
  if (key >= 'A' && key <= 'z') {
    // Map the index of the key pressed from the range between 'A' and 'z',
    // into a position for the left edge of the image. The maximum xoffset
    // is the width of the drawing area minus the size of the image.
    xoffset = int(map(key, 'A', 'z', 0, width - frog.width));
    // Update the letter shown to the screen
    letter = key;
    // Write the letter to the console
    println(key);
  }
}
