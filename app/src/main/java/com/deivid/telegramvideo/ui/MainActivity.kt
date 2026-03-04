package com.deivid.telegramvideo.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.deivid.telegramvideo.R
import com.deivid.telegramvideo.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity principal que hospeda o NavHostFragment.
 * Gerencia a navegação entre as telas do app.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.loginFragment, R.id.chatsFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.bottomNavigation.setupWithNavController(navController)

        // Oculta a toolbar na tela de login e player
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val layoutParams = binding.navHostFragment.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            when (destination.id) {
                R.id.loginFragment, R.id.codeFragment, R.id.passwordFragment, R.id.playerFragment -> {
                    supportActionBar?.hide()
                    binding.appBarLayout.visibility = android.view.View.GONE
                    binding.bottomNavigation.visibility = android.view.View.GONE
                    layoutParams.behavior = null
                }
                R.id.videoModeFragment, R.id.chatsFragment -> {
                    supportActionBar?.show()
                    binding.appBarLayout.visibility = android.view.View.VISIBLE
                    binding.bottomNavigation.visibility = android.view.View.VISIBLE
                    layoutParams.behavior = com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior()
                }
                else -> {
                    supportActionBar?.show()
                    binding.appBarLayout.visibility = android.view.View.VISIBLE
                    binding.bottomNavigation.visibility = android.view.View.GONE
                    layoutParams.behavior = com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior()
                }
            }
            binding.navHostFragment.layoutParams = layoutParams
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
