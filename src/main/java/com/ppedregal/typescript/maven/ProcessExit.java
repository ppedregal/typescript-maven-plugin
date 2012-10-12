package com.ppedregal.typescript.maven;

public class ProcessExit extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4226299808145715682L;
	
	private final int status;
	
	public ProcessExit() {
		this(0);
	}
	
	public ProcessExit(int status){
		this.status=status;
	}
	
	public int getStatus() {
		return status;
	}

}
