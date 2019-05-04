
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
    "0.1",
    "0.9",
    "0.5"
})
public class Quantiles {

    @JsonProperty("0.1")
    private List<Double> _01 = null;
    @JsonProperty("0.9")
    private List<Double> _09 = null;
    @JsonProperty("0.5")
    private List<Double> _05 = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("0.1")
    public List<Double> get01() {
        return _01;
    }

    @JsonProperty("0.1")
    public void set01(List<Double> _01) {
        this._01 = _01;
    }

    @JsonProperty("0.9")
    public List<Double> get09() {
        return _09;
    }

    @JsonProperty("0.9")
    public void set09(List<Double> _09) {
        this._09 = _09;
    }

    @JsonProperty("0.5")
    public List<Double> get05() {
        return _05;
    }

    @JsonProperty("0.5")
    public void set05(List<Double> _05) {
        this._05 = _05;
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
