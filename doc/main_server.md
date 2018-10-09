# Server
Thumbly has two servers. One is a main server that generate thumbnail, the other is a restful API server that provide application information and additional functions.

You can request thumbnail via HTTP.

## Main HTTP Server
### Usage
```html
http://example.com:{port}/{secret_path}
http://example.com:{port}/{command}/{channel}/{path}
http://example.com:{port}/status/{secret_path}
http://example.com:{port}/status/{command}/{channel}/{path}
```
* port
  * Server port
  * It's *main_http* in app_config.yml.
* command
  * Thumbnail command
  * Command syntax : type-size-format-quality
  * Example
     * thumb-200x0-webp-100
     * resize-200x200-origin
     * origin
     * thumb-0x200
     * resize-100x100-origin-50
     * origin-origin-origin-50
* channel
  * Channel name
  * It's *channel* in channel_config.yml
* path
  * Origin path
* status
  * Check if thumbnail is generated and cached in disk.

### Example
```html
http://example.com:57900/thumb-200x0-webp/thumbly_exampl1/test.jpg
http://example.com:57900/status/thumb-200x0-webp/thumbly_exampl1/test.jpg
http://example.com:57900/7dff0be6adda34635522bf457233843e20846ac9f5779dd3f05485c3828d1b17d2792991ebc5b001804ceb75473e2b53f44377edd162ce6b238b27a298c4253a
http://example.com:57900/status/7dff0be6adda34635522bf457233843e20846ac9f5779dd3f05485c3828d1b17d2792991ebc5b001804ceb75473e2b53f44377edd162ce6b238b27a298c4253a
```