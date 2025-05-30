package dev.slmpc.vkrenderer.graphics

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.utils.memory.memStack
import io.github.oshai.kotlinlogging.KLoggable
import io.github.oshai.kotlinlogging.KLogger
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.*

import kotlin.use

class Instance: KLoggable {
    override val logger: KLogger
        get() = Context.logger

    val instance: VkInstance

    init {
        memStack.use { stack ->
            val instanceInfo = VkInstanceCreateInfo.calloc(stack)
            instanceInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)

            // Layers
            val layers = getSupportedValidationLayers()
            val enabledLayers = stack.callocPointer(layers.size)
            layers.forEachIndexed { index, layer ->
                println("Using layer: $layer")
                enabledLayers.put(index, stack.UTF8(layer))
            }
            instanceInfo.ppEnabledLayerNames(enabledLayers)

            val debugMessenger = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(
                    VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT or
                            VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or
                            VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                )
                .messageType(
                    VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or
                            VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or
                            VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                )
                .pfnUserCallback { severity, type, pCallbackData, _ ->
                    val message = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData).pMessageString()
                    println("Validation Layer: $message")
                    VK_FALSE
                }
            instanceInfo.pNext(debugMessenger.address())

            // Application
            val appName = stack.UTF8("Vulkan Renderer")
            val appInfo = VkApplicationInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .apiVersion(VK_API_VERSION_1_3)
                .pApplicationName(appName)
                .applicationVersion(1)
                .pEngineName(appName)
                .engineVersion(0)
            instanceInfo.pApplicationInfo(appInfo)

            // Extensions
            val extensions = getExtensions()
            val enabledExtensions = stack.callocPointer(extensions.size)
            extensions.forEachIndexed { index, ext ->
                enabledExtensions.put(index, stack.UTF8(ext))
            }
            instanceInfo.ppEnabledExtensionNames(enabledExtensions)

            val pInstance = stack.mallocPointer(1)
            vkCreateInstance(instanceInfo, null, pInstance)
            instance = VkInstance(pInstance[0], instanceInfo)
        }
    }

    fun destroy() {
        vkDestroyInstance(instance, null)
    }

    private fun getSupportedValidationLayers(): List<String> {
        memStack.use { stack ->
            val numLayersArr = stack.callocInt(1)
            vkEnumerateInstanceLayerProperties(numLayersArr, null)
            val numLayers = numLayersArr[0]

            val propsBuf = VkLayerProperties.calloc(numLayers, stack)
            vkEnumerateInstanceLayerProperties(numLayersArr, propsBuf)
            val supportedLayers = mutableListOf<String>()
            for (i in 0 until numLayers) {
                val props = propsBuf[i]
                val layerName = props.layerNameString()
                supportedLayers.add(layerName)
            }

            val layersToUse = mutableListOf<String>()

            // Main validation layer
            if ("VK_LAYER_KHRONOS_validation" in supportedLayers) {
                layersToUse.add("VK_LAYER_KHRONOS_validation")
                return layersToUse
            }

            // Fallback 1
            if ("VK_LAYER_LUNARG_standard_validation" in supportedLayers) {
                layersToUse.add("VK_LAYER_LUNARG_standard_validation")
                return layersToUse
            }

            // Fallback 2 (set)
            val requestedLayers = mutableListOf<String>()
            requestedLayers.add("VK_LAYER_GOOGLE_threading")
            requestedLayers.add("VK_LAYER_LUNARG_parameter_validation")
            requestedLayers.add("VK_LAYER_LUNARG_object_tracker")
            requestedLayers.add("VK_LAYER_LUNARG_core_validation")
            requestedLayers.add("VK_LAYER_GOOGLE_unique_objects")
            return requestedLayers.stream().filter { it in supportedLayers }.toList()
        }
    }

    private fun getExtensions(): List<String> {
        val extensions = mutableListOf<String>()

        val glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions()
            ?: throw RuntimeException("Failed to find required Vulkan extensions")

        val requiredExtensions = listOf<String>(
            "VK_KHR_surface",
            "VK_KHR_win32_surface",
        )

        requiredExtensions.forEach { requiredExtension ->
            for (i in 0 until glfwExtensions.capacity()) {
                if (glfwExtensions.getStringUTF8(i) != requiredExtension) continue
                extensions.add(requiredExtension)
                println("Found required extension: $requiredExtension")
            }
        }

        extensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)

        return extensions
    }

}