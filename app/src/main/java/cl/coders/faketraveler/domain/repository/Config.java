package cl.coders.faketraveler.domain.repository;

public interface Config {
    Integer getVersion();

    String getMapProvider();

    double getLongitude(double defaultValue);

    double getLatitude(double defaultValue);

    double getDeltaLongitude();

    double getDeltaLatitude();

    int getMockedCount();

    int getMockFrequency();

    long getEndTime();
    double geZoom();

    ConfigEdit edit();

    interface ConfigEdit {
        ConfigEdit setVersion(int version);

        ConfigEdit setMapProvider(String provider);

        ConfigEdit setEndTime(long endTime);

        ConfigEdit setMockFrequency(int mockFrequency);

        ConfigEdit setMockedCount(int mockedCount);

        ConfigEdit setLatitude(double latitude);

        ConfigEdit setLongitude(double longitude);

        ConfigEdit setDeltaLatitude(double latitude);

        ConfigEdit setDeltaLongitude(double longitude);
        ConfigEdit setZoom(double zoom);

        void save();
    }
}
