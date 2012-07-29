#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform sampler2D textureSampler;
uniform vec2 texcoordOffset;

varying vec4 vertColor;
varying vec4 vertTexcoord;

#define KERNEL_SIZE 9

// Edge detection kernel
// -1 -1 -1
// -1 +8 -1
// -1 -1 -1
float kernel[KERNEL_SIZE];

vec2 offset[KERNEL_SIZE];

void main(void) {
    int i = 0;
    vec4 sum = vec4(0.0);

    offset[0] = vec2(-texcoordOffset.s, -texcoordOffset.t);
    offset[1] = vec2(0.0, -texcoordOffset.t);
    offset[2] = vec2(texcoordOffset.s, -texcoordOffset.t);

    offset[3] = vec2(-texcoordOffset.s, 0.0);
    offset[4] = vec2(0.0, 0.0);
    offset[5] = vec2(texcoordOffset.s, 0.0);

    offset[6] = vec2(-texcoordOffset.s, texcoordOffset.t);
    offset[7] = vec2(0.0, texcoordOffset.t);
    offset[8] = vec2(texcoordOffset.s, texcoordOffset.t);

    kernel[0] = -1.0;   kernel[1] = -1.0;   kernel[2] = -1.0;
    kernel[3] = -1.0;   kernel[4] = 8.0;    kernel[5] = -1.0;
    kernel[6] = -1.0;   kernel[7] = -1.0;   kernel[8] = -1.0;

    for(i = 0; i < KERNEL_SIZE; i++) {
        vec4 tmp = texture2D(textureSampler, vertTexcoord.st + offset[i]);
        sum += tmp * kernel[i];
    }

    gl_FragColor = vec4(sum.rgb, 1.0) * vertColor;
}
