package com.hoffi.minimal.microservices.microservice.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import annotations.TrivialTest;

class AverageDurationHelperTest {
    private static final Logger log = LoggerFactory.getLogger(AverageDurationHelperTest.class);


    @TrivialTest
    void failTests() {
        AverageDurationHelper averagesHelper = new AverageDurationHelper();
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.average();
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.average("doesnotexist");
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.averageDouble();
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.averageDouble("doesnotexist");
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.count();
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.count("doesnotexist");
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.getAverageData();
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.getAverageData("doesnotexist");
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.max();
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.max("doesnotexist");
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.min();
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.min("doesnotexist");
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.newAverage(-42l);
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.newAverage("doesnotexist", -42l);
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.remove();
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.remove("doesnotexist");
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.reset();
        });
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.reset("doesnotexist");
        });
        assertEquals("null", averagesHelper.toString(), "non existing named average should return string 'null'");
        assertEquals("null", averagesHelper.toString("doesnotexist"), "non existing named average should return string 'null'");

        Assertions.assertDoesNotThrow(() -> {
            averagesHelper.newAverage(42l);
        });
        Assertions.assertDoesNotThrow(() -> {
            averagesHelper.newAverage("doesnotexist", 42l);
        });
    }

    @TrivialTest
    void functionalTests() {
        AverageDurationHelper averagesHelper = new AverageDurationHelper();
        long avg = averagesHelper.newAverage(1000l);
        AverageDurationHelper.AverageData avgData = averagesHelper.getAverageData();
        assertEquals(1000l, avg);
        assertEquals(1000d, avgData.average);
        assertEquals(1, avgData.count);
        assertEquals(1000l, avgData.max);
        assertEquals(1000l, avgData.min);
        log.debug(averagesHelper.toString());
        double oldAverage = avgData.average;
        avg = averagesHelper.newAverage(1000l);
        avgData = averagesHelper.getAverageData();
        assertEquals(1000d, averagesHelper.average());
        assertEquals(2, averagesHelper.count());
        assertEquals(1000l, averagesHelper.max());
        assertEquals(1000l, averagesHelper.min());
        log.debug(averagesHelper.toString());
        oldAverage = avgData.average;
        avg = averagesHelper.newAverage(2000l);
        avgData = averagesHelper.getAverageData();
        assertEquals((long) ((2 * oldAverage) + 2000l) / 3, averagesHelper.average());
        assertEquals(((2 * oldAverage) + 2000l) / 3, averagesHelper.averageDouble());
        assertEquals(3, averagesHelper.count());
        assertEquals(2000l, averagesHelper.max());
        assertEquals(1000l, averagesHelper.min());
        log.debug(averagesHelper.toString());
        oldAverage = avgData.average;
        avg = averagesHelper.newAverage(2000l);
        avgData = averagesHelper.getAverageData();
        assertEquals((long) ((3 * oldAverage) + 2000l) / 4, averagesHelper.average());
        assertEquals(((3 * oldAverage) + 2000l) / 4, averagesHelper.averageDouble());
        assertEquals(4, averagesHelper.count());
        assertEquals(2000l, averagesHelper.max());
        assertEquals(1000l, averagesHelper.min());
        log.debug(averagesHelper.toString());
        oldAverage = avgData.average;
        avg = averagesHelper.newAverage(4000l);
        avgData = averagesHelper.getAverageData();
        assertEquals((long) ((4 * oldAverage) + 4000l) / 5, averagesHelper.average());
        assertEquals(((4 * oldAverage) + 4000l) / 5, averagesHelper.averageDouble());
        assertEquals(5, averagesHelper.count());
        assertEquals(4000l, averagesHelper.max());
        assertEquals(1000l, averagesHelper.min());
        log.debug(averagesHelper.toString());
        oldAverage = avgData.average;
        avg = averagesHelper.newAverage(500l);
        avgData = averagesHelper.getAverageData();
        assertEquals((long) ((5 * oldAverage) + 500l) / 6, averagesHelper.average());
        assertEquals(((5 * oldAverage) + 500l) / 6, averagesHelper.averageDouble());
        assertEquals(6, averagesHelper.count());
        assertEquals(4000l, averagesHelper.max());
        assertEquals(500l, averagesHelper.min());
        log.debug(averagesHelper.toString());

        averagesHelper.reset();
        avgData = averagesHelper.getAverageData();
        assertEquals(0l, averagesHelper.average());
        assertEquals(0d, averagesHelper.averageDouble());
        assertEquals(0, averagesHelper.count());
        assertEquals(0l, averagesHelper.max());
        assertEquals(0l, averagesHelper.min());
        averagesHelper.remove();
        Assertions.assertThrows(RuntimeException.class, () -> {
            averagesHelper.getAverageData();
        });

    }

}
