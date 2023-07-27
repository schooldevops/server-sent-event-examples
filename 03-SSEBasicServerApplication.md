# Server Application

- 이제는 기본적인 Server Application을 만들어 볼 차례이다. 
- 서버 애플리케이션을 개발할때에는 2가지 프레임워크를 사용할 수 있다. 
  - Spring Web MVC 이용
  - Spring WebFlux 이용
- 기본 서버 애플리케이션 개발에서는 Spring Web MVC를 이용할 것이다. 

## 작업 절차

1. SpringBoot 개발환경 구성 
2. RestController에서 SSE Subscribe 요청으로 커넥션 생성하기.
3. 서버에서 Client로 SSE 메시지 전송하기 
4. 클라이언트의 요청을 받았을때 Client로 SSE 메시지 전송테스트 하기

## 개발환경

- 다음과 같은 기본 준비사항이 필요하다. 
  - java version 1.7
  - SpringBoot 2.5.x or SpringBoot 3.1.x
  - servlet 3.1+
  - org.springframework.boot:spring-boot-starter-web

## 클라이언트 Subscribe 요청 처리하기

- 서버가 할일은 SSE Subscribe 를 요청할때 이를 받아들이고 SseEmitter 객체를 생성하는 것이다. 
- SSEController.java 클래스를 생성하고, 다음과 같이 Subscribe 요청을 처리하는 메소드를 만들자. 

```java
@RestController
public class SSEController {
    private SseEmitter emitter;

    @GetMapping(path="/sse", produces= MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter createConnection() {
        emitter = new SseEmitter();
        return emitter;
    }

    ... 생략
}
```

- 위와 같이 SseEmitter 전역 변수를 작성했다. 

```java
@GetMapping(path="/sse", produces= MediaType.TEXT_EVENT_STREAM_VALUE)
```

- Subscribe는 Get http 요청으로 받게 되므로 @GetMapping을 지정했다. 
- ```produces=MediaType.TEXT_EVENT_STREAM_VALUE``` 는 요청에 대한 클라이언트로 text/event-stream 컨텐츠 타입으로 내려주게 된다. 
- 이렇게 되면 클라이언트는 서버로 부터 스트림을 받을 수 있다. 

```java

        emitter = new SseEmitter();
        return emitter;
```

- 클라이언트 접속 하나에 대해서 SseEmitter 객체를 생성했다.
- SseEmitter은 클라이언트 하나를 의미한다. 

## 서버에서 Client로 SSE 메시지 전송하기

- 이제 서버에서 클라이언트로 주기적(매초)으로 클라이언트에 랜덤한 문자를 전송해보자. 
- 이를 위해서 다음과 같은 설정이 필요하다. 

### 서버 스케줄링 활성화 하기

- Main 애플리케이션 파일 ```SseDemoApplication.java``` 에 클래스 annotation으로 @EnableScheduling 을 걸어주자. 

```java
@EnableScheduling
@SpringBootApplication
public class SseDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SseDemoApplication.class, args);
	}

}
```

- @EnableScheduling을 걸어주면 내부적으로 스케줄링 동작이 수행된다. 

### 매 1초당 서버에서 클라이언트로 메시지 보내기

- SseEmitter이 생성되어 있다면, 매 2초마다 서버에서 클라이언트로 랜덤 UUID를 전송하자. 
- ```SSEController.java``` 파일에 다음과 같이 메소드에 스케줄러를 걸어준다. 

```java
    @Scheduled(fixedRate = 2000)
    public void sendRandomMessage() {
        try {
            emitter.send("ScheduledId: " + UUID.randomUUID().toString());

            // emitter.complete(); <-- 메시지를 주기적으로 보내려면 SseEmitter 를 닫으면 안된다.
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
```

