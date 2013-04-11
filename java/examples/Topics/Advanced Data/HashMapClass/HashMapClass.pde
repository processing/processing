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
 * A simpler example is CountingStrings which uses IntHash instead of 
 * HashMap.  The Processing classes IntHash, FloatHash, and StringHash
 * offer a simpler way of pairing Strings with numbers or other Strings.
 * Here we use a HashMap because we want to pair a String with a custom
 * object, in this case a "Word" object that stores two numbers.
 *
 * In this example, words that appear in one book (Dracula) only are colored white 
 * while words the other (Frankenstein) are colored black.
 */

// The next line is needed if running in JavaScript Mode with Processing.js
/* @pjs font="Georgia.ttf"; */

HashMap<String, Word> words;  // HashMap object

void setup() {
  size(640, 360);
  
  // Create the HashMap
  words = new HashMap<String, Word>();

  // Load two files
  loadFile("dracula.txt");
  loadFile("frankenstein.txt");

  // Create the font
  textFont(createFont("Georgia", 24));
}

void draw() {
  background(126);
  
  // Show words
  for (Word w : words.values()) {
    if (w.qualify()) {
        w.display(); 
        w.move();
    }
  }  
}

// Load a file
void loadFile(String filename) {
  String[] lines = loadStrings(filename);
  String allText = join(lines, " ").toLowerCase();
  String[] tokens = splitTokens(allText, " ,.?!:;[]-\"'");
  
  for (String s : tokens) {
    // Is the word in the HashMap
    if (words.containsKey(s)) {
      // Get the word object and increase the count
      // We access objects from a HashMap via its key, the String
      Word w = words.get(s);
      // Which book am I loading?
      if (filename.contains("dracula")) {
        w.incrementDracula();
      } 
      else if (filename.contains("frankenstein")) {
        w.incrementFranken();
      }
    } 
    else {
      // Otherwise make a new word
      Word w = new Word(s);
      // And add to the HashMap put() takes two arguments, "key" and "value"
      // The key for us is the String and the value is the Word object
      words.put(s, w);
      if (filename.contains("dracula")) {
        w.incrementDracula();
      } else if (filename.contains("frankenstein")) {
        w.incrementFranken();
      }
    }
  }
}

