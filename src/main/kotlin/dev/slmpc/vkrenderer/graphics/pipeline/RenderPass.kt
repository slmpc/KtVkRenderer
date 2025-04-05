package dev.slmpc.vkrenderer.graphics.pipeline

import dev.slmpc.vkrenderer.Context
import org.lwjgl.vulkan.VK10.vkDestroyRenderPass

class RenderPass(val handle: Long) {

    constructor(): this(0)

    fun destroy() {
        vkDestroyRenderPass(Context.device, handle, null)
    }

}