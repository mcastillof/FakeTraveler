package cl.coders.faketraveler;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Represents a KMZ file selected by the user through the Storage Access Framework.
 * Wraps a content {@link Uri} and exposes convenience methods to query metadata
 * such as the file size.
 */
public class KmzFile {

    @NonNull
    private static final String TAG = KmzFile.class.getSimpleName();

    /**
     * Mean Earth radius in kilometres (IUGG). Used by the haversine distance
     * calculation in {@link #distanceKm(Point, Point)} and exposed
     * package-private so tests can derive expected geographic distances from
     * the exact same value the production code uses.
     */
    static final double EARTH_RADIUS_KM = 6371.0088;

    /** MIME types accepted when launching a document picker for KMZ files. */
    @NonNull
    public static final String[] MIME_TYPES = {
            "application/vnd.google-earth.kmz",
            "application/octet-stream",
            "*/*"
    };

    @NonNull
    private final Context context;
    @NonNull
    private final Uri uri;

    public KmzFile(@NonNull Context context, @NonNull Uri uri) {
        this.context = context.getApplicationContext();
        this.uri = uri;
    }

    /**
     * Opens the KMZ file previously granted via the Storage Access Framework, or
     * {@code null} if no file has been persisted yet.
     */
    @Nullable
    public static KmzFile openPersisted(@NonNull Context context, @Nullable String uriString) {
        if (uriString == null) return null;
        try {
            return new KmzFile(context, Uri.parse(uriString));
        } catch (Throwable t) {
            Log.e(TAG, "Could not parse persisted KMZ URI: " + uriString, t);
            return null;
        }
    }

    /**
     * Persists read permission on this file across process restarts.
     */
    public void takePersistablePermission() {
        try {
            context.getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable t) {
            Log.w(TAG, "Could not persist URI permission for " + uri, t);
        }
    }

    @NonNull
    public Uri getUri() {
        return uri;
    }

