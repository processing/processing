// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Class to describe a fixed spinning object

class Windmill {

  // Our object is two boxes and one joint
  // Consider making the fixed box much smaller and not drawing it
  RevoluteJoint joint;
  Box box1;
  Box box2;

  Windmill(float x, float y) {

    // Initialize locations of two boxes
    box1 = new Box(x, y-20, 120, 10, false); 
    box2 = new Box(x, y, 10, 40, true); 

    // Define joint as between two bodies
    RevoluteJointDef rjd = new RevoluteJointDef();

    rjd.initialize(box1.body, box2.body, box1.body.getWorldCenter());

    // Turning on a motor (optional)
    rjd.motorSpeed = PI*2;       // how fast?
    rjd.maxMotorTorque = 1000.0; // how powerful?
    rjd.enableMotor = false;      // is it on?

    // There are many other properties you can set for a Revolute joint
    // For example, you can limit its angle between a minimum and a maximum
    // See box2d manual for more
    

      // Create the joint
    joint = (RevoluteJoint) box2d.world.createJoint(rjd);
  }

  // Turn the motor on or off
  void toggleMotor() {
    joint.enableMotor(!joint.isMotorEnabled());
  }

  boolean motorOn() {
    return joint.isMotorEnabled();
  }


  void display() {
    box2.display();
    box1.display();

    // Draw anchor just for debug
    Vec2 anchor = box2d.coordWorldToPixels(box1.body.getWorldCenter());
    fill(0);
    noStroke();
    ellipse(anchor.x, anchor.y, 8, 8);
  }
}

