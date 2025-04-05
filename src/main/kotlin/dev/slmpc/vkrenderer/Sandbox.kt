package dev.slmpc.vkrenderer

import org.lwjgl.glfw.GLFW

object Sandbox {

    @JvmStatic
    fun main(args: Array<String>) {
        println("Render engine is running...")

        if (!GLFW.glfwInit()) {
            throw IllegalStateException("Failed to initialize GLFW")
        }

        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE)
        val glfwWindow = GLFW.glfwCreateWindow(800, 600, "Vulkan Renderer", 0, 0)

        Context.init(glfwWindow)

        if (glfwWindow == 0L) {
            GLFW.glfwTerminate()
            throw IllegalStateException("Failed to create GLFW window")
        }

        GLFW.glfwShowWindow(glfwWindow)
        GLFW.glfwMakeContextCurrent(glfwWindow)

        while (!GLFW.glfwWindowShouldClose(glfwWindow)) {
            Context.render()

            GLFW.glfwPollEvents()
        }

        Context.cleanup()

        GLFW.glfwDestroyWindow(glfwWindow)
        GLFW.glfwTerminate()
    }

}