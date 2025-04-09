package dev.slmpc.vkrenderer.graphics.shader

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.utils.memory.memStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import java.nio.ByteBuffer

class Shader(
    vertexLocation: String,
    fragmentLocation: String,
) {

    val vertexModule: ShaderModule
    val fragmentModule: ShaderModule

    val stageInfo: VkPipelineShaderStageCreateInfo.Buffer

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

            // Needn't use stack here, because we need to use it when creating the pipeline
            stageInfo = VkPipelineShaderStageCreateInfo.calloc(2)
            stageInfo[0]
               .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
               .stage(VK_SHADER_STAGE_VERTEX_BIT)
               .module(vertexModule.handle)
               .pName(entryPoint)
            stageInfo[1]
               .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
               .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
               .module(fragmentModule.handle)
               .pName(entryPoint)
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

    companion object {
        private val entryPoint = MemoryUtil.memUTF8("main")
    }
}
