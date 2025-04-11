package dev.slmpc.vkrenderer.renderer

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.graphics.pipeline.Pipelines
import dev.slmpc.vkrenderer.graphics.pipeline.RenderPasses
import dev.slmpc.vkrenderer.utils.memory.memStack
import dev.slmpc.vkrenderer.utils.vk.checkVkResult
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.*

object GeneralRenderer {

    private val commandBuffer: VkCommandBuffer

    private var drawAvailableSem: Long = 0
    private var drawFinishSem: Long = 0
    private var cmdAvailableFence: Long = 0

    init {
        memStack.use { stack ->
            // Allocate command buffer
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
            val pSemaphore = stack.callocLong(1)
            checkVkResult(vkCreateSemaphore(Context.device, semaphoreInfo, null, pSemaphore))
            drawAvailableSem = pSemaphore[0]
            checkVkResult(vkCreateSemaphore(Context.device, semaphoreInfo, null, pSemaphore))
            drawFinishSem = pSemaphore[0]

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

            // Wait for previous frame to finish
            checkVkResult(vkWaitForFences(Context.device, cmdAvailableFence, true, Long.MAX_VALUE))
            vkResetFences(Context.device, cmdAvailableFence)

            // Acquire next image
            checkVkResult(vkAcquireNextImageKHR(
                Context.device,
                Context.swapChain.swapChain,
                Long.MAX_VALUE,
                drawAvailableSem,
                VK_NULL_HANDLE,
                pImageIndex
            ))
            val imageIndex = pImageIndex[0]

            // Reset command buffer
            vkResetCommandBuffer(commandBuffer, 0)

            // Begin command buffer
            val cmdBufBeginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
            checkVkResult(vkBeginCommandBuffer(commandBuffer, cmdBufBeginInfo))

            // Record commands
            run {
                val clearValue = VkClearValue.calloc(1, stack)
                clearValue[0].color(VkClearColorValue.calloc(stack)
                    .float32(0, 1f)
                    .float32(1, 0f)
                    .float32(2, 0f)
                    .float32(3, 1f))

                val renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(RenderPasses.DEFAULT.renderPass)
                    .framebuffer(Context.swapChain.frameBuffers[imageIndex])
                    .renderArea { it
                        .offset { it.set(0, 0) }
                        .extent(Context.swapChain.imageExtent)
                    }
                    .pClearValues(clearValue)
                    .clearValueCount(1)

                vkCmdBeginRenderPass(commandBuffer, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE)

                val viewPort = VkViewport.calloc(1, stack)
                viewPort[0].x(0f).y(0f)
                    .width(Context.swapChain.imageExtent.width().toFloat())
                    .height(Context.swapChain.imageExtent.height().toFloat())
                    .minDepth(0f)
                    .maxDepth(1f)
                vkCmdSetViewport(commandBuffer, 0, viewPort)

                val scissor = VkRect2D.calloc(1, stack)
                scissor[0].offset { it.x(0).y(0) }
                    .extent(Context.swapChain.imageExtent)
                vkCmdSetScissor(commandBuffer, 0, scissor)

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, Pipelines.GENERAL.pipeline.handle)
                vkCmdDraw(commandBuffer, 3, 1, 0, 0)

                vkCmdEndRenderPass(commandBuffer)
            }
            checkVkResult(vkEndCommandBuffer(commandBuffer))

            // Submit command buffer
            val waitStages = stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            val submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pWaitDstStageMask(waitStages)
                .waitSemaphoreCount(1)
                .pWaitSemaphores(stack.longs(drawAvailableSem))
                .pCommandBuffers(stack.pointers(commandBuffer))
                .pSignalSemaphores(stack.longs(drawFinishSem))

            checkVkResult(vkQueueSubmit(Context.graphicsQueue, submitInfo, cmdAvailableFence))

            // Present frame
            val presentInfo = VkPresentInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(stack.longs(drawFinishSem))
                .swapchainCount(1)
                .pSwapchains(stack.longs(Context.swapChain.swapChain))
                .pImageIndices(stack.ints(imageIndex))

            checkVkResult(vkQueuePresentKHR(Context.presentQueue, presentInfo))
        }
    }

    fun destroy() {
        vkDestroySemaphore(Context.device, drawFinishSem, null)
        vkDestroySemaphore(Context.device, drawAvailableSem, null)
        vkDestroyFence(Context.device, cmdAvailableFence, null)
        vkFreeCommandBuffers(Context.device, Context.commandPool, commandBuffer)
    }
}