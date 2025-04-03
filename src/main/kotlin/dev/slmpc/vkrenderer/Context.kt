package dev.slmpc.vkrenderer

import dev.slmpc.vkrenderer.graphics.Devices
import dev.slmpc.vkrenderer.graphics.Instance
import io.github.oshai.kotlinlogging.KLoggable
import io.github.oshai.kotlinlogging.KLogger
import org.lwjgl.vulkan.*

object Context: KLoggable {
    override val logger: KLogger = logger()

    private lateinit var instance0: Instance
    val instance: VkInstance get() = instance0.instance

    private lateinit var device0: Devices
    val device: VkDevice get() = device0.device
    val physicalDevice: VkPhysicalDevice get() = device0.physicalDevice

    fun init() {
        instance0 = Instance()
        device0 = Devices()
    }

    fun render() {

    }

    fun cleanup() {
        device0.destroy()
        instance0.destroy()
    }

}