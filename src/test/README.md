# Testing

## Xbox-authenticated servers
You need a Microsoft OAuth 2.0 refresh token acquired with Minecraft client id.  
The test code uses client id from `Nintendo` by default *(client_id=00000000441cc96b)*,   
and if you want to use another client id to login, replace this line `val deviceInfo = XboxDeviceInfo.DEVICE_ANDROID` in `Main.kt`.

1. visit [this link](https://login.live.com/oauth20_authorize.srf?client_id=00000000441cc96b&redirect_uri=https://login.live.com/oauth20_desktop.srf&response_type=code&scope=service::user.auth.xboxlive.com::MBI_SSL)
 in your browser.  
2. get the microsoft authorization code
3. save the microsoft authorization code into `.ms_refresh_token` file and run the program once to convert the code into refresh token
