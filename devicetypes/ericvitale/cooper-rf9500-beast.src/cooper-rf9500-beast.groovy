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
    	input "constrain", "bool", title: "Enforce Dimmer Constraints?", description: "Yes if you want your dimmer to stay between 0 & 100, No if you don't. Selecting No removes the requirement to sync your dimmers.", required: true, defaultValue: true
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
    
    try {
    	if(state.level == null) {
        	state.level = 100
      	}
    } catch(e) {
        log.debug "Failed to get the current dim level."
		state.level = 100
    }
    
    log.debug "description === ${description}"
    
    if(description?.trim()?.endsWith("payload: FF") || description?.trim()?.endsWith("payload: 00") || description?.trim()?.endsWith("payload: 01")) { // On / Off Toggle
        log.debug "CRF9500 -- parse -- Button Pressed"
        
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
        	log.debug "CRF9500 -- parse -- Exception = ${e}"
            state.switch = "off"
            attrValue = state.switch
        }
        onOffChange = true
        
    } else if(description?.trim()?.endsWith("payload: 20 01 04") || description?.trim()?.endsWith("payload: 20 01")) { // Raise Level
    	log.debug "CRF9500 -- parse -- Dim Level Raised."
        
        try {
        	if(state.level <= 90 && constrain) {
				state.level = state.level + 10
            } else {
            	state.level = state.level + 10
            }
        } catch(e) {
        	log.debug "CRF9500 -- parse -- Exception = ${e}"
			state.level = 100
        }
        attrName = "switch.setLevel"
        try {
	        attrValue = state.level
        } catch(e) {
        	log.debug "CRF9500 -- parse -- Exception = ${e}"
            state.level = 100
            attrValue = state.level
       	}
        
    } else if(description?.trim()?.endsWith("payload: 60 01 04") || description?.trim()?.endsWith("payload: 60 01")) { // Lower Level
    	log.debug "CRF9500 -- parse -- Dim Level Lowered."
        log.debug "CRF9500 -- parse -- device.currentValue(level) = ${state.level}."
        
        try {
        	if(state.level >= 10 && constrain) {
				state.level = state.level - 10
            } else {
            	state.level = state.level - 10
            }
        } catch(e) {
        	log.debug "CRF9500 -- parse -- Exception = ${e}"
			state.level = 0
        }
        attrName = "switch.setLevel"
        try {
	        attrValue = state.level
        } catch(e) {
        	log.debug "CRF9500 -- parse -- Exception = ${e}"
            state.level = 0
            attrValue = state.level
       	}
        
    } else {
    	//log.debug "CRF9500 -- discarded event -- description = ${description}"
        ignore = true
    }

	if(!ignore) {
		def result = createEvent(name: attrName, value: attrValue)
        try {
    		log.debug "CRF9500 -- parse -- returned ${result?.descriptionText}."
        } catch(e) {
       		log.debug "CRF9500 -- parse -- Exception ${e}"
        }
        
        //Doing this updates the UI for the level & on/off
        if(onOffChange == true) {
        	attrName = "switch"
        } else {
        	attrName = "level"
        }
        
        result = createEvent(name: attrName, value: attrValue)

		try {
    		log.debug "CRF9500 -- parse -- returned ${result?.descriptionText}."
        } catch(e) {
       		log.debug "CRF9500 -- parse -- Exception ${e}"
        }
        
		return result
    }
}

//External methos to set the level of the dimmer. Called by SmartApp.
def setLevel(value) {
	if((value <= 100 && value >= 0) && value.isNumber()) {
		log.debug "CRF9500 -- setLevel(${value})"
    	state.level = value
    	sendEvent(name: "switch.setLevel", value: value)
    }
}

//External methos to turn this dimmer / switch on. Called by SmartApp.
def on() {
	log.debug "CRF9500 -- on()"
    state.switch = "on"
    sendEvent(name: "switch", value: "on")
    //sendEvent(name: "button", value: "pushed")
}

//External methos to turn this dimmer / switch off. Called by SmartApp.
def off() {
	log.debug "CRF9500 -- off()"
    state.switch = "off"
    sendEvent(name: "switch", value: "off")
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
	log.debug "CRF9500 DH WakeUpNotification"
}

// A zwave command for a button press was received convert to button number
def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStartLevelChange cmd) {
	log.debug "CRF9500 DH SwitchMultilevelStartLevelChange"
    log.debug "CRF9500 DH --- cmd = ${cmd}"
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicGet cmd) {
    log.debug "CRF9500 DH BasicGet"
}

//This method get called when button 1 is pressed first, then BasicGet gets called.
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	log.debug "CRF9500 DH BasicSet"
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd) {
	log.debug "CRF9500 DH SwitchMultilevelStopLevelChange"
}


def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug "CRF9500 DH Command"
}