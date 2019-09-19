package com.hoffi.minimal.microservices.microservice.helpers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class AverageDurationHelper {

    public static class AverageData {
        public int count;
        public double average = -1;
        public long max;
        public long min;

        public AverageData() {}

        public AverageData(long average) {
            this.count = 1;
            this.average = average;
            this.max = average;
            this.min = average;
        }

        @Override
        public String toString() {
            return String.format("average:%d, max:%d, min:%d, count:%d", (long) average, max, min, count);
        }
    }

    public Map<String, AverageData> map = new ConcurrentHashMap<>();
    private AverageData noData = new AverageData();


    /**
     * internal helper method
     *
     * @param name     the name of the average to get
     * @param internal true if called from internal, false if called for outside call
     * @return the named AverageData or a default AverageData with count = 0 and average = -1
     */
    private AverageData getAverageData(String name, boolean throwRuntimeException) {
        AverageData data = map.get(name);
        if (data == null) {
            if (throwRuntimeException) {
                throw new RuntimeException("no average data found for '" + name + "'");
            } else {
                return noData;
            }
        }
        return data;
    }

    /**
     * calls getAverageData("default")
     *
     * @return the default AverageData or
     * @throws RuntimeException if named average does not exist
     */
    public AverageData getAverageData() {
        return getAverageData("default");
    }

    /**
     * getAverageData internal structure
     *
     * @param name the name of the average to get
     * @return the named AverageData or
     * @throws RuntimeException if named average does not exist
     */
    public AverageData getAverageData(String name) {
        AverageData data = getAverageData(name, true);
        return data;
    }

    /**
     * calls newAverage("default", measuredms)
     *
     * @param measuredms
     * @return the new average for "default" or
     * @throws RuntimeException if given measurement is less than zero
     */
    public long newAverage(long measuredms) {
        return newAverage("default", measuredms);
    }

    /**
     * add a measurement to the named average or creates a new named measurement with the given measurement as initial value
     *
     * @param name       the name of the average to add a measurement to
     * @param measuredms the measurement in milliseconds
     * @return the new average or
     * @throws RuntimeException if given measurement is less than zero
     */
    public long newAverage(String name, long measuredms) {
        if (measuredms < 0) {
            throw new RuntimeException("measured time value may not be less than zero for '" + name + "'");
        }
        AverageData data = getAverageData(name, false);
        if (data.average == -1) {
            data = new AverageData(measuredms);
            map.put(name, data);
            return measuredms;
        }
        double newAverage = ((data.count * data.average) + measuredms) / (data.count + 1);
        data.count++;
        data.average = newAverage;
        if (data.max < measuredms) {
            data.max = measuredms;
        }
        if (data.min > measuredms) {
            data.min = measuredms;
        }
        return (long) newAverage;
    }

    /**
     * calls average("default")
     *
     * @return the current average for "default" or
     * @throws RuntimeException if named average does not exist
     */
    public long average() {
        return average("default");
    }

    /**
     * gets the named average value as long
     *
     * @param name the name of the average to get the average for
     * @return the current average or
     * @throws RuntimeException if named average does not exist
     *
     */
    public long average(String name) {
        AverageData data = getAverageData(name, true);
        return (long) data.average;
    }

    /**
     * calls count("default")
     *
     * @return the number of measurements for "default" or
     * @throws RuntimeException if named average does not exist
     */
    public int count() {
        return count("default");
    }

    /**
     * gets the number of measurements for the named measurement
     *
     * @param name the name of the average to get the average for
     * @return the current average or
     * @throws RuntimeException if named average does not exist
     *
     */
    public int count(String name) {
        AverageData data = getAverageData(name, true);
        return data.count;
    }

    /**
     * calls averageDouble("default")
     *
     * @return the current average of "default" or
     * @throws RuntimeException if named average does not exist
     */
    public double averageDouble() {
        return averageDouble("default");
    }

    /**
     * gets the named average value as double
     *
     * @param name the name of the average to get the average for
     * @return the current average or
     * @throws RuntimeException if named average does not exist
     *
     */
    public double averageDouble(String name) {
        AverageData data = getAverageData(name, true);
        return data.average;
    }

    /**
     * calls max("default")
     *
     * @return the current average or
     * @throws RuntimeException if named average does not exist
     */
    public long max() {
        return max("default");
    }

    /**
     * gets the max value of all measurements
     *
     * @param name the name of the average to get the average for
     * @return the current average or
     * @throws RuntimeException if named average does not exist
     *
     */
    public long max(String name) {
        AverageData data = getAverageData(name, true);
        return data.max;
    }

    /**
     * calls min("default")
     *
     * @return the current average or
     * @throws RuntimeException if named average does not exist
     */
    public long min() {
        return min("default");
    }

    /**
     * gets the min value of all measurements
     *
     * @param name the name of the average to get the average for
     * @return the current average or
     * @throws RuntimeException if named average does not exist
     *
     */
    public long min(String name) {
        AverageData data = getAverageData(name, true);
        return data.min;
    }

    /**
     * calls reset("default")
     *
     * @throws RuntimeException if named average does not exist
     */
    public void reset() {
        reset("default");
    }

    /**
     * resets all measurements to the name average (average 0, max 0, min 0, count 0
     *
     * @param name the name of the average to get the average for
     * @throws RuntimeException if named average does not exist
     *
     */
    public void reset(String name) {
        AverageData data = getAverageData(name, true);
        data.count = 0;
        data.average = 0;
        data.max = 0;
        data.min = 0;
    }

    /**
     * calls remove("default")
     *
     * @return the curent average Object
     * @throws RuntimeException if named average does not exist
     */
    public AverageData remove() {
        return remove("default");
    }

    /**
     * removes the named average so that it is inknown for all future calls
     *
     * @param name the name of the average to remove
     * @return the curent average Object
     * @throws RuntimeException if named average does not exist
     */
    public AverageData remove(String name) {
        getAverageData(name, true);
        return map.remove(name);
    }

    /**
     * calls toString("default")
     *
     * @return the string representation of the named average Object or the string 'null' if it does not exist
     * @throws RuntimeException if named average does not exist
     */
    @Override
    public String toString() {
        return toString("default");
    }

    /**
     * String representation of the named average
     *
     * @param name the name of the average
     * @return the string representation of the named average Object or the string 'null' if it does not exist
     * @throws RuntimeException if named average does not exist
     */
    public String toString(String name) {
        AverageData data = map.get(name);
        if (data == null) {
            return "null";
        } else {
            return name + " " + data.toString();
        }
    }
}
