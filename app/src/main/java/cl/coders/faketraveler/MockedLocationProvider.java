package cl.coders.faketraveler;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

public class MockedLocationProvider {

    @NonNull
    private static final String TAG = MockedLocationProvider.class.getSimpleName();

    private static final int MAX_RETRY_COUNT = 3;

    @NonNull
    private final String providerName;
    @NonNull
    private final Context ctx;

    /**
     * Class constructor
     *
     * @param name provider
     * @param ctx  context
     */
    public MockedLocationProvider(@NonNull String name, @NonNull Context ctx) {
        this.providerName = name;
        this.ctx = ctx;

        int powerUsage = 0;
        int accuracy = 5;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            powerUsage = 1;
            accuracy = 2;
        }

        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        startup(lm, powerUsage, accuracy);
    }

    private void startup(@NonNull LocationManager lm, int powerUsage, int accuracy) {
        startup(lm, powerUsage, accuracy, 0);
    }

    private void startup(@NonNull LocationManager lm, int powerUsage, int accuracy, int currentRetryCount) {
        if (currentRetryCount < MAX_RETRY_COUNT) {
            try {
                shutdown();
                lm.addTestProvider(providerName, false, false, false, false, false, true, true, powerUsage, accuracy);
                lm.setTestProviderEnabled(providerName, true);
            } catch (Throwable t) {
                Log.e(TAG, "startup: ", t);
                startup(lm, powerUsage, accuracy, currentRetryCount + 1);
            }
        } else {
            throw new SecurityException("Not allowed to perform MOCK_LOCATION");
        }
    }

    /**
     * Pushes a mocked location into the system through this test provider.
     *
     * <p>The bearing and speed from {@code source} are forwarded so
     * navigation apps can display the correct movement direction and speed.
     * Missing fields are populated with sensible defaults.</p>
     */
    public void pushLocation(@NonNull Location source) {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

        Location mockLocation = new Location(providerName);
        mockLocation.setLatitude(source.getLatitude());
        mockLocation.setLongitude(source.getLongitude());
        mockLocation.setAltitude(3F);
        mockLocation.setTime(System.currentTimeMillis());
        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        mockLocation.setSpeed(source.hasSpeed() ? source.getSpeed() : 0.01F);
        // A bearing of 0 is a legitimate value (due north), so only skip when
        // the caller explicitly hasn't set one.
        mockLocation.setBearing(source.hasBearing() ? source.getBearing() : 0F);
        mockLocation.setAccuracy(3F);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mockLocation.setBearingAccuracyDegrees(
                    source.hasBearingAccuracy() ? source.getBearingAccuracyDegrees() : 0.1F);
            mockLocation.setVerticalAccuracyMeters(0.1F);
            mockLocation.setSpeedAccuracyMetersPerSecond(
                    source.hasSpeedAccuracy() ? source.getSpeedAccuracyMetersPerSecond() : 0.01F);
        }
        Log.d(TAG, "pushLocation: " + source.getLatitude() + ", " + source.getLongitude()
                + " bearing=" + mockLocation.getBearing() + " speed=" + mockLocation.getSpeed());
        lm.setTestProviderLocation(providerName, mockLocation);
    }

    /**
     * Removes the provider.
     */
    public void shutdown() {
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            lm.removeTestProvider(providerName);
        } catch (Throwable ignored) {
        }
    }

}
