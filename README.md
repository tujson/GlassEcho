
# GlassEcho  
Show iOS notifications on Google Glass via Bluetooth.

**IN DEVELOPMENT** 

## Setup
These setup instructions are atrocious. Once I improve the post-pairing experience, I'll look into making this... usable.
- Download "LightBlue" on an iPhone
- Install this application on a Google Glass (tested with XE)
- In the LightBlue iOS app, click "Virtual Devices" at the bottom navigation bar.
- Then click the "+" icon to add a virtual device.
- Click "Blank"
- On Glass, unpair any Bluetooth devices.
- Pair a new device, look for "Blank" (the virtual device from your iPhone's LightBlue app).
- Pair.
- DO NOT TOUCH GLASS. Enter the pairing code seen on Glass in the dialog box that should've popped up in the LightBlue app.
- After ~10 seconds, Glass should indicate that it is successfully paired.
- On your iPhone, you may need to navigate back to the home screen for a permission dialog (asking if you want to give Glass notification permission) to pop up. You can double check this setting by going into iOS Settings -> Bluetooth -> Glass (your device), and seeing if "Share System Notifications" is toggled on.
- On Glass, inside the GlassEcho app, tap once, then tap again to select the menu item "Connect".
- After a few seconds, Glass should be connected!

As of now, you may need to go through this pairing process every time Glass is disconnected, or the app is stopped. If you're having trouble seeing the virtual device on Glass, restart the LightBlue app, or toggle the check mark next to the virtual device's name in the LightBlue app. 