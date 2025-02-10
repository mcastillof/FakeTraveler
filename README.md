# FakeTraveler
<img alt="Logo" src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="80" />

Fake where your phone is located (Mock location for Android).

Sometimes you need to fake the location of your device (for privacy or to test an app). Fake Traveler provides you a map to select the location where you want your phone to be.

<a href="https://f-droid.org/app/cl.coders.faketraveler"><img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="100"></a>


## How does it work?

Long press in the map where you want to be located or type the latitude and longitude, and tap the Apply button. Tapping the gear (⚙) button (formerly the "..." button) will show two settings to mock the location over a period of time. 

<div style="display:flex;">
<img alt="App image" src="screenshots/Screenshot_Fake_Traveler_20180722-192131.png" width="30%">
<img alt="App image" src="screenshots/Screenshot_Fake_Traveler_20180722-192305.png" width="30%">
</div>

### Notes

If your reported location appears to bounce from one location to another or is otherwise unstable, you
may want to go to system **Settings**, and in **Location services**, disable **Wi-Fi scanning** and
**Bluetooth scanning** as these alternate location providers may compete with the GPS location data
you are mocking.

## Requirements?

In order to work, you need to allow Fake Traveler to mock locations. You have to [enable Developer options](https://developer.android.com/studio/debug/dev-options?hl=en-419) and select this app in "Settings/System/Developer options/Select mock location app" option.

<img alt="App image" src="screenshots/Screenshot_Settings_20180722-192328.png" width="30%">


## Changelogs

See fastlane/metadata/android/en-US/changelogs/


## License
Copyright © 2018 Matías Castillo Felmer

> This program is free software: you can redistribute it and/or modify
> it under the terms of the GNU General Public License as published by
> the Free Software Foundation, either version 3 of the License, or
> (at your option) any later version.
> 
> This program is distributed in the hope that it will be useful,
> but WITHOUT ANY WARRANTY; without even the implied warranty of
> MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
> GNU General Public License for more details.
> 
> You should have received a copy of the GNU General Public License
> along with this program.  If not, see <https://www.gnu.org/licenses/>.
    
The icon launcher is derivative of ["Location, map, navigation, pointer icon"](https://www.iconfinder.com/icons/2135924/location_map_navigation_pointer_icon) by First Styles, used under CC 3.0 BY. The icon Launcher was created using [Android Asset Studio](https://jgilfelt.github.io/AndroidAssetStudio/icons-launcher.html), and is licensed under CC 3.0 BY, by Matías Castillo Felmer. 
