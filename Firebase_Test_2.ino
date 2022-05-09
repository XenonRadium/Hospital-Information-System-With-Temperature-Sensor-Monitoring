#include <Arduino.h>
#include <WiFi.h>
#include <Firebase_ESP_Client.h>

//Provide the token generation process info.
#include "addons/TokenHelper.h"

//Provide the RTDB payload printing info and other helper functions.
#include "addons/RTDBHelper.h"

//Libraries needed for temperature sensor 
#include <OneWire.h>
#include <DallasTemperature.h>

//Libraries needed for Time and Date
#include <NTPClient.h>
#include <WiFiUdp.h>

//Device ID
#define DEVICE_UID "1X"

//Your WiFi credentials
#define WIFI_SSID "BernardBB"
#define WIFI_PASSWORD "Lolwtfhaha"

#define API_KEY "AIzaSyC01LEtbrc7ik2GBpfjfDh8NUY3YqOZ5zs"
#define DATABASE_URL "https://prototype-2fe1c-default-rtdb.firebaseio.com/"

//Firebase Realtime Database Object, Authentication Object, Configuration Object
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

//Device Location config
String device_location = "Living Room";

//Firebase database path
String databasePath = "";

//Firebase Unique Identifier
String fuid = "";

//Stores the elapsed time from device start up
unsigned long elapsedMillis = 0;

//The frequency of sensor updates to firebase, set to 10 seconds
unsigned long update_interval = 10000;

//Store device authentication status
bool isAuthenticated = false;

/*
 * The variables for the hardware
 */
//GPIO where the DS18B20 is connected to
const int oneWireBus = 4;
//Setup a oneWire instance to communicate with any OneWire devices
OneWire oneWire(oneWireBus);
//Pass out oneWire reference to Dallas Temperature sensor
DallasTemperature sensors(&oneWire);

//Define NTPClient to get tiem
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);

//Variables to save data and time
String formattedTime;
String timeStamp;
String currentDate;


//Wifi Function
void Wifi_Init() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-FI");
  while (WiFi.status() != WL_CONNECTED){
    Serial.print(".");
    delay(300);
  }
  Serial.println();
  Serial.print("Connected with IP: ");
  Serial.println(WiFi.localIP());
  Serial.println();
}



//Firebase Initiator
void firebase_init(){
  //configure firebase API Key
  config.api_key = API_KEY;
  //configure firebase realtime database url
  config.database_url = DATABASE_URL;
  Serial.println("-------------------------------------------");
  Serial.println("Sign up new user...");

  //Sign in to firebase Anonymously
  if (Firebase.signUp(&config, &auth, "", ""))
  {
    Serial.println("Success");
    isAuthenticated = true;

    //Set the database path where updates will be loaded for this device
    databasePath = "/" + device_location;
    fuid = auth.token.uid.c_str();
  }
  else{
    Serial.printf("Failed, %s\n", config.signer.signupError.message.c_str());

    isAuthenticated = false;
  }

  //Assign the callback function for the long runnign token generation task, see addons/TokenHelper.h
  config.token_status_callback = tokenStatusCallback;
  //Initialise the firebase library
  Firebase.begin(&config, &auth);
  //Enable WiFi reconnection
  Firebase.reconnectWiFi(true);
}



float get_reading(){
  //delay(update_interval);
  sensors.requestTemperatures();
  float temperatureC = sensors.getTempCByIndex(0);
  Serial.print(temperatureC);
  Serial.println("ºC");
  return temperatureC;
}



void database_test(){
  float reading = get_reading();
  if (Firebase.ready() && isAuthenticated) {
    // Put in temperature values into Firebase
    if ((Firebase.RTDB.setFloat(&fbdo, "Sensor/D-01/" + currentDate + "/" + timeStamp, reading))){
      Serial.println("PASSED");
      Serial.println("PATH: " + fbdo.dataPath());
      Serial.println("TYPE: " + fbdo.dataType());
    }
    else {
      Serial.println("FAILED");
      Serial.println("REASON: " + fbdo.errorReason());
    }
  }
}



void timeSetup(){
  //Initialize a NTPClient to get time
  timeClient.begin();
  //Set offset time in seconds to adjust for your timezone, for example:
  //GMT +8 for malaysia
  //8x60x60 = 28800 //TimeOffset
  timeClient.setTimeOffset(28800);
}

void getTimeDate(){
  if(!timeClient.update()){
    timeClient.forceUpdate();
  }
  //Format hh:mm:ss
  //Extract time
  formattedTime = timeClient.getFormattedTime();
  int splitT = formattedTime.indexOf("T");
  timeStamp = formattedTime.substring(0, splitT);
  Serial.print("Time: ");
  Serial.println(timeStamp);

  //seconds since 1,January,1970
  unsigned long epochTime = timeClient.getEpochTime();
  //Serial.print("Epoch Time: ");
  //Serial.print(epochTime);
  
  //Extract Date
  //Get a time structure
  struct tm *ptm = gmtime ((time_t *)&epochTime);

  int monthDay = ptm->tm_mday;
  int currentMonth = ptm->tm_mon+1;
  int currentYear = ptm->tm_year+1900;
  if(String(currentMonth).length() < 2 && String(monthDay).length() < 2 ){
    currentDate = String(currentYear) + "-0" + String(currentMonth) + "-0" + String(monthDay);
  }else if (String(currentMonth).length() < 2 ){
    currentDate = String(currentYear) + "-0" + String(currentMonth) + "-" + String(monthDay);
  }else if (String(monthDay).length() < 2 ){
    currentDate = String(currentYear) + "-" + String(currentMonth) + "-0" + String(monthDay);
  }

  Serial.print("Current date: ");
  Serial.println(currentDate);
}

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  sensors.begin();
  
  Wifi_Init();
  firebase_init();
  timeSetup();
}

void loop() {
  // put your main code here, to run repeatedly:
  getTimeDate();
  database_test();
  Serial.println("");
  delay(update_interval);
}
