// Brownian motion
// by REAS <http://reas.com>

// Recording random movement as a continuous line.

// Updated 21 August 2002

int num = 2000;
int range = 4;

float[] ax = new float[num];
float[] ay = new float[num]; 


void setup() 
{
  size(200, 200);
  for(int i=0; i<num; i++) {
    ax[i] = 50;
    ay[i] = height/2;
  }
  framerate(30);
}

void draw() 
{
  background(51);
  
  // Shift all elements 1 place to the left
  for(int i=1; i<num; i++) {
    ax[i-1] = ax[i];
    ay[i-1] = ay[i];
  }

  // Put a new value at the end of the array
  ax[num-1] += random(-range, range);
  ay[num-1] += random(-range, range);

  // Constrain all points to the screen
  ax[num-1] = constrain(ax[num-1], 0, width);
  ay[num-1] = constrain(ay[num-1], 0, height);
  
  // Draw a line connecting the points
  for(int i=1; i<num; i++) {    
    float val = float(i)/num * 204.0 + 51;
    stroke(val);
    line(ax[i-1], ay[i-1], ax[i], ay[i]);
  }
}
