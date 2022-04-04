package com.app.upload_file.models

import com.google.gson.Gson

/**
 * Created by tt on 20/02/2021.
 */
data class DetailsViewModel(val posts: List<Post>, val users: List<User>, val albums: List<Album>) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
}