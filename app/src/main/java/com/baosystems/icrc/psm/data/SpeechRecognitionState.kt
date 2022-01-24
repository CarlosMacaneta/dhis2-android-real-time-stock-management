package com.baosystems.icrc.psm.data

sealed class SpeechRecognitionState {
    object NotInitialized: SpeechRecognitionState()
    object Started: SpeechRecognitionState()
    object Stopped: SpeechRecognitionState()
    object NotAvailable: SpeechRecognitionState()
    data class Completed(val data: String?): SpeechRecognitionState()
    data class Errored(val code: Int): SpeechRecognitionState()
}