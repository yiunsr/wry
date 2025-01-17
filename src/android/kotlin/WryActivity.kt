// Copyright 2020-2023 Tauri Programme within The Commons Conservancy
// SPDX-License-Identifier: Apache-2.0
// SPDX-License-Identifier: MIT

package {{package}}

import {{package}}.RustWebView
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray

abstract class WryActivity : AppCompatActivity() {
    private lateinit var mWebView: RustWebView

    open fun onWebViewCreate(webView: WebView) { }

    private fun setWebView(webView: RustWebView) {
        mWebView = webView
        onWebViewCreate(webView)
    }

    val version: String
        @SuppressLint("WebViewApiAvailability", "ObsoleteSdkInt")
        get() {
            // Check getCurrentWebViewPackage() directly if above Android 8
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return WebView.getCurrentWebViewPackage()?.versionName ?: ""
            }

            // Otherwise manually check WebView versions
            var webViewPackage = "com.google.android.webview"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
              webViewPackage = "com.android.chrome"
            }
            try {
                @Suppress("DEPRECATION")
                val info = packageManager.getPackageInfo(webViewPackage, 0)
                return info.versionName
            } catch (ex: Exception) {
                Logger.warn("Unable to get package info for '$webViewPackage'$ex")
            }

            try {
                @Suppress("DEPRECATION")
                val info = packageManager.getPackageInfo("com.android.webview", 0)
                return info.versionName
            } catch (ex: Exception) {
                Logger.warn("Unable to get package info for 'com.android.webview'$ex")
            }

            // Could not detect any webview, return empty string
            return ""
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        create(this)
    }

    override fun onStart() {
        super.onStart()
        start()
    }

    override fun onResume() {
        super.onResume()
        resume()
    }

    override fun onPause() {
        super.onPause()
        pause()
    }

    override fun onStop() {
        super.onStop()
        stop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        focus(hasFocus)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        save()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        memory()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // 코드 추가 부분
    private fun walkTree(docTree: DocumentFile): JSONArray{
        Logger.debug("walkTree start")
        val jArray = JSONArray()
        for(doc in docTree.listFiles()) {
            if (doc.isDirectory) {
                val resultArray = walkTree(doc)
                for(idx in 0 until resultArray.length()){
                    val item = resultArray[idx]
                    jArray.put(item)
                }
            } else {
                jArray.put(doc.getUri().toString())
            }
        }
        Logger.debug("walkTree end")
        return jArray
    }

    // 코드 추가 부분
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Logger.debug("onActivityResult start")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            Logger.debug("onActivityResult result_ok")
            val directoryUri = data?.data ?: return

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            contentResolver.takePersistableUriPermission(
                directoryUri, takeFlags
            )
            val directoryTag = directoryUri.toString()

            val documentsTree = DocumentFile.fromTreeUri(this.application, directoryUri)?: return
            val jsonFiles = walkTree(documentsTree)

            val script = String.format("""directoryInfo("%s", %s)""", directoryTag, jsonFiles.toString())
            Logger.debug("onActivityResult eval_script")

            mWebView.evaluateJavascript(script, null)

        }
    }



    fun getAppClass(name: String): Class<*> {
        return Class.forName(name)
    }

    companion object {
        init {
            System.loadLibrary("{{library}}")
        }
    }

    private external fun create(activity: WryActivity)
    private external fun start()
    private external fun resume()
    private external fun pause()
    private external fun stop()
    private external fun save()
    private external fun destroy()
    private external fun memory()
    private external fun focus(focus: Boolean)

    {{class-extension}}
}
