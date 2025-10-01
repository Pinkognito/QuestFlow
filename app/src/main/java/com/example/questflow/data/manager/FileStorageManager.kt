package com.example.questflow.data.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FileStorageManager"
        private const val COLLECTIONS_DIR = "collections"
        private const val MEDIA_LIBRARY_DIR = "media_library"
        private const val GLOBAL_DIR = "global"
        private const val MAX_IMAGE_SIZE = 1920 // Max width/height in pixels
        private const val COMPRESSION_QUALITY = 85

        private val SUPPORTED_FORMATS = setOf("jpg", "jpeg", "png", "gif", "webp")
    }

    private val collectionsDir: File by lazy {
        File(context.filesDir, COLLECTIONS_DIR).apply {
            if (!exists()) {
                mkdirs()
                Log.d(TAG, "Created collections directory: $absolutePath")
            }
        }
    }

    private val mediaLibraryDir: File by lazy {
        File(context.filesDir, MEDIA_LIBRARY_DIR).apply {
            if (!exists()) {
                mkdirs()
                Log.d(TAG, "Created media library directory: $absolutePath")
            }
        }
    }

    /**
     * Save a single image from URI to storage
     * @param uri Source image URI
     * @param categoryId Category ID or null for global
     * @return Local file URI string or null if failed
     */
    suspend fun saveImage(uri: Uri, categoryId: Long?): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving image from URI: $uri for category: $categoryId")

            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: $uri")
                return@withContext null
            }

            inputStream.use { input ->
                // Decode and compress bitmap
                val bitmap = BitmapFactory.decodeStream(input)
                if (bitmap == null) {
                    Log.e(TAG, "Failed to decode bitmap from URI: $uri")
                    return@withContext null
                }

                val resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE)
                val targetFile = createTargetFile(categoryId)

                saveBitmapToFile(resizedBitmap, targetFile)

                if (!bitmap.isRecycled) bitmap.recycle()
                if (!resizedBitmap.isRecycled) resizedBitmap.recycle()

                val savedUri = Uri.fromFile(targetFile).toString()
                Log.d(TAG, "Image saved successfully: $savedUri")
                savedUri
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image", e)
            null
        }
    }

    /**
     * Save multiple images from URIs
     * @return List of saved file URIs
     */
    suspend fun saveImages(uris: List<Uri>, categoryId: Long?): List<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Saving ${uris.size} images for category: $categoryId")
        uris.mapNotNull { uri ->
            saveImage(uri, categoryId)
        }
    }

    /**
     * Extract and save images from ZIP file
     * @param zipUri ZIP file URI
     * @param categoryId Category ID or null for global
     * @return List of saved file URIs
     */
    suspend fun extractAndSaveZip(zipUri: Uri, categoryId: Long?): List<String> = withContext(Dispatchers.IO) {
        val savedUris = mutableListOf<String>()

        try {
            Log.d(TAG, "Extracting ZIP from URI: $zipUri")

            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    var processedCount = 0

                    while (entry != null) {
                        val fileName = entry.name.lowercase()
                        val extension = fileName.substringAfterLast('.', "")

                        if (!entry.isDirectory && SUPPORTED_FORMATS.contains(extension)) {
                            Log.d(TAG, "Processing ZIP entry: ${entry.name}")

                            try {
                                // Read entry to byte array
                                val bytes = zipStream.readBytes()
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                                if (bitmap != null) {
                                    val resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE)
                                    val targetFile = createTargetFile(categoryId)

                                    saveBitmapToFile(resizedBitmap, targetFile)

                                    if (!bitmap.isRecycled) bitmap.recycle()
                                    if (!resizedBitmap.isRecycled) resizedBitmap.recycle()

                                    savedUris.add(Uri.fromFile(targetFile).toString())
                                    processedCount++
                                    Log.d(TAG, "Successfully saved image from ZIP: ${entry.name}")
                                } else {
                                    Log.w(TAG, "Failed to decode image from ZIP entry: ${entry.name}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing ZIP entry: ${entry.name}", e)
                            }
                        }

                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }

                    Log.d(TAG, "ZIP extraction complete. Processed $processedCount images")
                }
            } ?: Log.e(TAG, "Failed to open input stream for ZIP")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting ZIP file", e)
        }

        savedUris
    }

    /**
     * Delete an image file
     */
    suspend fun deleteImage(fileUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(Uri.parse(fileUri).path ?: return@withContext false)
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "Deleted image: $fileUri")
            } else {
                Log.w(TAG, "Failed to delete image: $fileUri")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image", e)
            false
        }
    }

    /**
     * Get storage directory for a category
     */
    private fun getCategoryDir(categoryId: Long?): File {
        val dirName = if (categoryId == null) GLOBAL_DIR else "category_$categoryId"
        return File(collectionsDir, dirName).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Create a unique target file
     */
    private fun createTargetFile(categoryId: Long?): File {
        val dir = getCategoryDir(categoryId)
        val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        return File(dir, fileName)
    }

    /**
     * Resize bitmap if needed
     */
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Save bitmap to file
     */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, output)
            output.flush()
        }
    }

    /**
     * Clear all collection images (for testing/reset)
     */
    suspend fun clearAllImages(): Boolean = withContext(Dispatchers.IO) {
        try {
            collectionsDir.deleteRecursively()
            collectionsDir.mkdirs()
            Log.d(TAG, "Cleared all collection images")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing images", e)
            false
        }
    }

    /**
     * Save media to media library (centralized storage)
     * Handles both images (with compression) and audio files (raw copy)
     * @param uri Source media URI
     * @return Pair of (file path, file size) or null if failed
     */
    suspend fun saveToMediaLibrary(uri: Uri): Pair<String, Long>? = withContext(Dispatchers.IO) {
        try {
            val mimeType = getMimeType(uri)
            val fileName = getFileName(uri)
            Log.d(TAG, "Saving media to library from URI: $uri (MIME: $mimeType, fileName: $fileName)")

            // Check if it's a GIF by both MIME type and file extension
            val isGif = mimeType == "image/gif" || fileName?.lowercase()?.endsWith(".gif") == true
            val isImage = mimeType?.startsWith("image/") == true && !isGif
            val isAudio = mimeType?.startsWith("audio/") == true

            Log.d(TAG, "Media type detection: isGif=$isGif, isImage=$isImage, isAudio=$isAudio")

            if (isGif) {
                // GIFs must be saved as raw files to preserve animation
                Log.d(TAG, "Saving as GIF (raw copy)")
                return@withContext saveGifToMediaLibrary(uri)
            } else if (isImage) {
                // Process as image with compression
                Log.d(TAG, "Saving as Image (with compression)")
                return@withContext saveImageToMediaLibrary(uri)
            } else if (isAudio) {
                // Process as audio (raw copy)
                Log.d(TAG, "Saving as Audio (raw copy)")
                return@withContext saveAudioToMediaLibrary(uri)
            } else {
                // Fallback: try raw copy for unknown types
                Log.w(TAG, "Unknown MIME type, attempting raw copy: $mimeType")
                return@withContext saveRawFileToMediaLibrary(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving media to library", e)
            null
        }
    }

    /**
     * Save image with compression
     */
    private fun saveImageToMediaLibrary(uri: Uri): Pair<String, Long>? {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.e(TAG, "Failed to open input stream for URI: $uri")
            return null
        }

        inputStream.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: $uri")
                return null
            }

            val resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE)
            val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            val targetFile = File(mediaLibraryDir, fileName)

            saveBitmapToFile(resizedBitmap, targetFile)

            if (!bitmap.isRecycled) bitmap.recycle()
            if (!resizedBitmap.isRecycled) resizedBitmap.recycle()

            val filePath = targetFile.absolutePath
            val fileSize = targetFile.length()

            Log.d(TAG, "Image saved to media library: $filePath (size: $fileSize bytes)")
            return Pair(filePath, fileSize)
        }
    }

    /**
     * Save GIF file with raw copy to preserve animation
     */
    private fun saveGifToMediaLibrary(uri: Uri): Pair<String, Long>? {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.e(TAG, "Failed to open input stream for GIF URI: $uri")
            return null
        }

        inputStream.use { input ->
            val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.gif"
            val targetFile = File(mediaLibraryDir, fileName)

            // Raw copy to preserve animation
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
                output.flush()
            }

            val filePath = targetFile.absolutePath
            val fileSize = targetFile.length()

            Log.d(TAG, "GIF saved to media library: $filePath (size: $fileSize bytes)")
            return Pair(filePath, fileSize)
        }
    }

    /**
     * Save audio file with raw copy
     */
    private fun saveAudioToMediaLibrary(uri: Uri): Pair<String, Long>? {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.e(TAG, "Failed to open input stream for audio URI: $uri")
            return null
        }

        inputStream.use { input ->
            // Get original filename extension
            val originalName = getFileName(uri) ?: "audio_${System.currentTimeMillis()}"
            val extension = originalName.substringAfterLast('.', "mp3")

            val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
            val targetFile = File(mediaLibraryDir, fileName)

            // Raw copy
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
                output.flush()
            }

            val filePath = targetFile.absolutePath
            val fileSize = targetFile.length()

            Log.d(TAG, "Audio saved to media library: $filePath (size: $fileSize bytes)")
            return Pair(filePath, fileSize)
        }
    }

    /**
     * Save any file type with raw copy (fallback)
     */
    private fun saveRawFileToMediaLibrary(uri: Uri): Pair<String, Long>? {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.e(TAG, "Failed to open input stream for URI: $uri")
            return null
        }

        inputStream.use { input ->
            val originalName = getFileName(uri) ?: "file_${System.currentTimeMillis()}"
            val extension = originalName.substringAfterLast('.', "bin")

            val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
            val targetFile = File(mediaLibraryDir, fileName)

            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
                output.flush()
            }

            val filePath = targetFile.absolutePath
            val fileSize = targetFile.length()

            Log.d(TAG, "File saved to media library: $filePath (size: $fileSize bytes)")
            return Pair(filePath, fileSize)
        }
    }

    /**
     * Delete media library file by path
     */
    suspend fun deleteMediaLibraryFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.w(TAG, "Media file does not exist: $filePath")
                return@withContext false
            }

            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "Deleted media file: $filePath")
            } else {
                Log.w(TAG, "Failed to delete media file: $filePath")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting media file", e)
            false
        }
    }

    /**
     * Get file size for existing file
     */
    fun getFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size", e)
            0L
        }
    }

    /**
     * Check if media library file exists
     */
    fun mediaLibraryFileExists(filePath: String): Boolean {
        return try {
            File(filePath).exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get MIME type from URI
     */
    fun getMimeType(uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    /**
     * Get file name from URI
     */
    fun getFileName(uri: Uri): String? {
        var fileName: String? = null

        // Try to get from content resolver
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        // Fallback to last path segment
        return fileName ?: uri.lastPathSegment ?: "unknown_${System.currentTimeMillis()}"
    }

    /**
     * Add multiple images from URIs to media library (bulk operation)
     */
    suspend fun saveMultipleToMediaLibrary(uris: List<Uri>): List<Pair<String, Long>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<String, Long>>()

        uris.forEach { uri ->
            try {
                val result = saveToMediaLibrary(uri)
                if (result != null) {
                    results.add(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving file from URI: $uri", e)
            }
        }

        Log.d(TAG, "Bulk save complete: ${results.size}/${uris.size} files saved")
        results
    }

    /**
     * Extract images from ZIP and save to media library
     */
    suspend fun extractZipToMediaLibrary(zipUri: Uri): List<Pair<String, Long>> = withContext(Dispatchers.IO) {
        val savedFiles = mutableListOf<Pair<String, Long>>()

        try {
            Log.d(TAG, "Extracting ZIP from URI: $zipUri")

            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    var processedCount = 0

                    while (entry != null) {
                        val fileName = entry.name.lowercase()
                        val extension = fileName.substringAfterLast('.', "")

                        if (!entry.isDirectory && SUPPORTED_FORMATS.contains(extension)) {
                            Log.d(TAG, "Processing ZIP entry: ${entry.name}")

                            try {
                                val bytes = zipStream.readBytes()
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                                if (bitmap != null) {
                                    val resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE)
                                    val targetFileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                                    val targetFile = File(mediaLibraryDir, targetFileName)

                                    saveBitmapToFile(resizedBitmap, targetFile)

                                    if (!bitmap.isRecycled) bitmap.recycle()
                                    if (!resizedBitmap.isRecycled) resizedBitmap.recycle()

                                    savedFiles.add(Pair(targetFile.absolutePath, targetFile.length()))
                                    processedCount++
                                    Log.d(TAG, "Successfully saved image from ZIP: ${entry.name}")
                                } else {
                                    Log.w(TAG, "Failed to decode image from ZIP entry: ${entry.name}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing ZIP entry: ${entry.name}", e)
                            }
                        }

                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }

                    Log.d(TAG, "ZIP extraction complete. Processed $processedCount images")
                }
            } ?: Log.e(TAG, "Failed to open input stream for ZIP")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting ZIP file", e)
        }

        savedFiles
    }
}
