
# GlassEcho  
Show Android notifications on Google Glass via Bluetooth  
  
## Instructions  
- Install the Glass app on Glass  
- Install the Phone app on your Phone  
- Open GlassEcho on Glass
- Open GlassEcho on Phone
- Click "Connect" on your Phone
	- Click "Allow" in the popup dialog.
	- Scan the QR code on your Phone with the GlassEcho app on Glass.
	- If it errors while reading the QR code, simply quit out of the Glass app and try scanning again.
- Glass should now be connected! Try sending a notification.

## Development Priorities
- Making the pairing process easier.
	- [It's hard](https://stackoverflow.com/questions/20336968/google-glass-gdk-how-to-communicate-with-android-device).
- Interacting with notifications on Glass.
	- Dismissing notifications
	- Showing a list of notifications rather than only the most recent one
	- Clearing dismissed notifications from phone from Glass
	- Replying to notifications via a Bluetooth keyboard connected to Glass
	- Interacting with notification actions on Glass (e.g. "Like", "Mark as read")
- Investigate robustness.
	- Ensure Glass can auto-reconnect if the Bluetooth connection is severed. 
	- Investigate why sometimes some notifications aren't chunked correctly (resulting in gibberish being shown on Glass).
- iOS support.
	- Very rudimentary iOS support is already implemented. It's totally different code, so this will take quite some time.
- Wifi support.
	- Looking into using Firebase Cloud Firestore to sync notifications. The hope is being able to walk around a Wifi saturated area without needing a phone! However, this does mean that the user needs to have some sort of technical proficiency to set up a Firebase project.

## Known Limitations
- No BLE connection. 
- XE UI doesn't work well on EE2. The underlying logic should carry over.
- Can't add to [static cards](https://developers.google.com/glass/develop/mirror/static-cards) (cards to the right of the Glass clock). The Mirror API is shut down!
