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

attribute vec4 inVertex;
attribute vec4 inColor;
attribute vec3 inNormal;
attribute vec2 inTexcoord;

varying vec4 vertColor;
varying vec2 vertTexcoord;

void main() {
  gl_Position = projectionMatrix * modelviewMatrix * inVertex;
  
  vertColor = inColor;
  vertTexcoord = inTexcoord; 
}