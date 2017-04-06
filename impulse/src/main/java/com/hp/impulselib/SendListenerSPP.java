package com.hp.impulselib;

/** Interface used to watch the progress of an object transfer */
public interface SendListenerSPP extends OperationListener {
    /** The job is in progress with the specified amount of data sent */
    void onProgress(int total, int sent, boolean progress);
}
