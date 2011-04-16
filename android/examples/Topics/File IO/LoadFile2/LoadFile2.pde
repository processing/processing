/**
 * LoadFile 2
 * 
 * This example loads a data file about cars. Each element is separated
 * with a tab and corresponds to a different aspect of each car. The file stores 
 * the miles per gallon, cylinders, displacement, etc., for more than 400 different
 * makes and models. Press a mouse button to advance to the next group of entries.
 */

Record[] records;
String[] lines;
int recordCount;
PFont body;
int num = 9; // Display this many entries on each screen.
int startingEntry = 0;  // Display from this entry number

void setup() {
  size(200, 200);
  fill(255);
  noLoop();
  
  body = loadFont("TheSans-Plain-12.vlw");
  textFont(body);
  
  lines = loadStrings("cars2.tsv");
  records = new Record[lines.length];
  for (int i = 0; i < lines.length; i++) {
    String[] pieces = split(lines[i], TAB); // Load data into array
    if (pieces.length == 9) {
      records[recordCount] = new Record(pieces);
      recordCount++;
    }
  }
  if (recordCount != records.length) {
    records = (Record[]) subset(records, 0, recordCount);
  }
}

void draw() {
  background(0);
  for (int i = 0; i < num; i++) {
    int thisEntry = startingEntry + i;
    if (thisEntry < recordCount) {
      text(thisEntry + " > " + records[thisEntry].name, 20, 20 + i*20);
    }
  }
}

void mousePressed() {
  startingEntry += num; 
  if (startingEntry > records.length) {
    startingEntry = 0;  // go back to the beginning
  } 
  redraw();
}
