package dev.slmpc.vkrenderer.graphics.shader

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.utils.memory.memStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import java.nio.ByteBuffer

class Shader(
    vertexLocation: String,
    fragmentLocation: String,
) {

    val vertexModule: ShaderModule
    val fragmentModule: ShaderModule

    init {
        memStack.use { stack ->
            val byteBufferVertex = readShader(vertexLocation)
            val byteBufferFragment = readShader(fragmentLocation)

            val createInfo = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)

            val shaderModule = stack.callocLong(1)

            // Vertex
            createInfo.pCode(byteBufferVertex)
            vkCreateShaderModule(Context.device, createInfo, null, shaderModule)
            vertexModule = ShaderModule(shaderModule[0])

            // Fragment
            createInfo.pCode(byteBufferFragment)
            vkCreateShaderModule(Context.device, createInfo, null, shaderModule)
            fragmentModule = ShaderModule(shaderModule[0])
        }
    }

    private fun readShader(location: String): ByteBuffer {
        val bytes = this::class.java.getResourceAsStream("/assets/shaders/$location")
            ?.readAllBytes()
            ?: throw Exception("Spir-V Shader $location not found")

        val buffer = MemoryUtil.memAlloc(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        return buffer
    }

    fun destroy() {
        vertexModule.destroy()
        fragmentModule.destroy()
    }
}
