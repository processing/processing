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
uniform mat3 normalMatrix;

uniform int textured;

uniform int lightCount;
uniform vec4 lightPosition[8];
uniform vec3 lightNormal[8];

uniform vec4 lightAmbient[8];
uniform vec4 lightDiffuse[8];
uniform vec4 lightSpecular[8];
      
uniform float lightFalloffConstant[8];
uniform float lightFalloffLinear[8];
uniform float lightFalloffQuadratic[8];
      
uniform float lightSpotAngleCos[8];
uniform float lightSpotConcentration[8]; 

attribute vec4 inVertex;
attribute vec4 inColor;
attribute vec3 inNormal;
attribute vec2 inTexcoord;

//attribute vec4 inAmbientColor;
//attribute vec4 inSpecularColor;
//attribute vec4 inEmissiveColor;
//attribute float shine;

varying vec4 vertColor;
varying vec2 vertTexcoord;

float attenuationFactor(vec3 lightPos, vec3 vertPos, float c0, float c1, float c2) {
  float d = distance(lightPos, vertPos);
  return = 1.0 / (c0 + c1 * d + c2 * d * d);
}

float spotFactor(vec3 lightPos, vec3 vertPos, vec3 lightNorm, float minCos, float spotExp) {
  float spotCos = dot(-lightNorm, lightPos - vertPos);
  return spotCos <= minCos ? 0.0 : pow(spotCos, spotExp); 
}

float lambertFactor(vec3 lightDir, vec3 vecNormal) {
  return max(0.0, dot(lightDir, vecNormal));
}

float blinnPhongFactor(vec3 lightDir, vec3 lightPos, vec3 vecNormal, float shine) {
  return pow(max(0.0, dot(lightDir - lightPos, vecNormal)), shine);
}

void main() {
  gl_Position = projmodelviewMatrix * inVertex;
    
  // Vertex in eye coordinates
  vec3 ecVertex = vec3(modelviewMatrix * inVertex);
  
  // Normal vector in eye coordinates
  vec3 ecNormal = normalize(normalMatrix * inNormal);
  
  vertColor = inColor;
  if (0 < textured) {
    vertTexcoord = inTexcoord;
  } 
  
  // Light calculation *******************************************
  vec4 totalAmbient = vec4(0, 0, 0, 0);
  vec4 totalDiffuse = vec4(0, 0, 0, 0);
  vec4 totalSpecular = vec4(0, 0, 0, 0);
  for (int i = 0; i < lightCount; i++) {
    vec3 lightPos3 = lightPosition[i].xyz;
    bool isDir = 0.0 < lightPosition[i].w;
    float exp = lightSpotConcentration[i];
    float mcos = lightSpotAngleCos[i];
    
    vec3 lightDir;
    float falloff;    
    float spot;
    float shine = 0.5;
      
    if (isDir) {
      falloff = 1.0;
      lightDir = lightPos3 - ecVertex;
    } else {
      falloff = attenuationFactor(lightPos3, ecVertex, lightFalloffConstant[i], 
                                                       lightFalloffLinear[i],
                                                       lightFalloffQuadratic[i]);
      lightDir = -lightNormal[i];  
    }
  
    spot = exp > 0.0 ? spotFactor(lightPos3, ecVertex, lightNormal[i], mcos, exp) : 1.0;
    
    totalAmbient  += lightAmbient[i]  * falloff;
    totalDiffuse  += lightDiffuse[i]  * falloff * spot * lambertFactor(lightDir, ecNormal);
    totalSpecular += lightSpecular[i] * falloff * spot * blinnPhongFactor(lightDir, lightPos3, ecNormal, shine);       
  }  

}