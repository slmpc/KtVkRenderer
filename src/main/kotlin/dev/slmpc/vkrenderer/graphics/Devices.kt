package dev.slmpc.vkrenderer.graphics

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.utils.memory.memStack
import io.github.oshai.kotlinlogging.KLoggable
import io.github.oshai.kotlinlogging.KLogger
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR
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

    lateinit var graphicsQueue: VkQueue; private set
    lateinit var presentQueue: VkQueue; private set

    init {
        physicalDevice = pickupPhysicalDevice()
        queueFamilyIndices = queryQueueFamilyIndices()
        device = createDevice()

        getQueuesFromVk()
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
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
            val deviceQueueCreateInfo: VkDeviceQueueCreateInfo.Buffer

            if (queueFamilyIndices.graphicsQueue == queueFamilyIndices.presentQueue) {
                deviceQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack)
                val priority = 1.0f
                deviceQueueCreateInfo
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .pQueuePriorities(stack.floats(priority))
                    .queueFamilyIndex(queueFamilyIndices.graphicsQueue)
            } else {
                deviceQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(2, stack)
                val priority = 1.0f
                deviceQueueCreateInfo[0]
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .pQueuePriorities(stack.floats(priority))
                    .queueFamilyIndex(queueFamilyIndices.graphicsQueue)

                deviceQueueCreateInfo[1]
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .pQueuePriorities(stack.floats(priority))
                    .queueFamilyIndex(queueFamilyIndices.presentQueue)
            }

            deviceCreateInfo.pQueueCreateInfos(deviceQueueCreateInfo)

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
            var presentQueueIndex = Int.MAX_VALUE

            for (i in 0 until count.get(0)) {
                val property = properties.get(i)
                if ((property.queueFlags() and VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphicsQueueIndex = i
                }
                val presentSupport = stack.callocInt(1)
                vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, Context.surface, presentSupport)
                if (presentSupport.get(0) == VK_TRUE) {
                    presentQueueIndex = i
                }

                if (graphicsQueueIndex != Int.MAX_VALUE
                    && presentQueueIndex != Int.MAX_VALUE) break
            }

            return QueueFamilyIndices(
                graphicsQueue = graphicsQueueIndex,
                presentQueue = presentQueueIndex,
            )
        }
    }

    private fun getQueuesFromVk() {
        memStack.use { stack ->
            val graphicsQueue0 = stack.callocPointer(1)
            vkGetDeviceQueue(device, queueFamilyIndices.graphicsQueue, 0, graphicsQueue0)
            graphicsQueue = VkQueue(graphicsQueue0.get(0), device)

            val presentQueue0 = stack.callocPointer(1)
            vkGetDeviceQueue(device, queueFamilyIndices.presentQueue, 0, presentQueue0)
            presentQueue = VkQueue(presentQueue0.get(0), device)
        }
    }

    private data class QueueFamilyIndices(
        val graphicsQueue: Int,
        val presentQueue: Int,
    )

}