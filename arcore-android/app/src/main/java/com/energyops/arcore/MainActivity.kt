package com.energyops.arcore

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.Surface
import android.widget.FrameLayout
import android.widget.TextView
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : Activity(), GLSurfaceView.Renderer {
    private lateinit var glView: GLSurfaceView
    private lateinit var statusView: TextView
    @Volatile private var arSession: Session? = null
    @Volatile private var installRequested = false
    private val status = AtomicReference(
        "ENERGY OPS · ARCORE 0.1\nPreparando câmera e rastreamento espacial…"
    )
    private var cameraTextureId = 0
    private var frameCount = 0
    private var lastUiUpdate = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glView = GLSurfaceView(this).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setRenderer(this@MainActivity)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        statusView = TextView(this).apply {
            text = status.get()
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xCC08111D.toInt())
            setPadding(22, 24, 22, 24)
            gravity = Gravity.TOP or Gravity.START
        }

        val root = FrameLayout(this)
        root.addView(
            glView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            statusView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            )
        )
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        try {
            val installStatus = ArCoreApk.getInstance()
                .requestInstall(this, !installRequested)
            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                installRequested = true
                status.set("Instalação/atualização do Google Play Services for AR solicitada. Abra o app novamente após concluir.")
                updateStatusOnUi()
                return
            }

            if (arSession == null) {
                arSession = Session(this)
                val config = Config(arSession)
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                config.planeFindingMode = Config.PlaneFindingMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                arSession?.configure(config)
            }
            arSession?.resume()
            glView.onResume()
            status.set("ARCore iniciado. Mova e gire o celular em várias direções.")
            updateStatusOnUi()
        } catch (e: UnavailableException) {
            status.set("ARCORE INDISPONÍVEL: ${e.message ?: e.javaClass.simpleName}\nVerifique compatibilidade do aparelho e Google Play Services for AR.")
            updateStatusOnUi()
        } catch (e: Exception) {
            status.set("FALHA AO INICIAR ARCORE: ${e.message ?: e.javaClass.simpleName}")
            updateStatusOnUi()
        }
    }

    override fun onPause() {
        glView.onPause()
        try {
            arSession?.pause()
        } catch (_: Exception) {
        }
        super.onPause()
    }

    override fun onDestroy() {
        try {
            arSession?.close()
        } catch (_: Exception) {
        }
        arSession = null
        super.onDestroy()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        cameraTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        try {
            arSession?.setCameraTextureName(cameraTextureId)
        } catch (_: Exception) {
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        try {
            arSession?.setDisplayGeometry(
                Surface.ROTATION_0,
                width,
                height
            )
        } catch (_: Exception) {
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        val session = arSession ?: return
        try {
            session.setCameraTextureName(cameraTextureId)
            val frame = session.update()
            val camera = frame.camera
            frameCount++

            val tracking = camera.trackingState
            val pose = camera.pose
            val t = pose.translation
            val q = pose.rotationQuaternion

            val trackingLabel = when (tracking) {
                TrackingState.TRACKING -> "TRACKING · pose atualizada"
                TrackingState.PAUSED -> "PAUSED · procurando referências visuais"
                TrackingState.STOPPED -> "STOPPED · rastreamento encerrado"
                else -> tracking.toString()
            }

            val message = String.format(
                Locale.US,
                "ENERGY OPS · ARCORE 0.1\n" +
                    "CÂMERA: %s\n" +
                    "FRAMES: %d\n" +
                    "POSIÇÃO LOCAL (m)\nX: %.3f   Y: %.3f   Z: %.3f\n" +
                    "QUATERNION\nX: %.4f   Y: %.4f   Z: %.4f   W: %.4f\n\n" +
                    "TESTE: mantenha o app aberto, vire de costas para o marcador e caminhe devagar.\n" +
                    "Esta versão mede o rastreamento local; ainda não compartilha coordenadas entre celulares.",
                trackingLabel,
                frameCount,
                t[0], t[1], t[2],
                q[0], q[1], q[2], q[3]
            )

            val now = System.currentTimeMillis()
            if (now - lastUiUpdate > 250) {
                lastUiUpdate = now
                status.set(message)
                runOnUiThread { statusView.text = status.get() }
            }
        } catch (e: CameraNotAvailableException) {
            status.set("CÂMERA INDISPONÍVEL: ${e.message ?: "sem detalhes"}")
            updateStatusOnUi()
        } catch (e: Exception) {
            status.set("ERRO DE FRAME ARCORE: ${e.message ?: e.javaClass.simpleName}")
            updateStatusOnUi()
        }
    }

    private fun updateStatusOnUi() {
        runOnUiThread {
            if (::statusView.isInitialized) statusView.text = status.get()
        }
    }
}
