package cl.coders.faketraveler;

import java.util.Locale;

public final class MapProviderUtil {

    private MapProviderUtil() {
        throw new UnsupportedOperationException();
    }

    public static String getDefaultMapProvider(Locale locale) {
        String lang = locale.getLanguage();
        if (lang.equals("de")) return "OpenStreetMap.DE";
        if (lang.equals("fr")) return "OpenStreetMap.France";
        return "OpenStreetMap";
    }

}
