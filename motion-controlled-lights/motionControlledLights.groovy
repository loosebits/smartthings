
definition(
  name: "Motion Controlled Lights",
  namespace: "loosebits",
  author: "Rand McNeely",
  description: "Add motion control to a dimmer.  Light level based on nearby light levels"
  category: "Health & Wellness",
)

preferences {
  page(name: "rootPage")
  page(name: "schedulingPage")
  page(name: "completionPage")
  page(name: "numbersPage")
  page(name: "controllerExplanationPage")
  page(name: "unsupportedDevicesPage")
}

def rootPage() {
  dynamicPage(name: "rootPage", title: "", install: true, uninstall: true) {

    section("Which motion controller") {
      input(name: "motionController", type: "capability.motionSensor", title: "Motion Sensor", required: true)
    }
    section("Which dimmer to control") {
      input(name:"dimmer" type:"capability.switchLevel", title: "Dimmer", required:true)
    }
    section("Based on what dimmers") {
      input(name:"referenceDimmers", type:"capability.switchLevel", title:"Reference Dimmers", required: true, mutliple: true)
    }
  }
}

def intslled() {
  initialize();
}

def updated() {
  unsubscribe();
  initialize();
}

def initialize() {
  subscribe("motionController", "motion.active", motionDetected);
  subscribe("motionController", "motion.inactive", noMotionDetected);
}

def motionDetected(e) {
  def level = new BigDecimal();
  referenceDimmers.each { ref -> 
    def refLevel = 0;
    if (ref.currentSwitch == 'on') {
      level.add(ref.levelState.numberValue);
      log.debug("Dimmer at $ref.levelState.numberValue");
    } else {
      log.debug("Dimmer off");
    }
  }
  def targetLevel = Math.min(level.doubleValue() / referenceDimmers.size() + 20,100);
  log.debug("Setting dimmer to $targetLevel");
  dimmer.setLevel(targetLevel);
}

def noMotionDetected(e) {
  log.debug("No motion");
  dimmer.setLevel(0);

}