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
 */

import groovy.json.JsonOutput
import physicalgraph.zigbee.zcl.DataType

metadata {
   definition (name: "Aqara T1 Button", namespace: "sugarkub", author: "sugarkub", minHubCoreVersion: "000.022.0002", ocfDeviceType: "x.com.st.d.remotecontroller") {
      capability "Battery"
      capability "Button"
      capability "Actuator"
      capability "Sensor"
      capability "Refresh"
      capability "Configuration"

      fingerprint inClusters: "0000, 0003, 0001", outClusters: "0003, 0019, 0006", manufacturer: "LUMI", model: "lumi.remote.b1acn02", deviceJoinName: "Aqara T1 Button" //Aqara T1 Button
   }

   simulator {}

   preferences {
      input description: "2021.10.10", type: "paragraph", element: "paragraph", title: "Version"
      input name: "useInfoLog", type: "bool", title: "Display info log messages?", defaultValue: true
      input name: "useDebugLog", type: "bool", title: "Display debug log messages?"
   }
}

def installed() {
   infoLog("installed")

   // Arrival sensors only goes OFFLINE when Hub is off
   sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "zigbee", scheme:"untracked"]), displayed: false)
   sendEvent(name: "numberOfButtons", value: 1, displayed: false)
   sendEvent(name: "supportedButtonValues", value: ["pushed", "double"], displayed: true)

   // Initialize default states
   device.currentValue("numberOfButtons")?.times {
      sendEvent(name: "button", value: "pushed", data: [buttonNumber: it+1], displayed: false)
   }
}

def updated() {
   infoLog("updated")

   def cmds = zigbee.command(0x0004, 0x04)
   cmds.each { sendHubCommand(new physicalgraph.device.HubAction(it)) }
}

def refresh() {
   infoLog("refresh")

   return zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x20) +
         zigbee.enrollResponse()
}

def configure() {
   infoLog("configure")

   def cmds = []
   return zigbee.onOffConfig() +
          zigbee.levelConfig() +
          zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x20, DataType.UINT8, 30, 21600, 0x01) +
          zigbee.enrollResponse() +
          zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x20) +
          cmds

}

def parse(String description) {
   debugLog("description: $description")

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

      debugLog("Parse returned $event")
      def result = event ? createEvent(event) : []

      if (description?.startsWith('enroll request')) {
         List cmds = zigbee.enrollResponse()
         result = cmds?.collect { new physicalgraph.device.HubAction(it) }
      }
      return result
   }
}

private Map getBatteryResult(rawValue) {
   debugLog('parsing battery info')

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
   }
   else if (descMap.commandInt == 0) {
      buttonState = "double"
      descriptionText = "$device.displayName button was double clicked"
   }

   return createEvent(name: "button", value: buttonState, data: [buttonState: descMap.commandInt], descriptionText: descriptionText, isStateChange: true)
}

private infoLog(message) {
   if (useInfoLog) {
      log.info "${device.displayName} ${message}"
   }
}

private debugLog(message) {
   if (useDebugLog) {
      log.debug "${device.displayName} ${message}"
   }
}