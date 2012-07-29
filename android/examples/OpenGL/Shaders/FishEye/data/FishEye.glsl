// Inspired by the "Angular Fisheye à la Bourke" sketch from
// Jonathan Cremieux, as shown in the OpenProcessing website:
// http://openprocessing.org/visuals/?visualID=12140
// Using the inverse transform of the angular fisheye as
// explained in Paul Bourke's website:
// http://paulbourke.net/miscellaneous/domefisheye/fisheye/

#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform sampler2D textureSampler;
uniform mat4 texcoordMatrix;

varying vec4 vertColor;
varying vec4 vertTexcoord;

uniform float aperture;

const float PI = 3.1415926535;

void main(void) {    
  float apertureHalf = 0.5 * aperture * (PI / 180.0);
  
  // This factor ajusts the coordinates in the case that
  // the aperture angle is less than 180 degrees, in which
  // case the area displayed is not the entire half-sphere.
  float maxFactor = sin(apertureHalf);
  
  // The st factor takes into account the situation when non-pot
  // textures are not supported, so that the maximum texture
  // coordinate to cover the entire image might not be 1.
  vec2 stFactor = vec2(1.0 / abs(texcoordMatrix[0][0]), 1.0 / abs(texcoordMatrix[1][1]));  
  vec2 pos = (2.0 * vertTexcoord.st * stFactor - 1.0);
  
  float l = length(pos);
  if (l > 1.0) {
    gl_FragColor = vec4(0, 0, 0, 1);  
  } else {
    float x = maxFactor * pos.x;
    float y = maxFactor * pos.y;
    
    float n = length(vec2(x, y));
    
    float z = sqrt(1.0 - n * n);
  
    float r = atan(n, z) / PI; 
  
    float phi = atan(y, x);

    float u = r * cos(phi) + 0.5;
    float v = r * sin(phi) + 0.5;

    gl_FragColor = texture2D(textureSampler, vec2(u, v) / stFactor) * vertColor;
  }
}