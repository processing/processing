class Images{
  PImage particle;
  PImage emitter;
  PImage corona;
  PImage reflection;
  
  Images(){
    particle   = loadImage( "particle.png" );
    emitter    = loadImage( "emitter.png" );
    corona     = loadImage( "corona.png" );
    reflection = loadImage( "reflection.png" );
  }
}
