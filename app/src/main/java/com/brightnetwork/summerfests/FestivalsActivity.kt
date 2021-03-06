package com.brightnetwork.summerfests

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IllegalStateException

class FestivalsActivity : AppCompatActivity() {

    val adapterRV = FestivalsAdapter { festival ->
        FestivalDetailsActivity.getIntent(this@FestivalsActivity, festival)
            .also {
                startActivity(it)
            }
    }

    lateinit var viewModel: FestivalsViewModel
    var map: GoogleMap? = null
    val markersMap = mutableMapOf<Festival, Marker>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(FestivalsViewModel::class.java)
        setContentView(R.layout.activity_festivals)
        findViewById<RecyclerView>(R.id.festivals).apply {
            adapter = adapterRV
        }
        (supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment)?.let {
            it.getMapAsync {
                map = it
            }
        }

        setupFirebaseMessaging()
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            throw IllegalStateException("TEST crash")
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_1 -> {
                    InfoDialog().show(supportFragmentManager, "InfoDialog")
                    true
                }
                R.id.action_2 -> {
                    Firebase.analytics.logEvent("Action2", null)
                    Toast.makeText(this, "You've just clicked a crosount", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }


        adapterRV.setData(Datasource().loadFestivals(this@FestivalsActivity))

        viewModel._dataStream.observe(this) { festivalsState ->
            when (festivalsState) {
                FestivalsViewModel.FestivalsState.Loading -> Toast.makeText(this, "Loading!", Toast.LENGTH_SHORT)
                    .show()
                is FestivalsViewModel.FestivalsState.Loaded -> {
                    adapterRV.setData(festivalsState.list)
                    map?.apply {
                        markersMap.values.forEach {
                            it.remove()
                        }
                        festivalsState.list.forEach { festival ->
                            festival.position?.let { position ->
                                val marker = addMarker(MarkerOptions().title(festival.title).position(position))
                                markersMap[festival] = marker
                            }
                        }
                        animateCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                LatLngBounds(
                                    LatLng(
                                        50.8619974,
                                        -4.2396426
                                    ),
                                    LatLng(
                                        55.9412846,
                                        -0.098903
                                    ),
                                ), 100
                            )
                        )
                    }
                }
                FestivalsViewModel.FestivalsState.Error -> Toast.makeText(this, "ERRROR!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFirebaseMessaging() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FMFMFM", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            Log.d("FMFMFM", "$token")
        })
    }
}