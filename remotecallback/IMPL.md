# Remote Callbacks Implementation Details

Remote Callbacks do not create a new technology or aim to have any complex
logic in them. They only aim to make the experience of PendingIntents cleaner
and easier for developers.

## Handling calls

Remote callbacks tries hard to avoid reflection in its implementation, and
does not trigger reflection during a repeated callback creations or executions.
The only reflection is used to initialize the handlers for a given receiver
class, so once per class.

For any class $S that is a CallbackReceiver and has callbacks, the compiler will
generate a class called $SInitializer that is a Runnable. The
CallbackHandlerRegistry will instantiate/run this initializer the first time it
encounters $S, and that code will in turn register a bunch of CallbackHandlers
that know how to translate to/from a Bundle so they can be shoved through
PendingIntents (or other IPC mechanisms).

```java
/**
 * The interface used to trigger a callback when the pending intent is fired.
 * Note: This should only be referenced by generated code, there is no reason to reference
 * this otherwise.
 * @param <T> The receiver type for this callback handler.
 */
public interface CallbackHandler<T extends CallbackReceiver> {
    /**
     * Executes a callback given a Bundle of aurgements.
     * Note: This should only be called by generated code, there is no reason to reference this
     * otherwise.
     */
    void executeCallback(T receiver, Bundle arguments);

    /**
     * Transforms arguments into a Bundle that can cross processes and later be used with
     * {@link #executeCallback(Object, Bundle)}.
     * Note: This should only be called by generated code, there is no reason to reference this
     * otherwise.
     */
    Bundle assembleArguments(Object... args);
}
```

The CallbackHandlerRegistry keeps a map of a handler for all methods/classes that are
`@RemoteCallable` that have been initalized. Whenever a call to
`createRemoteCallback` happens, it looks up the appropriate handler and asks it
to assembleArguments, which will invoke the generated code that can do runtime
checking of number of arguments, types, etc.

Similarly when a callback is triggered, the concrete implementation of
`CallbackReceiver` (e.g. BroadcastReceiverWithCallbacks) gets a PendingIntent
that it knows to be a callback, it triggers the CallbackHandlerRegistry, which
looks up the corresponding handler and triggers `executeCallback` which will
call the actual implementation of the method.

This handling of callbacks means we don't have to blindly keep all methods
related to callbacks. Only the classname (which is needed for the Manifest
anyway). Internal methods can be obfuscated and optimized however proguard or
other tools see fit without causing issues with the callbacks.

## Inputs

In some cases an input could be provided by the calling app, such as a
notification with inline reply, or when a slider is dragged in a slice. Remote
Callbacks handle these through the RemoteInputHolder. A RemoteInputHolder is a
completely opaque class to developers, and will only be created as constant
instances within the androidx APIs, to avoid accidental security holes by
developers.

The reason inputs can easily form a security hole is that the input must be
blank in the incoming pending intent extras, and therefore can easily be
set by the calling app. So they should only be set in cases where the app
wants external data and not for cases where the app wants to rename bundle keys
or something similar.

Now back to a reasonable use case, these will act as constants that just hold
the place of the input argument when creating the callback.

```java
createRemoteCallback(context, "setSliderPosition", SLICE_SLIDER_VALUE);
```

In this case, the generated code will check for arguments that are instances of
RemoteInputHolder. When it finds one, it will check the type of the input
against the argument in the method, and write a relay value into the input
arguments (such as "android.remotecallback:android.app.slice.extra.SLIDER\_VALUE").
When the argument is read out to call the method, it will see the relay value
and pull the actual value for the argument from the specified key (such as
"android.app.slice.extra.SLIDER\_VALUE").

## Providers

Since providers don't actually have a form of a PendingIntent, RemoteCallbacks
includes a broadcast stub that acts as a relay for this case, which receives
the intent, then triggers a call on the corresponding authority.

Since slices will already have access to the provider directly, this relay step
can be optimized out in newer clients, but will still operate with the relay in
the case of older clients that expect a PendingIntent.

## Services

Coming soon, they are on the TODO list.
