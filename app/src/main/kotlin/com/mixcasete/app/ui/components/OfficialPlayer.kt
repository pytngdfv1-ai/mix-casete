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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OfficialPlayer(modifier: Modifier = Modifier) {
    val state by YtBridge.state.collectAsState()
    var webView by remember { mutableStateOf<WebView?>(null) }
    val html = remember { buildHtml() }

    DisposableEffect(Unit) {
        onDispose {
            if (YtBridge.controller != null) YtBridge.controller = null
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
                // UA NATIVO del WebView (no falsear): evita el chequeo anti-bot que causaba 152
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                setBackgroundColor(0xFF000000.toInt())
                addJavascriptInterface(object : Any() {
                    @JavascriptInterface fun onReady() { YtBridge.setState { it.copy(ready = true) } }
                    @JavascriptInterface fun onState(s: Int) {
                        YtBridge.setState { st ->
                            st.copy(
                                isPlaying = s == 1,
                                error = if (s == -1) st.error else null
                            )
                        }
                    }
                    @JavascriptInterface fun onTime(t: Double) { YtBridge.setState { it.copy(positionSec = t.toFloat()) } }
                    @JavascriptInterface fun onDur(d: Double) { YtBridge.setState { it.copy(durationSec = d.toFloat()) } }
                    @JavascriptInterface fun onErr(e: Int) { YtBridge.setState { it.copy(error = e, isPlaying = false) } }
                }, "Android")
                loadDataWithBaseURL("https://www.youtube.com/", html, "text/html", "utf-8", null)
                webView = this
                // Registrar controlador y cargar pending si lo hay
                val wv = this
                YtBridge.controller = object : YtBridge.YtController {
                    override fun load(id: String) { wv.evaluateJavascript("window.loadById&&loadById('$id')", null) }
                    override fun play() { wv.evaluateJavascript("window.playV&&playV()", null) }
                    override fun pause() { wv.evaluateJavascript("window.pauseV&&pauseV()", null) }
                    override fun seek(sec: Float) { wv.evaluateJavascript("window.seekV&&seekV($sec)", null) }
                    override fun stop() { wv.evaluateJavascript("window.stopV&&stopV()", null) }
                }
                YtBridge.pendingId?.let { pid ->
                    YtBridge.pendingId = null
                    wv.post { YtBridge.controller?.load(pid) }
                }
            }
        }
    )

    // Reaccionar a comandos (play/pause/seek/stop/load) emitidos por UI o notificacion
    LaunchedEffect(Unit) {
        YtBridge.commands.collect { cmd ->
            val c = YtBridge.controller
            when (cmd) {
                is YtCmd.Load -> {
                    YtBridge.setState { it.copy(videoId = cmd.id, title = cmd.title, artist = cmd.artist, positionSec = 0f, durationSec = 0f, error = null) }
                    if (c != null) c.load(cmd.id) else YtBridge.pendingId = cmd.id
                }
                YtCmd.Play -> c?.play()
                YtCmd.Pause -> c?.pause()
                YtCmd.Toggle -> { if (YtBridge.state.value.isPlaying) c?.pause() else c?.play() }
                is YtCmd.Seek -> c?.seek(cmd.sec)
                YtCmd.Stop -> { c?.stop(); YtBridge.setState { it.copy(videoId = null, isPlaying = false) } }
                else -> {}
            }
        }
    }
}

private fun buildHtml(): String = """
<html><head><meta charset="utf-8"><style>
html,body{margin:0;padding:0;background:#000;height:100%;overflow:hidden}
#wrap{position:fixed;inset:0}#player{width:100%;height:100%}
</style></head>
<body><div id="wrap"><div id="player"></div></div>
<script src="https://www.youtube.com/iframe_api"></script>
<script>
var player; var ready=false; var pendingId=null;
function onYouTubeIframeAPIReady(){
  player=new YT.Player('player',{
    host:'https://www.youtube.com',
    playerVars:{controls:0,rel:0,playsinline:1,modestbranding:1,iv_load_policy:3,origin:'https://www.youtube.com'},
    events:{
      onReady:function(){ ready=true; if(window.Android) Android.onReady(); if(pendingId){ player.loadVideoById(pendingId,0,'default'); pendingId=null; } },
      onStateChange:function(e){ if(window.Android) Android.onState(e.data); },
      onError:function(e){ if(window.Android) Android.onErr(e.data); }
    }
  });
}
setInterval(function(){
  if(ready&&player&&player.getCurrentTime){
    try{ Android.onTime(player.getCurrentTime()); Android.onDur(player.getDuration()); }catch(e){}
  }
},1000);
function loadById(id){ if(ready){ player.loadVideoById(id,0,'default'); } else { pendingId=id; } }
function playV(){ if(ready) player.playVideo(); }
function pauseV(){ if(ready) player.pauseVideo(); }
function seekV(s){ if(ready) player.seekTo(s,true); }
function stopV(){ if(ready){ player.pauseVideo(); try{player.seekTo(0);}catch(e){} } }
</script></body></html>
"""
