package dev.slmpc.vkrenderer.graphics.pipeline

object RenderPasses {

    val DEFAULT = RenderPass()

    fun destroy() {
        DEFAULT.destroy()
    }

}