package edu.illinois.cs.cs125.fall2019.mp;

import com.google.android.gms.maps.model.LatLng;

/**
 * Divides a rectangular area into identically sized, roughly square cells.
 * Each cell is given an X and Y coordinate. X increases from the west boundary toward the east boundary;
 * Y increases from south to north. So (0, 0) is the cell in the southwest corner.
 *
 * Instances of this class are created with a desired cell size. However, it is unlikely that the area dimensions
 * will be an exact multiple of that length, so placing fully sized cells would leave a small "sliver" on the east
 * or north side. Length should be redistributed so that each cell is exactly the same size. If the area is 70 meters
 * long in one dimension and the cell size is 20 meters, there will be four cells in that dimension (there's room for
 * three full cells plus a 10m sliver), each of which is 70 / 4 = 17.5 meters long. Redistribution happens independently
 * for the two dimensions, so a 70x40 area would be divided into 17.5x20.0 cells with a 20m cell size.
 */
public class AreaDivider {
    /**
     * north - north value.
     */
    private double north;
    /**
     * east - east value.
     */
    private double east;
    /**
     * south - south value.
     */
    private double south;
    /**
     * west - west value.
     */
    private double west;
    /**
     * *cell - cell value.
     */
    private double cell;
    /**
     *
     * Creates an AreaDivider for an area.
     * @param setNorth - latitude of the north boundary
     * @param setEast - longitude of the east boundary
     * @param setSouth - latitude of the south boundary
     * @param setWest - longitude of the west boundary
     * @param setCellSize - the requested side length of each cell, in meters
   */
    public AreaDivider(final double setNorth,
                       final double setEast,
                       final double setSouth,
                       final double setWest,
                       final double setCellSize) {
        north = setNorth;
        east = setEast;
        south = setSouth;
        west = setWest;
        cell = setCellSize;
    }

    /**
     * Gets the boundaries of the specified cell as a Google Maps LatLngBounds object.
     * @param x - the cell's X coordinate
     * @param y - the cell's Y coordinate
     * @return the boundaries of the cell
     */
    public com.google.android.gms.maps.model.LatLngBounds getCellBounds(final int x,
                                                                         final int y) {
        LatLng southwest = new LatLng();
        LatLng northeast = new LatLng();
        return getCellBounds(x, y);
    }

    /**
     * Gets the number of cells between the west and east boundaries.
     * See the class description for more details on area division.
     * @return the number of cells in the X direction
     */
    public int getXCells() {
        double a = west - east;
        double temp1 = a / cell;
        int n = (int) temp1;

        return n;
    }

    /**
     * Gets the X coordinate of the cell containing the specified location.
     * The point is not necessarily within the area.
     * @param location name
     * @return the X coordinate of the cell containing the lat-long point
     */
    public int getXCoordinate(final com.google.android.gms.maps.model.LatLng location) {
        return 0;
    }

    /**
     * Gets the number of cells between the south and north boundaries.
     * See the class description for more details on area division.
     * @return the number of cells in the Y direction
     */
    public int getYCells() {
        double b = south - north;
        double temp2 = b / cell;
        int n = (int) temp2;
        return n;
    }

    /**
     * Gets the Y coordinate of the cell containing the specified location.
     * The point is not necessarily within the area.
     * @param location name
     * @return the Y coordinate of the cell containing the lat-long point
     */
    public int getYCoordinate(final com.google.android.gms.maps.model.LatLng location) {
        return 0;
    }

    /**
     * Draws the grid to a map using solid black polylines.
     * There should be one line on each of the four boundaries of the overall area and as many
     * internal lines as necessary to divide the rows and columns of the grid. Each line should
     * span the whole width or height of the area rather than the side of just one cell.
     * For example, an area divided into a 2x3 grid would be drawn with 7 lines total: 4 for
     * the outer boundaries, 1 vertical line to divide the west half from the east half (2 columns),
     * and 2 horizontal lines to divide the area into 3 rows.
     *
     * See the provided addLine function from GameActivity for how to add a line to the map.
     * Since these lines should be black, you do not need the extra line to make the line appear to have a border.
     * @param map - the Google map to draw on
     */
    public void renderGrid(final com.google.android.gms.maps.GoogleMap map) {
    }
}
