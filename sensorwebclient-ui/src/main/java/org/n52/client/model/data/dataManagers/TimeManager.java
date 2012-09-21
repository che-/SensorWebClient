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

package org.n52.client.model.data.dataManagers;

import java.util.Stack;

import org.n52.client.control.I18N;
import org.n52.client.control.PropertiesManager;
import org.n52.client.eventBus.EventBus;
import org.n52.client.eventBus.events.DatesChangedEvent;
import org.n52.client.eventBus.events.dataEvents.sos.OverviewIntervalChangedEvent;
import org.n52.client.eventBus.events.dataEvents.sos.OverviewIntervalChangedEvent.IntervalType;
import org.n52.client.eventBus.events.dataEvents.sos.RequestDataEvent;
import org.n52.client.eventBus.events.dataEvents.sos.UndoEvent;
import org.n52.client.eventBus.events.dataEvents.sos.handler.OverviewIntervalChangedEventHandler;
import org.n52.client.eventBus.events.dataEvents.sos.handler.UndoEventHandler;
import org.n52.client.eventBus.events.handler.DatesChangedEventHandler;
import org.n52.client.model.data.representations.DateAction;
import org.n52.client.view.gui.widgets.Toaster;
import org.n52.shared.Constants;

public class TimeManager {
    
    private static TimeManager inst;

    private static long DAYS_TO_MILLIS_FACTOR = 24 * 60 * 60 * 1000; // h * min * sec * millis
    
    private static int MAX_OVERVIEW_INTERVAL = 5; // TODO make configurable
    
    private static int MIN_OVERVIEW_INTERVAL = 2;

    private long begin;

    private long end;

    protected Stack<DateAction> undoStack;

    protected long overviewInterval;

    protected IntervalType intervalType;

    private TimeManager() {
        new TimeManagerEventBroker(); // registers handler class on event bus
        PropertiesManager propertiesMgr = PropertiesManager.getInstance();
        long days = new Long(propertiesMgr.getParameterAsString(Constants.DEFAULT_INTERVAL));
        this.begin = System.currentTimeMillis() - (days * DAYS_TO_MILLIS_FACTOR);
        this.end = System.currentTimeMillis();
        this.undoStack = new Stack<DateAction>();
    }

    public static TimeManager getInst() {
        if (inst == null) {
            inst = new TimeManager();
        }
        return inst;
    }

    public long daysToMillis(String days) {
        return new Long(days).longValue() * DAYS_TO_MILLIS_FACTOR;
    }

    public long getBegin() {
        return this.begin;
    }

    public long getOverviewInterval() {
        return this.overviewInterval;
    }

    public long getOverviewOffset(long start, long end) {
        long inter = end - start;
        if (inter >= this.overviewInterval) {
            long gap = inter - this.overviewInterval;
            return gap + DAYS_TO_MILLIS_FACTOR;
        }
        return 0l;
    }

    public IntervalType getIntervalType() {
        return this.intervalType;
    }

    public long getEnd() {
        return this.end;
    }

    protected void undoLast() {
        if (this.undoStack.isEmpty()) {
            Toaster.getInstance().addMessage(I18N.sosClient.undoMessage());
            return;
        }
        DateAction last = this.undoStack.pop();
        TimeManager.this.begin = last.getBegin();
        TimeManager.this.end = last.getEnd();
    }

    private class TimeManagerEventBroker implements
            DatesChangedEventHandler,
            UndoEventHandler,
            OverviewIntervalChangedEventHandler {

        /**
         * Instantiates a new event broker.
         */
        public TimeManagerEventBroker() {
            EventBus.getMainEventBus().addHandler(DatesChangedEvent.TYPE, this);
            EventBus.getMainEventBus().addHandler(UndoEvent.TYPE, this);
            EventBus.getMainEventBus().addHandler(OverviewIntervalChangedEvent.TYPE, this);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.n52.client.eventBus.events.handler.DatesChangedEventHandler#
         * onDatesChanged(org.n52.client.eventBus.events.DatesChangedEvent)
         */
        public void onDatesChanged(DatesChangedEvent evt) {
            TimeManager.this.undoStack.push(new DateAction(TimeManager.this.begin, TimeManager.this.end));
            TimeManager.this.begin = evt.getStart();
            TimeManager.this.end = evt.getEnd();
            if (TimeManager.this.overviewInterval > (evt.getEnd() - evt.getStart()) * MAX_OVERVIEW_INTERVAL) {
				TimeManager.this.overviewInterval = (evt.getEnd() - evt.getStart()) * MAX_OVERVIEW_INTERVAL;
			} else if (TimeManager.this.overviewInterval < (evt.getEnd() - evt.getStart()) * MIN_OVERVIEW_INTERVAL) {
				TimeManager.this.overviewInterval = (evt.getEnd() - evt.getStart()) * MIN_OVERVIEW_INTERVAL;
			}
            if ( !evt.isSilent()) {
                EventBus.getMainEventBus().fireEvent(new RequestDataEvent());
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.n52.client.eventBus.events.dataEvents.sos.handler.UndoEventHandler #onUndo()
         */
        public void onUndo() {
            TimeManager.this.undoLast();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.n52.client.eventBus.events.dataEvents.sos.handler. OverviewIntervalChangedEventHandler
         * #onChanged(org.n52.client.eventBus. events.dataEvents.sos.OverviewIntervalChangedEvent)
         */
        public void onChanged(OverviewIntervalChangedEvent evt) {
            long interval = TimeManager.this.end - TimeManager.this.begin;
            if (interval > evt.getInterval()) {
                Toaster.getInstance().addMessage(I18N.sosClient.errorOverviewInterval());
            }
            else {
                TimeManager.this.intervalType = evt.getType();
                TimeManager.this.overviewInterval = evt.getInterval();
            }
        }
    }

}