#include <WiFi.h>
#include <HTTPClient.h>
#include <DHT.h>
#include <time.h>
#include "arduino_secrets.h"

#define DHT_PIN 4
#define DHT_TYPE DHT22
#define SEND_INTERVAL_MS 30000UL

const char* DEVICE_ID = "Hum-01";
const char* MEASUREMENT_TYPE = "humidity";
const char* UNIT = "%";
DHT dht(DHT_PIN, DHT_TYPE);
unsigned long lastSend = 0;
bool registered = false;

void connectWifi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Ansluter till Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) { delay(500); Serial.print("."); }
  Serial.println();
  Serial.print("Ansluten, IP: "); Serial.println(WiFi.localIP());
}

String createTimestamp() {
  struct tm timeInfo;
  if (!getLocalTime(&timeInfo, 1000)) return "";
  char output[25];
  strftime(output, sizeof(output), "%Y-%m-%dT%H:%M:%SZ", &timeInfo);
  return String(output);
}

int postJson(const char* url, const String& json) {
  if (WiFi.status() != WL_CONNECTED) return -1;
  HTTPClient http;
  if (!http.begin(url)) return -1;
  http.addHeader("Content-Type", "application/json");
  int status=http.POST(json);
  Serial.printf("POST %s -> HTTP %d\n",url,status);
  if(status>0) Serial.println(http.getString());
  http.end();
  return status;
}

bool registerDevice() {
  String json="{\"deviceId\":\""+String(DEVICE_ID)+"\",\"measurementType\":\""+
      String(MEASUREMENT_TYPE)+"\",\"unit\":\""+String(UNIT)+"\"}";
  int status=postJson(DEVICE_REGISTRATION_URL,json);
  // 201 means new registration; 200 means it was already registered with matching settings.
  registered=(status==201 || status==200);
  return registered;
}

bool sendMeasurement(float humidity) {
  String timestamp=createTimestamp();
  if(timestamp.isEmpty()) { Serial.println("NTP-tiden saknas; mätningen skickas inte"); return false; }
  String json="{\"deviceId\":\""+String(DEVICE_ID)+"\",\"measurementType\":\""+
      String(MEASUREMENT_TYPE)+"\",\"value\":"+String(humidity,1)+",\"unit\":\""+
      String(UNIT)+"\",\"timestamp\":\""+timestamp+"\"}";
  return postJson(MEASUREMENT_API_URL,json)==201;
}

void setup() {
  Serial.begin(115200);
  dht.begin();
  connectWifi();
  configTime(0,0,"pool.ntp.org","time.google.com");
  delay(1500);
  registered=registerDevice();
  if(!registered) Serial.println("Enheten registrerades inte. Försök igen om 30 sekunder.");
}

void loop() {
  const unsigned long now=millis();
  if(lastSend==0 || now-lastSend>=SEND_INTERVAL_MS) {
    lastSend=now;
    if(WiFi.status()!=WL_CONNECTED) WiFi.reconnect();
    if(!registered) registered=registerDevice();
    if(registered) {
      float humidity=dht.readHumidity();
      if(isnan(humidity)) Serial.println("DHT22 gav inget giltigt luftfuktighetsvärde");
      else if(!sendMeasurement(humidity)) Serial.println("Mätningen kunde inte skickas");
    }
  }
  delay(50);
}
