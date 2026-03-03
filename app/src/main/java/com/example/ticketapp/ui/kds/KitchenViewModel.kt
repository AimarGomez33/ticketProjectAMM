package com.example.ticketapp.ui.kds

import android.app.Application
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketapp.data.kds.KdsOrder
import com.example.ticketapp.repository.KdsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class KitchenUiState(
        val pendingOrders: List<KdsOrder> = emptyList(),
        val completedOrders: List<KdsOrder> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null
)

class KitchenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KdsRepository()

    private val _uiState = MutableStateFlow(KitchenUiState())
    val uiState: StateFlow<KitchenUiState> = _uiState.asStateFlow()

    // ─── Sound alert ────────────────────────────────────────────────────────
    private val soundPool: SoundPool =
            SoundPool.Builder()
                    .setMaxStreams(2)
                    .setAudioAttributes(
                            AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                    )
                    .build()

    // Use a built-in Android notification sound via its raw ID
    // We play system notification sound instead of a custom file
    private var bellSoundId: Int = 0

    init {
        // Load the system notification sound
        loadSystemSound()
        observeOrders()
        // Purge Firebase orders completed more than 3 days ago
        viewModelScope.launch {
            try {
                repository.cleanupOldOrders(maxAgeDays = 3)
            } catch (e: Exception) {
                android.util.Log.e("KitchenViewModel", "cleanup error: ${e.message}")
            }
        }
    }

    private fun loadSystemSound() {
        val context = getApplication<Application>()
        val notificationUri =
                android.media.RingtoneManager.getDefaultUri(
                        android.media.RingtoneManager.TYPE_NOTIFICATION
                )
        // We'll use Ringtone for one-shot playback instead of SoundPool for simplicity
    }

    /** Plays the alert sound once using the system notification ringtone */
    private fun playNewOrderSound() {
        val context = getApplication<Application>()
        try {
            val notificationUri =
                    android.media.RingtoneManager.getDefaultUri(
                            android.media.RingtoneManager.TYPE_NOTIFICATION
                    )
            val ringtone = android.media.RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            android.util.Log.e("KitchenViewModel", "Error playing sound: ${e.message}")
        }
    }

    // ─── Observe orders ──────────────────────────────────────────────────────
    private fun observeOrders() {
        viewModelScope.launch {
            try {
                repository.getPendingOrders().collect { pending ->
                    val previous = _uiState.value.pendingOrders
                    // Only alert for brand-new orders, not for items added to existing ones
                    val hasNewOrder =
                            pending.any { newOrder ->
                                previous.none { it.id == newOrder.id } && !newOrder.isReOrder
                            }
                    if (hasNewOrder && previous.isNotEmpty()) {
                        playNewOrderSound()
                    }
                    _uiState.value =
                            _uiState.value.copy(
                                    pendingOrders = pending,
                                    isLoading = false,
                                    error = null
                            )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }

        viewModelScope.launch {
            try {
                repository.getCompletedOrders().collect { completed ->
                    _uiState.value = _uiState.value.copy(completedOrders = completed)
                }
            } catch (e: Exception) {
                // Silent fail for completed orders
            }
        }
    }

    fun markOrderAsCompleted(orderId: String) {
        viewModelScope.launch {
            try {
                repository.updateOrderStatus(orderId, "COMPLETED")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to complete order")
            }
        }
    }

    fun markItemAsReady(orderId: String, itemIndex: Int) {
        viewModelScope.launch {
            try {
                repository.updateOrderItemStatus(orderId, itemIndex, "READY")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to update item")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundPool.release()
    }
}
