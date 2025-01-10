package com.example.bucketlist.models

import java.util.Date

data class Destination(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val description: String = "",
    val dateCreated: Date = Date()
)

