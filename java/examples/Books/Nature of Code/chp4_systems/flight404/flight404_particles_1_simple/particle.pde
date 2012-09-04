/*
General Structure notes.
 My classes tend to have a similar naming scheme and flow. I start with the 'exist' method.
 Exist is what an object needs to do every frame. Usually 'existing' consists of four main things.
 1) Find the velocity. This involves determining what influences there are on the velocity.
 2) Apply the velocity to the location.
 3) Render the object.
 4) Age the object.
 
 I also use the metaphor of aging and death. When first made, a particle's age will be zero.
 Every frame, the age will increment. If the age reaches the lifeSpan (which is a random number
 that I set in the constructor), then the boolean ISDEAD is set to true and the arraylist iterator
 removes the dead element from the list.
 */



class Particle {
  int len;            // number of elements in position array
  Vec3D[] loc;        // array of position vectors
  Vec3D startLoc;     // just used to make sure every loc[] is initialized to the same position
  Vec3D vel;          // velocity vector
  Vec3D perlin;       // perlin noise vector
  float radius;       // particle's size
  float age;          // current age of particle
  int lifeSpan;       // max allowed age of particle
  float agePer;       // range from 1.0 (birth) to 0.0 (death)
  float bounceAge;    // amount to age particle when it bounces off floor
  boolean ISDEAD;     // if age == lifeSpan, make particle die
  boolean ISBOUNCING; // if particle hits the floor...


  Particle( Vec3D _loc, Vec3D _vel ) {
    radius      = random( 10, 40 );
    len         = (int)( radius );
    loc         = new Vec3D[ len ];

    // This confusing-looking line does three things at once.
    // First, you make a random vector.
    // new Vec3D().randomVector()
    // Next, you multiply that vector by a random number from 0.0 to 5.0.
    // scaleSelf( 5.0 );
    // Finally, you add this new vector to the original sent vector.
    // _loc.add( );
    // This is just a way to make sure all the particles made this frame
    // don't all start on the exact same pixel. This staggering will be useful
    // when we incorporate magnetic repulsion in a later tutorial.
    startLoc    = new Vec3D( _loc.add( new Vec3D().randomVector().scaleSelf( random( 5.0 ) ) ) ); 

    for( int i=0; i<len; i++ ) {
      loc[i]    = new Vec3D( startLoc );
    }


    // This next confusing-looking line does four things.
    // 1) Make a random vector.
    // new Vec3D().randomVector()
    //
    // 2) Multiply that vector by a random number from 0.0 to 10.0.
    // scaleSelf( 15.0 )
    //
    // 3) Scale down the original sent velocity just to calm things down a bit.
    // _vel.scale( .5 )
    //
    // 4) Add this new vector to the scaled down original sent vector.
    // addSelf( )
    //
    // This randomizes the original sent velocity so the particles
    // dont all move at the same speed in the same direction.
    vel         = new Vec3D( _vel.scale( .5 ).addSelf( new Vec3D().randomVector().scaleSelf( random( 10.0 ) ) ) );

    perlin      = new Vec3D();

    age         = 0;
    bounceAge   = 2;
    lifeSpan    = (int)( radius );
  }

  void exist() {
    if( ALLOWPERLIN )
      findPerlin();

    findVelocity();
    setPosition();
    render();
    setAge();
  }

  void findPerlin() {
    float xyRads      = getRads( loc[0].x, loc[0].z, 10.0, 20.0 );
    float yRads       = getRads( loc[0].x, loc[0].y, 10.0, 20.0 );
    perlin.set( cos(xyRads), -sin(yRads), sin(xyRads) );
    perlin.scaleSelf( .5 );
  }

  void findVelocity() {
    if( ALLOWGRAVITY )
      vel.addSelf( gravity );

    if( ALLOWPERLIN )
      vel.addSelf( perlin );

    if( ALLOWFLOOR ) {
      if( loc[0].y + vel.y > floorLevel ) {
        ISBOUNCING = true;
      } 
      else {
        ISBOUNCING = false;
      }
    }

    if( ISBOUNCING ) {
      vel.scaleSelf( .75 );
      vel.y *= -.5;
    }
  }

  void setPosition() {
    // Every frame, the current location will be passed on to
    // the next element in the location array. Think 'cursor trail effect'.
    for( int i=len-1; i>0; i-- ) {
      loc[i].set( loc[i-1] );
    }

    // Set the initial location.
    // loc[0] represents the current position of the particle.
    loc[0].addSelf( vel );
  }

  void render() {
    // As the particle ages, it will gain blue but will lose red and green.
    color c = color( agePer, agePer*.75, 1.0 - agePer );
    renderImage(particleImg, loc[0], radius * agePer, c, 1.0 );
  }

  void renderTrails() {
    float xp, yp, zp;
    float xOff, yOff, zOff;
    beginShape(QUAD_STRIP);
    for ( int i=0; i<len - 1; i++ ) {
      float per     = 1.0 - (float)i/(float)(len-1);
      xp            = loc[i].x;
      yp            = loc[i].y;
      zp            = loc[i].z;

      if ( i < len - 2 ) {
        // Okay, here is some vector craziness that I probably cant explain very well.
        // This is one of those things that I was taught and though I can picture in my mind
        // what the following 4 lines of code does, I doubt I can explain it.  In short,
        // I am using the cross product (wikipedia it) of the vector between adjacent
        // location array elements (perp0), and finding two vectors that are at right angles to 
        // it (perp1 and perp2). I then use perp1 to allow me to draw a ribbon with controllable
        // widths.
        // 
        // It's much more useful when dealing with a 3D space and a rotating camera. Think of it
        // like this. These trails are meant to function like motion blurs rather than dragged ribbons.
        // A dragged ribbon can be observed at different angles which would make its width fluctuate.
        // You can view it side-on and it would be incredibly thin but you can also view it top-down
        // and you would see its full width. I don't want this effect for the trails so I need to
        // make sure I am always looking at them top-down. So no matter where the camera is, I will
        // always see the ribbons with their width oriented to the camera. The one change I made for
        // this particular piece of source which has no camera object is I have replaced the eyeNormal
        // (which would be the vector pointing from ribbon towards camera) with a generic Vec3D(0, 1, 0).
        // Why? Well cause it works and thats enough for me. WHEE!
        Vec3D perp0 = loc[i].sub( loc[i+1] );
        Vec3D perp1 = perp0.cross( new Vec3D( 0, 1, 0 ) ).normalize();
        Vec3D perp2 = perp0.cross( perp1 ).normalize();
        perp1 = perp0.cross( perp2 ).normalize();

        xOff        = perp1.x * radius * agePer * per * .1;
        yOff        = perp1.y * radius * agePer * per * .1;
        zOff        = perp1.z * radius * agePer * per * .1;

        fill( per, per*.25, 1.0 - per, per * .5);
        noStroke();
        vertex( xp - xOff, yp - yOff, zp - zOff );
        vertex( xp + xOff, yp + yOff, zp + zOff );
      }
    }
    endShape();
  }

  void setAge() {

    if( ALLOWFLOOR ) {
      if( ISBOUNCING ) {
        age += bounceAge;
        bounceAge ++;
      } 
      else {
        age += .25;
      }
    } 
    else {
      age ++;
    }

    if( age > lifeSpan ) {
      ISDEAD = true;
    } 
    else {
      // When spawned, the agePer is 1.0.
      // When death occurs, the agePer is 0.0.
      agePer = 1.0 - age/(float)lifeSpan;
    }
  }
}

