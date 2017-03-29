# UFInjector - User Friendly Injector
Small library that is focused to provide fluent API for creating Dagger2 components in Android.
After using same logic from project to project I tried to extract common part into this library to be shared between all of them.
Feel free to try it on you own and check if it fulfil your need.

### Usage
Main entry point to library is Injector.
1) In order to create request start with method 'with' and provide ComponentReleaser.
There is only one default ActivityComponentReleaser coming out of the box. It can be used also in fragments providing activity with method getActivity() (with extreme care)
```java
InjectRequest request = Injector.with(this);
```

2) 'with' will return InjectRequest object on which you can call several methods to adjust inject request.
```java
retainOnConfigChange(boolean retainOnChange)
```
Inform injector to put component into singleton cache to prevent it's destroying between config changes (like activity rotate).
Make sure you not hold any Context reference in this component, otherwise they will leak.

```java
allowComponentDuplicates(String key) // by default componentCache singleton
```
By providing this key, you could have many of Components of the same type in memory at a time.

```java
build(Class<T> componentClass, ComponentFactory<T> componentFactory)
```
Final step is to build Component. Need to provide component class and implementation of ComponentFactory.
If no component was persisted before, InjectRequest will refer to componentFactory to get component instance.

At this point, returned component can be used to inject dependencies.

3) i.e in Activity onCreate (java8 method reference used for create ComponentFactory impl):
```java
Injector.with(this)
  .retainOnConfigChange(true)
  .build(MyComponent.class, MyComponent::create)
  .inject(this)
```

### Installation
Because I don't want to share it via jcenter now, it requires additional step to add maven repo:
```groovy
repositories {
    jcenter()
        maven {
            url 'https://dl.bintray.com/knight704/ufinjector/'
        }
    }
```

And then as usual in build.gradle:
```
compile 'com.google.dagger:dagger:2.8'
annotationProcessor 'com.google.dagger:dagger-compiler:2.8'
compile 'knight704.ufinjector:injector:+'
```

### Todos
  - Come up with ComponentReleaser implementation for Fragments, since they have complex lifecycle.
  - ???