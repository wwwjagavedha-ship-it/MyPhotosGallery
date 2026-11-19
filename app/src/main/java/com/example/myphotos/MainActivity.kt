package com.example.myphotos

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("profiles", MODE_PRIVATE) }
    private var activeProfile = 1
    private lateinit var root: LinearLayout
    private lateinit var gallery: GridView
    private lateinit var status: TextView
    private lateinit var profileButtons: LinearLayout
    private val imageUris = mutableListOf<Uri>()
    private val requestCode = 44

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        if (hasImagePermission()) loadPhotos() else requestImagePermission()
    }

    private fun buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(8))
            setBackgroundColor(Color.WHITE)
        }
        val title = TextView(this).apply {
            text = "My Photos"
            textSize = 27f
            setTextColor(Color.rgb(25, 28, 38))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(title)
        root.addView(TextView(this).apply {
            text = "Private gallery profiles · Photos stay on this phone"
            textSize = 13f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(4), 0, dp(14))
        })

        profileButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(profileButtons)
        renderProfiles()

        status = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(14), 0, dp(10))
        }
        root.addView(status)

        gallery = GridView(this).apply {
            numColumns = 3
            verticalSpacing = dp(3)
            horizontalSpacing = dp(3)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            adapter = PhotoAdapter()
        }
        root.addView(gallery, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val refresh = Button(this).apply {
            text = "Refresh photos"
            setOnClickListener { if (hasImagePermission()) loadPhotos() else requestImagePermission() }
        }
        root.addView(refresh)
        setContentView(root)
        updateVisibility()
    }

    private fun renderProfiles() {
        profileButtons.removeAllViews()
        for (i in 1..3) {
            val button = Button(this).apply {
                text = "Profile $i"
                isAllCaps = false
                textSize = 12f
                setOnClickListener {
                    activeProfile = i
                    renderProfiles()
                    updateVisibility()
                }
            }
            button.alpha = if (activeProfile == i) 1f else 0.55f
            profileButtons.addView(button, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
    }

    private fun key() = "show_gallery_$activeProfile"
    private fun isVisibleForProfile() = prefs.getBoolean(key(), true)

    private fun updateVisibility() {
        status.text = "Profile $activeProfile · Local gallery visibility"
        val toggle = root.findViewWithTag<Switch>("visibility_switch")
        if (toggle == null) {
            val sw = Switch(this).apply {
                tag = "visibility_switch"
                text = "Show this phone's photos in Profile $activeProfile"
                textSize = 14f
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit().putBoolean(key(), checked).apply()
                    updateVisibility()
                }
            }
            val insertAt = root.indexOfChild(status) + 1
            root.addView(sw, insertAt)
        } else {
            toggle.setOnCheckedChangeListener(null)
            toggle.text = "Show this phone's photos in Profile $activeProfile"
            toggle.isChecked = isVisibleForProfile()
            toggle.setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key(), checked).apply()
                updateVisibility()
            }
        }
        gallery.visibility = if (isVisibleForProfile()) View.VISIBLE else View.GONE
        if (isVisibleForProfile()) {
            status.text = "Profile $activeProfile · ${imageUris.size} photos found on this phone"
            (gallery.adapter as? BaseAdapter)?.notifyDataSetChanged()
        } else {
            status.text = "Profile $activeProfile · Local photos are hidden here"
        }
    }

    private fun hasImagePermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestImagePermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        requestPermissions(arrayOf(permission), requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) loadPhotos()
        else status.text = "Photo permission not granted. Allow access to display your gallery."
    }

    private fun loadPhotos() {
        imageUris.clear()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        try {
            contentResolver.query(collection, projection, null, null, sort)?.use { cursor: Cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    imageUris.add(Uri.withAppendedPath(collection, id.toString()))
                }
            }
        } catch (_: SecurityException) {
            status.text = "Some photos may be unavailable. Check app photo permissions."
        }
        (gallery.adapter as? BaseAdapter)?.notifyDataSetChanged()
        updateVisibility()
    }

    private inner class PhotoAdapter : BaseAdapter() {
        override fun getCount() = imageUris.size
        override fun getItem(position: Int) = imageUris[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val image = (convertView as? ImageView) ?: ImageView(this@MainActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(118)
                )
            }
            try {
                contentResolver.openInputStream(imageUris[position])?.use {
                    image.setImageBitmap(BitmapFactory.decodeStream(it))
                }
            } catch (_: Exception) {
                image.setImageDrawable(null)
            }
            return image
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
