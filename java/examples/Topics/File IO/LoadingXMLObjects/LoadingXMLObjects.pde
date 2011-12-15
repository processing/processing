/**
 * Loading XML Data
 * by Daniel Shiffman.  
 * 
 * This example demonstrates how to use loadXML()
 * to retrieve data from an XML document and make
 * objects from that data
 *
 * Here is what the XML looks like:
 *
 * <bubbles>
     <bubble>
       <diameter>40</diameter>
       <color red="75" green="255" blue="0"/>
     </bubble>
   </bubbles>
 */

// An array of Bubble objects
Bubble[] bubbles;

void setup() {
  size(640, 360);
  smooth();
  // Load an XML document
  XML xml = loadXML("bubbles.xml");

  // Get all the child elements
  XML[] children = xml.getChildren("bubble");

  // Make an array of objects the same size
  bubbles = new Bubble[children.length];

  for (int i = 0; i < children.length; i ++ ) {
    
    // The diameter is the content of the child named "Diamater"
    XML diameterElement = children[i].getChild("diameter");
    int diameter = int(diameterElement.getContent());

    // The color element has three attributes
    XML colorElement = children[i].getChild("color");
    // An int for r g and b
    int r = colorElement.getInt("red");
    int g = colorElement.getInt("green");
    int b = colorElement.getInt("blue");

    // Make a new Bubble object with values from XML document
    bubbles[i] = new Bubble(r, g, b, diameter);
  }
}


void draw() {
  background(255);

  // Display and move all bubbles
  for (int i = 0; i < bubbles.length; i++ ) {
    bubbles[i].display();
    bubbles[i].drift();
  }
}

