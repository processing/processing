// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A rectangular box
class Box {
  //  Instead of any of the usual variables, we will store a reference to a Box2D Body
  Body body;      

  float w,h;

  Box(float x, float y) {
    w = 16;
    h = 16;

    // Build Body
    BodyDef bd = new BodyDef();			
    bd.type = BodyType.DYNAMIC;
    bd.position.set(box2d.coordPixelsToWorld(x,y));
    body = box2d.createBody(bd);


    // Define a polygon (this is what we use for a rectangle)
    PolygonShape ps = new PolygonShape();
    float box2dW = box2d.scalarPixelsToWorld(w/2);
    float box2dH = box2d.scalarPixelsToWorld(h/2);	// Box2D considers the width and height of a
    ps.setAsBox(box2dW, box2dH);		        // rectangle to be the distance from the
                 					// center to the edge (so half of what we
                					// normally think of as width or height.) 
    // Define a fixture
    FixtureDef fd = new FixtureDef();
    fd.shape = ps;
    // Parameters that affect physics
    fd.density = 1;
    fd.friction = 0.3;
    fd.restitution = 0.5;

    // Attach Fixture to Body						   
    body.createFixture(fd);
  }

  void display() {
    // We need the Body’s location and angle
    Vec2 pos = box2d.getBodyPixelCoord(body);		
    float a = body.getAngle();

    pushMatrix();
    translate(pos.x,pos.y);		// Using the Vec2 position and float angle to
    rotate(-a);			        // translate and rotate the rectangle
    fill(127);
    stroke(0);
    strokeWeight(2);
    rectMode(CENTER);
    rect(0,0,w,h);
    popMatrix();
  }

}

