package dev.slmpc.vkrenderer

import org.lwjgl.glfw.GLFW

object Sandbox {

    @JvmStatic
    fun main(args: Array<String>) {
        println("Render engine is running...")

        if (!GLFW.glfwInit()) {
            throw IllegalStateException("Failed to initialize GLFW")
        }

        val glfwWindow = GLFW.glfwCreateWindow(800, 600, "Vulkan Renderer", 0, 0)

        Context.init()

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