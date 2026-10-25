package se.simon.iot.alarm.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.transform.dom.DOMResult;
import java.time.Instant;
import se.simon.iot.alarm.model.RegisteredDevice;
import se.simon.iot.alarm.repository.DeviceRepository;

@RestController
@RequestMapping("/api")
public class IntegrationController {
    private final DeviceRepository devices;
    private final WebServiceTemplate soap;
    @Value("${alarm.soap.url:http://localhost:8083/ws}") private String soapUrl;

    public IntegrationController(DeviceRepository devices, WebServiceTemplate soap) {
        this.devices=devices; this.soap=soap;
    }

    @PostMapping("/devices")
    public ResponseEntity<?> registerDevice(@RequestBody DeviceRegistrationRequest request) {
        if (request==null || blank(request.deviceId()) || blank(request.measurementType()) || blank(request.unit()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"deviceId, measurementType and unit are required");
        RegisteredDevice existing=devices.findById(request.deviceId()).orElse(null);
        if(existing!=null) {
            if(!existing.getMeasurementType().equalsIgnoreCase(request.measurementType()) || !existing.getUnit().equals(request.unit()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Device is already registered with different measurement settings");
            return ResponseEntity.ok(existing);
        }
        RegisteredDevice created=devices.save(new RegisteredDevice(request.deviceId(),request.measurementType(),request.unit()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/measurements")
    public ResponseEntity<MeasurementResponse> acceptMeasurement(@RequestBody MeasurementRequest measurement) {
        if (measurement==null || blank(measurement.deviceId()) || blank(measurement.measurementType()) ||
                measurement.value()==null || !Double.isFinite(measurement.value()) || blank(measurement.unit()) || blank(measurement.timestamp()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"All five measurement fields are required and value must be finite");
        RegisteredDevice device=devices.findById(measurement.deviceId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN,"Device is not registered"));
        if(!device.getMeasurementType().equalsIgnoreCase(measurement.measurementType()) || !device.getUnit().equals(measurement.unit()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Measurement does not match the registered device settings");
        try { Instant.parse(measurement.timestamp()); }
        catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"timestamp must be ISO-8601 UTC, for example 2026-10-25T12:30:45Z"); }

        String payload="<tns:ProcessMeasurementRequest xmlns:tns=\"urn:simon:iot:alarm:v1\">"+
                "<tns:deviceId>"+escape(measurement.deviceId())+"</tns:deviceId>"+
                "<tns:measurementType>"+escape(measurement.measurementType())+"</tns:measurementType>"+
                "<tns:value>"+measurement.value()+"</tns:value><tns:unit>"+escape(measurement.unit())+"</tns:unit>"+
                "<tns:timestamp>"+escape(measurement.timestamp())+"</tns:timestamp></tns:ProcessMeasurementRequest>";
        try {
            DOMResult result=new DOMResult();
            soap.sendSourceAndReceiveToResult(soapUrl,new StringSource(payload),result);
            Element response=parse(result);
            boolean created=Boolean.parseBoolean(text(response,"alarmCreated"));
            String id=optionalText(response,"alarmId");
            String message=optionalText(response,"message");
            return ResponseEntity.status(HttpStatus.CREATED).body(new MeasurementResponse(true,created,id,message));
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"SOAP alarm service call failed: "+ex.getMessage());
        }
    }

    private static boolean blank(String value){return value==null || value.isBlank();}
    private static String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    private static Element parse(DOMResult result) throws Exception {
        Document doc=(Document)result.getNode();
        NodeList nodes=doc.getElementsByTagNameNS("*","ProcessMeasurementResponse");
        if(nodes.getLength()==0) throw new IllegalStateException("SOAP response element ProcessMeasurementResponse was missing");
        return (Element)nodes.item(0);
    }
    private static String text(Element root,String name){String value=optionalText(root,name); if(value==null) throw new IllegalStateException("Missing SOAP response field "+name); return value;}
    private static String optionalText(Element root,String name){NodeList nodes=root.getElementsByTagNameNS("*",name); return nodes.getLength()==0?null:nodes.item(0).getTextContent().trim();}
    public record MeasurementResponse(boolean accepted, boolean alarmCreated, String alarmId, String message) {}
}
