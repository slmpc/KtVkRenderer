package dev.slmpc.vkrenderer.graphics.pipeline

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.graphics.shader.Shader
import dev.slmpc.vkrenderer.utils.memory.memStack
import dev.slmpc.vkrenderer.utils.vk.checkVkResult
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR

class GraphicsPipeline(
    width: Int, height: Int,
    topology: Int,
    shader: Shader,
) {

    val pipeline: Pipeline
    val renderPass: RenderPass

    init {

        memStack.use { stack ->
            val pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)

            // Vertex input
            val vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
            pipelineInfo.pVertexInputState(vertexInputInfo)

            // Vertex Assembly
            val inputAssemblyInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
            inputAssemblyInfo.topology(topology)
            pipelineInfo.pInputAssemblyState(inputAssemblyInfo)

            // Shader
            val shaderStage = VkPipelineShaderStageCreateInfo.calloc(2, stack)
            shaderStage[0]  // Vertex shader stage info
                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(VK_SHADER_STAGE_VERTEX_BIT)
                .module(shader.vertexModule.handle)
                .pName(stack.UTF8("main"))

            shaderStage[1]  // Fragment shader stage info
                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                .module(shader.fragmentModule.handle)
                .pName(stack.UTF8("main"))
            pipelineInfo.pStages(shaderStage)

            // Viewport and scissor
            val viewportInfo = VkPipelineViewportStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .viewportCount(1)
                .scissorCount(1)
                .pScissors(VkRect2D.calloc(1, stack).also { it[0].extent().set(width, height) })
                .pViewports(VkViewport.calloc(1, stack).also { it[0].set(0.0f, 0.0f, width.toFloat(), height.toFloat(), 0.0f, 1.0f) })
            pipelineInfo.pViewportState(viewportInfo)

            // Rasterization
            val rasterizationInfo = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .polygonMode(VK_POLYGON_MODE_FILL)
                .cullMode(VK_CULL_MODE_BACK_BIT)
                .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                .lineWidth(1.0f)
                .depthClampEnable(false)
            pipelineInfo.pRasterizationState(rasterizationInfo)

            // Multisampling
            val multisamplingInfo = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                .sampleShadingEnable(false)
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
            pipelineInfo.pMultisampleState(multisamplingInfo)

            // Tests
            val depthStencilInfo = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                .depthTestEnable(true)
                .depthWriteEnable(true)
                .depthCompareOp(VK_COMPARE_OP_LESS)
                .depthBoundsTestEnable(false)
                .stencilTestEnable(false)
            pipelineInfo.pDepthStencilState(depthStencilInfo)

            // Color blending
            val colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                .colorWriteMask(
                    VK_COLOR_COMPONENT_R_BIT or
                    VK_COLOR_COMPONENT_G_BIT or
                    VK_COLOR_COMPONENT_B_BIT or
                    VK_COLOR_COMPONENT_A_BIT
                )
            val colorBlendInfo = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .logicOpEnable(false)
                .logicOp(VK_LOGIC_OP_COPY)
                .pAttachments(colorBlendAttachment)
            pipelineInfo.pColorBlendState(colorBlendInfo)

            val renderPass = createRenderPass(width, height)
            pipelineInfo.renderPass(renderPass.handle)
            this.renderPass = renderPass

            // Create pipeline
            val pPipeline = stack.mallocLong(1)
            checkVkResult(vkCreateGraphicsPipelines(Context.device, VK_NULL_HANDLE, pipelineInfo, null, pPipeline))
            pipeline = Pipeline(pPipeline[0])
        }

    }

    private fun createRenderPass(width: Int, height: Int): RenderPass {
        memStack.use { stack ->
            val createInfo = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)

            val attachments = VkAttachmentDescription.calloc(2, stack)
            attachments[0]  // Color attachment
                .format(Context.swapChain.format.format())
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
            attachments[1]  // Depth attachment
                .format(VK_FORMAT_D32_SFLOAT)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
            createInfo.pAttachments(attachments)

            val colorReference = VkAttachmentReference.calloc(1, stack)
                .attachment(0)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            val depthReference = VkAttachmentReference.calloc(1, stack)
                .attachment(1)
                .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
            val subPass = VkSubpassDescription.calloc(1, stack)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .pColorAttachments(colorReference)
                .pDepthStencilAttachment(depthReference[0])
            createInfo.pSubpasses(subPass)

            val dependencies = VkSubpassDependency.calloc(2, stack)
            dependencies[0]  // Color attachment
                .srcSubpass(VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(0)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT)
            dependencies[1]  // Depth attachment
                .srcSubpass(0)
                .dstSubpass(VK_SUBPASS_EXTERNAL)
                .srcStageMask(VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT)
                .srcAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT)
                .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT)
            createInfo.pDependencies(dependencies)

            val pRenderPass = stack.mallocLong(1)
            checkVkResult(vkCreateRenderPass(Context.device, createInfo, null, pRenderPass))
            return RenderPass(pRenderPass[0])
        }
    }

    fun destroy() {
        pipeline.destroy()
    }

}