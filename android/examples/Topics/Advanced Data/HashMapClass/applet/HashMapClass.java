import processing.core.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class HashMapClass extends PApplet {

/**
 * HashMap example
 * by Daniel Shiffman.  
 * 
 * This example demonstrates how to use a HashMap to store 
 * a collection of objects referenced by a key.
 * This is much like an array, only instead of accessing elements
 * with a numeric index, we use a String.
 * If you are familiar with associative arrays from other languages,
 * this is the same idea.
 *
 * This example uses the HashMap to perform a simple concordance
 * http://en.wikipedia.org/wiki/Concordance_(publishing)
 */


// HashMap object
HashMap words;

// Array of all words from input file
String[] tokens;
int counter;

PFont f;

public void setup() {
  size(640, 360);
  words = new HashMap();

  // Load file and chop it up
  String[] lines = loadStrings("dracula.txt");
  String allText = join(lines," ");
  tokens = splitTokens(allText," ,.?!:;[]-");
  f = createFont("Georgia",36,true);  
}

public void draw() {
  background(255);
  fill(0);
  
  // Look at words one at a time
  String s = tokens[counter];
  counter = (counter + 1) % tokens.length;

  // Is the word in the HashMap
  if (words.containsKey(s)) {
    // Get the word object and increase the count
    // We access objects from a HashMap via its key, the String
    Word w = (Word) words.get(s);
    w.count(); 
  } else {
    // Otherwise make a new word
    Word w = new Word(s);
    // And add to the HashMap
    // put() takes two arguments, "key" and "value"
    // The key for us is the String and the value is the Word object
    words.put(s,w);    
  }

  // Make an iterator to look at all the things in the HashMap
  Iterator i = words.values().iterator();

  // x and y will be used to locate each word
  float x = 0;
  float y = height-10;

  while (i.hasNext()) {
    // Look at each word
    Word w = (Word) i.next();
    
    // Only display words that appear 3 times
    if (w.count > 3) {
      // The size is the count
      int fsize = constrain(w.count,0,100);
      textFont(f,fsize);
      text(w.word,x,y);
      // Move along the x-axis
      x+=textWidth(w.word+" ");
    }
    
    // If x gets to the end, move Y
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
class Word {
  
  int count;
  String word;
  
  Word(String s) {
    word = s;
    count = 1;
  }
  
  public void count() {
    count++;
  }

}  

  static public void main(String args[]) {
    PApplet.main(new String[] { "HashMapClass" });
  }
}
