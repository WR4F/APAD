import time, cv2, sys, logging

class Drone:

    def __init__(self):

        logging.basicConfig(level=logging.INFO)
        self.log = logging.getLogger("Drone")
        
        #protcol lists
        self.droneInfo = [0,0,0,0,0]
        #self.appInfo = [0,0,0]

        #drone protocol variables
        self.status = 0      #offline, check, ready, manaul, follow me, trail, top
        self.velocity = 3
        self.battery = 100
        self.altitude = 0
        self.errorCode = 0

        #drone camera and flying variables
        self.camera = cv2.VideoCapture(0)
        self.frontCamera = True
        self.flyMode = 3
        self.flying = False

        #drone coords
        self.x = 0
        self.y = 0
        self.z = 0
        self.orientation = 90 # drones default at a 90 degree angle facing foward
        self.coords = []

        self.updateCoords()

        # Check Camera status
        ok, frame = self.camera.read()
        if not ok:
            self.log.info('Camera not working')
            
        #Check Drone
        self.checkDrone()
        
        self.updateInfo()

    #get latest info from app
    def sendAppData(self, update):

        #handle button pressed
        self.handleButton(update[0])


    #send latest drone info to app
    def getDroneData(self):

        #update info
        self.battery = self.getBattery()
        self.altitude = self.getAltitude()
        self.updateInfo()

        return self.droneInfo

    #Handle button logic
    def handleButton(self, button):
        if self.status != 8:
            
            #launch/land
            if button == 1:

                #launch
                if not self.flying:
                    self.launch()
                
                #land
                else:

                    #land and check drone 
                    self.land()

            #emergency land
            elif button == 2 and self.flying:
                    self.emergencyLand()
            
            #move drone according to button
            elif button >= 3 and button <= 10:
                self.moveDrone(button)
            
            #switch cameras
            elif button == 11:
                self.switchCamera()
            
            #land at base
            else:
                self.landAtBase()

    #Move drone logic
    def moveDrone(self, move):

        #code to move drone goes here
        if self.flying:
        
            if move == 3:
                self.log.info(f"Moving {self.velocity} up.")
                #self.altitude += self.velocity
                self.y += self.velocity

            elif move == 4:
                self.log.info(f"Moving {self.velocity} down.")
                if not self.y <= 0:
                    #self.altitude -= self.velocity
                    self.y -= self.velocity

            elif move == 5:
                self.log.info(f"Moving {self.velocity} left.")
                self.x += self.velocity

            elif move == 6:
                self.log.info(f"Moving {self.velocity} right.")
                self.x -= self.velocity

            elif move == 7:
                self.log.info(f"Moving {self.velocity} foward.")
                self.z += self.velocity

            elif move == 8:
                self.log.info(f"Moving {self.velocity} backwards.")
                self.z -= self.velocity

            elif move == 9:
                self.log.info(f"Rotating {self.velocity} left.")
                self.orientation += self.velocity

            elif move == 10:
                self.log.info(f"Rotating {self.velocity} right.")
                self.y -= self.velocity
            
            self.battery -= 1

            #update latest coords and print
            self.updateCoords()
            self.log.info(str(self.coords))
            
            return True
        return False
        
    #get frame from camera
    def getFrame(self):
        return self.camera.read()
        
    #change resolution, only supports native resolutions of camera 
    def changeCameraResolution(self, res):
        self.log.info("Camera resolution changed to" + str(res))   
        self.camera.set(cv2.CAP_PROP_FRAME_WIDTH, res[0])
        self.camera.set(cv2.CAP_PROP_FRAME_HEIGHT, res[1])

    #launch drone
    def launch(self):
        self.log.info("Launching!")
        #code to launch goes here

        #code to fly manaul goes here
        if self.flyMode == 3:
            string = "todo"
        
        #code to follow me goes here
        elif self.flyMode == 4:
            string = "todo"
        
        #code to trail behind goes here
        elif self.flyMode == 5:
            string = "todo"
        
        #code to fly above goes here
        elif self.flyMode == 6:
            string = "todo"

        self.status = self.flyMode
        self.flying = True

        self.log.info("Launched with fly mode: " + str(self.flyMode))

        return True
    
    #land drone
    def land(self):
        self.log.info("Landing!")

        #code to land goes here
        self.checkDrone()
        self.flying = False
        self.log.info("Landed!")
        return True

    #Emergency land drone
    def emergencyLand(self):
        self.log.info("Emergency landing!")

        #code to emergency land goes here
        self.status = 1

        self.checkDrone()
        self.flying = False
        self.status = 2
        self.log.info("Emergency landed sucess.")
        return True
    
    #attempt to return drone to base
    def landAtBase(self):
        self.log.info("Landing back at base.")

        #code goes here
        string = ""
        self.log.info("Landed back at base.")
        return True

    # check if drone can fly
    def checkDrone(self):
        self.log.info("Checking!")
        self.status = 1

        #code to check hardware goes here
        self.battery = self.getBattery()
        self.altitude = self.getAltitude()
        self.status = 2
        self.log.info("Check passed.")
        return True
    
    #update flight mode
    def updateFlightMode(self, mode):

        if mode != self.flyMode:
            self.log.info("Updating flight mode!")
            self.flyMode = mode

        
            #change flight mode code goes here
            if self.flying:
                self.status = mode

            self.log.info("Updated flight mode to " + str(mode))

            return True
        return False
    
    #toggle cameras
    def switchCamera(self):
        self.log.info("Switching camera.")

        #code to switch between cameras goes here
        if self.frontCamera:
            self.frontCamera = False
            self.log.info("Switched to bottom camera.")
        else:
            self.frontCamera = True
            self.log.info("Switched to front camera.")
        return True

    #get drone's battery
    def getBattery(self):

        #code to get hardware battery goes here
        self.log.info("Battery: " + str(self.battery))
        return self.battery

    #get drone's altitude
    def getAltitude(self):

        #code to get altitude goes here
        self.log.info("Drone altitude: " + str( self.y))
        return self.y
    
    #update drone info list
    def updateInfo(self):
        self.droneInfo = [self.status, self.battery, self.velocity, self.altitude, self.errorCode]
    
    #stop whatever the drone is doing
    def stopEverything(self):

        #code goes here
        self.log.info("Drone stopped.")
        return True
    
    def updateCoords(self):
        self.coords = [self.x, self.y, self.z, self.orientation]
    
