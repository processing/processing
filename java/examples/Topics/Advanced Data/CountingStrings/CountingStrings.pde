/**
 * CountingString example
 * by Daniel Shiffman.  
 * 
 * This example demonstrates how to use a IntDict to store 
 * a number associated with a String.  Java HashMaps can also
 * be used for this, however, this example uses the IntDict
 * class offered by Processing's data package for simplicity
 * and added functionality. 
 *
 * This example uses the IntDict to perform a simple concordance
 * http://en.wikipedia.org/wiki/Concordance_(publishing)
 *
 */

// The next line is needed if running in JavaScript Mode with Processing.js
/* @pjs font="Georgia.ttf"; */

// An IntDict pairs Strings with integers
IntDict concordance;

// The raw array of words in 
String[] tokens;
int counter = 0;

void setup() {
  size(640, 360);

  concordance = new IntDict();

  // Load file and chop it up
  String[] lines = loadStrings("dracula.txt");
  String allText = join(lines, " ").toLowerCase();
  tokens = splitTokens(allText, " ,.?!:;[]-\"");

  // Create the font
  textFont(createFont("Georgia", 24));
}

void draw() {
  background(51);
  fill(255);

  // Look at words one at a time
  if (counter < tokens.length) {
    String s = tokens[counter];
    counter++;
    concordance.increment(s);
  }

  // x and y will be used to locate each word
  float x = 0;
  float y = 48;

  concordance.sortValues();

  String[] keys = concordance.keyArray();

  // Look at each word
  for (String word : keys) {
    int count = concordance.get(word);

    // Only display words that appear 3 times
    if (count > 3) {
      // The size is the count
      int fsize = constrain(count, 0, 48);
      textSize(fsize);
      text(word, x, y);
      // Move along the x-axis
      x += textWidth(word + " ");
    }

    // If x gets to the end, move y
    if (x > width) {
      x = 0;
      y += 48;
      // If y gets to the end, we're done
      if (y > height) {
        break;
      }
    }
  }
}

