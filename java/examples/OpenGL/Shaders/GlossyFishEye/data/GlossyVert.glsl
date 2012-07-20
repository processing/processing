// Copyright (C) 2007 Dave Griffiths
// Licence: GPLv2 (see COPYING)
// Fluxus Shader Library
// ---------------------
// Glossy Specular Reflection Shader
// A more controllable version of blinn shading,
// Useful for ceramic or fluids - from Advanced 
// Renderman, thanks to Larry Gritz

uniform mat4 modelviewMatrix;
uniform mat4 projmodelviewMatrix;
uniform mat3 normalMatrix;

uniform vec4 lightPosition[8];

attribute vec4 inVertex;
attribute vec3 inNormal;

varying vec3 N;
varying vec3 P;
varying vec3 V;
varying vec3 L;

void main() {    
  N = normalize(normalMatrix * inNormal); 
  P = inVertex.xyz;
  V = -vec3(modelviewMatrix * inVertex);
  L = vec3(modelviewMatrix * (lightPosition[0] - inVertex));
  gl_Position = projmodelviewMatrix * inVertex;
}

