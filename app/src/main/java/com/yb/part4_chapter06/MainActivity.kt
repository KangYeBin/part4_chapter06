package com.yb.part4_chapter06

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.yb.part4_chapter06.data.Repository
import com.yb.part4_chapter06.data.models.airquality.Grade
import com.yb.part4_chapter06.data.models.airquality.MeasuredValue
import com.yb.part4_chapter06.data.models.monitoringstation.MonitoringStation
import com.yb.part4_chapter06.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var cancellationTokenSource: CancellationTokenSource? = null
    private lateinit var binding: ActivityMainBinding
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindViews()
        initVariables()
        requestLocationPermissions()

    }

    override fun onDestroy() {
        super.onDestroy()
        cancellationTokenSource?.cancel()
        scope.cancel()
    }

    private fun bindViews() {
        binding.refresh.setOnRefreshListener {
            fetchAirQualityData()
        }
    }

    private fun initVariables() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val locationPermissionGranted =
            requestCode == REQUEST_ACCESS_LOCATION_PERMISSIONS &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (!locationPermissionGranted) {
            finish()
        } else {
            fetchAirQualityData()
        }
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            REQUEST_ACCESS_LOCATION_PERMISSIONS
        )
    }

    @SuppressLint("MissingPermission")
    private fun fetchAirQualityData() {
        //TODO FETCH DATA
        cancellationTokenSource = CancellationTokenSource()
        fusedLocationProviderClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
//            LocationRequest.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource!!.token
        ).addOnSuccessListener { location ->
            //API 호출
            scope.launch {
                binding.errorDescriptionTextView.visibility = View.GONE
                try {
                    val monitoringStation =
                        Repository.getNearbyMonitoringStation(location.latitude, location.longitude)

                    val measuredValue =
                        Repository.getLatestAirQualityData(monitoringStation!!.stationName!!)

                    displayAirQualityData(monitoringStation, measuredValue!!)

                } catch (exception: Exception) {
                    binding.errorDescriptionTextView.visibility = View.VISIBLE
                    binding.refresh.isRefreshing = true
                    binding.contentsLayout.alpha = 0F

                } finally {
                    binding.progressBar.visibility = View.GONE
                    binding.refresh.isRefreshing = false
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun displayAirQualityData(monitoringStation: MonitoringStation, measuredValue: MeasuredValue) =
        with(binding) {
            contentsLayout.animate()
                .alpha(1F)
                .start()

            measuringStationNameTextView.text = monitoringStation.stationName
            measuringStationAddressTextView.text = monitoringStation.addr

            (measuredValue.khaiGrade ?: Grade.UNKNOWN).let { grade ->
                root.setBackgroundResource(grade.colorResId)
                totalGradeLabelTextView.text = grade.label
                totalGradeEmojiTextView.text = grade.emoji
            }

            with(measuredValue) {
                fineDustInformationTextView.text =
                    "미세먼지: $pm10Value ㎍/㎥ ${(pm10Grade ?: Grade.UNKNOWN).emoji}"
                ultraFineDustInformationTextView.text =
                    "초미세먼지: $pm25Value ㎍/㎥ ${(pm25Grade ?: Grade.UNKNOWN).emoji}"

                with(so2Item) {
                    labelTextView.text = "아황산가스"
                    gradeTextView.text = (so2Grade ?: Grade.UNKNOWN).toString()
                    valueTextView.text = "$so2Value PPM"
                }

                with(coItem) {
                    labelTextView.text = "일산화탄소"
                    gradeTextView.text = (coGrade ?: Grade.UNKNOWN).toString()
                    valueTextView.text = "$coValue PPM"
                }

                with(o3Item) {
                    labelTextView.text = "오존"
                    gradeTextView.text = (o3Grade ?: Grade.UNKNOWN).toString()
                    valueTextView.text = "$o3Value PPM"
                }

                with(no2Item) {
                    labelTextView.text = "이산화질소"
                    gradeTextView.text = (no2Grade ?: Grade.UNKNOWN).toString()
                    valueTextView.text = "$no2Value PPM"
                }
            }
        }

    companion object {
        private const val REQUEST_ACCESS_LOCATION_PERMISSIONS = 100
    }
}