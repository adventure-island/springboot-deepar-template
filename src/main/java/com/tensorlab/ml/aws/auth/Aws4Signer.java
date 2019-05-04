package com.tensorlab.ml.aws.auth;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.tensorlab.ml.AppConfig;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * This class wraps AWS4 signing logic. The signing details were implemented
 * based on <a href=
 * "https://docs.aws.amazon.com/general/latest/gr/sigv4-signed-request-examples.html">
 * Examples of the Complete Version 4 Signing Process (Python)</a>
 * 
 * @author JJ.Sun
 */
@Slf4j
@Service
public class Aws4Signer {
	private final static String REQUEST_CONTENT_TYPE = "application/json";
	private final static String AUTH_ALGORITHM = "AWS4-HMAC-SHA256";
	private final static String REQUEST_METHOD = "POST";

	@Data
	class AuthenticationData {
		@NonNull
		String timestamp;
		@NonNull
		String date;
		@NonNull
		String payloadHash;
		@NonNull
		String authorizationHeader;
	}

	@Autowired
	private AppConfig appConfig;

	/**
	 * Gets the timestamp in YYYYMMDD'T'HHMMSS'Z' format, which is the required
	 * format for AWS4 signing request headers and credential string
	 * 
	 * @param dateTime
	 *            an OffsetDateTime object representing the UTC time of current
	 *            signing request
	 * @return the formatted timestamp string
	 * 
	 * @see <a href=
	 *      "https://docs.aws.amazon.com/general/latest/gr/sigv4-signed-request-examples.html">
	 *      Examples of the Complete Version 4 Signing Process (Python)</a>
	 */
	public String getTimeStamp(OffsetDateTime dateTime) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
		String formatDateTime = dateTime.format(formatter);
		return formatDateTime;
	}

	/**
	 * Gets the date string in yyyyMMdd format, which is required to build the
	 * credential scope string
	 * 
	 * @return the formatted date string
	 */
	public String getDate(OffsetDateTime dateTime) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		String formatDateTime = dateTime.format(formatter);
		return formatDateTime;
	}

	/**
	 * Get current timestamp with local timezone in yyyyMMddHHmmss format
	 * @return the formatted timestamp string
	 */
	public String getCurrentLocalTimestamp() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		LocalDateTime now = LocalDateTime.now();
		return now.format(formatter);
	}

	public byte[] generateAws4SigningKey(String timestamp) {
		String secretKey = appConfig.getAwsAuthConfig().getSecretKey();
		String regionName = appConfig.getAwsAuthConfig().getServiceRegion();
		String serviceName = appConfig.getAwsAuthConfig().getServiceName();

		byte[] signatureKey = null;
		try {
			signatureKey = Aws4SignatureKeyGenerator.generateSignatureKey(secretKey, timestamp, regionName,
					serviceName);
		} catch (Exception e) {
			log.error("An error has ocurred when generate signature key: " + e, e);
		}

		return signatureKey;
	}

	public String buildEndpointUrl() {
		return String.format("/endpoints/%s/invocations", appConfig.getAwsAuthConfig().getServiceEndPoint());
	}

	public HttpHeaders buildHeadersWithAuthentication(String requestBody) {
		HttpHeaders headers = null;
		try {
			AuthenticationData authData = buildAuthorizationData(requestBody);
			headers = createAwsSagemakerRequestHeaders(authData);
		} catch (InvalidKeyException | NoSuchAlgorithmException | UnsupportedEncodingException | SignatureException
				| IllegalStateException e) {
			log.error("An error has ocurred when building authentication data: " + e, e);
		}

		return headers;
	}

	/**
	 * Builds an {@link AuthenticationData} object containing the timestamp, date,
	 * payload hash and the AWS4 signature
	 * <p>
	 * 
	 * The signing logic was translated from the Python implementation, see this
	 * link for more details: <a href=
	 * "https://docs.aws.amazon.com/general/latest/gr/sigv4-signed-request-examples.html">Examples
	 * of the Complete Version 4 Signing Process (Python)</a>
	 * 
	 * @param target
	 * @param requestBody
	 * 
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 * @throws IllegalStateException
	 * 
	 */
	public AuthenticationData buildAuthorizationData(String requestBody) throws NoSuchAlgorithmException,
			UnsupportedEncodingException, InvalidKeyException, SignatureException, IllegalStateException {
		log.info("predict - start");

		// Starting building the lengthy signing data
		AppConfig.AwsAuthConfig awsAuthConfig = appConfig.getAwsAuthConfig();
		String payloadHash = Hmac.getSha256Hash(requestBody);

		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		String timestamp = getTimeStamp(now);
		String date = getDate(now);

		// Step 1 is to define the verb (GET, POST, etc.) -- already done by defining
		// constant REQUEST_METHOD

		// Step 2: Create canonical URI--the part of the URI from domain to query
		// string (use '/' if no path)
		String canonical_uri = buildEndpointUrl();

		// Step 3: Create the canonical query string. In this example, request
		// parameters are passed in the body of the request and the query string
		// is blank.
		String canonical_querystring = "";

		// Step 4: Create the canonical headers. Header names must be trimmed
		// and lowercase, and sorted in code point order from low to high.
		// Note that there is a trailing \n.
		String canonical_headers = "content-type:" + REQUEST_CONTENT_TYPE + "\n" + "host:"
				+ awsAuthConfig.getServiceHost() + "\n" + "x-amz-content-sha256:" + payloadHash + "\n" + "x-amz-date:"
				+ timestamp + "\n";
		String signed_headers = "content-type;host;x-amz-content-sha256;x-amz-date";

		log.debug("canonical_headers : {}", canonical_headers);

		String canonical_request = REQUEST_METHOD + "\n" + canonical_uri + "\n" + canonical_querystring + "\n"
				+ canonical_headers + "\n" + signed_headers + "\n" + payloadHash;

		log.debug("canonical_request : {}", canonical_request);

		String credential_scope = date + "/" + awsAuthConfig.getServiceRegion() + "/" + awsAuthConfig.getServiceName()
				+ "/" + "aws4_request";
		String canonical_request_hash = Hmac.getSha256Hash(canonical_request);

		log.debug("canonical_request_hash : {}", canonical_request_hash);

		String string_to_sign = AUTH_ALGORITHM + "\n" + timestamp + "\n" + credential_scope + "\n"
				+ canonical_request_hash;

		log.debug("string_to_sign : {}", string_to_sign);
		byte[] sigKey = generateAws4SigningKey(date);

		String signature = Hmac.calculateHMAC(string_to_sign, sigKey, Hmac.HMAC_SHA256);
		String authorization_header = AUTH_ALGORITHM + " " + "Credential=" + awsAuthConfig.getAccessKey() + "/"
				+ credential_scope + ", " + "SignedHeaders=" + signed_headers + ", " + "Signature=" + signature;

		log.debug("authorization_header : {}", authorization_header);

		return new AuthenticationData(timestamp, date, payloadHash, authorization_header);
	}

	/**
	 * Creates the HTTP headers for the prediction request
	 * 
	 * @param authData
	 *            the {@link AuthenticationData} object containing required
	 *            authentication data for build the headers
	 * @return an {@link org.springframework.http.HttpHeaders} object that contains
	 *         all the required headers
	 */
	private HttpHeaders createAwsSagemakerRequestHeaders(AuthenticationData authData) {
		java.util.List<MediaType> accepts = Arrays.asList(MediaType.APPLICATION_JSON);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Host", appConfig.getAwsAuthConfig().getServiceHost());
		headers.add("X-Amz-Date", authData.getTimestamp());
		headers.add("X-Amz-Content-Sha256", authData.getPayloadHash());
		headers.add("Authorization", authData.getAuthorizationHeader());
		headers.setAccept(accepts);
		return headers;
	}

}
