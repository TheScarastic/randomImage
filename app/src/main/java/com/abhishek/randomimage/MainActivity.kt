package com.abhishek.randomimage

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.abhishek.randomimage.databinding.ActivityMainBinding
import com.abhishek.randomimage.utils.Utils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.HttpException
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var circularProgressDrawable: CircularProgressDrawable

    @Inject
    lateinit var prefs: SharedPreferences

    private lateinit var cachedImage: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        circularProgressDrawable = CircularProgressDrawable(this)
        circularProgressDrawable.apply {
            strokeWidth = 10.0f
            centerRadius = 60.0f
            setColorSchemeColors(
                ContextCompat.getColor(applicationContext, android.R.color.holo_blue_bright),
                ContextCompat.getColor(applicationContext, android.R.color.holo_green_light),
                ContextCompat.getColor(applicationContext, android.R.color.holo_orange_light),
                ContextCompat.getColor(applicationContext, android.R.color.holo_red_light)
            )
        }.start()

        val loadSeed = prefs.getString("seed", "")
        if (!loadSeed.isNullOrEmpty()) {
            loadImage(loadSeed)
        }

        cachedImage = File(getExternalFilesDir(null), "cached.png")

        binding.randomButton.setOnClickListener {
            loadImage()
        }

        binding.share.setOnClickListener {
            // val builder = StrictMode.VmPolicy.Builder()
            // StrictMode.setVmPolicy(builder.build())
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND

            shareIntent.putExtra(
                Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                    applicationContext, applicationContext.packageName + ".provider", cachedImage
                )
            )
            shareIntent.type = "image/*"
            startActivity(Intent.createChooser(shareIntent, "Image"))
        }
    }


    private fun loadImage(seed: String = Utils.getRandomString()) {
        val lastGoodSeed = prefs.getString("seed", "")
        Glide.with(binding.root)
            .load("https://picsum.photos/seed/$seed/600/900")
            .placeholder(circularProgressDrawable)
            .error("https://picsum.photos/seed/$lastGoodSeed/600/900")
            .timeout(10000)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .listener(object : RequestListener<Drawable> {
                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    prefs.edit().putString("seed", seed).apply()
                    try {
                        val stream = FileOutputStream(cachedImage)
                        (resource as BitmapDrawable).bitmap.compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            stream
                        )
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    val error = e?.rootCauses?.firstOrNull()
                    if (error is HttpException) {
                        Snackbar.make(
                            binding.root,
                            applicationContext.getString(R.string.network_issue),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }

                    return false
                }
            })
            .into(binding.randomImage)
    }
}