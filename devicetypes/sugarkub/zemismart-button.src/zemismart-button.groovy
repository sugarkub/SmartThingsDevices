/**
 *  Zemismart Button
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
 *  Based on 'zemismart-button' by 'YouSangBeom'
 */

import groovy.json.JsonOutput
import physicalgraph.zigbee.clusters.iaszone.ZoneStatus
import physicalgraph.zigbee.zcl.DataType

metadata
{
   definition (name: 'Zemismart Button', namespace: 'sugarkub', author: 'sugarkub', ocfDeviceType: 'x.com.st.d.remotecontroller', mcdSync: true)
   {
      capability 'Actuator'
      capability 'Button'
      capability 'Holdable Button'
      capability 'Sensor'
      capability 'Health Check'

      //1
      fingerprint inClusters: '0000, 0001, 0006', outClusters: '0019, 000A', manufacturer: '_TZ3400_keyjqthh', model: 'TS0041', deviceJoinName: 'Zemismart Button', mnmn: 'SmartThings', vid: 'generic-2-button'
      fingerprint inClusters: '0000, 0001, 0006', outClusters: '0019, 000A', manufacturer: '_TZ3400_tk3s5tyg', model: 'TS0041', deviceJoinName: 'Zemismart Button', mnmn: 'SmartThings', vid: 'generic-2-button'

      //2
      fingerprint inClusters: '0000, 0001, 0006', outClusters: '0019', manufacturer: '_TYZB02_keyjhapk', model: 'TS0042', deviceJoinName: 'Zemismart 2 Button', mnmn: 'SmartThings', vid: 'generic-2-button'
      fingerprint inClusters: '0000, 0001, 0006', outClusters: '0019', manufacturer: '_TZ3400_keyjhapk', model: 'TS0042', deviceJoinName: 'Zemismart 2 Button', mnmn: 'SmartThings', vid: 'generic-2-button'

      //3
      fingerprint inClusters: '0000, 0001, 0006', outClusters: '0019, 000A', manufacturer: '_TZ3400_key8kk7r', model: 'TS0043', deviceJoinName: 'Zemismart 3 Button', mnmn: 'SmartThings', vid: 'generic-4-button'
      fingerprint inClusters: '0000, 0001, 0006', outClusters: '0019', manufacturer: '_TYZB02_key8kk7r', model: 'TS0043', deviceJoinName: 'Zemismart 3 Button', mnmn: 'SmartThings', vid: 'generic-4-button'
      fingerprint inClusters: '0000, 0001, 0006', outClusters: '0019, 000A', manufacturer: '_TZ3000_qzjcsmar', model: 'TS0043', deviceJoinName: 'Zemismart 3 Button', mnmn: 'SmartThings', vid: 'generic-4-button'
      fingerprint inClusters: '0000, 000A, 0001, 0006', outClusters: '0019', manufacturer: '_TZ3000_rrjr1q0u', model: 'TS0043', deviceJoinName: 'Zemismart 3 Button', mnmn: 'SmartThings', vid: 'generic-4-button'
      fingerprint inClusters: '0000, 000A, 0001, 0006', outClusters: '0019', manufacturer: '_TZ3000_bi6lpsew', model: 'TS0043', deviceJoinName: 'Zemismart 3 Button', mnmn: 'SmartThings', vid: 'generic-4-button'
      fingerprint inClusters: '0000, 0001, 0006', outClusters: '0019', manufacturer: '_TZ3000_a7ouggvs', model: 'TS0043', deviceJoinName: 'Zemismart 3 Button', mnmn: 'SmartThings', vid: 'generic-4-button'

      //4
      fingerprint inClusters: '0000, 0001, 0006', outClusters: '0019, 000A', manufacturer: '_TZ3000_vp6clf9d', model: 'TS0044', deviceJoinName: 'Zemismart 4 Button', mnmn: 'SmartThings', vid: 'generic-4-button'
      fingerprint inClusters: '0000, 0001, 0006', outClusters: '0019, 000A', manufacturer: '_TZ3000_dku2cfsc', model: 'TS0044', deviceJoinName: 'Zemismart 4 Button', mnmn: 'SmartThings', vid: 'generic-4-button'
      fingerprint inClusters: '0000, 0001, 0003, 0004, 0006, 1000', outClusters: '0019, 000A, 0003, 0004, 0005, 0006, 0008, 1000', manufacturer: '_TZ3000_xabckq1v', model: 'TS004F', deviceJoinName: 'TS004F Button', mnmn: 'SmartThings', vid: 'generic-4-button'

      fingerprint inClusters: '0000, 0001, 0006', outClusters: '0019, 000A', manufacturer: '_TYZB01_cnlmkhbk', model: 'TS0044', deviceJoinName: 'Hejhome Smart Button', mnmn: 'SmartThings', vid: 'generic-4-button'
   }

   preferences {
      input name: "useInfoLog", type: "bool", title: "Display info log messages?", defaultValue: true
      input name: "useDebugLog", type: "bool", title: "Display debug log messages?"

   }
}

