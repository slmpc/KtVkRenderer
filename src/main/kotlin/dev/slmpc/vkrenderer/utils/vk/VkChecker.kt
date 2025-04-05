package dev.slmpc.vkrenderer.utils.vk

import org.lwjgl.vulkan.VK10.VK_SUCCESS

fun checkVkResult(result: Int): Boolean {
    if (result != VK_SUCCESS) {
        throw RuntimeException("Vulkan error: $result")
    }
    return true
}