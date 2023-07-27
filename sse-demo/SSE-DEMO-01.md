# Server-Sent Events (SSE) in Spring 5 with Web MVC and Web Flux

from: https://github.com/aliakh/demo-spring-sse

- 단순하고 일반적인 목적으로 서버에서 클라이언트로 커뮤니케이션을 수행하고 성능을 만족하는 비동기 웹 애플리케이션 구현체는 없는것 같다. 
- HTTP 는 요청/응답 프로토콜이며, 클라이언트-서버 컴퓨팅 모델이다. 
- exchange를 위해서 클라이언트는 서버에 요청을 서브밋 해야한다. 
- exchange 요청이 끝이나면 서버는 클라이언트로 응답을 내ㅐ 보낸다. 
- 서버는 응답 메시지를 오직 하나의 클라이언트로만 전송할 수 있다. 
- HTTP 프로토콜을 통해서 클라이언트는 메시지 교환을 위한 초기화를 수행한다.

- 서버가 교환 개시자가 되어야 하는 경우가 있다. 이를 구현하는 방법 중 하나는 pub/sub 컴퓨팅 모델을 통해서 서버가 클라이언트로 메시지를 푸시하도록 허용하는 것이다. 
- 교환을 시작하기 위해서 클라이언트는 서버의 메시지를 구독한다. 
- 교환을 수행하는 동안 서버는 메시지를 많은 구독자 클라이언트로 보내고, 클라이언트는 구독을 취소할 수 있다.

- Server Sent Event (SSE) 는 단순한 기술을 통해서 서버에서 클라이언트로 특정 웹 어플리케이션을 통해서 비동기로 전송할 수 있는 기술이다. 

## Overview

- 클라이언트가 비동기 수정에 대한 서버의 메시지를 수신하도록 허용하는 몇가지 방법이 있다.
- 여기에는 2가지 기술로 구분되며 하나는 client pull, 다른 하나는 server push가 그것이다. 

### Client Pull

- Client Pull 기술은 클라이언트가 주기적으로 서버에 업데이트에 대한 요청을 날리는 것이다. 
- 서버는 업데이트가 있으면 응답을 보내고, 아직 업데이트가 되지 않으면 특정 응답을 보내서 상태 변화를 알린다. 
- 그리고 여기에는 2가지 타입의 기술이 있으며 하나는 short polling, 다른 하나는 long polling 이디.

#### Short Polling

- client는 주기적으로 서버에 요청을 보낸다. 
- 만약 서버가 업데이트 사실이 있다면 클라이언트로 요청에 대한 응답으로 보내고, 커넥션을 끊는다. 
- 만약 서버가 업데이트 사실이 없다면 특정 응답을 클라이언트로 보내고, 역시 커넥션을 끊는다. 

#### Long Polling

- 클라이언트가 서버로 요청을 전송한다. 
- 서버는 업데이트가 있으면 응답을 클라이언트로 보내고 연결을 끊는다. 
- 만약 업데이트가 없다면 서버는 업데이트가 있을때 까지 커넥션을 유지하게 된다. 
- 업데이트가 생기면 서버는 응답을 클라이언트로 보내고, 커넥션을 끊는다. 
- 만약 업데이트가 일정 기간동안 없다면 서버는 특정 응답을 클라이언트로 보내고 커넥션을 끊게 된다. 

### Server Push

- Server Push 기술은 서버가 주기적으로 메시지를 클라이언트로 즉시 전송한다. 
- 여기에는 2가지 기술이 있으며 하나는 Server-Send Event 이고 다른 하나는 WebSocket이다. 

#### Server-Sent Event

- Server-Sent Event 는 텍스트 메시지를 서버에서 클라이언트로 브라우저 기반으로 전송한다. 
- Server-Sent Event 는 엳속적인 커넥션을 HTTP 프로토콜을 통해서 유지한다. 
- Server-Sent Event 는 네트워크프로토콜을 가지고 Event Source 클라이언트 인터피에스를 통해서 수행한다. 
- client interface는 HTML3 의 표준으로 W3C의 부분이다.

