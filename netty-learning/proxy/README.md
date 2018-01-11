Option                                  |Description    
------                                  |-----------    
-h, --remote-host                       |(e.g. api.icndb.com)
--help                                  |
-l, --local-port                        |<Integer> (default: 8080)
-p, --remote-port                       |<Integer> (default: 80)

```
$ ./gradlew clean build -p proxy
$ java -jar proxy/build/libs/proxy.jar -h api.icndb.com
$ curl -Ss -G "http://localhost:8080/jokes/random" --data-urlencode "limitTo=[nerdy,explicit]" | jq '.'
```

https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/proxy