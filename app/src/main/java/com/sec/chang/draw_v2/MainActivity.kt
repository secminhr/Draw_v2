package com.sec.chang.draw_v2

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private val requestPermissionCode = 1

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == requestPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.permission_write_storage, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fun writeExternalStoragePermissionExist(): Boolean {
            val permissionState = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return permissionState == PackageManager.PERMISSION_GRANTED
        }

        if (!writeExternalStoragePermissionExist()) {
            val permissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, 1)
        }

        val preferences = getPreferences(Context.MODE_PRIVATE)
        canvasView.paintLineWidth = preferences.getInt("paintWidth", 10)
        canvasView.eraserLineWidth = preferences.getInt("eraserWidth", 10)
        canvasView.colorR = preferences.getInt("R", 0)
        canvasView.colorG = preferences.getInt("G", 0)
        canvasView.colorB = preferences.getInt("B", 0)

        mainFloatingButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                //eraserFloatingButton and paintFloatingButton should show and hide at the same time
                if (eraserFloatingButton.isShown) {
                    hideFloatingButtons(eraserFloatingButton, paintFloatingButton)
                    mainFloatingButton.setImageResource(
                            if(canvasView.isUsingEraser) R.drawable.ic_erase_white_24dp
                            else R.drawable.ic_create_white_24dp )
                } else {
                    showFloatingButtons(eraserFloatingButton, paintFloatingButton)
                    mainFloatingButton.setImageResource(R.drawable.ic_close_white_24dp)
                }
            }

            private fun hideFloatingButtons(vararg buttons: FloatingActionButton) {
                for (button in buttons) {
                    button.hide()
                }
            }

            private fun showFloatingButtons(vararg buttons: FloatingActionButton) {
                for (button in buttons) {
                    button.show()
                }
            }
        })

        eraserFloatingButton.setOnClickListener {
            canvasView.isUsingEraser = true
            mainFloatingButton.setImageResource(R.drawable.ic_erase_white_24dp)
        }

        paintFloatingButton.setOnClickListener {
            canvasView.isUsingEraser = false
            mainFloatingButton.setImageResource(R.drawable.ic_create_white_24dp)
        }

        val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName

        val databaseReference = FirebaseDatabase.getInstance().getReference("info")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val newestVersion = dataSnapshot.child("version").value.toString()
                val link = dataSnapshot.child("link").value.toString()

                if (newestVersion != currentVersion) {
                    buildUpdateAppAlert(link).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("MainActivity", "read canceled")
            }

            private fun buildUpdateAppAlert(link: String): AlertDialog {
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle(R.string.update_available)
                builder.setMessage(R.string.update_alert_message)
                builder.setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    startActivity(intent)
                }
                builder.setNegativeButton(android.R.string.no) { dialogInterface, _ -> dialogInterface.dismiss() }
                builder.setCancelable(false)
                return builder.create()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = MenuInflater(this)
        inflater.inflate(R.menu.main_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val subMenu = menu.getItem(0).subMenu
        val setPaintSizeItem = subMenu.findItem(R.id.paintSizeItem)
        setPaintSizeItem.setTitle(
                if(canvasView.isUsingEraser) R.string.eraserSize
                else R.string.paint_size
        )
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.paletteItem -> {
                canvasView.isUsingEraser = false
                setPaintColor()
                mainFloatingButton.setImageResource(R.drawable.ic_create_white_24dp)
            }
            R.id.cleanCanvasItem -> canvasView.cleanCanvas()
            R.id.deleteItem -> {
                val deleteAlertBuilder = AlertDialog.Builder(this)
                deleteAlertBuilder.setTitle(R.string.delete_image).setMessage(R.string.sure_to_delete)
                deleteAlertBuilder.setNegativeButton(android.R.string.cancel) { dialogInterface, _ -> dialogInterface.dismiss() }
                deleteAlertBuilder.setPositiveButton(android.R.string.yes) {_, _ ->
                    val picFile = File(Environment.getExternalStorageDirectory().absolutePath + "/Draw_v2/Draw_v2.png")
                    if (picFile.exists()) {
                        val success = deleteImage(picFile)
                        val toast = Toast.makeText(applicationContext,
                                                   if(success) R.string.the_image_deleted
                                                   else R.string.fail_to_delete,
                                                   Toast.LENGTH_SHORT)
                        toast.show()
                    }
                    canvasView.cleanCanvas()
                }
                deleteAlertBuilder.show()
            }
            R.id.shareItem -> shareImage()
            R.id.paintSizeItem -> setSize()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        val filePath = arrayOf(Environment.getExternalStorageDirectory().absolutePath + "/Draw_v2/Draw_v2.png")
        MediaScannerConnection.scanFile(applicationContext, filePath, null, null)
        val preferencesEditor = getPreferences(Context.MODE_PRIVATE).edit()
        preferencesEditor.putInt("paintWidth", canvasView.paintLineWidth)
        preferencesEditor.putInt("eraserWidth", canvasView.eraserLineWidth)
        preferencesEditor.putInt("R", canvasView.colorR)
        preferencesEditor.putInt("G", canvasView.colorG)
        preferencesEditor.putInt("B", canvasView.colorB)
        preferencesEditor.apply()
        super.onStop()
    }

    private fun deleteImage(file: File): Boolean {
        return file.delete()
    }

    private fun shareImage() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/png"
        val uriN = FileProvider.getUriForFile(this@MainActivity,
                this@MainActivity.applicationContext.packageName + ".provider",
                File(Environment.getExternalStorageDirectory().toString() + "/Draw_v2/Draw_v2.png"))
        shareIntent.putExtra(Intent.EXTRA_STREAM, uriN)
        startActivity(shareIntent)
    }

    private fun setPaintColor() {
        val colorChooserAlertBuilder = AlertDialog.Builder(this)
        val colorChooserInflater = this.layoutInflater
        val layout = colorChooserInflater.inflate(R.layout.color_chooser, null)
        val seekBarR: SeekBar = layout.findViewById(R.id.seekBar)
        val seekBarG: SeekBar = layout.findViewById(R.id.seekBar2)
        val seekBarB: SeekBar = layout.findViewById(R.id.seekBar3)
        val editTextR: EditText = layout.findViewById(R.id.editText)
        val editTextG: EditText = layout.findViewById(R.id.editText2)
        val editTextB: EditText = layout.findViewById(R.id.editText3)
        val colorPreview: PreviewColorCanvas = layout.findViewById(R.id.view2)
        seekBarR.progress = canvasView.colorR
        seekBarG.progress = canvasView.colorG
        seekBarB.progress = canvasView.colorB
        editTextR.setText(canvasView.colorR.toString())
        editTextG.setText(canvasView.colorG.toString())
        editTextB.setText(canvasView.colorB.toString())
        colorChooserAlertBuilder.setPositiveButton(R.string.done) { _, _ ->
            canvasView.colorR = seekBarR.progress
            canvasView.colorG = seekBarG.progress
            canvasView.colorB = seekBarB.progress
        }

        colorChooserAlertBuilder.setNegativeButton(android.R.string.cancel) { dialogInterface, _ -> dialogInterface.dismiss() }

        seekBarR.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    editTextR.setText(progress.toString())
                }
                colorPreview.previewColor(seekBarR.progress, seekBarG.progress, seekBarB.progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                //Do nothing
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                //Do nothing
            }
        })
        seekBarG.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    editTextG.setText(progress.toString())
                }
                colorPreview.previewColor(seekBarR.progress, seekBarG.progress, seekBarB.progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                //Do nothing
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                //Do nothing
            }
        })
        seekBarB.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    editTextB.setText(progress.toString())
                }
                colorPreview.previewColor(seekBarR.progress, seekBarG.progress, seekBarB.progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                var aim = ValueAnimator.ofInt(seekBar.thumb.bounds.centerY(), seekBar.thumb.bounds.centerY() + seekBar.thumb.bounds.height() + 1)
                aim.duration = 3000
                aim.addUpdateListener { animator ->

                }
                aim.start()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                //Do nothing
            }
        })
        editTextR.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                //Do nothing
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (charSequence.isEmpty()) {
                    editTextR.setText("0")
                    editTextR.setSelection(1)
                }
                if (charSequence.length > 1 && charSequence[0] == '0') {
                    editTextR.setText(charSequence.subSequence(1, 2))
                    editTextR.setSelection(1)
                }
                if (charSequence.isNotEmpty() && Integer.parseInt(charSequence.toString()) > 255) {
                    editTextR.setText("255")
                    editTextR.setSelection(3)
                }
                seekBarR.progress = Integer.parseInt(editTextR.text.toString())
            }

            override fun afterTextChanged(editable: Editable) {
                //Do nothing
            }
        })
        editTextG.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                //Do nothing
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (charSequence.isEmpty()) {
                    editTextG.setText("0")
                    editTextG.setSelection(1)
                }
                if (charSequence.length > 1 && charSequence[0] == '0') {
                    editTextG.setText(charSequence.subSequence(1, 2))
                    editTextG.setSelection(1)
                }
                if (charSequence.isNotEmpty() && Integer.parseInt(charSequence.toString()) > 255) {
                    editTextG.setText("255")
                    editTextG.setSelection(3)
                }
                seekBarG.progress = Integer.parseInt(editTextG.text.toString())
            }

            override fun afterTextChanged(editable: Editable) {
                //Do nothing
            }
        })
        editTextB.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                //Do nothing
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (charSequence.isEmpty()) {
                    editTextB.setText("0")
                    editTextB.setSelection(1)
                }
                if (charSequence.length > 1 && charSequence[0] == '0') {
                    editTextB.setText(charSequence.subSequence(1, 2))
                    editTextB.setSelection(1)
                }
                if (charSequence.isNotEmpty() && Integer.parseInt(charSequence.toString()) > 255) {
                    editTextB.setText("255")
                    editTextB.setSelection(3)
                }
                seekBarB.progress = Integer.parseInt(editTextB.text.toString())
            }

            override fun afterTextChanged(editable: Editable) {
                //Do nothing
            }
        })
        colorChooserAlertBuilder.setView(layout)
        colorChooserAlertBuilder.show()
        colorPreview.previewColor(seekBarR.progress, seekBarG.progress, seekBarB.progress)
    }

    private fun setSize() {
        //----setting alert----
        val paintSizeAlertBuilder = AlertDialog.Builder(this)
        val paintSizeInflater = this.layoutInflater
        val layout = paintSizeInflater.inflate(R.layout.size_settter, null)
        if (canvasView.isUsingEraser) {
            paintSizeAlertBuilder.setTitle(R.string.set_eraser_size)
        } else {
            paintSizeAlertBuilder.setTitle(R.string.set_paint_size)
        }
        paintSizeAlertBuilder.setView(layout)
        val previewPaintSizeCanvas = layout.findViewById<PreviewPaintSizeCanvas>(R.id.view)
        val sizeSeekBar = layout.findViewById<SeekBar>(R.id.sizeSetterSeekBar)
        if (canvasView.isUsingEraser) {
            sizeSeekBar.progress = canvasView.eraserLineWidth
        } else {
            sizeSeekBar.progress = canvasView.paintLineWidth
        }
        sizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                previewPaintSizeCanvas.previewSize(progress, canvasView.isUsingEraser, canvasView.colorR,
                        canvasView.colorG, canvasView.colorB)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                //Do nothing
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                //Do nothing
            }
        })
        paintSizeAlertBuilder.setPositiveButton(R.string.done) { _, _ ->
            val position = sizeSeekBar.progress
            if (!canvasView.isUsingEraser) {
                canvasView.paintLineWidth = position
            } else {
                canvasView.eraserLineWidth = position
            }
        }
        paintSizeAlertBuilder.setNegativeButton(android.R.string.cancel) { dialogInterface, _ -> dialogInterface.cancel() }
        //---end set---
        paintSizeAlertBuilder.show()
        previewPaintSizeCanvas.previewSize(sizeSeekBar.progress, canvasView.isUsingEraser, canvasView.colorR,
                canvasView.colorG, canvasView.colorB)
    }

}