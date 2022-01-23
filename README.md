<div align="center">
  <h1>DynamicClass</h1>
  <p>DynamicClass is a simple, lightweight & easy to use utility which helps you remove bloat from your code by handling the class initializing when you need it.</p>
</div>

# Features
DynamicClass supports:
- [x] Constructor(s) 
- [x] Superclass(es)
- [x] Interface(s)
- [-] Interface(s) with Superclass(es) 

# How to use
It's extremely simple! All you need to do is, annotate the class that you'd want to be automatically created with @AutoRegister, 
and in your application, run DynamicClass#scan method. that's it!

## Maven
```xml
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>

        <dependency>
            <groupId>com.github.Tofpu</groupId>
            <artifactId>DynamicClass</artifactId>
            <version>ed459843c9</version>
        </dependency>
```

## Example:
You can view the example code at <https://github.com/Tofpu/DynamicClass/tree/main/src/main/java/example>
