/**
 * Loading XML Data
 * by Daniel Shiffman.  
 * 
 * This example demonstrates how to use loadJSON()
 * to retrieve data from a JSON file and make objects 
 * from that data.
 *
 * Here is what the JSON looks like (partial):
 *
 {
 "bubbles": [
 {
 "position": {
 "x": 160,
 "y": 103
 },
 "diameter": 43.19838,
 "label": "Happy"
 },
 {
 "position": {
 "x": 372,
 "y": 137
 },
 "diameter": 52.42526,
 "label": "Sad"
 }
 ]
 }
 */

// An Array of Bubble objects
Bubble[] bubbles;
// A JSON object
JSONObject json;

void setup() {
  size(640, 360);
  loadData();
}

void draw() {
  background(255);
  // Display all bubbles
  for (Bubble b : bubbles) {
    b.display();
    b.rollover(mouseX, mouseY);
  }
  //
  //  textAlign(LEFT);
  //  fill(0);
  //  text("Click to add bubbles.", 10, height-10);
}


void loadData() {
  // Load JSON file
  String jsonString = join(loadStrings("data.json"), "\n");
  //println(jsonString);

  json =  JSONObject.parse(jsonString);
  println(json);

  JSONArray bubbleData = json.getJSONArray("bubbles");

  // The size of the array of Bubble objects is determined by the total XML elements named "bubble"
  bubbles = new Bubble[bubbleData.size()]; 

  for (int i = 0; i < bubbleData.size(); i++) {
    /*JSONObject bubble = bubbleData.getJSONObject(i); 
    JSONObject position = bubble.getJSONObject("position");
    int x = position.getInt("x");
    int y = position.getInt("y");

    float diameter = (float)bubble.getDouble("diameter");
    String label = bubble.getString("label");

    bubbles[i] = new Bubble(x, y, diameter, label);*/
  }
}

// Still need to work on adding and deleting

void mousePressed() {

  // Create a new XML bubble element
  //  XML bubble = xml.addChild("bubble");
  //  
  //  // Set the poisition element
  //  XML position = bubble.addChild("position");
  //  // Here we can set attributes as integers directly
  //  position.setInt("x",mouseX);
  //  position.setInt("y",mouseY);
  //  
  //  // Set the diameter element
  //  XML diameter = bubble.addChild("diameter");
  //  // Here for a node's content, we have to convert to a String
  //  diameter.setContent("" + random(40,80));
  //  
  //  // Set a label
  //  XML label = bubble.addChild("label");
  //  label.setContent("New label");
  //  
  //  
  //  // Here we are removing the oldest bubble if there are more than 10
  //  XML[] children = xml.getChildren("bubble");
  //    // If the XML file has more than 10 bubble elements
  //  if (children.length > 10) {
  //    // Delete the first one
  //    xml.removeChild(children[0]);
  //  }
  //  
  //  // Save a new XML file
  //  saveXML(xml,"data/data.xml");
  //  
  //  // reload the new data 
  //  loadData();
}

