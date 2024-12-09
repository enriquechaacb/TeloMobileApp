package com.app.telomobileapp.ui.base

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.telomobileapp.R
import com.app.telomobileapp.data.session.SessionManager
import com.app.telomobileapp.ui.login.Login
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.widget.Toolbar
import com.app.telomobileapp.ui.main.MainActivity
import com.app.telomobileapp.ui.service.ServiceCurrent

// BaseActivity.kt
abstract class BaseActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        sessionManager = SessionManager(this)
        setupToolbar()
        setupBottomNavigation()

        // Infla el contenido específico de cada actividad
        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
        layoutInflater.inflate(getLayoutResourceId(), contentFrame)
    }

    // Método abstracto que cada actividad debe implementar para proporcionar su layout
    abstract fun getLayoutResourceId(): Int

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configura el título según la actividad
        supportActionBar?.title = getActivityTitle()
    }

    // Método que las actividades pueden sobrescribir para cambiar el título
    open fun getActivityTitle(): String = ""

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_current -> {
                    startActivity(Intent(this, ServiceCurrent::class.java))
                    true
                }
//                R.id.navigation_historic -> {
//                    startActivity(Intent(this, ServiceHistoric::class.java))
//                    true
//                }
//                R.id.navigation_account -> {
//                    startActivity(Intent(this, AccountState::class.java))
//                    true
//                }
//                R.id.navigation_profile -> {
//                    startActivity(Intent(this, UserProfile::class.java))
//                    true
//                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                performLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun performLogout() {
        // Limpia la sesión
        sessionManager.clearSession()

        // Regresa a Login
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}