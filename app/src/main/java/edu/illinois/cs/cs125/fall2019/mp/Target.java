package edu.illinois.cs.cs125.fall2019.mp;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * class.
 */
public class Target {
    /**
     * Map.
     */
    private com.google.android.gms.maps.GoogleMap map;
    /**
     * position.
     */
    private com.google.android.gms.maps.model.LatLng position;
    /**
     * team.
     */
    private int team;
    /**
     * marker.
     */
    private Marker marker;
    /**
     Creates a target in a target-mode game by placing an appropriately colored marker on the map.
     The marker's hue should reflect the team that captured the target.
     See the class description for the hue values to use.
     @param setMap the map to render to
     @param setPosition the position of the target
     @param setTeam the TeamID code of the team currently owning the target
     */
    public Target(final com.google.android.gms.maps.GoogleMap setMap,
                  final com.google.android.gms.maps.model.LatLng setPosition,
                  final int setTeam) {
        position = setPosition;
        team = setTeam;
        MarkerOptions options = new MarkerOptions().position(position);
        marker = setMap.addMarker(options);
        if (team == TeamID.OBSERVER) {
            BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);
            marker.setIcon(icon);
        }
    }

    /**
     * Gets the position of the target.
     * @return the coordinates of the target
     */
    public com.google.android.gms.maps.model.LatLng getPosition() {
        return position;
    }

    /**
     * Gets the ID of the team currently owning this target.
     * @return the owning team ID or OBSERVER if unclaimed
     */
    public int getTeam() {
        return team;
    }

    /**
     * Updates the owning team of this target and changes the marker hue appropriately.
     * @param newTeam - the ID of the team that captured the target
     */
    public void setTeam(final int newTeam) {
        team = newTeam;
        if (team == TeamID.TEAM_RED) {
            BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
            marker.setIcon(icon);
        } else if (team == TeamID.TEAM_YELLOW) {
            BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
            marker.setIcon(icon);
        } else if (team == TeamID.TEAM_GREEN) {
            BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
            marker.setIcon(icon);
        } else if (team == TeamID.TEAM_BLUE) {
            BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
            marker.setIcon(icon);
        }
    }
}
