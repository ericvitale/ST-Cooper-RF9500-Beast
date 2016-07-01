metadata {
	definition (name: "Cooper RF9500 Beast", namespace: "ericvitale", author: "ericvitale@gmail.com") {
		capability "Switch"
		capability "Switch Level"
		capability "Button"
        capability "Actuator"

		//fingerprint deviceId: "0x1200", inClusters: "0x77 0x86 0x75 0x73 0x85 0x72 0xEF", outClusters: "0x26"
	}

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: '${name}', action: "switch.on", icon: "st.Home.home30", backgroundColor: "#ffffff"
			state "on", label: '${name}', action: "switch.off", icon: "st.Home.home30", backgroundColor: "#79b821"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}
        valueTile("level", "device.level", inactiveLabel: false, decoration: "flat") {
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

def on() {
    sendEvent(name: "switch", value: "on")
}

def off() {
    sendEvent(name: "switch", value: "off")
}

def levelup() {
	//log.debug "in level up"

	def curlevel = device.currentValue('level') as Integer 
    log.debug "Device Handler: cur level = $curlevel"
	if (curlevel <= 95)
    	setLevel(curlevel + 5);     
}

def leveldown() {
	//log.debug "in level down"

	def curlevel = device.currentValue('level') as Integer 
    log.debug "Device Handler: cur level = $curlevel"
	if (curlevel > 5)
    	setLevel(curlevel - 5)    
}

def setLevel(value) {
	log.trace "Device Handler: setLevel($value)"
	sendEvent(name: "level", value: value)
    sendEvent(name:"switch.setLevel",value:value)
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
	log.debug "CRF9500 DH WakeUpNotification"
}

// A zwave command for a button press was received convert to button number
def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStartLevelChange cmd) {
	log.debug "CRF9500 DH SwitchMultilevelStartLevelChange"
    log.debug "CRF9500 DH --- cmd = ${cmd}"
}

// The controller likes to repeat the command... ignore repeats
def checkbuttonEvent(buttonid){

	if (state.lastScene == buttonid && (state.repeatCount < 4) && (now() - state.repeatStart < 2000)) {
    	log.debug "Device Handler: Button ${buttonid} repeat ${state.repeatCount}x ${now()}"
        state.repeatCount = state.repeatCount + 1
        createEvent([:])
    }
    else {
    	// If the button was really pressed, store the new scene and handle the button press
        state.lastScene = buttonid
        state.lastLevel = 0
        state.repeatCount = 0
        state.repeatStart = now()

        buttonEvent(buttonid)
    }
}

// Handle a button being pressed
def buttonEvent(button) {
	button = button as Integer
    log.trace "Device Handler 1: Button $button pressed"
    def result = []
	if (button == 1) {
    	def mystate = device.currentValue('switch');
        if (mystate == "on") 
            off()
        else
            on()   
    }
    updateState("currentButton", "$button")   
        // update the device state, recording the button press
        result << createEvent(name: "button", value: "pushed", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true)
    result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicGet cmd) {
    log.debug "CRF9500 DH BasicGet"
}

//This method get called when button 1 is pressed first, then BasicGet gets called.
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	log.debug "CRF9500 DH BasicSet"
    buttonPressed()
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd) {
	log.debug "CRF9500 DH SwitchMultilevelStopLevelChange"
//	createEvent([:])
}


def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug "CRF9500 DH Command"
//	[ descriptionText: "$cmd"]
}

// Update State
def updateState(String name, String value) {
	state[name] = value
	device.updateDataValue(name, value)
}

//This method is called by the zwaveEvent handler if button 1 is press
def buttonPressed() {
	log.debug "CRF9500 -- buttonPressed() -- Button 1 Pressed!"
    turnSwitchOn()
}

def turnSwitchOn() {
	//log.debug "CRF9500 -- turnSwitchOn() -- Turning Switch On."
    //sendEvent(name: "switch", value: "on")
    //log.debug "CRF9500 -- switch (on) event raised."
    //sendEvent(name: "switchLevel", value: 100)
    //log.debug "CRF9500 -- switchLevel (100) event raised."
    
}