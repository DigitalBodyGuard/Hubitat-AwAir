/*
*  Hubitat Driver for AwAir Elements
*  Licenses CC-0 public domain
*/

metadata {
    definition(name: "AwAir", namespace: "awair", author: "Digital_BG", importUrl: "https://raw.githubusercontent.com/DigitalBodyGuard/Hubitat-AwAir/master/AwAir_Driver.groovy") 
    {
        capability "Sensor"
        capability "Refresh"
        capability "Polling"
        capability "TemperatureMeasurement"
        capability "CarbonDioxideMeasurement"
        capability "RelativeHumidityMeasurement"

        attribute "pm25", "number"
        attribute "temperature", "number"
        attribute "voc", "number"
        attribute "humidity", "string"
        attribute "airQuality", "number"	
        attribute "carbonDioxide", "number" 

        attribute "alert_aiq", "ENUM", ["bad","good"]
        attribute "alert_pm25", "ENUM", ["bad","good"]
        attribute "alert_co2", "ENUM", ["bad", "good"]
        attribute "alert_voc", "ENUM", ["bad", "good"]
        //      attribute "alert_humidity", "ENUM", ["false", "good"]
        //      attribute "alert_temperature", "ENUM", ["false", "good"]
    }

    preferences {
        input("ip", "text", title: "IP Address", description: "ip of AwAir", required: true, defaultValue: "http://192.168.4.3" )
        input("urlPath", "text", title: "Path Address", description: "URL path of AwAir", required: true, defaultValue: "/air-data/latest" )

        input name: "pollingInterval", type: "number", title: "Time (seconds) between status checks", defaultValue: 300

        input name: "enableAlerts_pm25", type: "bool", title: "Enable Alerts_pm25", defaultValue: true
        input name: "pm2_5LevelBad", type: "number", title: "Alert Level pm2.5", defaultValue: 40
        input name: "pm2_5LevelGood", type: "number", title: "Reset Alert Level pm2.5", defaultValue: 30

        input name: "enableAlerts_co2", type: "bool", title: "Enable Alerts_co2", defaultValue: true
        input name: "co2LevelBad", type: "number", title: "Alert Level co2", defaultValue: 1000
        input name: "co2LevelGood", type: "number", title: "Reset Alert Level co2", defaultValue: 800

        input name: "enableAlerts_voc", type: "bool", title: "Enable Alerts_voc", defaultValue: true
        input name: "vocLevelBad", type: "number", title: "Alert Level voc", defaultValue: 1000
        input name: "vocLevelGood", type: "number", title: "Reset Alert Level voc", defaultValue: 800

        input name: "enableAlerts_aiq", type: "bool", title: "Enable Alerts_aiq", defaultValue: true
        input name: "aiqLevelBad", type: "number", title: "Alert Level low airQuality", defaultValue: 60
        input name: "aiqLevelGood", type: "number", title: "Reset Alert Level high airQuality", defaultValue: 70

        /*        input name: "enableAlerts_humidity", type: "bool", title: "Enable Alerts_humidity", defaultValue: true
input name: "humidityLevelBad", type: "number", title: "Alert Level humidity", defaultValue: 70
input name: "humidityLevelGood", type: "number", title: "Reset Alert Level humidity", defaultValue: 50

input name: "enableAlerts_temperature", type: "bool", title: "Enable Alerts_temperature", defaultValue: true
input name: "temperatureLevelBad", type: "number", title: "Alert Level temperature", defaultValue: 90
input name: "temperatureLevelGood", type: "number", title: "Reset Alert Level temperature", defaultValue: 70
*/
        input name: "logEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false        
        input name: "logDebug", type: "bool", title: "Enable debug logging", defaultValue: false  

        //  input "tempOffset", "number", title: "Temperature Offset", description: "Adjust temperature by this many degrees", range: "*..*", displayDuringSetup: false, defaultValue: 0
        //	input "tempUnitConversion", "enum", title: "Temperature Unit Conversion - select F to C, C to F, or no conversion", description: "", defaultValue: "1", required: true, multiple: false, options:[["1":"none"], ["2":"Fahrenheit to Celsius"], ["3":"Celsius to Fahrenheit"]], displayDuringSetup: false
    }
}

