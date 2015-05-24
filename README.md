# Android Notification Library

A notification library for Android applications.

By using this library, you can easily manage your notifications.

* send remote/global notification when application is in background.
* send local/global notification when application is in foreground.
* register a `NotificationListener` for updating the badge number of notification count.
* display a `NotificationBoard` with a list of current notifications.
* ...

## Overview

There are 3 major components:

* Notification Local (managing in-layout notifications)
* Notification Global (managing floating notifications)
* Notification Remote (managing status-bar notifications)

and 2 minor components:

* Notification Board (holding a notification list)
* Notification Effect (managing ringtone, vibration, ...)

Each notification can have its own layout and background. If not specified, the default layout and background will be used.

If a notification is sent to more than one component, its status is synchronized between those components.
Thus, canceling a notification in one component will trigger the cancel event in other components.

## Demo

Sample code is also available in this repository.

#### Notification Local

![local demo](https://github.com/lamydev/Android-Notification/blob/master/samples/demo/local.gif)

#### Notification Global

![global demo](https://github.com/lamydev/Android-Notification/blob/master/samples/demo/global.gif)

#### Notification Board

![global demo](https://github.com/lamydev/Android-Notification/blob/master/samples/demo/board.gif)

#### Notification Remote

![remote demo](https://github.com/lamydev/Android-Notification/blob/master/samples/demo/remote.gif)

## Programming Guide

### Compatibility
* minSdkVersion: 11

### Gradle

Android-Notification library is pushed to Maven Central as a AAR, so you just need to
declare the following dependency to your `build.gradle`.

``` xml
dependencies {
    compile 'com.github.lamydev:android-notification:2.0'
}
```

### Initialization

You need to tell `NotificationDelegater` the major components you want to use.

``` java
NotificationDelegater.initialize(context, components);
```

Available components:

* NotificationDelegater.LOCAL
* NotificationDelegater.GLOBAL
* NotificationDelegater.REMOTE

Once it has been inited, you can enable/disable a component at runtime.

For example, to disable the local notification:

``` java
NotificationLocal local = NotificationDelegater.getInstance().local();
local.setEnabled(false);
```

### Notification Local (in-layout notifications)

You need to add `NotificationView` to your layout.

Otherwise, nothing will be presented if you try to send a local notification.

``` xml
<zemin.notification.NotificationView
    android:id="@+id/nv"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"/>
```

Then attach it to `NotificationLocal`.

``` java
NotificationLocal local = NotificationDelegater.getInstance().local();
NotificationView view = (NotificationView) findViewById(R.id.nv);
local.setView(view);
```

To send a local notification:

``` java
NotificationBuilder.V1 builder = NotificationBuilder.local()
    .setIconDrawable(icon)
    .setTitle(title)
    .setText(text);
    
NotificationDelegater delegater = NotificationDelegater.getInstance();
delegater.send(builder.getNotification());
```

#### Customization

* Refer to the JavaDoc of `NotificationView`.
* Refer to the JavaDoc of `NotificationGlobal`.
* Implement `NotificationViewCallback` or `NotificationLocal#ViewCallback`.
  Pass an instance to `NotificationView#setCallback(NotificationViewCallback cb)`.

### Notification Global (floating notifications)

For global notification, you don't need to attach any `NotificationView`.
Instead, it will do that for you.

Once it has been fired, it is displayed on top of any screen and remains visible for its specified duration, regardless of the visibility of you application's main screen.

By default, all features are disabled. You need to manually enable them.

``` java
NotificationGlobal global = NotificationDelegater.getInstance().global();
global.setViewEnabled(true);
```

To send a global notification:

``` java
NotificationBuilder.V1 builder = NotificationBuilder.global()
    .setIconDrawable(icon)
    .setTitle(title)
    .setText(text);
    
NotificationDelegater delegater = NotificationDelegater.getInstance();
delegater.send(builder.getNotification());
```

#### Customization

* Refer to the JavaDoc of `NotificationView`.
* Refer to the JavaDoc of `NotificationGlobal`.
* Implement `NotificationViewCallback` or `NotificationGlobal#ViewCallback`.
  Pass an instance to `NotificationView#setCallback(NotificationViewCallback cb)`.

### Notification Board (notification list)

A notification board shows a list of currently delivered notifications.

Initially, the board is hidden.  You can open it by:

* invoking method: `NotificationBoard#open(boolean anim)`
* user gesture: touches inside a rectangular area on the screen and performs a scroll down action.

By default, user gesture is not supported since the touch area is not defined.

To specify a touch area, you can call `NotificationBoard#setInitialTouchArea(int l, int t, int r, int b)`.

#### Floating board

It is managed by the `NotificationGlobal`, so the board feature needs to be enabled before you can use it.

``` java
NotificationGlobal global = NotificationDelegater.getInstance().global();
global.setBoardEnabled(true);
```

In this case, the initial touch area is automatically set by `NotificationRootView`.

* if `NotificationView` is displayed, the initial touch area of the board would be the same as the view's dimension.
* if `NotificationView` is not displayed, the initial touch area would be reset to 0.

Thus, the board can be opened only when the `NotificationView` is displayed, and the user scrolls it down.

#### In-layout board

You just need to simply add `NotificationBoard` to your layout.... without attaching it to any
notification handler, such as `NotificationLocal`, `NotificationGlobal`.

``` xml
<zemin.notification.NotificationBoard
    android:id="@+id/board"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"/>
```

#### Customization

* Refer to the JavaDoc of `NotificationBoard`.
* Implement `NotificationBoardCallback`.
  Pass an instance to `NotificationBoard#setCallback(NotificationBoardCallback cb)`.

### Notification Remote (status-bar notifications)

This is eventually a StatusBar Notification.

``` java
NotificationBuilder.V2 builder = NotificationBuilder.remote()
    .setSmallIconResource(icon)
    .setTicker(tickerText)
    .setTitle(title)
    .setText(text);
    
NotificationDelegater delegater = NotificationDelegater.getInstance();
delegater.send(builder.getNotification());
```

#### Customization

* Refer to the JavaDoc of `NotificationRemote`.
* Implement `NotificationRemoteCallback`.
  Pass an instance to `NotificationRemote#setCallback`.

### Notification Listener

Implement this interface to listen to overall Notification status.

By default, all notifications will be delivered to all listeners.

Here's an example.

If you have a badge showing notification count, you may need register a `NotificationListener` to help keep it up-to-date.

![badge example](https://github.com/lamydev/Android-Notification/blob/master/samples/demo/badge_example.png)

``` java
public class MainActivity extends Activity {

    @Override
    protected void onResume() {
        super.onResume();

        NotificationDelegater.getInstance().addListener(mListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        NotificationDelegater.getInstance().removeListener(mListener);
    }

    private final NotificationListener mListener = new NotificationListener() {

        @Override
        public void onArrival(NotificationEntry entry) {
            updateNotificationCount();
        }

        @Override
        public void onCancel(NotificationEntry entry) {
            updateNotificationCount();
        }
    }

    private void updateNotificationCount() {
        final int count = NotificationDelegater.getInstance().getNotificationCount();
        // do something ...
    }
}
```

### Notification Effect

By default, notification effect is disabled for all components.

To enable it for a specific component, for example, for global notification:

``` java
NotificationGlobal global = NotificationDelegater.getInstance().global();
global.enableEffect(true);
```

Then you can play a ringtone when sending a global notification:

``` java
NotificationBuilder.V1 builder = NotificationBuilder.global()
    .setIconDrawable(icon)
    .setTitle(title)
    .setText(text)
    .setPlayRingtone(true)
    .setRingtone(context, resId);
    
NotificationDelegater delegater = NotificationDelegater.getInstance();
delegater.send(builder.getNotification());
```

## Developers
* Zemin Liu (lam2dev@gmail.com)

Any questions, contributions, bug fixes, and patches are welcomed. ^\_^

## License

```
Copyright (C) 2015 Zemin Liu

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
