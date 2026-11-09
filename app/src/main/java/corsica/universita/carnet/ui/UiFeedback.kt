package corsica.universita.carnet.ui

import kotlinx.coroutines.flow.MutableSharedFlow

data class UiMessage(val text: String, val error: Boolean = false)

object UiFeedback {
    val messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    fun show(text: String, error: Boolean = false) { messages.tryEmit(UiMessage(text, error)) }
}
