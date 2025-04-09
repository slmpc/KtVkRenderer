package dev.slmpc.vkrenderer.graphics.pipeline

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.utils.memory.memStack
import dev.slmpc.vkrenderer.utils.vk.checkVkResult
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkAttachmentDescription
import org.lwjgl.vulkan.VkAttachmentReference
import org.lwjgl.vulkan.VkRenderPassCreateInfo
import org.lwjgl.vulkan.VkSubpassDependency
import org.lwjgl.vulkan.VkSubpassDescription

class RenderPass {

    val renderPass: Long

    init {
        memStack.use { stack ->
            val renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)

            val attachmentDesc = VkAttachmentDescription.calloc(1, stack)
            attachmentDesc[0]
                .format(Context.swapChain.format.format())
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)

            renderPassInfo.pAttachments(attachmentDesc)

            val attachmentRef = VkAttachmentReference.calloc(1, stack)
            attachmentRef[0]
                .attachment(0)      // color attachment in attachment desc at index 0
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

            val subpassDesc = VkSubpassDescription.calloc(1, stack)
            subpassDesc[0]
               .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
               .pColorAttachments(attachmentRef)
            renderPassInfo.pSubpasses(subpassDesc)

            val dependencies = VkSubpassDependency.calloc(1, stack)
            dependencies[0]
                .srcSubpass(VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT or VK_ACCESS_COLOR_ATTACHMENT_READ_BIT)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            renderPassInfo.pDependencies(dependencies)

            val pRenderPass = stack.callocLong(1)
            checkVkResult(vkCreateRenderPass(Context.device, renderPassInfo, null, pRenderPass))
            renderPass = pRenderPass.get(0)
        }
    }

    fun destroy() {
        vkDestroyRenderPass(Context.device, renderPass, null)
    }

}