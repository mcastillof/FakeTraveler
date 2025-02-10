package cl.coders.faketraveler;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;

public class MockLocationProvider {
    final String providerName;
    final Context ctx;

    /**
     * Class constructor
     *
     * @param name provider
     * @param ctx  context
     * @return Void
     */
    public MockLocationProvider(String name, Context ctx) {
        this.providerName = name;
        this.ctx = ctx;

        int powerUsage = 0;
        int accuracy = 5;

        if (Build.VERSION.SDK_INT >= 30) {
            powerUsage = 1;
            accuracy = 2;
        }

        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        startup(lm, powerUsage, accuracy, /* maxRetryCount= */ 3, /* currentRetryCount= */ 0);
    }

    private void startup(LocationManager lm, int powerUsage, int accuracy, int maxRetryCount, int currentRetryCount) {
        if (currentRetryCount < maxRetryCount) {
            try {
                shutdown();
                lm.addTestProvider(providerName, false, false, false, false, false, true, true, powerUsage, accuracy);
                lm.setTestProviderEnabled(providerName, true);
            } catch (Exception e) {
                startup(lm, powerUsage, accuracy, maxRetryCount, (currentRetryCount + 1));
            }
        } else {
            throw new SecurityException("Not allowed to perform MOCK_LOCATION");
        }
    }

    /**
     * Pushes the location in the system (mock). This is where the magic gets done.
     *
     * @param lat latitude
     * @param lon longitude
     * @return Void
     */
    public void pushLocation(double lat, double lon) {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

        Location mockLocation = new Location(providerName);
        mockLocation.setLatitude(lat);
        mockLocation.setLongitude(lon);
        mockLocation.setAltitude(3F);
        mockLocation.setTime(System.currentTimeMillis());
        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        mockLocation.setSpeed(0.01F);
        mockLocation.setBearing(1F);
        mockLocation.setAccuracy(3F);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mockLocation.setBearingAccuracyDegrees(0.1F);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mockLocation.setVerticalAccuracyMeters(0.1F);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mockLocation.setSpeedAccuracyMetersPerSecond(0.01F);
        }
        lm.setTestProviderLocation(providerName, mockLocation);
    }

    /**
     * Removes the provider
     *
     * @return Void
     */
    public void shutdown() {
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            lm.removeTestProvider(providerName);
        } catch (Exception e) {
        }
    }
}
