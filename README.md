# ST-Cooper-RF9500-Beast
SmartThings Device Handler for the Cooper RF9500 Battery Operated Switch. This device handler is not initially intended to be use on a regular basis from the SmartThings app as when using the app you should be using the lightbulb or group of light bulb device to control the light. This is intended to make the physical RF battery operated switch work.

## Details
 *  This device handler was written specifically for the Cooper RF9500 (RF Battery Operated Switch).
 *  The version # of the switches I tested with is 3.11 as listed on th back of the switch.
 *  FCC ID: UH2-RF9500
 *  IC: 4706C-RF9500

## This device handler supports
1. On / Off
2. Dimming
3. Virtual dimming below 0 and above 100 so if you are using multiple of these, you don't need to worry about them staying the same value. Up is up, down is down.

## Installation via GitHub Integration
1. Open SmartThings IDE in your web browser and log into your account.
2. Click on the "My Device Types" section in the navigation bar.
3. Click on "Settings".
4. Click "Add New Repository".
5. Enter "ericvitale" as the namespace.
6. Enter "ST-Cooper-RF9500-Beast" as the repository.
7. Hit "Save".
8. Select "Update from Repo" and select "ST-Cooper-RF9500-Beast".
9. Select "cooper-rf9500-beast.groovy".
10. Check "Publish" and hit "Execute".
11. See the "Preferences" & "How to get your API Token" sections below on how to configure.

## Manual Installation (if that is your thing)
1. Open SmartThings IDE in your web browser and log into your account.
2. Click on the "My Device Types" section in the navigation bar.
3. On your Device Types page, click on the "+ New Device Type" button on the right.
4 . On the "New Device Type" page, Select the Tab "From Code" , Copy the "cooper-rf9500-beast.groovy" source code from GitHub and paste it into the IDE editor window.
5. Click the blue "Create" button at the bottom of the page. An IDE editor window containing device handler template should now open.
6. Click the blue "Save" button above the editor window.
7. Click the "Publish" button next to it and select "For Me". You have now self-published your Device Handler.
8. See the "Preferences" & "How to get your API Token" sections below on how to configure.

## Preferences
None at this time.
