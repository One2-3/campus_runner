#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_saturation;
uniform float u_contrast;
uniform vec4 u_overlay;

void main() {
    vec4 source = v_color * texture2D(u_texture, v_texCoords);
    float luminance = dot(source.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 rgb = mix(vec3(luminance), source.rgb, u_saturation);
    rgb = (rgb - vec3(0.5)) * u_contrast + vec3(0.5);
    rgb = mix(rgb, u_overlay.rgb, u_overlay.a);
    gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), source.a);
}
