package dev.slmpc.vkrenderer.graphics.pipeline

import dev.slmpc.vkrenderer.Context
import org.lwjgl.vulkan.VK10.vkDestroyPipeline

class Pipeline(val handle: Long) {

    constructor(): this(0)

    fun destroy() {
        vkDestroyPipeline(Context.device, handle, null)
    }

}