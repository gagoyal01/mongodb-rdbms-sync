package com.cisco.app.dbmigrator.migratorapp.utilities.encrypt;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import ch.qos.logback.core.encoder.ByteArrayUtil;

public final class EncryptorDecryptor {
	private static final String ALGORITHM = "AES";
	private static final byte[] keyBytes = new byte[] { '$','^','9','(','U','8','!','1','8','!','@' ,'#','3','9','$','@'};

	public static byte[] encrypt(String valueToEnc, String passPhrase) throws Exception {
	    Key key = generateKey(passPhrase);
	    Cipher c = Cipher.getInstance(ALGORITHM);
	    c.init(Cipher.ENCRYPT_MODE, key);
	    byte[] encValue = c.doFinal(valueToEnc.getBytes());
	    return encValue;
	}
	
	public static String encryptToString(String valueToEnc , String passPhrase) throws Exception {
	    return ByteArrayUtil.toHexString(encrypt(valueToEnc, passPhrase));
	}

	public static String decrypt(byte[] encryptedValue , String passPhrase) throws Exception {
	    Key key = generateKey(passPhrase);
	    Cipher c = Cipher.getInstance(ALGORITHM);
	    c.init(Cipher.DECRYPT_MODE, key);
	    byte[] decrypted = c.doFinal(encryptedValue);
	    return new String(decrypted);
	}
	
	public static String decrypt(String encryptedPwd , String passPhrase) throws Exception {
		return decrypt(ByteArrayUtil.hexStringToByteArray(encryptedPwd), passPhrase);
	}
	
	private static Key generateKey(String passPhrase) throws Exception {
	    Key key = new SecretKeySpec(getKeyBytes(passPhrase), ALGORITHM);
	    return key;
	}
	
	private static byte[] getKeyBytes(String passPhrase) throws Exception{
		if(passPhrase==null||passPhrase.isEmpty()){
			return keyBytes;
		}
		if(passPhrase.length()==16){
			return passPhrase.getBytes();
		}else{
			throw new Exception("Encryption key must be of 16 chars");
		}
	}
	
	public static void main(String [] args) throws Exception{
		String plainText = "ccwM1CDdevApp";
		String hex = encryptToString(plainText , "#$Y9K39KRYP8K3Y#");
		System.out.println(hex);
		String str = decrypt(hex , "#$Y9K39KRYP8K3Y#");
		System.out.println(str);
	}
}