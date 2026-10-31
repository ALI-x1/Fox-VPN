package com.vauth.foxyvpn.data

/**
 * تقسیم پیام اول TLS (ClientHello) در محل نام سایت (SNI) به چند تکه؛ مسیرهای فیلترشده نام سایت را نمی‌بینند.
 * فقط برای ارتباط اپ با موزیلا (ControlPlaneHttp) استفاده می‌شود و بدون تنظیمات، همیشه روشن است.
 */
internal object TlsSplit {
    fun looksLikeClientHello(b: ByteArray, off: Int, len: Int): Boolean {
        if (len < 6 || off + len > b.size) return false
        return b[off] == 0x16.toByte() && b[off + 1] == 0x03.toByte() && b[off + 5] == 0x01.toByte()
    }

    /** [شروع، پایان) نام سایت داخل بسته؛ null اگر پیدا نشد. */
    private fun findSni(b: ByteArray): Pair<Int, Int>? {
        if (b.size < 44 || b[0] != 0x16.toByte()) return null
        var c = 5
        if (b[c] != 0x01.toByte()) return null
        c += 4
        c += 2 + 32
        if (c >= b.size) return null
        c += 1 + u8(b[c])
        if (c + 2 > b.size) return null
        c += 2 + u16(b[c], b[c + 1])
        if (c >= b.size) return null
        c += 1 + u8(b[c])
        if (c + 2 > b.size) return null
        var extEnd = c + 2 + u16(b[c], b[c + 1])
        c += 2
        if (extEnd > b.size) extEnd = b.size
        while (c + 4 <= extEnd) {
            val type = u16(b[c], b[c + 1])
            val len = u16(b[c + 2], b[c + 3])
            val body = c + 4
            if (body + len > extEnd) return null
            if (type == 0) {
                if (len < 2) return null
                val listLen = u16(b[body], b[body + 1])
                val list = body + 2
                if (list + listLen > body + len) return null
                if (listLen < 3 || b[list] != 0.toByte()) return null
                val nameLen = u16(b[list + 1], b[list + 2])
                val start = list + 3
                val end = start + nameLen
                if (end > list + listLen || end > b.size) return null
                return start to end
            }
            c = body + len
        }
        return null
    }

    /** سه تکه: تا ابتدای نام سایت، یک بایت، بقیه. null = قابل تقسیم نبود (همان‌طور که هست فرستاده شود). */
    fun split(b: ByteArray, off: Int, len: Int): List<ByteArray>? {
        if (len < 2 || off + len > b.size) return null
        val slice = if (off == 0 && len == b.size) b else b.copyOfRange(off, off + len)
        val start = findSni(slice)?.first ?: return null
        if (start <= 0 || start >= len) return null
        return listOf(
            b.copyOfRange(off, off + start),
            b.copyOfRange(off + start, off + start + 1),
            b.copyOfRange(off + start + 1, off + len),
        )
    }

    private fun u8(x: Byte): Int = x.toInt() and 0xFF
    private fun u16(a: Byte, b: Byte): Int = ((a.toInt() and 0xFF) shl 8) or (b.toInt() and 0xFF)
}
