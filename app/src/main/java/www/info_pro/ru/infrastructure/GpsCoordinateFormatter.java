package www.info_pro.ru.infrastructure;

import android.location.Location;

import java.util.Locale;

/*Описывает способы форматирования местоположения*/
public class GpsCoordinateFormatter {
    public String toInDegree(Location location) {
        if (location == null) {
            return null;
        }
        return toInDegree(location.getLatitude(), location.getLongitude());
    }

    private String toInDegree(double latitude, double longitude) {
        try {
            int latSeconds = (int) Math.round(latitude * 3600);
            int latDegrees = latSeconds / 3600;
            latSeconds = Math.abs(latSeconds % 3600);
            int latMinutes = latSeconds / 60;
            latSeconds %= 60;

            int longSeconds = (int) Math.round(longitude * 3600);
            int longDegrees = longSeconds / 3600;
            longSeconds = Math.abs(longSeconds % 3600);
            int longMinutes = longSeconds / 60;
            longSeconds %= 60;
            String latDegree = latDegrees >= 0 ? "N" : "S";
            String lonDegrees = longDegrees >= 0 ? "E" : "W";

            return latDegree + " " + Math.abs(latDegrees) + "°" + latMinutes + "'" + latSeconds
                    + "\" / " + lonDegrees + " " + Math.abs(longDegrees) + "°" + longMinutes
                    + "'" + longSeconds + "\"";

        } catch (Exception e) {
            Locale def = Locale.getDefault();
            return String.format(def, "%8.5f", latitude) + "  " + String.format(def, "%8.5f", longitude);
        }
    }
}
