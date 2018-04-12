 # JFXOpenCV
 
In the commit number 5 (12.04.2018) (8ad068fe5fcccda40da1d0ca204e2505d3db553f) the scanner algorithm has been greatly updated and it now works Live, that is it won't stop when it finds a plate number, but it will continue to scan for motion again.


I recommend you to build/run the application and read the console output in order to understand better the way it works right now.


Based on the future development of the DB server and after we'll see how it will integrate with this application, the output of the current application might not be needed anymore, but we must first make the connection with the DB. The most important thing is that this application will return the license plate number everytime something triggers it (for example movement). It is the line that shows "The final license plate number: XXXXXXX". On the DB server we should check everytime if the last received license plate is the same as the current received license plate to deny the server to make the client pay multiple times.

-------------------------------------------------------------------------------------
You may need to download and install (extract) OpenCV libraries to your local machine.
-- You can get this from this link: https://opencv.org/opencv-3-4-1.html
There are 2 ways you can do this, either clone the repo from their git source or
simply get the executable file and install it.
In order to set up the opencv for the project (done in Intellij)
on your local machine you can follow these steps:
https://medium.com/@aadimator/how-to-set-up-opencv-in-intellij-idea-6eb103c1d45c
