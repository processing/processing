PImage mapImage;
Table locationTable;
int rowCount;

Table dataTable;
float dataMin = MAX_FLOAT;
float dataMax = MIN_FLOAT;


void setup() {
  size(640, 400);
  mapImage = loadImage("map.png");
  locationTable = new Table("locations.tsv");
  rowCount = locationTable.getRowCount();
  
  // Read the data table
  dataTable = new Table("random.tsv");

  // Find the minimum and maximum values
  for (int row = 0; row < rowCount; row++) {
    float value = dataTable.getFloat(row, 1);
    if (value > dataMax) {
      dataMax = value;
    }
    if (value < dataMin) {
      dataMin = value;
    }
  }

  smooth();
  noStroke();  
}


void draw() {
  background(255);
  image(mapImage, 0, 0);
  
  for (int row = 0; row < rowCount; row++) {
    String abbrev = dataTable.getRowName(row);
    float x = locationTable.getFloat(abbrev, 1);
    float y = locationTable.getFloat(abbrev, 2);
    drawData(x, y, abbrev);
  }
}


void drawData(float x, float y, String abbrev) {
  float value = dataTable.getFloat(abbrev, 1);  
  float diameter = 0;
  if (value >= 0) {
    diameter = map(value, 0, dataMax, 3, 30);
    fill(#333366);  // blue
  } else {
    diameter = map(value, 0, dataMin, 3, 30);
    fill(#ec5166);  // red
  }
  ellipse(x, y, diameter, diameter);
}