def installed() {
   String model = device.getDataValue('model')
   int numberOfButtons = 0

   if (model == 'TS004F') {
      infoLog("Sending request to initialize TS004F in Scene Switch mode")
      zigbee.writeAttribute(0x0006, 0x8004, 0x30, 0x01)
      state.lastButtonNumber = 0
   }

   switch (model) {
   case 'TS0041':
      numberOfButtons = 1
      break
   case 'TS0042':
      numberOfButtons = 2
      break
   case 'TS0043':
      numberOfButtons = 3
      break
   case 'TS0044':
   case 'TS004F':
      numberOfButtons = 4
      break
   }

   addChildButtonDevices(numberOfButtons)
   sendEvent(name: 'numberOfButtons', value: numberOfButtons , displayed: false)
   numberOfButtons.times {
      sendEvent(name: 'button', value: 'pushed', data: [buttonNumber: it + 1], displayed: false)
   }
   sendEvent(name: 'DeviceWatch-Enroll', value: JsonOutput.toJson([protocol: 'zigbee', scheme:'untracked']), displayed: false)
}

def updated() {
   debugLog("$device.displayName childDevices $childDevices")
   if (childDevices && device.label != state.oldLabel) {
      childDevices.each {
         def newLabel = getButtonName(channelNumber(it.deviceNetworkId))
         it.setLabel(newLabel)
      }
      state.oldLabel = device.label
   }
}

def configure() {
   debugLog('Configuring Reporting, IAS CIE, and Bindings.')

   return zigbee.enrollResponse() + readDeviceBindingTable()
}

def parse(description) {
   debugLog("description is $description")
   def event = zigbee.getEvent(description)

   if (event) {
      sendEvent(event)
      debugLog("sendEvent $event")
   }
   else {
      if ((description?.startsWith('catchall:')) || (description?.startsWith('read attr -'))) {
         Map descMap = zigbee.parseDescriptionAsMap(description)
         event = parseButtonMessage(descMap)
      }

      Map result = []
      if (event) {
         debugLog("Creating event: ${event}")
         result = createEvent(event)
      }
      else if (isBindingTableMessage(description)) {
         Integer groupAddr = getGroupAddrFromBindingTable(description)
         if (groupAddr != null) {
            List cmds = addHubToGroup(groupAddr)
            result = cmds?.collect
            {
               new physicalgraph.device.HubAction(it)
            }
         }
         else {
            groupAddr = 0x0000
            List cmds = addHubToGroup(groupAddr) +
            zigbee.command(CLUSTER_GROUPS, 0x00, "${zigbee.swapEndianHex(zigbee.convertToHexString(groupAddr, 4))} 00")
            result = cmds?.collect
            {
               new physicalgraph.device.HubAction(it)
            }
         }
      }
      return result
   }
}

private getCLUSTER_GROUPS() { 0x0004 }

private channelNumber(String dni) {
   dni.split(':')[-1] as Integer
}

//1 -clusterint 6 commandInt 1
//3 -clusterint 6 commandInt 0
//2 -clusterint 8 data[0]==00
//4 -clusterint 8 data[0]==01
private List parseTS004F(Map descMap) {
   String buttonState = 'pushed'
   int buttonNumber

   if (descMap.clusterInt == 0x0006) {
      buttonNumber = (descMap.commandInt == 1) ? 1 : 3
   }
   else if (descMap.clusterInt == 0x0008) {
      if (descMap.data[0] == '00') {
         buttonNumber = 2
      }
      else if (descMap.data[0] == '01') {
         buttonNumber = 4
      }

      if (descMap.commandInt == 1) {
         buttonState = 'held'
      }
   }

   return [buttonState, buttonNumber]
}

