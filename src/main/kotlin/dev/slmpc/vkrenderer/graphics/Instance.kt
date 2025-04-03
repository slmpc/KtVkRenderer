package dev.slmpc.vkrenderer.graphics

import dev.slmpc.vkrenderer.utils.memory.memStack
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.*

import kotlin.use

class Instance {
    val instance: VkInstance

    init {
        memStack.use { stack ->
            val instanceInfo = VkInstanceCreateInfo.calloc(stack)

            // Layers
            val layers = getSupportedValidationLayers()
            val enabledLayers = stack.callocPointer(1)
            for (layer in layers) {
                if (layer == "VK_LAYER_KHRONOS_validation") {
                    enabledLayers.put(stack.UTF8(layer))
                    instanceInfo.ppEnabledLayerNames(enabledLayers)
                    break
                }
            }

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
            for (extension in extensions) {
                enabledExtensions.put(stack.UTF8(extension))
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
            for (i in 0..<numLayers) {
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

        val glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions()?.stringUTF8
            ?: throw RuntimeException("Failed to find required Vulkan extensions")

        val requiredExtensions = listOf<String>(
            "VK_KHR_surface",
            "VK_KHR_win32_surface",
            "VK_EXT_debug_utils",
        )

        requiredExtensions.forEach {
            if (glfwExtensions.contains(it)) {
                extensions.add(it)
            }
        }

        return extensions
    }

}