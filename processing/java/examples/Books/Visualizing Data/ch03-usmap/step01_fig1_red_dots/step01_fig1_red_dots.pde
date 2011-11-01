PImage mapImage;
Table locationTable;
int rowCount;

void setup() {
  size(640, 400);
  mapImage = loadImage("map.png");
  // Make a data table from a file that contains 
  // the coordinates of each state.
  locationTable = new Table("locations.tsv");
  // The row count will be used a lot, store it locally.
  rowCount = locationTable.getRowCount();
}

void draw() {
  background(255);
  image(mapImage, 0, 0);

  // Drawing attributes for the ellipses   
  smooth();
  fill(192, 0, 0);
  noStroke();

  // Loop through the rows of the locations file and draw the points  
  for (int row = 0; row < rowCount; row++) {
    float x = locationTable.getFloat(row, 1);  // column 1
    float y = locationTable.getFloat(row, 2);  // column 2
    ellipse(x, y, 9, 9);
  }
}
