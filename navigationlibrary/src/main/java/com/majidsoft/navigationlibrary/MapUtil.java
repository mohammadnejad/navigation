package com.majidsoft.navigationlibrary;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
import java.util.List;

/**
 * The MapUtil class hold logical functions for google map
 *
 * @author Majid Mohammadnejad
 * @version 1.0
 * @email : majidrasht@gmail.com
 * @since 10/9/16
 */

public class MapUtil {

    private LinearRegression regression = new LinearRegression();

    /**
     * This method calculate distance between two geographical points
     *
     * @param StartP
     * @param EndP
     * @return
     */
    public double calculationByDistance(LatLng StartP, LatLng EndP) {

        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;

        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        double valueResult = Radius * c;

        double km = valueResult / 1;

        DecimalFormat newFormat = new DecimalFormat("####");

        int kmInDec = Integer.valueOf(newFormat.format(km));

        double meter = valueResult % 1000;

        int meterInDec = Integer.valueOf(newFormat.format(meter));

        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);

        return Radius * c;
    }

    /**
     * This method calculate min distance between gps location and list of locations and return member of list that is min distance
     *
     * @param gps
     * @return
     */
    public int minDistanceBetweenListAndGPSMember(List<LatLng> latLngs, LatLng gps) {
        double distanceThreshold = 2; //threshold for min distance
        int minMember = 0;
        double minDistance = Double.MAX_VALUE;
        Double distance;

        for (int i = 0; i < latLngs.size(); i++) {
            distance = calculationByDistance(gps, latLngs.get(i));

            Log.i("distance", "dis = " + distance);

            if (distance < minDistance) {
                minDistance = distance;
                minMember = i;
            }
        }

        if (minDistance > distanceThreshold)
            return -1;

        return minMember;
    }

    /**
     * This method calculate point p that is inside the line
     *
     * @param latLngs
     * @param gps
     * @return
     */

    public LatLng getProjectLocation(List<LatLng> latLngs, LatLng gps) {

        LatLng firstLocation;
        LatLng secondLocation;
        Point startPoint;
        Point endPoint;
        Point gpsPoint;
        Point pointInsideLine;
        int nearMember;

        nearMember = minDistanceBetweenListAndGPSMember(latLngs, gps);

        //if min distance greater than threshold distance
        if (nearMember == -1) {
            return gps;
        }

        if (nearMember == 0) {
            firstLocation = latLngs.get(nearMember);
            secondLocation = latLngs.get(nearMember + 1);
        } else {
            firstLocation = latLngs.get(nearMember - 1);
            secondLocation = latLngs.get(nearMember);
        }

        startPoint = new Point(firstLocation.latitude, firstLocation.longitude);
        endPoint = new Point(secondLocation.latitude, secondLocation.longitude);
        gpsPoint = new Point(gps.latitude, gps.longitude);

        pointInsideLine = regression.getProjectedPointOnLine(startPoint, endPoint, gpsPoint);

        if (nearMember == 0 || nearMember == (latLngs.size() - 1))
            return new LatLng(pointInsideLine.x, pointInsideLine.y);

        if (regression.checkPointInsideLine(startPoint, endPoint, pointInsideLine)) {
            return new LatLng(pointInsideLine.x, pointInsideLine.y);
        } else {
            firstLocation = latLngs.get(nearMember);
            secondLocation = latLngs.get(nearMember + 1);

            startPoint = new Point(firstLocation.latitude, firstLocation.longitude);
            endPoint = new Point(secondLocation.latitude, secondLocation.longitude);
            gpsPoint = new Point(gps.latitude, gps.longitude);

            pointInsideLine = regression.getProjectedPointOnLine(startPoint, endPoint, gpsPoint);

            return new LatLng(pointInsideLine.x, pointInsideLine.y);
        }

    }

}