package dev.qtremors.osyster.core.device.data.read

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import dev.qtremors.osyster.core.device.data.parse.ProcParsers

internal data class GpuInfo(
    val vendor: String?,
    val renderer: String?,
    val glVersion: String?,
    val glEsDeclared: String?,
    val shadingLanguage: String?,
    val eglVersion: String?,
    val maxTextureSize: Int?,
    val extensionCount: Int?,
    val vulkanVersion: String?,
    val vulkanSupported: Boolean
)

/**
 * Asks the GPU who it is through a throwaway 1×1 off-screen EGL context. Run off the main thread;
 * the answer never changes while the process lives, so callers cache it.
 */
internal object GpuProbe {
    fun probe(context: Context): GpuInfo {
        val declared = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
            ?.deviceConfigurationInfo?.glEsVersion
        val gl = runCatching { queryGl() }.getOrNull()
        val pm = context.packageManager
        val vulkanFeature = pm.systemAvailableFeatures.firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
        return GpuInfo(
            vendor = gl?.vendor,
            renderer = gl?.renderer,
            glVersion = gl?.version,
            glEsDeclared = declared,
            shadingLanguage = gl?.glsl,
            eglVersion = gl?.egl,
            maxTextureSize = gl?.maxTexture,
            extensionCount = gl?.extensions,
            vulkanVersion = vulkanFeature?.version?.let(ProcParsers::vulkanVersion),
            vulkanSupported = vulkanFeature != null
        )
    }

    private data class GlStrings(
        val vendor: String?, val renderer: String?, val version: String?, val glsl: String?,
        val egl: String, val maxTexture: Int?, val extensions: Int
    )

    private fun queryGl(): GlStrings? {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) return null
        try {
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            val attributes = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
            )
            if (!EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0) || count[0] == 0) return null
            val config = configs[0] ?: return null
            val ctx = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
            val surface = EGL14.eglCreatePbufferSurface(display, config, intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0)
            try {
                if (!EGL14.eglMakeCurrent(display, surface, surface, ctx)) return null
                val texture = IntArray(1)
                GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, texture, 0)
                return GlStrings(
                    vendor = GLES20.glGetString(GLES20.GL_VENDOR),
                    renderer = GLES20.glGetString(GLES20.GL_RENDERER),
                    version = GLES20.glGetString(GLES20.GL_VERSION),
                    glsl = GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION),
                    egl = "${version[0]}.${version[1]}",
                    maxTexture = texture[0].takeIf { it > 0 },
                    extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS)?.split(' ')?.count(String::isNotBlank) ?: 0
                )
            } finally {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(display, surface)
                EGL14.eglDestroyContext(display, ctx)
            }
        } finally {
            EGL14.eglTerminate(display)
        }
    }
}
