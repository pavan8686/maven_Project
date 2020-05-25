/**
 * QI Analyze is a Java EE framework for identifying and analyzing Quality
 * Indicators Copyright (C) 2010 QIAnalyze LLC http://www.qianalyze.org
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/
 */
package IBM.module;

import generated.CurrentObservation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qianalyze.api.indicator.IndicatorObservation;
import org.qianalyze.api.indicator.IndicatorResult;
import org.qianalyze.api.module.IIndicatorInspector;

/**
 * This class illustrates the use of a POJO to create a Quality Indicator
 *
 * @author Matthew Demerath
 *
 */
public class Sailing implements IIndicatorInspector {

    //The date format is a string like this Tue, 03 Apr 2012 14:51:00 -0500
    private SimpleDateFormat sdfRaw = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss Z");
    private SimpleDateFormat sdfMonth = new SimpleDateFormat("MM");

    /**
     * This is the only public method of the IIndicatorInspector interface. It
     * is called by the encapsulating Module.
     *
     * @param data this is the unmashalled object from the XMLPayload.
     * @return result of inspecting the data passed in.
     */
    @Override
    public IndicatorResult inspect(Object data) {

        //Create a result to pass back. 
        IndicatorResult indicatorResult = new IndicatorResult();

        //Cast the data into the java object we wish to inspect. 
        CurrentObservation currentObservation = (CurrentObservation) data;

        //Check to see if this date is in season. 
        try {
            indicatorResult.setDenominator(isInSeason(currentObservation, indicatorResult));
        } catch (ParseException ex) {
            indicatorResult.setResult("Error Parsing Date");
            IndicatorObservation indicatorObservation = new IndicatorObservation();
            indicatorObservation.setName("Error Parsing Date");
            indicatorObservation.setValue("Expected date to be formated like 'Tue, 03 Apr 2012 14:51:00 -0500' but recived " + currentObservation.getObservationTimeRfc822());
            indicatorResult.getIndicatorObservations().add(indicatorObservation);
            return indicatorResult;
        }

        //Call the rules you defined for this Indicator. 
        if (isTooColdException(currentObservation, indicatorResult)
                || isCalmException(currentObservation, indicatorResult)
                || isWindyException(currentObservation, indicatorResult)) {

            //If this data object is part of the desired sample then set the Denominator to true. 
            // For instance if this CurrentObservation is outside of the sailing season then
            //we do not need to examine it for this Indicator and we would set it to false. 
            indicatorResult.setDenominator(true);

            //If any of the exceptions happened then this indicator is false. 
            indicatorResult.setNumerator(false);

            //Set the result as a string that the calling program can use. 
            //This could be "yes", "no", "True", "False", "0", "1" or what ever you want. 
            indicatorResult.setResult("False");
            return indicatorResult;
        }

        //Observations are just Name value pairs that you can send back to the calling application. 
        // The can be informational message such as this or debugging statements. 
        IndicatorObservation observation = new IndicatorObservation();
        observation.setName("Perfect Day To Sail");
        observation.setValue("It is a perfect day to go sailing. The winds are " + currentObservation.getWindString()
                + " and the temperature is " + currentObservation.getTemperatureString());

        //Add the observation to the result. 
        indicatorResult.getIndicatorObservations().add(observation);

        //Set the values to true because there were no exceptions..
        indicatorResult.setDenominator(true);
        indicatorResult.setNumerator(true);
        indicatorResult.setResult("True");

        //Finaly we return the result to the Module. 
        return indicatorResult;
    }

    private boolean isInSeason(CurrentObservation currentObservation, IndicatorResult indicatorResult) throws ParseException {

        Date obDate = sdfRaw.parse(currentObservation.getObservationTimeRfc822());
        int month = Integer.valueOf(sdfMonth.format(obDate));
        return month > 3 && month < 11;

    }

    /**
     * Check to see if the Observation is too cold.
     *
     * @param currentObservation the data we need to look at.
     * @param indicatorResult the result that holds the results and
     * Observations.
     * @return true if the temperature is less then 50 (This is a personal
     * choice you may conceder that too cold or not cold enough.)
     */
    private boolean isTooColdException(CurrentObservation currentObservation, IndicatorResult indicatorResult) {

        if (currentObservation.getTempF().intValue() < 50) {
            IndicatorObservation observation = new IndicatorObservation();
            observation.setName("TooCold");
            observation.setValue("It wouldn't be comfortable sailing in when it is less the 50 degrees.");
            indicatorResult.getIndicatorObservations().add(observation);
            return true;
        }
        return false;
    }

    /**
     * Check to see if there is enough wind.
     *
     * @param currentObservation the data we need to look at.
     * @param indicatorResult the result that holds the results and
     * Observations.
     * @return true if the Wind is too calm.
     */
    private boolean isCalmException(CurrentObservation currentObservation, IndicatorResult indicatorResult) {

        if (currentObservation.getWindKt().intValue() < 3) {
            IndicatorObservation observation = new IndicatorObservation();
            observation.setName("TooCalm");
            observation.setValue("It is to calm to sail.");
            indicatorResult.getIndicatorObservations().add(observation);
            return true;
        }
        return false;
    }

    /**
     * Check to see if it is too windy.
     *
     * @param currentObservation the data we need to look at.
     * @param indicatorResult the result that holds the results and
     * Observations.
     * @return true if the wind is over 35 knots it would be to rough to enjoy.
     */
    private boolean isWindyException(CurrentObservation currentObservation, IndicatorResult indicatorResult) {

        if (currentObservation.getWindKt().intValue() > 35) {
            IndicatorObservation observation = new IndicatorObservation();
            observation.setName("TooWindy");
            observation.setValue("It is to rough to enjoy sailing.");
            indicatorResult.getIndicatorObservations().add(observation);
            return true;
        }
        return false;
    }
}
