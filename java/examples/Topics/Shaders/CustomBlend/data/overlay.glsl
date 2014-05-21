uniform sampler2D destSampler;
uniform sampler2D srcSampler;

uniform ivec2 destSize;
uniform ivec4 destRect;

uniform ivec2 srcSize;
uniform ivec4 srcRect;

varying vec4 vertTexCoord;

void main() {
  vec2 st = vertTexCoord.st;   
  
  vec2 dest = vec2(destRect.xy) / vec2(destSize) + st * vec2(destRect.zw) / vec2(destSize);
  vec2 src = vec2(srcRect.xy) / vec2(srcSize) + st * vec2(srcRect.zw) / vec2(srcSize); 
  
  vec3 destColor = texture2D(destSampler, dest).rgb;
  vec3 srcColor = texture2D(srcSampler, src).rgb;
  
  float luminance = dot(vec3(0.2126, 0.7152, 0.0722), destColor);

  if (luminance < 0.5) {
    gl_FragColor = vec4(2.0 * destColor * srcColor, 1.0);
  } else {
    gl_FragColor = vec4(1.0 - 2.0 * (1.0 - destColor) * (1.0 - srcColor), 1);
  }
}