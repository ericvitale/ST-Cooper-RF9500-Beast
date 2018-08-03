/**
 *  Copyright 2015 ericvitale@gmail.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 * 
 *  08/02/2018 - 1.02 - Fix for double press issue.
 *
 *  This device handler was written specifically for the Cooper RF9500 (RF Battery Operated Switch).
 *  The version # of the switches I tested with is 3.11 as listed on th back of the switch.
 *  FCC ID: UH2-RF9500
 *  IC: 4706C-RF9500
 *
 *  You can find this devices handler @ https://github.com/ericvitale/ST-CooperRF9500Beast
 *  You can find my other device handlers & SmartApps @ https://github.com/ericvitale
 *
 */

metadata {
     definition (name: "Cooper RF9500 Beast", namespace: "ericvitale", author: "ericvitale@gmail.com") {
		capability "Switch"
		capability "Switch Level"
		capability "Button"
        capability "Actuator"
	}
    
    preferences {
    	section("Settings") {
        	input "constrain", "bool", title: "Enforce Dimmer Constraints?", description: "Yes if you want your dimmer to stay between 0 & 100, No if you don't. Selecting No removes the requirement to sync your dimmers.", required: true, defaultValue: true
		input "timeBetween", "num", title: "Minimum Gap (ms)", description: "This device likes to send multiple requests for the same action, this is the time in ms between events you will accept.", required: true, defaultValue: 400
		input "logging", "enum", title: "Log Level", required: false, defaultValue: "DEBUG", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
	}
    }
    
	tiles {
    	standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
        	state "on", label: "on", icon: "st.Lighting.light13.on", backgroundColor: "#79b821", action: "off"
            state "off", label: "off", icon: "st.Lighting.light13.off", backgroundColor: "#ffffff", action: "on"
        }
        
        valueTile("Brightness", "device.level", width: 1, height: 1) {
        	state "level", label: '${currentValue}%'
        }
        
        controlTile("levelSliderControl", "device.level", "slider", width: 3, height: 1) {
        	state "level", action:"level.setLevel"
        }
        
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
	}
}

// Pressed: CRF9500 -- parse -- description = zw device: 2E, command: 2001, payload: FF
//   OR
// Pressed: CRF9500 -- parse -- description = zw device: 2E, command: 2001, payload: FF
// Up: CRF9500 -- parse -- description = zw device: 2E, command: 2604, payload: 20 01 04
// Down: CRF9500 -- parse -- description = zw device: 2E, command: 2604, payload: 60 01 04

