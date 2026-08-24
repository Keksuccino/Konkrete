package de.keksuccino.konkrete.web;

interface InternetAvailabilityProbe extends AutoCloseable {

    boolean isAvailable() throws Exception;

    @Override
    void close();

}
