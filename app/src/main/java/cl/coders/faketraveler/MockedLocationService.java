package cl.coders.faketraveler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
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

    /** Notification channel & id used to keep the service in the foreground. */
    @NonNull
    private static final String CHANNEL_ID = "cl.coders.faketraveler.mocking";
    private static final int NOTIFICATION_ID = 1001;

    @NonNull
    protected final MutableLiveData<MockState> mockState = new MutableLiveData<>();
    @NonNull
    protected final MutableLiveData<Location> mockedLocation = new MutableLiveData<>();

    @NonNull
    private final List<MockedLocationProvider> providers = new ArrayList<>();

    @NonNull
    private final Timer timer = new Timer();
    @NonNull
    private final Set<TimerTask> tasks = Collections.synchronizedSet(new HashSet<>());

    @NonNull
    @Override
    public IBinder onBind(Intent intent) {
        // Promote to foreground immediately: if the activity is destroyed
        // while mocking is running, Android would otherwise stop us.
        enterForeground();
        indicateBinding();
        return new MockedBinder(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Required within a few seconds when started via startForegroundService(...).
        enterForeground();
        return START_NOT_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbind (mocking continues if the service was also started).");
        // Do NOT tear down here: if the activity unbinds because it's going
        // to the background, we want the mock to keep running. Teardown
        // happens in onDestroy(), which fires when stopService(...) is
        // called (Stop button) or after the last tick.
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed; stopping all mocks.");
        for (TimerTask t : tasks) t.cancel();
        tasks.clear();
        timer.cancel();
        for (MockedLocationProvider prov : providers) prov.shutdown();
        providers.clear();
        mockState.postValue(MockState.NOT_MOCKED);
        super.onDestroy();
    }

    private void enterForeground() {
        try {
            ensureNotificationChannel();
            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to enter foreground", t);
        }
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.MockingService_ChannelName),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.MockingService_ChannelDesc));
        channel.setShowBadge(false);
        nm.createNotificationChannel(channel);
    }

    @NonNull
    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.MockingService_Title))
                .setContentText(getString(R.string.MockingService_Text))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pi)
                .build();
    }

    private void indicateBinding() {
        mockState.postValue(MockState.SERVICE_BOUND);
    }

    protected void startMockedService(double longitude, double latitude, double longitudeDistance, double latitudeDistance, long mockMilli, int maxTime, float mockSpeed) {
        try {
            createProviders();

            MockedTask mockedTask = new MockedTask(longitude, latitude, longitudeDistance, latitudeDistance, maxTime, mockSpeed);
            timer.schedule(mockedTask, 0L, mockMilli);
            tasks.add(mockedTask);
            mockState.postValue(MockState.MOCKED);
        } catch (SecurityException e) {
            Log.e(TAG, "Could not construct mock location providers!", e);
            mockState.postValue(MockState.MOCK_ERROR);
        }
    }

    /**
     * Publishes a sequence of pre-computed points along a route, one per
     * {@code mockMilli} tick. Used for File-mode mocking (KMZ LineString).
     */
    protected void startMockedRoute(@NonNull List<KmzFile.Point> points, long mockMilli, float mockSpeed) {
        if (points.isEmpty()) {
            mockState.postValue(MockState.MOCK_ERROR);
            return;
        }
        try {
            createProviders();

            RouteTask routeTask = new RouteTask(points, mockSpeed);
            timer.schedule(routeTask, 0L, mockMilli);
            tasks.add(routeTask);
            mockState.postValue(MockState.MOCKED);
        } catch (SecurityException e) {
            Log.e(TAG, "Could not construct mock location providers!", e);
            mockState.postValue(MockState.MOCK_ERROR);
        }
    }

    private void createProviders() {
        providers.clear();
        providers.add(new MockedLocationProvider(LocationManager.GPS_PROVIDER, this));
        providers.add(new MockedLocationProvider(LocationManager.NETWORK_PROVIDER, this));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providers.add(new MockedLocationProvider(LocationManager.FUSED_PROVIDER, this));
        }
    }

    private void applyCommonLocationFields(@NonNull Location value, float speed) {
        if (speed > 0) {
            value.setSpeed(speed);
            value.setAccuracy(0.1f);
            value.setTime(System.currentTimeMillis());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                value.setSpeedAccuracyMetersPerSecond(0.01f);
            }
        }
    }

    /**
     * Sets the bearing (compass heading) that navigation apps use to rotate
     * their direction arrow. No-op when {@code fromLat/fromLon} and
     * {@code toLat/toLon} are the same point (no direction of movement).
     */
    private static void applyBearing(@NonNull Location value,
                                     double fromLat, double fromLon,
                                     double toLat, double toLon) {
        if (fromLat == toLat && fromLon == toLon) return;
        float bearing = KmzFile.bearingDegrees(
                new KmzFile.Point(fromLon, fromLat, 0),
                new KmzFile.Point(toLon, toLat, 0));
        value.setBearing(bearing);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            value.setBearingAccuracyDegrees(0.1f);
        }
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
            applyCommonLocationFields(value, speed);
            // Point movement: heading derived from the step deltas.
            applyBearing(value, latitude, longitude,
                    latitude + latitudeMockedDistance, longitude + longitudeMockedDistance);

            mockedLocation.postValue(value);
            for (MockedLocationProvider prov : providers)
                prov.pushLocation(value);
            ++currentTimes;
            if (maxLocationTimes != 0 && maxLocationTimes == currentTimes) {
                this.cancel();
                stopSelf();
                mockState.postValue(MockState.NOT_MOCKED);
            }
            latitude += latitudeMockedDistance;
            longitude += longitudeMockedDistance;
        }
    }

    /**
     * Timer task that walks through a pre-computed list of geographic points,
     * publishing one per tick until the list is exhausted.
     */
    class RouteTask extends TimerTask {
        @NonNull
        private final List<KmzFile.Point> points;
        private final float speed;
        private int index = 0;

        RouteTask(@NonNull List<KmzFile.Point> points, float mockSpeed) {
            this.points = points;
            this.speed = mockSpeed;
        }

        @Override
        public void run() {
            if (index >= points.size()) {
                finish();
                return;
            }
            KmzFile.Point p = points.get(index++);
            Location value = new Location(LocationManager.GPS_PROVIDER);
            value.setLongitude(p.lon());
            value.setLatitude(p.lat());
            applyCommonLocationFields(value, speed);
            // Heading toward the next route point; on the final point reuse
            // the previous heading (going backwards to the point we came from
            // would point the arrow the wrong way).
            if (index < points.size()) {
                KmzFile.Point next = points.get(index);
                applyBearing(value, p.lat(), p.lon(), next.lat(), next.lon());
            } else if (index >= 2) {
                KmzFile.Point prev = points.get(index - 2);
                applyBearing(value, prev.lat(), prev.lon(), p.lat(), p.lon());
            }

            mockedLocation.postValue(value);
            for (MockedLocationProvider prov : providers)
                prov.pushLocation(value);

            if (index >= points.size()) finish();
        }

        private void finish() {
            this.cancel();
            stopSelf();
            mockState.postValue(MockState.NOT_MOCKED);
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

        public void startRoute(@NonNull List<KmzFile.Point> points, long mockMilli, float mockSpeed) {
            service.startMockedRoute(points, mockMilli, mockSpeed);
        }
    }

}
