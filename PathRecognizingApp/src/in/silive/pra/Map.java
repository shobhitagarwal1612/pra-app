package in.silive.pra;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class Map extends FragmentActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

	private static final int GPS_ERRORDIALOG_REQUEST = 9001;
	@SuppressWarnings("unused")
	private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9002;
	GoogleMap mMap;
	Button track, stop, retrack, view;
	boolean s1 = false;
	boolean s2 = false;
	boolean flag = true;
	double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
	int x = 0;
	private static final float DEFAULTZOOM = 15;
	@SuppressWarnings("unused")
	private static final String LOGTAG = "Maps";
	LocationClient mLocationClient;
	DB entry = new DB(Map.this);
	/* DB info = new DB(this); */
	Polyline line;

	/* MarkerOptions options1, options2, options3; */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (servicesOK()) {
			setContentView(R.layout.activity_map);

			if (initMap()) {
				// mMap.setMyLocationEnabled(true);
				mLocationClient = new LocationClient(this, this, this);
				mLocationClient.connect();
			} else {
				Toast.makeText(this, "Map not available!", Toast.LENGTH_SHORT)
						.show();
			}
		} else {
			setContentView(R.layout.activity_map);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MediaPlayer mp = MediaPlayer.create(this, R.raw.click);

		// Inflate the menu; this adds items to the action bar if it is present.
		track = (Button) findViewById(R.id.button1);
		stop = (Button) findViewById(R.id.button2);
		retrack = (Button) findViewById(R.id.button3);

		track.setOnTouchListener(new View.OnTouchListener() {

			@SuppressLint("ClickableViewAccessibility") @Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (s2 == false) {
						track.setBackground(getResources().getDrawable(
								R.drawable.track2));
					}
					// Button Pressed
				}
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (s2 == false) {
						x = 1;
						track.setBackground(getResources().getDrawable(
								R.drawable.button_stop));
						stop.setBackground(getResources().getDrawable(
								R.drawable.paused));
						mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
						mp.start();
						s1 = true;
						entry.open();
						Location currentLocation = mLocationClient
								.getLastLocation();
						entry.createEntry(currentLocation.getLatitude(),
								currentLocation.getLongitude());
						x1 = (double) currentLocation.getLatitude();
						y1 = (double) currentLocation.getLongitude();
						gotoCurrentLocationStart();

					}
				}
				return true;
			}
		});
		stop.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				s2 = true;
				if (s1 == true) {
					mp.start();
					mp.stop();
					track.setBackground(getResources().getDrawable(
							R.drawable.ic_button));
					stop.setBackground(getResources().getDrawable(
							R.drawable.pause_pressed));
					gotoCurrentLocationStop();
					s1 = false;
					s2 = false;
					entry.close();
				}
			}
		});

		retrack.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mMap.clear();
				gotoCurrentLocation();
			}

		});

		getMenuInflater().inflate(R.menu.splash, menu);
		return true;
	}

	public boolean servicesOK() {
		int isAvailable = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);

		if (isAvailable == ConnectionResult.SUCCESS) {
			return true;
		} else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable,
					this, GPS_ERRORDIALOG_REQUEST);
			dialog.show();
		} else {
			Toast.makeText(this, "Can't connect to Google Play services",
					Toast.LENGTH_SHORT).show();
		}
		return false;
	}

	private boolean initMap() {
		if (mMap == null) {
			SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map);
			mMap = mapFrag.getMap();
		}

		return (mMap != null);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.mapTypeNone:
			mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
			break;
		case R.id.mapTypeNormal:
			mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			break;
		case R.id.mapTypeSatellite:
			mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
			break;
		case R.id.mapTypeTerrain:
			mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
			break;
		case R.id.mapTypeHybrid:
			mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
			break;
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStop() {
		super.onStop();
		MapStateManager mgr = new MapStateManager(this);
		mgr.saveMapState(mMap);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MapStateManager mgr = new MapStateManager(this);
		CameraPosition position = mgr.getSavedCameraPosition();
		if (position != null) {
			CameraUpdate update = CameraUpdateFactory
					.newCameraPosition(position);
			mMap.moveCamera(update);
			// This is part of the answer to the code challenge
			mMap.setMapType(mgr.getSavedMapType());
		}

		if (mLocationClient.isConnected()) {
			requestLocationUpdates();
		}
	}

	protected void gotoCurrentLocationStart() {

		Location currentLocation = mLocationClient.getLastLocation();
		if (currentLocation == null) {
			Toast.makeText(this, "Current location isn't available",
					Toast.LENGTH_SHORT).show();
		} else {
			LatLng ll = new LatLng(currentLocation.getLatitude(),
					currentLocation.getLongitude());
			CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll,
					DEFAULTZOOM);
			mMap.animateCamera(update);
			String Start = "Start";
			MarkerOptions options1 = new MarkerOptions()
					.title(Start)
					.position(ll)
					.anchor(.5f, .5f)
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.start));
			mMap.addMarker(options1);

		}

	}

	protected void gotoCurrentLocation() {
		Location currentLocation = mLocationClient.getLastLocation();
		if (currentLocation == null) {
			Toast.makeText(this, "Current location isn't available",
					Toast.LENGTH_SHORT).show();
		} else {
			LatLng ll = new LatLng(currentLocation.getLatitude(),
					currentLocation.getLongitude());
			CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll,
					DEFAULTZOOM);
			mMap.animateCamera(update);

			if (x1 == 0) {
				x1 = (double) currentLocation.getLatitude();
				y1 = (double) currentLocation.getLongitude();
			}
			x2 = (double) currentLocation.getLatitude();
			y2 = (double) currentLocation.getLongitude();
			if (Math.abs(x2 - x1) > 0.00001 || Math.abs(y2 - y1) > 0.5) {
				entry.createEntry(x2, y2);
				LatLng ll2 = new LatLng(x1, y1);
				PolylineOptions line2 = new PolylineOptions().add(ll).add(ll2)
						.color(Color.BLUE);
				line = mMap.addPolyline(line2);
				x1 = x2;
				y1 = y2;
			}

		}
	}

	protected void gotoCurrentLocationStop() {
		flag = false;
		Location currentLocation = mLocationClient.getLastLocation();
		if (currentLocation == null) {
			Toast.makeText(this, "Current location isn't available",
					Toast.LENGTH_SHORT).show();
		} else {
			LatLng ll = new LatLng(currentLocation.getLatitude(),
					currentLocation.getLongitude());
			CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll,
					DEFAULTZOOM);
			mMap.animateCamera(update);
			String Stop = "Stop";
			MarkerOptions options3 = new MarkerOptions()
					.title(Stop)
					.position(ll)
					.anchor(.5f, .5f)
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.stop));
			mMap.addMarker(options3);

			LatLng ll2 = new LatLng(x1, y1);
			PolylineOptions line2 = new PolylineOptions().add(ll).add(ll2);
			line = mMap.addPolyline(line2);

		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnected(Bundle arg0) {
		Toast.makeText(this, "Connected to location service",
				Toast.LENGTH_SHORT).show();
		requestLocationUpdates();
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub

	}

	private void requestLocationUpdates() {
		LocationRequest request = LocationRequest.create();
		request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		request.setInterval(2000);
		request.setFastestInterval(1000);
		mLocationClient.requestLocationUpdates(request, this);
	}

	@Override
	public void onLocationChanged(Location loc) {
		if (x == 0) {
			gotoCurrentLocation();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mLocationClient.removeLocationUpdates(this);
	}

}
