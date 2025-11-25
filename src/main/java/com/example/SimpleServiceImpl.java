package com.example;

import jakarta.jws.WebService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.soap.SOAPBinding;

// Annotation หลักที่ระบุว่านี่คือ Web Service
@WebService(serviceName = "SimpleService", // ชื่อของ Service ใน WSDL
		portName = "SimpleServicePort", //port name
		targetNamespace = "http://example.com/simple" // Namespace สำหรับ Service
)
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class SimpleServiceImpl {

	// Annotation ที่ระบุว่านี่คือเมธอดที่เปิดเผยเป็น Web Operation
	@WebMethod(operationName = "greetUser")
	public String sayHi(@WebParam(name = "userName") String name) {
		System.out.println("Invoked greetUser with: " + name);
		return "Greetings, " + name + "! (POJO Style)";
	}

//	// เมธอดนี้จะไม่ถูกเปิดเผยเป็น Web Service Operation
//	public int internalHelper() {
//		return 42;
//	}

}