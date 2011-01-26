import processing.core.*; 

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

public class Springs extends PApplet {

/**
 * Springs. 
 * 
 * Move the mouse over one of the circles and click to re-position. 
 * When you release the mouse, it will snap back into position. 
 * Each circle has a slightly different behavior.  
 */
 

int num = 3; 
Spring[] springs = new Spring[num]; 

public void setup()
{
  size(200, 200);
  noStroke(); 
  smooth();
  springs[0] = new Spring( 70, 160,  20, 0.98f, 8.0f, 0.1f, springs, 0); 
  springs[1] = new Spring(150, 110,  60, 0.95f, 9.0f, 0.1f, springs, 1); 
  springs[2] = new Spring( 40,  70, 120, 0.90f, 9.9f, 0.1f, springs, 2);   
}

public void draw() 
{
  background(51); 
  
  for (int i = 0; i < num; i++) { 
    springs[i].update(); 
    springs[i].display(); 
  }  
}

public void mousePressed() 
{
  for (int i = 0; i < num; i++) { 
    springs[i].pressed(); 
  } 
}

public void mouseReleased() 
{
  for (int i=0; i<num; i++) { 
    springs[i].released(); 
  } 
}

class Spring 
{ 
  // Screen values 
  float xpos, ypos;
  float tempxpos, tempypos; 
  int size = 20; 
  boolean over = false; 
  boolean move = false; 

  // Spring simulation constants 
  float mass;       // Mass 
  float k = 0.2f;    // Spring constant 
  float damp;       // Damping 
  float rest_posx;  // Rest position X 
  float rest_posy;  // Rest position Y 

  // Spring simulation variables 
  //float pos = 20.0; // Position 
  float velx = 0.0f;   // X Velocity 
  float vely = 0.0f;   // Y Velocity 
  float accel = 0;    // Acceleration 
  float force = 0;    // Force 

  Spring[] friends;
  int me;
  
  // Constructor
  Spring(float x, float y, int s, float d, float m, 
         float k_in, Spring[] others, int id) 
  { 
    xpos = tempxpos = x; 
    ypos = tempypos = y;
    rest_posx = x;
    rest_posy = y;
    size = s;
    damp = d; 
    mass = m; 
    k = k_in;
    friends = others;
    me = id; 
  } 

  public void update() 
  { 
    if (move) { 
      rest_posy = mouseY; 
      rest_posx = mouseX;
    } 

    force = -k * (tempypos - rest_posy);  // f=-ky 
    accel = force / mass;                 // Set the acceleration, f=ma == a=f/m 
    vely = damp * (vely + accel);         // Set the velocity 
    tempypos = tempypos + vely;           // Updated position 

    force = -k * (tempxpos - rest_posx);  // f=-ky 
    accel = force / mass;                 // Set the acceleration, f=ma == a=f/m 
    velx = damp * (velx + accel);         // Set the velocity 
    tempxpos = tempxpos + velx;           // Updated position 

    
    if ((over() || move) && !otherOver() ) { 
      over = true; 
    } else { 
      over = false; 
    } 
  } 
  
  // Test to see if mouse is over this spring
  public boolean over() {
    float disX = tempxpos - mouseX;
    float disY = tempypos - mouseY;
    if (sqrt(sq(disX) + sq(disY)) < size/2 ) {
      return true;
    } else {
      return false;
    }
  }
  
  // Make sure no other springs are active
  public boolean otherOver() {
    for (int i=0; i<num; i++) {
      if (i != me) {
        if (friends[i].over == true) {
          return true;
        }
      }
    }
    return false;
  }

  public void display() 
  { 
    if (over) { 
      fill(153); 
    } else { 
      fill(255); 
    } 
    ellipse(tempxpos, tempypos, size, size);
  } 

  public void pressed() 
  { 
    if (over) { 
      move = true; 
    } else { 
      move = false; 
    }  
  } 

  public void released() 
  { 
    move = false; 
    rest_posx = xpos;
    rest_posy = ypos;
  } 
} 

  static public void main(String args[]) {
    PApplet.main(new String[] { "Springs" });
  }
}
