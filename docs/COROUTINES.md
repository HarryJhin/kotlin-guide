# Coroutines (코루틴)

`Kotlin`은 다른 라이브러리에서 코루틴을 활용할 수 있도록 표준 라이브러리에 최소한의 low-level API를 제공합니다.
비슷한 기능을 가진 다른 언어와 달리, Kotlin에서는 `async`/`await` 키워드가 아니며, 표준 라이브러리의 일부로 포함되어 있지 않습니다.
게다가, `Kotlin`의 `suspending function` 개념은 비동기 작업을 위한 `future`나 `promise`보다 더 안전하고 오류가 적은 추상화를 제공합니다.

`kotlinx.coroutines`는 JetBrains에서 개발한 코루틴을 위한 라이브러리입니다.
이 라이브러리에는 `launch`, `async` 등을 포함한 여러 high-level 코루틴 지원 요소들이 포함되어 있습니다.

`kotlinx.coroutines`를 사용하려면 `build.gradle.kts`에 다음과 같은 의존성을 추가해야 합니다.

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}
```

## Coroutine Concepts

코루틴은 중단 가능한 연산의 인스턴스입니다.
개념적으로는 코드 블록을 실행하면서 다른 코드와 동시에 작동할 수 있다는 점에서 스레드와 유사합니다.
그러나 코루틴은 특정 스레드에 묶여 있지 않습니다.
한 스레드에서 실행을 중단할 수 있으며, 다른 스레드에서 다시 실행을 재개할 수 있습니다.

코루틴을 경량 스레드라고 생각할 수 있지만, 실제 사용에서 코루틴과 스레드를 구분 짓는 중요한 차이점들이 있습니다.
이러한 차이점들은 코루틴의 실질적인 활용 방식을 스레드와 매우 다르게 만듭니다.

다음 코드를 보면 코루틴이 어떻게 동작하는지 간단히 이해할 수 있습니다.

```kotlin
fun main() = runBlocking { // this: CoroutineScope
    launch { // launch a new coroutine and continue
        delay(1_000L) // non-blocking delay for 1 second (default time unit is ms)
        println("World!") // print after delay
    }
    println("Hello") // main coroutine continues while a previous one is delayed
}
```

위 코드는 다음과 같은 결과를 출력합니다.

```
Hello
World!
```

이 코드를 분석해봅시다.

`launch`는 **코루틴 빌더**(coroutine builder)로, 코드의 나머지 부분과 동시에 새로운 코루틴을 시작하며 해당 코드는 독립적으로 계속 실행됩니다.
이 때문에 Hello가 먼저 출력된 것입니다.

`delay`는 특별한 **중단 함수**(suspending function)로, 지정된 시간 동안 코루틴의 실행을 일시 중단합니다.
코루틴의 중단은 기본 스레드를 차단하지 않으며, 다른 코루틴이 실행되도록 하고 해당 스레드가 코드를 실행할 수 있게 합니다.

`runBlocking` 역시 코루틴 빌더로, 비 코루틴 영역인 `fun main()`과 중괄호 안의 코루틴 코드 간의 다리를 놓습니다.
IDE에서는 `runBlocking { ... }`의 여는 중괄호 바로 뒤에 `CoroutineScope` 힌트가 표시됩니다.

이 코드에서 `runBlocking`을 제거하거나 잊었다면 `launch` 호출 시 오류가 발생합니다.
이는 `launch`가 `CoroutineScope` 내에서만 호출할 수 있기 때문입니다.
`runBlocking`은 코루틴의 범위를 설정해주는 역할을 하므로, 이를 생략하면 코드가 실행될 스코프를 제공하지 못해 문제가 발생합니다.

```
Unresolved reference: launch
```

`runBlocking`이라는 이름은 그것을 실행하는 스레드(위 코드는 메인 스레드)가 `runBlocking { ... }` 내의 모든 코루틴이 실행을 완료할 때까지 차단된다는 의미입니다.
`runBlocking`은 주로 애플리케이션의 최상위 레벨에서 사용되며, 실제 코드 내에서는 거의 사용되지 않습니다.
이는 스레드가 값비싼 자원이기 때문에 이를 차단하는 것은 비효율적이며, 일반적으로 원하지 않는 상황이기 때문입니다.

### 구조적 동시성 (Structured concurrency)

구조적 동시성 원칙에 따라, 코루틴은 특정 `CoroutineScope` 내에서만 새로 시작될 수 있으며,
이 스코프는 코루틴의 생명 주기를 한정합니다.
위 코드는 `runBlocking`이 해당 스코프를 설정한다는 것을 보여줍니다.
이 때문에 위 코드에서는 1초 후 "World!"가 출력될 때까지 기다린 후에야 프로그램이 종료됩니다.

실제 애플리케이션에서는 많은 코루틴을 실행하게 됩니다.
구조적 동시성은 이러한 코루틴들이 잃어버려지거나 누출되지 않도록 보장합니다.
외부 스코프는 그 안에 있는 모든 자식 코루틴이 완료될 때까지 종료되지 않습니다.
또한, 구조적 동시성은 코드 내의 오류가 적절히 보고되며 절대 누락되지 않도록 보장합니다.

## Suspending 함수

이제 함수 추출 리팩토링을 해보겠습니다.
`launch { ... }` 블록 안의 코드를 변도의 함수로 추출하면 다음과 같습니다. 

```kotlin
fun main() = runBlocking { // this: CoroutineScope
    launch { doWorld() }
    println("Hello")
}

