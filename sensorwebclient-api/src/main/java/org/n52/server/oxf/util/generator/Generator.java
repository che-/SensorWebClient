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

package org.n52.server.oxf.util.generator;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.PropertyException;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.servlet.ServletUtilities;
import org.n52.oxf.OXFException;
import org.n52.oxf.OXFRuntimeException;
import org.n52.oxf.feature.OXFFeatureCollection;
import org.n52.oxf.feature.sos.ObservationSeriesCollection;
import org.n52.oxf.ows.capabilities.ITime;
import org.n52.oxf.valueDomains.time.TimeFactory;
import org.n52.server.oxf.util.ConfigurationContext;
import org.n52.server.oxf.util.access.ObservationAccessor;
import org.n52.server.oxf.util.access.oxfExtensions.TimePosition_OXFExtension;
import org.n52.server.oxf.util.properties.GeneralizationConfiguration;
import org.n52.shared.exceptions.ServerException;
import org.n52.shared.exceptions.TimeoutException;
import org.n52.shared.responses.RepresentationResponse;
import org.n52.shared.serializable.pojos.DesignOptions;
import org.n52.shared.serializable.pojos.TimeSeriesProperties;
import org.n52.shared.serializable.pojos.sos.Offering;
import org.n52.shared.serializable.pojos.sos.SOSMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class Generator.
 * 
 * @author <a href="mailto:tremmersmann@uni-muenster.de">Thomas Remmersmann</a>
 * @author <a href="mailto:broering@52north.org">Arne Broering</a>
 */
