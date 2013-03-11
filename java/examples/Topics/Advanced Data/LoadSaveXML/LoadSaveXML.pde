/**
 * Loading XML Data
 * by Daniel Shiffman.  
 * 
 * This example demonstrates how to use loadXML()
 * to retrieve data from an XML file and make objects 
 * from that data.
 *
 * Here is what the XML looks like:
 *
<?xml version="1.0"?>
<bubbles>
  <bubble>
    <position x="160" y="103"/>
    <diameter>43.19838</diameter>
    <label>Happy</label>
  </bubble>
  <bubble>
    <position x="372" y="137"/>
    <diameter>52.42526</diameter>
    <label>Sad</label>
  </bubble>
</bubbles>
 */
 
// An Array of Bubble objects
Bubble[] bubbles;
// A Table object
XML xml;

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
  // Load XML file
  xml = loadXML("data.xml");
  // Get all the child nodes named "bubble"
  XML[] children = xml.getChildren("bubble");

  // The size of the array of Bubble objects is determined by the total XML elements named "bubble"
  bubbles = new Bubble[children.length]; 

  for (int i = 0; i < bubbles.length; i++) {
    
    
    // The position element has two attributes: x and y
    XML positionElement = children[i].getChild("position");
    // Note how with attributes we can get an integer or float directly
    float x = positionElement.getInt("x");
    float y = positionElement.getInt("y");
    
    // The diameter is the content of the child named "diamater"
    XML diameterElement = children[i].getChild("diameter");
    // Note how with the content of an XML node, we retrieve as a String and then convert
    float diameter = float(diameterElement.getContent());

    // The label is the content of the child named "label"
    XML labelElement = children[i].getChild("label");
    String label = labelElement.getContent();

    // Make a Bubble object out of the data read
    bubbles[i] = new Bubble(x, y, diameter, label);
  }  

}

// Still need to work on adding and deleting

/*void mousePressed() {
  // Create a new row
  TableRow row = table.addRow();
  // Set the values of that row
  row.setFloat("x", mouseX);
  row.setFloat("y", mouseY);
  row.setFloat("diameter", random(40, 80));
  row.setString("name", "Blah");
  
  // If the table has more than 10 rows
  if (table.getRowCount() > 10) {
    // Delete the oldest row
    table.removeRow(0); 
  }

  // Writing the CSV back to the same file
  saveTable(table,"data/data.csv");
  // And reloading it
  loadData();
}*/

