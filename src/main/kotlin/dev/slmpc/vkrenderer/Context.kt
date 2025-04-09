package dev.slmpc.vkrenderer

import dev.slmpc.vkrenderer.graphics.Devices
import dev.slmpc.vkrenderer.graphics.Instance
import dev.slmpc.vkrenderer.graphics.Surface
import dev.slmpc.vkrenderer.graphics.SwapChain
import dev.slmpc.vkrenderer.graphics.command.CommandPool
import dev.slmpc.vkrenderer.graphics.pipeline.Pipelines
import dev.slmpc.vkrenderer.graphics.pipeline.RenderPasses
import dev.slmpc.vkrenderer.graphics.shader.Shaders
import dev.slmpc.vkrenderer.renderer.GeneralRenderer
import dev.slmpc.vkrenderer.utils.memory.memStack
import io.github.oshai.kotlinlogging.KLoggable
import io.github.oshai.kotlinlogging.KLogger
import org.lwjgl.glfw.GLFW
import org.lwjgl.vulkan.*

object Context: KLoggable {
    override val logger: KLogger = logger()

    var window: Long = 0L; private set
    var width: Int = 0; private set
    var height: Int = 0; private set

    private lateinit var instance0: Instance
    val instance: VkInstance get() = instance0.instance

    lateinit var device0: Devices; private set

    val device: VkDevice get() = device0.device
    val graphicsQueue: VkQueue get() = device0.graphicsQueue
    val presentQueue: VkQueue get() = device0.presentQueue
    val physicalDevice: VkPhysicalDevice get() = device0.physicalDevice

    private lateinit var surface0: Surface
    val surface: Long get() = surface0.handle

    lateinit var swapChain: SwapChain; private set

    private lateinit var commandPool0: CommandPool
    val commandPool: Long get() = commandPool0.commandPool

    fun init(window: Long) {
        this.window = window
        memStack.use { stack ->
            val pWidth = stack.mallocInt(1)
            val pHeight = stack.mallocInt(1)
            GLFW.glfwGetWindowSize(window, pWidth, pHeight)
            width = pWidth[0]
            height = pHeight[0]
        }

        instance0 = Instance()
        surface0 = Surface()
        device0 = Devices()
        swapChain = SwapChain()
        RenderPasses
        swapChain.createFrameBuffers(RenderPasses.DEFAULT)
        Shaders
        Pipelines

        commandPool0 = CommandPool()
        GeneralRenderer
    }

    fun render() {
        GeneralRenderer.render()
    }

    fun cleanup() {
        VK10.vkDeviceWaitIdle(device)

        GeneralRenderer.destroy()
        commandPool0.destroy()

        Pipelines.destroy()
        Shaders.destroy()
        swapChain.destroy()
        RenderPasses.destroy()
        surface0.destroy()
        device0.destroy()
        instance0.destroy()
    }

}