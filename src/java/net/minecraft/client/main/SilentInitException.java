package net.minecraft.client.main;


public class SilentInitException extends RuntimeException {
    public SilentInitException(String pMessage) {
        super(pMessage);
    }

    public SilentInitException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}