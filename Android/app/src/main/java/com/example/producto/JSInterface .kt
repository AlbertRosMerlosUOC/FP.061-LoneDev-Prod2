
import android.app.Activity
import android.content.Context
import android.webkit.JavascriptInterface

class JSInterface internal constructor(var mContext: Context) {
    @JavascriptInterface
    fun goBackToApp() {
        (mContext as Activity).finish()
    }
}