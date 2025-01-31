package com.app.telomobileapp.ui.userprofile

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.telomobileapp.R
import com.app.telomobileapp.data.local.PreferenceManager
import com.app.telomobileapp.data.session.SessionManager
import com.app.telomobileapp.databinding.ActivityAccountstateBinding
import com.app.telomobileapp.databinding.ActivityUserprofileBinding
import com.app.telomobileapp.ui.base.BaseActivity

class Userprofile : BaseActivity() {
    private lateinit var binding: ActivityUserprofileBinding
    private lateinit var sessionManager: SessionManager
    private var licencia: String = ""

    override fun getLayoutResourceId(): Int = R.layout.activity_userprofile
    override fun getActivityTitle(): String = "Mi Usuario"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserprofileBinding.bind(findViewById(R.id.user_profile_container))
        sessionManager = SessionManager(this)
        licencia = PreferenceManager(context = this@Userprofile).getLicencia().toString()
        //loadAccountGrid()
//        val spinner = findViewById<Spinner>(R.id.spinnerFiltro)
//        ArrayAdapter.createFromResource(
//            this,
//            R.array.filtro_estadocuenta,
//            android.R.layout.simple_spinner_item
//        ).also { adapter ->
//            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//            spinner.adapter = adapter
//        }
//        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
//                when (pos) {
//                    0 -> loadAccountList(1)
//                    1 -> loadAccountList(2)
//                    2 -> loadAccountList(3)
//                }
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {
//                // Opcional: Manejar cuando no hay selección
//            }
//        }
    }
}