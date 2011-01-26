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

void setup() 
{
  size(200, 200);
  fill(255);
  noLoop();
  
  body = loadFont("TheSans-Plain-12.vlw");
  textFont(body);
  
  lines = loadStrings("cars2.tsv");
  records = new Record[lines.length];
  for (int i = 0; i < lines.length; i++) {
    String[] pieces = split(lines[i], '\t'); // Load data into arrayif (pieces.length == 9) {
    records[recordCount] = new Record(pieces);
    recordCount++;
  }
}

void draw() {
  background(0);
  for (int i = 0; i < num; i++) {
    int thisEntry = startingEntry + i;
    text(thisEntry + " > " + records[thisEntry].name, 20, 20 + i*20); // Print name to console
  }
}

void mousePressed() {
  startingEntry += num; 
  if (startingEntry + num > records.length) {
    startingEntry -= num;
  } 
  redraw();
}

class Record {
  String name;
  float mpg;
  int cylinders;
  float displacement;
  float horsepower;
  float weight;
  float acceleration;
  int year;
  float origin;
  public Record(String[] pieces) {
    name = pieces[0];
    mpg = float(pieces[1]);
    cylinders = int(pieces[2]);
    displacement = float(pieces[3]);
    horsepower = float(pieces[4]);
    weight = float(pieces[5]);
    acceleration = float(pieces[6]);
    year = int(pieces[7]);
    origin = float(pieces[8]);
  }
}
