// Toon shader using per-pixel lighting. Based on the glsl 
// tutorial from lighthouse 3D:
// http://www.lighthouse3d.com/tutorials/glsl-tutorial/toon-shader-version-ii/

uniform mat4 modelviewMatrix;
uniform mat4 projmodelviewMatrix;
uniform mat3 normalMatrix;

uniform vec3 lightNormal[8];

attribute vec4 inVertex;
attribute vec3 inNormal;

varying vec3 vertNormal;
varying vec3 vertLightDir;

void main() {
  // Vertex in clip coordinates
  gl_Position = projmodelviewMatrix * inVertex;
  
  // Normal vector in eye coordinates is passed
  // to the fragment shader
  vertNormal = normalize(normalMatrix * inNormal);
  
  // Assuming that there is only one directional light.
  // Its normal vector is passed to the fragment shader
  // in order to perform per-pixel lighting calculation.  
  vertLightDir = -lightNormal[0]; 
}