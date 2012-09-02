class Nebula{
  Vec3D loc;
  Vec3D vel;
  float radius;
  float scaleFac;
  float age;
  int lifeSpan;
  float agePer;
  float rot;
  color c;

  boolean ISDEAD;
  boolean ISGROUNDED;

  Nebula( Vec3D _loc, float _radius, boolean _ISGROUNDED ){
    loc            = new Vec3D( _loc );
    vel            = new Vec3D( pov.eyeNormal.scale( 2.0 ) );
    radius         = random( _radius*.8, _radius*1.75 );
    
    scaleFac       = random( 1.005, 1.10 );
    age            = 0;
    lifeSpan       = (int)random(10,30);
    rot            = random( TWO_PI );
    c              = color( random(.75, 1.0), random(.5,.75), random(.2,.8) );
    ISGROUNDED     = _ISGROUNDED;
    
    if( ISGROUNDED ){
      scaleFac     = random( 1.01, 1.025 );
      vel.y       -= random( 1.0 );
      radius      *= 2.0;
    }
  }
  
  void exist(){
    move();
    render();
    checkAge();
  }
  
  void move(){
    radius *= scaleFac;
    loc.addSelf( vel );
  }
  
  void render(){
    renderImageAndRotate( loc, radius, c, sin(agePer*PI) * .4, rot );
  }
  
  void checkAge(){
    age ++;
    agePer = 1.0 - age/(float)lifeSpan;
    
    if (age > lifeSpan)
      ISDEAD = true;
  }
}
