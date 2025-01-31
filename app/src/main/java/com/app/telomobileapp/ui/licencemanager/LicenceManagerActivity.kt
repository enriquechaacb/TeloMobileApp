package com.app.telomobileapp.ui.licencemanager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.app.telomobileapp.data.local.DeviceIdentifier
import com.app.telomobileapp.data.local.PreferenceManager
import com.app.telomobileapp.data.model.AppResponse
import com.app.telomobileapp.data.model.LicenceResponse
import com.app.telomobileapp.data.model.LicenciaRequest
import com.app.telomobileapp.data.network.ApiClient
import com.app.telomobileapp.databinding.ActivityLicenceManagerBinding
import com.app.telomobileapp.ui.login.Login
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LicenceManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLicenceManagerBinding
    var Licencia: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLicenceManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val deviceId = DeviceIdentifier(context = this).getDeviceId()

        Licencia = PreferenceManager(context = this@LicenceManagerActivity).getLicencia().toString()

        if(Licencia != "null" && Licencia != ""){
            Log.d("Licencia","Existe la licencia ${Licencia} del dispositivo ${deviceId}... pasa a dashboard")
            finish()
            startActivity(Intent(this, Login::class.java))
        }else{
            binding.btnRequestLicence.visibility = View.VISIBLE
        }

        binding.btnRequestLicence.setOnClickListener {
            requestLicence(deviceId)
        }

        binding.btnValidateLicence.setOnClickListener {
            val lic = binding.etLicencia.text
            validateLicence(deviceId, lic.toString())
        }
    }

    private fun requestLicence(deviceId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val solLic: List<AppResponse> = ApiClient.apiService.solicitarLicencia(LicenciaRequest(deviceId, "", generateRandomString(), 99))

                withContext(Dispatchers.Main) {
                    if (solLic[0].Indicador != 1) {
                        binding.btnRequestLicence.visibility = View.GONE
                    }
                    showError(solLic[0].Mensaje)
                    binding.cvValidateLicence.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Error de conexión: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
//                    binding.progressBar.visibility = View.GONE
//                    binding.loginButton.isEnabled = true
                }
            }
        }
    }

    private fun validateLicence(deviceId: String, licencia: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val solLic: List<LicenceResponse> = ApiClient.apiService.validarLicencia(LicenciaRequest(deviceId, "", licencia, 0))

                withContext(Dispatchers.Main) {
                    if (solLic.size == 1) {
                        if(solLic[0].EsValida){
                            PreferenceManager(context = this@LicenceManagerActivity).saveLicencia(solLic[0].NumeroLicencia)
                            Log.d("Licencia","Respuesta ${solLic[0]}")
                        }else{
                            showError("La licencia ha sido revocada")
                        }
                    }else{
                        showError("No existe una licencia coincidente")
                    }

                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Error: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
//                    binding.progressBar.visibility = View.GONE
//                    binding.loginButton.isEnabled = true
                }
            }
        }
    }

    private fun generateRandomString() = (('A'..'Z') + ('0'..'9'))
        .shuffled()
        .take(10)
        .joinToString("")

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}