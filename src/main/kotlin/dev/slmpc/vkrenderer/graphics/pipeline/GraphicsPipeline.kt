package dev.slmpc.vkrenderer.graphics.pipeline

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.graphics.shader.Shader
import dev.slmpc.vkrenderer.utils.memory.memStack
import dev.slmpc.vkrenderer.utils.vk.checkVkResult
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.*

class GraphicsPipeline(
    width: Int, height: Int,
    topology: Int, renderPass: RenderPass,
    private val shader: Shader,
) {

    val pipeline: Pipeline
    val layout: Long

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
            pipelineInfo.pStages(shader.stageInfo)

            // Viewport and scissor
            val viewportInfo = VkPipelineViewportStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .viewportCount(1)
                .scissorCount(1)
                .pScissors(VkRect2D.calloc(1, stack).also {
                    it[0].extent().set(width, height) })
                .pViewports(VkViewport.calloc(1, stack).also {
                    it[0].set(0.0f, 0.0f, width.toFloat(), height.toFloat(), 0.0f, 1.0f) })
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
                .depthTestEnable(false)
                .depthWriteEnable(false)
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

            // Dynamic state
            val dynamicStateEnables = stack.ints(VK_DYNAMIC_STATE_VIEWPORT or VK_DYNAMIC_STATE_SCISSOR)
            val dynamicStateInfo = VkPipelineDynamicStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(dynamicStateEnables)
            pipelineInfo.pDynamicState(dynamicStateInfo)

            // Layout
            layout = createLayout()
            pipelineInfo.layout(layout)

            // Render pass
            pipelineInfo.renderPass(renderPass.renderPass)

            // Create pipeline
            val pPipeline = stack.callocLong(1)
            checkVkResult(vkCreateGraphicsPipelines(Context.device, VK_NULL_HANDLE, pipelineInfo, null, pPipeline))
            pipeline = Pipeline(pPipeline[0])
        }

    }

    private fun createLayout(): Long {
        memStack.use { stack ->
            val layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)

            val pLayout = stack.callocLong(1)
            checkVkResult(vkCreatePipelineLayout(Context.device, layoutInfo, null, pLayout))
            return pLayout[0]
        }
    }

    fun destroy() {
        vkDestroyPipelineLayout(Context.device, layout, null)
        pipeline.destroy()
    }

}