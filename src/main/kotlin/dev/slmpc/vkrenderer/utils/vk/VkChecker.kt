package dev.slmpc.vkrenderer.utils.vk

import org.lwjgl.vulkan.VK10.VK_SUCCESS

fun checkVkResult(result: Int, method: (() -> Unit)? = null): Boolean {
    if (result != VK_SUCCESS) {
        method?.invoke()
        throw RuntimeException("Vulkan error: $result")
    }
    return true
}