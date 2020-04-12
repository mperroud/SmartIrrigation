/**
 *  Simple Irrigation
 *
 *  Copyright 2020 Martin Perroud
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
 */
definition(
    name: "Simple Irrigation",
    namespace: "mperroud",
    author: "Martin Perroud",
    description: "Simply irrigation app that check rain in the last 24hs and turn on a smart plug if it didnt rain",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture@2x.png")

//    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
//    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
//    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "mainPage")
}   

def mainPage() {
	
    dynamicPage(name: "mainPage", title: "Settings", uninstall: true, install: true) 
    	{

        section("Location") {
                        input (name: "usZip", type: "bool", title: ("Use US ZIP Code?"), required: true, defaultValue: false, submitOnChange: true, width: 3)
    	   				if (settings.usZip) {	
                				input "zip", title: "US ZIP code", description: "number", multiple: false, required: true
        						}
                        }
        section("Select your pump controller") {
                input "pump", "capability.switch", multiple: true, required: true
        }
        section("Irrigation Time") {
                input "irrTime", "number", title: "Irrigation Time", multiple: false, required: true
        }
        section {
                input (
                name: "irrigationDays",
                type: "enum",
                title: "Irrigation days?",
                required: true,
                multiple: true,
                metadata: [values: ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']])
        }
        section("Start Irrigation at what time...") {
                input name: "irrTimeOne",  type: "time", required: true, title: "Turn on at..."
            }
        section("Skip Irrigation") {
                        input (name: "skipWater", type: "bool", title: ("Skip if rain last 24hs?"), required: true, defaultValue: false, submitOnChange: true, width: 3)
    	   				if (settings.skipWater) {
                            input name: "hsthr", type: "decimal", title: "Threshold?", defaultValue: "5", required: true
        				}
        }
        section("Application Parameters") {
        	mode(title: "set for specific mode(s)")
        	label(name: "label", title: "Assign a name...", required: false,    multiple: false)
    	}
     }
}
def installed() {
	initialize()
}

def updated() {
	unschedule()
	initialize()
}

def initialize() {
	log.debug "***************************************************************"
	log.debug "scheduling irrigation to run at $settings.irrTimeOne for $settings.irrTime minutes"
	schedule(irrTimeOne, "pumpOnMethod")
}

def pumpOnMethod() {

	log.debug "***************************************************************"
    def weather = getTwcConditions(zip)
	log.debug "Check if irrigation is needed. Last 24hs rain is: " + weather['precip24Hour']
	def dayCheck = irrigationDays.contains(new Date().format("EEE"))
	
    if(dayCheck && skip){
          	if (weather['precip24Hour'] <= threshold) {
            		log.debug "Last 24hs " + weather['precip24Hour'] + " is lower than $threshold - Irrigation Pump is ON"
					pump.on()
					def delay = irrTime * 60
					runIn(delay, "pumpOffMethod")
                    }
            else { log.debug "Last 24hs " + $weather['precip24Hour'] + " is higher than $threshold - No Irrigation needed"}
     }
    if(dayCheck && !skip){
          	pump.on()
			log.debug "Irrigation Pump is ON"
            def irrDelay = irrTime * 60
            runIn(irrDelay, "pumpOffMethod")
			log.debug "Irrigation Pump Turn off scheduled in $irrTime min or " + irrDelay +" seconds"
     }
}

def pumpOffMethod() {
    pump.off()
    log.debug "Irrigation Pump Turn it OFF"
}

