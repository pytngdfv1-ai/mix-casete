package com.mixcasete.app.ui.components

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mixcasete.app.audio.YtBridge
import com.mixcasete.app.audio.YtCmd

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OfficialPlayer(modifier: Modifier = Modifier) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            YtBridge.controller = null
            webView?.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                val cm = CookieManager.getInstance()
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(this, true)
                cm.setCookie(".youtube.com", "SOCS=CAI; path=/; secure")
                cm.flush()

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadsImagesAutomatically = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false

                // UA real del dispositivo SIN el marcador "; wv": evita que YouTube
                // redirija la watch page a la app nativa y sirve el reproductor HTML5.
                val baseUA = settings.userAgentString
                settings.userAgentString = baseUA.replace("; wv", "").trim()

                // Registrar el bridge JS ANTES de navegar, para que los eventos del
                // <video> (timeupdate/play/pause/ended) lleguen a Android.
                addJavascriptInterface(object : Any() {
                    @JavascriptInterface fun onReady() { YtBridge.setState { it.copy(ready = true) } }
                    @JavascriptInterface fun onState(s: Int) {
                        YtBridge.setState { st -> st.copy(isPlaying = s == 1, error = if (s == -1) st.error else null) }
                    }
                    @JavascriptInterface fun onTime(t: Double) { YtBridge.setState { it.copy(positionSec = t.toFloat()) } }
                    @JavascriptInterface fun onDur(d: Double) { YtBridge.setState { it.copy(durationSec = d.toFloat()) } }
                    @JavascriptInterface fun onErr(e: Int) { YtBridge.setState { it.copy(error = e, isPlaying = false) } }
                }, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Inyecta el controlador del <video> despues de cada carga
                        view?.evaluateJavascript(CONTROL_JS, null)
                    }
                }
                webChromeClient = WebChromeClient()
                setBackgroundColor(0xFF000000.toInt())

                val wv = this
                YtBridge.controller = object : YtBridge.YtController {
                    // load = navegar a la watch page real (NO embed)
                    override fun load(id: String) { wv.loadUrl("https://www.youtube.com/watch?v=$id") }
                    override fun play() { wv.evaluateJavascript("window.playV&&playV()", null) }
                    override fun pause() { wv.evaluateJavascript("window.pauseV&&pauseV()", null) }
                    override fun seek(sec: Float) { wv.evaluateJavascript("window.seekV&&seekV($sec)", null) }
                    override fun stop() { wv.evaluateJavascript("window.stopV&&stopV()", null) }
                }

                // Tema pendiente (emitido antes de que el WebView existiera) o activo
                val startId = YtBridge.pendingId ?: YtBridge.activeId.value
                YtBridge.pendingId = null
                if (startId != null) wv.loadUrl("https://www.youtube.com/watch?v=$startId")
            }
        }
    )

    // Comandos en vivo (cambio de tema, play/pause/seek/stop)
    LaunchedEffect(Unit) {
        YtBridge.commands.collect { cmd ->
            val c = YtBridge.controller ?: return@collect
            when (cmd) {
                is YtCmd.Load -> c.load(cmd.id)
                YtCmd.Play -> c.play()
                YtCmd.Pause -> c.pause()
                YtCmd.Toggle -> { if (YtBridge.state.value.isPlaying) c.pause() else c.play() }
                is YtCmd.Seek -> c.seek(cmd.sec)
                YtCmd.Stop -> c.stop()
                else -> {}
            }
        }
    }
}

// JS que engancha el <video> de la watch page, reporta estado a Android,
// fuerza autoplay, anula la pausa por "page hidden" (para segundo plano)
// y remueve overlays de consentimiento/"abrir en app" que tapan el reproductor.
private const val CONTROL_JS = """
(function(){
  window.__mixBind=function(){
    var v=document.querySelector('video');
    if(!v){
      if(window.__mixTries===undefined)window.__mixTries=0;
      window.__mixTries++;
      if(window.__mixTries<80) setTimeout(window.__mixBind,250);
      return;
    }
    window.__mixTries=0;
    if(!v.__mixbound){
      v.__mixbound=true;
      v.addEventListener('timeupdate',function(){ try{Android.onTime(v.currentTime);Android.onDur(v.duration||0);}catch(e){} });
      v.addEventListener('play',function(){ try{Android.onState(1);}catch(e){} });
      v.addEventListener('pause',function(){ try{Android.onState(2);}catch(e){} });
      v.addEventListener('ended',function(){ try{Android.onState(0);}catch(e){} });
    }
    try{ Object.defineProperty(document,'hidden',{get:function(){return false;},configurable:true}); }catch(e){}
    try{ Object.defineProperty(document,'visibilityState',{get:function(){return 'visible';},configurable:true}); }catch(e){}
    var kill=['ytd-enforcement-message-view-model','.ytp-inline-player-small','tp-yt-paper-dialog','ytd-consent-bump-v2-lightbox','#dismiss-button','.ytd-enforcement-message-view-model','ytd-mealbar-promo-renderer'];
    kill.forEach(function(s){ try{document.querySelectorAll(s).forEach(function(el){el.remove();});}catch(e){} });
    try{ v.muted=false; v.play(); }catch(e){}
  };
  window.playV=function(){ var v=document.querySelector('video'); if(v){try{v.play();}catch(e){}} };
  window.pauseV=function(){ var v=document.querySelector('video'); if(v){try{v.pause();}catch(e){}} };
  window.seekV=function(s){ var v=document.querySelector('video'); if(v){try{v.currentTime=s;}catch(e){}} };
  window.stopV=function(){ var v=document.querySelector('video'); if(v){try{v.pause();v.currentTime=0;}catch(e){}} };
  window.__mixBind();
})();
"""
