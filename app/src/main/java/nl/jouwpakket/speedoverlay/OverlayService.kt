package nl.jouwpakket.speedoverlay

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

class OverlayService : Service(), LocationListener {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: SpeedOverlayView
    private lateinit var params: WindowManager.LayoutParams
    private var locationManager: LocationManager? = null

    private val handler = Handler(Looper.getMainLooper())
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var longPressTriggered = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        startForegroundWithNotification()
        setupOverlay()
        requestLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refreshOverlaySettings()
        Prefs.saveRunning(this, true)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            windowManager.removeView(overlayView)
        } catch (_: Exception) {
        }
        locationManager?.removeUpdates(this)
        Prefs.saveRunning(this, false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupOverlay() {
        overlayView = SpeedOverlayView(this).apply {
            setOverlayAlpha(Prefs.loadAlpha(context))
            setScaleFactor(Prefs.loadScale(context))
        }
        val (savedX, savedY) = Prefs.loadPosition(this)
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }

        overlayView.setOnTouchListener { _: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    longPressTriggered = false
                    handler.postDelayed({
                        longPressTriggered = true
                        openSettings()
                    }, 2000)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    windowManager.updateViewLayout(overlayView, params)
                    if (kotlin.math.abs(deltaX) > 10 || kotlin.math.abs(deltaY) > 10) {
                        handler.removeCallbacksAndMessages(null)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacksAndMessages(null)
                    Prefs.savePosition(this, params.x, params.y)
                    if (!longPressTriggered && event.eventTime - event.downTime < 2000) {
                        performClick()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(overlayView, params)
    }

    private fun performClick() {
        // Placeholder to satisfy touch feedback; no-op
    }

    private fun openSettings() {
        val settingsIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(settingsIntent)
    }

    private fun refreshOverlaySettings() {
        overlayView.setOverlayAlpha(Prefs.loadAlpha(this))
        overlayView.setScaleFactor(Prefs.loadScale(this))
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }
        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            1f,
            this,
            Looper.getMainLooper()
        )
    }

    override fun onLocationChanged(location: Location) {
        val speedMps = location.speed
        val unit = Prefs.loadUnit(this)
        val converted = if (unit == SpeedUnit.MPH) speedMps * 2.23694f else speedMps * 3.6f
        overlayView.setSpeed(converted)
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun startForegroundWithNotification() {
        val channelId = "overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val contentIntent = Intent(this, MainActivity::class.java)
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.overlay_running))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(android.app.PendingIntent.getActivity(
                this,
                0,
                contentIntent,
                PendingIntentFlags
            ))
            .build()

        startForeground(1, notification)
    }

    private val PendingIntentFlags: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }
}
