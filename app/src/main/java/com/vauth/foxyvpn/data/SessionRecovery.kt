package com.vauth.foxyvpn.data

import android.content.Context

/**
 * وقتی نشست موزیلا خراب می‌شه (کلید رد شد / 401 از Guardian)، کاربرِ «کد» به‌جای بیرون‌انداخته‌شدن از اکانت،
 * با همون کدش از سرور کلید تازه می‌گیره. ادمین (ورود مستقیم) و کدِ باطل/منقضی شامل این نمی‌شن.
 */
object SessionRecovery {
    /** [rotate] = true: سرور دستگاه رو به حساب دیگه‌ای هم منتقل کنه (برای 401 موقع اتصال). */
    suspend fun recover(context: Context, repo: FxaAuthRepository, rotate: Boolean): Boolean {
        val rc = RedeemClient(context)
        val code = rc.activeCode
        if (rc.adminMode || code == null) return false
        val grant = (if (rotate) rc.rotate() else rc.redeem(code)).getOrNull() ?: return false
        return repo.loginWithRefreshToken(grant.refreshToken).isSuccess
    }
}
