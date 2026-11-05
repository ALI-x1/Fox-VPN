package com.vauth.foxyvpn.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/** مرحله‌ی فعلی ورود + لاگ کوتاه (برای نمایش روی صفحه و دکمه‌ی «کپی لاگ»). هیچ رمز/توکنی ثبت نمی‌شه. */
object LoginTrace {
    private val _stage = MutableStateFlow("")
    val stage: StateFlow<String> = _stage
    private val lines = CopyOnWriteArrayList<String>()
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun reset() { lines.clear(); _stage.value = "" }

    fun log(msg: String) {
        lines.add(fmt.format(Date()) + " " + msg.take(300))
        while (lines.size > 60) lines.removeAt(0)
        _stage.value = msg.take(120)
    }

    fun dump(): String = lines.joinToString("\n")
}
