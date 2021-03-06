package edu.upc.eetac.dsa.dsaqt1415g4.uTroll.api.model;

public class uTrollError {
	private int status; // Estado del error, 4XX o 5XX
	private String message;

	public uTrollError() {
		super();
	}

	public uTrollError(int status, String message) {
		super();
		this.status = status;
		this.message = message;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
