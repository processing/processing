
class Particle{
  int len;            // number of elements in position array
  Vec3D[] loc;        // array of position vectors
  Vec3D startLoc;     // just used to make sure every loc[] is initialized to the same position
  Vec3D vel;          // velocity vector
  Vec3D perlin;       // perlin noise vector
  float radius;       // particle's size
  float age;          // current age of particle
  int lifeSpan;       // max allowed age of particle
  float agePer;       // range from 1.0 (birth) to 0.0 (death)
  int gen;            // number of times particle has been involved in a SPLIT
  float bounceAge;    // amount to age particle when it bounces off floor
  float bounceVel;    // speed at impact
  boolean ISDEAD;     // if age == lifeSpan, make particle die
  boolean ISBOUNCING; // if particle hits the floor...
  boolean ISSPLIT;    // if particle hits the floor with enough speed...

  
  Particle( int _gen, Vec3D _loc, Vec3D _vel ){
    gen         = _gen;
    radius      = random( 10 - gen, 50 - ( gen-1)*10 );
    
    len         = (int)( radius*.5 );
    loc         = new Vec3D[ len ];
    startLoc    = new Vec3D( _loc.add( new Vec3D().randomVector().scaleSelf( random( 1.0 ) ) ) ); 
    
    for( int i=0; i<len; i++ ){
      loc[i]    = new Vec3D( startLoc );
    }
    
    vel         = new Vec3D( _vel );
    if( gen > 1 ){
      vel.addSelf( new Vec3D().randomVector().scaleSelf( random( 7.0 ) ) );
    } else {
      vel.addSelf( new Vec3D().randomVector().scaleSelf( random( 10.0 ) ) );
    }

    perlin      = new Vec3D();
    
    age         = 0;
    bounceAge   = 2;
    lifeSpan    = (int)( radius );
  }

  void exist(){
    if( ALLOWPERLIN )
      findPerlin();
      
    findVelocity();
    setPosition();
    render();
    setAge();
  }
  
  void findPerlin(){
    float xyRads      = getRads( loc[0].x, loc[0].z, 20.0, 50.0 );
    float yRads       = getRads( loc[0].x, loc[0].y, 20.0, 50.0 );
    perlin.set( cos(xyRads), -sin(yRads), sin(xyRads) );
    perlin.scaleSelf( .5 );
  }
  
  void findVelocity(){
    if( ALLOWGRAVITY )
      vel.addSelf( gravity );
      
    if( ALLOWPERLIN )
      vel.addSelf( perlin );
    
    if( ALLOWFLOOR ){
      if( loc[0].y + vel.y > floorLevel ){
        ISBOUNCING = true;
      } else {
        ISBOUNCING = false;
      }
    }
    
    // if the particle is moving fast enough, when it hits the ground it can
    // split into a bunch of smaller particles.
    if( ISBOUNCING ){
      bounceVel = vel.magnitude();
      
      vel.scaleSelf( .7 );
      vel.y *= -( ( radius/40.0 ) * .5 );
      
      if( bounceVel > 15.0 && gen < 4 )
        ISSPLIT  = true;
        
    } else {
      ISSPLIT = false;
    }
  }
  
  void setPosition(){
    for( int i=len-1; i>0; i-- ){
      loc[i].set( loc[i-1] );
    }

    loc[0].addSelf( vel );
  }
  
  void render(){
    color c = color( agePer - .5, agePer*.25, 1.5 - agePer );
    renderImage(images.particle, loc[0], radius * agePer, c, 1.0 );
    
    // Rendering two graphics here. Makes the particles more vivid,
    // but will hinder the performance.
    c = color( 1, agePer, agePer );
    renderImage(images.particle, loc[0], radius * agePer * .5, c, agePer );
  }
  
  void renderReflection(){
    float altitude           = floorLevel - loc[0].y;
    float reflectMaxAltitude = 25.0;
    float yPer               = ( 1.0 - ( altitude/reflectMaxAltitude ) ) * .5;
    
    if( yPer > .05 )
      renderImageOnFloor(images.particle, new Vec3D( loc[0].x, floorLevel, loc[0].z ), radius * agePer * 8.0 * yPer, color( agePer, agePer*.25, 0 ), yPer + random( .2 ) );
  }
  
  void renderTrails(){
    float xp, yp, zp;
    float xOff, yOff, zOff;
    
    beginShape(QUAD_STRIP);
    
    for ( int i=0; i<len - 1; i++ ){
      float per     = 1.0 - (float)i/(float)(len-1);
      xp            = loc[i].x;
      yp            = loc[i].y;
      zp            = loc[i].z;

      if ( i < len - 2 ){
        Vec3D perp0 = loc[i].sub( loc[i+1] );
        Vec3D perp1 = perp0.cross( new Vec3D( 0, 1, 0 ) ).normalize();
        Vec3D perp2 = perp0.cross( perp1 ).normalize();
              perp1 = perp0.cross( perp2 ).normalize();

        xOff        = perp1.x * radius * agePer * per * .05;
        yOff        = perp1.y * radius * agePer * per * .05;
        zOff        = perp1.z * radius * agePer * per * .05;
        
        fill( per, per*.5, 1.5 - per, per);
        noStroke();
        vertex( xp - xOff, yp - yOff, zp - zOff );
        vertex( xp + xOff, yp + yOff, zp + zOff );
      }
    }
    
    endShape();
  }
  
  void setAge(){

    if( ALLOWFLOOR ){
      if( ISBOUNCING ){
        age += bounceAge;
        bounceAge ++;
      } else {
        age += .025;
      }
    } else {
      age ++;
    }
    
    if( age > lifeSpan ){
      ISDEAD = true;
    } else {
      agePer = 1.0 - age/(float)lifeSpan;
    }
  }
}
