import time
import cv2
import sys
import logging


class Drone:

    def __init__(self):

        logging.basicConfig(level=logging.INFO)
        self.droneLog = logging.getLogger("Drone")
        self.appLog = logging.getLogger("App")

        # drone protocol variables
        self.status = 0  # offline, check, ready, flying, land, error
        self.velocity = 3
        self.battery = 100
        self.altitude = 0
        self.errorCode = 0
        self.flyMode = 3

        # drone camera and flying variable
        self.camera = cv2.VideoCapture(0)
        self.frontCamera = True
        self.flying = False

        # drone coords
        self.x = 0
        self.y = 0
        self.z = 0
        self.orientation = 0

        #One time check of camera status
        ok, frame = self.camera.read()
        if not ok:
            self.droneLog.info('Error: Camera not working')
            self.status = 5
            self.errorCode = 6

        self.checkDrone()

    #launched when drone connects
    def initDrone(self):
        self.checkDrone()

    # get latest info from app
    def sendAppData(self, update):

        # handle new flight mode
        if self.flyMode != update[1]:
            self.flyMode = update[1]
            self.updateFlightMode()

        #self.appLog.info(str(update))

        # handle button pressed
        self.handleButton(update[0])

    # send latest drone info to app
    def getDroneData(self):

        # update info
        self.battery = self.getBattery()
        self.altitude = self.getAltitude()

        return self.updateInfo()

    # Handle button logic
    def handleButton(self, button):
        
        #land if drone is flying and battery is 20 or less
        if self.flying and self.battery <= 20:
            self.land()

        if self.status != 5:

            # launch/land
            if button == 1:

                # launch
                if not self.flying:
                    self.launch()

                # land
                else:

                    # land and check drone
                    self.land()

            # emergency land
            elif button == 2 and self.flying:
                self.emergencyLand()

            # move drone according to button
            elif button >= 3 and button <= 10 and self.flying:
                self.moveDrone(button)

            # switch cameras
            elif button == 11:
                self.switchCamera()
          
            elif self.flying and button ==12:
                self.landAtBase()

    # Move drone logic
    def moveDrone(self, move):

        # code to move drone goes here

        if move == 3:
            self.droneLog.info(f"Moving {self.velocity} up.")
            # self.altitude += self.velocity
            self.y += self.velocity

        elif move == 4 and not self.y <= 0:
            self.droneLog.info(f"Moving {self.velocity} down.")
            # self.altitude -= self.velocity
            self.y -= self.velocity

        elif move == 5:
            self.droneLog.info(f"Moving {self.velocity} left.")
            self.x += self.velocity

        elif move == 6:
            self.droneLog.info(f"Moving {self.velocity} right.")
            self.x -= self.velocity

        elif move == 7:
            self.droneLog.info(f"Moving {self.velocity} foward.")
            self.z += self.velocity

        elif move == 8:
            self.droneLog.info(f"Moving {self.velocity} backwards.")
            self.z -= self.velocity

        elif move == 9:
            self.droneLog.info(f"Rotating {self.velocity} left.")
            self.orientation += self.velocity

        elif move == 10:
            self.droneLog.info(f"Rotating {self.velocity} right.")
            self.orientation -= self.velocity

        self.battery -= 1

        # get latest coords and print
        self.droneLog.info(str(self.getCoords()))

        #return True
        
    # get frame from camera
    def getFrame(self):
        return self.camera.read()
        
    # change resolution, only supports native resolutions of camera 
    def changeCameraResolution(self, res):
        self.droneLog.info("Camera resolution changed to" + str(res))   
        self.camera.set(cv2.CAP_PROP_FRAME_WIDTH, res[0])
        self.camera.set(cv2.CAP_PROP_FRAME_HEIGHT, res[1])

    # launch drone
    def launch(self):

        self.droneLog.info("Launching!")
        # code to launch goes here

        # code to fly manaul goes here
        if self.flyMode == 3:
            string = "todo"
        
        # code to follow me goes here
        elif self.flyMode == 4:
            string = "todo"
        
        # code to trail behind goes here
        elif self.flyMode == 5:
            string = "todo"
        
        # code to fly above goes here
        elif self.flyMode == 6:
            string = "todo"

        self.status = 3
        self.flying = True
        self.battery -= 5

        self.droneLog.info("Launched with fly mode: " + str(self.flyMode))
    
    # land drone
    def land(self):

        # code to land goes here
        self.flying = False
        self.droneLog.info("Landed!")
        
        self.checkDrone()     
        
    # Emergency land drone
    def emergencyLand(self):
        self.droneLog.info("Emergency landing!")

        # code to emergency land goes here
        self.status = 1

        self.checkDrone()
        self.flying = False
        self.status = 2
        self.droneLog.info("Emergency landed sucess.")
    
    # attempt to return drone to base
    def landAtBase(self):
        self.droneLog.info("Landing back at base.")
        
        # code goes here
        self.flying = False
        self.status = 2
        self.droneLog.info("Landed back at base.")

    # check if drone can fly
    def checkDrone(self):
        self.droneLog.info("Checking!")
        self.status = 1

        # code to check hardware goes here
        self.battery = self.getBattery()
        self.altitude = self.getAltitude()
        self.orientation = self.getOrientation()
     
        #low battery check
        if self.battery <= 20 :
            self.status = 5
            self.errorCode = 1
            self.droneLog.info("Error: Low Battery")
        else:
            self.status = 2
            self.droneLog.info("Check passed.")
    
    # update flight mode
    def updateFlightMode(self):

        self.droneLog.info("Switched flying mode: "+ str(self.flyMode))
    
    # toggle cameras
    def switchCamera(self):
        self.droneLog.info("Switching camera.")

        # code to switch between cameras goes here
        if self.frontCamera:
            self.frontCamera = False
            self.droneLog.info("Switched to bottom camera.")
        else:
            self.frontCamera = True
            self.droneLog.info("Switched to front camera.")

    # get drone's battery
    def getBattery(self):

        # code to get hardware battery goes here
        # self.log.info("Battery: " + str(self.battery))

        return self.battery

    # get drone's altitude
    def getAltitude(self):

        # code to get altitude goes here
        # self.log.info("Drone altitude: " + str( self.y))
        return self.y
    
    # update drone info list
    def updateInfo(self):
        return self.status, self.battery, self.velocity, self.altitude, self.errorCode
    
    # stop whatever the drone is doing
    def stopEverything(self):

        # code goes here
        self.droneLog.info("Drone stopped.")
    
    def getCoords(self):
         return self.x, self.y, self.z, self.orientation
    
    def resetDrone(self):
        if self.flying:
            self.land()

        # drone coords
        self.x = 0
        self.y = 0
        self.z = 0
        self.orientation = self.getOrientation()
    
    # get drone's orientation
    def getOrientation(self):

        # code goes here
        return 90 # drones default at a 90 degree angle facing foward
    

        
        
        
