FloatTable data; 
float dataMin, dataMax; 

float plotX1, plotY1; 
float plotX2, plotY2; 

int yearMin, yearMax; 
int[] years; 


void setup() { 
  size(720, 405); 

  data = new FloatTable("milk-tea-coffee.tsv"); 

  years = int(data.getRowNames()); 
  yearMin = years[0]; 
  yearMax = years[years.length - 1]; 
  
  dataMin = 0; 
  dataMax = data.getTableMax(); 

  // Corners of the plotted time series 
  plotX1 = 50; 
  plotX2 = width - plotX1; 
  plotY1 = 60; 
  plotY2 = height - plotY1; 

  smooth(); 
}


void draw() { 
  background(224); 
  
  // Show the plot area as a white box 
  fill(255); 
  rectMode(CORNERS); 
  noStroke(); 
  rect(plotX1, plotY1, plotX2, plotY2); 

  strokeWeight(5); 
  // Draw the data for the first column 
  stroke(#5679C1); 
  drawDataPoints(0); 
} 


// Draw the data as a series of points 
void drawDataPoints(int col) { 
  int rowCount = data.getRowCount(); 
  for (int row = 0; row < rowCount; row++) { 
    if (data.isValid(row, col)) { 
      float value = data.getFloat(row, col); 
      float x = map(years[row], yearMin, yearMax, plotX1, plotX2); 
      float y = map(value, dataMin, dataMax, plotY2, plotY1); 
      point(x, y); 
    } 
  } 
} 
