package com.utkarsh.tripmate.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "currency-exchange")
public class CurrencyExchangeProperties {

    private VatComply vatComply = new VatComply();

    public VatComply getVatComply() {
        return vatComply;
    }

    public void setVatComply(VatComply vatComply) {
        this.vatComply = vatComply;
    }

    public static class VatComply {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
