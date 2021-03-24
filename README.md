# Autonomous Personal Assistant Drone

TODO: Video recording

APAD Version Changes

1.0: APAD App that launches in landscape mode. Connects to server to get live camera feed

2.0: Added ConnectionThread class that implements runnable to enable connect/disconnect to server
     Added connect and disconnect button
     Added AI class, contains all smart opencv operations like DNN module to identify objects

2.1: Added archs to lib to create static linking and avoid having to download opencv manager on phone or emulator

2.2: Added version change log to README.txt
     Added DroneServer.py to project which is the server script the app communicates with

2.3  Fixed disconnect button that was showing up when it was not supposed to
     Renamed ConntionThread to DroneConnect and added a listener interface which allows communication between threads

2.4  Removed the gui update listener in drone connect and replaced it with online status one to update status whenever it changes
     Added custom navigation buttons, a launch/land button, and an emergency land button, along with some xml files for the custom buttons
     Made a button array to handle all navigation buttons
     The order is 1-10: Land/takeoff, emergency, up, down, left, right, forward, backward, rotate left, rotate right
     Replaced connect and disconnect button with an online switch slider to connect to drone server
     Added an onClick method to handle all the navigation buttons

2.5 Added a DroneListener interface for all thread communication from drone(DroneConnect) to app(main)
    Added a AppListener interface for all thread communication from app(main) to drone(DroneConnect)
    Added a DroneConnect class that handles both drone video and drone navigation sockets
    Updated server to v3 to run two sockets for video and navigation
    Fix: Image view will only update if the received video packet is greater than one byte to prevent app from crashing when it tries to convert it to a bitmap
    Bug: onDisconnect listener in AppListener does work for droneNav but not for droneVideo for some reason, so using their disconnect methods instead

2.6 Found a way to send/get ints through sockets
    Updated drone server script to version 4 for sending/receiving navigation data
    Added listeners for getting and setting drone info
    Added an Excel sheet for the drone documentation and manual, which will change as the project updates
    Added mic button, follow me switch, settings button, switch camera button, and battery progress bar with % text
    Added a Drone.py that will represent the drone class to handle all drone activities. Drone server will use it directly
    Needs Debugging but the video works and both sockets close correctly

2.7 Fixed all found bugs from update 2.6, cleaned up a lot of logic and code in: main, drone connect, drone, and drone server
    Removed some listener functions from app listener due to not being needed
    Drone status is now: offline, checking, flying, landing, and error
    Added functionality to switch camera button and land at base button
    Camera button can save current image from view into phone's gallery
    Added location services as several functions to get phone's gps locations, off for now
    Fixed a bug where if battery went below 0 the server script would crash since it cant convert negative ints to bytes
    Drone will now land and send error code 1 (low battery) if battery is 20 or below, can't respond to commands after
    Created error codes values found in manual and added a geterrorcodestring function in main to handle error codes
    Fixed a bug where online switch would not reset if server is offline
    Fixed a bug where I forgot to add break statements in a switch to error status and default (facepalm)
    Fixed a bug where I forgot to update getDroneInfo in drone so the errorCode was not being sent thus not update on app
    Camera status is only be checked once when drone is initialized else it has a change of failing since its already being used by video socket

2.8 Tried using videowrite from opencv but does not work, doesn't create the file
    Fixed the constraints on the view

2.8.1 On button press changes color of button

2.8.2 Created a thread handler for when holding down the button to send continuous commands
      Changed view mode to immersive, hiding the phone UI
      Changed background of imageview in offline mode
      imported the pocketshpinx AAR as aar module and added as dependency to main app

2.8.3 Switched dataToDrone array to double to send gps data
      Edited server scripts to deal with doubles instead
      Can calculatre distance between gps coords

2.8.4 Added updated drone.py, droneServer.py, and ARDRONE.py which can be used to control the ar drone 2.0 by parot

2.8.5 Added voice functionality with pocketsphinx
      Always listening, use "apad" to wake up
      Added a chime when it detects "apad" so you know its waiting for a command
      Command words are stored in commands.gram in assets/sync
      Needs further debugging
2.8.6 Switch voice commands to activate only from buttons
      Fixed bug with recognition not stopping correctly
      Added additional sounds for app AI and code
      Added youtube android streaming API to project
      Switched imageview to a fragment to switch between map view
      Using a view model to communicate between fragment and main
      Updated server script to version 5, fixed a nav unpack issue by adding a if else checker
      Fixed a mediaplayer sound bug with wrong state when playing
      Added a google map fragment view to show where the drone is on the map
      Added a settings window
      Fixed settings window so it would disconnect the sockets before switching windows
      