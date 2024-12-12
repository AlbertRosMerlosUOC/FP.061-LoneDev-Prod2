package com.example.producto2

import JSInterface
import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class HelpActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val webView = findViewById<WebView>(R.id.webViewHelp)

        webView.webViewClient = WebViewClient()
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(JSInterface(this), "Android")

        webView.loadUrl("file:///android_asset/help.html")
    }
}
