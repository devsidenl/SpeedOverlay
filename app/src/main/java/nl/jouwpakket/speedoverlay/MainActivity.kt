package nl.jouwpakket.speedoverlay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import nl.jouwpakket.speedoverlay.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestOverlayPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                ensureLocationPermissionAndStart()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAlphaControl()
        setupSizeControl()
        setupUnitSpinner()
        setupLanguageSpinner()
        setupButtons()
        binding.previewOverlay.setSpeed(80f)
    }

    override fun onResume() {
        super.onResume()
        updateToggleButton()
    }

    private fun setupAlphaControl() {
        binding.alphaSeek.progress = Prefs.loadAlpha(this)
        binding.alphaSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Prefs.saveAlpha(this@MainActivity, progress)
                binding.previewOverlay.setOverlayAlpha(progress)
                notifyServiceToRefresh()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupSizeControl() {
        val savedScale = Prefs.loadScale(this)
        binding.sizeSeek.progress = (savedScale * 100).toInt().coerceAtLeast(50)
        binding.previewOverlay.setScaleFactor(savedScale)
        binding.sizeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = progress.coerceAtLeast(50) / 100f
                Prefs.saveScale(this@MainActivity, scale)
                binding.previewOverlay.setScaleFactor(scale)
                notifyServiceToRefresh()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupUnitSpinner() {
        val units = listOf(getString(R.string.unit_kmh), getString(R.string.unit_mph))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units)
        binding.unitSpinner.adapter = adapter
        binding.unitSpinner.setSelection(if (Prefs.loadUnit(this) == SpeedUnit.KMH) 0 else 1)
        binding.unitSpinner.setOnItemSelectedListener { position ->
            val unit = if (position == 0) SpeedUnit.KMH else SpeedUnit.MPH
            Prefs.saveUnit(this, unit)
        }
    }

    private fun setupLanguageSpinner() {
        val languages = listOf(
            "nl" to getString(R.string.language_nl),
            "en" to getString(R.string.language_en),
            "fr" to getString(R.string.language_fr),
            "de" to getString(R.string.language_de),
            "it" to getString(R.string.language_it),
            "es" to getString(R.string.language_es),
            "pt" to getString(R.string.language_pt)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages.map { it.second })
        binding.languageSpinner.adapter = adapter
        val savedCode = Prefs.loadLanguage(this)
        val selectedIndex = languages.indexOfFirst { it.first == savedCode }.takeIf { it >= 0 } ?: 0
        binding.languageSpinner.setSelection(selectedIndex)
        binding.languageSpinner.setOnItemSelectedListener { position ->
            Prefs.saveLanguage(this, languages[position].first)
        }
    }

    private fun setupButtons() {
        binding.toggleOverlayButton.setOnClickListener {
            if (Prefs.isRunning(this)) {
                stopService(Intent(this, OverlayService::class.java))
                Prefs.saveRunning(this, false)
            } else {
                startOverlayFlow()
            }
            updateToggleButton()
        }

        binding.exitButton.setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            finish()
        }
    }

    private fun updateToggleButton() {
        val running = Prefs.isRunning(this)
        binding.toggleOverlayButton.setText(if (running) R.string.stop_overlay else R.string.start_overlay)
    }

    private fun startOverlayFlow() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            requestOverlayPermission.launch(intent)
            return
        }
        ensureLocationPermissionAndStart()
    }

    private fun ensureLocationPermissionAndStart() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
            return
        }
        startService(Intent(this, OverlayService::class.java))
        Prefs.saveRunning(this, true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startService(Intent(this, OverlayService::class.java))
            Prefs.saveRunning(this, true)
            updateToggleButton()
        }
    }

    private fun notifyServiceToRefresh() {
        if (Prefs.isRunning(this)) {
            startService(Intent(this, OverlayService::class.java))
        }
    }

    private fun Spinner.setOnItemSelectedListener(onSelected: (position: Int) -> Unit) {
        this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                onSelected(position)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    companion object {
        private const val REQUEST_LOCATION = 2001
    }
}
