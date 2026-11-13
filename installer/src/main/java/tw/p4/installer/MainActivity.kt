package tw.p4.installer

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 選一個 APK 檔，用 PackageInstaller session 安裝。
 * 這種安裝方式的來源是「未指定」，Android 13 不會把裝好的 App 標成「受限制的設定」。
 */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var allowButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (32 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Color.rgb(0x2B, 0x32, 0x45))
        }
        fun text(s: String, size: Float) = TextView(this).apply {
            text = s; textSize = size; setTextColor(Color.WHITE); setPadding(0, 0, 0, pad / 2)
        }
        root.addView(text("P4 安裝器", 32f))
        root.addView(
            text(
                "用來安裝 P4 桌面，裝好後才能開「通知存取」。\n" +
                    "第一次使用：先把舊的 P4 桌面解除安裝，再按下面的「選擇 APK 並安裝」，選 tw-p4-launcher-debug.apk。\n" +
                    "之後更新 P4 桌面也請用這裡安裝（直接覆蓋即可，設定會保留）。",
                20f,
            ),
        )
        allowButton = Button(this).apply {
            text = "允許本 App 安裝應用程式"
            textSize = 22f
            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")),
                )
            }
        }
        root.addView(allowButton)
        root.addView(Button(this).apply {
            text = "選擇 APK 並安裝"
            textSize = 22f
            setOnClickListener { pick() }
        })
        status = text("", 20f).apply { setPadding(0, pad / 2, 0, 0) }
        root.addView(status)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        val allowed = packageManager.canRequestPackageInstalls()
        allowButton.visibility = if (allowed) android.view.View.GONE else android.view.View.VISIBLE
        if (!allowed) status.text = "請先按「允許本 App 安裝應用程式」並打開開關。"
        else if (status.text.startsWith("請先")) status.text = ""
    }

    private fun pick() {
        if (!packageManager.canRequestPackageInstalls()) {
            status.text = "請先按「允許本 App 安裝應用程式」並打開開關。"
            return
        }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
        }
        runCatching { startActivityForResult(intent, REQ_PICK) }
            .onFailure { status.text = "找不到檔案選擇器：${it.message}" }
    }

    @Deprecated("Activity 結果 API；這個小工具不用 AndroidX")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode != REQ_PICK || resultCode != RESULT_OK || uri == null) return
        status.text = "安裝中：${displayName(uri)}…"
        Thread { install(uri) }.start()
    }

    private fun displayName(uri: Uri): String =
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment.orEmpty()

    private fun install(uri: Uri) {
        val result = runCatching {
            val installer = packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val id = installer.createSession(params)
            installer.openSession(id).use { session ->
                contentResolver.openInputStream(uri)!!.use { input ->
                    session.openWrite("base.apk", 0, -1).use { out ->
                        input.copyTo(out)
                        session.fsync(out)
                    }
                }
                val pi = PendingIntent.getBroadcast(
                    this, id, Intent(this, InstallResultReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                session.commit(pi.intentSender)
            }
        }
        runOnUiThread {
            status.text = result.fold(
                { "已送出安裝，請在跳出的視窗按「安裝」。" },
                { "安裝失敗：${it.message}" },
            )
        }
    }

    private companion object {
        const val REQ_PICK = 1
    }
}
