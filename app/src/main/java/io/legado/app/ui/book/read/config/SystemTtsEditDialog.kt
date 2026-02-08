package io.legado.app.ui.book.read.config

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.databinding.DialogRecyclerViewBinding
import io.legado.app.databinding.ItemRadioButtonBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding

class SystemTtsEditDialog(val engine: String) : BaseDialogFragment(R.layout.dialog_recycler_view),
    TextToSpeech.OnInitListener {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val adapter by lazy { Adapter() }
    private var textToSpeech: TextToSpeech? = null
    private var actualEngine: String = engine
    private var currentVoice: String? = null

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.speak_engine)
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        initTts()
    }

    private fun initTts() {
        try {
            if (engine.isNullOrBlank()) {
                textToSpeech = TextToSpeech(requireContext(), this)
            } else {
                textToSpeech = TextToSpeech(requireContext(), this, engine)
            }
        } catch (e: Exception) {
            toastOnUi(e.localizedMessage)
            dismissAllowingStateLoss()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                actualEngine = tts.defaultEngine ?: engine
                // Load saved voice for this actual engine
                val savedVoice = AppConfig.getTtsVoice(actualEngine)
                if (!savedVoice.isNullOrEmpty()) {
                    currentVoice = savedVoice
                }

                val voices = try {
                    tts.voices?.toList()?.sortedBy { it.locale.toLanguageTag() }
                } catch (e: Exception) {
                    null
                }
                if (voices.isNullOrEmpty()) {
                    toastOnUi("No voices found for this engine.")
                    return
                }
                adapter.setItems(voices)
            }
        } else {
            toastOnUi("TTS Init Failed")
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.shutdown()
    }

    inner class Adapter : RecyclerAdapter<Voice, ItemRadioButtonBinding>(requireContext()) {

        override fun getViewBinding(parent: ViewGroup): ItemRadioButtonBinding {
            return ItemRadioButtonBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemRadioButtonBinding,
            item: Voice,
            payloads: MutableList<Any>
        ) {
            binding.radioButton.text = "${item.locale.displayName} - ${item.name}"
            binding.radioButton.isChecked = item.name == currentVoice
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemRadioButtonBinding) {
            binding.radioButton.setOnClickListener {
                getItemByLayoutPosition(holder.layoutPosition)?.let { voice ->
                    currentVoice = voice.name
                    AppConfig.setTtsVoice(actualEngine, currentVoice)
                    notifyDataSetChanged()
                }
            }
        }
    }
}
