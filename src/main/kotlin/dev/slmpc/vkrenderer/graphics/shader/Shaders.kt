package dev.slmpc.vkrenderer.graphics.shader

object Shaders {

    val GENERAL = Shader(
        vertexLocation = "general.vert.spv",
        fragmentLocation = "general.frag.spv",
    )

    fun destroy() {
        GENERAL.destroy()
    }

}