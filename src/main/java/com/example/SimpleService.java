package com.example;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

@WebService(serviceName = "SimpleService", // ชื่อของ Service ใน WSDL
		portName = "SimpleServicePort", //port name
		targetNamespace = "http://example.com/simple" // Namespace สำหรับ Service
)
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface SimpleService {

	@WebMethod(operationName = "greetUser")
	public String sayHi(@WebParam(name = "userName") String userName);

}
