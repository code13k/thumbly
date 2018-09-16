# Thumbly is a on-demand image server for generating thumbnail dynamically.
- It dynamically generate thumbnail image on request.
- Generated thumbnails are cached in disk.
- It provide secret url that is automatically deleted after specific time.
- It use Imagemagick for image processing

[![Build Status](https://travis-ci.org/code13k/thumbly.svg?branch=master)](https://travis-ci.org/code13k/thumbly)


# Supported image format
* JPG
* GIF
* PNG
* WEBP


# Dependency
* Imagemagick
  * https://www.imagemagick.org
  * Thumbly use Imagemagick for image processing
 

# Configuration

## app_config.yml
It's application configuration file.
```yaml
# Server port
port:
  main_http: 57910
  api_http: 57911

# Cache configuration
cache:
  root_directory: ".cache"
  total_size_of_origin_images: "1G"
  total_size_of_thumbnail_images: "1G"
```

## channel_config.yml
It's channel configuration file.
```yaml
# Example-1
thumbly_example1:
  browser_cache_expiration: "3600s"
  normal_url_enabled: true
  secret_url_enabled: true
  type: "http"
  base_url: "https://data.forsnap.com"

# Example-2
thumbly_example2:
  browser_cache_expiration: "3600s"
  normal_url_enabled: true
  secret_url_enabled: false
  type: "http"
  base_url: "https://data.forsnap.com"

# Example-3
thumbly_example3:
  browser_cache_expiration: "3600s"
  normal_url_enabled: false
  secret_url_enabled: true
  type: "aws_s3"
  access_key: ""
  secret_key: ""
  region: ""
  bucket: ""
```

## logback.xml
It's Logback configuration file that is famous logging library.
* You can send error log to Telegram.
  1. Uncomment *Telegram* configuration.
  2. Set value of `<botToken>` and `<chatId>`.
       ```xml
       <appender name="TELEGRAM" class="com.github.paolodenti.telegram.logback.TelegramAppender">
           <botToken></botToken>
           <chatId></chatId>
           ...
       </appender>
       ```
  3. Insert `<appender-ref ref="TELEGRAM"/>` into `<root>`
     ```xml
     <root level="WARN">
         <appender-ref ref="FILE"/>
         <appender-ref ref="TELEGRAM"/>
     </root>
     ```
* You can send error log to Slack.
  1. Uncomment *Slack* configuration.
  2. Set value of `<webhookUri>`.
       ```xml
       <appender name="SLACK_SYNC" class="com.github.maricn.logback.SlackAppender">
           <webhookUri></webhookUri>
           ...
       </appender>
       ```
  3. Insert `<appender-ref ref="SLACK"/>` into `<root>`
     ```xml
     <root level="WARN">
         <appender-ref ref="FILE"/>
         <appender-ref ref="SLACK"/>
     </root>
     ```
* You can reload configuration but need not to restart application.


# Server
Thumbly has two servers. 
One is a main server that generate thumbnail
The other is a restful API server that provide application information and additional functions.

## Main HTTP Server
### Usage
```html
http://example.com:{port}/{command}/{channel}/{path}
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

### Example
```html
http://example.com:57910/thumb-200x0-webp/thumbly_exampl1/test.jpg
```

## API HTTP Server
### Usage
```html
http://example.com:{port}/{domain}/{method}
```

### Example
```html
http://example.com:57911/app/status
http://example.com:57911/app/hello
http://example.com:57911/app/ping
```

### API
#### GET /cache/origin/{channel}/{path}
* Get cached origin image information
##### Response
```json
{
  "data": {
    "responseHeaders": {
      "cacheControl": "public",
      "contentLength": "7189763",
      "contentType": "image/jpeg",
      "date": "Sun, 16 Sep 2018 13:58:52 GMT",
      "etag": "\"5a66f210-6db503\"",
      "expires": "Sun, 16 Sep 2018 14:03:52 GMT",
      "lastModified": "Tue, 23 Jan 2018 08:28:00 GMT"
    },
    "fileSize": 7189763,
    "filePath": ".cache/origin/e146f95f6e015c9999fb2f4ec4a9f041.jpg",
    "lastAccessedTimeSeconds": 0,
    "expiredTimeSeconds": 1537106634,
    "url": "https://data.forsnap.com/product/ae/934/portfolio5a66f20fbe831.jpg"
  }
}
```

#### DELETE /cache/origin/{channel}/{path}
* Delete cached origin image (Purge)
##### Response
```json
{"data": true}
```

#### GET /image/secret/{secretPath}
* Get secret url information
##### Response
```json
{
  "data": {
    "expired": 87,
    "originPath": "thumb-270x0-webp/thumbly_example1/product/ae/934/portfolio5a66f20fbe831.jpg"
  }
}
```

#### POST /image/secret
* Set secret url
##### Request
```json
{
  "data": [
    {
      "secretPath":"test3", 
      "originPath":"thumb-270x0-webp/thumbly_example1/product/ae/934/portfolio5a66f20fbe831.jpg", 
	  "expired":100,
    }
  ]
}
```
##### Response
```json
{
  "data": [
    {
      "result": true,
      "expired": 100,
      "originPath": "thumb-270x0-webp/thumbly_example1/product/ae/934/portfolio5a66f20fbe831.jpg",
      "secretPath": "test3"
    }
  ]
}
```


#### GET /app/status
* Get application status and environment.
##### Response
```json
{
  "data":{
    "applicationVersion":"0.1.0-alpha.3",
    "cpuUsage":2.56,
    "threadInfo":{...},
    "vmMemoryFree":"190M",
    "javaVersion":"1.8.0_25",
    "vmMemoryMax":"3,641M",
    "currentDate":"2018-09-16T18:48:58.795+09:00",
    "threadCount":15,
    "startedDate":"2018-09-16T18:48:40.901+09:00",
    "javaVendor":"",
    "runningTimeHour":0,
    "osName":"Mac OS X",
    "cpuProcessorCount":4,
    "vmMemoryTotalFree":"3,585M",
    "hostname":"",
    "osVersion":"10.11.6",
    "jarFile":"code13k-thumbly-0.1.0-alpha.3.jar",
    "vmMemoryAllocated":"245M",
  }
}
```
#### GET /app/hello
* Hello, World
##### Response
```json
{"data":"world"}
```

#### GET /app/ping
* Ping-Pong
##### Response
```json
{"data":"pong"}