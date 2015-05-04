# Android Notification Library

A notification library for Android applications.

There are 3 major components:

* local notification (in-layout notification)
* global notification (floating notification)
* remote notification (Statusbar notification)

and 1 extra component:

* notification effect (ringtone, vibration)


By using this library, you can send notifications in different situations:

* send remote/global notification when application is in background.
* send local/global notification when application is in foreground.
* register a `NotificationListener` for updating the badge number of notification count.
* ...

Each notification can have its own layout and background. If not specified, the default layout and background will be used.

If a notification is sent to more than one component, its status is synchronized between those components.
Thus, canceling a notification in one component will trigger the cancel event in other components.

## Demo

### Local Notification

![local demo](https://github.com/lamydev/Android-Notification/blob/master/samples/demo/local.gif)

### Global Notification

![global demo](https://github.com/lamydev/Android-Notification/blob/master/samples/demo/global.gif)

### Remote Notification

![remote demo](https://github.com/lamydev/Android-Notification/blob/master/samples/demo/remote.gif)

Sample code is also available in this repository.

## Programming Guide

### Compatibility
* minSdkVersion: 11

### Initialization

You need to tell `NotificationDelegater` the major features you want to use.

``` java
NotificationDelegater delegater = NotificationDelegater.getInstance();
delegater.init(context, flags);
```

Available flags:

* NotificationDelegater.FLAG\_LOCAL\_NOTIFICATION
* NotificationDelegater.FLAG\_GLOBAL\_NOTIFICATION
* NotificationDelegater.FLAG\_REMOTE\_NOTIFICATION

### Local Notification

You need to add `NotificationView` to your layout.
Otherwise, nothing will be present if you try to send a local notification.

``` xml

<zemin.notification.NotificationView
    android:id="@+id/nv"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"/>

```

Then attach it to `NotificationLocal`.

``` java
NotificationDelegater delegater = NotificationDelegater.getInstance();
NotificationLocal local = delegater.local();
NotificationView view = (NotificationView) findViewById(R.id.nv);
local.setView(view);
```

Now it is ready to send such local notification.

``` java
NotificationEntry entry = NotificationEntry.create();
entry.sendToLocalView(true);
// ...
NotificationDelegater.getInstance().send(entry);
```

### Global Notification

For global notification, you don't need to attach any `NotificationView`.
Instead, it will do that for you.

However, once it has been fired, it is displayed on top of any screen and remains visible for its specified duration, regardless of the visibility of you application's main screen.

``` java
NotificationEntry entry = NotificationEntry.create();
entry.sendToGlobalView(true);
// ...
NotificationDelegater.getInstance().send(entry);
```

### Remote Notification

This is eventually a StatusBar Notification.

``` java
NotificationEntry entry = NotificationEntry.create();
entry.sendToRemote(true);
entry.setSmallIconResource(drawable);
// ...
NotificationDelegater.getInstance().send(entry);
```

### NotificationListener

Implement this interface to listen to overall Notification status.

By default, all notifications will be delivered to all listeners.

#### Use Case

For example, if you have a badge showing notification count, you may need register a `NotificationListener` to help keep it up-to-date.

``` java
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
    // refresh the TextView presenting the notification count...
}
```

### Notification Effect

By default, notification effect is disabled for all components.

To enable it for a specific component, for example, for global notification:

``` java
NotificationDelegater delegater = NotificationDelegater.getInstance();
NotificationGlobal global = delegater.global();
global.enableEffect(true);
```

Then you can play a ringtone when sending a global notification:

``` java
NotificationEntry entry = NotificationEntry.create();
entry.sendToGlobalView(true);
entry.setRingtone(context, resId);
NotificationDelegater.getInstance().send(entry);
```

## Customization

Some sample layouts are provided in this repository.
If you need a customized layout, you may have to implement it.

* For customizing notification view, you need to create a class implementing `NotificationView#Callback`, and pass an instance to `NotificationView` by calling `NotificationView#setCallback(Callback cb)`.

* For customizing remote notification, you need to create a class extending `NotificationRemoteFactory`, and pass an instance to `NotificationRemote` by calling `NotificationRemote#setFactory(NotificationRemoteFactory factory)`.

## Under Development

Notification panel.

## Developer
* Zemin Liu (lam2dev@gmail.com)

Any contributions, bug fixes, and patches are welcomed. ^\_^

## Apache License

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
