// Copyright (C) 2007 Dave Griffiths
// Licence: GPLv2 (see COPYING)
// Fluxus Shader Library
// ---------------------
// Glossy Specular Reflection Shader
// A more controllable version of blinn shading,
// Useful for ceramic or fluids - from Advanced 
// Renderman, thanks to Larry Gritz

#define PROCESSING_LIGHT_SHADER

uniform mat4 modelview;
uniform mat4 transform;
uniform mat3 normalMatrix;

uniform vec4 lightPosition[8];

attribute vec4 vertex;
attribute vec3 normal;

varying vec3 N;
varying vec3 P;
varying vec3 V;
varying vec3 L;

void main() {    
  N = normalize(normalMatrix * normal); 
  P = vertex.xyz;
  V = -vec3(modelview * vertex);
  L = vec3(modelview * (lightPosition[0] - vertex));
  gl_Position = transform * vertex;
}

