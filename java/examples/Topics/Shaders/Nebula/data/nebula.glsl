#define PROCESSING_COLOR_SHADER

uniform float time;
uniform vec2 resolution;

// NEBULA - CoffeeBreakStudios.com (CBS)
// Work in progress...
//
// 3148.26: Switched from classic to simplex noise
// 3148.27: Reduced number of stars
// 3249.0:  Switched to fast computed 3D noise. Less quality but ~ 2x faster
// 3249.5:  Removed use of random number generator to gain performance
// 3265.0:  Added rotation: glsl.heroku.com/e#3005.1

//Utility functions

vec3 fade(vec3 t) {
  return vec3(1.0,1.0,1.0);//t*t*t*(t*(t*6.0-15.0)+10.0);
}

vec2 rotate(vec2 point, float rads) {
	float cs = cos(rads);
	float sn = sin(rads);
	return point * mat2(cs, -sn, sn, cs);
}	

vec4 randomizer4(const vec4 x)
{
    vec4 z = mod(x, vec4(5612.0));
    z = mod(z, vec4(3.1415927 * 2.0));
    return(fract(cos(z) * vec4(56812.5453)));
}

// Fast computed noise
// http://www.gamedev.net/topic/502913-fast-computed-noise/

const float A = 1.0;
const float B = 57.0;
const float C = 113.0;
const vec3 ABC = vec3(A, B, C);
const vec4 A3 = vec4(0, B, C, C+B);
const vec4 A4 = vec4(A, A+B, C+A, C+A+B);

float cnoise4(const in vec3 xx)
{
    vec3 x = mod(xx + 32768.0, 65536.0);
    vec3 ix = floor(x);
    vec3 fx = fract(x);
    vec3 wx = fx*fx*(3.0-2.0*fx);
    float nn = dot(ix, ABC);

    vec4 N1 = nn + A3;
    vec4 N2 = nn + A4;
    vec4 R1 = randomizer4(N1);
    vec4 R2 = randomizer4(N2);
    vec4 R = mix(R1, R2, wx.x);
    float re = mix(mix(R.x, R.y, wx.y), mix(R.z, R.w, wx.y), wx.z);

    return 1.0 - 2.0 * re;
}
float surface3 ( vec3 coord, float frequency ) {
	
	float n = 0.0;	
		
	n += 1.0	* abs( cnoise4( coord * frequency ) );
	n += 0.5	* abs( cnoise4( coord * frequency * 2.0 ) );
	n += 0.25	* abs( cnoise4( coord * frequency * 4.0 ) );
	n += 0.125	* abs( cnoise4( coord * frequency * 8.0 ) );
	n += 0.0625	* abs( cnoise4( coord * frequency * 16.0 ) );
	
	return n;
}
	
void main( void ) {
	float rads = radians(time*3.15);
	vec2 position = gl_FragCoord.xy / resolution.xy;
	position += rotate(position, rads);
	float n = surface3(vec3(position*sin(time*0.1), time * 0.05)*mat3(1,0,0,0,.8,.6,0,-.6,.8),0.9);
	float n2 = surface3(vec3(position*cos(time*0.1), time * 0.04)*mat3(1,0,0,0,.8,.6,0,-.6,.8),0.8);
    	float lum = length(n);
    	float lum2 = length(n2);

	vec3 tc = pow(vec3(1.0-lum),vec3(sin(position.x)+cos(time)+4.0,8.0+sin(time)+4.0,8.0));
	vec3 tc2 = pow(vec3(1.1-lum2),vec3(5.0,position.y+cos(time)+7.0,sin(position.x)+sin(time)+2.0));
	vec3 curr_color = (tc*0.8) + (tc2*0.5);
	
	
	//Let's draw some stars
	
	float scale = sin(0.3 * time) + 5.0;
	vec2 position2 = (((gl_FragCoord.xy / resolution) - 0.5) * scale);
	float gradient = 0.0;
	vec3 color = vec3(0.0);
	float fade = 0.0;
	float z = 0.0;
 	vec2 centered_coord = position2 - vec2(sin(time*0.1),sin(time*0.1));
	centered_coord = rotate(centered_coord, rads);
	
	for (float i=1.0; i<=60.0; i++)
	{
		vec2 star_pos = vec2(sin(i) * 250.0, sin(i*i*i) * 250.0);
		float z = mod(i*i - 10.0*time, 256.0);
		float fade = (256.0 - z) /256.0;
		vec2 blob_coord = star_pos / z;
		gradient += ((fade / 384.0) / pow(length(centered_coord - blob_coord), 1.5)) * ( fade);
	}

	curr_color += gradient;
	
	gl_FragColor = vec4(curr_color, 1.0);
}