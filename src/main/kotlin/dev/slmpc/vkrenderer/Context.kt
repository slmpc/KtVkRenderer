package dev.slmpc.vkrenderer

import dev.slmpc.vkrenderer.graphics.Devices
import dev.slmpc.vkrenderer.graphics.Instance
import dev.slmpc.vkrenderer.graphics.Surface
import dev.slmpc.vkrenderer.graphics.SwapChain
import io.github.oshai.kotlinlogging.KLoggable
import io.github.oshai.kotlinlogging.KLogger
import org.lwjgl.vulkan.*

object Context: KLoggable {
    override val logger: KLogger = logger()

    var window: Long = 0L; private set

    private lateinit var instance0: Instance
    val instance: VkInstance get() = instance0.instance

    lateinit var device0: Devices; private set

    val device: VkDevice get() = device0.device
    val graphicsQueue: VkQueue get() = device0.graphicsQueue
    val presentQueue: VkQueue get() = device0.presentQueue
    val physicalDevice: VkPhysicalDevice get() = device0.physicalDevice

    private lateinit var surface0: Surface
    val surface: Long get() = surface0.handle

    private lateinit var swapChain: SwapChain

    fun init(window: Long) {
        this.window = window
        instance0 = Instance()
        surface0 = Surface()
        device0 = Devices()
        swapChain = SwapChain()
    }

    fun render() {

    }

    fun cleanup() {
        swapChain.destroy()
        surface0.destroy()
        device0.destroy()
        instance0.destroy()
    }

}