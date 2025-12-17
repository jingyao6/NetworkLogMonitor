# NetworkLogMonitor

NetworkLogMonitor 是一个 Android 网络日志监控库，用于捕获和展示应用的网络请求和响应信息。

## 功能特性

- 📱 实时捕获网络请求和响应
- 📊 显示请求方法、URL、状态码、响应时间
- 📝 记录请求头、请求体、响应头、响应体
- 🔍 支持按 URL 模糊搜索日志
- 🎯 悬浮窗实时显示日志数量
- 🎨 直观的可视化界面
- 🌈 根据状态码自动显示不同颜色
- 📱 悬浮窗智能显示（仅在应用前台显示）
- 🔄 支持在详情页重新发起请求
- 🛠️ 支持自定义请求字段编辑
- 🔐 支持自定义加解密处理
- ⚡ 重新请求后实时更新界面显示

## 集成方法

### 1. 添加依赖

将 NetworkLogMonitor-1.0.2.aar 文件复制到您的项目 libs 目录下，然后在模块的 build.gradle 文件中添加：

```groovy
// 在 repositories 中添加 libs 目录
repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    implementation (name: 'NetworkLogMonitor-1.0.2', ext: 'aar')
    
    // 确保项目中已添加以下依赖
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
}
```

### 2. 添加权限

在 AndroidManifest.xml 文件中添加以下权限：

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 3. 注册服务

在 AndroidManifest.xml 文件中注册悬浮窗服务：

```xml
<application>
    <!-- ... 其他配置 ... -->
    
    <service
        android:name="com.jy.networklogmonitor.FloatingService"
        android:exported="false" />
</application>
```

## 使用方法

### 1. 初始化库

在应用启动时（如 Application 类的 onCreate 方法或 MainActivity 的 onCreate 方法）初始化库：

```java
NetworkLogMonitor.initialize(this);
```

### 2. 添加 OkHttp 拦截器

在创建 OkHttpClient 实例时，添加 NetworkLogInterceptor：

```java
OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .addInterceptor(new NetworkLogInterceptor())
        // 或者使用便捷方法
        // .addInterceptor(NetworkLogMonitor.getInterceptor())
        // 其他配置
        .build();
```

### 3. 设置自定义加解密处理（可选）

如果需要对请求体和响应体进行自定义加解密处理，可以实现 EncryptionHandler 接口：

```java
NetworkLogInterceptor.setEncryptionHandler(new NetworkLogInterceptor.EncryptionHandler() {
    @Override
    public String encryptRequestBody(String url, RequestBody originalBody) {
        // 根据 URL 决定是否加密
        if (url.contains("/api/secure/")) {
            // 加密逻辑
            try {
                Buffer buffer = new Buffer();
                originalBody.writeTo(buffer);
                String originalContent = buffer.readUtf8();
                return encrypt(originalContent); // 自定义加密方法
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 不加密，直接返回原始内容
        try {
            Buffer buffer = new Buffer();
            originalBody.writeTo(buffer);
            return buffer.readUtf8();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
    
    @Override
    public String decryptResponseBody(String url, ResponseBody responseBody) {
        // 根据 URL 决定是否解密
        if (url.contains("/api/secure/")) {
            // 解密逻辑
            try {
                String encryptedContent = responseBody.string();
                return decrypt(encryptedContent); // 自定义解密方法
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 不解密，直接返回原始内容
        try {
            return responseBody.string();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
});
```

### 4. 停止监控（可选）

如果需要停止监控，可以调用：

```java
NetworkLogMonitor.stop(this);
```

## 功能说明

### 悬浮窗

- 实时显示当前捕获的日志数量
- 只有当应用在前台时才会显示
- 点击悬浮窗可打开日志列表页面
- 支持拖拽移动位置

### 日志列表页面

- 显示所有捕获的网络请求
- 每条日志显示：方法、状态码、响应时间、URL、时间戳
- 支持按 URL 进行模糊搜索
- 支持清除所有日志
- 点击日志项可查看详情

### 日志详情页面

- 显示完整的请求和响应信息
- 包括：请求方法、状态码、响应时间、URL、时间戳
- 显示请求头和响应头
- 显示请求体和响应体
- 支持编辑请求字段：URL、请求头、请求体
- 支持重新发起请求
- 重新请求后实时更新界面显示
- 支持查看请求失败时的错误信息
- 根据状态码自动显示不同颜色
- 根据请求方法自动显示不同颜色

## 注意事项

1. 本库使用 OkHttp 拦截器捕获网络请求，仅支持使用 OkHttp 的网络请求
2. 悬浮窗功能需要系统权限，请确保在 Android 6.0+ 设备上动态请求权限
3. 日志数据存储在内存中，应用重启后会丢失
4. 建议仅在开发和测试环境中使用，生产环境中请移除

## 技术栈

- Java
- OkHttp 4.11.0
- AndroidX
- Material Design

## 版本历史
- v1.0.2
 - 支持在详情页重新发起请求
  - 支持自定义请求字段编辑
  - 支持自定义加解密处理
  - 重新请求后实时更新界面显示
  - 修复了连接失败或超时等状态的显示
- v1.0.1
  - 修复了 ResponseBody.create() 方法参数顺序问题
  - 实现了悬浮窗日志数量动态更新
  - 实现了应用前后台状态监听，后台时隐藏悬浮窗
  - 添加了搜索功能
- v1.0.0
  - 初始版本
  - 实现了基本的网络日志捕获和显示功能
  - 支持悬浮窗显示

## 许可证

MIT License
