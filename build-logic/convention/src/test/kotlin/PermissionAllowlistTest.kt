import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionAllowlistTest {
    private val manifest =
        """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <uses-permission android:name="android.permission.INTERNET" />
            <uses-permission android:name="android.permission.VIBRATE" />
            <uses-permission-sdk-23 android:name="android.permission.WAKE_LOCK" />
        </manifest>
        """.trimIndent()

    @Test
    fun parsesOnePermissionPerLineAndIgnoresReasonsAndBlankLines() {
        val text =
            """
            # Ads (spec 002)
            android.permission.INTERNET   # ad requests

            android.permission.VIBRATE
            """.trimIndent()
        assertEquals(
            setOf("android.permission.INTERNET", "android.permission.VIBRATE"),
            PermissionAllowlist.parse(text),
        )
    }

    @Test
    fun listedPermissionsPass() {
        val text = "android.permission.INTERNET\nandroid.permission.VIBRATE\nandroid.permission.WAKE_LOCK"
        assertEquals(emptyList(), PermissionAllowlist.violations(manifest, text))
    }

    @Test
    fun unlistedPermissionsAreReportedSorted() {
        assertEquals(
            listOf("android.permission.VIBRATE", "android.permission.WAKE_LOCK"),
            PermissionAllowlist.violations(manifest, "android.permission.INTERNET # ads"),
        )
    }
}
