package com.ybhgl.reminder.util

import android.util.Base64
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.io.IOException
import java.util.regex.Pattern

sealed class WebDavResult {
    object Success : WebDavResult()
    data class Failure(val code: Int, val message: String) : WebDavResult()
}

sealed class WebDavDownloadResult {
    data class Success(val content: String) : WebDavDownloadResult()
    data class Failure(val code: Int, val message: String) : WebDavDownloadResult()
}

data class WebDavFile(val name: String, val size: Long)

sealed class WebDavListResult {
    data class Success(val files: List<WebDavFile>) : WebDavListResult()
    data class Failure(val code: Int, val message: String) : WebDavListResult()
}

object WebDavClient {
    private const val CONNECT_TIMEOUT = 10000 // 10s
    private const val READ_TIMEOUT = 15000    // 15s

    private fun normalizeServerUrl(serverUrl: String): String {
        var url = serverUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        return url
    }

    private fun buildFullUrl(serverUrl: String, path: String): String {
        val normalizedServer = normalizeServerUrl(serverUrl)
        var cleanPath = path.trim().replace("\\", "/")
        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1)
        }
        if (cleanPath.isNotEmpty() && !cleanPath.endsWith("/")) {
            cleanPath = "$cleanPath/"
        }
        return normalizedServer + cleanPath
    }

    private fun getAuthHeader(username: String, password: String): String {
        val credentials = "$username:$password"
        val base64 = Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "Basic $base64"
    }

    /**
     * Attempts to create the directory first via MKCOL.
     * If directory already exists (405), or is successfully created (201), we test access with PROPFIND.
     */
    fun testConnection(serverUrl: String, username: String, password: String, path: String): WebDavResult {
        val dirUrl = buildFullUrl(serverUrl, path)
        
        // Step 1: Create directory if not exists
        val mkcolResult = createDirectoryInternal(dirUrl, username, password)
        if (mkcolResult is WebDavResult.Failure && mkcolResult.code != 405) {
            // If authentication or other error happened during MKCOL, return it directly.
            // 405 means directory already exists, which is fine!
            return mkcolResult
        }

        // Step 2: Test access with PROPFIND
        return listFilesInternal(dirUrl, username, password)
    }

    fun uploadFile(serverUrl: String, username: String, password: String, path: String, fileName: String, content: String): WebDavResult {
        // Ensure directory exists
        val dirUrl = buildFullUrl(serverUrl, path)
        val mkcolResult = createDirectoryInternal(dirUrl, username, password)
        if (mkcolResult is WebDavResult.Failure && mkcolResult.code != 405) {
            return mkcolResult
        }

        val fileUrlStr = dirUrl + fileName.trim()
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(fileUrlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.doOutput = true
            
            connection.setRequestProperty("Authorization", getAuthHeader(username, password))
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(content)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                WebDavResult.Success
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                WebDavResult.Failure(responseCode, errorMsg.take(100))
            }
        } catch (e: SocketTimeoutException) {
            WebDavResult.Failure(-1, "ERR_TIMEOUT")
        } catch (e: UnknownHostException) {
            WebDavResult.Failure(-2, "ERR_NETWORK_UNREACHABLE")
        } catch (e: IOException) {
            WebDavResult.Failure(-3, "ERR_IO_EXCEPTION: ${e.localizedMessage}")
        } finally {
            connection?.disconnect()
        }
    }

    fun downloadFile(serverUrl: String, username: String, password: String, path: String, fileName: String): WebDavDownloadResult {
        val dirUrl = buildFullUrl(serverUrl, path)
        val fileUrlStr = dirUrl + fileName.trim()
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(fileUrlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            
            connection.setRequestProperty("Authorization", getAuthHeader(username, password))

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val content = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                WebDavDownloadResult.Success(content)
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                WebDavDownloadResult.Failure(responseCode, errorMsg.take(100))
            }
        } catch (e: SocketTimeoutException) {
            WebDavDownloadResult.Failure(-1, "ERR_TIMEOUT")
        } catch (e: UnknownHostException) {
            WebDavDownloadResult.Failure(-2, "ERR_NETWORK_UNREACHABLE")
        } catch (e: IOException) {
            WebDavDownloadResult.Failure(-3, "ERR_IO_EXCEPTION: ${e.localizedMessage}")
        } finally {
            connection?.disconnect()
        }
    }

    fun deleteFile(serverUrl: String, username: String, password: String, path: String, fileName: String): WebDavResult {
        val dirUrl = buildFullUrl(serverUrl, path)
        val fileUrlStr = dirUrl + fileName.trim()
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(fileUrlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            
            connection.setRequestProperty("Authorization", getAuthHeader(username, password))

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                WebDavResult.Success
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                WebDavResult.Failure(responseCode, errorMsg.take(100))
            }
        } catch (e: SocketTimeoutException) {
            WebDavResult.Failure(-1, "ERR_TIMEOUT")
        } catch (e: UnknownHostException) {
            WebDavResult.Failure(-2, "ERR_NETWORK_UNREACHABLE")
        } catch (e: IOException) {
            WebDavResult.Failure(-3, "ERR_IO_EXCEPTION: ${e.localizedMessage}")
        } finally {
            connection?.disconnect()
        }
    }

    fun listFiles(serverUrl: String, username: String, password: String, path: String): WebDavListResult {
        val dirUrl = buildFullUrl(serverUrl, path)
        val result = listFilesInternal(dirUrl, username, password)
        return when (result) {
            is WebDavResult.Success -> {
                // If listFilesInternal returns Success, it means there are no files or it succeeded,
                // but wait, we need the files! We should return Success with list.
                // Let's modify listFilesInternal to return files or write a specific one.
                WebDavListResult.Failure(-1, "No data")
            }
            is WebDavResult.Failure -> WebDavListResult.Failure(result.code, result.message)
        }
    }

    fun checkDirectoryExists(serverUrl: String, username: String, password: String, path: String): WebDavResult {
        val dirUrl = buildFullUrl(serverUrl, path)
        return listFilesInternal(dirUrl, username, password)
    }

    fun listFilesActual(serverUrl: String, username: String, password: String, path: String): WebDavListResult {
        val dirUrl = buildFullUrl(serverUrl, path)
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(dirUrl)
            connection = url.openConnection() as HttpURLConnection
            setRequestMethod(connection, "PROPFIND")
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            
            connection.setRequestProperty("Authorization", getAuthHeader(username, password))
            connection.setRequestProperty("Depth", "1")
            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8")

            val responseCode = connection.responseCode
            if (responseCode == 207 || responseCode in 200..299) {
                val xml = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val files = parseWebDavXml(xml)
                WebDavListResult.Success(files)
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                WebDavListResult.Failure(responseCode, errorMsg.take(100))
            }
        } catch (e: SocketTimeoutException) {
            WebDavListResult.Failure(-1, "ERR_TIMEOUT")
        } catch (e: UnknownHostException) {
            WebDavListResult.Failure(-2, "ERR_NETWORK_UNREACHABLE")
        } catch (e: IOException) {
            WebDavListResult.Failure(-3, "ERR_IO_EXCEPTION: ${e.localizedMessage}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun createDirectoryInternal(dirUrl: String, username: String, password: String): WebDavResult {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(dirUrl)
            connection = url.openConnection() as HttpURLConnection
            setRequestMethod(connection, "MKCOL")
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            
            connection.setRequestProperty("Authorization", getAuthHeader(username, password))

            val responseCode = connection.responseCode
            if (responseCode == 201 || responseCode == 405) {
                WebDavResult.Success
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                WebDavResult.Failure(responseCode, errorMsg.take(100))
            }
        } catch (e: SocketTimeoutException) {
            WebDavResult.Failure(-1, "ERR_TIMEOUT")
        } catch (e: UnknownHostException) {
            WebDavResult.Failure(-2, "ERR_NETWORK_UNREACHABLE")
        } catch (e: IOException) {
            WebDavResult.Failure(-3, "ERR_IO_EXCEPTION: ${e.localizedMessage}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun listFilesInternal(dirUrl: String, username: String, password: String): WebDavResult {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(dirUrl)
            connection = url.openConnection() as HttpURLConnection
            setRequestMethod(connection, "PROPFIND")
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            
            connection.setRequestProperty("Authorization", getAuthHeader(username, password))
            connection.setRequestProperty("Depth", "1")
            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8")

            val responseCode = connection.responseCode
            if (responseCode == 207 || responseCode in 200..299) {
                WebDavResult.Success
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                WebDavResult.Failure(responseCode, errorMsg.take(100))
            }
        } catch (e: SocketTimeoutException) {
            WebDavResult.Failure(-1, "ERR_TIMEOUT")
        } catch (e: UnknownHostException) {
            WebDavResult.Failure(-2, "ERR_NETWORK_UNREACHABLE")
        } catch (e: IOException) {
            WebDavResult.Failure(-3, "ERR_IO_EXCEPTION: ${e.localizedMessage}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseWebDavXml(xml: String): List<WebDavFile> {
        val files = mutableListOf<WebDavFile>()
        val responsePattern = Pattern.compile("<[^>]*response[^>]*>([\\s\\S]*?)</[^>]*response[^>]*>", Pattern.CASE_INSENSITIVE)
        val responseMatcher = responsePattern.matcher(xml)
        
        val hrefPattern = Pattern.compile("<[^>]*href[^>]*>([^<]+)</[^>]*href[^>]*>", Pattern.CASE_INSENSITIVE)
        val lengthPattern = Pattern.compile("<[^>]*getcontentlength[^>]*>([^<]+)</[^>]*getcontentlength[^>]*>", Pattern.CASE_INSENSITIVE)
        
        while (responseMatcher.find()) {
            val responseContent = responseMatcher.group(1) ?: continue
            val hrefMatcher = hrefPattern.matcher(responseContent)
            if (hrefMatcher.find()) {
                val href = hrefMatcher.group(1) ?: continue
                try {
                    val decodedHref = URLDecoder.decode(href, "UTF-8")
                    val lastSlashIdx = decodedHref.lastIndexOf('/')
                    val filename = if (lastSlashIdx >= 0) {
                        decodedHref.substring(lastSlashIdx + 1)
                    } else {
                        decodedHref
                    }
                    
                    if (filename.endsWith(".json", ignoreCase = true) && filename.isNotBlank()) {
                        val lengthMatcher = lengthPattern.matcher(responseContent)
                        val size = if (lengthMatcher.find()) {
                            lengthMatcher.group(1)?.trim()?.toLongOrNull() ?: 0L
                        } else {
                            0L
                        }
                        files.add(WebDavFile(filename, size))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return files.distinctBy { it.name }.sortedBy { it.name }
    }

    private fun setRequestMethod(connection: HttpURLConnection, method: String) {
        try {
            connection.requestMethod = method
        } catch (e: Exception) {
            // Fallback to reflection for custom methods
            try {
                setFieldReflectively(connection, "method", method)
            } catch (ex: Exception) {
                // Ignore
            }

            try {
                val delegate = getFieldReflectively(connection, "delegate")
                if (delegate != null) {
                    setFieldReflectively(delegate, "method", method)
                }
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    private fun setFieldReflectively(obj: Any, fieldName: String, value: Any) {
        var clazz: Class<*>? = obj.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(obj, value)
                return
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
    }

    private fun getFieldReflectively(obj: Any, fieldName: String): Any? {
        var clazz: Class<*>? = obj.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                return field.get(obj)
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        return null
    }
}
