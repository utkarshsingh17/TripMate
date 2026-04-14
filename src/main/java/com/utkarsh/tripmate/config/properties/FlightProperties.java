package com.utkarsh.tripmate.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "flight")
public class FlightProperties {

    private Aviationstack aviationstack = new Aviationstack();

    public Aviationstack getAviationstack() {
        return aviationstack;
    }

    public void setAviationstack(Aviationstack aviationstack) {
        this.aviationstack = aviationstack;
    }

    public static class Aviationstack {
        private String urlRoot;
        private String urlAirports;
        private String urlFlights;
        private String accessKey;

        public String getUrlRoot() {
            return urlRoot;
        }

        public void setUrlRoot(String urlRoot) {
            this.urlRoot = urlRoot;
        }

        public String getUrlAirports() {
            return urlAirports;
        }

        public void setUrlAirports(String urlAirports) {
            this.urlAirports = urlAirports;
        }

        public String getUrlFlights() {
            return urlFlights;
        }

        public void setUrlFlights(String urlFlights) {
            this.urlFlights = urlFlights;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }
    }
}
