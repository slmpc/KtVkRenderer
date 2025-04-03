package dev.slmpc.vkrenderer.graphics

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.utils.memory.memStack
import io.github.oshai.kotlinlogging.KLoggable
import io.github.oshai.kotlinlogging.KLogger
import org.lwjgl.vulkan.EXTDescriptorIndexing
import org.lwjgl.vulkan.KHRBufferDeviceAddress
import org.lwjgl.vulkan.KHRDeferredHostOperations
import org.lwjgl.vulkan.KHRPipelineLibrary
import org.lwjgl.vulkan.KHRShaderFloatControls
import org.lwjgl.vulkan.KHRSpirv14
import org.lwjgl.vulkan.KHRSwapchain
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import org.lwjgl.vulkan.VkQueue
import org.lwjgl.vulkan.VkQueueFamilyProperties

class Devices: KLoggable {
    override val logger: KLogger get() = Context.logger

    private val queueFamilyIndices: QueueFamilyIndices

    val physicalDevice: VkPhysicalDevice
    val device: VkDevice

    val graphicsQueue: VkQueue

    init {
        physicalDevice = pickupPhysicalDevice()
        queueFamilyIndices = queryQueueFamilyIndices()
        device = createDevice()

        graphicsQueue = getGraphicsQueueFromVk()
    }

    fun destroy() {
        vkDestroyDevice(device, null)
    }

    private fun pickupPhysicalDevice(): VkPhysicalDevice {
        memStack.use { stack ->
            val deviceCount = stack.mallocInt(1)
            vkEnumeratePhysicalDevices(Context.instance, deviceCount, null)
            val devices = stack.mallocPointer(deviceCount.get(0))
            vkEnumeratePhysicalDevices(Context.instance, deviceCount, devices)

            val physicalDevice = VkPhysicalDevice(devices.get(0), Context.instance)
            val deviceProperties = VkPhysicalDeviceProperties.calloc(stack)
            vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties)
            val deviceName = deviceProperties.deviceNameString()
            println("Picked up physical device: $deviceName")

            return physicalDevice
        }
    }

    private fun createDevice(): VkDevice {
        memStack.use { stack ->
            val deviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
            val deviceQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack)

            val queueFamilyIndices = this.queueFamilyIndices

            val priority = 1.0f
            deviceQueueCreateInfo
                .pQueuePriorities(stack.floats(priority))
                .queueFamilyIndex(queueFamilyIndices.graphicsQueue)

            deviceCreateInfo
                .pQueueCreateInfos(deviceQueueCreateInfo)

            val device = stack.callocPointer(1)
            vkCreateDevice(physicalDevice, deviceCreateInfo, null, device)
            return VkDevice(device.get(0), physicalDevice, deviceCreateInfo)
        }
    }

    private fun queryQueueFamilyIndices(): QueueFamilyIndices {
        memStack.use { stack ->
            val count = stack.mallocInt(1)
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, null)
            val properties = VkQueueFamilyProperties.calloc(count.get(0), stack)
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, properties)

            var graphicsQueueIndex = Int.MAX_VALUE

            for (i in 0 until count.get(0)) {
                val property = properties.get(i)
                if ((property.queueFlags() and VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphicsQueueIndex = i
                }
            }

            return QueueFamilyIndices(
                graphicsQueue = graphicsQueueIndex,
            )
        }
    }

    private fun getGraphicsQueueFromVk(): VkQueue {
        memStack.use { stack ->
            val graphicsQueue = stack.callocPointer(1)
            vkGetDeviceQueue(device, queueFamilyIndices.graphicsQueue, 0, graphicsQueue)
            return VkQueue(graphicsQueue.get(0), device)
        }
    }

    val deviceExtensions = listOf(
        KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME,
        EXTDescriptorIndexing.VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME,
        KHRBufferDeviceAddress.VK_KHR_BUFFER_DEVICE_ADDRESS_EXTENSION_NAME,
        "VK_KHR_synchronization2",
        KHRPipelineLibrary.VK_KHR_PIPELINE_LIBRARY_EXTENSION_NAME,
        KHRDeferredHostOperations.VK_KHR_DEFERRED_HOST_OPERATIONS_EXTENSION_NAME,
        KHRSpirv14.VK_KHR_SPIRV_1_4_EXTENSION_NAME,
        KHRShaderFloatControls.VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME
    )

    private data class QueueFamilyIndices(
        val graphicsQueue: Int,
    )

}