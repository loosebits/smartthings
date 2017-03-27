definition(
  name: "Motion Controlled Lights",
  namespace: "loosebits",
  author: "Rand McNeely",
  description: "Add motion control to a dimmer.  Light level based on nearby light levels",
  category: "Health & Wellness"
)

preferences {
  page name: "rootPage"
}

def rootPage() {
  dynamicPage(name: "rootPage", title: "", uninstall: true, install: true, nextPage:"timeIntervalInput") {

    section("Which motion controller") {
      input(name: "motionController", type: "capability.motionSensor", title: "Motion Sensor", required: true)
    }
    section("Which dimmer to control") {
      input(name:"dimmer", type:"capability.switchLevel", title: "Dimmer", required:true)
    }
    section("Based on what dimmers") {
      input(name:"referenceDimmers", type:"capability.switchLevel", title:"Reference Dimmers", required: true, multiple: true)
    }
    section("Add an additional offset") {
      input(name:"offset", type:"number", title:"Offset from Average", required: true, defaultValue: 20)
    }
    section("More Options", hideable: true, hidden:true) {
      paragraph "Enter start and end times for the dimmer to use a fixed value and not an average"
        input "fixedValue", type:"number", title:"Fixed Value", defaultValue:100
        input "startTimeType", "enum", title: "Starting at", options: [["time": "A specific time"], ["sunrise": "Sunrise"], ["sunset": "Sunset"]], defaultValue: "time", submitOnChange: true
        if (startTimeType in ["sunrise","sunset"]) {
          input "startTimeOffset", "number", title: "Offset in minutes (+/-)", range: "*..*", required: false
        } else {
          input "starting", "time", title: "Start time", required: false
        }
        input "endTimeType", "enum", title: "Ending at", options: [["time": "A specific time"], ["sunrise": "Sunrise"], ["sunset": "Sunset"]], defaultValue: "time", submitOnChange: true
        if (endTimeType in ["sunrise","sunset"]) {
          input "endTimeOffset", "number", title: "Offset in minutes (+/-)", range: "*..*", required: false
        } else {
          input "ending", "time", title: "End time", required: false
        }
    }
  }
}


private getTimeOk() {
  def result = true
  def start = timeWindowStart()
  def stop = timeWindowStop()
  if (start && stop && location.timeZone) {
    result = timeOfDayIsBetween(start, stop, new Date(), location.timeZone)
  }
  log.trace "timeOk = $result"
  result
}

private timeWindowStart() {
  def result = null
  if (startTimeType == "sunrise") {
    result = location.currentState("sunriseTime")?.dateValue
    if (result && startTimeOffset) {
      result = new Date(result.time + Math.round(startTimeOffset * 60000))
    }
  }
  else if (startTimeType == "sunset") {
    result = location.currentState("sunsetTime")?.dateValue
    if (result && startTimeOffset) {
      result = new Date(result.time + Math.round(startTimeOffset * 60000))
    }
  }
  else if (starting && location.timeZone) {
    result = timeToday(starting, location.timeZone)
  }
  log.trace "timeWindowStart = ${result}"
  result
}

private timeWindowStop() {
  def result = null
  if (endTimeType == "sunrise") {
    result = location.currentState("sunriseTime")?.dateValue
    if (result && endTimeOffset) {
      result = new Date(result.time + Math.round(endTimeOffset * 60000))
    }
  }
  else if (endTimeType == "sunset") {
    result = location.currentState("sunsetTime")?.dateValue
    if (result && endTimeOffset) {
      result = new Date(result.time + Math.round(endTimeOffset * 60000))
    }
  }
  else if (ending && location.timeZone) {
    result = timeToday(ending, location.timeZone)
  }
  log.trace "timeWindowStop = ${result}"
  result
}


def installed() {
  initialize();
}

def updated() {
  unsubscribe();
  initialize();
}

def initialize() {
  subscribe(motionController, "motion.active", motionDetected);
  subscribe(motionController, "motion.inactive", noMotionDetected);
}

def motionDetected(e) {
  int level = 0;
  if (timeOk) {
    level = referenceDimmers.collect { ref -> 
      if (ref.currentSwitch == 'on') {
        log.debug("Dimmer at $ref.levelState.numberValue");
        return ref.levelState.numberValue.intValue();
      } else {
        log.debug("Dimmer off");
        return 0;
      }
    }.sum() / referenceDimmers.size() + offset;
  } else {
    level = fixedValue
  }
  if (level > 100) {
    level = 100;
  }
  if (level < 0) {
    level = 0;
  }
  log.debug("Setting dimmer to $level");
  dimmer.setLevel(level);
}

def noMotionDetected(e) {
  log.debug("No motion");
  dimmer.setLevel(0);

}