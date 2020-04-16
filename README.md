# Total_Fifo_chat

This application implements  total ordering as well as FIFO, while handling app failures. 
It gurantees -
  *FIFO which is first come first serve gurantee in respect to the sender.
  *TOTAL which is when all the nodes (in our case android emulators) have the same order of msgs.
  
ONE EMULATOR WILL CRASH in the test showing that the other 4/5 running emulator still keeps consistant order of msgs without loss.

#Requirements-
- jdk 8
- linux (tested on Ubuntu 18)
- android studio
- android emulator kitkat 4.4 with Google APIs Intel x86 Atom System Image checked
- Make sure hardware acceleration is turned on  (https://developer.android.com/studio/run/emulator-acceleration.html#accel-vm)
- run "apt-get install -qq -y libc6:i386 libgcc1:i386 libstdc++6:i386 libz1:i386"
- You need to have the Android SDK and Python 2.x (not 3.x; Python 3.x versions are not compatible with the scripts provided.) installed on your machine. If you have not installed these, please do it first and proceed to the next step.
- Add <your Android SDK directory>/tools/bin to your $PATH so you can run Android’s development tools from anywhere.
        ◦ To find out what your Android SDK directory is, click File -> Settings (on Mac, Android Studio -> Preferences), go to Appearance & Behavior -> System Settings -> Android SDK. On the top right side, it will show your Android SDK location.
        ◦ A good reference on how to change $PATH is here.
- Add <your Android SDK directory>/platform-tools to your $PATH so you can run Android’s platform tools from anywhere.
- Add <your Android SDK directory>/emulator to your $PATH so you can run Android’s emulator from anywhere.
- python create_avd.py 5 <your Android SDK directory>
  
#How to run-
Before you run the program, please make sure that you are running five AVDs. "python2 run_avd.py 5" will do it. Then run "python2 set_redir.py 10000", this will set up ports among the emulators

Make sure that groupmessenger2-grading.linux  has appropriate permission. (use "sudo chmod +x groupmessenger2-grading.linux" otherwise"

Feel free to test with "./groupmessenger2-grading.linux -h"  note that "-h" will explain how to test  or  run 5 emulators from knock yourself out.  

-enjoy-

Created by - (me) Syed Rehman
Contact    - syedrehm@buffalo.edu 
           - sedrehman@gmail.com
 

