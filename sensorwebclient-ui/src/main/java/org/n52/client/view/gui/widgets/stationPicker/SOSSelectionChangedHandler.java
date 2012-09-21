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
package org.n52.client.view.gui.widgets.stationPicker;

import org.n52.client.control.PropertiesManager;
import org.n52.client.eventBus.EventBus;
import org.n52.client.eventBus.events.dataEvents.sos.GetPhenomenonsEvent;
import org.n52.client.eventBus.events.dataEvents.sos.GetStationsEvent;
import org.n52.client.model.data.dataManagers.DataManagerSosImpl;
import org.n52.shared.serializable.pojos.BoundingBox;
import org.n52.shared.serializable.pojos.sos.SOSMetadata;
import org.n52.shared.serializable.pojos.sos.SOSMetadataBuilder;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;

final class SOSSelectionChangedHandler implements SelectionChangedHandler {

	private final StationPickerController controller;

	public SOSSelectionChangedHandler(StationPickerController controller) {
		this.controller = controller;
	}

	@Override
	public void onSelectionChanged(SelectionEvent event) {
		Record record = event.getRecord();
		if (event.getState() && record != null) {
			String serviceURL = record.getAttribute("url");
			parseAndStoreSOSMetadata(serviceURL, record);
			performSOSDataRequests(serviceURL);
		}
	}

	private void parseAndStoreSOSMetadata(String serviceURL, Record record) {
		SOSMetadataBuilder builder = new SOSMetadataBuilder();
		DataManagerSosImpl dataManager = DataManagerSosImpl.getInst();
		if (!dataManager.contains(serviceURL)) {
	        parseAndSetServiceConfiguration(builder, record);
			dataManager.storeData(serviceURL, builder.build());
		};
	}

	private void parseAndSetServiceConfiguration(SOSMetadataBuilder builder, Record record) {
		try {
		    builder.addServiceName(record.getAttribute("itemName"))
                   .addServiceURL(record.getAttribute("url"))
		           .addServiceVersion(record.getAttribute("version"))
                   .setWaterML(record.getAttributeAsBoolean("waterML"))
                   .setAutoZoom(Boolean.parseBoolean(record.getAttribute("autoZoom")))
                   //.addDefaultZoom(record.getAttributeAsInt("defaultZoom"))
                   .addLowerLeftEasting(record.getAttributeAsDouble("llEasting"))
                   .addLowerLeftNorthing(record.getAttributeAsDouble("llNorthing"))
                   .addUpperRightEasting(record.getAttributeAsDouble("urEasting"))
                   .addUpperRightNorthing(record.getAttributeAsDouble("urNorthing"));
		    if (GWT.isProdMode()) {
                builder.setRequestChunk(record.getAttributeAsInt("requestChunk"));
            } else {
                PropertiesManager properties = PropertiesManager.getInstance();
                builder.setRequestChunk(properties.getParamaterAsInt("devModeRequestChunk", 50));
            }
		} catch (Exception e) {
			GWT.log("Could not parse initial extent for SOS " + builder.getServiceURL());
		}
	}

	private void performSOSDataRequests(String serviceURL) {
	    /*
	     * XXX
	     * Using the current extent would require the client to get missing stations
	     * from the server part. this would make neccessary an interaction (zoom, pan) 
	     * based rendering of stations!
	     */
//		BoundingBox bbox = controller.getCurrentExtent();
	    DataManagerSosImpl dataManager = DataManagerSosImpl.getInst();
	    SOSMetadata metadata = dataManager.getServiceMetadata(serviceURL);
        BoundingBox bbox = metadata.getConfiguredExtent();
		GetStationsEvent getStations = new GetStationsEvent(serviceURL, bbox);
		controller.loadingStations(true);
		GetPhenomenonsEvent getPhenomenons = new GetPhenomenonsEvent.Builder(serviceURL).build();
		EventBus.getMainEventBus().fireEvent(getStations);
		EventBus.getMainEventBus().fireEvent(getPhenomenons);
		controller.setSelectedServiceURL(serviceURL);
	}
}