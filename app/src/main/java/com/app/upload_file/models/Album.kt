package com.app.upload_file.models

import com.google.gson.annotations.SerializedName

/**
 * Created by tt on 20/02/2021.
 */
data class Album(@SerializedName("id") val id: Int,
                 @SerializedName("userId") val userId: Int,
                 @SerializedName("title") val title: String)

/*
{
    "userId": 1,
    "id": 1,
    "title": "quidem molestiae enim"
}
 */