package se.simon.iot.alarm.endpoint;

import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;
import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;

@SoapFault(faultCode=FaultCode.CLIENT, faultStringOrReason="Invalid or unsupported request")
public class ContractFault extends RuntimeException {
    public ContractFault(String message) { super(message); }
}
