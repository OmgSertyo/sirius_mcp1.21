#version 330 core

// Uniforms
uniform vec2 size;
uniform float round;
uniform vec2 smoothness;
uniform vec2 swap;
uniform vec4 firstColor, secondColor;

// Input
in vec2 TexCoord;  // Texture coordinates from the vertex shader

// Output
out vec4 fragColor;

// Function to calculate distance
float dstfn(vec2 p, vec2 b, float r) {
    return length(max(abs(p) - b, .0f)) - r;
}

void main() {
    vec2 pixel = TexCoord * size;
    vec2 centre = .5f * size;

    // Calculate smooth step value
    float sa = smoothstep(smoothness.x, smoothness.y, dstfn(centre - pixel, centre - round - 1.f, round));

    // Interpolate between first and second colors
    vec4 result = mix(firstColor, secondColor, clamp(min(pixel.x - swap.x, pixel.y - swap.y), 0.0f, 1.0f));

    // Mix with smooth step for the final effect
    vec4 c = mix(vec4(result.rgb, 1.0f), vec4(result.rgb, 0.0f), sa);

    // Set the final fragment color with alpha blending
    fragColor = vec4(c.rgb, result.a * c.a);
}
