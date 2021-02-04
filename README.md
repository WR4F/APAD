# Autonomous Personal Assistant Drone

===================APAD-LandScape Version Changes=================
1.0: APAD App that launches in landscape mode. Connects to server to get live camera feed.

2.0: Added ConnectionThread class that implements runnable to enable connect/disconnect to server.
     Added connect and disconnect button.
     Added AI class, contains all smart opencv operations like DNN module to identify objects.

2.1: Added archs to lib to create static linking and avoid having to download opencv manager on phone or emulator.

2.2: Added version change log to README.txt
     Added DroneServer.py to project which is the server script the app communicates with
2.3  Fixed disconnect button that was showing up when it was not supposed to.