public abstract class Generator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    protected String folderPostfix;

    /**
     * can be executed to produce a presentation of sensor data.<br>
     * IMPORTANT: do not close the OutputStream within this method.
     * 
     * @param options
     *            the options
     * @return RepresentationResponse
     * @throws TimeoutException
     *             the timeout exception
     * @throws ServerException
     */
    public abstract RepresentationResponse producePresentation(DesignOptions options) throws TimeoutException, ServerException;

    /**
     * returns an object of type Map<String, OXFFeatureCollection>. The key is a
     * composed String object consisting of the offering-ID and the SOS URL from
     * which the observations of the corresponding OXFFeatureCollection have
     * been requested. The key-String looks like this: "<offeringID>@<sosURL>".
     *
     * @param options the options
     * @param onlyActiveTimeseries the only active timeseries
     * @return the map
     * @throws ServerException the server exception
     * @throws TimeoutException the timeout exception
     */
    protected Map<String, OXFFeatureCollection> getFeatureCollectionFor(DesignOptions options, boolean generalize) throws ServerException {
        try {
            ITime time = null;
            if (options.getTimeParam() == null) {
                time = readOutTime(options);
            } else {
                time = new TimePosition_OXFExtension(options.getTimeParam());
            }
            
            for (TimeSeriesProperties con : options.getProperties()) {
                SOSMetadata meta = ConfigurationContext.getSOSMetadata(con.getSosUrl());
                if (meta.canGeneralize() && generalize) {
                    String phenomenonURN = con.getPhenomenon().getId();
                    try {
                        String gen = GeneralizationConfiguration.getProperty(phenomenonURN);
                        if (gen != null) {
                            LOGGER.debug("Generalizer found for: " + phenomenonURN);
                            con.getProcedure().setId(con.getProcedure().getId()+","+gen);
                        }
                    } catch (PropertyException e) {
                        LOGGER.error("Error loading generalizer property for '{}'.", phenomenonURN, e);
                    }
                }
            }
            Map<String, OXFFeatureCollection> collectionResult = sendRequest(options, time);
            updateTimeSeriesPropertiesForHavingData(options, collectionResult);
            return collectionResult;
        } catch (OXFException e) {
            if (options.getTimeParam().equals("getFirst") || options.getTimeParam().equals("latest")) {
                throw new ServerException("SOS does not provide getFirst/getLast time extension.", e);
            } else {
                throw new ServerException("Unable to parse incoming data.", e);
            }
        } catch (Exception e) {
            throw new ServerException("Processing Observations failed.", e);
        }
    }

    private void updateTimeSeriesPropertiesForHavingData(DesignOptions options, Map<String, OXFFeatureCollection> entireCollMap) {
        for (TimeSeriesProperties prop : options.getProperties()) {

            OXFFeatureCollection obsColl = entireCollMap.get(prop.getOffering().getId() + "@" + prop.getSosUrl());

            String foiID = prop.getFoi().getId();
            String obsPropID = prop.getPhenomenon().getId();
            String procID = prop.getProcedure().getId();
            // if (procID.contains("urn:ogc:generalizationMethod:")) {
            // procID = procID.split(",")[0];
            // }
            ObservationSeriesCollection seriesCollection =
                    new ObservationSeriesCollection(obsColl, new String[] { foiID }, new String[] { obsPropID },
                            new String[] { procID }, true);

            if (seriesCollection.getSortedTimeArray().length > 0) {
                prop.setHasData(true);
            } else {
                prop.setHasData(false);
            }
        }
    }
    
    protected String formatDate(Date date) {
        return dateFormat.format(date);
    }

    public String getFolderPostfix() {
        return this.folderPostfix;
    }
    
    protected String createAndSaveImage(DesignOptions options, JFreeChart chart, ChartRenderingInfo renderingInfo) throws OXFException {
        int width = options.getWidth();
        int height = options.getHeight();
        BufferedImage image = chart.createBufferedImage(width, height, renderingInfo);
        Graphics2D chartGraphics = image.createGraphics();
        chartGraphics.setColor(Color.white);
        chartGraphics.fillRect(0, 0, width, height);
        chart.draw(chartGraphics, new Rectangle2D.Float(0, 0, width, height));

        try {
            return ServletUtilities.saveChartAsPNG(chart, width, height, renderingInfo, null);
        } catch (IOException e) {
            throw new OXFException("Could not save PNG", e);
        }
    }

    private Map<String, OXFFeatureCollection> sendRequest(DesignOptions options, ITime time) throws OXFException {
        List<RequestConfig> requests = createRequestList(options, time);
        Map<String, OXFFeatureCollection> result = null;
        try {
            result = new ObservationAccessor().sendRequests(requests);
        } catch (OXFRuntimeException e) {
            LOGGER.error("Failure when requesting GetObservation.", e);
        } catch (TimeoutException e) {
            LOGGER.error("GetObservation request timed out.", e);
        } catch (InterruptedException e) {
            LOGGER.error("Thread got interrupted during GetObservation request.", e);
        } catch (ExecutionException e) {
            LOGGER.error("Could not execute GetObservation request.", e);
        } catch (Exception e) {
			LOGGER.error("Error sending observation requests.", e);
		}
        return result;
    }

    private List<RequestConfig> createRequestList(DesignOptions options, ITime time) {
        List<RequestConfig> requests = new ArrayList<RequestConfig>();
        for (TimeSeriesProperties property : options.getProperties()) {
            List<String> fois = new ArrayList<String>();
            List<String> procedures = new ArrayList<String>();
            List<String> observedProperties = new ArrayList<String>();

            // extract request parameters from offering
            Offering offering = property.getOffering();
            observedProperties.add(property.getPhenomenon().getId());
            procedures.add(property.getProcedure().getId());
            fois.add(property.getFoi().getId());
            
            String sosUrl = property.getSosUrl();
            String offeringId = offering.getId();
            requests.add(new RequestConfig(sosUrl, offeringId, fois, observedProperties, procedures, time));
        }
        return requests;
    }

    protected ITime readOutTime(DesignOptions prop) {
        Calendar beginPos = Calendar.getInstance();
        beginPos.setTimeInMillis(prop.getBegin());
        Calendar endPos = Calendar.getInstance();
        endPos.setTimeInMillis(prop.getEnd());
        String begin = dateFormat.format(beginPos.getTime());
        String end = dateFormat.format(endPos.getTime());
        return TimeFactory.createTime(begin + "/" + end); 
    }

}