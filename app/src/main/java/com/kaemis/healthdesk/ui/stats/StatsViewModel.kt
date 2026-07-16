package com.kaemis.healthdesk.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaemis.healthdesk.data.repository.StatsRepository
import com.kaemis.healthdesk.domain.stats.StatsSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(
    statsRepository: StatsRepository,
) : ViewModel() {
    val stats: StateFlow<StatsSnapshot> = statsRepository.observeStats().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = StatsSnapshot(),
    )

    companion object {
        fun factory(statsRepository: StatsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    StatsViewModel(statsRepository) as T
            }
    }
}
