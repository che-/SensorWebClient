/**
 * ﻿Copyright (C) 2012
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */

package org.n52.client.view.gui.elements.tabImpl;

import java.util.ArrayList;
import java.util.Collection;

import org.gwtopenmaps.openlayers.client.Marker;
import org.n52.client.control.mapTab.OpenlayersTabController;
import org.n52.client.model.data.DataStoreTimeSeriesImpl;
import org.n52.client.model.data.representations.TimeSeries;
import org.n52.client.view.View;
import org.n52.client.view.gui.elements.controlsImpl.DataControls;
import org.n52.client.view.gui.elements.interfaces.DataPanelTab;
import org.n52.client.view.gui.widgets.mapping.Coordinate;
import org.n52.client.view.gui.widgets.mapping.OpenlayersMarker;
import org.n52.client.view.gui.widgets.mapping.OverviewMap;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.layout.Layout;

@Deprecated
public class OpenlayersTab extends DataPanelTab {

    private OverviewMap map;

    private OpenlayersTabController controller;

    private Layout layout;

    private String selectedTimeSeries = "";

    public OpenlayersTab(String id, String title) {
        setID(id);
        setTitle(title);
        setIcon("../img/icons/map.png");
//        controller = new OpenlayersTabController(this);
//        map = new OpenlayersTabMap(); // FIXME clean up class
        TAB_ID = "OpenlayersMapTab";
        init();
    }

    private void init() {
        this.layout = new Layout();
        this.layout.ensureDebugId("layout1");
        this.layout.setOverflow(Overflow.HIDDEN);
        this.layout.addMember(this.map.getMapWidget());
        setPane(this.layout);

        int width = View.getInstance().getDataPanelWidth();
        int height = View.getInstance().getDataPanelHeight();
        resizeTo(width, height);
    }

    public OverviewMap getMap() {
        return this.map;
    }

    public void update() {
        this.map.removeAllMarkers();
        Collection<TimeSeries> ts = DataStoreTimeSeriesImpl.getInst().getDataItems().values();

        for (TimeSeries timeSeries : ts) {
            Coordinate coords = timeSeries.getCoords();
            if (coords == null) {
                GWT.log("TimeSeries has no coordinates; skip creating map button.");
                continue;
            }

            OpenlayersMarker olMarker = new OpenlayersMarker(coords, timeSeries);
            ArrayList<Marker> markers = map.getMarkers();
            
            if (markers.isEmpty()) {
                map.addMarker(olMarker);
                olMarker.createInfoPopup(map.getMap());
            }
            else {
                int size = markers.size(); // avoid CMException
                for (int i = 0; i < size; i++) {
                    Marker marker = markers.get(i);

                    if ( ! (marker instanceof OpenlayersMarker)) {
                        continue; // ignore non OL markers
                    }

                    // XXX necessary to differentiate between OLmarker and "normal" marker?
                    if (isMarkerAlreadyShown(olMarker)) {
                        extendOLMarkerInfo(timeSeries.getId(), marker, olMarker.getInfoTxt());
                    }
                    else {
                        map.addMarker(olMarker);
                        olMarker.createInfoPopup(map.getMap());
                    }
                }
            }
        }
        markSelectedMarker();
        if (map.getMarkers().size() != 0) {
        	map.zoomToMarkers();
		} else {
			map.initMap();
		}
    }

    private void markSelectedMarker() {
        if (this.selectedTimeSeries != null && this.selectedTimeSeries.length() > 0) {
            for (Marker m : map.getMarkers()) {
                if (m instanceof OpenlayersMarker) {
                    OpenlayersMarker olm = (OpenlayersMarker) m;
                    if (olm.containsTS(this.selectedTimeSeries)) {
                        olm.mark();
                    }
                    else {
                        olm.unmark();
                    }
                }
            }
        }
    }

    private void extendOLMarkerInfo(String id, Marker marker, String olInfo) {
        OpenlayersMarker olm = (OpenlayersMarker) marker;
        if ( !olm.availableTimeseriesIds().contains(id)) {
            this.map.removeMarker(marker);
            olm.addToInfoTxt("---------</br>" + olInfo);
            olm.addTimeseriesId(id);
            map.addMarker(olm);
        }
    }

    /**
     * Checks if given marker is already shown on the map.
     * 
     * @param olMarker
     *        the open layers marker to check.
     * @return <code>true</code> if marker is already shown, and <code>false</code> otherwise.
     */
    private boolean isMarkerAlreadyShown(OpenlayersMarker olMarker) {
        for (Marker marker : map.getMarkers()) {
            if (olMarker.getLonLat().equals(marker.getLonLat())) {
                return true;
            }
        }
        return false;
    }

    public void resizeTo(int width, int height) {
        this.map.resizeTo(width, height);
    }

    @Override
    public DataControls getDataControls() {
        return this.controller.getControls();
    }

    public void setSelectedTimeSeries(String id) {
        this.selectedTimeSeries = id;
    }

}