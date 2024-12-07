import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener


class WebSocketManager(
    private val url: String,
    private val callback: WebSocketCallback  // Cambiamos listener por callback
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun connect() {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            // Ahora heredamos correctamente de WebSocketListener de OkHttp3
            override fun onMessage(webSocket: WebSocket, text: String) {
                callback.onMessageReceived(text)  // Usamos nuestro callback
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                callback.onError(t.message ?: "Error de conexión")  // Usamos nuestro callback
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Cerrando conexión")
    }
}


// Interfaz personalizada para nuestros callbacks
interface WebSocketCallback {
    fun onMessageReceived(message: String)
    fun onError(error: String)
}