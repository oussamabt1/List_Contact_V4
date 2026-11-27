package com.example.contacts.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Les photos sont copiées dans filesDir/photos : une URI de galerie peut devenir
 * illisible après redémarrage, un fichier interne reste toujours accessible.
 * La base ne stocke que le chemin absolu (colonne photo_uri).
 */
object PhotoStorage {

    private fun dir(context: Context): File =
        File(context.filesDir, "photos").apply { mkdirs() }

    fun newFile(context: Context): File =
        File(dir(context), "contact_${System.currentTimeMillis()}_${(0..9999).random()}.jpg")

    /** Copie l'image choisie dans la galerie ; renvoie le chemin interne, ou null en cas d'échec. */
    fun copyFromUri(context: Context, uri: Uri): String? = try {
        val target = newFile(context)
        val input = context.contentResolver.openInputStream(uri)
        if (input == null) {
            null
        } else {
            input.use { src -> target.outputStream().use { dst -> src.copyTo(dst) } }
            target.absolutePath
        }
    } catch (e: Exception) {
        null
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Supprime le fichier seulement s'il se trouve dans notre dossier photos. */
    fun deleteIfOwned(context: Context, path: String?) {
        if (path.isNullOrEmpty()) return
        try {
            val f = File(path)
            if (f.canonicalPath.startsWith(dir(context).canonicalPath + File.separator)) f.delete()
        } catch (e: Exception) {
            // un fichier orphelin n'est pas critique
        }
    }
}
