/**
 *  Aqara T1 Button (WXKG13LM)
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
 *  Based on original ZigBee Button by Mitch Pond 2015
 *
 *  Problem: Both "held" and "pushed" perform the same action.
 *
 */

import groovy.json.JsonOutput
import physicalgraph.zigbee.zcl.DataType

metadata {
    definition (name: "Aqara T1 Button", namespace: "sugarkub", author: "sugarkub", minHubCoreVersion: "000.022.0002", ocfDeviceType: "x.com.st.d.remotecontroller") {
        capability "Battery"
        capability "Button"
        capability "Actuator"
        capability "Switch"
        capability "Momentary"
        capability "Configuration"
        capability "Sensor"
        capability "Refresh"

        fingerprint inClusters: "0000, 0003, 0001", outClusters: "0003, 0019, 0006", manufacturer: "LUMI", model: "lumi.remote.b1acn02", deviceJoinName: "Aqara T1 Button" //Aqara T1 Button
    }

    simulator {}

    preferences {}

    tiles {
        standardTile("button", "device.button", width: 2, height: 2) {
            state "default", label: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
            state "button 1 pushed", label: "pushed #1", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#00A0DC"
        }

        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false) {
            state "battery", label:'${currentValue}% battery', unit:""
        }
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main (["button"])
        details(["button", "battery", "refresh"])
    }
}

def parse(String description) {
    log.debug "parse >> $description"
    def event = zigbee.getEvent(description)
    if (event) {
        sendEvent(event)
    }
    else {
        def descMap = zigbee.parseDescriptionAsMap(description)
        if (descMap.clusterInt == 0x0001 && descMap.attrInt == 0x0020 && descMap.value != null) {
            event = getBatteryResult(zigbee.convertHexToInt(descMap.value))
        }
        else if (descMap.clusterInt == 0x0006) {
            event = parseButtonMessage(descMap)
        }
        
        log.debug "Parse returned $event"
        def result = event ? createEvent(event) : []

        if (description?.startsWith('enroll request')) {
            List cmds = zigbee.enrollResponse()
            result = cmds?.collect { new physicalgraph.device.HubAction(it) }
        }
        return result
    }
}

private Map getBatteryResult(rawValue) {
    log.debug 'Battery'
    def volts = rawValue / 10
    if (volts > 3.0 || volts == 0 || rawValue == 0xFF) {
        return [:]
    }
    else {
        def result = [
                name: 'battery'
        ]
        def minVolts = 2.1
        def maxVolts = 3.0
        def pct = (volts - minVolts) / (maxVolts - minVolts)
        result.value = Math.min(100, (int)(pct * 100))
        def linkText = getLinkText(device)
        result.descriptionText = "${linkText} battery was ${result.value}%"
        return result
    }
}

private Map parseButtonMessage(Map descMap){
    def buttonState = ""
    def descriptionText = ""
    
    if (descMap.commandInt == 2) {
        buttonState = "pushed"
        descriptionText = "$device.displayName button was pushed"
    } else if (descMap.commandInt == 0) {
        buttonState = "double"
        descriptionText = "$device.displayName button was double clicked"
    }
    
    return createEvent(name: "button", value: buttonState, data: [buttonState: descMap.commandInt], descriptionText: descriptionText, isStateChange: true)
}

def refresh() {
    log.debug "Refreshing Battery"

    return zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x20) +
            zigbee.enrollResponse()
}

def configure() {
    log.debug "Configuring Aqara T1 Button (WXKG13LM)"
    def cmds = []
    return zigbee.onOffConfig() +
            zigbee.levelConfig() +
            zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x20, DataType.UINT8, 30, 21600, 0x01) +
            zigbee.enrollResponse() +
            zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x20) +
            cmds

}

def installed() {
    initialize()

    // Initialize default states
    device.currentValue("numberOfButtons")?.times {
        sendEvent(name: "button", value: "pushed", data: [buttonNumber: it+1], displayed: false)
    }
}

def updated() {
    initialize()
}

def initialize() {
    // Arrival sensors only goes OFFLINE when Hub is off
    sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "zigbee", scheme:"untracked"]), displayed: false)    
    sendEvent(name: "numberOfButtons", value: 1, displayed: false)
    sendEvent(name: "supportedButtonValues", value: ["pushed","double"], displayed: true)
}