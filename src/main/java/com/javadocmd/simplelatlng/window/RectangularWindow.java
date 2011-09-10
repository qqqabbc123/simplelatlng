/*
 *    Copyright 2010 Tyler Coles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.javadocmd.simplelatlng.window;

import static java.lang.Math.abs;
import static java.lang.Math.min;

import java.math.BigDecimal;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LatLngConfig;
import com.javadocmd.simplelatlng.util.LengthUnit;

/**
 * <p>A "pseudo-rectangular" window bounded by a minimum and maximum latitude
 * and a minimum and maximum longitude. (The larger the window, the less rectangular 
 * this window actually is.) Naturally a window cannot span more than 180 
 * degrees latitude or 360 degrees longitude.</p>
 * 
 * <p>Note: the latitude span provided when creating this window is not a guarantee. 
 * If you create a latitude whose center is (90, 0) (the geographic North Pole) and 
 * whose latitude span is 10 degrees, the resulting window has a maximum latitude of 90 
 * and a minimum latitude of 85. Thus, windows are "squashed" if they hit the poles.</p>
 * 
 * @author Tyler Coles
 */
public class RectangularWindow extends LatLngWindow<RectangularWindow> {

	private BigDecimal latitudeDelta;
	private BigDecimal longitudeDelta;
	private BigDecimal minLatitude;
	private BigDecimal maxLatitude;
	private BigDecimal leftLongitude; // Defined as normalized longitude_center - (delta_longitude / 2)
	private BigDecimal rightLongitude; // Defined as normalized longitude_center + (delta_longitude / 2)
	private boolean crosses180thMeridian;
	private LatLng center;

	/**
	 * Creates a pseudo-rectangular window.
	 * 
	 * @param center the center point.
	 * @param deltaLat the span of the window in latitude in degrees.
	 * @param deltaLng the span of the window in longitude in degrees.
	 */
	public RectangularWindow(LatLng center, double deltaLat, double deltaLng) {
		this.setWindow(center, deltaLat, deltaLng);
	}

	/**
	 * Creates a psuedo-rectangular window. The height will include the all latitudes
	 * within <code>height/2</code> North and South, while the width will include all
	 * longitudes within <code>width/2</code> East and West of the center point. This
	 * is an approximation that will work fine for small window away from the poles, but
	 * keep in mind that, for example in the northern hemisphere, the top of the rectangle
	 * is narrower than the bottom of the rectangle, with the middle of the rectangle's width
	 * being somewhere in between. 
	 * 
	 * @param center the center point of the window.
	 * @param width the approximate width of the window in <code>LengthUnit</code>s. 
	 * @param height the height of the window in <code>LenghtUnit</code>s.
	 * @param unit the units for <code>width</code> and <code>height</code>.
	 */
	public RectangularWindow(LatLng center, double width, double height,
			LengthUnit unit) {
		double deltaLat = LatLngWindow.lengthToLatitudeDelta(height, unit);
		double deltaLng = LatLngWindow.lengthToLongitudeDelta(width, unit,
				center.getLatitude());
		this.setWindow(center, deltaLat, deltaLng);
	}

	/**
	 * Creates a psuedo-square window. This is a convenience method for creating a 
	 * window with the same height and width as in {@link #RectangularWindow(LatLng, double, double, LengthUnit)}.
	 * 
	 * @param center the center point of the window.
	 * @param widthHeight the approximate height and width of the window in <code>LengthUnit</code>s.
	 * @param unit the units for <code>widthHeight</code>.
	 */
	public RectangularWindow(LatLng center, double widthHeight, LengthUnit unit) {
		this(center, widthHeight, widthHeight, unit);
	}

	/**
	 * Sets the bounds of this window.
	 * 
	 * @param center the center point.
	 * @param deltaLat the span of the window in latitude in degrees.
	 * @param deltaLng the span of the window in longitude in degrees.
	 */
	public void setWindow(LatLng center, double deltaLat, double deltaLng) {
		if (center == null)
			throw new IllegalArgumentException("Invalid center point.");
		if (Double.isNaN(deltaLat) || Double.isInfinite(deltaLat))
			throw new IllegalArgumentException("Invalid latitude delta.");
		if (Double.isNaN(deltaLng) || Double.isInfinite(deltaLng))
			throw new IllegalArgumentException("Invalid longitude delta.");

		double dlat = min(abs(deltaLat), 180.0);
		this.setLatWindow(center.getLatitude(), dlat);

		double dlng = min(abs(deltaLng), 360.0);
		this.setLngWindow(center.getLongitude(), dlng);

		this.center = center;
	}

	/**
	 * Fixes and sets the latitude parameters for the window.
	 */
	private void setLatWindow(double centerLat, double deltaLat) {
		BigDecimal lat1 = LatLngTool.normalizeLatitude(new BigDecimal(centerLat
				+ (deltaLat / 2.0), LatLngConfig.DEGREE_CONTEXT));
		BigDecimal lat2 = LatLngTool.normalizeLatitude(new BigDecimal(centerLat
				- (deltaLat / 2.0), LatLngConfig.DEGREE_CONTEXT));
		this.maxLatitude = lat1.max(lat2);
		this.minLatitude = lat1.min(lat2);
		this.latitudeDelta = new BigDecimal(deltaLat, LatLngConfig.DEGREE_CONTEXT);
	}

