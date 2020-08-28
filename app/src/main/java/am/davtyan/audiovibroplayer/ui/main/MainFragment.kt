package am.davtyan.audiovibroplayer.ui.main

import am.davtyan.audiovibroplayer.databinding.MainFragmentBinding
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.main_fragment.view.*
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.view.WindowManager


class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private var recorder: MediaRecorder? = null
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: MainFragmentBinding
    private lateinit var vibrator: Vibrator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false).apply {
            viewModel = viewModel
        }

        return binding.main.also {
            it.toggleRecordingButton.setOnClickListener {
                if (!viewModel.isRecordingStarted.get()) {
                    startRecording()
                } else {
                    stopRecording()
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        binding.viewModel = viewModel

        vibrator = activity?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Throwable) {
            // ignore
        }

        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewModel.isRecordingStarted.set(false)
    }

    private fun startRecording() {

        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Thread {
            try {
                val currentRecorder = MediaRecorder()
                recorder = currentRecorder

                currentRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                currentRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                currentRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                currentRecorder.setOutputFile("/dev/null")

                try {
                    currentRecorder.prepare()
                } catch (e: Throwable) {
                    return@Thread
                }

                try {
                    currentRecorder.start()
                } catch (e: Throwable) {
                    currentRecorder.release()
                    return@Thread
                }

                viewModel.isRecordingStarted.set(true)

                while (true)
                {
                    // Compute a simple average of the amplitude over one
                    // second
                    val nMeasures = 5
                    var sumAmplitude = 0
                    currentRecorder.maxAmplitude // First call returns 0
                    var n = 0
                    for (i in 0 until nMeasures) {
                        val maxAmplitude = currentRecorder.maxAmplitude
                        if (maxAmplitude > 0) {
                            sumAmplitude += maxAmplitude
                            n++
                        }

                        try {
                            Thread.sleep((50 / nMeasures).toLong())
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                            break
                        }

                    }

                    var avgAmplitude = sumAmplitude.toDouble() / n
                    Log.d("RecordingThread", "avgAmplitude: ${avgAmplitude}")

                    val sensitivity = (if (viewModel.sensitivity.get() > 0) viewModel.sensitivity.get() else 1) * 1000.0

                    if (avgAmplitude > sensitivity)
                    {
                        avgAmplitude = sensitivity
                    }

                    val vibrationAmplitude = avgAmplitude * 255.0 / sensitivity

                    if (vibrationAmplitude > viewModel.minimumRequiredSoundAverage.get()) {
                        val effect = VibrationEffect.createOneShot(50, vibrationAmplitude.toInt())
                        vibrator.vibrate(effect)
                    }
                }
            } catch (e: Throwable) {
                Log.e("RecordingThread", "Error while recording.\n${e}")
                // ignore
            } finally {
                Handler(Looper.getMainLooper()).post {
                    stopRecording()
                }
            }
        }.start()
    }
}
