# Mix.Casete

Reproductor de casete retro para Android (Kotlin + Jetpack Compose), compilado y firmado con GitHub Actions.

## Características
- Reproductor de casete dibujado 100% con Canvas (tema oscuro), carretes que giran con el audio real.
- Audio en segundo plano con Media3/ExoPlayer + notificación y controles en pantalla de bloqueo.
- Búsqueda en YouTube (NewPipe Extractor) con fallback a previews de iTunes.
- Listas de reproducción propias con Room: crear, renombrar, reordenar (arrastre), borrar, favoritos, shuffle y repeat.
- Modo TV: video limpio a pantalla completa (sin botones ni textos) para duplicar con Wireless Display / Smart View.

## Compilar localmente
```bash
./gradlew assembleDebug
   <!-- Build firmado activado -->