#### WebSocket

- WebSocket 은 양방향 실시간 커뮤니케이션을 동시에 구현한 것으로 웹 어플리케이션으로 수행한다. 
- WebSocket은 HTTP 프로토콜을 이용하며, 네트워크 인프라를 위해서 추가적인 설정이 필요하며 (proxy server, NAT, firewall 등이 있다.)
- 그러나 websocket 은 성능을 보장하지만 http기반 기술중 어려운 부분을 이해해야 한다. 

## SSE network protocol

- server event 를 수신받기 위해서 클라이언트는 GET 요청을 헤더와 함께 요청해야한다. 
  - Accept: text/event-stream 은 표준에 따른 이벤트 미디어 타입을 설정해야한다. 
  - Cache-Control: no-cache 는 이벤트 캐시는 활성화 하지 않는다. 
  - Connection: keep-alive 는 사용할 커넥션을 유지한다. 

```json
GET /sse HTTP/1.1
Accept: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

- server 는 헤더가 포함된 응답으로 구독을 확인해야한다. 
  - Content-Type: text/event-stream;charset=UTF-8 
    - 미디어 타입을 가리키며, 표준에 의해 필요한 이벤트의 인코딩을 수행한다. 
  - Transfer-Encoding: chunked 
    - 서버가 동적으로 생성된 콘텐츠를 스트리밍하므로 콘텐츠 크기를 미리 알 수 없음을 나타낸다.

```json
HTTP/1.1 200
Content-Type: text/event-stream;charset=UTF-8
Transfer-Encoding: chunked
```

- subscribing 이후에 서버는 메시지를 바로 전송한다. 
- 이벤트들은 텏흐트 메시지이고, UTF-8 인코딩으로 수행된다. 
- 이벤트들은 두 줄바꿈 문자로 서로 구분된다. 
- 각 이벶트들은 단일 개행 문자로 구분되며, 하나 혹은 여러개의 name: value 필드를 가진다. 

- data 필드에서 서버는 이벤트를 전송할 수 있다. 이는 2개의 뉴라인으로 구분됨

```json
data: The first event.

data: The second event.
```

- 서버는 data 필드를 여러 라인으로 분리할 수 있고, 뉴라인으로 구분된다.

- id 필드에서 서버는 유니크 이벤트 구분자를 전송할 수 있다. 
- 만약 연결이 깨지면, 클라이언트는 자동으로 서버에 리커넥트를 수행한다. 그리고 가장 마지막 이벤트를 수신한다. 
- 이때 헤더는 Last-Event-ID 를 받는다. 

```json
id: 1
data: The first event.

id: 2 
data: The second event.
```

- event 필드에서 서버는 이벤트 타입을 전송할 수 있다. 
- 서버는 서로다른 이벤트의 타입을 전송할 수 있고, 동일한 구독에 대해서 특정 타입없이 전송도 가능하다. 

```json
event: type1
data: An event of type1.

event: type2
data: An event of type2.

