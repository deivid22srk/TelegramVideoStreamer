package com.deivid.telegramvideo.ui.setup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.deivid.telegramvideo.R
import com.deivid.telegramvideo.databinding.FragmentSetupBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupFragment : Fragment() {

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val args: SetupFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("telegram_prefs", Context.MODE_PRIVATE)
        
        // Se já tiver configurado e não estiver editando, vai direto pro login
        if (!args.isEditing && prefs.contains("api_id") && prefs.contains("api_hash")) {
            findNavController().navigate(R.id.action_setupFragment_to_loginFragment)
            return
        }

        if (args.isEditing) {
            binding.etApiId.setText(prefs.getInt("api_id", 0).toString())
            binding.etApiHash.setText(prefs.getString("api_hash", ""))
            val currentPlayer = prefs.getString("player_type", "EXO")
            if (currentPlayer == "VLC") binding.rbVlcPlayer.isChecked = true else binding.rbExoPlayer.isChecked = true
            binding.btnSave.text = "Salvar Alterações"
        }

        binding.btnSave.setOnClickListener {
            val apiId = binding.etApiId.text.toString()
            val apiHash = binding.etApiHash.text.toString()

            if (apiId.isBlank() || apiHash.isBlank()) {
                Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val playerType = if (binding.rbVlcPlayer.isChecked) "VLC" else "EXO"

            prefs.edit()
                .putInt("api_id", apiId.toInt())
                .putString("api_hash", apiHash)
                .putString("player_type", playerType)
                .apply()

            Toast.makeText(requireContext(), "Configurações salvas!", Toast.LENGTH_SHORT).show()
            if (args.isEditing) {
                findNavController().popBackStack()
            } else {
                findNavController().navigate(R.id.action_setupFragment_to_loginFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
