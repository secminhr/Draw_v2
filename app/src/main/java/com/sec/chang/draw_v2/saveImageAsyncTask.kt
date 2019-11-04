package com.sec.chang.draw_v2

import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Environment

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class saveImageAsyncTask : AsyncTask<Bitmap, Void, Void>() {

    override fun doInBackground(vararg bitmaps: Bitmap): Void? {
        var out: FileOutputStream? = null
        val originBitmap = bitmaps[0]
        val filePath = Environment.getExternalStorageDirectory().absolutePath + "/Draw_v2/Draw_v2.png"
        val folderPath = Environment.getExternalStorageDirectory().absolutePath + "/Draw_v2"
        val dataFile = File(filePath)
        val path = File(folderPath)
        try {
            if (!dataFile.exists()) {
                path.mkdir()
                try {
                    dataFile.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
            out = FileOutputStream(dataFile)
            originBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            if (out != null) {
                try {
                    out.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        return null
    }
}
