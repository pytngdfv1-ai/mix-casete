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
                settings.allowFileAccess = false
                settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                // UA real SIN el marcador "; wv": evita que YouTube mande a la app
                // nativa y sirve el reproductor HTML5 de la watch page.
                val baseUA = settings.userAgentString
                settings.userAgentString = baseUA.replace("; wv", "").trim()

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
                        view?.evaluateJavascript(STYLE_JS, null)   // recorta a solo <video>
                        view?.evaluateJavascript(CONTROL_JS, null)  // engancha y controla el video
                    }
                }
                webChromeClient = WebChromeClient()
                setBackgroundColor(0xFF000000.toInt())

                val wv = this
                YtBridge.controller = object : YtBridge.YtController {
                    override fun load(id: String) { wv.loadUrl("https://www.youtube.com/watch?v=$id") }
                    override fun play() { wv.evaluateJavascript("window.playV&&playV()", null) }
                    override fun pause() { wv.evaluateJavascript("window.pauseV&&pauseV()", null) }
                    override fun seek(sec: Float) { wv.evaluateJavascript("window.seekV&&seekV($sec)", null) }
                    override fun stop() { wv.evaluateJavascript("window.stopV&&stopV()", null) }
                }

                val startId = YtBridge.pendingId ?: YtBridge.activeId.value
                YtBridge.pendingId = null
                if (startId != null) wv.loadUrl("https://www.youtube.com/watch?v=$startId")
            }
        }
    )

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

// Oculta toda la pagina watch y deja SOLO el <video> a pantalla de la caja del WebView.
// El overlay propio de sonido (#mixSound) se excluye de la regla hidden.
private const val STYLE_JS = """
(function(){
  var s=document.getElementById('mixcss');
  if(!s){ s=document.createElement('style'); s.id='mixcss'; document.head.appendChild(s); }
  s.textContent = '*{visibility:hidden!important}' +
    'video{visibility:visible!important;position:fixed!important;left:0!important;top:0!important;' +
    'right:0!important;bottom:0!important;width:100%!important;height:100%!important;' +
    'object-fit:contain!important;z-index:2147483647!important;background:#000!important;margin:0!important;padding:0!important}' +
    '#mixSound{visibility:visible!important;position:fixed!important;left:0!important;top:0!important;width:100%!important;height:100%!important;' +
    'z-index:2147483647!important;display:flex!important;align-items:center!important;justify-content:center!important;' +
    'background:rgba(0,0,0,0.45)!important;color:#fff!important;font:700 16px sans-serif!important;cursor:pointer!important}' +
    '#mixSound.hide{display:none!important}';
})();
"""

// Engancha el <video>: reporta estado, intenta autoplay desmutado con click sintético,
// y si YouTube lo bloquea muestra el overlay "Toca para el sonido".
private const val CONTROL_JS = """
(function(){
  function ensureOverlay(){
    var o=document.getElementById('mixSound');
    if(!o){ o=document.createElement('div'); o.id='mixSound'; o.innerHTML='\\u25B6  Toca para el sonido';
      o.addEventListener('click',function(){ var v=document.querySelector('video'); if(v){ try{v.muted=false;v.play();}catch(e){} } o.classList.add('hide'); });
      o.addEventListener('touchend',function(e){ e.preventDefault(); var v=document.querySelector('video'); if(v){ try{v.muted=false;v.play();}catch(e2){} } o.classList.add('hide'); });
      document.body.appendChild(o);
    }
    return o;
  }
  function hideOverlay(){ var o=document.getElementById('mixSound'); if(o) o.classList.add('hide'); }
  function showOverlay(){ ensureOverlay().classList.remove('hide'); }

  window.__mixBind=function(){
    var v=document.querySelector('video');
    if(!v){
      if(window.__mixTries===undefined)window.__mixTries=0;
      window.__mixTries++;
      if(window.__mixTries<120) setTimeout(window.__mixBind,200);
      return;
    }
    window.__mixTries=0;
    if(!v.__mixbound){
      v.__mixbound=true;
      v.addEventListener('timeupdate',function(){ try{Android.onTime(v.currentTime);Android.onDur(v.duration||0);}catch(e){} });
      v.addEventListener('play',function(){ try{Android.onState(1); if(!v.muted) hideOverlay();}catch(e){} });
      v.addEventListener('pause',function(){ try{Android.onState(2);}catch(e){} });
      v.addEventListener('ended',function(){ try{Android.onState(0);}catch(e){} });
      v.addEventListener('volumechange',function(){ if(!v.muted) hideOverlay(); });
    }
    try{ Object.defineProperty(document,'hidden',{get:function(){return false;},configurable:true}); }catch(e){}
    try{ Object.defineProperty(document,'visibilityState',{get:function(){return 'visible';},configurable:true}); }catch(e){}
    // intenta liberar autoplay sin toque: click sintético + unmute + play
    try{ v.dispatchEvent(new MouseEvent('click',{bubbles:true})); }catch(e){}
    try{ v.muted=false; v.play(); }catch(e){}
    setTimeout(function(){ if(v.paused || v.muted) showOverlay(); }, 700);
  };
  window.playV=function(){ var v=document.querySelector('video'); if(v){try{v.muted=false;v.play();hideOverlay();}catch(e){}} };
  window.pauseV=function(){ var v=document.querySelector('video'); if(v){try{v.pause();}catch(e){}} };
  window.seekV=function(s){ var v=document.querySelector('video'); if(v){try{v.currentTime=s;}catch(e){}} };
  window.stopV=function(){ var v=document.querySelector('video'); if(v){try{v.pause();v.currentTime=0;}catch(e){}} };
  window.__mixBind();
})();
"""