data: An event without any type.
```

- retry 필드에서 서버는 타임아웃을 보낼 수 있다. (밀리초단위) 
- 커넥션이 깨지면 클라이언트는 자동으로 리커넥트를 수행할 수 있다.
- 만약 필드가 지정되지 않으면, 기본적으로 3000 밀리초 (3초)로 설정된다. 

```json
retry: 1000
```

- 만약 라인에서 ':' (콜론)으로 시작하면 클라이언트에 의해서 무시된다. 
- 이것은 서버로 부터 커멘트를 보낼대 사용되거나, 타임아웃에 의해서 프록시 커넥션이 클로징되는 것으로 부터 방지한다.

```json
: ping
```

## SSE client: EventSource interface

- 커넥션을 오픈하기 위해서, EventSource 객체를 생성해야한다. 

```js
var eventSource = new EventSource('/sse);
```

- Server-Sent Event 가 이벤트를 서버에서 클라이언트로 전송하는 목적에도 불구하고, 클라이언트에서 서버로 GET 요청을 전송할 수 있다. 

```js
var eventSource = new EventSource('/sse?event=type1); 
...
eventSource.close();
eventSource = new EventSource('/sse?event=type1&event=type2);
...
```

- 커넥션을 클로즈 하기 위해서 close() 메소드를 호출할 수 있다. 

```js
eventSource.close();
```

- readyState 속성은 연결 상태를 나타내는데 사용한다. 
    - EventSource.CONNECTING = 0
      - 서버와 커넥션이 아직 생성되지 않은경우이며, 혹은 클로즈 되어 리커넥트를 수행하는 경우의 상태이다.
    - EventSource.OPEN = 1
      - 클라이언트는 커넥션을 오픈했고, 이들로 부터 수신을 위해 이벤트를 핸들링 한다. 
    - EventSource.CLOSED = 2
      - 커넥션이 오픈되지 않았고, 클라이언트가 리커넥션을 시도하지 않는 경우이다. 
      - 이는 fatal error이나, close() 메소드가 호출된 경우 발생한다. 

- 커넥션이 생성된 것을 핸들링을 위해서 onopen 이벤트 핸들러를 추가할 수 있다. 

```js
eventSource.onopen = function () {
   console.log('connection is established');
};
```

- 커넥션 상태의 변화나 fatal error를 처리하기 위해서 onerror 이벤트 핸들러를 구독할 수 있다. 

```js
eventSource.onerror = function (event) {
    console.log('connection state: ' + eventSource.readyState + ', error: ' + event);
};
```

- event 필드 없이 이벤트를 수신하기 위해서 onmessage 이벤트 핸들러를 구독할 수 있다. 

```js
eventSource.onmessage = function (event) {
    console.log('id: ' + event.lastEventId + ', data: ' + event.data);
};
```

- event 필드를 함께 수신하기 위해서 이벤트 핸들러를 다음과 같이 구독할 수 있다. 

```js
eventSource.addEventListener('type1', function (event) {
    console.log('id: ' + event.lastEventId + ', data: ' + event.data);
}, false);
```

- EventSource 클라이언트 인터페이스는 가장 최근의 브라우저에서 구현되어 있다. 
- https://caniuse.com/#feat=eventsource

## SSE Java server: Spring Web MVC

### 소개

- 스프링 WebMVC 프레임워크 5.2.0 은 Servlet 3.1 API를 기반으로 작성되어 있다. 
- 이는 thread pool 을 이용하여 비동기 자바 웹 어플리케이션을 구현한다. 
- 이는 Servlet 3.1 이상 버젼에서 수행이 가능하며, Tomcat 8.5와 Jetty 9.3 이상에 탭재 되어 있다. 

### Overview

- Spring Web MVC 프레임워크로 이벤트 전송을 구현하기 위해서 다음이 필요하다. 
  1. controller class 를 생성하고, @RestController 어노테이션을 설정한다. 
  2. client connection 을 위한 메소드를 생성하고, [SseEmitter](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html) 를 반환하도록 메소드를 만든다. 이는 GET 요청을 처리하고, text/event-stream 을 프로듀싱한다. 
     1. 새로운 SseEmitter 를 생성하고, 저장한 후, 메소드의 결과로 반환한다. 
  3. 이벤트를 비동기로 전송한다. 다른 쓰레드에서 SseEmitter 를 저장하고, SseEmitter.send 메소드를 필요한만큼 여러번 호출하면 된다. 
     1. 이벤트 전송이 종료되기 위해서 [SseEmitter.complete()](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/ResponseBodyEmitter.html#complete--) 메소드를 호출한다.
     2. 이벤트를 예외적으로 전송을 종료하기 위해서 [SseEmitter.completeWithError()](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/ResponseBodyEmitter.html#completeWithError-java.lang.Throwable-) 를 호출하자. 
- 컨트롤러 소스는 다음과 같다. 

```java
@RestController
public class SseWebMvcController

    private SseEmitter emitter;

    @GetMapping(path="/sse", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter createConnection() {
        emitter = new SseEmitter();
        return emitter;
    }

    // in another thread
    void sendEvents() {
        try {
            emitter.send("Alpha");
            emitter.send("Omega");

            emitter.complete();
        } catch(Exception e) {
            emitter.completeWithError(e);
        }
    }
}
```

- 이벤트를 오직 data 필드만 전송하기 위해서 [SseEmitter.send(Object object)](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html#send-java.lang.Object-) 메소드를 이용할 수 있다.
- 이벤트를 data, id, event, retry 와 커멘트를 함께 보내기 위해서는 [SseEmitter.send(SseEventBuilder builder)](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html#send-org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder-) 를 이용하자. 

- 아래 예외는 동일한 이벤트를 여러 클라이언트에 전송한다. SseEmitters 가 이 처리를 구현하고 있다. 
- 클라이언트 커넥션을 위해서 add(SseEmitter emitter) 메소드가 구현되어 있으며, SseEmitter 를 저장하며 thread safe 하다.
- 이벤트를 비동기로 전송하기 위해서 send(Object obj) 메소드가 있으며 이는 동일한 이벤트를 모든 연결된 클라이언트에 전송한다. 

- 단순화된 코드는 다음과 같다. 

```java
class SseEmitters {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    SseEmitter add(SseEmitter emitter) {
        this.emitters.add(emitter);

        emitter.onCompletion(() -> {
            this.emitters.remove(emitter);
        });
        emitter.onTimeout(() -> {
            emitter.complete();
            this.emitters.remove(emitter);
        });

        return emitter;
    }

    void send(Object obj) {
        List<SseEmitter> failedEmitters = new ArrayList<>();

        this.emitters.forEach(emitter -> {
            try {
                emitter.send(obj);
            } catch (Exception e) {
                emitter.completeWithError(e);
                failedEmitters.add(emitter);
            }
        });

        this.emitters.removeAll(failedEmitters);
    }
}
```

### short-lasting 주기적 이벤트 스트림 처리하기 

- 이 예제에서 서버는 short-lasting 주기 이벤트 스트림을 전송한다. 단어의 한정된 스트림을 매초마다 전송하고 단어가 끝나면 종료한다. 
- 이를 구현하기 위해서 SseEmitters 클래스가 사용되었다. 
- 이벤트를 비동기로 전송하기 위해서 그리고 주기적으로 수행하기 위해서 cached thread pool 이 생성되었다. 
- 이벤트 스트림은 short-lasting 이기 때문에 각 클라이언트 커넥션은 스레드풀에 분리된 태스크로 서브밋 된며 바로 컨트롤러 메소드 내부에 작성된다. 

```java
@Controller
@RequestMapping("/sse/mvc")
public class WordsController {

   private static final String[] WORDS = "The quick brown fox jumps over the lazy dog.".split(" ");

   private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

   @GetMapping(path = "/words", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   SseEmitter getWords() {
       SseEmitter emitter = new SseEmitter();

       cachedThreadPool.execute(() -> {
           try {
               for (int i = 0; i < WORDS.length; i++) {
                   emitter.send(WORDS[i]);
                   TimeUnit.SECONDS.sleep(1);
               }

               emitter.complete();
           } catch (Exception e) {
               emitter.completeWithError(e);
           }
       });

       return emitter;
   }
}
```

- 이벤트 클라이언트로 curl 커맨드라인 툴을 이용하였다. 

```shell
curl -v http://localhost:8080/sse/mvc/words
```

- 브라우저에서 http://localhost:8090/sse/mvc/words 를 요청하면 자바스크립트 로그로 결과가 노출된다. 
- 클라이언트 자바스크립트 소스는 다음과 같다. 

```javascript
<!DOCTYPE html>
<html lang="en">
<head>
   <meta charset="UTF-8">
   <title>Server-Sent Events client example with EventSource</title>
</head>
<body>
<script>
   if (window.EventSource == null) {
       alert('The browser does not support Server-Sent Events');
   } else {
       var eventSource = new EventSource('/sse/mvc/words');

       eventSource.onopen = function () {
           console.log('connection is established');
       };

       eventSource.onerror = function (error) {
           console.log('connection state: ' + eventSource.readyState + ', error: ' + event);
       };

       eventSource.onmessage = function (event) {
           console.log('id: ' + event.lastEventId + ', data: ' + event.data);

           if (event.data.endsWith('.')) {
               eventSource.close();
               console.log('connection is closed');
           }
       };
   }
</script>
</body>
</html>
```

- 이벤트 클라이언트 예제 결과는 EventSource 라는 자바스크립트 클라이언트 라이브러리를 이용한다. 
- 이것은 자동으로 재연결 (reconnect) 을 수행한 결과를 확인할 수 있다. 

### Handling long-lasting periodic events

- 이 예제에서 서버는 long-lasting periodic event stream을 전송한다. 
- 이는 무한 스트림으로 매초마다 정보를 내보낸다.
  - 가상 메모리 크기
  - 토탈 스왑 공간 크기
  - 사용가능한 free 스왑 공간 크기
  - 총 물리 메모리 크기
  - 사용가능한 free 메모리 크기
  - 시스템 CPU 로드
  - 프로세스 CPU 로드 

- PerformanceService 클래스를 구현하는것은 OperatingSystemMXBean 클래스를 이용하여 구현된 것이며, 운영체제의 성능 정보를 읽는다. 
- 또한 SseEmitters 클래스역시 사용했다. 
- 이벤트를 비동기로, 주기적으로 전송하기 위해서 scheduled thread pool 이 생성되었다. 
- 이벤트 스트림은 long-lasting 이며, 단일 태스크가 이벤트를 모든 클라이언트에 동시에 전송하기 위해서 thread pool 로 서브밋 되었다. 

```java
@RestController
@RequestMapping("/sse/mvc")
public class PerformanceController {

   private final PerformanceService performanceService;

   PerformanceController(PerformanceService performanceService) {
       this.performanceService = performanceService;
   }

   private final AtomicInteger id = new AtomicInteger();

   private final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

   private final SseEmitters emitters = new SseEmitters();

   @PostConstruct
   void init() {
       scheduledThreadPool.scheduleAtFixedRate(() -> {
           emitters.send(performanceService.getPerformance());
       }, 0, 1, TimeUnit.SECONDS);
   }

   @GetMapping(path = "/performance", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   SseEmitter getPerformance() {
       return emitters.add();
   }
}
```

- 이벤트 클라이언트는 Highcharts Javascript 라이브러리를 이용하여, 서버 성능 정보를 그려주고 있다. 

### Handling aperiodic events

- 이 예제에서는 서버가 비 주기적인 이벤트를 스트림으로 전송하며, 파일의 변경사항에 대한 내용을 보낸다. 
- create, modify, delete 처리가 폴더에 발생하는 것을 감시하다가 발생하면 전송한다. 
- 폴더로 현재 사용자의 홈 폴더가 사용되며 이는 System.getProperty("user.home") 속성을 이용한다. 

- FolderWatchService 클래스를 구현하기 위해서 java NIO 파일을 이용하였고, watch 기능을 사용하였다. 
- 또한 SseEmitters 클래스를 이용하였다. 
- 이벤트를 비동기로 비 주기적으로 전송하기 위해서 FolderWatchService 클래스라 스프링 어플리케이션 이벤트를 프로듀스 한다. 
- 이것은 controller에 의해서 컨슘 된다. (listener 메소드로 구현하였다.)

```java
@RestController
@RequestMapping("/sse/mvc")
public class FolderWatchController implements ApplicationListener<FolderChangeEvent> {

   private final FolderWatchService folderWatchService;

   FolderWatchController(FolderWatchService folderWatchService) {
       this.folderWatchService = folderWatchService;
   }

   private final SseEmitters emitters = new SseEmitters();

   @PostConstruct
   void init() {
       folderWatchService.start(System.getProperty("user.home"));
   }

   @GetMapping(path = "/folder-watch", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   SseEmitter getFolderWatch() {
       return emitters.add(new SseEmitter());
   }

   @Override
   public void onApplicationEvent(FolderChangeEvent event) {
       emitters.send(event.getEvent());
   }
}
```

- 클라이언트를 위해서는 Javascript 클라이언트 라이브러리로 EventSource 를 이용했다. 

## SSE Java server: Spring Web Flux

### 소개

- Spring Web Flux 프레임워크는 5.2.0 을 사용하였고 이는 Reactive Stream API를 기준으로 한다. 
- 그리고 event-loop 컴퓨팅 모델을 이용하여 비동기 자바 웹 어플리케이션을 구션하였다. 
- 어플리케이션들은 non-blocking 으로 수행되고, Netty 4.1과 Undertow 1.4와 Server 3.1+ 컨테이너를 Tomcat 8.5 와 Jetty 9.3 에 수행했다.

### Overview

- Spring Web Flux 프레임워크로 이벤트를 전송하기 위해서 다음과 같이 했다. 
  1. Controller 클래스를 생성하고, @RestController 어노테이션을 달았다. 
  2. 클라이언트 커넥션을 생성하기 위해서 메소드를 생성했고, 이벤트를 전송했다. 반환값은 Flux를 수행하였으며, 요청에 대해서 GET메소드로 text/event-stream 으로 프로듀스 했다. 
     1. 메소드 응답으로 Flux 를 새로 생성했다. 

```java
@RestController
public class ExampleController

    @GetMapping(path="/sse", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> createConnectionAndSendEvents() {
        return Flux.just("Alpha", "Omega");
    }
}
```

- 오직 데이터필드로만 이벤트를 전송하기 위해서 Flux<T> 타입을 이용했다. 
- 이벤트를 필드(data, id, event, retry, comments등)와 함께 전송하기 위해서 Flux<ServerSentEvent<T>> 을 사용했다. 

### Handling short-lasting periodic event stream 

- 다음 예제는 서버가 short-lasting periodic 이벤트 스트림을 전송한다. 
- 이는 단어의 유한 스트림이며 매초마다 전송한다. 이는 단어가 끝이 날때까지 전송된다. 
- 구현을 위해서 다음과 같이 수행했다. 
  - 단어의 Flux를 생성하기 위해서 Flux.just(WORDS) 를 수행했다. 이는 Flux<String> 타입이다. 
  - Flux 를 생성하고 long 값을 매초마다 전송하는 것을 구현하기 위해서 Flux.interval(Duration.ofSeconds(1)) 으로 Flux<Long> 타입으로 지정했다. 
  - zip 메소드에 의해서 Flux<Tuple2<String, Long>> 을 서로 연결했다. 
  - Flux<String> 타입의 map(Tuple2::getT1)에 의해서 튜플의 첫번재 엘리먼트를 추출했다 .

```java
@RestController
@RequestMapping("/sse/flux")
public class WordsController {

   private static final String[] WORDS = "The quick brown fox jumps over the lazy dog.".split(" ");

   @GetMapping(path = "/words", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   Flux<String> getWords() {
       return Flux
               .zip(Flux.just(WORDS), Flux.interval(Duration.ofSeconds(1)))
               .map(Tuple2::getT1);
   }
}
```

- 이 예제를 위한 이벤트 클라이언트들은 WEB MVC 예제에서 사용한것이다. 

### Handling long-lasting periodic events

- 이 예제에서 서버는 long-=lasting 주기 이벤트 스트림을 보낸다.
- 매초마다 서버의 성능 정보가 무한 스트림으로 전송될 것이다 .
- 구현하기 위해서ㅓ :
  - 매 초마다 증가되는 long 값을 emit 하기 위해서 Flux를 생성했다. 이는 Flux.interval(Duration.ofSeconds(1)) 을 사용했으며 이는 Flux<Long> 타입이다. 
  - map(sequence -> perfornamceService.getPerformance()) 메소드로 이 타입은 Flux<Performance> 에 의해서 변환된다. 

```java
@RestController
@RequestMapping("/sse/flux")
public class PerformanceController {

   private final PerformanceService performanceService;

   PerformanceController(PerformanceService performanceService) {
       this.performanceService = performanceService;
   }

   @GetMapping(path = "/performance", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   Flux<Performance> getPerformance() {
       return Flux
               .interval(Duration.ofSeconds(1))
               .map(sequence -> performanceService.getPerformance());
   }
}
```

- 이 예제의 클라이언트는 이전에 사용한 예제와 동일하다. 

### Handling aperiodic events

- 이 예제에서 서버는 감시중인 폴더의 파일변경 (생성, 수정, 삭제)에 대한 비주기적 이벤트 스트림을 보낸다.
- 폴더로 현재 사용자의 홈 폴더를 사용하며 이는 System.getPeoperty("user.home") 프로퍼티 값으로 정의되어 있따. 

- FolderWatchService 클래스는 Java NIO 파일을 이용하여 구현되어 있으며 watch 기능을 사용한다. 
- 이벤트를 비동기로 비주기적으로 전달한다. FolderWatchService 클래스는 스프링 어플리케이션 이벤트를 프로듀스 한다. 
- 이것은 컨트롤러에 의해서 컨슘 된다. (리스너 메소드 구현에 의해 수행됨)
- 컨트롤러 리스너 메소드는 SubscribableChannel 로 이벤트를 전송한다. 
- 이는 컨트롤러에서 수행되며 이벤트의 Flux로 프로듀스 된다. 

```java
@RestController
@RequestMapping("/sse/flux")
public class FolderWatchController implements ApplicationListener<FolderChangeEvent> {

   private final FolderWatchService folderWatchService;

   FolderWatchController(FolderWatchService folderWatchService) {
       this.folderWatchService = folderWatchService;
   }

   private final SubscribableChannel subscribableChannel = MessageChannels.publishSubscribe().get();

   @PostConstruct
   void init() {
       folderWatchService.start(System.getProperty("user.home"));
   }

   @GetMapping(path = "/folder-watch", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   Flux<FolderChangeEvent.Event> getFolderWatch() {
       return Flux.create(sink -> {
           MessageHandler handler = message -> sink.next(FolderChangeEvent.class.cast(message.getPayload()).getEvent());
           sink.onCancel(() -> subscribableChannel.unsubscribe(handler));
           subscribableChannel.subscribe(handler);
       }, FluxSink.OverflowStrategy.LATEST);
   }

   @Override
   public void onApplicationEvent(FolderChangeEvent event) {
       subscribableChannel.send(new GenericMessage<>(event));
   }
}
```

## SSE limitations

- SSE의 제약은 다음과 같다. 
  - 메시지 전송은 오직 단방향으로만 수행된다. 서버에서 클라이언트로만 전송
  - 텍스트 메시지만 전송이 가능하다. Base64 인코딩을 이용할 수 있음에도 gzip 압축을 수행해도 이것은 비효율적이게 된다. 
- 구현에 대한 SSE 의 제약사항은 다음과 같다. 
  - 인터넷 Explorer/Edge 와 많은 모바일 브라우저는 SSE를 지원하지 않는다. polyfills를 이용하더라도 이는 비효율 적이다. 
  - 많은 브라우저는 SSE 커넥션의 제한된 수만 오픈할수 있다. (Chrome, Firefox 각 브라우저당 6개의 커넥션만 가능)

- 참고 지원되는 브라우저 버젼: 
  - https://www.lambdatest.com/web-technologies/eventsource
  
## 결론

- 완료된 코드는 https://github.com/aliakh/demo-spring-sse 을 참조하자.