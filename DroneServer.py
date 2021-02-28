import socket, cv2, sys, logging, time, struct
from Drone import Drone

# import thread module 
from threading import Thread

#=================Drone Server Version 4==================================

#logging
logging.basicConfig(level=logging.NOTSET)
log = logging.getLogger("Server")

#socket info
host_ip = '10.0.0.41' 
vport = 9999
nport = 9998
video_address = (host_ip, vport)
nav_address = (host_ip, nport)

#attempt to bind sockets
try:
    # video socket create
    video_socket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
    video_socket.bind(video_address)

    #nav socket create
    nav_socket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
    nav_socket.bind(nav_address)

except socket.error as e:
    log.debug("Failed to bind sockets: " + str(e))
    sys.exit()

#drone class
drone = Drone()

time.sleep(10)

#threaded video connection
def video_connect():
    
    encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 90]
    
    while True:
        c, vid_addr = video_socket.accept()
    
        log.info("Video connection established.")
        
        with c:
            while True:

                try:
                    #read frame
                    frame = drone.getFrame()
                
                    #resize and encode to jpg
                    #frame = imutils.resize(frame, width=320)
                    result, frame = cv2.imencode('.jpg', frame, encode_param)

                    #convert to bytes and get size
                    data = bytes(frame)    
                    size = len(data)
                
                    #send size of frame in big indian byte order
                    c.sendall(size.to_bytes(4, byteorder='big'))
                
                    #send frame
                    c.sendall(frame)

                    #print("sent: " + str(size) + " bytes")

                except socket.error as e:
                    log.debug("Video disconnected or lost connection.")
                    break
                except OverflowError as e:
                    log.debug("Overflow error: " + str(e))
                    log.info("Frame: " + str(frame))
                    log.info("Size: " + str(size))

#threaded nav connection
def nav_connect():
    
    
    while True:
        c, nav_addr = nav_socket.accept()
        
        log.info("Navigation connection established.")

        recv = [0,0,0,0,0]

        with c:
            
            while True:
                try:

                    #get latest data from drone
                    send = drone.getDroneData()

                    #log.info("passed getDroneData")

                    #recieve -> send
                    for x in range(0,len(recv)):

                        #recive bytes
                        data = c.recv(4096)

                        #conver from binary to int and place in recv list
                        integer = struct.unpack("!d",data)[0]
                        recv[x]= integer        

                        #send drone data to app
                        c.sendall(send[x].to_bytes(4, byteorder='big'))
                    
                    #log.info("recieved:" + str(recv))
                    
                    #log.info("sent:" + str(send))
                    
                    print(recv)
                    
                    #send app data to drone    
                    drone.sendAppData(recv)
                
                    #log.info("passed send data to app")
            
                except socket.error as e:
                    log.debug("Nav disconnected or lost connection.")
                    break
                except OverflowError as e:
                    log.debug("Overflow error: " + str(e))
                    log.info("Send: " + str(send))
                    log.info("Recieved" + str(recv))
        
        drone.resetDrone()
                

#listen for incoming connections
video_socket.listen()
nav_socket.listen()

log.info("Listening for incoming connections.")
    
    
#create threaded connections
new_threads = [Thread(target=video_connect), Thread(target= nav_connect)]

#launch
for th in new_threads:
    th.start()
    
