package se.simon.iot.alarm.endpoint;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import se.simon.iot.alarm.model.AlarmEntity;
import se.simon.iot.alarm.service.AlarmService;

@Endpoint
public class AlarmEndpoint {
    private static final String NS = "urn:simon:iot:alarm:v1";
    private final AlarmService service;
    public AlarmEndpoint(AlarmService service) { this.service=service; }

    @PayloadRoot(namespace=NS, localPart="ProcessMeasurementRequest")
    @ResponsePayload
    public Source processMeasurement(@RequestPayload Source request) {
        Element root=parse(request);
        String deviceId=text(root,"deviceId");
        String type=text(root,"measurementType");
        String unit=text(root,"unit");
        double value;
        Instant timestamp;
        try { value=Double.parseDouble(text(root,"value")); }
        catch (RuntimeException ex) { throw new ContractFault("value must be numeric"); }
        try { timestamp=OffsetDateTime.parse(text(root,"timestamp")).toInstant(); }
        catch (RuntimeException ex) { throw new ContractFault("timestamp must be an ISO-8601 date-time"); }
        AlarmEntity alarm=service.evaluate(deviceId,type,value,unit,timestamp);
        String id=alarm==null?"":"<alarmId>"+alarm.getId()+"</alarmId>";
        String msg=alarm==null?"No alarm created; value is within the allowed limit":alarm.getMessage();
        return xml("<ProcessMeasurementResponse><alarmCreated>"+(alarm!=null)+"</alarmCreated><message>"+escape(msg)+"</message>"+id+"</ProcessMeasurementResponse>");
    }

    @PayloadRoot(namespace=NS, localPart="GetAlarmRequest")
    @ResponsePayload
    public Source getAlarm(@RequestPayload Source request) {
        long id=longValue(parse(request),"alarmId");
        AlarmEntity alarm=service.find(id);
        if (alarm==null) return xml("<GetAlarmResponse><found>false</found><message>Alarm not found</message></GetAlarmResponse>");
        return xml("<GetAlarmResponse><found>true</found>"+alarmFields(alarm)+"</GetAlarmResponse>");
    }

    @PayloadRoot(namespace=NS, localPart="GetAllAlarmsRequest")
    @ResponsePayload
    public Source getAllAlarms(@RequestPayload Source request) {
        List<AlarmEntity> alarms=service.findAll();
        StringBuilder body=new StringBuilder("<GetAllAlarmsResponse>");
        for (AlarmEntity alarm:alarms) body.append("<alarms>").append(alarmFields(alarm)).append("</alarms>");
        body.append("</GetAllAlarmsResponse>");
        return xml(body.toString());
    }

    private static Element parse(Source source) {
        try {
            DOMResult result=new DOMResult();
            TransformerFactory.newInstance().newTransformer().transform(source,result);
            Document doc=(Document)result.getNode();
            return doc.getDocumentElement();
        } catch (Exception ex) { throw new ContractFault("Unable to parse SOAP XML"); }
    }
    private static String text(Element parent,String name) {
        NodeList nodes=parent.getElementsByTagNameNS("*",name);
        if (nodes.getLength()==0) throw new ContractFault("Missing required field: "+name);
        String value=nodes.item(0).getTextContent().trim();
        if(value.isEmpty()) throw new ContractFault("Required field is empty: "+name);
        return value;
    }
    private static long longValue(Element root,String name) {
        try { return Long.parseLong(text(root,name)); }
        catch (NumberFormatException ex) { throw new ContractFault(name+" must be an integer"); }
    }
    private static String alarmFields(AlarmEntity a) {
        return "<alarmId>"+a.getId()+"</alarmId><deviceId>"+escape(a.getDeviceId())+"</deviceId>"+
                "<measurementType>"+escape(a.getMeasurementType())+"</measurementType><value>"+a.getValue()+"</value>"+
                "<unit>"+escape(a.getUnit())+"</unit><createdAt>"+a.getCreatedAt()+"</createdAt><message>"+escape(a.getMessage())+"</message>";
    }
    private static Source xml(String body) {
        int start=body.indexOf('>')+1;
        int end=body.lastIndexOf('<');
        String root=body.substring(1, body.indexOf('>'));
        String inner=body.substring(start,end);
        String response="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
                "<"+root+" xmlns=\""+NS+"\">"+inner+"</"+root+">";
        return new StringSource(response);
    }
    private static String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");
    }
}
