#version 450 core

//layout (location = 0) in vec3 Position;
//layout (location = 1) in vec4 Color;

//layout (binding = 0) uniform UniformBufferObject {
//    mat4 u_MVPMatrix;
//} ubo;

layout (location = 0) out vec4 v_Color;

vec4[] positions = vec4[3](
    vec4(-0.5, -0.5, 0.0, 1.0),
    vec4(0.5, -0.5, 0.0, 1.0),
    vec4(0.0, 0.5, 0.0, 1.0)
);

vec4[] colors = vec4[3](
    vec4(1.0, 0.0, 0.0, 1.0),
    vec4(0.0, 1.0, 0.0, 1.0),
    vec4(0.0, 0.0, 1.0, 1.0)
);

void main() {
    gl_Position = positions[gl_VertexIndex];
    v_Color = colors[gl_VertexIndex];
}