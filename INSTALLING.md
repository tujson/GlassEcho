
# Installing GlassEcho
Very detailed instructions on how to install GlassEcho. This is primarily a stopgap measure while I'm working on getting GlassEcho to a stable state.

## Installation
1. Install [Android Studio](https://developer.android.com/studio/install)
2. [Download](https://github.com/tujson/GlassEcho/archive/main.zip) the code, then extract it.
3. In Android Studio, go to File->Open and select the extracted folder.
4. You may need to install the proper SDK for GlassEcho. Since this can vary between users, best to Google around (reach out if you need help!). Android Studio should complain if there are any issues.
5. Set up your phone  
a. This depends on your phone. Try [this guide](https://developer.android.com/studio/debug/dev-options). If it's hard to understand, try searching on YouTube for your specific phone model.
6. You *should* be able to see your phone in Android Studio in the upper right to the left of the green play button.  
a. You may need to install the corresponding USB driver. The [Google USB Driver](https://developer.android.com/studio/run/win-usb) has always worked for me, but here's a [more comprehensive guide](https://developer.android.com/studio/run/oem-usb).
7. Make sure "phone" is selected in the drop down to the left of the drop down box containing your phone. Click run.
8. Wait for the app to be installed on your phone. Then disconnect your phone from your computer.
9. Connect Google Glass to your computer.
10. Make sure "glass" is selected on the drop down to the left of the drop down that should say something like "Glass".
11. Hit the green play button.
12. Ta-da! Everything is installed!

## Running
1. In the phone app, click "Connect"
2. Open the GlassEcho app on Glass
3. Scan the QR code that shows up on your phone with Glass  
a. If it errors out, just swipe down to exit the app on Glass and try again.
4. Everything should be connected now! Try using the GlassEcho app to send a test notification.  
a. If you can't get notifications from other apps, make sure to check the "notifications" tab on the phone app. Everything is currently disabled by default

Feel free to reach out if you have any questions!