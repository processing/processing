import processing.core.*; 
import processing.xml.*; 

import guicomponents.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class try_curve extends PApplet {



final int P1 = 0;  // first point
final int P2 = 1;  // second point
final int C1 = 2;  // first control point
final int C2 = 3;  // second control point
final int TAN_LENGTH = 15; // half length of tangent line

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
final int RANGE = 20; 
int trackPoint = -1;  // which point is being dragged? (-1 = none)

public void setup() {
  size(300, 370);
  background(255);
  smooth();
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
public void handleButtonEvents(GButton button) {
}

/*
 * If someone enters a number in a text field,
 * update the curve to represent that data.
 */
public void handleTextFieldEvents(GTextField field)
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

public void draw()
{
}


public void mouseDragged()
{
  int i = 0;
  /* deselect all the buttons */
  for (i = 0; i < 4; i++)
  {
    buttonList[i].setColorScheme(GCScheme.GREY_SCHEME);
  }

  /* Find which point the mouse is in range of */
  i = 0;
  while (i < 4 && (abs(mouseX - xPos[i]) >= RANGE
    || abs(mouseY - yPos[i]) >= RANGE))
  {
    i++;
  }
  
  /* 
   * If on one of the points, it becomes the point
   * to be tracked; update the curve.
   */
  if (i < 4)
  {
    buttonList[i].setColorScheme(GCScheme.YELLOW_SCHEME);
    trackPoint = i;
    xPos[trackPoint] = mouseX;
    yPos[trackPoint] = mouseY;
    updateCurve();
  }
  else
  {
    trackPoint = -1;
  }
}

/*
 * This function draws the bezier curve and
 * the control points (plus the lines connecting
 * them to the draw points). It also sets the
 * text fields to reflect the points' positions.
 */
public void updateCurve()
{
  float slope;
  float intercept;
  
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
    ellipse(xPos[i], yPos[i], 7, 7);
  }
  /* draw control point lines */
  noFill();
  stroke(128, 128, 255);
  line(xPos[C1], yPos[C1], xPos[P2], yPos[P2]);
  drawTangent(xPos[C1], yPos[C1], xPos[P2], yPos[P2],
    xPos[P1], yPos[P1]);
   
  stroke(255,128,128);
  line(xPos[C2], yPos[C2], xPos[P1], yPos[P1]);
  drawTangent(xPos[C2], yPos[C2], xPos[P1], yPos[P1],
    xPos[P2], yPos[P2]);
  
  /* the curve */
  stroke(0);
  curve(xPos[C1], yPos[C1], xPos[P1], yPos[P1],
    xPos[P2], yPos[P2], xPos[C2], yPos[C2]);
    
  /* and update the text fields */
  for (int i = 0; i < 4; i++)
  {
    textField[i*2].setText(Integer.toString(xPos[i]));
    textField[i*2 + 1].setText(Integer.toString(yPos[i]));
  }
  
  /* then update the call to the curve() function */
  noStroke();
  fill(255);
  rect(0, 350, 300, 20);
  fill(0);
  text("curve(" +
    xPos[C1] + ", " + yPos[C1] + ", " +
    xPos[P1] + ", " + yPos[P1] + ", " +
    xPos[P2] + ", " + yPos[P2] + ", " +
    xPos[C2] + ", " + yPos[C2] + ")", 5, 360);
}

/*
  Draw tangent line at px, py that is
  parallel to line from (x1, y1) to (x2, y2)
*/
public void drawTangent(float x1, float y1, float x2, float y2,
  float px, float py)
{
  float theta;
  float xOffset;
  float slope = (y1 - y2) / (x1 - x2);
  float intercept;
  if (abs(slope) != Float.POSITIVE_INFINITY)
  {
    intercept = py - slope * px;
    theta = atan2(y1 - y2, x1 - x2);
    xOffset = TAN_LENGTH * cos(theta);
    line(px - xOffset, slope * (px - xOffset) + intercept,
      px + xOffset, slope * (px + xOffset) + intercept);
  }
  else
  {
    line(px, py - TAN_LENGTH, px, py + TAN_LENGTH);
  }
  
}

/* note to self:
  if a line y = mx + b passes through point (X, Y),
  and we know m, then
  Y = mX + b
  Y - mX = b
  b = Y - mX */

  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#DFDFDF", "try_curve" });
  }
}
