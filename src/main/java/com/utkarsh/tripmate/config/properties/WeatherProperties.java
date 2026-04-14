package com.utkarsh.tripmate.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "weather")
public class WeatherProperties {

    private VisualCrossing visualcrossing = new VisualCrossing();

    public VisualCrossing getVisualcrossing() {
        return visualcrossing;
    }

    public void setVisualcrossing(VisualCrossing visualcrossing) {
        this.visualcrossing = visualcrossing;
    }

    public static class VisualCrossing {
        private String url;
        private String apiKey;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
