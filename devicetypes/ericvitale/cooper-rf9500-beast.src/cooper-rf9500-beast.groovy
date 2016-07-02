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

	//User Interface
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: '${name}', action: "switch.on", icon: "st.Home.home30", backgroundColor: "#ffffff"
			state "on", label: '${name}', action: "switch.off", icon: "st.Home.home30", backgroundColor: "#79b821"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		controlTile("levelSliderControl", "state.level", "slider", height: 1, width: 3, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}
        valueTile("level", "state.level", inactiveLabel: false, decoration: "flat") {
			state "level", label:'${currentValue} %', unit:"%", backgroundColor:"#ffffff"
		}
		main "switch"
		details(["switch", "refresh", "level", "levelSliderControl"])
	}
}

// Pressed: CRF9500 -- parse -- description = zw device: 2E, command: 2001, payload: FF
// Up: CRF9500 -- parse -- description = zw device: 2E, command: 2604, payload: 20 01 04
// Down: CRF9500 -- parse -- description = zw device: 2E, command: 2604, payload: 60 01 04

def parse(String description) {
	//log.debug "CRF9500 -- parse -- description = ${description}"
    
    def attrName = null
    def attrValue = null
    def ignore = false
    
    try {
    	if(state.level == null) {
        	state.level = 100
      	}
    } catch(e) {
    	log.debug "Failed to get currentValue('level')"
		state.level = 100
    }
    
    if(description?.trim()?.endsWith("payload: FF")) {
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
    } else if(description?.trim()?.endsWith("payload: 20 01 04")) {
    	log.debug "CRF9500 -- parse -- Dim Level Raised."
        //attrName = "switch.setLevel"
        //attrValue = "100"
        try {
        	if(state.level <= 90) {
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
    } else if(description?.trim()?.endsWith("payload: 60 01 04")) {
    	log.debug "CRF9500 -- parse -- Dim Level Lowered."
        log.debug "CRF9500 -- parse -- device.currentValue(level) = ${state.level}."
        
        try {
        	if(state.level >= 10) {
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
    	log.debug "CRF9500 -- parse -- returned ${result?.descriptionText}."
		return result
    }
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