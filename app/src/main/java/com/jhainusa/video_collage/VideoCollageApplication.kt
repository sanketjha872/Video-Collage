package com.jhainusa.video_collage

import android.app.Application
import com.jhainusa.video_collage.di.AppContainer

class VideoCollageApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
