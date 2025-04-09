package dev.slmpc.vkrenderer.renderer

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.graphics.pipeline.Pipelines
import dev.slmpc.vkrenderer.graphics.pipeline.RenderPasses
import dev.slmpc.vkrenderer.utils.memory.memStack
import dev.slmpc.vkrenderer.utils.vk.checkVkResult
import org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR
import org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR
import org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkClearColorValue
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo
import org.lwjgl.vulkan.VkCommandBufferBeginInfo
import org.lwjgl.vulkan.VkFenceCreateInfo
import org.lwjgl.vulkan.VkOffset2D
import org.lwjgl.vulkan.VkPresentInfoKHR
import org.lwjgl.vulkan.VkRect2D
import org.lwjgl.vulkan.VkRenderPassBeginInfo
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import org.lwjgl.vulkan.VkSubmitInfo
import org.lwjgl.vulkan.VkViewport

object GeneralRenderer {

    private val commandBuffer: VkCommandBuffer

    private val drawFinishSem: Long
    private val drawAvailableSem: Long
    private val cmdAvailableFence: Long

    private val clearColor = VkClearColorValue.calloc().apply {
        float32(0, 0.2f)
        float32(1, 0.2f)
        float32(2, 0.2f)
        float32(3, 1.0f)
    }

    init {
        memStack.use { stack ->
            val commandBufferInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(Context.commandPool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1)
            val pCommandBuffer = stack.callocPointer(1)
            checkVkResult(vkAllocateCommandBuffers(Context.device, commandBufferInfo, pCommandBuffer))
            commandBuffer = VkCommandBuffer(pCommandBuffer[0], Context.device)

            // Create semaphores
            val semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
            val pSem = stack.callocLong(1)
            checkVkResult(vkCreateSemaphore(Context.device, semaphoreInfo, null, pSem))
            drawFinishSem = pSem[0]
            checkVkResult(vkCreateSemaphore(Context.device, semaphoreInfo, null, pSem))
            drawAvailableSem = pSem[0]

            // Create fence
            val fenceInfo = VkFenceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                .flags(VK_FENCE_CREATE_SIGNALED_BIT)
            val pFence = stack.callocLong(1)
            checkVkResult(vkCreateFence(Context.device, fenceInfo, null, pFence))
            cmdAvailableFence = pFence[0]
        }
    }

    fun render() {
        memStack.use { stack ->
            val pImageIndex = stack.callocInt(1)
            checkVkResult(vkAcquireNextImageKHR(
                Context.device, Context.swapChain.swapChain, Long.MAX_VALUE,
                drawAvailableSem, VK_NULL_HANDLE, pImageIndex
            )) {
                println("Failed to acquire next image")
            }
            val imageIndex = pImageIndex[0]

            // Reset command buffer
            vkResetCommandBuffer(commandBuffer, 0)

            val cmdBufBeginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
            checkVkResult(vkBeginCommandBuffer(commandBuffer, cmdBufBeginInfo))
            run {
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, Pipelines.GENERAL.pipeline.handle)
                val clearValue = VkClearValue.calloc(1, stack)
                clearValue[0].color(clearColor)
                val renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(RenderPasses.DEFAULT.renderPass)
                    .framebuffer(Context.swapChain.frameBuffers[imageIndex])
                    .renderArea(VkRect2D.malloc(stack)
                        .offset(VkOffset2D.malloc(stack).set(0, 0))
                        .extent(Context.swapChain.imageExtent))
                    .pClearValues(clearValue)
                vkCmdBeginRenderPass(commandBuffer, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE)
                run {
                    val viewport = VkViewport.calloc(1, stack)
                        .x(0f) .y(0f)
                        .width(Context.swapChain.imageExtent.width().toFloat())
                        .height(Context.swapChain.imageExtent.height().toFloat())
                        .minDepth(0f) .maxDepth(1f)
                    vkCmdSetViewport(commandBuffer, 0, viewport)
                    val scissor = VkRect2D.calloc(1, stack)
                        .offset(VkOffset2D.calloc(stack).set(0, 0))
                        .extent(Context.swapChain.imageExtent)
                    vkCmdSetScissor(commandBuffer, 0, scissor)

                    vkCmdDraw(commandBuffer, 3, 1, 0, 0)
                }
                vkCmdEndRenderPass(commandBuffer)
            }
            checkVkResult(vkEndCommandBuffer(commandBuffer))

            val summitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(stack.pointers(commandBuffer))
                .pWaitSemaphores(stack.longs(drawAvailableSem))
                .pSignalSemaphores(stack.longs(drawFinishSem))
                .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
            vkQueueSubmit(Context.graphicsQueue, summitInfo, cmdAvailableFence)

            val presentInfo = VkPresentInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pSwapchains(stack.longs(Context.swapChain.swapChain))
                .swapchainCount(1)
                .pImageIndices(stack.ints(imageIndex))
                .pWaitSemaphores(stack.longs(drawFinishSem))
            checkVkResult(vkQueuePresentKHR(Context.presentQueue, presentInfo)) {
                println("Failed to present image")
            }

            checkVkResult(vkWaitForFences(Context.device, cmdAvailableFence, true, Long.MAX_VALUE)) {
                println("Failed to wait for fence")
            }
            vkResetFences(Context.device, cmdAvailableFence)
        }
    }

    fun destroy() {
        vkDestroySemaphore(Context.device, drawFinishSem, null)
        vkDestroySemaphore(Context.device, drawAvailableSem, null)
        vkDestroyFence(Context.device, cmdAvailableFence, null)
        clearColor.free()
        vkFreeCommandBuffers(Context.device, Context.commandPool, commandBuffer)
    }
}