/**
 * HashMap example
 * by Daniel Shiffman.  
 * 
 * This example demonstrates how to use a HashMap to store 
 * a collection of objects referenced by a key. This is much like an array, 
 * only instead of accessing elements with a numeric index, we use a String.
 * If you are familiar with associative arrays from other languages,
 * this is the same idea.
 *
 * This example uses the HashMap to perform a simple concordance
 * http://en.wikipedia.org/wiki/Concordance_(publishing)
 */

// The next line is needed if running in JavaScript Mode with Processing.js
/* @pjs font="Georgia.ttf"; */

HashMap<String, Word> words;  // HashMap object

String[] tokens;  // Array of all words from input file
int counter;

void setup() {
  size(640, 360);
  
  words = new HashMap();

  // Load file and chop it up
  String[] lines = loadStrings("dracula.txt");
  String allText = join(lines, " ");
  tokens = splitTokens(allText, " ,.?!:;[]-");
  
  // Create the font
  textFont(createFont("Georgia", 24)); 
}

void draw() {
  background(51);
  fill(255);
  
  // Look at words one at a time
  String s = tokens[counter];
  counter = (counter + 1) % tokens.length;

  // Is the word in the HashMap
  if (words.containsKey(s)) {
    // Get the word object and increase the count
    // We access objects from a HashMap via its key, the String
    Word w = words.get(s);
    w.count(); 
  } else {
    // Otherwise make a new word
    Word w = new Word(s);
    // And add to the HashMap put() takes two arguments, "key" and "value"
    // The key for us is the String and the value is the Word object
    words.put(s, w);    
  }

  // x and y will be used to locate each word
  float x = 0;
  float y = height-10;

  // Look at each word
  for (Word w : words.values()) {
    
    // Only display words that appear 3 times
    if (w.count > 3) {
      // The size is the count
      int fsize = constrain(w.count, 0, 100);
      textSize(fsize);
      text(w.word, x, y);
      // Move along the x-axis
      x += textWidth(w.word + " ");
    }
    
    // If x gets to the end, move y
    if (x > width) {
      x = 0;
      y -= 100;
      // If y gets to the end, we're done
      if (y < 0) {
        break; 
      }
    }
  } 
}

