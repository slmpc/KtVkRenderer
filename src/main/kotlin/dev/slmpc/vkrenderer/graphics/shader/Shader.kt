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
            readShader(vertexLocation, byteBufferVertex)
            readShader(fragmentLocation, byteBufferFragment)

            val createInfo = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(byteBufferVertex)

            val shaderModule = stack.callocLong(1)
            vkCreateShaderModule(Context.device, createInfo, null, shaderModule)
            vertexModule = ShaderModule(shaderModule[0])

            createInfo.pCode(byteBufferFragment)
            vkCreateShaderModule(Context.device, createInfo, null, shaderModule)
            fragmentModule = ShaderModule(shaderModule[0])
        }
    }

    private fun readShader(location: String, buffer: ByteBuffer) {
        val bytes = (this::class.java.getResourceAsStream(location)?.readAllBytes()
            ?: throw Exception("Spir-V Shader $location not found"))

        if (buffer.capacity() < bytes.size) {
            MemoryUtil.memRealloc(buffer, bytes.size)
        }

        buffer.put(bytes)
        buffer.flip()
    }

    fun destroy() {
        vertexModule.destroy()
        fragmentModule.destroy()
    }

    companion object {
        val byteBufferVertex: ByteBuffer = MemoryUtil.memAlloc(1)
        val byteBufferFragment: ByteBuffer = MemoryUtil.memAlloc(1)
    }

}