	private static final BigDecimal DEGREE_180 = new BigDecimal(180,
			LatLngConfig.DEGREE_CONTEXT);

	/**
	 * Fixes and sets the longitude parameters for the window.
	 */
	private void setLngWindow(double centerLng, double deltaLng) {
		BigDecimal left = new BigDecimal(centerLng - (deltaLng / 2.0),
				LatLngConfig.DEGREE_CONTEXT);
		BigDecimal right = new BigDecimal(centerLng + (deltaLng / 2.0),
				LatLngConfig.DEGREE_CONTEXT);
		if (right.compareTo(DEGREE_180) > 0
				|| left.compareTo(DEGREE_180.negate()) < 0) {
			this.crosses180thMeridian = true;
		} else {
			this.crosses180thMeridian = false;
		}
		this.leftLongitude = LatLngTool.normalizeLongitude(left);
		this.rightLongitude = LatLngTool.normalizeLongitude(right);
		this.longitudeDelta = new BigDecimal(deltaLng,
				LatLngConfig.DEGREE_CONTEXT);
	}

	@Override
	public boolean contains(LatLng point) {

		if (point.getLatitudeBig().compareTo(maxLatitude) > 0
				|| point.getLatitudeBig().compareTo(minLatitude) < 0) {
			return false;
		}

		BigDecimal longitude = point.getLongitudeBig();
		if (crosses180thMeridian) {
			if (longitude.compareTo(BigDecimal.ZERO) < 0
					&& longitude.compareTo(rightLongitude) > 0) {
				return false;
			}
			if (longitude.compareTo(BigDecimal.ZERO) >= 0
					&& longitude.compareTo(leftLongitude) < 0) {
				return false;
			}
		} else if (longitude.compareTo(rightLongitude) > 0
				|| longitude.compareTo(leftLongitude) < 0) {
			return false;
		}
		return true;
	}

	@Override
	public boolean overlaps(RectangularWindow window) {

		if (window.maxLatitude.compareTo(minLatitude) < 0
				|| window.minLatitude.compareTo(maxLatitude) > 0) {
			return false;
		}

		BigDecimal thisLeft = leftLongitude;
		BigDecimal thisRight = rightLongitude;
		BigDecimal thatLeft = window.leftLongitude;
		BigDecimal thatRight = window.rightLongitude;
		if (thisRight.compareTo(thisLeft) < 0)
			thisRight = thisRight.add(LatLng.DEGREE_360);
		if (thatRight.compareTo(thisLeft) < 0)
			thatRight = thatRight.add(LatLng.DEGREE_360);

		if (thisRight.compareTo(thatLeft) < 0
				|| thisLeft.compareTo(thatRight) > 0) {
			return false;
		}

		return true;
	}

	/**
	 * Returns the height of the window.
	 * 
	 * @param unit the length units to return.
	 * @return the height of the window in the desired units.
	 */
	public double getHeight(LengthUnit unit) {
		return LatLngWindow.latitudeDeltaToLength(latitudeDelta.doubleValue(),
				unit);
	}

	/**
	 * Returns the width at the mid-line of the window.
	 * 
	 * @param unit the length units to return.
	 * @return the width of the window's mid-line in the desired units
	 */
	public double getWidth(LengthUnit unit) {
		return LatLngWindow.longitudeDeltaToLength(longitudeDelta.doubleValue(),
				unit, center.getLatitude());
	}

	@Override
	public LatLng getCenter() {
		return center;
	}

	/**
	 * If this window spans the 180 degree longitude meridian, this method
	 * returns true. Logic that uses this window in calculations may need
	 * to handle it specially. In this case, minLatitude is the negative-degree 
	 * meridian and maxLatitude is the positive-degree meridian and the
	 * window should extend from both lines to the 180 degree meridian.
	 * So instead of testing whether a point lies between the min/max-longitude,
	 * you would have to test if a point lay outside the min/max-longitude.
	 * 
	 * @return true if this window spans the 180th meridian. 
	 */
	public boolean crosses180thMeridian() {
		return crosses180thMeridian;
	}

	public double getLatitudeDelta() {
		return latitudeDelta.doubleValue();
	}

	public double getLongitudeDelta() {
		return longitudeDelta.doubleValue();
	}

	public double getMinLatitude() {
		return minLatitude.doubleValue();
	}

	public double getMaxLatitude() {
		return maxLatitude.doubleValue();
	}

	public double getLeftLongitude() {
		return leftLongitude.doubleValue();
	}

	public double getRightLongitude() {
		return rightLongitude.doubleValue();
	}

	@Override
	public String toString() {
		return String.format(
				"center: %s; lat range: [%s,%s]; lng range: [%s,%s]; meridian? %s",
				getCenter().toString(),
				LatLngConfig.DEGREE_FORMAT.format(getMinLatitude()),
				LatLngConfig.DEGREE_FORMAT.format(getMaxLatitude()),
				LatLngConfig.DEGREE_FORMAT.format(getLeftLongitude()),
				LatLngConfig.DEGREE_FORMAT.format(getRightLongitude()),
				Boolean.toString(crosses180thMeridian()));
	}
}
