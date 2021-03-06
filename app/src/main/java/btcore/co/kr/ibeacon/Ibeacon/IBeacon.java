package btcore.co.kr.ibeacon.Ibeacon;

import android.util.Log;

import java.util.Arrays;
import java.util.Collections;

import btcore.co.kr.ibeacon.KalmanFilter;

/**
 * Created by thenathanjones on 24/01/2014.
 */
public class IBeacon {
    private static String TAG = IBeacon.class.getName();

    public final String uuid;
    public final int major;
    public final int minor;
    public final int txPower;
    public final String hash;

    private long lastReport;
    private double lastRssi;
    private double distanceInMetres;

    private IBeacon(String uuid, int major, int minor, int txPower) {
        this.uuid = uuid;
        this.major = major;
        this.minor = minor;
        this.txPower = txPower;
        this.hash = uuid + ":" + major + ":" + minor;
    }

    protected static boolean isBeacon(byte[] scanRecord) {
        Integer[] headerBytes = new Integer[9];

        for (int i=0;i<headerBytes.length;i++) {
            headerBytes[i] = scanRecord[i] & 0xff;
        }

        return Collections.indexOfSubList(Arrays.asList(headerBytes), IBeaconConstants.IBEACON_HEADER) == IBeaconConstants.IBEACON_HEADER_INDEX;
    }

    protected static IBeacon from(byte[] scanRecord) {
        String uuid = parseUUIDFrom(scanRecord);
        int major = (scanRecord[IBeaconConstants.MAJOR_INDEX] & 0xff) * 0x100 + (scanRecord[IBeaconConstants.MAJOR_INDEX+1] & 0xff);
        int minor = (scanRecord[IBeaconConstants.MINOR_INDEX] & 0xff) * 0x100 + (scanRecord[IBeaconConstants.MINOR_INDEX+1] & 0xff);
        int txPower = (int)scanRecord[IBeaconConstants.TXPOWER_INDEX];

        Log.d(TAG, "UUID: " + uuid + "  Major: " + major + "  Minor: " + minor + "  TxPower: " + txPower);

        return new IBeacon(uuid, major, minor, txPower);
    }

    private static String parseUUIDFrom(byte[] scanRecord) {
        int[] proximityUuidBytes = new int[16];
        char[] proximityUuidChars = new char[proximityUuidBytes.length * 2];

        for (int i=0;i<proximityUuidBytes.length;i++) {
            proximityUuidBytes[i] = scanRecord[i + IBeaconConstants.PROXIMITY_UUID_INDEX] & 0xFF;
            proximityUuidChars[i * 2] = IBeaconConstants.HEX_ARRAY[proximityUuidBytes[i] >>> 4];
            proximityUuidChars[i * 2 + 1] = IBeaconConstants.HEX_ARRAY[proximityUuidBytes[i] & 0x0F];
        }

        String proximityUuidHexString = new String(proximityUuidChars);
        StringBuilder builder = new StringBuilder();
        builder.append(proximityUuidHexString.substring(0, 8));
        builder.append("-");
        builder.append(proximityUuidHexString.substring(8, 12));
        builder.append("-");
        builder.append(proximityUuidHexString.substring(12, 16));
        builder.append("-");
        builder.append(proximityUuidHexString.substring(16, 20));
        builder.append("-");
        builder.append(proximityUuidHexString.substring(20, 32));

        return builder.toString();
    }

    public void calculateDistanceFrom(double rssi, IBeacon existingBeacon) {
        double filteredRssi = KalmanFilter.filter(rssi, hash);

        double distanceInMetres = distanceFrom(filteredRssi, txPower);

        if (existingBeacon != null) {
            distanceInMetres = filteredDistance(distanceInMetres, existingBeacon.distanceInMetres());
        }

        this.distanceInMetres = distanceInMetres;
        this.lastRssi = filteredRssi;
        this.lastReport = System.currentTimeMillis();
    }

    private double distanceFrom(double rssi, int txPower) {
        if (rssi == 0) {
            return -1;
        }

        double ratio = rssi / txPower;

        if (ratio < 1) {
            return Math.pow(ratio, 10);
        }
        else {
            return 0.89976 * Math.pow(ratio, 7.7095) + 0.111;
        }
    }

    private static double filteredDistance(double newAccuracy, double previousAccuracy) {
        return previousAccuracy * (1 - IBeaconConstants.FILTER_FACTOR) + newAccuracy * IBeaconConstants.FILTER_FACTOR;
    }

    public double lastRssi() {
        return lastRssi;
    }

    public long lastReport() {
        return lastReport;
    }

    public double distanceInMetres() {
        return distanceInMetres;
    }
}
