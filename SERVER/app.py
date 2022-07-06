import socket
import io
import struct
from keras.models import Model, load_model
from keras.preprocessing import image
import numpy as np
from glob import glob
import tensorflow_hub as hub
import cv2
import PIL.Image as Image
import tensorflow as tf
IMAGE_SHAPE = (224, 224)
YOLO_SHAPE = (608, 608)
print("------...............-----")
clasificador = tf.keras.models.load_model('modeloPajaros.h5',custom_objects={'KerasLayer':hub.KerasLayer})

#clasificador = load_model('modeloPajaros.h5',custom_objects={'KerasLayer':hub.KerasLayer})
print("-----MODELO CARGADO-----")

net = cv2.dnn.readNetFromDarknet('yolov4.cfg', 'yolov4.weights')
with open('coco.names', 'r') as f:
    classes = f.read().splitlines()
model = cv2.dnn_DetectionModel(net)
model.setInputParams(scale=1 / 255, size=YOLO_SHAPE, swapRB=True)
print("-----YOLO CARGADO-----")

"""EMPIEZA ESCUCHA EN SERVIDOR"""
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind(('0.0.0.0',8050))
server.listen()
BUFFER_SIZE = 8192

while True: #BUCLE DE ESCUCHA
    try:
        client_socket, addr = server.accept() #Recibe datos
        print("Conectado desde:",addr)

        #Recibe primero 3 bytes para saber el valor de rotacion de la imagen
        data_received = client_socket.recv(3)
        c = int(data_received.decode()[2])
        print(c)

        file_stream = io.BytesIO()
        recv_data = client_socket.recv(BUFFER_SIZE)
        while recv_data:
            file_stream.write(recv_data)
            recv_data = client_socket.recv(BUFFER_SIZE)

        imgPIL = Image.open(file_stream)

        if (c == 0):
            imgPIL = imgPIL.rotate(0)
        elif(c == 1):
            imgPIL = imgPIL.rotate(-90)
        elif(c == 2):
            imgPIL = imgPIL.rotate(-180)
        else: #c == 3
            imgPIL = imgPIL.rotate(-270)

        img = np.array(imgPIL) # im2arr.shape: height x width x channel
        
        """DETECCION DE OBJETOS"""
        classIds, scores, boxes = model.detect(img, confThreshold=0.6, nmsThreshold=0.4)


        if(len(scores)<1):
            print("nada")
            client_socket.sendall('99\n'.encode())
        else:
            print(classIds)

            box = False
            if 14 not in classIds:
                client_socket.sendall('99\n'.encode())
            else:
                multiple = False #varios pajaros en imagen
                for classId in classIds:
                    if classId == 14 and not multiple:
                        box = boxes[0]
                        multiple = True
                        if(box.any()):
                            print("Pajaro detectado")
                            img = img[box[1]:box[1]+box[3], box[0]:box[0]+box[2]]
                            img = cv2.resize(img, IMAGE_SHAPE)
                            img = img/255
                            img = np.expand_dims(img, axis=0)
                            res = clasificador.predict(img) #Clasificamos la imagen
                            num_bird = np.argmax(res, axis=1)
                            print("Pajaro tipo: ",num_bird)
                            client_socket.sendall((str(num_bird[0])+'\n').encode())



        """for (classId, score, box) in zip(classIds, scores, boxes):
            cv2.rectangle(img, (box[0], box[1]), (box[0] + box[2], box[1] + box[3]),
                          color=(0, 255, 0), thickness=1)
            text = '%s: %.2f' % (classes[classId], score)
            cv2.putText(img, text, (box[0], box[1] - 5), cv2.FONT_HERSHEY_SIMPLEX, 1,
                        color=(0, 255, 0), thickness=2)"""
    except Exception as e:
        print(e)
