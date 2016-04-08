/**
 *  AP Hue Bulb
 *
 *	Version 1.3: Added Color Temp slider & valueTile
 *				 Added Transition Time slider & valueTile
 *	Version 1.4: Revised Transition Time 
 *				 Conformed child functions with parent HLGS app
 *
 *  Authors: Anthony Pastor (infofiend) and Clayton (claytonjn)
 */

// for the UI
metadata {
	// Automatically generated. Make future change here.
	definition (name: "AP Hue Bulb", namespace: "info_fiend", author: "Anthony Pastor") {
		capability "Switch Level"
		capability "Actuator"
		capability "Color Control"
        capability "Color Temperature"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"

		command "setAdjustedColor"
        command "reset"
        command "refresh"
        command "setColorTemperature"
        command "setTransitionTime"
		command "alert"
		command "colorloopOn"
		command "colorloopOff"
		command "bri_inc"
		command "sat_inc"
		command "hue_inc"
		command "ct_inc"
		command "xy_inc"
		command "log", ["STRING","STRING"]

        attribute "transitionTime", "NUMBER"
        attribute "colorTemperature", "NUMBER"
		attribute "hueID", "NUMBER"
		attribute "colormode", "enum", ["xy", "ct", "hs"]
        attribute "effect", "enum", ["none", "colorloop"]
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"rich-control", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#C6C7CC", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#C6C7CC", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel", range:"(0..100)"
			}
            tileAttribute ("device.level", key: "SECONDARY_CONTROL") {
	            attributeState "level", label: 'Level ${currentValue}%'
			}
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setAdjustedColor"
			}
		}

        controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 5, height: 1, inactiveLabel: false, range:"(2000..6500)") {
            state "colorTemperature", action:"color temperature.setColorTemperature"
        }
        valueTile("colorTemp", "device.colorTemperature", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "colorTemperature", label: '${currentValue} K'
        }

		standardTile("reset", "device.reset", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:"Reset Color", action:"reset", icon:"st.lights.philips.hue-single"
		}
		standardTile("refresh", "device.refresh", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

        controlTile("transitionTimeSliderControl", "device.transitionTime", "slider", inactiveLabel: false,  width: 5, height: 1, range:"(0..4)") {
        	state "setTransitionTime", action:"setTransitionTime", backgroundColor:"#d04e00"
		}
		valueTile("transitionTime", "device.transitionTime", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
			state "transitionTime", label: 'Transition    Time: ${currentValue}'
        }

		valueTile("hueID", "device.hueID", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "default", label: 'ID: ${currentValue}'
		}
		valueTile("colormode", "device.colormode", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
			state "default", label: 'Colormode: ${currentValue}'
		}

        standardTile("toggleColorloop", "device.effect", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "colorloop", label:"On", action:"colorloopOff", nextState: "updating", icon:"https://raw.githubusercontent.com/infofiend/Hue-Lights-Groups-Scenes/master/smartapp-icons/hue/png/colorloop-on.png"
            state "none", label:"Off", action:"colorloopOn", nextState: "updating", icon:"https://raw.githubusercontent.com/infofiend/Hue-Lights-Groups-Scenes/master/smartapp-icons/hue/png/colorloop-off.png"
            state "updating", label:"Working", icon: "st.secondary.secondary"
		}

	}

	main(["rich-control"])
	details(["rich-control", "colorTempSliderControl", "colorTemp", "transitionTimeSliderControl", "transitionTime", "reset", "toggleColorloop", "refresh", "hueID", "colormode"])
}

// parse events into attributes
def parse(description) {
	log.debug "parse() - $description"
	def results = []

	def map = description
	if (description instanceof String)  {
		log.debug "Hue Bulb stringToMap - ${map}"
		map = stringToMap(description)
	}

	if (map?.name && map?.value) {
		results << createEvent(name: "${map?.name}", value: "${map?.value}")
	}

	results

}

// handle commands
void setTransitionTime(transitionTime) {
	log.debug "Executing 'setTransitionTime': transition time is now ${transitionTime}."
	sendEvent(name: "transitionTime", value: transitionTime, isStateChange: true)
}

void on() {
	log.debug "Executing 'on'"
    
	parent.on(this, "lights")

	sendEvent(name: "switch", value: "on", isStateChange: true)
}

void off() {
	log.debug "Executing 'off"
    
	parent.off(this, "lights")
	sendEvent(name: "switch", value: "off")
    sendEvent(name: "effect", value: "none", isStateChange: true)
}

void nextLevel() {
	log.debug "Executing 'nextLevel'"
    
    def lPercent = device.currentValue("level") as Integer ?: 0
	if (lPercent < 100) { 
    	lPercent = Math.min(25 * (Math.round(lPercent / 25) + 1), 100) as Integer 
    } else { 
    	lPercent = 25 
	}        
	setLevel(lPercent, transitionTime)
}

void setLevel(percent) { 	
	log.debug "Executing 'setLevel'"
       
	if (verifyPercent(percent)) {
	  parent.setLevel(this, percent, "lights")
      sendEvent(name: "level", value: percent, isStateChange: true)  
    }  

}

void setSaturation(percent) {
	log.debug "Executing 'setSaturation'"
      
	if (verifyPercent(percent)) {
		parent.setSaturation(this, percent, "lights")
		sendEvent(name: "saturation", value: percent)
		sendEvent(name: "colormode", value: "hs", isStateChange: true)
	}
}

void setHue(percent) {		
	log.debug "Executing 'setHue'"
   
	if (verifyPercent(percent)) {
		parent.setHue(this, percent, "lights")
		sendEvent(name: "hue", value: percent)
		sendEvent(name: "colormode", value: "hs", isStateChange: true)
	}
}

