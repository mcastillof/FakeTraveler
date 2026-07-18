package cl.coders.faketraveler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;/**
 * Unit tests for {@link KmzFile}.
 *
 * <p>The test loads a real KMZ file bundled under
 * {@code app/src/test/resources}, uses its byte length as the expected size,
 * and mocks the Android {@link ContentResolver} to report that size for
 * {@link OpenableColumns#SIZE}. It then verifies that
 * {@link KmzFile#getSize()} returns the same value.</p>
 */
public class KmzFileTest {

    /** KMZ file bundled under {@code app/src/test/resources}. */
    private static final String EXAMPLE_KMZ_RESOURCE = "/Zavicaj-Mandarina.kmz";

    private Context context;
    private ContentResolver resolver;
    private Uri uri;
    private long exampleKmzSize;

    @Before
    public void setUp() throws Exception {
        exampleKmzSize = loadExampleKmzSize();

        context = mock(Context.class);
        Context appContext = mock(Context.class);
        resolver = mock(ContentResolver.class);
        uri = mock(Uri.class);

        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getContentResolver()).thenReturn(resolver);
    }

    @Test
    public void getSize_returnsSizeReportedByContentResolver() {
        assertTrue("Example KMZ must be non-empty", exampleKmzSize > 0);

        Cursor cursor = mock(Cursor.class);
        when(resolver.query(any(), any(), any(), any(), any())).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(0);
        when(cursor.isNull(0)).thenReturn(false);
        when(cursor.getLong(0)).thenReturn(exampleKmzSize);

        KmzFile kmz = new KmzFile(context, uri);

        assertEquals(exampleKmzSize, kmz.getSize());
    }

    @Test
    public void getSize_returnsMinusOneWhenQueryFails() {
        when(resolver.query(any(), any(), any(), any(), any())).thenReturn(null);

        KmzFile kmz = new KmzFile(context, uri);

        assertEquals(-1, kmz.getSize());
    }

    @Test
    public void getSize_returnsMinusOneWhenSizeColumnMissing() {
        Cursor cursor = mock(Cursor.class);
        when(resolver.query(any(), any(), any(), any(), any())).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(-1);

        KmzFile kmz = new KmzFile(context, uri);

        assertEquals(-1, kmz.getSize());
    }

    @Test
    public void getPlacemarkName_returnsNameFromKml() throws Exception {
        // Serve the real KMZ bytes when the class opens the URI.
        when(resolver.openInputStream(any())).thenAnswer(
                inv -> getClass().getResourceAsStream(EXAMPLE_KMZ_RESOURCE));

        KmzFile kmz = new KmzFile(context, uri);

        assertEquals("Zavi\u010daj - Mandarina", kmz.getPlacemarkName());
    }

    @Test
    public void getPlacemarkName_returnsNullWhenStreamCannotBeOpened() throws Exception {
        when(resolver.openInputStream(any())).thenReturn(null);

        KmzFile kmz = new KmzFile(context, uri);

        assertNull(kmz.getPlacemarkName());
    }

    @Test
    public void getLinePoints_returnsCoordinatesFromLineString() throws Exception {
        // Serve the real KMZ bytes each time the class opens the URI.
        when(resolver.openInputStream(any())).thenAnswer(
                inv -> getClass().getResourceAsStream(EXAMPLE_KMZ_RESOURCE));

        KmzFile kmz = new KmzFile(context, uri);
        List<KmzFile.Point> points = kmz.getLinePoints();

        assertNotNull(points);
        assertFalse("Expected at least one point", points.isEmpty());

        // First point in the LineString of the example file.
        KmzFile.Point first = points.get(0);
        assertEquals(20.460511, first.lon(), 1e-9);
        assertEquals(44.793898, first.lat(), 1e-9);
        assertEquals(0d, first.alt(), 1e-9);

        // A known point from the middle of the LineString snippet.
        assertTrue("Expected a specific mid-line point to be present",
                points.contains(new KmzFile.Point(20.467617, 44.790755, 0d)));

        // Every point should have plausible WGS84 lon/lat values.
        for (KmzFile.Point p : points) {
            assertTrue("lon out of range: " + p.lon(), p.lon() >= -180 && p.lon() <= 180);
            assertTrue("lat out of range: " + p.lat(), p.lat() >= -90 && p.lat() <= 90);
        }
    }

    @Test
    public void getLinePoints_returnsEmptyWhenStreamCannotBeOpened() throws Exception {
        when(resolver.openInputStream(any())).thenReturn(null);

        KmzFile kmz = new KmzFile(context, uri);

        assertTrue(kmz.getLinePoints().isEmpty());
    }

    @Test
    public void sampleAlongLine_walksStraightLineAtGivenSpeed() {
        // 1 km east along the equator, using the same Earth radius that
        // KmzFile uses for the haversine distance.
        final double kmPerDegreeAtEquator = Math.toRadians(1) * KmzFile.EARTH_RADIUS_KM;
        final double dLon = 1d / kmPerDegreeAtEquator;
        List<KmzFile.Point> line = java.util.Arrays.asList(
                new KmzFile.Point(0d, 0d, 0d),
                new KmzFile.Point(dLon, 0d, 0d));

        // 60 km/h, sampling every 60 s → 1 km per step → straight to endpoint.
        // First point is always emitted at t=0.
        List<KmzFile.Point> samples = KmzFile.sampleAlongLine(line, 60d, 60d);

        assertEquals(2, samples.size());
        assertEquals(0d, samples.get(0).lon(), 1e-9);
        assertEquals(0d, samples.get(0).lat(), 1e-9);
        assertEquals(dLon, samples.get(1).lon(), 1e-9);
        assertEquals(0d, samples.get(1).lat(), 1e-9);
    }

    @Test
    public void sampleAlongLine_producesRegularlySpacedInterpolatedPoints() {
        // Straight ~10 km segment along the equator, using the same Earth
        // radius that KmzFile uses for the haversine distance.
        final double kmPerDegreeAtEquator = Math.toRadians(1) * KmzFile.EARTH_RADIUS_KM;
        final double totalKm = 10d;
        final double oneKmDeg = 1d / kmPerDegreeAtEquator;
        final double dLon = totalKm * oneKmDeg;
        List<KmzFile.Point> line = java.util.Arrays.asList(
                new KmzFile.Point(0d, 0d, 0d),
                new KmzFile.Point(dLon, 0d, 0d));

        // 60 km/h, every 60 s → 1 km per step → expect first + 10 more = 11 points.
        List<KmzFile.Point> samples = KmzFile.sampleAlongLine(line, 60d, 60d);
        assertEquals(11, samples.size());

        // Consecutive samples must be spaced ~1 km apart in longitude.
        for (int i = 1; i < samples.size(); i++) {
            double step = samples.get(i).lon() - samples.get(i - 1).lon();
            assertEquals(oneKmDeg, step, 1e-9);
        }
        // Last point is exactly the endpoint.
        assertEquals(dLon, samples.get(samples.size() - 1).lon(), 1e-9);
    }

    @Test
    public void sampleAlongLine_returnsFirstPointForZeroSpeedOrInterval() {
        List<KmzFile.Point> line = java.util.Arrays.asList(
                new KmzFile.Point(1d, 2d, 0d),
                new KmzFile.Point(3d, 4d, 0d));

        assertEquals(1, KmzFile.sampleAlongLine(line, 0d, 60d).size());
        assertEquals(1, KmzFile.sampleAlongLine(line, 60d, 0d).size());
        assertEquals(line.get(0), KmzFile.sampleAlongLine(line, 0d, 0d).get(0));
    }

    @Test
    public void sampleAlongLine_returnsEmptyForEmptyLine() {
        assertTrue(KmzFile.sampleAlongLine(java.util.Collections.emptyList(), 60d, 60d).isEmpty());
    }

    @Test
    public void bearingDegrees_cardinalDirections() {
        KmzFile.Point origin = new KmzFile.Point(0d, 0d, 0d);
        assertEquals(0f,   KmzFile.bearingDegrees(origin, new KmzFile.Point(0d,  1d, 0d)), 1e-3f); // N
        assertEquals(90f,  KmzFile.bearingDegrees(origin, new KmzFile.Point(1d,  0d, 0d)), 1e-3f); // E
        assertEquals(180f, KmzFile.bearingDegrees(origin, new KmzFile.Point(0d, -1d, 0d)), 1e-3f); // S
        assertEquals(270f, KmzFile.bearingDegrees(origin, new KmzFile.Point(-1d, 0d, 0d)), 1e-3f); // W
    }

    @Test
    public void bearingDegrees_returnsZeroForSamePoint() {
        KmzFile.Point p = new KmzFile.Point(10d, 20d, 0d);
        assertEquals(0f, KmzFile.bearingDegrees(p, p), 0f);
    }

    @Test
    public void samplePoints_producesFirstAndLastPointsOfKmzLineString() throws Exception {
        when(resolver.openInputStream(any())).thenAnswer(
                inv -> getClass().getResourceAsStream(EXAMPLE_KMZ_RESOURCE));

        KmzFile kmz = new KmzFile(context, uri);
        List<KmzFile.Point> line = kmz.getLinePoints();
        List<KmzFile.Point> samples = kmz.samplePoints(60d, 10d);

        assertFalse(samples.isEmpty());
        // Always starts at the first LineString point.
        assertEquals(line.get(0).lon(), samples.get(0).lon(), 1e-9);
        assertEquals(line.get(0).lat(), samples.get(0).lat(), 1e-9);
        // Always ends at the last LineString point.
        KmzFile.Point lastLine = line.get(line.size() - 1);
        KmzFile.Point lastSample = samples.get(samples.size() - 1);
        assertEquals(lastLine.lon(), lastSample.lon(), 1e-9);
        assertEquals(lastLine.lat(), lastSample.lat(), 1e-9);
    }

    /**
     * Loads the bundled example KMZ from the test classpath and returns its
     * length in bytes.
     */
    private long loadExampleKmzSize() throws Exception {
        URL resource = getClass().getResource(EXAMPLE_KMZ_RESOURCE);
        assertNotNull("Missing test resource: " + EXAMPLE_KMZ_RESOURCE, resource);
        // Prefer a direct filesystem read when possible, so we get the exact
        // on-disk byte length without buffering the whole file in memory.
        if ("file".equals(resource.getProtocol())) {
            return Files.size(Paths.get(resource.toURI()));
        }
        try (InputStream in = resource.openStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.size();
        }
    }
}









