package com.tensorlab.ml.aws.auth;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Formatter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class used to generate hash and sign data
 * @author JJ.Sun
 */
@Slf4j
public class Hmac {
	public static final String HMAC_SHA512 = "HmacSHA512";
	public static final String HMAC_SHA256 = "HmacSHA256";
	
	public static String getSha256Hash(String input) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-256"); 
        byte[] messageDigest = md.digest(input.getBytes("UTF-8"));
        return toHexString(messageDigest);
	}
	
	public static String calculateHMAC(String data, byte[] key, String method)
			throws SignatureException, NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException {
		log.debug("calculateHMAC - input data: {}", data);
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, method);
		Mac mac = Mac.getInstance(method);
		mac.init(secretKeySpec);
		return toHexString(mac.doFinal(data.getBytes("UTF-8")));
	}

	private static String toHexString(byte[] bytes) {
		Formatter formatter = new Formatter();
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		String s = formatter.toString();
		formatter.close();
		return s;
	}

}
