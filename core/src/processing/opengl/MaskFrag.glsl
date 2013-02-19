/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-13 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License version 2.1 as published by the Free Software Foundation.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
 */
 
#define PROCESSING_TEXTURE_SHADER 
 
uniform sampler2D texture;
uniform sampler2D mask;

varying vec4 vertTexCoord;

void main() {
  vec3 texColor = texture2D(texture, vertTexCoord.st).rgb;
  vec3 maskColor = texture2D(mask, vertTexCoord.st).rgb;
  float luminance = dot(maskColor, vec3(0.2126, 0.7152, 0.0722));
  gl_FragColor = vec4(texColor, luminance);  
}