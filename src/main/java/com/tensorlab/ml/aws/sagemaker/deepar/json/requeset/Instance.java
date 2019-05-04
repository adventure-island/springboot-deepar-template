
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
    "start",
    "target",
    "cat"
})
public class Instance {

    @JsonProperty("start")
    private String start;
    @JsonProperty("target")
    private List<Integer> target = null;
    @JsonProperty("cat")
    private List<Integer> cat = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("start")
    public String getStart() {
        return start;
    }

    @JsonProperty("start")
    public void setStart(String start) {
        this.start = start;
    }

    @JsonProperty("target")
    public List<Integer> getTarget() {
        return target;
    }

    @JsonProperty("target")
    public void setTarget(List<Integer> target) {
        this.target = target;
    }

    @JsonProperty("cat")
    public List<Integer> getCat() {
        return cat;
    }

    @JsonProperty("cat")
    public void setCat(List<Integer> cat) {
        this.cat = cat;
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
