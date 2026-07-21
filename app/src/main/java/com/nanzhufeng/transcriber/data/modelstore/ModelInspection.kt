package com.nanzhufeng.transcriber.data.modelstore

import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import java.nio.file.Path

data class ModelInspection(
    val state: ModelInstallState,
    val reason: String,
    val modelPath: Path? = null,
)
