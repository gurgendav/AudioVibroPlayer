package am.davtyan.audiovibroplayer.ui.main

import androidx.databinding.*
import androidx.lifecycle.ViewModel


class MainViewModel : ViewModel() {

    val isRecordingStarted = ObservableBoolean(false)

    val minimumRequiredSoundAverage = ObservableInt(45)

    val sensitivity = ObservableInt(25)
}

