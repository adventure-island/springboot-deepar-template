package com.tensorlab.ml;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class WebController {

	@Autowired
	private LabManager labManager;
	
	/**
	 * Process the prediction request based on the specified target name
	 * @param target The name of the target to predict, the value will be used to 
	 * construct the name of the file containing the input data for the DeepAR model,
	 * with format like this: prediction-input-[TARGET].csv, 
	 * see {@link LabManager#buildPredictionInputFilePath(String)} for more details
	 * @param request
	 * @return the javascript string containing prediction graph
	 * @throws Exception
	 */
	@RequestMapping(value="/predict/sagemaker/deepar", method=RequestMethod.GET)
	public String predict(
			@RequestParam("target") String target, 
			HttpServletRequest request
			) throws Exception {
		log.info("Request received, prediction target name: {}", target);
		return labManager.predict(target);
	}
}