- @Scheduled 를 통해서 주기적으로 sendRandomMessage가 실행된다.  
- 여기서는 fixedRate를 지정했으므로 매 2초마다 수행된다. 
- ```emitter.send()``` 로 텍스트 문자를 푸시한다. 
- ```emitter.complete()``` 는 클라이언트 요청 처리가 완료 되었음을 나타낸다. 
  - 여기서는 complete() 메소드로 커넥션을 close하면 안되므로 주석처리했다. . 

### 테스트하기 

```go
$ curl -X GET http://localhost:8080/sse
data:ScheduledId: 29ed9d4f-7e17-477c-9778-46f97cf216e2

data:ScheduledId: e349de5b-38c6-4f89-8d0c-d74177aee3bf

data:ScheduledId: 6541d8af-4a60-4a2d-b1d9-c02ed4205616

data:ScheduledId: 53b35b5c-2d58-4520-a5de-8abd22e2f274

...
```

- 위와 같이 2초에 한번씩 메시지를 클라이언트로 전송하는 것을 확인할 수 있다. 

- 만약 서버를 먼저 끊으면 다음과 같은 클라이언트측 메시지가 나타난다. 

```go
curl: (18) transfer closed with outstanding read data remaining
```

- "미해결된 읽기 데이터가 남아 있는 상태에서 전송이 종료 되었음" 이라는 내용이다. 

## 클라이언트 요청에 대해 push 메시지 보내기

- 이번에는 클라이언트가 요청을 보내면, 그것을 http 리스폰스가 아닌 Sse로 푸시해보자. 

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

- source from: https://github.com/aliakh/demo-spring-sse/blob/master/README.md

- 위 예제는 /words 로 get요청을 보내면 미리 준비된 WORDS 의 단어들을 클라이언트로 내려보내준다. 
- 매번 클라이언트 요청이 들어올때마다 SseEmitter를 생성하고 있음을 확인하자. (즉, 이것은 요청에 대한 응답을 SSE로 하고 단어가 모두 전송되면 커넥션을 바로 종료한다는 의미이다. )
- ```emitter.complete()``` 는 전송이 끝났으니 서버에서 커넥션을 끊겠다고 알려주는 것이다. 
- ```emitter.completeWithError(e)``` 는 예외 발생시 예외 내용을 클라이언트에 전송하고, 커넥션을 종료한다는 의미가 된다. 

### 태스트하기

- 아래와 같이 테스트 결과를 확인할 수 있다. 

```go
$ curl -v http://localhost:8080/words        

*   Trying 127.0.0.1:8080...
* Connected to localhost (127.0.0.1) port 8080 (#0)
> GET /words HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.88.1
> Accept: */*
> 
< HTTP/1.1 200 
< Content-Type: text/event-stream
< Transfer-Encoding: chunked
< Date: Wed, 26 Jul 2023 23:57:04 GMT
< 
data:The

data:quick

data:brown

data:fox

data:jumps

data:over

data:the

data:lazy

data:dog.

* Connection #0 to host localhost left intact
```

- 단어가 매 초마다 클라이언트로 SSE를 통해 푸시 되었음을 알 수 있다. 

## WrapUp

- 지금까지 SSE 서버를 애플리케이션을 작성했다. 
- SpringBoot Web 을 이용해서 서버 푸시를 작성했으며, 별도의 의존성 라이브러리 없이 표준으로 SSE 를 작성할 수 있었다. 
- SseEmitter 는 클라이언트 요청마다 새로 생성하며, 이를 통해서 메시지를 전송할 수 있음을 알수 있었다. 
- SSE는 정상적으로 메시지를 보내기 위해서 send() 메소드를, 연결을 종료하기 위해서 complete() 메소드를, 그리고 예외사항인경우 completeWithError(e) 메소드를 이용하여 클라이언트로 푸시 할 수 있다. 
- 관련 소스는: https://github.com/schooldevops/server-sent-event-examples/blob/main/sse-demo/README.md 에서 확인할 수 있다. 

## 참고

- https://github.com/aliakh/demo-spring-sse/blob/master/README.md