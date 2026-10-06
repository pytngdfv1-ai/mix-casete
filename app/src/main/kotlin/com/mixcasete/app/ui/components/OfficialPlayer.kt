package com.mixcasete.app.ui.components

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mixcasete.app.audio.AudioPlayerViewModel
import kotlinx.coroutines.delay

private const val ORIGIN = "https://www.youtube.com"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OfficialPlayer(viewModel: AudioPlayerViewModel, modifier: Modifier = Modifier) {
    val currentVideoUrl by viewModel.currentVideoUrl.collectAsState()
    val command by viewModel.ytCommand.collectAsState()
    val errorInfo by viewModel.errorInfo.collectAsState()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var retries by remember { mutableStateOf(0) }
    val html = remember { buildHtml() }

    DisposableEffect(Unit) { onDispose { webView?.destroy() } }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                val cm = CookieManager.getInstance()
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(this, true)
                // Solo la cookie de consentimiento valida (SOCS); NO inventar CONSENT
                cm.setCookie(".youtube.com", "SOCS=CAI; path=/; secure")
                cm.flush()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                // NO falsear el User-Agent: el UA por defecto del WebView evita el chequeo anti-bot
                webViewClient = WebViewClient()
                addJavascriptInterface(object : Any() {
                    @JavascriptInterface fun onState(s: Int) { viewModel.reportYtState(s) }
                    @JavascriptInterface fun onTime(t: Double) { viewModel.reportYtTime(t.toFloat()) }
                    @JavascriptInterface fun onDur(d: Double) { viewModel.reportYtDuration(d.toFloat()) }
                    @JavascriptInterface fun onErr(e: Int) { viewModel.reportYtError(e) }
                }, "Android")
                loadDataWithBaseURL("$ORIGIN/", html, "text/html", "utf-8", null)
                webView = this
            }
        }
    )

    fun loadCurrent() {
        extractId(currentVideoUrl)?.let { id ->
            webView?.evaluateJavascript("window.loadById && loadById('$id')", null)
        }
    }

    LaunchedEffect(currentVideoUrl) { loadCurrent() }

    // Un reintento automatico si el player reporto error (transitorio)
    LaunchedEffect(errorInfo) {
        if (errorInfo != null && retries < 1) {
            retries++
            delay(900)
            webView?.evaluateJavascript("window.retryBoot && retryBoot()", null)
            loadCurrent()
        }
    }

    LaunchedEffect(command) {
        val cmd = command ?: return@LaunchedEffect
        val wv = webView ?: return@LaunchedEffect
        when (cmd.type) {
            "play" -> wv.evaluateJavascript("window.playV && playV()", null)
            "pause" -> wv.evaluateJavascript("window.pauseV && pauseV()", null)
            "seek" -> wv.evaluateJavascript("window.seekV && seekV(${cmd.sec})", null)
            "load" -> loadCurrent()
        }
    }
}

private fun extractId(url: String?): String? {
    if (url == null) return null
    return when {
        url.contains("watch?v=") -> url.substringAfter("watch?v=").substringBefore("&")
        url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
        else -> null
    }
}

private fun buildHtml(): String = """
<html><head><style>
html,body{margin:0;padding:0;background:#000;height:100%;overflow:hidden}
#wrap{position:fixed;inset:0}#player{width:100%;height:100%}
</style></head>
<body><div id="wrap"><div id="player"></div></div>
<script src="https://www.youtube.com/iframe_api"></script>
<script>
var player; var ready=false; var pendingId=null; var booting=false;
function onYouTubeIframeAPIReady(){ boot(); }
function boot(){
  if(booting) return; booting=true;
  player=new YT.Player('player',{
    host:'https://www.youtube.com',
    playerVars:{
      controls:0, rel:0, playsinline:1, modestbranding:1, iv_load_policy:3,
      origin:'https://www.youtube.com'
    },
    events:{
      onReady:function(){ ready=true; booting=false; if(pendingId){ player.loadVideoById(pendingId,0,'default'); pendingId=null; } },
      onStateChange:function(e){ if(window.Android) Android.onState(e.data); },
      onError:function(e){ if(window.Android) Android.onErr(e.data); }
    }
  });
}
function retryBoot(){
  try{ if(player&&player.destroy){ player.destroy(); } }catch(e){}
  var d=document.createElement('div'); d.id='player';
  document.getElementById('wrap').appendChild(d);
  booting=false; boot();
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
</script></body></html>
"""
