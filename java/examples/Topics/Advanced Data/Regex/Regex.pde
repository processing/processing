/**
 * Regular Expression example
 * by Daniel Shiffman.  
 * 
 * This example demonstrates how to use matchAll() to create
 * a list of all matches of a given regex.
 *
 * Here we'll load the raw HTML from a URL and search for any
 * <a href=" "> links
 */

// Our source url
String url = "http://processing.org";
// We'll store the results in an array
String[] links;

void setup() {
  size(640, 360);
  smooth();
  // Load the links
  links = loadLinks(url);
}

void draw() {
  background(0);
  // Display the raw links
  fill(255);
  for (int i = 0; i < links.length; i++) {
    text(links[i],10,16+i*16);
  }
}

String[] loadLinks(String s) {
  // Load the raw HTML
  String[] lines = loadStrings(s);
  // Put it in one big string
  String html = join(lines,"\n");
  
  // A wacky regex for matching a URL
  String regex = "<\\s*a\\s+href\\s*=\\s*\"(.*?)\"";
  // The matches are in a two dimensional array
  // The first dimension is all matches
  // The second dimension is the groups
  String[][] matches = matchAll(html, regex);
  
  // An array for the results
  String[] results = new String[matches.length];
  
  // We want group 1 for each result
  for (int i = 0; i < results.length; i++) {
     results[i] = matches[i][1];
  }
  
  // Return the results
  return results;
}

