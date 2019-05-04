package com.tensorlab.ml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.tensorlab.ml.aws.auth.Aws4SignatureKeyGenerator;
import com.tensorlab.ml.aws.auth.Aws4Signer;
import com.tensorlab.ml.aws.sagemaker.deepar.json.requeset.Configuration;
import com.tensorlab.ml.aws.sagemaker.deepar.json.requeset.DeepArRequest;
import com.tensorlab.ml.aws.sagemaker.deepar.json.requeset.Instance;
import com.tensorlab.ml.aws.sagemaker.deepar.json.response.DeepArResponse;
import com.tensorlab.ml.aws.sagemaker.deepar.json.response.Prediction;
import com.tensorlab.ml.aws.sagemaker.deepar.json.response.Quantiles;

import lombok.extern.slf4j.Slf4j;

/**
 * The main class used to process the predction request
 * 
 * @author JJ.Sun
 */
@Slf4j
@Service
public class LabManager implements InitializingBean {
	
	private String lastExistingDate;
	
	@Autowired
	private AppConfig appConfig;
	
	@Autowired
	private Aws4Signer signer;
	

	@Override
	public void afterPropertiesSet() throws Exception {
	    // do your initialization work here
	}
	
    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        int timeout = 60 * 1000;
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory =
          new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(timeout);
        return clientHttpRequestFactory;
    }
    
	/**
	 * Builds and sends the prediction request to the remote endpoint
	 * @param target The name used to locate the input file
	 * @return a {@link DeepArResponse} json object representing the predciton response data
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 */
	public DeepArResponse processPredictionRequest(String target) throws IOException {
		log.info("predict - start, target:{}", target);
		
		// Starting building the lengthy signing data
		
		// load the input file as a json string
	    String requestBody = constructJsonRequestObject(target);

	    // build the required headers
	    HttpHeaders headers = signer.buildHeadersWithAuthentication(requestBody);
	    
	    // Now we have the headers, send the request to the endpoint 
		DeepArResponse resp = null;
		RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());
		HttpEntity<String> entityPost = new HttpEntity<String>(requestBody, headers);
		
		String awsEndpointFullUrl = buildAwsEndpointFullUrl(target);
		
		log.info("Sending request to AWS DeepAR, url: {}", awsEndpointFullUrl);
		
		ResponseEntity<DeepArResponse> response = restTemplate.exchange(awsEndpointFullUrl, HttpMethod.POST, entityPost,
				DeepArResponse.class);

		log.debug("Result - status: " + response.getStatusCode());
		if (response.getStatusCode() == HttpStatus.OK) {
			resp = response.getBody();
			log.info("DeepArResponse received: {}", resp);
		}
		
		log.info("predict - done");
		return resp;
	}
	
	public String buildAwsEndpointFullUrl(String target) {
	    return "https://" + appConfig.getAwsAuthConfig().getServiceHost() + signer.buildEndpointUrl();
	}
	
	/**
	 * The entry point method of prediction request processing, this method locates the input file based on the specified target name,
	 * coverts the data in the input file to json object, generates authentication headers, sends the request to remove DeepAR endpoint,
	 * stores the response as json file and returns the plotted response data as javascript string
	 * 
	 * @param target The value used to locate which input file to use to build the request data
	 * @return the plotted response data represented by javascript
	 */
	public String predict(String target) {
		//default response
		String response = "Ooops, error occured :<";
		DeepArResponse deepArResponse = null;
		
		try {
			deepArResponse = processPredictionRequest(target);
		} catch (IOException e) {
			log.error("Error has occurred when processing the request: " + e, e);
			return response;
		}
		
		// parse and save the response data as json file, which will be loaded and rendered by Tablesaw
		String resposeFilePrefix = buildFilePrefix(target);
		try {
		    // save the response as a json file
			saveResponseAsJsonFile(deepArResponse, resposeFilePrefix+"-prediction-response.json");
			
			// convert the json response to csv file for plotting
			List<String> savedPredictionFiles = exportJsonResponseToCsv(deepArResponse, resposeFilePrefix, target);
			
			log.info("Generating response data for plotting, source file: {}", savedPredictionFiles.get(0));
			String plotTitle = "DeepAR Prediction for target " + target;
			
			// plot the csv data, first category only for now
			response = PlotUtil.plotTimeSeriesToHtml(plotTitle, savedPredictionFiles.get(0));
		} catch (IOException e) {
			log.error("Error has occurred when processing the response: " + e, e);
		}
		
		return response;
	}
	
	public String buildFilePrefix(String target) {
		return String.format("%s-%s", target, signer.getCurrentLocalTimestamp());
	}
	
	public void saveResponseAsJsonFile(DeepArResponse resp, String flieName) throws JsonGenerationException, JsonMappingException, IOException {
		log.info("Saving response to file: {}", flieName);
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
		writer.writeValue(new File(flieName), resp);
	}
	
	public List<String> exportJsonResponseToCsv(DeepArResponse resp, String filePrefix, String target) throws IOException {
		// we need to export each category to separate csv file for plotting
		List<String> savedPredictionFile = new ArrayList<>();
		int categoryIdx = 0;
		List<Prediction> predictions= resp.getPredictions();
		for(; categoryIdx < predictions.size(); categoryIdx++) {
			Prediction pred = predictions.get(categoryIdx);
			String absFilePath = exportCategoryPredictionToCsv(pred, categoryIdx, filePrefix, target);
			savedPredictionFile.add(absFilePath);
		}
		
		return savedPredictionFile;
	}
	
	public void exportCategoryPredictionToCsv(Prediction p, int category) throws IOException {
		List<Double> mean = p.getMean();
		Quantiles quantiles = p.getQuantiles();
		List<Double> quantile01 = quantiles.get01();
		List<Double> quantile05 = quantiles.get05();
		List<Double> quantile09 = quantiles.get09();
		LocalDate lastDate = LocalDate.parse(lastExistingDate);
		
		log.debug("exportCategoryPredictionToCsv - lastDate: {}", lastDate);
		
		String fileName = String.format("prediction-category-%d.csv", category);
		
		log.debug("exportCategoryPredictionToCsv - fileName: {}", fileName);
		
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName));

        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
        		"date", "mean", "quantile-01", "quantile-05", "quantile-09"));
        
        for(int i = 0; i < mean.size(); i++) {
        	LocalDate currentPredictedDate = lastDate.plusDays(1+i);
        	log.debug("exportCategoryPredictionToCsv - currentPredictedDate: {}", currentPredictedDate);
        	csvPrinter.printRecord(
        			currentPredictedDate.toString(), 
        			mean.get(i), 
        			quantile01.get(i), 
        			quantile05.get(i),
        			quantile09.get(i));
        }
        
        csvPrinter.flush();
	
	}
	
	public String exportCategoryPredictionToCsv(Prediction p, int category, String filePrefix, String target) throws IOException {
		List<Double> mean = p.getMean();
		Quantiles quantiles = p.getQuantiles();
		List<Double> quantile01 = quantiles.get01();
		List<Double> quantile05 = quantiles.get05();
		List<Double> quantile09 = quantiles.get09();
		LocalDate lastDate = LocalDate.parse(lastExistingDate);
		
		log.debug("exportCategoryPredictionToCsv - lastDate: {}", lastDate);
		
		String fileName = String.format("%s-prediction-category-%d.csv", filePrefix, category);
		
		log.debug("exportCategoryPredictionToCsv - fileName: {}", fileName);
		
		Path filePath = Paths.get(fileName);
		
		log.info("exportCategoryPredictionToCsv - File to save: {}", filePath.toAbsolutePath());
		
		BufferedWriter writer = Files.newBufferedWriter( filePath );
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("date", "value", "category"));
        
        //load validation data
        Reader in = new FileReader( ResourceUtils.getFile(buildValidationInputFilePath(target)) );
		
		Iterable<CSVRecord> validationRecords = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
		List<CSVRecord> validationRecordList = StreamSupport.stream(validationRecords.spliterator(), false).collect(Collectors.toList());
		
        for(int i = 0; i < mean.size(); i++) {
        	LocalDate currentPredictedDate = lastDate.plusDays(1+i);
        	log.debug("exportCategoryPredictionToCsv - currentPredictedDate: {}", currentPredictedDate);
        	csvPrinter.printRecord(
        			currentPredictedDate.toString(), 
        			quantile01.get(i), 
        			ValueCategory.QUANTILE_01.getValue());
        	csvPrinter.printRecord(
        			currentPredictedDate.toString(), 
        			quantile05.get(i), 
        			ValueCategory.QUANTILE_05.getValue());
        	csvPrinter.printRecord(
        			currentPredictedDate.toString(), 
        			quantile09.get(i), 
        			ValueCategory.QUANTILE_09.getValue());
        	
        	if( i < validationRecordList.size()) {
            	CSVRecord validateRec = validationRecordList.get(i);
            	if(validateRec != null) {
                	csvPrinter.printRecord(
                			currentPredictedDate.toString(), 
                			category == 0? validateRec.get("category0"):validateRec.get("category1"), 
                			ValueCategory.ACTUAL.getValue());
            	}
        	}

        }
        
        csvPrinter.flush();
        
        return filePath.toAbsolutePath().toString();
	}
	
	/**
	 * Builds DeepAR request string with json format from input file
	 * @param target The tag used to locate the input file, the input file name should have
	 * pattern 'prediction-input-<target>.csv'
	 * @return
	 * @throws IOException
	 */
	public String constructJsonRequestObject(String target) throws IOException {
		List<Integer> cat0ValueList = new ArrayList<>();
		List<Integer> cat1ValueList = new ArrayList<>();
		String startDateTime = null;
		
		Reader in = new FileReader( ResourceUtils.getFile(buildPredictionInputFilePath(target)) );
		Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
		
		//convert to lists of values which will be used to construct the 'Instance' elements of the json request object
		//Note - You might need to customize code below based on how many categories you have for you input data, you 
		//can also extend the code to support input features if you want
		List<CSVRecord> recList = StreamSupport.stream(records.spliterator(), false).collect(Collectors.toList());
		for(int i = 0; i < recList.size(); i++) {
			CSVRecord record = recList.get(i);
			String datetime = record.get("datetime");
		    String valueOfCategory0 = record.get("category0");
		    String valueOfCategory1 = record.get("category1");
			if(i == 0) {
				//Remember the start time, which is needed to build the request
				startDateTime = datetime;
				log.info("constructJsonRequestObject - startDateTime: {}", startDateTime);
			}
			if(i == recList.size()-1) {
				this.lastExistingDate = datetime;
				log.info("constructJsonRequestObject - lastExistingDate: {}", lastExistingDate);
			}
			
		    cat0ValueList.add(Integer.valueOf(valueOfCategory0));
		    cat1ValueList.add(Integer.valueOf(valueOfCategory1));
		}
		
		DeepArRequest req = new DeepArRequest();
		List<Instance> insts = new ArrayList<>();
		
		//set cat 0 
		Instance inst = new Instance();
		inst.setStart(startDateTime);
		List<Integer> cat = new ArrayList<>();
		cat.add(0);
		inst.setCat(cat);
		inst.setTarget(cat0ValueList);
		insts.add(inst);
		
		//set cat 1
		inst = new Instance();
		inst.setStart(startDateTime);
		cat = new ArrayList<>();
		cat.add(1);
		inst.setCat(cat);
		inst.setTarget(cat1ValueList);
		insts.add(inst);
		
		req.setInstances(insts);
		
		//set configuration data
		Configuration config = new Configuration();
		config.setNumSamples(50);
		
		List<String> quantiles = new ArrayList<>();
		quantiles.add("0.1");
		quantiles.add("0.5");
		quantiles.add("0.9");
		
		config.setQuantiles(quantiles);
		
		List<String> outputTypes = new ArrayList<>();
		outputTypes.add("mean");
		outputTypes.add("quantiles");
		outputTypes.add("samples");
		
		config.setOutputTypes(outputTypes);
		req.setConfiguration(config);
		
		ObjectMapper objectMapper = new ObjectMapper();
		String result = objectMapper.writeValueAsString(req);
		
		log.debug("constructJsonRequestObject result: {}", result);
		
		return result;
	}
	
    public String buildPredictionInputFilePath(String target) {
        return String.format("classpath:prediction-input-%s.csv", target);
    }
    
    public String buildValidationInputFilePath(String target) {
        return String.format("classpath:validation-%s.csv", target);
    }
}
