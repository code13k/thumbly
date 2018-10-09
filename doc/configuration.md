# Configuration
Thumbly has three configuration files.


## app_config.yml
It's application configuration file.
```yaml
# Server port
port:
  main_http: 57900
  api_http: 57901

# Cache configuration
cache:
  root_directory: ".cache"
  total_size_of_origin_images: "1G"
  total_size_of_thumbnail_images: "1G"
  
# Cluster
#cluster:
#  port: 57910
#  nodes:
#    - 192.168.100.1
#    - 192.168.100.2
```
* Thumbly has two servers. One is a main server that generate thumbnail, the other is a restful API server that provide application information and additional functions. You can edit server port.
* Thumbly support clustering. You can cluster node by adding nodes IP. 
  * If you want to use clustering nodes, uncomment cluster configuration and add nodes IP.


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
