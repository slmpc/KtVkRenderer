package dev.slmpc.vkrenderer.graphics.pipeline

import dev.slmpc.vkrenderer.Context
import dev.slmpc.vkrenderer.graphics.shader.Shaders
import org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST

object Pipelines {

    val GENERAL = GraphicsPipeline(
        Context.width, Context.height,
        VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
        RenderPasses.DEFAULT, Shaders.GENERAL
    )

    fun destroy() {
        GENERAL.destroy()
    }

}