// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A blob skeleton
// Could be used to create blobbly characters a la Nokia Friends
// http://postspectacular.com/work/nokia/friends/start

class Skeleton {

  // A list to keep track of all the bodies and joints
  ArrayList<Body> bodies;
  ArrayList<Joint> joints;

  float bodyRadius;  // The radius of each body that makes up the skeleton
  float radius;      // The radius of the entire blob
  float totalPoints; // How many points make up the blob


  // We should modify this constructor to receive arguments
  // So that we can make many different types of blobs
  Skeleton() {

    // Create the empty ArrayLists
    bodies = new ArrayList<Body>();
    joints = new ArrayList<Joint>();

    // Where and how big is the blob
    Vec2 center = new Vec2(width/2, height/2);
    radius = 100;
    totalPoints = 32;
    bodyRadius = 10;

    // Initialize all the points in a circle
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
      fd.density = 1;
      fd.friction = 0.5;
      fd.restitution = 0.3;

      // Finalize the body
      body.createFixture(fd);

      // Store our own copy for later rendering
      bodies.add(body);
    }

    // Now connect the outline of the shape all with joints
    for (int i = 0; i < bodies.size(); i++) {
      DistanceJointDef djd = new DistanceJointDef();
      Body a = bodies.get(i);
      int next = i+1;
      if (i == bodies.size()-1) {
        next = 0;
      }
      Body b = bodies.get(next);
      // Connection between previous particle and this one
      djd.bodyA = a;
      djd.bodyB = b;
      // Equilibrium length is distance between these bodies
      Vec2 apos = a.getWorldCenter();
      Vec2 bpos = b.getWorldCenter();
      float d = dist(apos.x, apos.y, bpos.x, bpos.y);
      djd.length = d;
      // These properties affect how springy the joint is 
      djd.frequencyHz = 10;
      djd.dampingRatio = 0.9;

      // Make the joint.  
      DistanceJoint dj = (DistanceJoint) box2d.world.createJoint(djd);
      joints.add(dj);
    }


    // Make some joints that cross the center of the blob between bodies
    for (int i = 0; i < bodies.size(); i++) {
      for (int j = i+2; j < bodies.size(); j+=4) { 
        DistanceJointDef djd = new DistanceJointDef();
        Body a = bodies.get(i);
        Body b = bodies.get(j);
        // Connection between two bides
        djd.bodyA = a;
        djd.bodyB = b;
        // Equilibrium length is distance between these bodies
        Vec2 apos = a.getWorldCenter();
        Vec2 bpos = b.getWorldCenter();
        float d = dist(apos.x, apos.y, bpos.x, bpos.y);

        djd.length = d;
        // These properties affect how springy the joint is 
        djd.frequencyHz = 3;
        djd.dampingRatio = 0.1;

        // Make the joint.  
        DistanceJoint dj = (DistanceJoint) box2d.world.createJoint(djd);
        joints.add(dj);
      }
    }
  }


  // Draw the skeleton as circles for bodies and lines for joints
  void displaySkeleton() {
    // Draw the outline
    stroke(0);
    strokeWeight(1);
    for (Joint j: joints) {
      Body a = j.getBodyA();
      Body b = j.getBodyB();
      Vec2 posa = box2d.getBodyPixelCoord(a);
      Vec2 posb = box2d.getBodyPixelCoord(b);
      line(posa.x, posa.y, posb.x, posb.y);
    }

    // Draw the individual circles
    for (Body b: bodies) {
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
  
  
  // Draw it as a creature
  void displayCreature() {
    // Let's compute the center!
    Vec2 center = new Vec2(0, 0);
   
    // Make a curvy polygon 
    beginShape();
    stroke(175);
    strokeWeight(bodyRadius*2);
    fill(175);
    for (Body b: bodies) {
      // We look at each body and get its screen position
      Vec2 pos = box2d.getBodyPixelCoord(b);
      curveVertex(pos.x, pos.y);
      center.addLocal(pos);
    }
    endShape(CLOSE);
    // Center is average of all points
    center.mulLocal(1.0/bodies.size());

    // Find angle between center and side body
    Vec2 pos = box2d.getBodyPixelCoord(bodies.get(0));
    float dx = pos.x - center.x;
    float dy = pos.y - center.y;
    float angle = atan2(dy, dx)-PI/2;

    // Draw eyes and mouth relative to center
    pushMatrix();
    strokeWeight(1);
    stroke(0);
    translate(center.x, center.y);
    rotate(angle);
    fill(0);
    ellipse(-25, -50, 16, 16);
    ellipse(25, -50, 16, 16);
    line(-50, 50, 50, 50);
    popMatrix();
  }

  Body getFirstBody() {
    return bodies.get(0);
  }
}

