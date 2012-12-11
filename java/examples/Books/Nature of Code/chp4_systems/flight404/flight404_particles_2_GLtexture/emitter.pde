class Emitter{
  Vec3D loc;
  Vec3D vel;
  Vec3D velToMouse;
  float radius;

  Texture coronaTex; 
  Texture emitterTex; 
  Texture particleTex; 
  Texture reflectionTex;

  color myColor;

  ArrayList particles;
  ArrayList nebulae;

  Emitter(  ){

    try {
      coronaTex = TextureIO.newTexture(new File(dataPath("corona.png")), true); 
      emitterTex = TextureIO.newTexture(new File(dataPath("emitter.png")), true); 
      particleTex = TextureIO.newTexture(new File(dataPath("particle.png")), true); 
      reflectionTex = TextureIO.newTexture(new File(dataPath("reflection.png")), true); 
    }
    catch (IOException e) {    
      println("Texture file is missing");
      exit();  // or handle it some other way
    }  

    loc        = new Vec3D();
    vel        = new Vec3D();
    velToMouse = new Vec3D();

    radius     = 100;

    myColor    = color( 1, 1, 1 );

    particles  = new ArrayList();
    nebulae    = new ArrayList();
  }

  void exist(){
    findVelocity();
    setPosition();
    iterateListExist();
    render();

    gl.glDisable( GL.GL_TEXTURE_2D );

    if( ALLOWTRAILS )
      iterateListRenderTrails();
  }

  void findVelocity(){
    Vec3D dirToMouse = new Vec3D( mouse.loc.sub( loc ).scale( .15 ) );
    vel.set( dirToMouse );
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
    gl.glEnable( GL.GL_TEXTURE_2D );


    int mylength = particles.size(); 
    for( int i=mylength-1; i>=0; i-- ){ 
      Particle p = ( Particle )particles.get(i); 
      if( p.ISSPLIT ) 
        addParticles( p ); 

      if ( !p.ISDEAD ){
        //        pgl.bindTexture( images.particle );
        particleTex.bind();
        particleTex.enable();
        p.exist();
        particleTex.disable();

      } 
      else { 
        particles.set( i, particles.get( particles.size() - 1 ) ); 
        particles.remove( particles.size() - 1 ); 
      } 
    }

    if( ALLOWFLOOR ){
      //      pgl.bindTexture( images.reflection );
      reflectionTex.bind();
      reflectionTex.enable();
      for( Iterator it = particles.iterator(); it.hasNext(); ){
        Particle p = (Particle) it.next();
        p.renderReflection();
      }
      reflectionTex.disable();
    }

    //    pgl.bindTexture( images.corona );
    coronaTex.bind();
    coronaTex.enable();
    for( Iterator it = nebulae.iterator(); it.hasNext(); ){
      Nebula n = (Nebula) it.next();
      if( !n.ISDEAD ){
        n.exist();
      } 
      else {
        it.remove();
      }
    }
    coronaTex.disable();
  }


  void render(){
    //    pgl.bindTexture( images.emitter );
    emitterTex.bind();
    emitterTex.enable();
    renderImage( loc, radius, myColor, 1.0 );
    emitterTex.enable();

    if( ALLOWNEBULA ){
      nebulae.add( new Nebula( loc, 15.0, true ) );
      nebulae.add( new Nebula( loc, 45.0, true ) );
    }


    if( ALLOWFLOOR ){
      //      pgl.bindTexture( images.reflection );
      reflectionTex.bind();
      reflectionTex.enable();
      renderReflection();
      reflectionTex.disable();
    }
  }

  void renderReflection(){
    float altitude           = floorLevel - loc.y;
    float reflectMaxAltitude = 300.0;
    float yPer               = 1.0 - altitude/reflectMaxAltitude;

    if( yPer > .05 )
      renderImageOnFloor( new Vec3D( loc.x, floorLevel, loc.z ), radius * 10.0, color( 0.5, 1.0, yPer*.25 ), yPer );

    if( mousePressed )
      renderImageOnFloor( new Vec3D( loc.x, floorLevel, loc.z ), radius + ( yPer + 1.0 ) * radius * random( 2.0, 3.5 ), color( 1.0, 0, 0 ), yPer );
  }

  void iterateListRenderTrails(){
    for( Iterator it = particles.iterator(); it.hasNext(); ){
      Particle p = (Particle) it.next();
      p.renderTrails();
    }
  }

  void addParticles( int _amt ){
    for( int i=0; i<_amt; i++ ){
      particles.add( new Particle( 1, loc, vel ) );
    }

    if( ALLOWNEBULA ){
      nebulae.add( new Nebula( loc, 40.0, false ) );
      nebulae.add( new Nebula( loc, 100.0, false ) );
    }
  }

  void addParticles( Particle _p ){
    // play with amt if you want to control how many particles spawn when splitting
    int amt = (int)( _p.radius * .15 );
    for( int i=0; i<amt; i++ ){
      particles.add( new Particle( _p.gen + 1, _p.loc[0], _p.vel ) );
      if( ALLOWNEBULA )
        nebulae.add( new Nebula( _p.loc[0], random( 5.0, 50.0 ), true ) );
    }
  }
}
