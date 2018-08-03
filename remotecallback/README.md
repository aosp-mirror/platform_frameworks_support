# Remote Callbacks

Remote callbacks provide a wrapper that makes it easier for developers to
provide a PendingIntent. Generally those writing widgets, notifications, and
more recently slices, have the fun of writing code that looks like this
relatively frequently.

```java
public class MyReceiver extends BroadcastReceiver {
  final String ACTION_MY_CALLBACK_ACTION = "...";
  final String EXTRA_PARAM_1 = "...";
  final String EXTRA_PARAM_2 = "...";

  public PendingIntent getPendingIntent(Context context, int value1, int value2) {
    Intent intent = new Intent(context, MyReceiver.class);
    intent.setaction(ACTION_MY_CALLBACK_ACTION);
    intent.putExtra(EXTRA_PARAM_1, value1);
    intent.putExtra(EXTRA_PARAM_2, value2);
    intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
    return PendingIntent.getBroadcast(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public void onReceive(Context context, Intent intent) {
    if (ACTION_MY_CALLBACK_ACTION.equals(intent.getAction())) {
      int param1 = intent.getIntExtra(EXTRA_PARAM_1, 0);
      int param2 = intent.getintExtra(EXTRA_PARAM_2, 0);
      doMyAction(param1, param2);
    }
  }

  public void doMyAction(int value1, int value2) {
    ...
  }
}
```

The goal of Remote Callbacks is to remove as much of that fun as possible
and let you get right down to business. Which looks like this much abbreviated
version.

```java
public class MyReceiver extends BroadcastReceiverWithCallbacks {
  public PendingIntent getPendingIntent(Context context, int value1, int value2) {
    return createRemoteCallback(context, "doMyAction", value1, value2)
        .toPendingIntent();
  }

  @RemoteCallable
  public void doMyAction(int value1, int value2) {
    ...
  }
}
```

## Features

### Components

Currently BroadcastReceivers and ContentProviders are supported, but it is
relatively easy to add new components. Support will also be built into
AppWidgetProvider and SliceProvider to make it especially easy for those cases
to take advantage of these.

### Inputs

Remote Callbacks allow for caller supplied inputs to be handled alongside
receiver specified arguments. For instance, if a `SLIDER_VALUE` input is
declared, an app could easily create callbacks for several sliders.

```java
// These are callbacks that could be hooked up to sliders.
createRemoteCallback(context, "setSliderValue", R.id.slider_1, SLIDER_VALUE);
createRemoteCallback(context, "setSliderValue", R.id.slider_2, SLIDER_VALUE);
createRemoteCallback(context, "setSliderValue", R.id.slider_3, SLIDER_VALUE);
// This could be attached to a reset button.
createRemoteCallback(context, "setSliderValue", R.id.slider_1, 0);

public void setSliderValue(int slideId, int newValue) {
  ...
}
```

### Paramater types

Supported parameter types:

 - byte/Byte
 - char/Character
 - short/Short
 - int/Integer
 - long/Long
 - float/Float
 - double/Double
 - boolean/Boolean
 - String
 - Uri

Array support coming soon and possibly some other platform parcelables support.

### Compile time verification

At compile time Methods tagged with `@RemoteCallable` have hooks generated for
them. The vast majority of the calls are done through generated code directly,
so everything except for class names can be optimized/obfuscated. Given that
remote callbacks are only accessible on platform components such as receivers
and providers, they are already generally not able to be obfuscated.

### Runtime verification

When `createRemoteCallback` is called, all of the parameters will be validated.
If there is an incorrect number or a type mismatch, then the callback will not
be created and a `RuntimeException` will be generated. It would be nice to do
these checks at compile time, but the java annotation processor API makes that
very difficult.

### Security

Just like PendingIntents, Remote Callbacks don't require components be
exported. They also ensure that all parameters always have a value in the
PendingIntent generated, which ensures that the caller cannot inject new values
except when explicitly requested by the receiving app. They also generate the
intent Uris to ensure that the callbacks stay separate and don't collide with
each other.
