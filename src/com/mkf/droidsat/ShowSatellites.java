package com.mkf.droidsat;

/*
 <p>This programme is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public Licence for more details.</p>

 <p>You should have received a copy of the GNU General Public Licence
 along with this programme; if not, write to the Free Software
 Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.</p>
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import uk.me.chiandh.Lib.Hmelib;
import uk.me.chiandh.Sputnik.Satellite;
import uk.me.chiandh.Sputnik.SatellitePosition;
import uk.me.chiandh.Sputnik.SatelliteTrack;
import uk.me.chiandh.Sputnik.Telescope;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

public class ShowSatellites extends Activity {
	
	/** Called when the activity is first created. */
	private static final int SENSOR_SAMPLE_SIZE = 5;
	float heading = 0;
	float pitch = 0;
	float roll = 0;
	private static float _prevHeading = 0;
	private static float _prevPitch = 0;
	StereoView stereoView;
	SensorManager sensorManager;
	CameraPreview cameraPreview;
	LocationManager locationManager;
	static Location location;
	static boolean manualLocation = false;
	static int latitude = 0;
	static int longitude = 0;
	static double lat = 49;
	static double lon = -122;
	static double alt = 0;
	// EditText bearingText;
	EditText pitchRollText;
	private static Spinner satellites;
	private static ImageView tintPane;
	public static volatile ArrayList<SatellitePosition> satellitePositions = new ArrayList<SatellitePosition>();
	private static Satellite satellite = new Satellite();
	private static Telescope station = new Telescope();
	public static volatile SatelliteTrack satelliteTrack;
	public static volatile boolean updateSatelliteTrack = false;
	private Resources droidSatResources = null;
	private InputStream satFileStream = null;
	private ArrayAdapter<SatellitePosition> satPosnsAdapter;
	private Handler handler = new Handler();
	public volatile static boolean orientationLocked = false;
	public static SatellitePosition selectedSatPosn = null;
	/* list of celestrak TLEs */
	private static final String[] celestrakTles = { "tle-new",
			"stations", "visual", "1999-025", "iridium-33-debris",
			"cosmos-2251-debris", "weather", "noaa", "goes", "resource",
			"sarsat", "dmc", "tdrss", "geo", "intelsat", "gorizont", "raduga",
			"molniya", "iridium", "orbcomm", "globalstar", "amateur", "x-comm",
			"other-comm", "gps-ops", "glo-ops", "galileo", "sbas", "nnss",
			"musson", "science", "geodetic", "engineering", "military",
			"radar", "cubesat", "other" };
	private static String availTles[] = { "" };
	private static File tleDir;
	private static byte[] tleBuf = new byte[8192];
	private static int tleMenuId;
	private static final String[] speeds = { "1", "10", "30", "60" };
	private static Thread satPosUpdateThread = null;
	public static String selectedTle = "stations.txt";
	private static String previousTle = "visual";
	static volatile boolean loadingTle = false;
	static volatile boolean gettingTles = false;
	private static volatile boolean nightVis = false;
	public static volatile boolean video = false;
	private static volatile boolean forceLoadTle = false;
	private static volatile boolean forceResetLocation = false;
	public static volatile double magDeclination;
	private static volatile boolean threadSuspended = false;
	public volatile static int selectedSpeed = 1;
	private float headingDiff = 0;
	private int lastHdiff = 0;
	private static float[] headingDiffs = new float[SENSOR_SAMPLE_SIZE];
	private static float[] sortedHeadingDiffs = new float[SENSOR_SAMPLE_SIZE];

	private float pitchDiff = 0;
	private int lastPdiff = 0;
	private static float[] pitchDiffs = new float[SENSOR_SAMPLE_SIZE];
	private static float[] sortedPitchDiffs = new float[SENSOR_SAMPLE_SIZE];
	public static volatile boolean fullSky = false;
	public static volatile float viewAngle = -1;
	private static Context instance;
	private static volatile int sensorSensitivity = 10;
	public static volatile boolean sensorOrientationOn = true;

	public static final String PREFS_NAME = "DroidSatPrefsFile";
	public static volatile long displayTime;
	private static volatile int prevSatelliteSelection;

	private static final int matrix_size = 16;
	private static volatile float[] Rm = new float[matrix_size];
	private static volatile float[] outRm = new float[matrix_size];
	private static volatile float[] Im = new float[matrix_size];
	private static volatile float[] valuesM = new float[3];
	private static volatile float[] mags = new float[3];
	private static volatile float[] accels = new float[3];
	private static volatile boolean resetVideoProjectionRadius = false;
	private static float rawHeading;
	private static OnSharedPreferenceChangeListener prefChangeListener = new OnSharedPreferenceChangeListener() {

		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {

			setPreference(sharedPreferences, key);

		}

	};
	private static Double manualLat = 49d;
	private static Double manualLon = -121d;

	private static void setPreference(SharedPreferences sharedPreferences,
			String key) {

		if (key.equals("redScreenOn")) {
			nightVis = sharedPreferences.getBoolean(key, false);
		} else if (key.equals("sensorOrientationOn")) {
			sensorOrientationOn = sharedPreferences.getBoolean(key, true);
			orientationLocked = false;
		} else if (key.equals("displayType")) {
			String displayType = sharedPreferences.getString(key,
					"Stereographic");
			if (displayType.equals("Full Sky")) {
				fullSky = true;
				video = false;
			} else if (displayType.equals("Video Backdrop")) {
				video = true;
				fullSky = false;
				resetVideoProjectionRadius = true;
			} else {
				video = false;
				fullSky = false;
				StereoView.projectionRadius = 480;
			}
			orientationLocked = false;
		}

		else if (key.equals("satelliteTrackOn")) {
			StereoView.displaySatelliteTrack = sharedPreferences.getBoolean(
					key, true);
		} else if (key.equals("altAzOn")) {
			StereoView.displayAltAzGrid = sharedPreferences.getBoolean(key,
					true);
		} else if (key.equals("darkSatsOn")) {
			StereoView.displayDarkSats = sharedPreferences
					.getBoolean(key, true);
		} else if (key.equals("lowSatsOn")) {
			StereoView.displayLowSats = sharedPreferences.getBoolean(key, true);
		}

		else if (key.equals("textSize")) {
			String textSize = sharedPreferences.getString(key, "medium");
			if (textSize.equals("small")) {
				StereoView.textSize = 12.0f;
			} else if (textSize.equals("medium")) {
				StereoView.textSize = 16.0f;
			} else if (textSize.equals("large")) {
				StereoView.textSize = 20.0f;
			}

			StereoView.textHeight = (int) StereoView.textSize;
		}

		else if (key.equals("altAzGridSize") || key.equals("altAzSmoothing")) {
			StereoView.latDisplayDegrees = Integer.valueOf(sharedPreferences
					.getString("altAzGridSize", "30"));
			StereoView.lonDisplayDegrees = Integer.valueOf(sharedPreferences
					.getString("altAzGridSize", "30"));
			StereoView.lonIncrement = StereoView.lonDisplayDegrees
					/ StereoView.gridSizeDegrees;
			StereoView.latIncrement = StereoView.latDisplayDegrees
					/ StereoView.gridSizeDegrees;

			String altAzSmoothing = sharedPreferences.getString(
					"altAzSmoothing", "medium");

			if (altAzSmoothing.equals("low")) {
				switch (StereoView.latDisplayDegrees) {

				case 15:
					StereoView.segmentsPerLine = 1;
					break;
				case 30:
					StereoView.segmentsPerLine = 2;
					break;
				case 45:
					StereoView.segmentsPerLine = 3;
					break;
				case 90:
					StereoView.segmentsPerLine = 6;
					break;
				}
			} else if (altAzSmoothing.equals("medium")) {
				switch (StereoView.latDisplayDegrees) {

				case 15:
					StereoView.segmentsPerLine = 1;
					break;
				case 30:
					StereoView.segmentsPerLine = 3;
					break;
				case 45:
					StereoView.segmentsPerLine = 3;
					break;
				case 90:
					StereoView.segmentsPerLine = 9;
					break;
				}
			} else if (altAzSmoothing.equals("high")) {
				switch (StereoView.latDisplayDegrees) {

				case 15:
					StereoView.segmentsPerLine = 3;
					break;
				case 30:
					StereoView.segmentsPerLine = 6;
					break;
				case 45:
					StereoView.segmentsPerLine = 9;
					break;
				case 90:
					StereoView.segmentsPerLine = 18;
					break;
				}
			}
		}

		else if (key.equals("textColor")) {

			String selectedColor = sharedPreferences.getString(key, "green");
			if (selectedColor.equals("white")) {
				StereoView.textPaint.setColor(Color.WHITE);
			} else if (selectedColor.equals("red")) {
				StereoView.textPaint.setColor(Color.RED);
			} else if (selectedColor.equals("black")) {
				StereoView.textPaint.setColor(Color.BLACK);
			} else if (selectedColor.equals("blue")) {
				StereoView.textPaint.setColor(Color.BLUE);
			} else if (selectedColor.equals("green")) {
				StereoView.textPaint.setColor(Color.GREEN);
			}
		}

		else if (key.equals("satColor")) {

			String selectedColor = sharedPreferences.getString(key, "green");
			if (selectedColor.equals("white")) {
				StereoView.satPaint.setColor(Color.WHITE);
				StereoView.sunlitPaint.setColor(Color.WHITE);
				StereoView.notSunlitPaint.setColor(Color.WHITE);
			} else if (selectedColor.equals("red")) {
				StereoView.satPaint.setColor(Color.RED);
				StereoView.sunlitPaint.setColor(Color.RED);
				StereoView.notSunlitPaint.setColor(Color.RED);
			} else if (selectedColor.equals("black")) {
				StereoView.satPaint.setColor(Color.BLACK);
				StereoView.sunlitPaint.setColor(Color.BLACK);
				StereoView.notSunlitPaint.setColor(Color.BLACK);
			} else if (selectedColor.equals("blue")) {
				StereoView.satPaint.setColor(Color.BLUE);
				StereoView.sunlitPaint.setColor(Color.BLUE);
				StereoView.notSunlitPaint.setColor(Color.BLUE);
			} else if (selectedColor.equals("green")) {
				StereoView.satPaint.setColor(Color.GREEN);
				StereoView.sunlitPaint.setColor(Color.GREEN);
				StereoView.notSunlitPaint.setColor(Color.GREEN);
			}
		}

		else if (key.equals("altAzColor")) {

			String selectedColor = sharedPreferences.getString(key, "green");
			if (selectedColor.equals("white")) {
				StereoView.latLonPaint.setColor(Color.WHITE);
			} else if (selectedColor.equals("red")) {
				StereoView.latLonPaint.setColor(Color.RED);
			} else if (selectedColor.equals("black")) {
				StereoView.latLonPaint.setColor(Color.BLACK);
			} else if (selectedColor.equals("blue")) {
				StereoView.latLonPaint.setColor(Color.BLUE);
			} else if (selectedColor.equals("green")) {
				StereoView.latLonPaint.setColor(Color.GREEN);
			}
		}

		else if (key.equals("sensorSensitivity")) {
			String sensitivity = sharedPreferences.getString(key, "medium");
			if (sensitivity.equals("low")) {
				sensorSensitivity = 30;
			} else if (sensitivity.equals("medium")) {
				sensorSensitivity = 15;
			} else if (sensitivity.equals("high")) {
				sensorSensitivity = 10;
			}

			StereoView.textHeight = (int) StereoView.textSize;
		}

		else if (key.equals("manualLocation")) {

			manualLocation = sharedPreferences.getBoolean(key, false);

			forceResetLocation = true;
		}

		else if (key.equals("manualLat")) {
			try {

				manualLat = Double.valueOf(sharedPreferences.getString(key,
						"49"));
				if (90 < manualLat || -90 > manualLat) {
					throw (new Exception());
				}

				forceResetLocation = true;
			} catch (Exception e) {
				manualLat = 0d;
			}
		}

		else if (key.equals("manualLon")) {
			try {

				manualLon = Double.valueOf(sharedPreferences.getString(key,
						"-121"));
				if (180 < manualLon || -180 > manualLon) {
					throw (new Exception());
				}

				forceResetLocation = true;
			} catch (Exception e) {
				manualLon = 0d;
			}
		}

	}

	private final SensorEventListener sensorEventListener = new SensorEventListener() {

		public void onSensorChanged(SensorEvent event) {
			if (sensorOrientationOn || video) {// always use sensor orientation
												// when video is on

				int type = event.sensor.getType();
				boolean magChanged = false;
				boolean accelChanged = false;

				switch (type) {
				case Sensor.TYPE_MAGNETIC_FIELD:
					mags = event.values;
					magChanged = true;
					break;
				case Sensor.TYPE_ACCELEROMETER:
					accels = event.values;
					accelChanged = true;
					break;
				}

				if (mags != null && accels != null
						&& (accelChanged || magChanged)) {

					SensorManager.getRotationMatrix(Rm, Im, accels, mags);
					SensorManager.remapCoordinateSystem(Rm,
							SensorManager.AXIS_X, SensorManager.AXIS_Z, outRm);
					SensorManager.getOrientation(outRm, valuesM);
					rawHeading = (float) Math.toDegrees(valuesM[0])
							+ (float) magDeclination;
					if (rawHeading > 360)
						rawHeading = rawHeading % 360;
					if (rawHeading < 0)
						rawHeading = (rawHeading + 360) % 360;

					updateOrientation(rawHeading,
							(float) Math.toDegrees(valuesM[1]) * -1,
							(float) Math.toDegrees(valuesM[2]), magChanged,
							accelChanged);

					magChanged = false;
					accelChanged = false;

				}

			}

		}

		public void onAccuracyChanged(Sensor arg0, int arg1) {
			// TODO Auto-generated method stub

		}

	};

	@Override
	protected void onRestart() {
		if (threadSuspended) {
			threadSuspended = false;
		}
		super.onRestart();
	}

	@Override
	protected void onResume() {
		if (threadSuspended) {
			threadSuspended = false;
		}
		super.onResume();

		sensorManager.registerListener(sensorEventListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(sensorEventListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_GAME);

	}

	@Override
	protected void onPause() {
		sensorManager.unregisterListener(sensorEventListener);
		threadSuspended = true;
		if (this.cameraPreview != null && this.cameraPreview.inPreview) {
			this.cameraPreview.turnOff();
		}
		super.onPause();

	}

	@Override
	protected void onStop() {
		sensorManager.unregisterListener(sensorEventListener);
		threadSuspended = true;
		if (this.cameraPreview != null && this.cameraPreview.inPreview) {
			this.cameraPreview.turnOff();
		}
		super.onStop();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		stereoView = (StereoView) this.findViewById(R.id.stereoView);
		cameraPreview = (CameraPreview) this.findViewById(R.id.cameraPreview);
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		satellites = (Spinner) this.findViewById(R.id.satellites);

		tintPane = (ImageView) this.findViewById(R.id.tintPane);
		getLocation();
		if (station == null) {
			station = new Telescope();
		}

		if (satellite == null) {
			satellite = new Satellite();
		}

		// check to see if we have any files, if not get them
		// check external file dir

		refreshTleDir();

		if (availTles != null && availTles.length < 1) {
			gettingTles = true;
		} else if (satellitePositions.isEmpty()) {
			forceLoadTle = true;
		}

		if (threadSuspended) {
			threadSuspended = false;
		}
		if (satPosUpdateThread == null) {

			satPosUpdateThread = new Thread(null, doBackgroundUpdate,
					"Background");
			satPosUpdateThread.start();
		}

		/*
		 * if (!satellitePositions.isEmpty()){ Satellite.showAllSats(null,
		 * station, satellitePositions);//orientation change causing bug }
		 */

		updateSpinner();

		Bitmap tintBitmap = Bitmap.createBitmap(getWindowManager()
				.getDefaultDisplay().getWidth(), getWindowManager()
				.getDefaultDisplay().getHeight(), Bitmap.Config.ARGB_8888);
		tintBitmap.eraseColor(Color.RED);
		tintPane.setImageBitmap(tintBitmap);
		tintPane.setAlpha(0);
		ShowSatellites.instance = this.getApplicationContext();

		if (VERSION.SDK_INT < 8) {
			StereoView.segmentsPerLine = 2;
		} else {
			StereoView.segmentsPerLine = 6;
		}

		PreferenceManager.setDefaultValues(this, R.xml.preference, false);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		restoreValuesFromPreferences(prefs);
		prefs.registerOnSharedPreferenceChangeListener(prefChangeListener);
		updateOrientation(0, 0, 0, true, true);

	}

	private void getLocation() {

		if (manualLocation) {

			if (null != location) {
				location.setLatitude(manualLat);
				location.setLongitude(manualLon);
				location.setAltitude(0);
			}
		} else {
			if (null == locationManager) {
				locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			}
			if (null != locationManager) {

				location = locationManager
						.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (location == null)
					location = locationManager
							.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				if (location == null)
					location = locationManager
							.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			}
		}
	}

	private void refreshTleDir() {

		if (externalStorageAvailable()) {
			String droidSatDir = Environment.getExternalStorageDirectory()
					.getAbsolutePath()
					+ "/Android/data/com.mkf.droidsat/files/";
			tleDir = new File(droidSatDir);

			if (!tleDir.exists()) {
				tleDir.mkdirs();
			}

			if (tleDir != null) {
				availTles = tleDir.list();
			} else {
				availTles = null;
			}

		} else {
			tleDir = null;
			availTles = null;
		}
	}

	private void restoreValuesFromPreferences(SharedPreferences prefs) {

		Map<String, ?> pref = prefs.getAll();

		for (String key : pref.keySet()) {
			setPreference(prefs, key);
		}

	}

	private Runnable doBackgroundUpdate = new Runnable() {
		public void run() {
			updateSatPositions();
		}
	};

	private void updateSatPositions() {
		long lastRealTime = System.currentTimeMillis();
		long lastSimTime = System.currentTimeMillis();
		long currentSimTime = System.currentTimeMillis();
		long currentRealTime = System.currentTimeMillis();
		long diffTime = 0;
		while (true) {
			if (threadSuspended) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (!stereoView.updatingDisplay && !threadSuspended) {

				if (gettingTles) {
					refreshTleDir();
					if (tleDir != null) {
						getSatDataFromNet(tleDir.getAbsolutePath());
						gettingTles = false;
					}
				}
				if (forceLoadTle || selectedTle.compareTo(previousTle) != 0) {
					previousTle = new String(selectedTle);
					loadTle(selectedTle);
					forceLoadTle = false;
				} else {

					if (forceResetLocation) {
						resetLocation();
						forceResetLocation = false;
					}
					currentRealTime = System.currentTimeMillis();
					diffTime = currentRealTime - lastRealTime;
					currentSimTime = lastSimTime + selectedSpeed * diffTime;
					lastRealTime = currentRealTime;
					lastSimTime = currentSimTime;

					if (selectedSpeed == 1) {
						station.SetUTSystem();
						currentSimTime = currentRealTime;
						lastSimTime = currentSimTime;
					} else {
						station.SetUTanyTime(currentSimTime);
					}
					displayTime = currentSimTime;
					Satellite.showAllSats(null, station, satellitePositions);

					// if satellite selection has changed, update the satellite
					// track

					if (StereoView.displaySatelliteTrack
							&& updateSatelliteTrack) {
						if (null != selectedSatPosn
								&& null != selectedSatPosn.sat) {
							satelliteTrack = new SatelliteTrack(
									selectedSatPosn.sat, station, displayTime);
							updateSatelliteTrack = false;
						}
						updateSatelliteTrack = false;
					}
				}

				try {
					if (diffTime < 125) {
						Thread.sleep(125 - diffTime);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				handler.post(doUpdateGui);
			}
		}
	}

	private Runnable doUpdateSpinner = new Runnable() {
		public void run() {
			updateSpinner();
		}
	};

	private void updateSpinner() {

		if (satellitePositions != null && satellitePositions.size() > 0) {

			satPosnsAdapter = new ArrayAdapter<SatellitePosition>(this,
					android.R.layout.simple_spinner_item, satellitePositions);
			satPosnsAdapter
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			satellites.setAdapter(satPosnsAdapter);
			satellites.setSelection(0);
			selectedSatPosn = (SatellitePosition) satellites.getSelectedItem();
			satellites.invalidate();
		}
	}

	private Runnable doEnableSpinner = new Runnable() {
		public void run() {
			enableSpinner();
		}
	};

	private Runnable doDisableSpinner = new Runnable() {
		public void run() {
			disableSpinner();
		}
	};

	private void enableSpinner() {
		satellites.setEnabled(true);
	}

	private void disableSpinner() {
		satellites.setEnabled(false);
	}

	private Runnable doUpdateGui = new Runnable() {
		public void run() {
			if (!loadingTle) {
				updateGui();
				stereoView.invalidate();
			}
		}

	};

	private void updateGui() {
		int selection = satellites.getSelectedItemPosition();
		satellites.setAdapter(satPosnsAdapter);
		if (!loadingTle && selection != AdapterView.INVALID_POSITION
				&& selection < satellites.getCount()) {
			satellites.setSelection(selection);
			selectedSatPosn = (SatellitePosition) satellites.getSelectedItem();
			if (selection != prevSatelliteSelection) {
				updateSatelliteTrack = true;
			}
			prevSatelliteSelection = selection;
		}
		if (nightVis) {
			tintPane.setAlpha(128);
		} else {
			tintPane.setAlpha(0);
		}
		if (video) {
			if (resetVideoProjectionRadius
					|| (this.cameraPreview != null
							&& !this.cameraPreview.inPreview && !fullSky)) {
				resetVideoProjectionRadius = false;
				this.cameraPreview.turnOn();
				StereoView.projectionRadius = Math.max(stereoView.getWidth(),
						stereoView.getHeight())
						/ Math.tan(Math.toRadians(cameraPreview.viewAngle));
			} else if (this.cameraPreview != null && fullSky
					&& this.cameraPreview.inPreview) {
				this.cameraPreview.turnOff();
			}
		} else {
			if (this.cameraPreview != null && this.cameraPreview.inPreview) {
				this.cameraPreview.turnOff();
			}
		}

		if (fullSky) {
			if (sensorOrientationOn) {
				stereoView.setPitch(-180);
			} else {
				stereoView.setPitch(90);
			}
		}
	}

	private void updateOrientation(float _heading, float _pitch, float _roll,
			boolean magChanged, boolean accelChanged) {

		if (magChanged) {
			headingDiff = _heading - _prevHeading;

			if (headingDiff > 180) {
				headingDiff = (360 - headingDiff) * -1;
			} else if (headingDiff < -180) {
				headingDiff = (-360 - headingDiff) * -1;
			}

			if (lastHdiff == SENSOR_SAMPLE_SIZE - 1) {
				lastHdiff = 0;
			}

			headingDiffs[lastHdiff++] = headingDiff;
			System.arraycopy(headingDiffs, 0, sortedHeadingDiffs, 0, headingDiffs.length);
			Arrays.sort(sortedHeadingDiffs);
			_heading = _prevHeading + sortedHeadingDiffs[SENSOR_SAMPLE_SIZE/2] / sensorSensitivity;

		} else {
			_heading = _prevHeading;
		}

		if (accelChanged) {
			pitchDiff = _pitch - _prevPitch;

			if (lastPdiff == SENSOR_SAMPLE_SIZE - 1) {
				lastPdiff = 0;
			}

			pitchDiffs[lastPdiff++] = pitchDiff;
			System.arraycopy(pitchDiffs, 0, sortedPitchDiffs, 0, pitchDiffs.length);
			Arrays.sort(sortedPitchDiffs);
			_pitch = _prevPitch + sortedPitchDiffs[SENSOR_SAMPLE_SIZE/2] / sensorSensitivity;
			
		} else {
			_pitch = _prevPitch;
		}

		if (stereoView != null) {

			if (fullSky) {
				_pitch = -180;
			}

			heading = _heading;
			pitch = _pitch;
			roll = _roll;

			if (droidSatResources == null)
				droidSatResources = getResources();

			stereoView.setHeading(heading);
			stereoView.setPitch(pitch);
			stereoView.setRoll(roll);
			stereoView.invalidate();
		}

		_prevHeading = _heading;
		_prevPitch = _pitch;
	}

	private class SatellitePositionComparator implements Comparator {
		public int compare(Object s1, Object s2) {

			return s1.toString().compareTo(s2.toString());

		}

	}

	public void onVisibilityChanged(boolean arg0) {
		// TODO Auto-generated method stub

	}

	public void onZoom(boolean arg0) {

	}

	private void toggleLock() {
		orientationLocked = !orientationLocked;

	}

	private void resetLocation() {
		TSAGeoMag geoMag = new TSAGeoMag();
		boolean origOrientationLocked = orientationLocked;
		if (!origOrientationLocked)
			orientationLocked = true;

		// disable spinner
		handler.post(doDisableSpinner);
		// satellitePositions.clear();
		station.Init();
		station.SetUTSystem();

		getLocation();
		if (location == null) {

			if (manualLocation) {
				lat = manualLat;
				lon = manualLon;
				alt = 0;
			} else {
				lat = 0;
				lon = 0;
				alt = 0;
			}
		} else {
			lat = location.getLatitude();
			lon = location.getLongitude();
			alt = location.getAltitude();
		}
		magDeclination = geoMag.getDeclination(lat, lon);
		latitude = (int) lat;
		longitude = (int) lon;

		station.SetGeodetic("Vancouver", lon / Hmelib.DEGPERRAD, lat
				/ Hmelib.DEGPERRAD, alt / 1000000000);
		satellite.Init();

		// Satellite.showAllSats(satFileStream, station, satellitePositions);
		// Collections.sort(satellitePositions, new
		// SatellitePositionComparator());

		handler.post(doUpdateSpinner);
		handler.post(doEnableSpinner);
		orientationLocked = origOrientationLocked;
		updateSatelliteTrack = true;

	}

	private void loadTle(String tle) {

		TSAGeoMag geoMag = new TSAGeoMag();
		boolean origOrientationLocked = orientationLocked;
		if (!origOrientationLocked)
			orientationLocked = true;

		// disable spinner
		handler.post(doDisableSpinner);
		satellitePositions.clear();
		station.Init();
		station.SetUTSystem();

		getLocation();
		if (location == null) {

			if (manualLocation) {
				lat = manualLat;
				lon = manualLon;
				alt = 0;
			} else {
				lat = 0;
				lon = 0;
				alt = 0;
			}
		} else {
			lat = location.getLatitude();
			lon = location.getLongitude();
			alt = location.getAltitude();
		}
		magDeclination = geoMag.getDeclination(lat, lon);
		latitude = (int) lat;
		longitude = (int) lon;

		station.SetGeodetic("Vancouver", lon / Hmelib.DEGPERRAD, lat
				/ Hmelib.DEGPERRAD, alt / 1000000000);
		satellite.Init();

		loadingTle = true;
		try {
			refreshTleDir();
			satFileStream = new FileInputStream(new File(tleDir, tle));
		} catch (Exception e) {
			Log.d(this.getClass().getName(), "error reading tle file");
		}

		Satellite.showAllSats(satFileStream, station, satellitePositions);
		Collections.sort(satellitePositions, new SatellitePositionComparator());
		loadingTle = false;
		// enable spinner
		handler.post(doUpdateSpinner);
		handler.post(doEnableSpinner);
		orientationLocked = origOrientationLocked;
		updateSatelliteTrack = true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		MenuItem tleMenuItem = menu.findItem(tleMenuId);
		tleMenuItem.setOnMenuItemClickListener(null);
		SubMenu tleSubMenu = tleMenuItem.getSubMenu();

		tleSubMenu.clear();

		int i = 0;

		if (externalStorageAvailable()) {
			// SD Card connected, create menu items for any files

			refreshTleDir();
			for (String tle : availTles) {

				tleSubMenu.add(0, Menu.FIRST + i, Menu.NONE, tle)
						.setOnMenuItemClickListener(
								new OnMenuItemClickListener() {
									public boolean onMenuItemClick(MenuItem m) {
										selectedTle = m.getTitle().toString();
										stereoView.invalidate();
										return true;
									}
								});

			}
			// SD Card connected but no tle files
			if (availTles == null
					|| (availTles != null && availTles.length == 0)) {
				tleMenuItem
						.setOnMenuItemClickListener(new OnMenuItemClickListener() {
							public boolean onMenuItemClick(MenuItem m) {

								Toast msg = Toast
										.makeText(
												instance,
												"No tles, choose menu->update tles or copy manually to SDCard/Android/data.com.mkf.droidsat/files",
												Toast.LENGTH_LONG);
								msg.show();
								return true;
							}
						});
			}
		} else {
			// SD Card not connected or USB Storage in use
			tleMenuItem
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						public boolean onMenuItemClick(MenuItem m) {

							Toast msg = Toast
									.makeText(
											instance,
											"USB Storage in use. Turn off USB storage and try again",
											Toast.LENGTH_LONG);
							msg.show();
							return true;
						}
					});
		}
		return true;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		int groupId = 0;
		int menuItemOrder = Menu.NONE;

		MenuItem menuItemData = menu.add(groupId, Menu.FIRST, menuItemOrder,
				"update tles");
		MenuItem editPreference = menu.add(groupId, Menu.FIRST + 1,
				menuItemOrder, "preferences");
		SubMenu tleMenu = menu.addSubMenu("choose tles");
		tleMenuId = tleMenu.getItem().getItemId();
		SubMenu speedMenu = menu.addSubMenu("playback speed");

		menuItemData.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem m) {

				try {
					if (externalStorageAvailable()) {
						gettingTles = true;
					} else {
						Toast msg = Toast
								.makeText(
										instance,
										"USB Storage in use. Turn off USB storage and try again",
										Toast.LENGTH_LONG);
						msg.show();
					}
					if (!isOnline()) {
						Toast msg = Toast
								.makeText(
										instance,
										"Internet not enabled or celestrak.net not available",
										Toast.LENGTH_LONG);
						msg.show();
					}

				} catch (Exception e) {
					Toast msg = Toast
							.makeText(
									instance,
									"Internet not enabled or celestrak.net not available",
									Toast.LENGTH_LONG);
					msg.show();
					return true;
				}

				return true;
			}

		});

		editPreference
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem m) {
						startActivity(new Intent(getBaseContext(),
								EditPreferences.class));
						return true;
					}
				});

		int i = 0;

		for (String speed : speeds) {

			speedMenu.add(groupId, Menu.FIRST + i, Menu.NONE, speed)
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						public boolean onMenuItemClick(MenuItem m) {
							selectedSpeed = Integer.valueOf(
									m.getTitle().toString()).intValue();
							return true;
						}
					});

		}

		return true;
	}

	private void getSatDataFromNet(String droidSatDir) {

		String prefix = "http://www.celestrak.net/NORAD/elements/";
		final int BUFFER = 16 * 1024;

		try {

			// standard celestrak tle's
			refreshTleDir();
			for (String tle : celestrakTles) {

				URL satDataUrl = new URL(prefix + tle + ".txt");
				Log.d(this.getClass().getName(), "gettting file " + satDataUrl);

				InputStream is = satDataUrl.openStream();

				FileOutputStream fos = new FileOutputStream(new File(tleDir,
						tle + ".txt"));
				int len;
				while ((len = is.read(tleBuf)) > 0) {
					fos.write(tleBuf, 0, len);
				}
				is.close();
				fos.close();
			}

			// amateur satellite org's tle
			URL amsatDataUrl = new URL(
					"http://www.amsat.org/amsat/ftp/keps/current/nasabare.txt");
			InputStream is = amsatDataUrl.openStream();

			FileOutputStream fos = new FileOutputStream(new File(tleDir,
					"nasabare.txt"));
			int len;
			while ((len = is.read(tleBuf)) > 0) {
				fos.write(tleBuf, 0, len);
			}
			is.close();
			fos.close();

			// mike mccant's classified satellite tle
			URL mcCantUrl = new URL(
					"http://www.prismnet.com/~mmccants/tles/classfd.zip");
			is = mcCantUrl.openStream();

			fos = new FileOutputStream(new File(tleDir, "classfd.zip"));

			while ((len = is.read(tleBuf)) > 0) {
				fos.write(tleBuf, 0, len);
			}
			is.close();
			fos.close();

			BufferedOutputStream dest = null;

			FileInputStream fis = new FileInputStream(new File(tleDir,
					"classfd.zip"));

			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
					fis, BUFFER));
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				System.out.println("Extracting: " + entry);
				int count;
				byte data[] = new byte[BUFFER];
				// write the files to the disk
				fos = new FileOutputStream(new File(tleDir, entry.getName()));
				dest = new BufferedOutputStream(fos, BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
			zis.close();
			new File(tleDir, "classfd.zip").delete();

		} catch (Exception e) {
			System.out.println("error in file or url");

		}

		refreshTleDir();
	}

	private void getSatZipDataFromNet() {

		String prefix = "https://sites.google.com/site/droidsatproject/celestraktles/";
		final int BUFFER = 16 * 1024;

		try {

			URL satDataUrl = new URL(prefix + "tles.zip");

			InputStream is = satDataUrl.openStream();

			FileOutputStream fos = openFileOutput("tles.zip",
					Context.MODE_PRIVATE);
			int b;
			while ((b = is.read()) != -1) {
				fos.write(b);
			}
			is.close();
			fos.close();
		} catch (Exception e) {
			System.out.println("error in file or url reading");

		}
		try {

			BufferedOutputStream dest = null;
			FileInputStream fis = openFileInput("tles.zip");
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
					fis, BUFFER));
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				System.out.println("Extracting: " + entry);
				int count;
				byte data[] = new byte[BUFFER];
				// write the files to the disk
				FileOutputStream fos = openFileOutput(entry.getName(),
						Context.MODE_PRIVATE);
				dest = new BufferedOutputStream(fos, BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
			zis.close();
		} catch (Exception e) {
			System.out.println("error writing");
			System.out.println(e);

		}

	}

	private boolean externalStorageAvailable() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}

		return mExternalStorageAvailable;
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		}
		return false;
	}

}