import socket, cv2, jpysocket

# import thread module 
from threading import Thread

#=================Drone Server Version 3==================================
#dependencies: opencv (cv2), jpysocket

host_ip = '10.0.0.41'
vport = 9999
nport = 9998
video_address = (host_ip, vport)
nav_address = (host_ip, nport)

# video socket create
video_socket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
video_socket.bind(video_address)

#nav socket create
nav_socket = jpysocket.jpysocket()
nav_socket.bind(nav_address)

#setup camera
vid = cv2.VideoCapture(0)

#change resolution, only supports native resolutions of camera 
vid.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
vid.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

#threaded video connection
def video_connect(c, v):

    print("Video connection established.")
    encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 90]
        
    with c:
        
        while True:
            #read frame
            img,frame = v.read()
            
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

            print("sent: " + str(size) + " bytes")

            # print("sent ", frame) 
            # cv2.imshow('TRANSMITTING VIDEO',frame)
            # key = cv2.waitKey(1) & 0xFF
            # if key ==ord('q'):
            #client_socket.close()

#threaded nav connection
def nav_connect(c):

    print("Navigation connection established.")

    with c:

        while True:
            data = c.recv(1024)
            data = jpysocket.jpydecode(data)
            
            if data != "0":
                print(data)
            
            send = jpysocket.jpyencode(data)

            c.send(send)


#listen for incoming connections
video_socket.listen()
nav_socket.listen()

print("Listening for incoming connections.")

while True:
    #wait
    vid_client, vid_addr = video_socket.accept()
    nav_client, nav_addr = nav_socket.accept()

    #create threaded connections
    new_threads = [Thread(target=video_connect, args=(vid_client, vid)), Thread(target= nav_connect, args=(nav_client,))]

    #launch
    for th in new_threads:
        th.start()
    
