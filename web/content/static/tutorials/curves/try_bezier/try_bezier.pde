import guicomponents.*;

/* Four buttons: 2 draw points, 2 control points */
GButton[] buttonList = new GButton[4];
String[] buttonLegend = {
  "P1", "P2", "C1", "C2"};

/* Eight text fields; X and Y coords for four points */
GTextField[] textField = new GTextField[8];

/*
 * Initial position of the draw points
 * and control points, in that order.
 */
int [] xPos = { 
  100, 200, 50, 250 };
int [] yPos = { 
  100, 100, 50, 50 };

PFont font;  // for displaying the bezier() call

/*
 * If mouse is within RANGE pixels of a point or
 * control point, consider it "on target."
 */
final int RANGE = 15; 
int trackPoint = 0;  // which point is being dragged?

void setup() {
  size(300, 370);
  background(255);
  line(0, 300, 300, 300);
  
  G4P.setFont(this, "Arial", 12);
  font = createFont("Arial", 12, true);
  textFont(font);
  
  GComponent.globalColor = GCScheme.getColor(this,
    GCScheme.GREY_SCHEME);
  
  // set up the buttons and text fields
  for (int i = 0; i < 4; i++)
  {
    buttonList[i] = new GButton(this, buttonLegend[i],
    5 + 175 * (i % 2), 305 + 25 * (i / 2), 20, 15);
    textField[i*2] = new GTextField(this, Integer.toString(xPos[i]),
    35 + 175 * (i % 2), 305 + 25 * (i / 2), 35, 15);
    textField[i*2 + 1] = new GTextField(this, Integer.toString(yPos[i]),
    75 + 175 * (i % 2), 305 + 25 * (i / 2), 35, 15);
  }
  updateCurve();
}

/*
 * Clicking a button doesn't do anything in this version
 */
void handleButtonEvents(GButton button) {
}

/*
 * If someone enters a number in a text field,
 * update the curve to represent that data.
 */
void handleTextFieldEvents(GTextField field)
{
  if (field.eventType == GTextField.ENTERED)
  {
    for (int i = 0; i < 8; i++)
    {
      if (field == textField[i])
      {
        if (i % 2 == 0)
        {
          xPos[i / 2] = Integer.parseInt(field.getText());
        }
        else
        {
          yPos[i / 2] = Integer.parseInt(field.getText());
        }
      }
    }
    updateCurve();
  }
}

void draw()
{
}

/*
 * Determine which of the points the mouse was
 * clicked on (if any).
 */
void mouseDragged()
{
  int i = 0;
  /* deselect all the buttons */
  for (i = 0; i < 4; i++)
  {
    buttonList[i].setColorScheme(GCScheme.GREY_SCHEME);
  }

  /* Find which point the mouse is in range of */
  i = 0;
  while (i < 4 &&
    (dist(mouseX, mouseY, xPos[i], yPos[i]) >= RANGE))
  {
    i++;
  }
  if (i < 4)
  {
    buttonList[i].setColorScheme(GCScheme.YELLOW_SCHEME);
    trackPoint = i;
    xPos[trackPoint] = mouseX;
    yPos[trackPoint] = mouseY;
    updateCurve();
  }
}

/*
 * This function draws the bezier curve and
 * the control points (plus the lines connecting
 * them to the draw points). It also sets the
 * text fields to reflect the points' positions.
 */
void updateCurve()
{
  fill(255);
  noStroke();
  rect(0, 0, 300, 299);
  stroke(0);
  for (int i = 0; i < 4; i++)
  {
    if (i == trackPoint) // highlight the point being tracked
    {
      fill(255, 255, 0);
    }
    else
    {
      noFill();
    }
    ellipse(xPos[i], yPos[i], 5, 5);
  }
  /* draw control point lines */
  noFill();
  stroke(192);
  line(xPos[2], yPos[2], xPos[0], yPos[0]);
  line(xPos[3], yPos[3], xPos[1], yPos[1]);
  
  /* the curve */
  stroke(0);
  bezier(xPos[0], yPos[0], xPos[2], yPos[2],
    xPos[3], yPos[3], xPos[1], yPos[1]);
    
  /* and update the text fields */
  for (int i = 0; i < 4; i++)
  {
    textField[i*2].setText(Integer.toString(xPos[i]));
    textField[i*2 + 1].setText(Integer.toString(yPos[i]));
  }
  
  /* then update the call to the bezier() function */
  noStroke();
  fill(255);
  rect(0, 350, 300, 20);
  fill(0);
  text("bezier(" +
    xPos[0] + ", " + yPos[0] + ", " +
    xPos[2] + ", " + yPos[2] + ", " +
    xPos[3] + ", " + yPos[3] + ", " +
    xPos[1] + ", " + yPos[1] + ")", 5, 360);
}


