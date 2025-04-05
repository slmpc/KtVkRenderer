#version 450 core

layout (location = 0) in vec3 Position;
layout (location = 1) in vec4 Color;

layout (binding = 0) uniform UniformBufferObject {
    mat4 u_MVPMatrix;
} ubo;

layout (location = 0) out vec4 v_Color;

void main() {
    gl_Position = ubo.u_MVPMatrix * vec4(Position, 1.0);
    v_Color = Color;
}