// The Nature of Code
// <http://www.shiffman.net/teaching/nature>
// Spring 2012
// PBox2D example

// A blob skeleton
// Could be used to create blobbly characters a la Nokia Friends
// http://postspectacular.com/work/nokia/friends/start

class Blob {

  // A list to keep track of all the points in our blob
  ArrayList<Body> skeleton;

  float bodyRadius;  // The radius of each body that makes up the skeleton
  float radius;      // The radius of the entire blob
  float totalPoints; // How many points make up the blob


  // We should modify this constructor to receive arguments
  // So that we can make many different types of blobs
  Blob() {

    // Create the empty 
    skeleton = new ArrayList<Body>();

    // Let's make a volume of joints!
    ConstantVolumeJointDef cvjd = new ConstantVolumeJointDef();

    // Where and how big is the blob
    Vec2 center = new Vec2(width/2, height/2);
    radius = 100;
    totalPoints = 20;
    bodyRadius = 12;


    // Initialize all the points
    for (int i = 0; i < totalPoints; i++) {
      // Look polar to cartesian coordinate transformation!
      float theta = PApplet.map(i, 0, totalPoints, 0, TWO_PI);
      float x = center.x + radius * sin(theta);
      float y = center.y + radius * cos(theta);

      // Make each individual body
      BodyDef bd = new BodyDef();
      bd.type = BodyType.DYNAMIC;

      bd.fixedRotation = true; // no rotation!
      bd.position.set(box2d.coordPixelsToWorld(x, y));
      Body body = box2d.createBody(bd);

      // The body is a circle
      CircleShape cs = new CircleShape();
      cs.m_radius = box2d.scalarPixelsToWorld(bodyRadius);

      // Define a fixture
      FixtureDef fd = new FixtureDef();
      fd.shape = cs;

      // For filtering out collisions
      //fd.filter.groupIndex = -2;

      // Parameters that affect physics
      fd.density = 1;

      // Finalize the body
      body.createFixture(fd);
      // Add it to the volume
      cvjd.addBody(body);


      // Store our own copy for later rendering
      skeleton.add(body);
    }

    // These parameters control how stiff vs. jiggly the blob is
    cvjd.frequencyHz = 10.0f;
    cvjd.dampingRatio = 1.0f;

    // Put the joint thing in our world!
    box2d.world.createJoint(cvjd);
  }


  // Time to draw the blob!
  // Can you make it a cute character, a la http://postspectacular.com/work/nokia/friends/start
  void display() {

    // Draw the outline
    beginShape();
    noFill();
    stroke(0);
    strokeWeight(1);
    for (Body b: skeleton) {
      Vec2 pos = box2d.getBodyPixelCoord(b);
      vertex(pos.x, pos.y);
    }
    endShape(CLOSE);

    // Draw the individual circles
    for (Body b: skeleton) {
      // We look at each body and get its screen position
      Vec2 pos = box2d.getBodyPixelCoord(b);
      // Get its angle of rotation
      float a = b.getAngle();
      pushMatrix();
      translate(pos.x, pos.y);
      rotate(a);
      fill(175);
      stroke(0);
      strokeWeight(1);
      ellipse(0, 0, bodyRadius*2, bodyRadius*2);
      popMatrix();
    }
  }
}

