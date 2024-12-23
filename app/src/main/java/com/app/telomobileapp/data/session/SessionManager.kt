package com.app.telomobileapp.data.session

import android.content.Context
import com.app.telomobileapp.data.model.LoginResponse

class SessionManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("TeloMobilePrefs", Context.MODE_PRIVATE)

    fun saveSession(loginResponse: LoginResponse) {
        with(sharedPreferences.edit()) {
            putString("token", loginResponse.Token)
            putString("expira", loginResponse.Expira)
            putInt("idUsuario", loginResponse.Usuario.IdUsuario)
            putInt("idOperador", loginResponse.Usuario.IdEmpleado) // Guardamos el IdEmpleado como idOperador
            putString("userName", loginResponse.Usuario.UserName)
            putString("licencia","FW2583L")
            apply()
        }
    }

    fun getToken(): String? = sharedPreferences.getString("token", null)
    fun isLoggedIn(): Boolean = getToken() != null
    fun getIdOperador(): Int = sharedPreferences.getInt("idOperador", 0)
    fun getIdUsuario(): Int = sharedPreferences.getInt("idUsuario", 0)
    fun getLicencia(): String? = sharedPreferences.getString("licencia", "FW2583L")

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
}