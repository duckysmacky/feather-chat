package io.github.duckysmacky.featherchat.common;

import java.util.concurrent.BlockingQueue;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/// A `Runnable` class responsible for continuously listening for messages from a provided message pool and invoking a
/// callback on each received message.
///
/// It is **used in a thread** in order to constantly read messages from a message pool backed by a `BlockingQueue` and
/// process them. If there are no more messages left in the pool, the thread will wait until a new one will be available.
/// After taking a message from a pool it will process it by submitting it to the provided `onMessageCallback` consumer
/// function.
///
/// The thread will keep on running until the `runFlag` will evaluate to be `false`. The `runFlag` is provided via a
/// `BooleanSupplier` which can accept any function which in turn returns a `boolean`. The flag is checked on each
/// iteration, and it makes sure that the loop will only keep iterating if it evaluates to `true`, as well as it will
/// block the invocation of the provided message callback in case it evaluates to `false`
public class MessageListener implements Runnable {
    private final BooleanSupplier runFlag;
    private final BlockingQueue<Message> messagePool;
    private final Consumer<Message> onMessageCallback;

    public MessageListener(
        BooleanSupplier runFlag,
        BlockingQueue<Message> messagePool,
        Consumer<Message> onMessageCallback
    ) {
        this.runFlag = runFlag;
        this.messagePool = messagePool;
        this.onMessageCallback = onMessageCallback;
    }

    @Override
    public void run() {
        while (runFlag.getAsBoolean()) {
            try {
                Message message = messagePool.take();

                if (runFlag.getAsBoolean())
                    onMessageCallback.accept(message);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
