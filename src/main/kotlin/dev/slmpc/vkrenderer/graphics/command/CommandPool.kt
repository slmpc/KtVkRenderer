package dev.slmpc.vkrenderer.graphics.command

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.utils.memory.memStack
import dev.slmpc.vkrenderer.utils.vk.checkVkResult
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandPoolCreateInfo

class CommandPool {

    val commandPool: Long

    init {
        memStack.use { stack ->
            val createInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

            val pCommandPool = stack.mallocLong(1)
            checkVkResult(vkCreateCommandPool(Context.device, createInfo, null, pCommandPool))
            commandPool = pCommandPool[0]
        }
    }

    fun destroy() {
        vkDestroyCommandPool(Context.device, commandPool, null)
    }

}