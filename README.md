# Autonomous Personal Assistant Drone

===================APAD-LandScape Version Changes=================/n

1.0: APAD App that launches in landscape mode. Connects to server to get live camera feed.

2.0: Added ConnectionThread class that implements runnable to enable connect/disconnect to server.
     Added connect and disconnect button.
     Added AI class, contains all smart opencv operations like DNN module to identify objects.

2.1: Added archs to lib to create static linking and avoid having to download opencv manager on phone or emulator.

2.2: Added version change log to README.txt.
     Added DroneServer.py to project which is the server script the app communicates with.

2.3  Fixed disconnect button that was showing up when it was not supposed to.
     Renamed ConntionThread to DroneConnect and added a listener interface which allows communication between threads.

2.4  Removed the gui update listener in drone connect and replaced it with online status one to update status whenever it changes.
     Added custom navigation buttons, a launch/land button, and an emergency land button, along with some xml files for the custom buttons.
     Made a button array to handle all navigation buttons.
     The order is 1-10: Land/takeoff, emergency, up, down, left, right, forward, backward, rotate left, rotate right.
     Replaced connect and disconnect button with an online switch slider to connect to drone server.
     Added an onClick method to handle all the navigation buttons.

