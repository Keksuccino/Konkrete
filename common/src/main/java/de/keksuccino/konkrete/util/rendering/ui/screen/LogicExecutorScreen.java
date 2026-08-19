package de.keksuccino.konkrete.util.rendering.ui.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Executes one callback during screen initialization and then yields control to its caller. */
public class LogicExecutorScreen extends Screen {

    /** Task executed after the screen becomes active. */
    @NotNull
    protected Runnable task;

    /** Creates a screen that runs the supplied task when initialized. */
    @NotNull
    public static LogicExecutorScreen build(@NotNull Runnable taskToExecuteOnScreenInit) {
        return new LogicExecutorScreen(taskToExecuteOnScreenInit);
    }

    /** Schedules the supplied task when the screen initializes. */
    protected LogicExecutorScreen(@NotNull Runnable taskToExecuteOnScreenInit) {
        super(Component.empty());
        this.task = taskToExecuteOnScreenInit;
    }

    /** Initializes resources required by this logic executor screen. */
    @Override
    protected void init() {
        this.task.run();
    }

}
