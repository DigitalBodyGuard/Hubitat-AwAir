# Awair Element Driver for Hubitat

## About
This is a Hubitat driver for the [Awair Element](https://www.getawair.com/home/element)
air quality monitor. It uses the  local API available in the device for better
reliability and ease of setup.

## Driver Installation
Add the contents of the AwAir_Driver.groovy file as a custom driver in Hubitat.
This can be done easily with the following steps:

1. Launch the management site of your Hubitat hub.
2. Go into the Drivers Code section and choose the "New Driver" button.
3. Choose "Import" and fill in the URL
   `https://raw.githubusercontent.com/DigitalBodyGuard/Hubitat-AwAir/master/AwAir_Driver.groovy`
4. Choose to save the driver.

## Adding the Device
**Note:** The driver requires knowing the IP of your Awair Element device. It is
recommended that you use static DHCP reservations as a change in IP will require
reconfiguring the device.

### Enabling Element Local API
Before adding an Awair Element to Hubitat, you need to enable its local API
support. This is on a per-device basis, so you will need to follow these steps
for each Element you want to add.

1. Open the Awair Home application and go to the device you wish to set up in Hubitat.
2. From the device screen, go to the "Awair+" section and then choose "Awair APIs".
3. From the API options, choose "Local API" and then "Enable Local API". There is
   no indicator that this is enabled other than the popup alert when you choose
   the enable option. 

### Creating the Device in Hubitat
This driver doesn't use a companion application, so you will need to create your
device manually. This is a straightforward process that can be done by following
these steps:

1. Launch the management site of your Hubitat hub.
2. Go to the devices section and choose the `Add Virtual Device` button.
3. In the `Device Name` field, enter the name you want to give to your Awair device.
4. Leave the Network ID field alone, and change the `Type` to "AwAir".
5. Save the device. This will create the device in Hubitat and allow you to access
   the device preferences.
6. Configure the `IP Address` preference to the IP of your Awair Element device.
   The leading "http://" is required, so do not remove it.
7. Choose "Save Preferences". You are done. On the next poll of the device, the
   device states will be updated with their actual values.
   
## Driver Preferences

The driver supports the following custom preferences to configure:

- **IP Address**: A URL-styled IP address to your Awair Element device. The default
  value is the example `http://192.168.1.3`. You will want to change this to match
  the IP of your Element.
- **Path Address**: The path to the JSON-encoded report of air quality. This defaults
  to `/air-data/latest` and should not need to be changed.
- **Time between status checks**: The number of seconds between polls of the
  Awair device. Setting this too low can result in stability issues with Hubitat.
  The default is 300 seconds (5 minutes).
- **Enable Alerts_pm25**: Enables/disables updating of the PM 2.5 value.
- **Alert Level pm2.5**: The value above which the PM 2.5 value is considered "bad". Default is "40".
- **Reset Alert Level pm2.5**: The value below which the PM 2.5 value is considered "good". Default is "30".
- **Enable Alerts_co2**: Enables/disables updating of the CO2 (Carbon Dioxide) value.
- **Alert Level co2**: The value above which the CO2 value is considered "bad". Default is "1000".
- **Reset Alert Level co2**: The value below which the CO2 value is considered "good". Default is "800".
- **Enable Alerts_voc**: Enables/disables updating of the VOC (Volatile Organic
  Compound) value.
- **Alert Level voc**: The value above which the VOC value is considered "bad". Default is "1000".
- **Reset Alert Level voc**: The value below which the VOC value is considered "good". Default is "800".
- **Enable Alerts_aiq**: Enables/disables updating of the Awair Score.
- **Alert Level low airQuality**: The value below which the Awair Score value is considered "bad". Default is "60".
- **Reset Alert Level high airQuality**: The value above which the Awair Score value is considered "good". Default is "70".

## License
This driver is released under the Creative Commons CC0 license.