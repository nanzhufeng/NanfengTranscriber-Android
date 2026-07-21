package com.nanzhufeng.transcriber.domain.model

enum class ModelInstallState {
    NOT_INSTALLED,
    DOWNLOADING,
    PAUSED,
    VERIFYING,
    READY,
    FAILED,
    CORRUPT,
}
