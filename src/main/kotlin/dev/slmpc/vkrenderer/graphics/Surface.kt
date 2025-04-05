package dev.slmpc.vkrenderer.graphics

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.utils.memory.memStack
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.vulkan.VK13.*

class Surface {

    val handle: Long

    init {
        memStack.use { stack ->
            val surface0 = stack.callocLong(1)
            val err = GLFWVulkan.glfwCreateWindowSurface(Context.instance, Context.window, null, surface0)
            if (err != VK_SUCCESS) {
                throw RuntimeException("Failed to create window surface, error code: $err")
            }
            handle = surface0.get(0)
        }
    }

}