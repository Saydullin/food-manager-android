package com.devsgroup.foodmanager.core.network.upload.model

import kotlinx.serialization.Serializable

/** Wire model for the Food Manager upload API (`/uploads/image`). */

// ---- Responses --------------------------------------------------------------

/** Response of `POST /uploads/image` — the public http(s) URL of the stored file. */
@Serializable
data class UploadImageResponse(val imageUrl: String)
