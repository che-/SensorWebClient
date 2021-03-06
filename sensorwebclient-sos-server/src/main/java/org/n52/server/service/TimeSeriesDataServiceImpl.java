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
package org.n52.server.service;

import org.n52.client.service.TimeSeriesDataService;
import org.n52.server.oxf.util.generator.TimeSeriesDataGenerator;
import org.n52.shared.requests.TimeSeriesDataRequest;
import org.n52.shared.responses.TimeSeriesDataResponse;
import org.n52.shared.serializable.pojos.DesignOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeSeriesDataServiceImpl implements TimeSeriesDataService {

    private static final Logger LOG = LoggerFactory.getLogger(TimeSeriesDataServiceImpl.class);
    
    @Override
    public TimeSeriesDataResponse getTimeSeriesData(TimeSeriesDataRequest request) throws Exception {
        try {
            LOG.debug("Performing TimeSeries data request.");
            DesignOptions options = request.getOptions();
            TimeSeriesDataGenerator generator = new TimeSeriesDataGenerator();
            TimeSeriesDataResponse resp = (TimeSeriesDataResponse) generator.producePresentation(options);
            LOG.debug(resp.toDebugString());
            return resp;
        } catch (Exception e) {
            LOG.error("Exception occured on server side.", e);
            throw e; // last chance to log on server side
        }
    }

}
