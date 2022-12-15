# Testing

## Xbox-authenticated servers
You need a refresh token acquired from minecraft(appid 00000000441cc96b).   

1. visit [this link](https://login.live.com/oauth20_authorize.srf?client_id=00000000441cc96b&redirect_uri=https://login.live.com/oauth20_desktop.srf&response_type=code&scope=service::user.auth.xboxlive.com::MBI_SSL)
 in your browser.  
2. get the m.r3_bay code
3. send a POST request to [https://login.live.com/oauth20_token.srf](https://login.live.com/oauth20_token.srf)
 with body
```
client_id=00000000441cc96b&redirect_uri=https://login.live.com/oauth20_desktop.srf&grant_type=authorization_code&code=$YOUR_CODE
```
4. get the refresh token from response body  
5. save the refresh token into `.ms_refresh_token` file that allows the program able to use it  
