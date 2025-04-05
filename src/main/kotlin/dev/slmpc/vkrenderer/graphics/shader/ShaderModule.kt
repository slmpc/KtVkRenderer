package dev.slmpc.vkrenderer.graphics.shader

import dev.slmpc.vkrenderer.Context
import org.lwjgl.vulkan.VK10.vkDestroyShaderModule

class ShaderModule(val handle: Long) {

    constructor(): this(0)

    fun destroy() {
        vkDestroyShaderModule(Context.device, handle, null)
    }

}