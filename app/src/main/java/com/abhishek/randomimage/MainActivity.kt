package com.abhishek.randomimage

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var circularProgressDrawable: CircularProgressDrawable

    @Inject
    lateinit var prefs: SharedPreferences

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
        binding.randomButton.setOnClickListener {
            loadImage()
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