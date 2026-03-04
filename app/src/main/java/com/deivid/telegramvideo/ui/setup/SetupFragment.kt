package com.deivid.telegramvideo.ui.setup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState: Bundle?)

        val prefs = requireContext().getSharedPreferences("telegram_prefs", Context.MODE_PRIVATE)
        
        // Se já tiver configurado, vai direto pro login
        if (prefs.contains("api_id") && prefs.contains("api_hash")) {
            findNavController().navigate(R.id.action_setupFragment_to_loginFragment)
            return
        }

        binding.btnSave.setOnClickListener {
            val apiId = binding.etApiId.text.toString()
            val apiHash = binding.etApiHash.text.toString()

            if (apiId.isBlank() || apiHash.isBlank()) {
                Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putInt("api_id", apiId.toInt())
                .putString("api_hash", apiHash)
                .apply()

            Toast.makeText(requireContext(), "Configurações salvas!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_setupFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
