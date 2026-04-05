package cl.coders.faketraveler;

import android.net.Uri;

import androidx.annotation.Nullable;

public record GeoUri(double lat, double lng, @Nullable Double zoom) {

    @Nullable
    public static GeoUri parse(String geoUri) {
        Uri uri = Uri.parse(geoUri);
        if (!"geo".equals(uri.getScheme())) return null;

        String[] split = uri.getSchemeSpecificPart().split("\\?", 2);
        if (split.length < 1) return null;

        String[] latLng = split[0].split(",", 2);
        if (latLng.length < 2) return null;

        double lat = Double.parseDouble(latLng[0]);
        double lng = Double.parseDouble(latLng[1]);

        // Try to parse query parameters (if existent)
        if (split.length < 2) return new GeoUri(lat, lng, null);
        Double zoom = null;
        String[] queryParams = split[1].split("&");
        for (String q : queryParams) {
            if (q.startsWith("z=")) {
                zoom = Double.parseDouble(q.replaceFirst("z=", ""));
            }
        }
        return new GeoUri(lat, lng, zoom);
    }

}
