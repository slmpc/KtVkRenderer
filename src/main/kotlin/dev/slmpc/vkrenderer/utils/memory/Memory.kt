package dev.slmpc.vkrenderer.utils.memory

import org.lwjgl.system.MemoryStack

val memStack: MemoryStack get() = MemoryStack.stackPush()