
/*
The emitter is just an object that follows the cursor and
can spawn new particle objects. It would be easier to just make
the location vector match the cursor position but I have opted
to use a velocity vector because later I will be allowing for 
multiple emitters.
*/

class Emitter{
  Vec3D loc;
  Vec3D vel;
  Vec3D velToMouse;
  
  color myColor;
  
  ArrayList particles;
  
  Emitter(  ){
    loc        = new Vec3D();
    vel        = new Vec3D();
    velToMouse = new Vec3D();
    
    myColor    = color( 1, 1, 1 );
    
    particles  = new ArrayList();
  }
  
  void exist(){
    setVelToMouse();
    findVelocity();
    setPosition();
    iterateListExist();
    render();
    
    pgl.disable( PGL.TEXTURE_2D );
    
    if( ALLOWTRAILS )
      iterateListRenderTrails();
  }
  
  void setVelToMouse(){
    velToMouse.set( mouseX - loc.x, mouseY - loc.y, 0 );
  }
  
  void findVelocity(){
    vel.interpolateToSelf( velToMouse, .35 );
  }
  
  void setPosition(){
    loc.addSelf( vel );
    
    if( ALLOWFLOOR ){
      if( loc.y > floorLevel ){
        loc.y = floorLevel;
        vel.y = 0;
      }
    }
  }
  
  void iterateListExist(){
    for( Iterator it = particles.iterator(); it.hasNext(); ){
      Particle p = (Particle) it.next();
      if( !p.ISDEAD ){
        p.exist();
      } else {
        it.remove();
      }
    }
  }
  
  
  void render(){
    renderImage( emitterImg, loc, 150, myColor, 1.0 );
  }
  
  void iterateListRenderTrails(){
    for( Iterator it = particles.iterator(); it.hasNext(); ){
      Particle p = (Particle) it.next();
      p.renderTrails();
    }
  }

  void addParticles( int _amt ){
    for( int i=0; i<_amt; i++ ){
      particles.add( new Particle( loc, vel ) );
    }
  }
}
