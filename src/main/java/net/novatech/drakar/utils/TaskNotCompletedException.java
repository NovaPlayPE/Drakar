package net.novatech.drakar.utils;

public class TaskNotCompletedException extends Exception {
    public TaskNotCompletedException(Task t) {
        super("Unable to complete task: " + t.ID);
        t.markNotCompleted();
    }
}