def parse(String description) {

    def attrName = null
    def attrValue = null
    def ignore = false
    def onOffChange = false

    def theCurrentTime = new Date().time
	
    if(state.lastPress == null) {
    	state.lastPress = theCurrentTime
    } else if((theCurrentTime - state.lastPress) >= getGap()) {
        state.lastPress = theCurrentTime
    } else {
        return
    }

    try {
    	if(state.level == null) {
        	state.level = 100
      	}
    } catch(e) {
        log("Failed to get the current dim level.", "ERROR")
		state.level = 100
    }
    
    log("description === ${description}", "DEBUG")
    
    if(description?.trim()?.endsWith("payload: FF") || description?.trim()?.endsWith("payload: 00") || description?.trim()?.endsWith("payload: 01")) { // On / Off Toggle
        log("CRF9500 -- parse -- Button Pressed", "DEBUG")
        
        try {
        	if(state.switch == "on") {
            	state.switch = "off"
            } else {
            	state.switch = "on"
           	}
        } catch(e) {
        	state.switch = "off"
        }
        
        attrName = "switch"
        
        try {
	        attrValue = state.switch
        } catch(e) {
        	log("CRF9500 -- parse -- Exception = ${e}", "ERROR")
            state.switch = "off"
            attrValue = state.switch
        }
        onOffChange = true
        
    } else if(description?.trim()?.endsWith("payload: 20 01 04") || description?.trim()?.endsWith("payload: 20 01")) { // Raise Level
    	log("CRF9500 -- parse -- Dim Level Raised.", "DEBUG")
        
        try {
        	if(state.level <= 90 && constrain) {
				state.level = state.level + 10
            } else {
            	state.level = state.level + 10
            }
        } catch(e) {
        	log("CRF9500 -- parse -- Exception = ${e}", "ERROR")
			state.level = 100
        }
        attrName = "switch.setLevel"
        try {
	        attrValue = state.level
        } catch(e) {
        	log("CRF9500 -- parse -- Exception = ${e}", "ERROR")
            state.level = 100
            attrValue = state.level
       	}
        
    } else if(description?.trim()?.endsWith("payload: 60 01 04") || description?.trim()?.endsWith("payload: 60 01")) { // Lower Level
    	log("parse -- Dim Level Lowered.", "DEBUG")
        log("parse -- device.currentValue(level) = ${state.level}.", "DEBUG")
        
        try {
        	if(state.level >= 10 && constrain) {
				state.level = state.level - 10
            } else {
            	state.level = state.level - 10
            }
        } catch(e) {
        	log("parse -- Exception = ${e}", "ERROR")
			state.level = 0
        }
        attrName = "switch.setLevel"
        try {
	        attrValue = state.level
        } catch(e) {
        	log("parse -- Exception = ${e}", "ERROR")
            state.level = 0
            attrValue = state.level
       	}
        
    } else {
        ignore = true
    }

	if(!ignore) {
		def result = createEvent(name: attrName, value: attrValue)
        try {
    		log("parse -- returned ${result?.descriptionText}.", "DEBUG")
        } catch(e) {
       		log("CRF9500 -- parse -- Exception ${e}", "ERROR")
        }
        
        //Doing this updates the UI for the level & on/off
        if(onOffChange == true) {
        	attrName = "switch"
        } else {
        	attrName = "level"
        }        
		return result
    }
}

//External methos to set the level of the dimmer. Called by SmartApp.
def setLevel(value) {
	if((value <= 100 && value >= 0) && value.isNumber()) {
		log("setLevel(${value})", "DEBUG")
    	state.level = value
    	sendEvent(name: "switch.setLevel", value: value)
    }
}

//External methos to turn this dimmer / switch on. Called by SmartApp.
def on() {
	log("on()", "DEBUG")
    state.switch = "on"
    sendEvent(name: "switch", value: "on")
}

//External methos to turn this dimmer / switch off. Called by SmartApp.
def off() {
	log("off()", "DEBUG")
    state.switch = "off"
    sendEvent(name: "switch", value: "off")
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
}

// A zwave command for a button press was received convert to button number
def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStartLevelChange cmd) {
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicGet cmd) {
}

//This method get called when button 1 is pressed first, then BasicGet gets called.
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd) {
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
}

def getGap() {
	if(state.timeBetween == null) {
		return 400
	} else {
		return state.timeBetween	
	}
}	

private determineLogLevel(data) {
    switch (data?.toUpperCase()) {
        case "TRACE":
            return 0
            break
        case "DEBUG":
            return 1
            break
        case "INFO":
            return 2
            break
        case "WARN":
            return 3
            break
        case "ERROR":
        	return 4
            break
        default:
            return 1
    }
}

def log(data, type) {
    data = "RF9500.B -- ${device.label} -- ${data ?: ''}"
        
    if (determineLogLevel(type) >= determineLogLevel(settings?.logging ?: "INFO")) {
        switch (type?.toUpperCase()) {
            case "TRACE":
                log.trace "${data}"
                break
            case "DEBUG":
                log.debug "${data}"
                break
            case "INFO":
                log.info "${data}"
                break
            case "WARN":
                log.warn "${data}"
                break
            case "ERROR":
                log.error "${data}"
                break
            default:
                log.error "RF9500.B -- ${device.label} -- Invalid Log Setting"
        }
    }
}
