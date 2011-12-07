/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011 Andres Colubri

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

attribute vec2 vertDisp;
 
uniform vec4 eye;
uniform int lights;

// From the "Directional Lights I & II" tutorials from lighthouse 3D: 
// http://www.lighthouse3d.com/tutorials/glsl-tutorial/directional-lights-i/
vec4 calculateLight(int i) {
  // Per-vertex diffuse and ambient lighting.
  vec3 normal, lightDir;
  vec4 diffuse, ambient, specular;
  float NdotL, NdotHV;
  
  // first transform the normal into eye space and normalize the result.
  normal = normalize(gl_NormalMatrix * gl_Normal);
	
  // now normalize the light's direction. Note that according to the
  // OpenGL specification, the light is stored in eye space. Also since
  // we're talking about a directional light, the position field is actually
  // direction
  lightDir = normalize(vec3(gl_LightSource[i].position));
	
  // compute the cos of the angle between the normal and lights direction.
  // The light is directional so the direction is constant for every vertex.
  // Since these two are normalized the cosine is the dot product. We also
  // need to clamp the result to the [0,1] range.
  NdotL = max(dot(normal, lightDir), 0.0);
	
  // Compute the diffuse term. Ambient and diffuse material components
  // are stored in the vertex color, since processing uses GL_COLOR_MATERIAL
  diffuse = gl_Color * gl_LightSource[i].diffuse;
	
  // Compute the ambient and globalAmbient terms
  ambient = gl_Color * gl_LightSource[i].ambient;
    
  // compute the specular term if NdotL is  larger than zero
  if (NdotL > 0.0) {
    // normalize the half-vector, and then compute the
    // cosine (dot product) with the normal
    NdotHV = max(dot(normal, gl_LightSource[i].halfVector.xyz), 0.0);
	specular = gl_FrontMaterial.specular * gl_LightSource[i].specular * pow(NdotHV, gl_FrontMaterial.shininess);
  }
	  
  return NdotL * diffuse + ambient + specular;
}  

void main() {
  vec4 pos = gl_ModelViewMatrix * gl_Vertex;
  pos.xy += vertDisp.xy;
  gl_Position = gl_ProjectionMatrix * pos;
  
  gl_FrontColor = vec4(0, 0, 0, 0);
  vec4 globalAmbient = gl_Color * gl_LightModel.ambient;  
  if (lights == 0) {  
    gl_FrontColor = gl_Color;
  }  
  for (int i = 0; i < lights; i++) {
    vec4 light = calculateLight(i);
    gl_FrontColor += light;  
  }
  gl_FrontColor += globalAmbient;
}