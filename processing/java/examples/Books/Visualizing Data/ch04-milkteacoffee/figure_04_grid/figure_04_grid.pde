FloatTable data;
float dataMin, dataMax;

float plotX1, plotY1;
float plotX2, plotY2;

int rowCount;
int columnCount;
int currentColumn = 0;

int yearMin, yearMax;
int[] years;

int yearInterval = 10;

PFont plotFont; 


void setup() {
  size(720, 405);
  
  data = new FloatTable("milk-tea-coffee.tsv");
  rowCount = data.getRowCount();
  columnCount = data.getColumnCount();
  
  years = int(data.getRowNames());
  yearMin = years[0];
  yearMax = years[years.length - 1];
  
  //println(data.getTableMin());
  dataMin = 0; //data.getTableMin();
  dataMax = data.getTableMax();

  // Corners of the plotted time series
  plotX1 = 50; 
  plotX2 = width - plotX1;
  plotY1 = 60;
  plotY2 = height - plotY1;  
  
  plotFont = createFont("SansSerif", 20);
  textFont(plotFont);

  smooth();
}


void draw() {
  background(224);
  
  // Show the plot area as a white box  
  fill(255);
  rectMode(CORNERS);
  noStroke();
  rect(plotX1, plotY1, plotX2, plotY2);

  // Draw the title of the current plot
  drawTitle();
  drawYearLabels();

  stroke(#5679C1);
  strokeWeight(5);
  drawDataPoints(currentColumn);
}


void drawTitle() {
  fill(0);
  textSize(20);
  textAlign(LEFT);
  String title = data.getColumnName(currentColumn);
  text(title, plotX1, plotY1 - 10);
}


void drawYearLabels() {
  fill(0);
  textSize(10);
  textAlign(CENTER, TOP);
  
  // Use thin, gray lines to draw the grid
  stroke(224);
  strokeWeight(1);
  
  for (int row = 0; row < rowCount; row++) {
    if (years[row] % yearInterval == 0) {
      float x = map(years[row], yearMin, yearMax, plotX1, plotX2);
      text(years[row], x, plotY2 + 10);
      line(x, plotY1, x, plotY2);
    }
  }
}


void drawDataPoints(int col) {
  for (int row = 0; row < rowCount; row++) {
    if (data.isValid(row, col)) {
      float value = data.getFloat(row, col);
      float x = map(years[row], yearMin, yearMax, plotX1, plotX2);
      float y = map(value, dataMin, dataMax, plotY2, plotY1);
      point(x, y);
    }
  }
}


void drawDataLine(int col) {  
  beginShape();
  for (int row = 0; row < rowCount; row++) {
    if (data.isValid(row, col)) {
      float value = data.getFloat(row, col);
      float x = map(years[row], yearMin, yearMax, plotX1, plotX2);
      float y = map(value, dataMin, dataMax, plotY2, plotY1);      
      vertex(x, y);
    }
  }
  endShape();
}


void drawDataHighlight(int col) {
  for (int row = 0; row < rowCount; row++) {
    if (data.isValid(row, col)) {
      float value = data.getFloat(row, col);
      float x = map(years[row], yearMin, yearMax, plotX1, plotX2);
      float y = map(value, dataMin, dataMax, plotY2, plotY1);
      if (dist(mouseX, mouseY, x, y) < 3) {
        strokeWeight(10);
        point(x, y);
        fill(0);
        textSize(10);
        textAlign(CENTER);
        text(nf(value, 0, 2) + " (" + years[row] + ")", x, y-8);
        textAlign(LEFT);
      }
    }
  }
}


void drawDataCurve(int col) {  
  //stroke(0);
  //noStroke();
  beginShape();
  for (int row = 0; row < rowCount; row++) {
    if (data.isValid(row, col)) {
      float value = data.getFloat(row, col);
      float x = map(years[row], yearMin, yearMax, plotX1, plotX2);
      //float x = map(row, 0, rowCount-1, plotX1, plotX2);
      float y = map(value, dataMin, dataMax, plotY2, plotY1);
      
      //ellipse(x, y, 5, 5);  // only change for ellipses
      curveVertex(x, y);
      // double the curve points for the start and stop
      if ((row == 0) || (row == rowCount-1)) {
        curveVertex(x, y);
      }
    }
  }
  endShape();
}


void drawDataArea(int col) {
  float leftEdge = width;
  float rightEdge = 0;
  
  noStroke();
  beginShape();
  for (int row = 0; row < rowCount; row++) {
    if (data.isValid(row, col)) {
      float value = data.getFloat(row, col);
      float x = map(years[row], yearMin, yearMax, plotX1, plotX2);
      float y = map(value, dataMin, dataMax, plotY2, plotY1);
      
      if (x < leftEdge) {
        leftEdge = x;
      }      
      if (x > rightEdge) {
        rightEdge = x;
      }
      
      vertex(x, y);
    }
  }
  // draw the lower-right and lower-left corners
  vertex(rightEdge, plotY2);
  vertex(leftEdge, plotY2);
  endShape(CLOSE);
}


void drawDataEllipses(int col) {
  ellipseMode(CENTER);
  for (int row = 0; row < rowCount; row++) {
    if (data.isValid(row, col)) {
      float value = data.getFloat(row, col);
      //float x = map(row, 0, rowCount-1, plotX1, plotX2);
      float x = map(years[row], yearMin, yearMax, plotX1, plotX2);
      float y = map(value, dataMin, dataMax, plotY2, plotY1);
      ellipse(x, y, 5, 5);
    }
  }
}


void keyPressed() {
  if (key == '[') {
    currentColumn--;
    if (currentColumn < 0) {
      currentColumn = columnCount - 1;
    }
  } else if (key == ']') {
    currentColumn++;
    if (currentColumn == columnCount) {
      currentColumn = 0;
    }
  }
}


  /*
  // print the min and max
  println(dataMin + " " + dataMax);
  */
  
  /*
  // print column names
  for (int i = 0; i < data.getColumnCount(); i++) {
    println(data.getColumnName(i));
  }
  */

  /*
  // print row names
  for (int i = 0; i < data.getRowCount(); i++) {
    println(data.getRowName(i));
  }
  */
  
  /*
  // print a row of data
  int row = 4;
  for (int i = 0; i < data.getColumnCount(); i++) {
    print(data.getFloat(row, i) + "\t");
  }
  println();
  */  
