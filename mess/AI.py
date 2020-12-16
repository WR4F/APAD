import cv2
import sys
import time
import numpy as np

# using caffee model
def run_caffe(video, image):
    #creating model
    prototext = 'MobileNetSSD_deploy.prototxt.txt'
    caffe = 'MobileNetSSD_deploy.caffemodel'
    model = cv2.dnn.readNetFromCaffe(prototext, caffe)
    print("model created")

    image_height, image_width, _ = image.shape
    bbox = (0,0,0,0)
    person = False

    #time.sleep(5)

    while True:
        # Read a new frame
        ok, image = video.read()
        if not ok:
            break

        model.setInput(cv2.dnn.blobFromImage(image, 0.007843, (300, 300), 127.5))
        output = model.forward()
        # print(output[0,0,:,:].shape)

        for detection in output[0, 0, :, :]:
            
            class_id = detection[1]          
            confidence = detection[2]

            if class_id == 15 and confidence > .5:
                
                #print(str(str(class_id) + " " + str(detection[2])  + " " + class_name))
                box_x = (detection[3] * image_width) - 50
                box_y = (detection[4] * image_height) -50
                box_width = (detection[5] * image_width) - 100
                box_height = (detection[6] * image_height) - 50
                #cv2.rectangle(image, (int(box_x), int(box_y)), (int(box_width), int(box_height)), (23, 230, 210), thickness=1)
                #cv2.putText(image, 'person',(int(box_x), int(box_y+.05*image_height)),cv2.FONT_HERSHEY_SIMPLEX,(.005*image_width),(0, 0, 255))
                bbox = (int(box_x),int(box_y),int(box_width),int(box_height))
                person = True
                return bbox

        #cv2.imshow('image', image)
        # cv2.imwrite("image_box_text.jpg",image)

        # Exit if ESC pressed
        k = cv2.waitKey(1) & 0xff
        if k == 27 or person:
            return bbox


def track(tracker_type, video, frame, bbox):

    tracker = None

    if tracker_type == 'BOOSTING':
        tracker = cv2.TrackerBoosting_create()
    if tracker_type == 'MIL':
        tracker = cv2.TrackerMIL_create()
    if tracker_type == 'KCF':
        tracker = cv2.TrackerKCF_create()
    if tracker_type == 'TLD':
        tracker = cv2.TrackerTLD_create()
    if tracker_type == 'MEDIANFLOW':
        tracker = cv2.TrackerMedianFlow_create()
    if tracker_type == 'CSRT':
        tracker = cv2.TrackerCSRT_create()
    if tracker_type == 'MOSSE':
        tracker = cv2.TrackerMOSSE_create()

    # Read first frame.
    ok, frame = video.read()
    if not ok:
        print('Cannot read video file')
        sys.exit()


    # Initialize tracker with first frame and bounding box
    ok = tracker.init(frame, bbox)

    while True:
        # Read a new frame
        ok, frame = video.read()
        if not ok:
            break

        # Start timer
        timer = cv2.getTickCount()

        # Update tracker
        ok, bbox = tracker.update(frame)

        # Calculate Frames per second (FPS)
        fps = cv2.getTickFrequency() / (cv2.getTickCount() - timer);

        # Draw bounding box
        if ok:
            # Tracking success
            p1 = (int(bbox[0]), int(bbox[1]))
            p2 = (int(bbox[0] + bbox[2]), int(bbox[1] + bbox[3]))
            cv2.rectangle(frame, p1, p2, (255,0,0), 2, 1)
        else :
            # Tracking failure
            cv2.putText(frame, "Tracking failure detected", (100,80), cv2.FONT_HERSHEY_SIMPLEX, 0.75,(0,0,255),2)

        # Display tracker type on frame
        cv2.putText(frame, tracker_type + " Tracker", (100,20), cv2.FONT_HERSHEY_SIMPLEX, 0.75, (50,170,50),2);

        # Display FPS on frame
        cv2.putText(frame, "FPS : " + str(int(fps)), (100,50), cv2.FONT_HERSHEY_SIMPLEX, 0.75, (50,170,50), 2);

        # Display result
        cv2.imshow("Tracking", frame)

        # Exit if ESC pressed
        k = cv2.waitKey(1) & 0xff
        if k == 27 : break


# video
video = cv2.VideoCapture(0)

# Read first frame.
ok, image = video.read()
if not ok:
    print('Cannot read video file')
    sys.exit()


bbox = run_caffe(video, image)

tracker_types = ['BOOSTING', 'MIL','KCF', 'TLD', 'MEDIANFLOW', 'CSRT', 'MOSSE']
tracker_type = tracker_types[2]

track(tracker_type,video,image,bbox)
