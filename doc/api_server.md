# Server
Thumbly has two servers. One is a main server that generate thumbnail, the other is a restful API server that provide application information and additional functions.

You can request api via HTTP.


### Usage
```html
http://example.com:{port}/{domain}/{method}
```

### Example
```html
http://example.com:57901/app/env
http://example.com:57901/app/status
http://example.com:57901/app/hello
http://example.com:57901/app/ping
```

# API (Cache)
## DELETE /cache/origin/{channel}/{path}
Delete cached origin image (Purge)
```json
{"data": true}
```

# API (Image)
## GET /image/secret/{secretPath}
Get secret url information
```json
{
  "data": "thumb-270x0-webp/thumbly_example1/product/ae/934/portfolio5a66f20fbe831.jpg"
}
```

## POST /image/secret
Set secret url
* Request
```json
{
  "data": [
    {
      "path":"thumb-270x0-webp/thumbly_example1/product/ae/934/portfolio5a66f20fbe831.jpg", 
      "expires":100
    }
  ]
}
```
* Response
```json
{
  "data": [
    {
      "path": "thumb-270x0-webp/thumbly_example1/product/ae/934/portfolio5a66f20fbe831.jpg",
      "secretPath": "7dff0be6adda34635522bf457233843e20846ac9f5779dd3f05485c3828d1b17d2792991ebc5b001804ceb75473e2b53f44377edd162ce6b238b27a298c4253a"
    }
  ]
}
```

# API (App)
## GET /app/env
Get application environments
```json
{
  "data":{
    "applicationVersion": "1.4.0",
    "hostname": "hostname",
    "osVersion": "10.11.6",
    "jarFile": "code13k-thumbly-1.0.0-alpha.1.jar",
    "javaVersion": "1.8.0_25",
    "ip": "192.168.0.121",
    "javaVendor": "Oracle Corporation",
    "osName": "Mac OS X",
    "cpuProcessorCount": 4
  }
}
```
#### GET /app/status
Get application status
```json
{
  "data":{
    "threadInfo":{...},
    "cpuUsage": 2.88,
    "threadCount": 25,
    "currentDate": "2018-10-02T01:15:21.290+09:00",
    "startedDate": "2018-10-02T01:14:40.995+09:00",
    "runningTimeHour": 0,
    "vmMemoryUsage":{...}
  }
}
```
## GET /app/config
Get application configuration
```json
{
  "data": {
    "cluster": {
      "nodes": [
        "192.168.100.1",
        "192.168.100.2"
      ],
      "port": 57910
    },
    "cache": {
      "totalSizeOfThumbnailImages": 1073741824,
      "rootDirectory": ".cache",
      "totalSizeOfOriginImages": 1073741824
    },
    "port": {
      "mainHttp": 57900,
      "apiHttp": 57901
    }
  }
}
```
## GET /app/hello
Hello, World
```json
{"data":"world"}
```
## GET /app/ping
Ping-Pong
```json
{"data":"pong"}