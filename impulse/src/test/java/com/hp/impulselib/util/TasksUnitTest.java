package com.hp.impulselib.util;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TasksUnitTest {
    Tasks tasks;
    Tasks.Task task;
    IOException ioException = new IOException("sample exception");

    @Before
    public void setup() {
        System.out.println("setup...");
        tasks = new Tasks();
        task = mock(Tasks.Task.class);
    }

    @After
    public void tearDown() {
        System.out.println("tearDown...");
        tasks.close();
    }

    private void flush() throws Exception {
        while(tasks.getTaskCount() > 0) {
            Thread.sleep(50);
        }
    }

    @Test
    public void startsAndStops() throws Exception {
        tasks.queue(task);
        verify(task, timeout(100)).run();
        verify(task, timeout(100)).onDone();
    }

    @Test
    public void fireError() throws Exception {
        doThrow(ioException).when(task).run();
        tasks.queue(task);
        verify(task, timeout(100)).run();
        verify(task, timeout(100)).onError(ioException);
    }

    @Test
    public void closeInterruptsTask() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                System.out.println("Answering run by sleeping 5000");
                // This should throw InterruptedException
                Thread.sleep(5000);
                return null;
            }
        }).when(task).run();
        tasks.queue(task);
        flush();
        tasks.close();

        // Task dies silently
        verify(task, timeout(250)).run();
        verify(task, never()).onDone();
        verify(task, never()).onError(any(IOException.class));
    }

    @Test
    public void closeIoErrorOk() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // This should throw InterruptedException
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // Throw an IOException when interrupted...
                    throw ioException;
                }
                return null;
            }
        }).when(task).run();
        tasks.queue(task);
        flush();
        tasks.close();

        // Task dies silently
        verify(task, timeout(100)).run();
        verify(task, never()).onDone();
        verify(task, never()).onError(any(IOException.class));
    }
}
