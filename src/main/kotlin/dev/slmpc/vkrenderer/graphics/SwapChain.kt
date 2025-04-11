package dev.slmpc.vkrenderer.graphics

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.graphics.image.Image
import dev.slmpc.vkrenderer.graphics.image.ImageView
import dev.slmpc.vkrenderer.graphics.pipeline.RenderPass
import dev.slmpc.vkrenderer.utils.memory.memStack
import dev.slmpc.vkrenderer.utils.vk.checkVkResult
import org.lwjgl.glfw.GLFW
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
import org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR
import org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR
import org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR
import org.lwjgl.vulkan.KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR
import org.lwjgl.vulkan.VK13.*

class SwapChain {

    val swapChain: Long

    val images: List<Image>
    val imageViews: List<ImageView>
    lateinit var frameBuffers: List<Long>; private set

    val imageExtent: VkExtent2D
    val imageCount: Int
    val format: VkSurfaceFormatKHR
    val transform: Int
    val presentMode: Int

    var windowWidth = 0; private set
    var windowHeight = 0; private set

    init {
        memStack.use { stack ->
            val swapChainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .clipped(true)
                .imageArrayLayers(1)
                .surface(Context.surface)
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)

            // Choose surface format
            val formatCount = stack.callocInt(1)
            vkGetPhysicalDeviceSurfaceFormatsKHR(Context.physicalDevice, Context.surface, formatCount, null)
            val surfaceFormats = VkSurfaceFormatKHR.calloc(formatCount[0], stack)
            vkGetPhysicalDeviceSurfaceFormatsKHR(Context.physicalDevice, Context.surface, formatCount, surfaceFormats)
            var format = surfaceFormats[0]
            for (index in 0 until formatCount[0]) {
                val surfaceFormat = surfaceFormats[index]
                if (surfaceFormat.format() == VK_FORMAT_R8G8B8A8_SRGB
                    && surfaceFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                    format = surfaceFormats[index]
                    break
                }
            }
            this.format = format

            // Get surface capabilities
            val capabilities = VkSurfaceCapabilitiesKHR.calloc(stack)
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(Context.physicalDevice, Context.surface, capabilities)
            imageCount = 2.coerceIn(capabilities.minImageCount(), capabilities.maxImageCount())

            val windowWidth = stack.callocInt(1)
            val windowHeight = stack.callocInt(1)
            GLFW.glfwGetWindowSize(Context.window, windowWidth, windowHeight)
            imageExtent = VkExtent2D.calloc()
                .width(windowWidth[0].coerceIn(capabilities.minImageExtent().width(), capabilities.maxImageExtent().width()))
                .height(windowHeight[0].coerceIn(capabilities.minImageExtent().height(), capabilities.maxImageExtent().height()))
            this.windowWidth = windowWidth[0]
            this.windowHeight = windowHeight[0]

            transform = capabilities.currentTransform()

            // Present mode
            val presentModeCount = stack.callocInt(1)
            vkGetPhysicalDeviceSurfacePresentModesKHR(Context.physicalDevice, Context.surface, presentModeCount, null)
            val presentModes = stack.callocInt(presentModeCount[0])
            vkGetPhysicalDeviceSurfacePresentModesKHR(Context.physicalDevice, Context.surface, presentModeCount, presentModes)
            var presentMode = VK_PRESENT_MODE_FIFO_KHR  // FIFO is always available
            for (index in 0 until presentModeCount[0]) {
                val present = presentModes[index]
                if (present == VK_PRESENT_MODE_MAILBOX_KHR) {
                    presentMode = present
                    break
                }
            }
            this.presentMode = presentMode

            swapChainCreateInfo
                .imageColorSpace(format.colorSpace())
                .imageFormat(format.format())
                .imageExtent(imageExtent)
                .minImageCount(imageCount)
                .presentMode(presentMode)
                .preTransform(VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR)

            // Queue family indices
            val indices = Context.device0.queueFamilyIndices
            if (indices.graphicsQueue == indices.presentQueue) {
                swapChainCreateInfo
                    .pQueueFamilyIndices(stack.ints(indices.graphicsQueue))
                    .queueFamilyIndexCount(1)
                    .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
            } else {
                swapChainCreateInfo
                    .pQueueFamilyIndices(stack.ints(indices.graphicsQueue, indices.presentQueue))
                    .queueFamilyIndexCount(2)
                    .imageSharingMode(VK_SHARING_MODE_CONCURRENT)
            }

            // Create swap chain
            val swapChain = stack.callocLong(1)
            checkVkResult(vkCreateSwapchainKHR(Context.device, swapChainCreateInfo, null, swapChain))
            this.swapChain = swapChain[0]

            images = getImagesFromSwapChain()
            imageViews = getImageViewsFromImages()
        }
    }

    private fun getImagesFromSwapChain(): List<Image> {
        memStack.use { stack ->
            val count = stack.callocInt(1)
            vkGetSwapchainImagesKHR(Context.device, swapChain, count, null)
            val images = stack.callocLong(count[0])
            vkGetSwapchainImagesKHR(Context.device, swapChain, count, images)

            val result = mutableListOf<Image>()
            for (index in 0 until count[0]) {
                result.add(Image(images[index]))
            }
            return result
        }
    }

    private fun getImageViewsFromImages(): List<ImageView> {
        memStack.use { stack ->
            return images.map { image ->
                val createInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format.format())
                    .components(VkComponentMapping.calloc(stack)
                        .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .a(VK_COMPONENT_SWIZZLE_IDENTITY))
                    .subresourceRange(VkImageSubresourceRange.calloc(stack)
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseMipLevel(0)
                        .levelCount(1)
                        .baseArrayLayer(0)
                        .layerCount(1))
                    .image(image.handle)
                val imageView = stack.callocLong(1)
                checkVkResult(vkCreateImageView(Context.device, createInfo, null, imageView))
                ImageView(imageView[0])
            }
        }
    }

    fun createFrameBuffers(renderPass: RenderPass) {
        memStack.use { stack ->
            val frameBuffers = mutableListOf<Long>()
            val pFrameBuffer = stack.callocLong(1)
            repeat(images.size) {
                val createInfo = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(renderPass.renderPass)
                    .layers(1)
                    .width(imageExtent.width())
                    .height(imageExtent.height())
                    .pAttachments(stack.longs(imageViews[it].handle))
                    .attachmentCount(1)
                checkVkResult(vkCreateFramebuffer(Context.device, createInfo, null, pFrameBuffer))
                frameBuffers.add(pFrameBuffer[0])
            }
            this.frameBuffers = frameBuffers
        }
    }

    fun destroy() {
        imageExtent.free()
        // Needn't destroy images, they are destroyed by the swap chain
        imageViews.forEach { it.destroy() }
        frameBuffers.forEach { vkDestroyFramebuffer(Context.device, it, null) }
        vkDestroySwapchainKHR(Context.device, swapChain, null)
    }

}