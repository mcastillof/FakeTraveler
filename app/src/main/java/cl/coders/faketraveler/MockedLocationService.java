package cl.coders.faketraveler;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Timer;
import java.util.TimerTask;

public class MockedLocationService extends Service {

    private static final String TAG = MockedLocationService.class.getSimpleName();

    protected final MutableLiveData<MockedState> mockedState = new MutableLiveData<>();
    protected final MutableLiveData<Location> mockedLocation = new MutableLiveData<>();

    private final Timer timer = new Timer();

    private MockLocationProvider gpsProvider;
    private MockLocationProvider networkProvider;
    private MockLocationProvider fusedProvider;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        MockedBinder binder = new MockedBinder(this);
        try {
            gpsProvider = new MockLocationProvider(LocationManager.GPS_PROVIDER, this);
            networkProvider = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                fusedProvider = new MockLocationProvider(LocationManager.FUSED_PROVIDER, this);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Could not construct mock location providers!", e);
            mockedState.setValue(MockedState.MOCKED_ERROR);
            stopSelf();
        }
        mockedState.setValue(MockedState.CAN_MOCKED);
        return binder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Mock is finished");
        timer.cancel();
        gpsProvider.shutdown();
        networkProvider.shutdown();
        if(fusedProvider != null)
            fusedProvider.shutdown();
        mockedState.setValue(MockedState.NO_MOCKED);
        super.onDestroy();
    }

    protected void startMockedService(double longitude, double latitude, double longitudeDistance, double latitudeDistance, long mockMilli, int maxTime) {
        MockedTask mockedTask = new MockedTask(longitude, latitude, longitudeDistance, latitudeDistance, maxTime);
        timer.schedule(mockedTask, 0L, mockMilli);
        mockedState.setValue(MockedState.MOCKED);
    }

    class MockedTask extends TimerTask {
        private double longitude;
        private double latitude;
        private final double longitudeMockedDistance;
        private final double latitudeMockedDistance;
        private final int maxLocationTimes;
        private int currentTimes = 0;

        public MockedTask(double longitude, double latitude, double longitudeMockedDistance, double latitudeMockedDistance, int maxTimes) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.longitudeMockedDistance = longitudeMockedDistance;
            this.latitudeMockedDistance = latitudeMockedDistance;
            this.maxLocationTimes = maxTimes;
        }

        @Override
        public void run() {
            Location value = new Location(LocationManager.GPS_PROVIDER);
            value.setLongitude(longitude);
            value.setLatitude(latitude);
            mockedLocation.postValue(value);
            gpsProvider.pushLocation(latitude, longitude);
            networkProvider.pushLocation(latitude, longitude);
            if(fusedProvider != null)
                fusedProvider.pushLocation(latitude, longitude);
            ++currentTimes;
            if (maxLocationTimes != 0 && maxLocationTimes == currentTimes) {
                this.cancel();
                stopSelf();
            }
            latitude += latitudeMockedDistance;
            longitude += longitudeMockedDistance;
        }
    }

    public static class MockedBinder extends Binder {
        private final MockedLocationService service;
        public final LiveData<MockedState> mockedState;
        public final LiveData<Location> mockedLocation;

        public MockedBinder(MockedLocationService service) {
            this.service = service;
            this.mockedState = service.mockedState;
            this.mockedLocation = service.mockedLocation;
        }

        public void startMocked(double longitude, double latitude, double longitudeDistance, double latitudeDistance, long mockMilli, int maxTimes) {
            service.startMockedService(longitude, latitude, longitudeDistance, latitudeDistance, mockMilli, maxTimes);
        }
    }

}
