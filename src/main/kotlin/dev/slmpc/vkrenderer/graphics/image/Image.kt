package dev.slmpc.vkrenderer.graphics.image

import dev.slmpc.vkrenderer.Context
import org.lwjgl.vulkan.VK10

class Image(val handle: Long) {

    constructor(): this(0L)

    fun destroy() {
        VK10.vkDestroyImage(Context.device, handle, null)
    }

}