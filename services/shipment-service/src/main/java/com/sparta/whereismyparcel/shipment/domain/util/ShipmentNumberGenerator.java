package com.sparta.whereismyparcel.shipment.domain.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class ShipmentNumberGenerator {

    private static final String PREFIX = "SHP";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private ShipmentNumberGenerator() {
        // util class
    }

    public static String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);

        return PREFIX + "-" + timestamp + "-" + random;
    }
}