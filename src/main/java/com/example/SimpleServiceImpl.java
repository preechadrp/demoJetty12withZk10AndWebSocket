package com.example;

public class SimpleServiceImpl implements SimpleService {

	public String sayHi(String userName) {
		System.out.println("Invoked greetUser with: " + userName);
		return "Greetings, " + userName + "! (POJO Style)";
	}

	// เมธอดนี้จะไม่ถูกเปิดเผยเป็น Web Service Operation
	public int internalHelper() {
		return 42;
	}

}