

# GlassEcho  

Show Android notifications on Google Glass via Bluetooth  
  
## Instructions  
- Install the Glass app on Glass  
- Install the Phone app on your Phone  
- Open GlassEcho on Phone
- Open GlassEcho on Glass
- Click "Connect" on your Phone
	- Click "Allow" in the popup dialog.
	- Scan the QR code on your Phone with the GlassEcho app on Glass.
	- If it errors while reading the QR code, simply quit out of the Glass app and try scanning again.
- Glass should now be connected! Try sending a notification. If you're not receiving notifications on Glass from other apps, make sure that app is enabled in the "Notification List" page on the phone app.

The QR code pairing process only needs to happen once. For subsequent launches, make sure you click the "Connect" button in the phone app before launching the Glass app. 

See the INSTALLING.md file for detailed instructions.

## Development Priorities
In no particular order:
- Making the pairing process easier.
	- Currently requires phone to be in connect mode first
	- Sometimes Glass just refuses to connect? Bluetooth is fun, y'all.
- Interacting with notifications on Glass.
	- Replying to notifications.
		- Currently flaky. Somewhat works via a bluetooth keyboard (tested with Tap Keyboard). Would like to get speech-to-text working.
- Investigate robustness.
	- Ensure Glass can auto-reconnect if the Bluetooth connection is severed. 
	- Have connection status indicators on phone and Glass.
- Wake screen on notification.
    - And/or hook into Glass XE's notification glance/head nudge.
- iOS support.
	- Very rudimentary iOS support is somewhat implemented in a different branch. It's totally different code, so this will take quite some time.
- Wifi support.
	- Looking into using Firebase Cloud Firestore to sync notifications. The hope is being able to walk around a Wifi saturated area without needing a phone! However, this does mean that the user needs to have some sort of technical proficiency to set up a Firebase project.
- Camera pictures
	- Being able to take a picture on Glass and have it show up on the phone.

## What Works
- Receiving notifications
- Dismissing notifications
- Performing notification actions (e.g. "Like", "Mark as Read", "Archive")
- Replying to notifications
	- Currently in progress.

## Known Limitations
- No BLE connection. 
- Can't add to [static cards](https://developers.google.com/glass/develop/mirror/static-cards) (cards to the right of the Glass clock). The Mirror API is shut down!