void installed() {
    if (logDebug) log.debug "installed()..."
    refresh()   
    runIn(2, poll)
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def refresh() {
    if (logDebug) log.debug "refreshing"
    fireUpdate("voc",-1,"ppb","voc is ${-1} ppb")
    fireUpdate("pm25",-1,"ug/m3","pm25 is ${-1} ug/m3")
    fireUpdate("airQuality",-1,"","airQuality is ${-1}")
    fireUpdate("temperature",-1,"°${location.temperatureScale}","Temperature is ${-1}°${location.temperatureScale}")
    fireUpdate("carbonDioxide",-1,"ppm","carbonDioxide is ${-1} ppm")
    fireUpdate("humidity",-1,"%","humidity is ${-1}")

    fireUpdate_small("alert_aiq","good")
    fireUpdate_small("alert_voc","good")
    fireUpdate_small("alert_co2","good")
    fireUpdate_small("alert_pm25","good")
    //  fireUpdate_small("alert_humidity","bad")
    //  fireUpdate_small("alert_temperature","bad")

    runIn(2, poll)
}

def poll() {
    try {
        def Params = [ 
            uri: ip,
            path: urlPath,
            contentType: "application/json" ]
        asynchttpGet( 'ReceiveData', Params)
        if (logDebug) log.debug "poll state"
    } catch(Exception e) {
        if (logDebug) 
        log.error "error occured calling httpget ${e}"
        else
            log.error "error occured calling httpget"
    }

    runIn(pollingInterval, poll)
}

def ReceiveData(response, data) {
    try{
        if (response.getStatus() == 200 || response.getStatus() == 207) {
            if (logDebug) log.debug "start parsing"      

            Json = parseJson( response.data )

            fireUpdate("voc",Json.voc,"ppb","voc is ${Json.voc} ppb")
            fireUpdate("pm25",Json.pm25,"ug/m3","pm25 is ${Json.pm25} ug/m3")
            fireUpdate("airQuality",Json.score,"","airQuality is ${Json.score}")

            temperature=convertTemperatureIfNeeded(Json.temp,"c",1)
            fireUpdate("temperature",temperature,"°${location.temperatureScale}","Temperature is ${temperature}°${location.temperatureScale}")
            fireUpdate("carbonDioxide",Json.co2,"ppm","carbonDioxide is ${Json.co2} ppm")
            fireUpdate("humidity",(int)Json.humid,"%","humidity is ${Json.humid}")

            if(enableAlerts_aiq)
            {
                if(getAttribute("alert_aiq")=="good")
                {
                    if(Json.score < aiqLevelBad)
                    fireUpdate_small("alert_aiq","bad")
                }
                else
                    if(Json.score > aiqLevelGood)
                    fireUpdate_small("alert_aiq","good")
            }

            if(enableAlerts_pm25)
            {
                if(getAttribute("alert_pm25")=="good")
                {
                    if(Json.pm25 > pm2_5LevelBad)
                    fireUpdate_small("alert_pm25","bad")
                }
                else
                    if(Json.pm25 < pm2_5LevelGood)
                    fireUpdate_small("alert_pm25","good")
            }

            if(enableAlerts_co2)
            {
                if(getAttribute("alert_co2")=="good")
                {    
                    if(Json.co2 > co2LevelBad)
                    fireUpdate_small("alert_co2","bad")
                }
                else
                    if(Json.co2 < co2LevelGood)
                    fireUpdate_small("alert_co2","good")
            }

            if(enableAlerts_voc)
            {
                if(getAttribute("alert_voc")=="good")
                {  
                    if(Json.voc > vocLevelBad)
                    fireUpdate_small("alert_voc","bad")
                }
                else
                    if(Json.voc < vocLevelGood)
                    fireUpdate_small("alert_voc","good")
            }
            /*      if(enableAlerts_temperature)
{
if(getAttribute("alert_temperature")=="good")
{  
if(Json.temp > temperatureLevelBad)
fireUpdate_small("alert_temperature","bad")
}
else
if(Json.temp < temperatureLevelGood)
fireUpdate_small("alert_temperature","good")
}
if(enableAlerts_humidity)
{
if(getAttribute("alert_humidity")=="good")
{  
if(Json.humid > humidityLevelBad)
fireUpdate_small("alert_humidity","bad")
}
else
if(Json.humid < humidityLevelGood)
fireUpdate_small("alert_humidity","good")
}*/
        } else {
            log.error "parsing error"
        }
    } catch(Exception e) {
        log.error "error #5415 : ${e}"
    }
}

void fireUpdate(name,value,unit,description)
{
    result = [
        name: name,
        value: value,
        unit: unit,
        descriptionText: description
        //	translatable:true
    ]
    eventProcess(result)   
}

void fireUpdate_small(name,value)
{
    result = [
        name: name,
        value: value
    ]
    eventProcess(result)   
}

def getAttribute(name)
{
    return device.currentValue(name).toString()
}

void eventProcess(Map evt) {
    if (getAttribute(evt.name).toString() != evt.value.toString() ) 
    {
        evt.isStateChange=true
        sendEvent(evt)

        if (logEnable) log.info device.getName()+" "+evt.descriptionText
        if (logDebug) log.debug "result : "+evt
    }
}
