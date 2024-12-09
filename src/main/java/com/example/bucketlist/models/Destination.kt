package com.example.bucketlist.models

data class Destination(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val visited: Boolean = false
)
