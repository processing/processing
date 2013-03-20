/**
 * Loading JSON Data
 * by Daniel Shiffman.  
 * 
 * This example demonstrates how to use loadJSON()
 * to retrieve data from a JSON file and make objects 
 * from that data.
 *
 * Here is what the JSON looks like (partial):
 *
{
  "bubbles": {
    "bubble": [
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
          "x": 121,
          "y": 179
        },
        "diameter": 44.758068,
        "label": "Melancholy"
      }
    ]
  }
}
 */

// An Array of Bubble objects
Bubble[] bubbles;
// A Table object
JSONArray json;

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

  textAlign(LEFT);
  fill(0);
  text("Click to add bubbles.", 10, height-10);
}

void loadData() {
  // Load JSON file
  String jsonString = join(loadStrings("data.json"), "\n");
  //println(jsonString);

  // Load the entire document in a JSONObject
  JSONObject root = JSONObject.parse(jsonString);

  // Fetch the internal JSONObject called "bubbles" (all the bubbles are stored inside this object)
  JSONObject json_bubbles = root.getJSONObject("bubbles");

  // Load the JSONArray of bubbles
  json = json_bubbles.getJSONArray("bubble");

  // The size of the array of Bubble objects is determined by the total XML elements named "bubble"
  bubbles = new Bubble[json.size()]; 

  for (int i = 0; i < bubbles.length; i++) {

    // Get the JSONObject representing the bubble
    JSONObject bubble = json.getObject(i);

    // The position element has two attributes: x and y
    float x = bubble.getJSONObject("position").getInt("x");
    float y = bubble.getJSONObject("position").getInt("y");

    // The diameter is the content of the child named "diamater"
    float diameter = bubble.getFloat("diameter");

    // The label is the content of the child named "label"
    String label = bubble.getString("label");

    // Make a Bubble object out of the data read
    bubbles[i] = new Bubble(x, y, diameter, label);
  }
}

void mousePressed() {

  // Create a new JSON bubble element
  JSONObject newBubble = new JSONObject();

  // Set the poisition element
  JSONObject position = new JSONObject();
  position.setInt("x", mouseX);
  position.setInt("y", mouseY);
  newBubble.setJSONObject("position", position);

  // Set the diameter element
  newBubble.setFloat("diameter", random(40, 80));

  // Set a label
  newBubble.setString("label", "New label");

  // Append the new bubble to the JSONArray json
  json.append( newBubble );
	
  // Here we are removing the oldest bubble if there are more than 10
  // If the XML file has more than 10 bubble elements
  if (json.size() > 10) {
    // Delete the first one
    json.removeIndex(0);
  }
	
  JSONObject jsonBubbles = new JSONObject();
  jsonBubbles.setJSONArray( "bubble", json );
	
  JSONObject root = new JSONObject();
  root.setJSONObject( "bubbles", jsonBubbles );
	
  // Save a new XML file
  saveStrings("data/data.json", split(root.toString(), "\n"));

  // reload the new data 
 loadData();
}
