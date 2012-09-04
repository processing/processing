// Camera class which uses Kristian Damkjer's OCD library
//     http://www.cise.ufl.edu/~kdamkjer/processing/libraries/ocd/

class POV{
  PApplet parent;
  Camera cam;
  
  Vec3D eye;
  Vec3D center;
  
  Vec3D eyeNormal;
  
  boolean ISDRAGGING;
  
  POV( PApplet _parent ){
    parent    = _parent;
    cam       = new Camera( parent, 0, 100, 1500 );
    
    eye       = new Vec3D();
    center    = new Vec3D();
    eyeNormal = new Vec3D();
  }

  void exist(){
    perspective( PI/3.0, (float)xSize/(float)ySize, .5, 5000 );
    if( ISDRAGGING ){
      cam.circle( radians( ( mouseX - pmouseX ) * .25 ) );
      cam.arc( radians( ( mouseY - pmouseY ) * .25 ) );
    }

    cam.feed();
    setPosition();
  }

  
  // Code by JohnG from the Processing forum
  // http://processing.org/discourse/yabb_beta/YaBB.cgi?board=Programs;action=display;num=1170790832
  //
  // Does the camera transformations in reverse to allow for images that always face the camera.
  void glReverseCamera(){
    float deltaX   = eye.x - center.x; 
    float deltaY   = eye.y - center.y; 
    float deltaZ   = eye.z - center.z; 
 
    float angleZ   = atan2( deltaY,deltaX ); 
    float hyp      = sqrt( sq( deltaX ) + sq( deltaY ) ); 
    float angleY   = atan2( hyp,deltaZ ); 
    
    gl.glRotatef( degrees( angleZ ), 0, 0, 1.0 );
    gl.glRotatef( degrees( angleY ), 0, 1.0, 0 );
  }
  
  
  void setPosition(){
    float[] e     = cam.position();
    float[] c     = cam.target();
    
    eye.set( e[0], e[1], e[2] );
    center.set( c[0], c[1], c[2] );
    eyeNormal = eye.sub(center).normalize();
  }
}
