package com.kaemis.healthdesk.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaemis.healthdesk.data.defaults.DefaultData
import com.kaemis.healthdesk.data.entity.UserProfileEntity
import com.kaemis.healthdesk.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val fallbackProfile = DefaultData.profile(System.currentTimeMillis())

    val profile: StateFlow<UserProfileEntity> = profileRepository.observeProfile()
        .map { it ?: fallbackProfile }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = fallbackProfile,
        )

    fun updateDisplayName(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || trimmedName == profile.value.displayName) return

        viewModelScope.launch {
            profileRepository.saveProfile(
                profile.value.copy(
                    displayName = trimmedName,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun updateAvatarPath(path: String) {
        viewModelScope.launch {
            profileRepository.saveProfile(
                profile.value.copy(
                    avatarMode = "localImage",
                    avatarLocalPath = path,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun useInitialsAvatar() {
        viewModelScope.launch {
            profileRepository.saveProfile(
                profile.value.copy(
                    avatarMode = "initials",
                    avatarLocalPath = null,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    companion object {
        fun factory(profileRepository: ProfileRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProfileViewModel(profileRepository) as T
            }
    }
}
