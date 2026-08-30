package com.ybhgl.reminder.util.shizuku

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.IConnectivityManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * 小米超级岛白名单绕过（复刻 InstallerX-Revived 的 "Xiaomi Magic"）：
 *
 * 应用进程内通过 [ShizukuBinderWrapper] 包装 connectivity 服务的 binder，
 * 以 shell 身份直调隐藏接口 IConnectivityManager，把小米云服务
 * （com.xiaomi.xmsf）的 UID 加入 FIREWALL_CHAIN_OEM_DENY_3 黑名单断网，
 * 使焦点通知绕过白名单/签名校验直接上岛，通知发出后立即恢复。
 *
 * IConnectivityManager 来自 hiddenapi 模块（compileOnly）：
 * 编译期是手写桩，运行期解析到 framework 真实 Stub/Proxy，
 * 无需 Shizuku 用户服务，无绑定超时问题。
 */
object XiaomiBypassHelper {

    const val XMSF_PACKAGE = "com.xiaomi.xmsf"
    const val SHIZUKU_REQUEST_CODE = 10001

    /** 屏蔽时长：通知发出后保持屏蔽，随后恢复（InstallerX 默认 100ms，可调 50–350ms） */
    const val DEFAULT_BLOCK_INTERVAL_MS = 100L

    private const val TAG = "XiaomiBypassHelper"

    fun isShizukuAvailable(): Boolean =
        runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun isAuthorized(): Boolean = isShizukuAvailable() && runCatching {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    /**
     * 绕过是否可用：Shizuku 可用且已授权
     */
    fun isBypassAvailable(): Boolean = isAuthorized()

    fun getXmsfUid(context: Context): Int? = try {
        val pm = context.packageManager
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.getPackageUid(XMSF_PACKAGE, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageUid(XMSF_PACKAGE, 0)
        }
    } catch (_: Exception) {
        null
    }

    private val firewallManager: IConnectivityManager by lazy {
        val originalBinder = SystemServiceHelper.getSystemService("connectivity")
        val wrapper = ShizukuBinderWrapper(originalBinder)
        IConnectivityManager.Stub.asInterface(wrapper)
    }

    /**
     * 断网/恢复 xmsf 网络（两级后端，复刻 MAA-Meow XmsfFirewall 策略）：
     *
     * 1. cmd connectivity（主路径）：HyperOS 的 shell 命令接口，
     *    `set-package-networking-enabled false com.xiaomi.xmsf` + `set-chain3-enabled true`，
     *    通过 Shizuku 以 shell 身份执行，跨版本兼容性最好
     * 2. IConnectivityManager binder 直调（兜底）：旧系统接口仍在时可用；
     *    必须捕获 Throwable——新系统（Android 16 tethering APEX）已移除该方法，
     *   NoSuchMethodError 是 Error 不是 Exception
     *
     * 原理：HyperOS 焦点岛云端鉴权为 fail-open，断网让鉴权拿 No network 即视为通过；
     * 联网被查到无签名授权则会移除岛。
     *
     * 恢复：只把该 uid/包的规则重置为放行；绝不能禁用整条链，
     * 否则同链上其他被屏蔽的应用会意外恢复网络。
     */
    fun setPackageNetworkingEnabled(uid: Int, enabled: Boolean): Boolean {
        return if (enabled) {
            applyViaCmd(enabled) || applyViaBinder(uid, enabled)
        } else {
            applyViaBinder(uid, false) || applyViaCmd(false)
        }
    }

    private fun applyViaCmd(enabled: Boolean): Boolean {
        if (!isAuthorized()) return false
        val ok = if (enabled) {
            runShizukuCommand(
                "cmd", "connectivity", "set-package-networking-enabled", "true", XMSF_PACKAGE
            )
        } else {
            // 链 3 = FIREWALL_CHAIN_OEM_DENY_3：包级规则落在该链上，需确保链已启用
            runShizukuCommand("cmd", "connectivity", "set-chain3-enabled", "true") &&
                    runShizukuCommand(
                        "cmd", "connectivity", "set-package-networking-enabled", "false", XMSF_PACKAGE
                    )
        }
        return ok
    }

    private fun applyViaBinder(uid: Int, enabled: Boolean): Boolean {
        return try {
            val chain = IConnectivityManager.FIREWALL_CHAIN_OEM_DENY_3
            val rule = if (enabled) {
                IConnectivityManager.FIREWALL_RULE_DEFAULT
            } else {
                IConnectivityManager.FIREWALL_RULE_DENY
            }

            if (!enabled) {
                firewallManager.setFirewallChainEnabled(chain, true)
                firewallManager.setUidFirewallRule(chain, uid, rule)
            } else {
                firewallManager.setUidFirewallRule(chain, uid, rule)
            }
            true
        } catch (t: Throwable) {
            // NoSuchMethodError 等：新系统 tethering APEX 的 IConnectivityManager 已无此方法
            Log.w(TAG, "binder 防火墙调用不可用（系统接口已变更）：${t.javaClass.simpleName}: ${t.message}")
            false
        }
    }

    private val shizukuService: moe.shizuku.server.IShizukuService? by lazy {
        runCatching {
            val binder = Shizuku.getBinder() ?: return@runCatching null
            moe.shizuku.server.IShizukuService.Stub.asInterface(binder)
        }.getOrNull()
    }

    /** 通过 Shizuku 以 shell 身份执行命令，返回退出码是否为 0 */
    private fun runShizukuCommand(vararg command: String): Boolean {
        val service = shizukuService ?: return false
        return try {
            val process = service.newProcess(command, null, null)
            android.os.ParcelFileDescriptor.AutoCloseInputStream(process.inputStream).use { stream ->
                stream.readBytes().decodeToString()
            }
            process.waitFor() == 0
        } catch (t: Throwable) {
            Log.w(TAG, "cmd 执行失败：${t.message}")
            false
        }
    }

    /**
     * 发送通知的完整“魔法”流程：
     * 屏蔽 xmsf 网络 → 发送焦点通知 → 短暂等待 → 恢复网络。
     * 带焦点参数的通知在无权限且 xmsf 可联网时会被系统整体吞掉，
     * 因此任何环节失败都必须改发 fallbackNotification（无焦点参数的标准通知）。
     *
     * @return true 表示已发送焦点通知（可能上岛）；false 表示已降级发送标准通知
     */
    suspend fun notifyWithXiaomiMagic(
        context: Context,
        notificationManager: NotificationManager,
        notifyId: Int,
        islandNotification: Notification,
        fallbackNotification: Notification,
        blockIntervalMs: Long = DEFAULT_BLOCK_INTERVAL_MS
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isBypassAvailable()) {
            notificationManager.notify(notifyId, fallbackNotification)
            return@withContext false
        }
        val uid = getXmsfUid(context)
        if (uid == null) {
            notificationManager.notify(notifyId, fallbackNotification)
            return@withContext false
        }

        var blocked = false
        var usedIsland = false
        try {
            blocked = setPackageNetworkingEnabled(uid, false)
            if (blocked) {
                notificationManager.notify(notifyId, islandNotification)
                usedIsland = true
                delay(blockIntervalMs)
            } else {
                notificationManager.notify(notifyId, fallbackNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "绕过流程异常，降级为标准通知", e)
            runCatching { notificationManager.notify(notifyId, fallbackNotification) }
        } finally {
            if (blocked) {
                setPackageNetworkingEnabled(uid, true)
            }
        }
        usedIsland
    }
}
