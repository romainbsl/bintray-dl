import io.ktor.client.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.io.File

fun main(): Unit = runBlocking {
    val apiUrl = "https://api.bintray.com/packages/acinq/libs/secp256k1-kmp/files"
//    val apiUrl = "https://api.bintray.com/packages/acinq/libs/tor-mobile-kmp/files"
//    val apiUrl = "https://api.bintray.com/packages/acinq/libs/bitcoin-kmp/files"
    val fileUrl = "https://dl.bintray.com/acinq/libs/"

    val client = HttpClient {
        install(Auth) {
            basic {
                username = TODO("YOUR_BINTRAY_USERNAME")
                password = TODO("YOUR_BINTRAY_API_KEY")
            }
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    val files = client.get<List<BintrayFile>>(apiUrl)

    var count = 0
    files.forEach { f ->
        File(f.dir).mkdirs()
        client.downloadFile(File(f.path), fileUrl + f.path) {
            if (!it) println("unable to download ${f.path}")
            else print("\r ${count++} / ${files.size}")
        }
    }

    println("downloaded: $count/${files.size}")
}

suspend fun HttpClient.downloadFile(file: File, url: String, callback: suspend (boolean: Boolean) -> Unit) {
    val call = this.request<HttpResponse> {
        url(url)
        method = HttpMethod.Get
    }
    if (!call.status.isSuccess()) {
        callback(false)
    }
    call.content.copyAndClose(file.writeChannel())
    return callback(true)
}


@Serializable
data class BintrayFile(
    val name: String,
    val path: String,
) {
    val dir: String get() = path.split("/").dropLast(1).joinToString(separator = "/")
}