void setColor(value) {
    def events = []
    def validValues = [:]
	def deviceLevel = device.currentValue("level") 
    
	if (verifyPercent(value.hue)) {
		events << createEvent(name: "hue", value: value.hue)
		events << createEvent(name: "colormode", value: "hs", isStateChange: true)
		validValues.hue = value.hue
	}
	if (verifyPercent(value.saturation)) {
		events << createEvent(name: "saturation", value: value.saturation)
		events << createEvent(name: "colormode", value: "hs", isStateChange: true)
		validValues.saturation = value.saturation
	}
	if (value.hex != null) {
		if (value.hex ==~ /^\#([A-Fa-f0-9]){6}$/) {
			events << createEvent(name: "color", value: value.hex)
			events << createEvent(name: "colormode", value: "xy", isStateChange: true)
			validValues.hex = value.hex
		} else {
            log.warn "$value.hex is not a valid color"
        }
	}
	if (verifyPercent(value.level)) {
		validValues.level = value.level
        if ( deviceLevel == 0 ) {
        	validValues.level = 0
        }               
		events << createEvent(name: "level", value: validValues.level)

    }
    
    
    if (value.switch == "off" ) {
        events << createEvent(name: "switch", value: "off", isStateChange: true)
        validValues.on = false
    } else if (value.switch == "on" || validValues.level > 0) {
    	events << createEvent(name: "switch", value: "on", isStateChange: true)
		validValues.on = true
    }

	if (!events.isEmpty()) {
    	log.debug "setColor: ${this} to ${validValues},"
		parent.setColor(this, validValues, "lights")
	}
    events.each {
        sendEvent(it)
    }
}

void reset() {
	log.debug "Executing 'reset'"
    setColorTemperature(2710)
	parent.poll()
}

void setAdjustedColor(value) {
	if (value) {

        log.trace "setAdjustedColor: ${value}"
        def adjusted = value + [:]
        adjusted.hue = adjustOutgoingHue(value.hue)

        adjusted.level = device.currentValue("level") ?: 100 // null
        setColor(adjusted)
    } else {
		log.warn "Invalid color input"
	}
}

void setColorTemperature(value) {  	
    
	if (value) {
        log.trace "setColorTemperature: ${value}k"

		parent.setColorTemperature(this, value, "lights")
		sendEvent(name: "switch", value: "on", descriptionText: "Has been turned on")
		sendEvent(name: "colorTemperature", value: value)
		sendEvent(name: "colormode", value: "ct", isStateChange: true)
	} else {
		log.warn "Invalid color temperature"
	}
    
}

void refresh() {
	log.debug "Executing 'refresh'"
	parent.manualRefresh()
}

def adjustOutgoingHue(percent) {
	def adjusted = percent
	if (percent > 31) {
		if (percent < 63.0) {
			adjusted = percent + (7 * (percent -30 ) / 32)
		}
		else if (percent < 73.0) {
			adjusted = 69 + (5 * (percent - 62) / 10)
		}
		else {
			adjusted = percent + (2 * (100 - percent) / 28)
		}
	}
	log.info "percent: $percent, adjusted: $adjusted"
	adjusted
}

def verifyPercent(percent) {
    if (percent == null)
        return false
    else if (percent >= 0 && percent <= 100) {
        return true
    } else {
        log.warn "$percent is not 0-100"
        return false
    }
}

def log(message, level = "trace") {
	switch (level) {
    	case "trace":
        	log.trace "LOG FROM PARENT>" + message
            break;

    	case "debug":
        	log.debug "LOG FROM PARENT>" + message
            break

    	case "warn":
        	log.warn "LOG FROM PARENT>" + message
            break

    	case "error":
        	log.error "LOG FROM PARENT>" + message
            break

        default:
        	log.error "LOG FROM PARENT>" + message
            break;
    }

    return null // always child interface call with a return value
}

def getDeviceType() { return "lights" }

void initialize(hueID) {
    log.debug "Initializing ${this} with ID ${hueID}"
    sendEvent(name: "hueID", value: "${hueID}", displayed: false, isStateChange: true)
}

void alert(value) {
	log.debug "Executing 'alert'"
	parent.setAlert(this, value, "lights")
}

void colorloopOn() {
    log.debug "Executing 'colorloopOn'"
    def dState = device.latestValue("switch") as String ?: "off"

    if (dState == "off") {
        def level = device.currentValue("level")
	    if( level == null || level == 0) { 
	        level = 100 
			sendEvent(name: "level", value: percent)  
        }
		parent.on(this, 0, level, "lights")
        sendEvent(name: "switch", value: "on")
	}
    
	parent.setEffect(this, "colorloop", "lights")
    sendEvent(name: "effect", value: "colorloop", isStateChange: true)
}

void colorloopOff() {
    log.debug "Executing 'colorloopOff'"
    parent.setEffect(this, "none", "lights")
    sendEvent(name: "effect", value: "none", isStateChange: true)
}

void bri_inc(value) {
	log.debug "Executing 'bri_inc'"
	parent.setBri_Inc(this, value, "lights")
}

void sat_inc(value) {
	log.debug "Executing 'sat_inc'"
	parent.setSat_Inc(this, value, "lights")
}

void hue_inc(value) {
	log.debug "Executing 'hue_inc'"
	parent.setHue_Inc(this, value, "lights")
}

void ct_inc(value) {
	log.debug "Executing 'ct_inc'"
	parent.setCt_Inc(this, value, "lights")
}

void xy_inc(x, y) {
	log.debug "Executing 'xy_inc'"
	parent.setXy_Inc(this, x, y, "lights")
}
