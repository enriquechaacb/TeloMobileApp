package com.app.telomobileapp.data.local

import android.content.Context
import com.app.telomobileapp.data.model.Usuario

class PreferenceManager(context: Context) {
    private val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    fun saveLicencia(licencia: String) {
        prefs.edit().putString("licencia", licencia).apply()
    }

    fun getLicencia(): String? {
        return prefs.getString("licencia", null)
    }

    fun deleteLicencia() {
        prefs.edit().remove("licencia").apply()
        // prefs.edit().putString("licencia", null).apply()
    }

    fun saveUsuario(usuario: Array<Usuario>) {
        prefs.edit().putString("NombreUsuario", usuario[0].Nombre).apply()
        prefs.edit().putInt("IdUsuario", usuario[0].IdUsuario).apply()
    }

    fun getIdUsuario(): Int {
        return prefs.getInt("IdUsuario",0)
    }

    fun getNombreUsuario(): String? {
        return prefs.getString("NombreUsuario", null)
    }

    fun deleteUsuario() {
        prefs.edit().remove("NombreUsuario").apply()
        prefs.edit().remove("IdUsuario").apply()
        // prefs.edit().putString("licencia", null).apply()
    }
}