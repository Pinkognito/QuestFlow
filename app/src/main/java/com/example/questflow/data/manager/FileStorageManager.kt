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
}
