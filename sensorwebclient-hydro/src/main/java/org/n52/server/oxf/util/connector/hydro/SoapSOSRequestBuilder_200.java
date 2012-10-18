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
package org.n52.server.oxf.util.connector.hydro;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.n52.oxf.OXFException;
import org.n52.oxf.adapter.ParameterContainer;
import org.n52.oxf.xmlbeans.tools.XMLBeansTools;
import org.n52.server.oxf.util.access.oxfExtensions.SOSRequestBuilder_200_OXFExtension;
import org.w3.x2003.x05.soapEnvelope.EnvelopeDocument;

public class SoapSOSRequestBuilder_200 extends SOSRequestBuilder_200_OXFExtension {
	
	/** Soap header actions for the sos requests */
	private static final String GET_CAPABILITIES_SOAP_HEADER_ACTION = "http://www.opengis.net/def/serviceOperation/sos/core/2.0/GetCapabilities";
	private static final String GET_FOI_SOAP_HEADER_ACTION = "http://www.opengis.net/def/serviceOperation/sos/foiRetrieval/2.0/GetFeatureOfInterest";
	private static final String DESCRIBE_SENSOR_SOAP_HEADER_ACTION = "http://www.opengis.net/def/serviceOperation/sos/core/2.0/DescribeSensor"; // spec says: http://www.opengis.net/swes/2.0/DescribeSensor
	private static final String GET_OBS_SOAP_HEADER_ACTION = "http://www.opengis.net/def/serviceOperation/sos/core/2.0/GetObservation";
	
	@Override
	public String buildGetCapabilitiesRequest(ParameterContainer parameters) {
		String request = super.buildGetCapabilitiesRequest(parameters);
		EnvelopeDocument envelope = addSoapEnvelope(request, GET_CAPABILITIES_SOAP_HEADER_ACTION);
		return envelope.xmlText(XMLBeansTools.PRETTYPRINT);
	}

	@Override
	public String buildGetFeatureOfInterestRequest(ParameterContainer parameters) {
		String request = super.buildGetFeatureOfInterestRequest(parameters);
		EnvelopeDocument envelope = addSoapEnvelope(request, GET_FOI_SOAP_HEADER_ACTION);
		return envelope.xmlText(XMLBeansTools.PRETTYPRINT);  
	}

	@Override
	public String buildDescribeSensorRequest(ParameterContainer parameters) {
		String request = super.buildDescribeSensorRequest(parameters);
		EnvelopeDocument envelope = addSoapEnvelope(request, DESCRIBE_SENSOR_SOAP_HEADER_ACTION);
		return envelope.xmlText(XMLBeansTools.PRETTYPRINT);
	}
	
	@Override
	public String buildGetObservationRequest(ParameterContainer parameters) throws OXFException {
		String request = super.buildGetObservationRequest(parameters);
		EnvelopeDocument envelope = addSoapEnvelope(request, GET_OBS_SOAP_HEADER_ACTION);
		return envelope.xmlText(XMLBeansTools.PRETTYPRINT);
	}

	private EnvelopeDocument addSoapEnvelope(String request, String action) {
		XmlObject xb_request = null;
		try {
			xb_request = XmlObject.Factory.parse(request);
		} catch (XmlException e) {
			e.printStackTrace();
		}
		EnvelopeDocument envelope = SoapUtil.wrapToSoapEnvelope(xb_request, action);
		return envelope;
	}
	

}