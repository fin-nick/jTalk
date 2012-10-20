/*
 * Copyright (C) 2012, Igor Ustyugov <igor@ustyugov.net>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/
 */

package net.ustyugov.jtalk.listener;

import net.ustyugov.jtalk.service.JTalkService;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class LocationChangedListener implements LocationListener {

	@Override
	public void onLocationChanged(Location location) {
		JTalkService service = JTalkService.getInstance();
		service.sendLocation(location);
	}

	@Override
	public void onProviderDisabled(String arg0) { }

	@Override
	public void onProviderEnabled(String arg0) { }

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) { }
}
