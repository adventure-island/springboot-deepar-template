
package com.tensorlab.ml.aws.sagemaker.deepar.json.requeset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "num_samples",
    "output_types",
    "quantiles"
})
public class Configuration {

    @JsonProperty("num_samples")
    private Integer numSamples;
    @JsonProperty("output_types")
    private List<String> outputTypes = null;
    @JsonProperty("quantiles")
    private List<String> quantiles = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("num_samples")
    public Integer getNumSamples() {
        return numSamples;
    }

    @JsonProperty("num_samples")
    public void setNumSamples(Integer numSamples) {
        this.numSamples = numSamples;
    }

    @JsonProperty("output_types")
    public List<String> getOutputTypes() {
        return outputTypes;
    }

    @JsonProperty("output_types")
    public void setOutputTypes(List<String> outputTypes) {
        this.outputTypes = outputTypes;
    }

    @JsonProperty("quantiles")
    public List<String> getQuantiles() {
        return quantiles;
    }

    @JsonProperty("quantiles")
    public void setQuantiles(List<String> quantiles) {
        this.quantiles = quantiles;
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
