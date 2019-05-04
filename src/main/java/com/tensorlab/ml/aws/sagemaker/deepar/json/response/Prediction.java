
package com.tensorlab.ml.aws.sagemaker.deepar.json.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.ToString;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "quantiles",
    "samples",
    "mean"
})
public class Prediction {

    @JsonProperty("quantiles")
    private Quantiles quantiles;
    @JsonProperty("samples")
    private List<List<Double>> samples = null;
    @JsonProperty("mean")
    private List<Double> mean = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("quantiles")
    public Quantiles getQuantiles() {
        return quantiles;
    }

    @JsonProperty("quantiles")
    public void setQuantiles(Quantiles quantiles) {
        this.quantiles = quantiles;
    }

    @JsonProperty("samples")
    public List<List<Double>> getSamples() {
        return samples;
    }

    @JsonProperty("samples")
    public void setSamples(List<List<Double>> samples) {
        this.samples = samples;
    }

    @JsonProperty("mean")
    public List<Double> getMean() {
        return mean;
    }

    @JsonProperty("mean")
    public void setMean(List<Double> mean) {
        this.mean = mean;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
