package com.darkempire78.opencalculator.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.darkempire78.opencalculator.BuildConfig
import com.darkempire78.opencalculator.MyPreferences
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.Themes
import com.darkempire78.opencalculator.databinding.ActivityAboutBinding
import com.darkempire78.opencalculator.dialogs.DonationDialog

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Themes
        val themes = Themes(this)
        themes.applyDayNightOverride()
        setTheme(themes.getTheme())

        // Change the status bar color
        if (MyPreferences(this).theme == 1) { // Amoled theme
            window.statusBarColor = ContextCompat.getColor(this, R.color.amoled_background_color)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.background_color)
        }

        binding = ActivityAboutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Set app version
        val versionName =  this.getString(R.string.about_other_version) + " "+ BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"
        binding.aboutAppVersion.text = versionName

        // back button
        binding.aboutBackButton.setOnClickListener {
            finish()
        }
        binding.aboutBackButtonHitbox.setOnClickListener {
            finish()
        }

        // This derivative app has no store page, translation project, donation page or community of its own yet:
        // the upstream shortcuts (rate, translate, donate, Discord) are hidden so they do not point to another app.
        listOf(
            binding.aboutHelp, binding.aboutTranslate, binding.aboutRate,
            binding.categoryHelpUsSeparator, binding.aboutDiscord, binding.categorySocialSeparator
        ).forEach { it.visibility = android.view.View.GONE }
        binding.aboutAboutMadeInFrance.visibility = android.view.View.GONE

        // Credit to the original project (GPL-3.0 attribution)
        binding.aboutGithub.text = getString(R.string.about_based_on)
        binding.aboutGithub.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Darkempire78/OpenCalc")))
        }

        // Privacy policy (bundled text: the app works offline)
        binding.aboutPrivacyPolicy.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.privacy_title)
                .setMessage(R.string.privacy_text)
                .setPositiveButton(R.string.privacy_close, null)
                .show()
        }

        // License and credits
        binding.aboutLicense.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_license_title)
                .setMessage(R.string.about_license_text)
                .setNeutralButton(R.string.about_license_open) { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html")))
                }
                .setPositiveButton(R.string.privacy_close, null)
                .show()
        }

        // Easter egg
        var clickAppVersionCount = 0
        binding.aboutAppVersion.setOnClickListener {
            clickAppVersionCount++
            if (clickAppVersionCount > 3) {
                Toast.makeText(this, this.getString(R.string.about_easter_egg), Toast.LENGTH_SHORT).show()
                clickAppVersionCount = 0
            }
        }
    }
}
