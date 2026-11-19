package com.payas.quelle.admin

import android.content.Context
import com.payas.quelle.data.hashPin
import com.payas.quelle.data.isValidPin
import java.util.UUID

/**
 * Yönetim panelinin PIN'i. Yalnızca bu tablette durur (senkronlanmaz) ve
 * düz metin değil, tuzlu özet olarak saklanır.
 */
class AdminLock(context: Context) {
    private val prefs = context.getSharedPreferences("quelle_admin", Context.MODE_PRIVATE)

    val hasPin: Boolean get() = prefs.getString(KEY_HASH, null) != null

    fun check(pin: String): Boolean {
        val hash = prefs.getString(KEY_HASH, null) ?: return true
        val salt = prefs.getString(KEY_SALT, "") ?: ""
        return hashPin(pin, salt) == hash
    }

    fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val salt = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_SALT, salt).putString(KEY_HASH, hashPin(pin, salt)).apply()
        return true
    }

    fun clearPin() {
        prefs.edit().remove(KEY_SALT).remove(KEY_HASH).apply()
    }

    private companion object {
        const val KEY_HASH = "pin_hash"
        const val KEY_SALT = "pin_salt"
    }
}
