/**
 *  Aqara D1 Wireless Switch (WXKG06LM)
 *
 *  Copyright 2021 sugarkub
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
 *  Based on 'Xiaomi Aqara Wireless Switch' by 'bspranger'
 */

import groovy.json.JsonOutput
import physicalgraph.zigbee.zcl.DataType

metadata {
	definition (name: "Aqara D1 Wireless Switch", namespace: "sugarkub", author: "sugarkub", minHubCoreVersion: "000.022.0002", ocfDeviceType: "x.com.st.d.remotecontroller") {

		capability 'Button'
        capability 'Battery'

		capability 'Health Check'

        capability 'Actuator'
		capability 'Sensor'
		capability "Configuration"

		fingerprint deviceId: "5F01", inClusters: "0000,0003,0019,0012,FFFF", outClusters: "0000,0003,0004,0005,0019,0012,FFFF", manufacturer: "LUMI", model: "lumi.remote.b186acn02", deviceJoinName: "Aqara Switch WXKG06LM"
	}

	simulator {
		status "Press button": "on/off: 0"
		status "Release button": "on/off: 1"
	}

	preferences {
		//Live Logging Message Display Config
		input description: "These settings affect the display of messages in the Live Logging tab of the SmartThings IDE.", type: "paragraph", element: "paragraph", title: "LIVE LOGGING"
		input name: "infoLogging", type: "bool", title: "Display info log messages?", defaultValue: true
		input name: "debugLogging", type: "bool", title: "Display debug log messages?"
	}
}

// Parse incoming device messages to generate events
def parse(description) {
	displayDebugLog(": Parsing '$description'")
	def result = [:]

	// Send message data to appropriate parsing function based on the type of report
	if (description?.startsWith("read attr")) {
		// Parse button messages of other models, or messages on short-press of reset button
		result = parseReadAttrMessage(description - "read attr - ")
	} else if (description?.startsWith("catchall")) {
		// Parse battery level from regular hourly announcement messages
		result = parseCatchAllMessage(description)
	}

	if (result != [:]) {
		displayDebugLog(": Creating event $result")
		return createEvent(result)
	}

	return [:]
}

private Map parseReadAttrMessage(String description) {
	Map descMap = (description).split(",").inject([:]) {
		map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
	Map resultMap = [:]
	if (descMap.cluster == "0012") {
		resultMap = mapButtonEvent(Integer.parseInt(descMap.endpoint,16), Integer.parseInt(descMap.value[2..3],16))

	} else if (descMap.cluster == "0000" && descMap.attrId == "0005")	{
		// Process message containing model name and/or battery voltage report
		def data = ""
		def model = descMap.value
		if (descMap.value.length() > 45) {
			model = descMap.value.split("01FF")[0]
			data = descMap.value.split("01FF")[1]
			if (data[4..7] == "0121") {
				def BatteryVoltage = (Integer.parseInt((data[10..11] + data[8..9]),16))
				resultMap = getBatteryResult(BatteryVoltage)
			}
		}
	}
	return resultMap
}

// Create map of values to be used for button events
private mapButtonEvent(buttonValue, actionValue) {
	// buttonValue (message endpoint) 1 = left, 2 = right, 3 = both (and 0 = virtual app button)
	// actionValue (message value) 0 = hold, 1 = push, 2 = double-click (hold & double-click on 2018 revision only)
	def pressType = ["held", "pushed", "pushed_2x"]
    def eventType = pressType[actionValue]
	def lastPressType = (actionValue == 0) ? "Held" : "Pressed"
	def descText = "Button was ${pressType[actionValue]} (Button $buttonValue $eventType)"
	sendEvent(name: "buttonStatus", value: "${pressType[actionValue]}", isStateChange: true, displayed: false)

	displayInfoLog(": $descText")
	runIn(1, clearButtonStatus)
	return [
		name: 'button',
		value: eventType,
		data: [buttonNumber: 1],
		descriptionText: descText,
		isStateChange: true
	]
}

def clearButtonStatus() {
	sendEvent(name: "buttonStatus", value: "released", isStateChange: true, displayed: false)
}

// Check catchall for battery voltage data to pass to getBatteryResult for conversion to percentage report
private Map parseCatchAllMessage(String description) {
	Map resultMap = [:]
	def catchall = zigbee.parse(description)
	if (catchall.clusterId == 0x0000) {
		def MsgLength = catchall.data.size()
		// Xiaomi CatchAll does not have identifiers, first UINT16 is Battery
		if ((catchall.data.get(0) == 0x01 || catchall.data.get(0) == 0x02) && (catchall.data.get(1) == 0xFF)) {
			for (int i = 4; i < (MsgLength-3); i++) {
				if (catchall.data.get(i) == 0x21) { // check the data ID and data type
					// next two bytes are the battery voltage
					resultMap = getBatteryResult((catchall.data.get(i+2)<<8) + catchall.data.get(i+1))
					break
				}
			}
		}
	}
	return resultMap
}

// Convert raw 4 digit integer voltage value into percentage based on minVolts/maxVolts range
private Map getBatteryResult(rawValue) {
	// raw voltage is normally supplied as a 4 digit integer that needs to be divided by 1000
	// but in the case the final zero is dropped then divide by 100 to get actual voltage value
	def rawVolts = rawValue / 1000
	def minVolts = 2.7
	def maxVolts = 3.2
	def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
	def roundedPct = Math.min(100, Math.round(pct * 100))
	def descText = "Battery at ${roundedPct}% (${rawVolts} Volts)"
	displayInfoLog(": $descText")
	return [
		name: 'battery',
		value: roundedPct,
		unit: "%",
		isStateChange: true,
		descriptionText : "$device.displayName $descText"
	]
}

private def displayDebugLog(message) {
	if (debugLogging)
		log.debug "${device.displayName}${message}"
}

private def displayInfoLog(message) {
	if (infoLogging || state.prefsSetCount < 3)
		log.info "${device.displayName}${message}"
}

// installed() runs just after a sensor is paired using the "Add a Thing" method in the SmartThings mobile app
def installed() {
	state.prefsSetCount = 0
	displayInfoLog(": Installing")
	checkIntervalEvent("")
}

// configure() runs after installed() when a sensor is paired
def configure() {
	displayInfoLog(": Configuring")
	initialize(true)
	checkIntervalEvent("configured")
	return
}

// updated() will run twice every time user presses save in preference settings page
def updated() {
	displayInfoLog(": Updating preference settings")
	if (!state.prefsSetCount)
		state.prefsSetCount = 1
	else if (state.prefsSetCount < 3)
		state.prefsSetCount = state.prefsSetCount + 1
	initialize(false)

	checkIntervalEvent("preferences updated")
	displayInfoLog(": Info message logging enabled")
	displayDebugLog(": Debug message logging enabled")
}

def initialize(newlyPaired) {
	sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "zigbee", scheme:"untracked"]), displayed: false)
	clearButtonStatus()
	setNumButtons()
}

def setNumButtons() {
	def modelText = "Wireless Smart Light Switch WXKG06LM"
	state.numButtons = 1
	sendEvent(name: "numberOfButtons", value: state.numButtons, displayed: false)
	sendEvent(name: "supportedButtonValues", value: ["pushed","held","pushed_2x"], displayed: true)
	device.currentValue("numberOfButtons")?.times {
		sendEvent(name: "button", value: "pushed", data: [buttonNumber: it+1], displayed: false)
	}
}

private checkIntervalEvent(text) {
	// Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
	if (text)
		displayInfoLog(": Set health checkInterval when ${text}")
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}