// this is suspending function
suspend fun doWorld() {
    delay(1_000L)
    println("World!")
}
```

이 코드는 이전 코드와 동일한 결과를 출력합니다.

중단 함수는 `suspend` 한정자를 가진 함수입니다.
이러한 함수는 일반 함수처럼 코루틴 내에서 사용할 수 있지만,
추가적인 특징은 다른 중단 함수(이 경우 `delay`)를 사용하여 코루틴의 실행을 중단할 수 있다는 점입니다.

## Scope 빌더

다양한 빌더가 제공하는 코루틴 스코프 외에도 `coroutineScope` 빌더를 사용하여 사용자 정의 스코프를 선언할 수 있습니다.
`coroutineScope`는 코루틴 스코프를 생성하며, 시작된 모든 자식 코루틴이 완료될 때까지 완료되지 않습니다.

`runBlocking`과 `coroutineScope` 빌더는 본문과 그 안의 모든 자식 코루틴이 완료될 때까지 대기한다는 점에서 비슷해 보일 수 있습니다.
그러나 주요 차이점은 `runBlocking`은 현재 스레드를 차단하여 대기하는 반면,
`coroutineScope`는 단순히 중단되어 기본 스레드를 다른 용도로 사용할 수 있데 해준다는 점입니다.
이 차이로 인해 `runBlocking`은 일반 함수이고, `coroutineScope`는 중단 함수입니다.

```kotlin
fun main() = runBlocking {
    doWorld()
}

suspend fun doWorld() = coroutineScope {  // this: CoroutineScope
    launch {
        delay(1_000L)
        println("World!")
    }
    println("Hello")
}
```

이 코드는 다음과 같은 결과를 출력합니다.

```
Hello
World!
```

## Scope 빌더와 동시성

`coroutineScope` 빌더는 중단 함수 내에서 여러 동시 작업을 수행하기 위해 사용할 수 있습니다.
`doWorld`라는 중단 함수 안에서 두 개의 동시 코루틴을 시작해보겠습니다.

```kotlin
// Sequentially executes doWorld followed by "Done"
fun main() = runBlocking {
    doWorld()
    println("Done")
}

// Concurrently executes both sections
suspend fun doWorld() = coroutineScope { // this: CoroutineScope
    launch {
        delay(2_000L)
        println("World 2")
    }
    launch {
        delay(1_000L)
        println("World 1")
    }
    println("Hello")
}
```

이 코드는 다음과 같은 결과를 출력합니다.

```
Hello
World 1
World 2
Done
```

`launch { ... }` 블록 내부의 두 코드 조각은 동시에 실행되며, 실행 시작 후 1초가 지나면 "World 1"이 출력되고,
2초가 지나면 "World 2"가 출력됩니다.
`doWorld` 내의 `coroutineScope`는 두 코루틴이 모두 완료된 후에만 완료됩니다.
따라서 `doWorld`는 두 코루틴이 모두 완료된 후에 반환되며, 그제서야 "Done"이 출력됩니다.

## 명시적인 Job

`launch` 코루틴 빌더가 반환하는 Job 객체는 시작된 코루틴에 대한 핸들 역할을 하며 이를 통해 명시적으로 완료될 때까지 기다릴 수 있습니다.
예를 들어, 자식 코루틴의 완료를 기다린 후에 "Done"을 출력하려면 다음과 같이 할 수 있습니다.

```kotlin
fun main() = runBlocking {
    val job = launch {
        delay(1_000L)
        println("World!")
    }
    println("Hello")
    job.join() // wait until child coroutine completes
    println("Done")
}
```

이 코드는 다음과 같은 결과를 출력합니다.

```
Hello
World!
Done
```

## 코루틴은 가볍다

코루틴은 경량이므로 JVM 스레드보다 자원 소모가 적습니다.