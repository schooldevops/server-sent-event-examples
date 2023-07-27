# SSEBasic Client Application in SpringBoot

- SpringBoot를 이용하여 SSE Client 애플리케이션을 개발하기 위해서는 [WebClient](https://www.baeldung.com/spring-5-webclient) 라이브러리를 이용해야한다. 

## 의존성 추가하기 

- WebClient는 SpringBoot Webflux에 조재한다 그러므로 다음과 같이 의존성을 추가하자. 
- build.gradle 파일내 dependencies에 다음 내용을 추가한다. 

```groovy
	implementation 'org.springframework.boot:spring-boot-starter-webflux'

```

## 메인 애플리케이션에 CommandLineRunner 구현하기 

- 서버가 시작되면 바로 SSE subscribe를 conmand Line Runner에서 수행하고, 서버에서 내려오는 요청을 전달 받도록 테스트 할 것이다. 
- SseDemoApplication.java 파일을 다음과 같이 작업하자. 


```java
@Slf4j
@EnableScheduling
@SpringBootApplication
public class SseDemoApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(SseDemoApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		WebClient client = WebClient.create("http://localhost:8080/sse");
		ParameterizedTypeReference<ServerSentEvent<String>> type
				= new ParameterizedTypeReference<ServerSentEvent<String>>() {};

		Flux<ServerSentEvent<String>> eventStream = client.get()
				.uri("")
				.retrieve()
				.bodyToFlux(type);

		eventStream.subscribe(
				content -> log.info("Time: {} - event: name[{}], id [{}], content[{}] ",
						LocalTime.now(), content.event(), content.id(), content.data()),
				error -> log.error("Error receiving SSE: {}", error),
				() -> log.info("Completed!!!"));
	}
}
```

- ```@Slf4j```
  - lombok내 logging을 이용하기 위해 어노테이션을 걸어준다. 
- ```... implements CommandLinerunner```
  - 스프링부트가 시작하면서 아래 run() 메소드를 실행하도록 하낟. 
- ```public void run(String... args) throws Exception {}```
  - 이 메소드를 CommandLineRunner의 메소드를 구현하는 역할을 하며, 여기서 SSE 정보를 수신 받도록 한다. 

```java
		WebClient client = WebClient.create("http://localhost:8080/sse");

```

- WebClient로 서버와 접속을 수행한다. 우리가 접근할 경로는 ```/sse``` 이다. 

```java
		ParameterizedTypeReference<ServerSentEvent<String>> type
				= new ParameterizedTypeReference<ServerSentEvent<String>>() {};

		Flux<ServerSentEvent<String>> eventStream = client.get()
				.uri("")
				.retrieve()
				.bodyToFlux(type);
```

- 위와 같이 반환값에 대한 타입을 지정한다. 
- ServerSentEvent는 오직 문자열만 보낼수 있기 때문에 String 타입으로 지정했다. 
- 또한 단순 타입을 수신받는 것이 아니라 ParameterizedTypeReference<ServerSentEvent<String>> 으로 ServerSentEvent가 문자열 반환하는 타입이라고 명시적으로 기술해 주었다. 
- Flux는 복수개의 응답값을 작성할때 사용되는 Reactive 타입이다. 우리는 여기서 응답값으로 ServerSentEvent 겍체를 Flux로 받겠다고 선언했다.
- client는 WebClient 의 기본 경로에서 uri("") 에 해당하는 리소스에 접근하고, 해당 리소스를 조회(retrieve)한다. 
- 결과적으로 bodyToFlux(type) 를 통해서 결과타입을 지정하여 클라이언트로 반환하면 실제 ServerSentEvent<String> 객체를 내려받게 된다. 

```java
		eventStream.subscribe(
				content -> log.info("Time: {} - event: name[{}], id [{}], content[{}] ",
						LocalTime.now(), content.event(), content.id(), content.data()),
				error -> log.error("Error receiving SSE: {}", error),
				() -> log.info("Completed!!!"));
```

- 위 evetnStream객체가 커넥션 생성의 결과라면 이제는 서버에서 내려오는 메시지를 수신하도록 설정하자. 
- content: 영역
  - content 영역은 정상적인 메시지가 왔을때 수행되어야할 이벤트를 lambda로 기술하고 있다. 
- error: 영역
  - error 영역은 서버에서 오류가 내려온경우 처리하는 부분으로 lambda로 기술하였다. 
- (): 영역
  - 이것은 위 content, error 이외의 처리를 수행할때 사용하므로 default로 생각하면 된다. 

## 결과 확인하기. 

- 서버를 기동하면 조금의 시간이 흐른 후 아래와 같이 서버에서 스트림으로 메시지가 내려오는 것을 로그로 확인할 수 있다. 

```go
... 생략 ...

2023-07-27T11:05:56.811+09:00  INFO 33450 --- [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2023-07-27T11:05:56.811+09:00  INFO 33450 --- [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2023-07-27T11:05:56.811+09:00  INFO 33450 --- [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 0 ms
2023-07-27T11:05:58.588+09:00  INFO 33450 --- [ctor-http-nio-2] c.s.sse.ssedemo.SseDemoApplication       : Time: 11:05:58.588124 - event: name[null], id [null], content[ScheduledId: ba108702-c71f-40ff-864b-01c747c70a95] 
2023-07-27T11:06:00.569+09:00  INFO 33450 --- [ctor-http-nio-2] c.s.sse.ssedemo.SseDemoApplication       : Time: 11:06:00.569764 - event: name[null], id [null], content[ScheduledId: 7e22eafa-219c-4a8d-aef3-702589f360c5] 
2023-07-27T11:06:02.566+09:00  INFO 33450 --- [ctor-http-nio-2] c.s.sse.ssedemo.SseDemoApplication       : Time: 11:06:02.566321 - event: name[null], id [null], content[ScheduledId: 7748e58d-46f1-4ced-ad88-19547912c6fd] 
2023-07-27T11:06:04.567+09:00  INFO 33450 --- [ctor-http-nio-2] c.s.sse.ssedemo.SseDemoApplication       : Time: 11:06:04.567329 - event: name[null], id [null], content[ScheduledId: 74da9c14-e9df-42f8-aec3-62d6d664730e] 
2023-07-27T11:06:06.564+09:00  INFO 33450 --- [ctor-http-nio-2] c.s.sse.ssedemo.SseDemoApplication       : Time: 11:06:06.564296 - event: name[null], id [null], content[ScheduledId: 8294144a-9aeb-48d1-b5a3-4bfc860eb332] 
2023-07-27T11:06:08.566+09:00  INFO 33450 --- [ctor-http-nio-2] c.s.sse.ssedemo.SseDemoApplication       : Time: 11:06:08.566337 - event: name[null], id [null], content[ScheduledId: fd5dba7d-1059-40c6-9d0a-c6f641be1134] 
```

## WrapUp

- 지금까지 Server Sent Event의 클라이언트를 java 애플리케이션으로 작성해 보았다. 
- WebClient라는 라이브러리를 이용하며, 이를 통해 Reactive방식으로 서버의 푸시 이벤트를 수신받을 수 있게 된다. 

## 참고

- https://www.baeldung.com/spring-server-sent-events