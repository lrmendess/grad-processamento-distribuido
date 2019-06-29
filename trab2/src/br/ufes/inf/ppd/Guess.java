package br.ufes.inf.ppd;

/**
 * Guess.java
 */
import java.io.Serializable;
import java.util.Base64;

import org.json.JSONObject;

@SuppressWarnings("serial")
public class Guess implements Serializable {
	
//	Chave candidata
	private String key;

//	Mensagem decriptografada com a chave candidata
	private byte[] message;

	public Guess (JSONObject obj) {
		key = obj.getString("key");
		message = Base64.getDecoder().decode(obj.getString("message"));
	}
	
	public Guess(String key, byte[] message) {
		this.key = key;
		this.message = message;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public byte[] getMessage() {
		return message;
	}

	public void setMessage(byte[] message) {
		this.message = message;
	}
	
	public JSONObject toJson() {
		JSONObject obj = new JSONObject();
		
		obj.put("key", key);
		obj.put("message", new String(Base64.getEncoder().encode(message)));
		
		return obj;
	}

}
