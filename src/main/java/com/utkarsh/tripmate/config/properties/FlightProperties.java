package com.utkarsh.tripmate.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "flight")
public class FlightProperties {

    private Amadeus amadeus = new Amadeus();

    public Amadeus getAmadeus() {
        return amadeus;
    }

    public void setAmadeus(Amadeus amadeus) {
        this.amadeus = amadeus;
    }

    public static class Amadeus {
        private String urlRoot;
        private String urlReferenceAirports;
        private String urlShopping;
        private String urlSecurity;
        private String clientId;
        private String clientSecret;

        public String getUrlRoot() {
            return urlRoot;
        }

        public void setUrlRoot(String urlRoot) {
            this.urlRoot = urlRoot;
        }

        public String getUrlReferenceAirports() {
            return urlReferenceAirports;
        }

        public void setUrlReferenceAirports(String urlReferenceAirports) {
            this.urlReferenceAirports = urlReferenceAirports;
        }

        public String getUrlShopping() {
            return urlShopping;
        }

        public void setUrlShopping(String urlShopping) {
            this.urlShopping = urlShopping;
        }

        public String getUrlSecurity() {
            return urlSecurity;
        }

        public void setUrlSecurity(String urlSecurity) {
            this.urlSecurity = urlSecurity;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }
}
