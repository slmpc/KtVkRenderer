package dev.slmpc.vkrenderer.graphics.image

import dev.slmpc.vkrenderer.Context
import org.lwjgl.vulkan.VK10.vkDestroyImageView

class ImageView(val handle: Long) {

    constructor(): this(0L)

    fun destroy() {
        vkDestroyImageView(Context.device, handle, null)
    }

}