private List parseTS004X(Map descMap) {
   def buttonState = 'pushed'
   def buttonNumber = 0
   def manufacturer = device.getDataValue('manufacturer')

   if (descMap.clusterInt == 0x0006) {
      switch (descMap.sourceEndpoint) {
      case '01':
         buttonNumber = (manufacturer == '_TZ3000_vp6clf9d') ? 3 : 1
         break
      case '02':
         buttonNumber = (manufacturer == '_TZ3000_vp6clf9d') ? 4 : 2
         break
      case '03':
         buttonNumber = (manufacturer == '_TZ3000_vp6clf9d') ? 2 : 3
         break
      case '04':
         buttonNumber = (manufacturer == '_TZ3000_vp6clf9d') ? 1 : 4
         break
      }

      switch (descMap.data) {
      case '[00]':
         buttonState = 'pushed'
         break
      case '[01]':
         buttonState = 'double'
         break
      case '[02]':
         buttonState = 'held'
         break
      }
      debugLog("${manufacturer}: ${descMap.sourceEndpoint} Button ${buttonNumber} ${buttonState}")
   }

   return [buttonState, buttonNumber]
}

private Map parseButtonMessage(Map descMap) {
   def model = device.getDataValue('model')
   Map result = [:]

   def parsed = []
   switch (model) {
      case 'TS004F':
         parsed = parseTS004F(descMap)
         break
      default:
         parsed = parseTS004X(descMap)
         break
   }

   def buttonState = parsed[0]
   def buttonNumber = parsed[1]

   if (buttonNumber != 0) {
      def descriptionText = "button $buttonNumber was $buttonState"
      result = [name: 'button', value: buttonState, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true, displayed: false]
      sendButtonEvent(buttonNumber, buttonState)
   }

   return result
}

private sendButtonEvent(buttonNumber, buttonState) {
   def child = childDevices?.find { channelNumber(it.deviceNetworkId) == buttonNumber }
   if (child) {
      // TODO: Verify if this is needed, and if capability template already has it handled
      def descriptionText = "$child.displayName was $buttonState"
      debugLog("child $child: $descriptionText")
      child?.sendEvent([name: 'button', value: buttonState, data: [buttonNumber: 1], descriptionText: descriptionText, isStateChange: true, displayed: false])
   }
   else {
      debugLog("Child device $buttonNumber not found!")
   }
}

private getButtonName(buttonNum) {
   return "${device.displayName} " + buttonNum
}

private def getSupportedButtonValues(int i) {
   def model = device.getDataValue('model')
   def supported = ['pushed', 'held', 'double']

   if (model == 'TS004F') {
      switch (i) {
      case 1:
      case 3:
         supported = ['pushed']
         break
      case 2:
      case 4:
         supported = ['pushed', 'held']
         break
      }
   }

   return supported
}

private addChildButtonDevices(numberOfButtons) {
   state.oldLabel = device.label
   debugLog("Creating $numberOfButtons children")

   for (i in 1..numberOfButtons) {
      debugLog("Creating child $i")

      def supported = getSupportedButtonValues(i)
      def child = addChildDevice('smartthings', 'Child Button', "${device.deviceNetworkId}:${i}", device.hubId, [completedSetup: true, label: getButtonName(i),
                 isComponent: true, componentName: "button$i", componentLabel: "buttton ${i}"])
      child.sendEvent(name: 'supportedButtonValues', value: supported.encodeAsJSON(), displayed: false)
      child.sendEvent(name: 'numberOfButtons', value: 1, displayed: false)
      child.sendEvent(name: 'button', value: 'pushed', data: [buttonNumber: 1], displayed: false)
   }
}

private Integer getGroupAddrFromBindingTable(description) {
   infoLog("Parsing binding table - '$description'")
   def btr = zigbee.parseBindingTableResponse(description)
   def groupEntry = btr?.table_entries?.find { it.dstAddrMode == 1 }

   if (groupEntry != null) {
      infoLog("Found group binding in the binding table: ${groupEntry}")
      Integer.parseInt(groupEntry.dstAddr, 16)
   }
   else {
      infoLog('The binding table does not contain a group binding')
      null
   }
}

private List addHubToGroup(Integer groupAddr) {
   ["st cmd 0x0000 0x01 ${CLUSTER_GROUPS} 0x00 {${zigbee.swapEndianHex(zigbee.convertToHexString(groupAddr, 4))} 00}", 'delay 200']
}

private List readDeviceBindingTable() {
   ["zdo mgmt-bind 0x${device.deviceNetworkId} 0", 'delay 200']
}

private debugLog(message) {
   if (useDebugLog) {
      log.debug "${device.displayName} ${message}"
   }
}

private infoLog(message) {
   if (useDebugLog) {
      log.info "${device.displayName} ${message}"
   }
}