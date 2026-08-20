package de.keksuccino.konkrete.util;

interface InternetAvailabilityProbe extends AutoCloseable {

    boolean isAvailable() throws Exception;

    @Override
    void close();

}
