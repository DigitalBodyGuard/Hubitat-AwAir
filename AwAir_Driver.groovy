/*
*  Driver for AwAir Elements
*/
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition(name: "AwAir", namespace: "awair", author: "Digital_BG", importUrl: "https://raw.githubusercontent.com/DigitalBodyGuard/Hubitat-AwAir/master/AwAir_Driver.groovy") 
    {
        capability "Sensor"
        capability "Refresh"
        capability "Polling"
        capability "TemperatureMeasurement"
        capability "CarbonDioxideMeasurement"
        capability "RelativeHumidityMeasurement"
        //    if (isST) {capability "Air Quality Sensor"} else {attribute "airQuality", "number"}

        attribute "pm25", "number"
        attribute "temperature", "number"
        attribute "voc", "number"
        attribute "humidity", "string"
        attribute "airQualityIndex", "number"	
        attribute "carbonDioxide", "number" 
    }

    /*
tiles() {
multiAttributeTile(name:"aiq", type: "generic", width: 6, height: 4){
tileAttribute ("device.aiq", key: "PRIMARY_CONTROL") {
attributeState("clear", label:"clear", icon:"st.alarm.smoke.clear", backgroundColor:"#ffffff")
attributeState("aiq", label:"aiq", icon:"st.alarm.carbon-monoxide.carbon-monoxide", backgroundColor:"#e86d13")

//	attributeState("carbonMonoxide", label:"dioxide", icon:"st.alarm.carbon-monoxide.carbon-monoxide", backgroundColor:"#e86d13")
}
}
}*/
    
    preferences {
        input("ip", "text", title: "IP Address", description: "ip of AwAir", required: true, defaultValue: "http://192.168.4.3" )
        input("urlPath", "text", title: "Path Address", description: "URL path of AwAir", required: true, defaultValue: "/air-data/latest" )

        input name: "pollingInterval", type: "number", title: "Time (seconds) between status checks", defaultValue: 120    
        input name: "logEnable", type: "bool", title: "Enable logging", defaultValue: false        
        input name: "logDebug", type: "bool", title: "Enable debug logging", defaultValue: false  

        input "tempOffset", "number", title: "Temperature Offset", description: "Adjust temperature by this many degrees", range: "*..*", displayDuringSetup: false, defaultValue: 0
        //	input "tempUnitConversion", "enum", title: "Temperature Unit Conversion - select F to C, C to F, or no conversion", description: "", defaultValue: "1", required: true, multiple: false, options:[["1":"none"], ["2":"Fahrenheit to Celsius"], ["3":"Celsius to Fahrenheit"]], displayDuringSetup: false
    }
}
void installed() {
    if (logDebug) log.debug "installed()..."
    poll()   
    runIn(2, poll)
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def refresh() {
    if (logDebug){ log.debug "refreshing"}

    fireUpdate("voc",-1,"ppb","voc is ${-1} ppb")
    fireUpdate("pm25",-1,"ug/m3","pm25 is ${-1} ug/m3")
    fireUpdate("airQualityIndex",-1,"","airQualityIndex is ${-1}")
    fireUpdate("temperature",-1,"°${location.temperatureScale}","Temperature is ${-1}°${location.temperatureScale}")
    fireUpdate("carbonDioxide",-1,"ppm","carbonDioxide is ${-1} ppm")
    fireUpdate("humidity",-1,"%","humidity is ${-1}")

    runIn(2, poll)
}

def poll() {
    try {
        def Params = [ 
            uri: ip,
            path: urlPath,
            contentType: "application/json" ]
        asynchttpGet( 'ReceiveData', Params)
        if (logDebug)     	log.debug "poll state"

    } catch(Exception e) {
        if (logDebug) 
        log.error "error occured calling httpget ${e}"
        else
            log.error "error occured calling httpget"
    }

    runIn(pollingInterval, poll)
}

def ReceiveData(response, data) {
    if (logEnable) log.info "start ReceiveData"  
    if (response.getStatus() == 200 || response.getStatus() == 207) {
        if (logDebug) log.info "start parsing"      
        if (logDebug) log.debug response.data

        Json = parseJson( response.data )

        fireUpdate("voc",Json.voc,"ppb","voc is ${Json.voc} ppb")
        fireUpdate("pm25",Json.pm25,"ug/m3","pm25 is ${Json.voc} ug/m3")
        fireUpdate("airQualityIndex",Json.score,"","airQualityIndex is ${Json.co2}")

        temperature=convertTemperatureIfNeeded(Json.temp-tempOffset,"c",1)
        fireUpdate("temperature",temperature,"°${location.temperatureScale}","Temperature is ${temperature}°${location.temperatureScale}")
        fireUpdate("carbonDioxide",Json.co2,"ppm","carbonDioxide is ${Json.co2} ppm")
        fireUpdate("humidity",Json.humid,"%","humidity is ${Json.humid}")

        if (logEnable) log.info "done"
    } else {
        log.error "parsing error"
    }
}
void fireUpdate(name,value,unit,description)
{
    result =    [
        name:name,
        value:value,
        unit: unit,
        descriptionText: description,
        //	translatable:true
    ]
    eventProcess(result)   
}

void eventProcess(Map evt) {
    if (device.currentValue(evt.name).toString() != evt.value.toString() ) {
        evt.isStateChange=true
        evt.translatable=true
        log.info device.getName()+" "+evt.descriptionText

        if (logDebug) log.info "result : "+evt
        sendEvent(evt)
    }
}