    /**
     * @return the file size in bytes, or {@code -1} if it cannot be determined.
     */
    public long getSize() {
        ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    return cursor.getLong(sizeIdx);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Could not read KMZ file size for " + uri, t);
        }
        return -1;
    }

    /**
     * Reads the first {@code <name>} element found under
     * {@code kml > Document > Placemark} in the KML entry of the KMZ archive.
     *
     * @return the placemark name, or {@code null} if none could be read
     *         (missing entry, malformed XML, no matching element, etc.).
     */
    @Nullable
    public String getPlacemarkName() {
        return parseKmlEntry(new NameHandler());
    }

    /**
     * Reads all points from the first {@code <LineString><coordinates>} found
     * under {@code kml > Document > Placemark} in the KML entry of the KMZ
     * archive.
     *
     * <p>KML coordinate tuples are {@code longitude,latitude[,altitude]},
     * whitespace-separated.</p>
     *
     * @return an immutable list of points; empty when nothing could be read
     *         (missing entry, malformed XML, no matching element, etc.).
     */
    @NonNull
    public List<Point> getLinePoints() {
        String raw = parseKmlEntry(new CoordinatesHandler());
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        List<Point> points = new ArrayList<>();
        for (String tuple : raw.trim().split("\\s+")) {
            if (tuple.isEmpty()) continue;
            String[] parts = tuple.split(",");
            if (parts.length < 2) continue;
            try {
                double lon = Double.parseDouble(parts[0]);
                double lat = Double.parseDouble(parts[1]);
                double alt = parts.length >= 3 ? Double.parseDouble(parts[2]) : 0d;
                points.add(new Point(lon, lat, alt));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Skipping malformed KML coordinate: " + tuple, e);
            }
        }
        return Collections.unmodifiableList(points);
    }

    /**
     * A single point extracted from a KML {@code <coordinates>} tuple.
     *
     * <p>KML uses {@code longitude,latitude[,altitude]} ordering.</p>
     */
    public record Point(double lon, double lat, double alt) {}

    /**
     * Samples points along this KMZ's LineString as if a vehicle were driving
     * at {@code speedKmh} kilometres per hour, emitting one position every
     * {@code intervalSeconds} seconds.
     *
     * <p>The returned list always starts with the first point of the
     * LineString (time = 0) and ends with the last point of the LineString
     * (arrival), with intermediate positions interpolated linearly along the
     * polyline segments.</p>
     *
     * @return an immutable list of sampled points; empty when the LineString
     *         cannot be read.
     */
    @NonNull
    public List<Point> samplePoints(double speedKmh, double intervalSeconds) {
        return sampleAlongLine(getLinePoints(), speedKmh, intervalSeconds);
    }

    /**
     * Package-private helper that performs the sampling on a supplied
     * polyline. Extracted for direct unit testing.
     */
    @NonNull
    static List<Point> sampleAlongLine(@NonNull List<Point> line,
                                       double speedKmh,
                                       double intervalSeconds) {
        if (line.isEmpty()) return Collections.emptyList();
        if (line.size() == 1 || speedKmh <= 0 || intervalSeconds <= 0) {
            return Collections.singletonList(line.get(0));
        }
        final double stepKm = speedKmh * intervalSeconds / 3600.0;
        if (stepKm <= 0) return Collections.singletonList(line.get(0));

        List<Point> result = new ArrayList<>();
        result.add(line.get(0));

        int segIdx = 0;                                       // current segment start
        double segOffset = 0;                                 // distance already consumed within current segment
        double segLen = distanceKm(line.get(0), line.get(1)); // length of current segment

        while (true) {
            double remaining = stepKm;
            // Advance across segments until we've walked `remaining` km.
            // Using >= collapses the exact-boundary case (step lands on a
            // vertex, or on the very end of the line) so we don't emit the
            // final vertex twice on the next outer iteration.
            while (remaining >= segLen - segOffset) {
                remaining -= (segLen - segOffset);
                segIdx++;
                segOffset = 0;
                if (segIdx >= line.size() - 1) {
                    result.add(line.get(line.size() - 1));
                    return Collections.unmodifiableList(result);
                }
                segLen = distanceKm(line.get(segIdx), line.get(segIdx + 1));
            }
            segOffset += remaining;
            double t = segLen > 0 ? segOffset / segLen : 0d;
            result.add(interpolate(line.get(segIdx), line.get(segIdx + 1), t));
        }
    }

    /** Great-circle distance between two points in kilometres (haversine). */
    private static double distanceKm(@NonNull Point a, @NonNull Point b) {
        double lat1 = Math.toRadians(a.lat());
        double lat2 = Math.toRadians(b.lat());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.lon() - a.lon());
        double s = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1d, Math.sqrt(s)));
    }

    /**
     * Initial bearing (forward azimuth) from {@code a} to {@code b}, in
     * degrees clockwise from true north, in the range {@code [0, 360)}.
     * Returns {@code 0} if the two points are identical.
     */
    public static float bearingDegrees(@NonNull Point a, @NonNull Point b) {
        double lat1 = Math.toRadians(a.lat());
        double lat2 = Math.toRadians(b.lat());
        double dLon = Math.toRadians(b.lon() - a.lon());
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        if (y == 0 && x == 0) return 0f;
        double deg = Math.toDegrees(Math.atan2(y, x));
        return (float) ((deg + 360d) % 360d);
    }

    /** Linear interpolation between two points, {@code t} in {@code [0, 1]}. */
    @NonNull
    private static Point interpolate(@NonNull Point a, @NonNull Point b, double t) {
        return new Point(
                a.lon() + (b.lon() - a.lon()) * t,
                a.lat() + (b.lat() - a.lat()) * t,
                a.alt() + (b.alt() - a.alt()) * t);
    }

    /**
     * Opens the first {@code .kml} entry in the archive and feeds it to the
     * given handler. Returns the handler's captured result, or {@code null}
     * on any error.
     */
    @Nullable
    private <H extends DefaultHandler & ResultProvider> String parseKmlEntry(@NonNull H handler) {
        ContentResolver resolver = context.getContentResolver();
        try (InputStream raw = resolver.openInputStream(uri)) {
            if (raw == null) {
                Log.w(TAG, "Could not open KMZ stream for " + uri);
                return null;
            }
            try (ZipInputStream zis = new ZipInputStream(raw)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();
                    if (name != null && name.toLowerCase().endsWith(".kml")) {
                        try {
                            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                            parser.parse(new InputSource(new NonClosingInputStream(zis)), handler);
                        } catch (StopParsingException stop) {
                            // Handler signalled it captured the desired value.
                        }
                        return handler.getResult();
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Could not parse KML entry from " + uri, t);
        }
        return null;
    }

    /** Marks a SAX handler that exposes a single captured string result. */
    private interface ResultProvider {
        @Nullable String getResult();
    }

    /**
     * SAX handler that captures the first {@code <name>} text found within the
     * path {@code kml/Document/Placemark}. Element matching is
     * case-insensitive and ignores XML namespaces.
     */
    private static final class NameHandler extends PathTextHandler {
        NameHandler() { super("kml", "document", "placemark", "name"); }
    }

    /**
     * SAX handler that captures the first {@code <coordinates>} text found
     * within {@code kml/Document/Placemark/LineString}.
     */
    private static final class CoordinatesHandler extends PathTextHandler {
        CoordinatesHandler() { super("kml", "document", "placemark", "linestring", "coordinates"); }
    }

    /**
     * Base SAX handler that captures the concatenated text content of the
     * first element that matches a fixed lower-cased element path (namespace
     * agnostic). Parsing is short-circuited via {@link StopParsingException}
     * once the value is captured.
     */
    private static class PathTextHandler extends DefaultHandler implements ResultProvider {
        @NonNull private final String[] path;
        @NonNull private final Deque<String> stack = new ArrayDeque<>();
        @Nullable private StringBuilder currentText;
        @Nullable private String result;

        PathTextHandler(@NonNull String... path) { this.path = path; }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            stack.push(pickLocalName(localName, qName).toLowerCase());
            if (result == null && matchesPath()) {
                currentText = new StringBuilder();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (currentText != null) currentText.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (currentText != null) {
                result = currentText.toString().trim();
                currentText = null;
                if (!stack.isEmpty()) stack.pop();
                throw new StopParsingException();
            }
            if (!stack.isEmpty()) stack.pop();
        }

        @Nullable
        @Override
        public String getResult() { return result; }

        private boolean matchesPath() {
            if (stack.size() != path.length) return false;
            int i = path.length - 1;
            for (String tag : stack) {
                if (!path[i--].equals(tag)) return false;
            }
            return true;
        }

        @NonNull
        private static String pickLocalName(@Nullable String localName, @Nullable String qName) {
            if (localName != null && !localName.isEmpty()) return localName;
            if (qName == null) return "";
            int colon = qName.indexOf(':');
            return colon >= 0 ? qName.substring(colon + 1) : qName;
        }
    }

    /** Thrown to short-circuit SAX parsing once the desired value has been found. */
    private static final class StopParsingException extends SAXException {}

    /** Wrapper that prevents the SAX parser from closing the underlying stream. */
    private static final class NonClosingInputStream extends FilterInputStream {
        NonClosingInputStream(@NonNull InputStream in) { super(in); }
        @Override public void close() throws IOException { /* no-op */ }
    }
}





