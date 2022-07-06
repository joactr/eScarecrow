# eScarecrow
Full system with an Android app and a server that is able to detect different bird species and scare away some of them depending on user input. Uses computer vision and machine learning models to detect its surroundings and act upon them.

The eScarecrow folder contains the Android app, made with Android studio and compatible with most smartphones, as they only need to have a camera, speaker and internet connection. The SERVER folder contains the server structure, which communicates with the app to detect and classify the birds.

The system works as follows. The Android app captures images every 15 seconds and sends them to the server, which then uses an object detection model called YOLOv4 to identify possible birds in the image. If a bird is indeed located, the image will be cropped around it and sent to a classifier model that is trained to support ten different urban bird species (modeloPajaros.h5), then the output is transformed to a number and sent back to the app. The app has a number for each bird and will scare them away depending on user preferences of each individual species.

This was started as a student project for university but development is still active and I will keep on adding new features to it.
