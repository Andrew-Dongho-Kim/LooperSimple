package com.pnd.android.loop


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.ads.MobileAds
import com.pnd.android.loop.alarm.notification.isNotificationPermissionRequested
import com.pnd.android.loop.alarm.notification.markNotificationPermissionRequested
import com.pnd.android.loop.databinding.ContentMainBinding
import com.pnd.android.loop.ui.theme.AppTheme
import com.pnd.android.loop.util.LocalBackPressedDispatcher
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    // Android 13+에서는 POST_NOTIFICATIONS가 런타임 권한이라, 허용하지 않으면 알림이 아예
    // 표시되지 않는다. 거부 결과를 여기서 붙잡아 둘 필요는 없다 — 홈 목록의
    // NotificationStatusCard가 화면이 보일 때마다 상태를 다시 판정해 안내하고 되돌릴 길을 준다.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets -> insets }

        requestNotificationPermissionIfNeeded()

        savedInstanceState ?: MobileAds.initialize(this) { }

        setContent {
            CompositionLocalProvider(LocalBackPressedDispatcher provides onBackPressedDispatcher) {
                AppTheme {
                    AndroidViewBinding(factory = ContentMainBinding::inflate)
                }
            }
        }
    }

    /**
     * 첫 실행 때 한 번만 알림 권한을 물어본다.
     *
     * 예전에는 미허용이면 onCreate마다 launch했는데, Android 13+는 두 번 거부하면 시스템이
     * 다이얼로그를 더 띄우지 않는다. 그 뒤로는 launch가 즉시 거부 콜백만 돌리는 무의미한 호출이
     * 되고, 사용자는 알림을 못 받는 이유도 알 수 없다.
     *
     * 그래서 여기서는 첫 요청만 담당하고, 그 뒤의 안내와 복구는 홈의 NotificationStatusCard가
     * 맡는다(다시 물어볼 수 있으면 다이얼로그, 영구 거부면 시스템 설정으로 안내).
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (isNotificationPermissionRequested()) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            markNotificationPermissionRequested()
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController().navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * See https://issuetracker.google.com/142847973
     */
    private fun findNavController(): NavController {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController
    }
}

