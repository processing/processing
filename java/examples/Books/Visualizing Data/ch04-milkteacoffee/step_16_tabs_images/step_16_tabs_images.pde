// The images used in this example are identical to the text-only tabs used in the
// previous step. Some might say that's rather unimaginative. Others might see it
// as an opportunity to produce nicer tab images to replace them.

FloatTable data;
float dataMin, dataMax;

float plotX1, plotY1;
float plotX2, plotY2;
float labelX, labelY;

int rowCount;
int columnCount;
int currentColumn = 0;

int yearMin, yearMax;
int[] years;

int yearInterval = 10;
int volumeInterval = 10;

PFont plotFont; 


void setup() {
  size(720, 405);
  
  data = new FloatTable("milk-tea-coffee.tsv");
  rowCount = data.getRowCount();
  columnCount = data.getColumnCount();
  
  years = int(data.getRowNames());
  yearMin = years[0];
  yearMax = years[years.length - 1];
  
  dataMin = 0;
  dataMax = ceil(data.getTableMax() / volumeInterval) * volumeInterval;

  // Corners of the plotted time series
  plotX1 = 120; 
  plotX2 = width - 80;
  labelX = 50;
  plotY1 = 60;
  plotY2 = height - 70;
  labelY = height - 25;
  
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

  drawTitleTabs();
  drawAxisLabels();

  drawYearLabels();
  drawVolumeLabels();

  noStroke();
  fill(#5679C1);
  drawDataArea(currentColumn);   
}


void drawTitle() {
  fill(0);
  textSize(20);
  textAlign(LEFT);
  String title = data.getColumnName(currentColumn);
  text(title, plotX1, plotY1 - 10);
}


float[] tabLeft, tabRight;  // Add above setup()
float tabTop, tabBottom;
float tabPad = 0;           // No padding necessary when using images
PImage[] tabImageNormal;
PImage[] tabImageHighlight;

void drawTitleTabs() {
  rectMode(CORNERS);
  noStroke();
  textSize(20);
  textAlign(LEFT);

  // Allocate the tab position array, and load the tab images.
  if (tabLeft == null) {
    tabLeft = new float[columnCount];
    tabRight = new float[columnCount];
    
    tabImageNormal = new PImage[columnCount];
    tabImageHighlight = new PImage[columnCount];
    for (int col = 0; col < columnCount; col++) {
      String title = data.getColumnName(col);
      tabImageNormal[col] = loadImage(title + "-unselected.png");
      tabImageHighlight[col] = loadImage(title + "-selected.png");
    }
  }
  
  float runningX = plotX1; 
  tabBottom = plotY1;
  // Size based on the height of the tabs by checking the
  // height of the first (all images are the same height)
  tabTop = plotY1 - tabImageNormal[0].height;
  
  for (int col = 0; col < columnCount; col++) {
    String title = data.getColumnName(col);
    tabLeft[col] = runningX; 
    float titleWidth = tabImageNormal[col].width;
    tabRight[col] = tabLeft[col] + tabPad + titleWidth + tabPad;
    
    PImage tabImage = (col == currentColumn) ? 
      tabImageHighlight[col] : tabImageNormal[col];
    image(tabImage, tabLeft[col], tabTop);
    
    runningX = tabRight[col];
  }
}


void mousePressed() {
  if (mouseY > tabTop && mouseY < tabBottom) {
    for (int col = 0; col < columnCount; col++) {
      if (mouseX > tabLeft[col] && mouseX < tabRight[col]) {
        setCurrent(col);
      }
    }
  }
}


void setCurrent(int col) {
  currentColumn = col;
}


void drawAxisLabels() {
  fill(0);
  textSize(13);
  textLeading(15);
  
  textAlign(CENTER, CENTER);
  text("Gallons\nconsumed\nper capita", labelX, (plotY1+plotY2)/2);
  textAlign(CENTER);
  text("Year", (plotX1+plotX2)/2, labelY);
}


void drawYearLabels() {
  fill(0);
  textSize(10);
  textAlign(CENTER);
  
  // Use thin, gray lines to draw the grid
  stroke(224);
  strokeWeight(1);
  
  for (int row = 0; row < rowCount; row++) {
    if (years[row] % yearInterval == 0) {
      float x = map(years[row], yearMin, yearMax, plotX1, plotX2);
      text(years[row], x, plotY2 + textAscent() + 10);
      line(x, plotY1, x, plotY2);
    }
  }
}


int volumeIntervalMinor = 5;   // Add this above setup()

void drawVolumeLabels() {
  fill(0);
  textSize(10);
  textAlign(RIGHT);
  
  stroke(128);
  strokeWeight(1);

  for (float v = dataMin; v <= dataMax; v += volumeIntervalMinor) {
    if (v % volumeIntervalMinor == 0) {     // If a tick mark
      float y = map(v, dataMin, dataMax, plotY2, plotY1);  
      if (v % volumeInterval == 0) {        // If a major tick mark
        float textOffset = textAscent()/2;  // Center vertically
        if (v == dataMin) {
          textOffset = 0;                   // Align by the bottom
        } else if (v == dataMax) {
          textOffset = textAscent();        // Align by the top
        }
        text(floor(v), plotX1 - 10, y + textOffset);
        line(plotX1 - 4, y, plotX1, y);     // Draw major tick
      } else {
        //line(plotX1 - 2, y, plotX1, y);     // Draw minor tick
      }
    }
  }
}


void drawDataArea(int col) {
  beginShape();
  for (int row = 0; row < rowCount; row++) {
    if (data.isValid(row, col)) {
      float value = data.getFloat(row, col);
      float x = map(years[row], yearMin, yearMax, plotX1, plotX2);
      float y = map(value, dataMin, dataMax, plotY2, plotY1);
      vertex(x, y);
    }
  }
  // Draw the lower-right and lower-left corners
  vertex(plotX2, plotY2);
  vertex(plotX1, plotY2);
  endShape(CLOSE);
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
