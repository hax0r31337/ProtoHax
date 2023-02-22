# Testing

## Xbox-authenticated servers
You need a refresh token acquired from minecraft(appid 00000000441cc96b).   

1. visit [this link](https://login.live.com/oauth20_authorize.srf?client_id=00000000441cc96b&redirect_uri=https://login.live.com/oauth20_desktop.srf&response_type=code&scope=service::user.auth.xboxlive.com::MBI_SSL)
 in your browser.  
2. get the m.r3 code
3. save the m.r3 code into `.ms_refresh_token` file and run the program once to convert the code into refresh token
