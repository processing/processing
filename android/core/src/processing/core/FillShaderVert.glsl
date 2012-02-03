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

uniform mat4 modelviewMatrix;
uniform mat4 projectionMatrix;
uniform mat4 projmodelviewMatrix;

uniform int textured;

uniform int lightCount;
uniform vec4 lightPosition[8];
uniform vec4 lightNormal[8];
uniform vec4 lightAmbient[8];
uniform vec4 lightDiffuse[8];
uniform vec4 lightSpecular[8];      
uniform float lightFalloffConstant[8];
uniform float lightFalloffLinear[8];
uniform float lightFalloffQuadratic[8];      
uniform float lightSpotAngle[8];
uniform float lightSpotConcentration[8]; 

attribute vec4 inVertex;
attribute vec4 inColor;
attribute vec3 inNormal;
attribute vec2 inTexcoord;


varying vec4 vertColor;
varying vec2 vertTexcoord;

void main() {
  gl_Position = projmodelviewMatrix * inVertex;
  
  
  // vertex in eye coordinates
  vec3 ecVertex = vec3(modelviewMatrix * inVertex);
  
  // Normal in eye coordinates
  vec3 ecNormal = normalize(normalMatrix * inNormal);
  
  vertColor = inColor;
  if (0 < textured) {
    vertTexcoord = inTexcoord;
  } 
    
  vec4 total = vec4(0, 0, 0, 0);
  //vec4 total = vec4(1, 1, 1, 1);  
  for (int i = 0; i < lightCount; i++) {
    float distance = length(lightPosition[i] - ecVertex);
  
    // Some random calculation just to stop the compiler from discarding the uniforms.
    //float c = lightFalloffConstant[i] * lightFalloffLinear[i] * lightFalloffQuadratic[i] * lightSpotAngle[i] * lightSpotConcentration[i];
    //total += lightDiffuse[i] * dot(lightPosition[i], lightNormal[i]) + c * lightAmbient[i] + lightSpecular[i];
    
    
    
  }  
  
  vertColor += total;
}