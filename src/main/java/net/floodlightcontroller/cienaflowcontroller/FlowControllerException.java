package net.floodlightcontroller.cienaflowcontroller;

/**
 * Created by shabirmean on 2017-12-26 with some hope.
 */
public class FlowControllerException extends Exception {
    public FlowControllerException() {
    }

    public FlowControllerException(String message) {
        super(message);
    }

    public FlowControllerException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
