package cl.coders.faketraveler;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;

public class MockLocationProvider {
    String providerName;
    Context ctx;

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

        LocationManager lm = (LocationManager) ctx.getSystemService(
                Context.LOCATION_SERVICE);
        lm.addTestProvider(providerName, false, false, false, false, false,
                true, true, 0, 5);
        lm.setTestProviderEnabled(providerName, true);
    }

    /**
     * Pushes the location in the system (mock). This is where the magic gets done.
     *
     * @param lat latitude
     * @param lon longitude
     * @return Void
     */
    public void pushLocation(double lat, double lon) {
        LocationManager lm = (LocationManager) ctx.getSystemService(
                Context.LOCATION_SERVICE);

        Location mockLocation = new Location(providerName);
        mockLocation.setLatitude(lat);
        mockLocation.setLongitude(lon);
        mockLocation.setAltitude(3F);
        mockLocation.setTime(System.currentTimeMillis());
        //mockLocation.setAccuracy(16F);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        lm.setTestProviderLocation(providerName, mockLocation);
    }

    /**
     * Removes the provider
     *
     * @return Void
     */
    public void shutdown() {
        LocationManager lm = (LocationManager) ctx.getSystemService(
                Context.LOCATION_SERVICE);
        lm.removeTestProvider(providerName);
    }
}
