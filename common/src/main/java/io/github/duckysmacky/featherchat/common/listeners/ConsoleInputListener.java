package io.github.duckysmacky.featherchat.common.listeners;

import java.util.Scanner;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/// A `Runnable` class responsible for continuously listening for input from the system's console and invoking a
/// callback on each received input.
///
/// It is **used in a thread** in order to constantly read input line-by-line from the system console (`System.in`) via
/// a `Scanner`. A fully read line is considered to be valid input if it is not black. The input is later processed by
/// passing it to the provided `onInputCallback` consumer function. If there are no more available lines to read (e.g.,
/// user pressed `CTRL + D`), the provided `onEOFCallback` runnable function will be invoked and the continuous loop
/// will break.
///
/// The thread will keep on running until the `runFlag` will evaluate to be `false` or there is no more input to be read
/// from the user. The `runFlag` is provided via a `BooleanSupplier` which can accept any function which in turn returns
/// a `boolean`. The flag is checked on each iteration, and it makes sure that the loop will only keep iterating if it
/// evaluates to `true`, as well as it will block the invocation of the provided input callback in case it evaluates to
/// `false`
public class ConsoleInputListener implements Runnable {
    private final BooleanSupplier runFlag;
    private final Consumer<String> onInputCallback;
    private final Runnable onEOFCallback;

    public ConsoleInputListener(
        BooleanSupplier runFlag,
        Consumer<String> onInputCallback,
        Runnable onEOFCallback
    ) {
        this.runFlag = runFlag;
        this.onInputCallback = onInputCallback;
        this.onEOFCallback = onEOFCallback;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        while (runFlag.getAsBoolean()) {
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!runFlag.getAsBoolean()) break;

                if (!line.isBlank())
                    onInputCallback.accept(line);
            } else {
                onEOFCallback.run();
                break;
            }
        }

        scanner.close();
    }
}