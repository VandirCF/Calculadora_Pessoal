package com.darkempire78.opencalculator.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.darkempire78.opencalculator.BuildConfig
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.Themes
import com.darkempire78.opencalculator.databinding.ActivityAboutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Version, credits to the base project (OpenCalc, GPL-3.0) and privacy. Everything is local text. */
class AboutActivity : BaseActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(Themes(this).getTheme())

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // keep the content below the status bar and above the navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        binding.aboutAppVersion.text =
            getString(R.string.about_other_version) + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"

        binding.aboutBackButton.setOnClickListener { finish() }
        binding.aboutBackButtonHitbox.setOnClickListener { finish() }

        binding.aboutLicense.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_license_title)
                .setMessage(R.string.about_license_text)
                .setPositiveButton(R.string.privacy_close, null)
                .show()
        }
        binding.aboutPrivacyPolicy.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.privacy_title)
                .setMessage(R.string.privacy_text)
                .setPositiveButton(R.string.privacy_close, null)
                .show()
        }
    }
}
