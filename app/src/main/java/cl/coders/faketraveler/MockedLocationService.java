package cl.coders.faketraveler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MockedLocationService extends Service {

    @NonNull
    private static final String TAG = MockedLocationService.class.getSimpleName();

    public static final String ACTION_START = "cl.coders.faketraveler.action.START";
    public static final String ACTION_STOP = "cl.coders.faketraveler.action.STOP";
    public static final String ACTION_TOGGLE = "cl.coders.faketraveler.action.TOGGLE";

    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "mocked_location_channel";

    private static volatile boolean isRunning = false;
    @Nullable
    private static volatile MockedLocationService activeInstance = null;

    @NonNull
    protected final MutableLiveData<MockState> mockState = new MutableLiveData<>();
    @NonNull
    protected final MutableLiveData<Location> mockedLocation = new MutableLiveData<>();

    @NonNull
    private final List<MockedLocationProvider> providers = new ArrayList<>();

    @NonNull
    private Timer timer = new Timer();
    @NonNull
    private final Set<TimerTask> tasks = Collections.synchronizedSet(new HashSet<>());

    public static boolean isMocking() {
        return isRunning;
    }

    public static void startMockService(@NonNull Context context) {
        Intent intent = new Intent(context, MockedLocationService.class);
        intent.setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopMockService(@NonNull Context context) {
        if (activeInstance != null) {
            activeInstance.stopMock();
            activeInstance.stopSelf();
        } else {
            Intent intent = new Intent(context, MockedLocationService.class);
            intent.setAction(ACTION_STOP);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent);
            } else {
                context.startService(intent);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        activeInstance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        activeInstance = this;
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                stopMock();
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_START.equals(action)) {
                startMockFromPreferences();
                return START_STICKY;
            } else if (ACTION_TOGGLE.equals(action)) {
                if (isRunning) {
                    stopMock();
                    stopSelf();
                    return START_NOT_STICKY;
                } else {
                    startMockFromPreferences();
                    return START_STICKY;
                }
            }
        }
        return START_NOT_STICKY;
    }

    public void startMockFromPreferences() {
        SharedPreferences sharedPref = getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
        double lat = SharedPrefsUtil.getDouble(sharedPref, "lat", 12.0);
        double lng = SharedPrefsUtil.getDouble(sharedPref, "lng", 15.0);
        int mockCount = sharedPref.getInt("mockCount", 0);
        int mockFrequency = sharedPref.getInt("mockFrequency", 10);
        if (mockFrequency <= 0) mockFrequency = 1;
        double dLat = SharedPrefsUtil.getDouble(sharedPref, "dLat", 0.0);
        double dLng = SharedPrefsUtil.getDouble(sharedPref, "dLng", 0.0);
        boolean mockSpeed = sharedPref.getBoolean("mockSpeed", true);

        float speed = 0f;
        if (mockSpeed) {
            float[] results = new float[1];
            Location.distanceBetween(lat, lng, lat + dLat / 1000000.0, lng + dLng / 1000000.0, results);
            speed = results[0] / (mockFrequency * 1000L);
        }

        ensureForeground(lat, lng);
        startMockedService(lng, lat, dLng / 1000000.0, dLat / 1000000.0, mockFrequency * 1000L, mockCount, speed);

        long endTime = mockCount > 0 ? (System.currentTimeMillis() + (mockCount - 1L) * mockFrequency * 1000L) : 0;
        sharedPref.edit().putLong("endTime", endTime).apply();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription(getString(R.string.notification_channel_desc));
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(double lat, double lng) {
        createNotificationChannel();

        String contentText = getString(R.string.notification_text,
                MainActivity.DECIMAL_FORMAT.format(lat), MainActivity.DECIMAL_FORMAT.format(lng));

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingOpen = PendingIntent.getActivity(this, 0, openIntent, getPendingIntentFlags());

        Intent stopIntent = new Intent(this, FakeTravelerWidget.class);
        stopIntent.setAction(FakeTravelerWidget.ACTION_STOP_MOCK);
        PendingIntent pendingStop = PendingIntent.getBroadcast(this, 1, stopIntent, getPendingIntentFlags());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingOpen)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.widget_action_stop), pendingStop);

        return builder.build();
    }

    private void ensureForeground(double lat, double lng) {
        Notification notification = buildNotification(lat, lng);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } catch (SecurityException e) {
                Log.e(TAG, "Cannot start foreground service with type location", e);
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @NonNull
    @Override
    public IBinder onBind(Intent intent) {
        indicateBinding();
        return new MockedBinder(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Mock onUnbind");
        if (!isRunning) {
            stopMock();
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MockedLocationService onDestroy");
        stopMock();
        activeInstance = null;
        super.onDestroy();
    }

    private void indicateBinding() {
        if (isRunning) {
            mockState.postValue(MockState.MOCKED);
        } else {
            mockState.postValue(MockState.SERVICE_BOUND);
        }
    }

    protected synchronized void startMockedService(double longitude, double latitude, double longitudeDistance, double latitudeDistance, long mockMilli, int maxTime, float mockSpeed) {
        try {
            for (TimerTask t : tasks) {
                t.cancel();
            }
            tasks.clear();

            if (providers.isEmpty()) {
                providers.add(new MockedLocationProvider(LocationManager.GPS_PROVIDER, this));
                providers.add(new MockedLocationProvider(LocationManager.NETWORK_PROVIDER, this));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    providers.add(new MockedLocationProvider(LocationManager.FUSED_PROVIDER, this));
                }
            }

            MockedTask mockedTask = new MockedTask(longitude, latitude, longitudeDistance, latitudeDistance, maxTime, mockSpeed);
            scheduleTimerTask(mockedTask, 0L, mockMilli);
            tasks.add(mockedTask);
            isRunning = true;
            mockState.postValue(MockState.MOCKED);
            FakeTravelerWidget.updateAllWidgets(this);
        } catch (SecurityException e) {
            Log.e(TAG, "Could not construct mock location providers!", e);
            mockState.postValue(MockState.MOCK_ERROR);
            stopMock();
            stopSelf();
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(getApplicationContext(), R.string.MainActivity_MockNotApplied, Toast.LENGTH_LONG).show()
            );
        }
    }

    @SuppressWarnings("deprecation")
    public synchronized void stopMock() {
        Log.d(TAG, "Mock is stopping");
        for (TimerTask t : tasks) {
            t.cancel();
        }
        tasks.clear();
        for (MockedLocationProvider prov : providers) {
            prov.shutdown();
        }
        providers.clear();
        isRunning = false;
        mockState.postValue(MockState.NOT_MOCKED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }

        SharedPreferences sharedPref = getSharedPreferences(MainActivity.sharedPrefKey, Context.MODE_PRIVATE);
        sharedPref.edit().putLong("endTime", 0).apply();

        FakeTravelerWidget.updateAllWidgets(this);
    }

    private synchronized void scheduleTimerTask(TimerTask task, long delay, long period) {
        try {
            timer.schedule(task, delay, period);
        } catch (IllegalStateException e) {
            timer = new Timer();
            timer.schedule(task, delay, period);
        }
    }

    private static int getPendingIntentFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.FLAG_UPDATE_CURRENT;
    }

    class MockedTask extends TimerTask {
        private final float speed;
        private double longitude;
        private double latitude;
        private final double longitudeMockedDistance;
        private final double latitudeMockedDistance;
        private final int maxLocationTimes;
        private int currentTimes = 0;

        public MockedTask(double longitude, double latitude, double longitudeMockedDistance, double latitudeMockedDistance, int maxTimes, float mockSpeed) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.longitudeMockedDistance = longitudeMockedDistance;
            this.latitudeMockedDistance = latitudeMockedDistance;
            this.maxLocationTimes = maxTimes;
            this.speed = mockSpeed;
        }

        @Override
        public void run() {
            Location value = new Location(LocationManager.GPS_PROVIDER);
            value.setLongitude(longitude);
            value.setLatitude(latitude);
            if (speed > 0) {
                value.setSpeed(speed);
                value.setAccuracy(0.1f);
                value.setTime(System.currentTimeMillis());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    value.setSpeedAccuracyMetersPerSecond(0.01f);
                }
            }

            mockedLocation.postValue(value);
            for (MockedLocationProvider prov : providers)
                prov.pushLocation(latitude, longitude);
            ++currentTimes;
            if (maxLocationTimes != 0 && maxLocationTimes == currentTimes) {
                this.cancel();
                stopMock();
                stopSelf();
                return;
            }
            latitude += latitudeMockedDistance;
            longitude += longitudeMockedDistance;
        }
    }

    public static class MockedBinder extends Binder {
        @NonNull
        private final MockedLocationService service;
        @NonNull
        public final LiveData<MockState> mockState;
        @NonNull
        public final LiveData<Location> mockedLocation;

        public MockedBinder(@NonNull MockedLocationService service) {
            this.service = service;
            this.mockState = service.mockState;
            this.mockedLocation = service.mockedLocation;
        }

        public void continueMock() {
            service.indicateBinding();
        }

        public void startMock(double longitude, double latitude, double longitudeDistance, double latitudeDistance, long mockMilli, int maxTimes, float mockSpeed) {
            service.startMockedService(longitude, latitude, longitudeDistance, latitudeDistance, mockMilli, maxTimes, mockSpeed);
        }

        public boolean isMocking() {
            return MockedLocationService.isMocking();
        }
    }

}
