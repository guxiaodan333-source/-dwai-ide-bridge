package com.dwai.idebridge.model

data class DiffApplyRequest(
    val id: String? = null,
    val files: List<FileDiff>? = null,
    val summary: String? = null
)

data class FileDiff(
    val path: String,
    val diff: String? = null,
    val modified: String? = null,
    val description: String? = null,
    val skip_review: Boolean = false
)

data class DiffResult(
    val path: String,
    val status: String,
    val reason: String? = null
)