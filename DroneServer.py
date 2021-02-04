import socket, cv2, pickle,struct, imutils, pickle

# import thread module 
from threading import Thread

host_ip = '10.0.0.41'
port = 9999
socket_address = (host_ip,port)

# Socket Create
server_socket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
#host_name  = socket.gethostname()

server_socket.bind(socket_address)

vid = cv2.VideoCapture(0)

#change resolution, only supports native resolutions of camera 
vid.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
vid.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

#threaded connections
def connect(c, v):

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


# Socket Listen
server_socket.listen()
print("LISTENING AT:",socket_address)

# Server loop
while True:
    #wait for connection
    client_socket,addr = server_socket.accept()
        
    #start connection thread
    new_thread = Thread(target = connect, args = (client_socket, vid))
    new_thread.start()
        
        
server_socket.close()
cam.release()
