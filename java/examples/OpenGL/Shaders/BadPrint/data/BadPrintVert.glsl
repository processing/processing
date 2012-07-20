// Copyright (C) 2007 Dave Griffiths
// Licence: GPLv2 (see COPYING)
// Fluxus Shader Library
// ---------------------
// BadPrint NPR Shader
// This shader tries to emulate the effect
// of a bad printing process. Can be controlled
// with different settings for RGB

uniform mat4 projectionMatrix;
uniform mat4 projmodelviewMatrix;
uniform mat3 normalMatrix;

uniform vec4 lightPosition[8];

attribute vec4 inVertex;
attribute vec3 inNormal;

varying vec3 N;
varying vec3 P;
varying vec4 S;
varying vec3 L;

void main() {    
  N = normalize(normalMatrix * inNormal); 
  P = inVertex.xyz;
  gl_Position = projmodelviewMatrix * inVertex;
  L = vec3(lightPosition[0] - gl_Position);
  S = projectionMatrix * gl_Position;
}
