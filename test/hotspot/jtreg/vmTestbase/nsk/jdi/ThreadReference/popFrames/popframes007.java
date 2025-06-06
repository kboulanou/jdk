/*
 * Copyright (c) 2002, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package nsk.jdi.ThreadReference.popFrames;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method:<br>
 * <code>com.sun.jdi.ThreadReference.popFrames()</code><br>
 * properly throws <i>IllegalArgumentException</i> - if frame is not
 * on specified thread's call stack.<p>
 *
 * The target VM executes two debuggee threads: <i>popframes007tMainThr</i>
 * and <i>popframes007tAuxThr</i>. Debugger part of the test tries to
 * pop stack frame of the <i>popframes007tAuxThr</i> thread using
 * reference of the <i>popframes007tMainThr</i> thread.
 */
public class popframes007 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ThreadReference.popFrames.popframes007t";

    // names of debuggee threads
    static final String DEBUGGEE_MAIN_THREAD_NAME = "popframes007tMainThr";
    static final String DEBUGGEE_AUX_THREAD_NAME  = "popframes007tAuxThr";

    // debuggee local var used to find needed stack frame
    static final String DEBUGGEE_LOCALVAR = "popframes007tFindMe";
    // debuggee field used to indicate that popping has been done
    static final String DEBUGGEE_FIELD = "leaveMethod";

    // debuggee source line where it should be stopped
    static final int DEBUGGEE_STOPATLINE = 86;

    static final int ATTEMPTS = 5;
    static final int DELAY = 500; // in milliseconds

    static final String COMMAND_READY = "ready";
    static final String COMMAND_GO = "go";
    static final String COMMAND_QUIT = "quit";

    private ArgumentHandler argHandler;
    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private VirtualMachine vm;
    private BreakpointRequest BPreq;
    private ObjectReference objRef;
    private volatile int tot_res = Consts.TEST_PASSED;
    private volatile boolean gotEvent = false;

    public static void main (String argv[]) {
        int result = run(argv,System.out);
        if (result != 0) {
            throw new RuntimeException("TEST FAILED with result " + result);
        }
    }

    public static int run(String argv[], PrintStream out) {
        return new popframes007().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "popframes007t.err> ");
        debuggee.resume();
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        Field doExit = null;
        try {
            // debuggee main class
            ReferenceType rType = debuggee.classByName(DEBUGGEE_CLASS);

            ThreadReference mainThread =
                debuggee.threadByFieldName(rType, "mainThread", DEBUGGEE_MAIN_THREAD_NAME);
            if (mainThread == null) {
                log.complain("TEST FAILURE: method Debugee.threadByFieldName() returned null for debuggee thread "
                             + DEBUGGEE_MAIN_THREAD_NAME);
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }

            ThreadReference auxThread =
                debuggee.threadByFieldName(rType, "auxThr", DEBUGGEE_AUX_THREAD_NAME);
            if (auxThread == null) {
                log.complain("TEST FAILURE: method Debugee.threadByFieldName() returned null for debuggee thread "
                             + DEBUGGEE_AUX_THREAD_NAME);
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }

            suspendAtBP(rType, DEBUGGEE_STOPATLINE);

            // debuggee field used to indicate that popping has been done
            doExit = rType.fieldByName(DEBUGGEE_FIELD);

            // debuggee stack frame to be popped which is not on specified thread's call stack
            StackFrame stFrame = findFrame(auxThread, DEBUGGEE_LOCALVAR);

            log.display("\nTrying to pop stack frame \"" + stFrame
                + "\"\n\tlocation \"" + stFrame.location()
                + "\"\n\tgot from thread reference \"" + auxThread
                + "\"\n\tand the frame is not on the following thread's call stack: \""
                + mainThread + "\" ...");

// Check the tested assersion
            try {
                mainThread.popFrames(stFrame);
                log.complain("TEST FAILED: expected IllegalArgumentException was not thrown"
                    + "\n\twhen attempted to pop stack frame \"" + stFrame
                    + "\"\n\tlocation \"" + stFrame.location()
                    + "\"\n\tgot from thread reference \"" + auxThread
                    + "\"\n\tand the frame is not on the following thread's call stack: \""
                    + mainThread + "\"");
                tot_res = Consts.TEST_FAILED;
            } catch(IllegalArgumentException ee) {
                log.display("CHECK PASSED: caught expected " + ee);
            } catch(UnsupportedOperationException une) {
                if (vm.canPopFrames()) {
                    une.printStackTrace();
                    log.complain("TEST FAILED: caught exception: " + une
                        + "\n\tHowever, VirtualMachine.canPopFrames() shows, that the target VM"
                        + "\n\tdoes support popping frames of a threads stack: "
                        + vm.canPopFrames());
                    tot_res = Consts.TEST_FAILED;
                } else {
                    log.display("Warinig: unable to test an assertion: caught exception: " + une
                        + "\n\tand VirtualMachine.canPopFrames() shows, that the target VM"
                        + "\n\tdoes not support popping frames of a threads stack as well: "
                        + vm.canPopFrames());
                }
            } catch(Exception ue) {
                ue.printStackTrace();
                log.complain("TEST FAILED: ThreadReference.popFrames(): caught unexpected "
                    + ue + "\n\tinstead of IllegalArgumentException"
                    + "\n\twhen attempted to pop stack \"" + stFrame
                    + "\"\n\tlocation \"" + stFrame.location()
                    + "\"\n\tgot from thread reference \"" + auxThread
                    + "\"\n\tand the frame is not on the following thread's call stack: \""
                    + mainThread + "\"");
                tot_res = Consts.TEST_FAILED;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
        } finally {
// Finish the test
            // force an method to exit
            if (objRef != null && doExit != null) {
                try {
                    objRef.setValue(doExit, vm.mirrorOf(true));
                } catch(Exception sve) {
                    sve.printStackTrace();
                }
            }
        }

        return quitDebuggee();
    }

    private StackFrame findFrame(ThreadReference thrRef, String varName) {
        try {
            List frames = thrRef.frames();
            Iterator iter = frames.iterator();
            while (iter.hasNext()) {
                StackFrame stackFr = (StackFrame) iter.next();
                try {
                    LocalVariable locVar =
                        stackFr.visibleVariableByName(varName);
                    // visible variable with the given name is found
                    if (locVar != null) {
                        objRef = (ObjectReference)
                            stackFr.getValue(locVar);
                        return stackFr;
                    }
                } catch(AbsentInformationException e) {
                    // this is not needed stack frame, ignoring
                } catch(NativeMethodException ne) {
                    // current method is native, also ignoring
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            tot_res = Consts.TEST_FAILED;
            throw new Failure("findFrame: caught unexpected exception: " + e);
        }
        throw new Failure("findFrame: needed debuggee stack frame not found");
    }

    private BreakpointRequest setBP(ReferenceType refType, int bpLine) {
        EventRequestManager evReqMan =
            debuggee.getEventRequestManager();
        Location loc;

        try {
            List locations = refType.allLineLocations();
            Iterator iter = locations.iterator();
            while (iter.hasNext()) {
                loc = (Location) iter.next();
                if (loc.lineNumber() == bpLine) {
                    BreakpointRequest BPreq =
                        evReqMan.createBreakpointRequest(loc);
                    log.display("created " + BPreq + "\n\tfor " + refType
                        + " ; line=" + bpLine);
                    return BPreq;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Failure("setBP: caught unexpected exception: " + e);
        }
        throw new Failure("setBP: location corresponding debuggee source line "
            + bpLine + " not found");
    }

    private void suspendAtBP(ReferenceType rType, int bpLine) {

        /**
         * This is a class containing a critical section which may lead to time
         * out of the test.
         */
        class CriticalSection extends Thread {
            public volatile boolean waitFor = true;

            public void run() {
                try {
                    do {
                        EventSet eventSet = vm.eventQueue().remove(DELAY);
                        if (eventSet != null) { // it is not a timeout
                            EventIterator it = eventSet.eventIterator();
                            while (it.hasNext()) {
                                Event event = it.nextEvent();
                                if (event instanceof VMDisconnectEvent) {
                                    log.complain("TEST FAILED: unexpected VMDisconnectEvent");
                                    break;
                                } else if (event instanceof VMDeathEvent) {
                                    log.complain("TEST FAILED: unexpected VMDeathEvent");
                                    break;
                                } else if (event instanceof BreakpointEvent) {
                                    if (event.request().equals(BPreq)) {
                                        log.display("expected Breakpoint event occured: "
                                            + event.toString());
                                        gotEvent = true;
                                        return;
                                    }
                                } else
                                    log.display("following JDI event occured: "
                                        + event.toString());
                            }
                        }
                    } while(waitFor);
                    log.complain("TEST FAILED: no expected Breakpoint event");
                    tot_res = Consts.TEST_FAILED;
                } catch (Exception e) {
                    e.printStackTrace();
                    tot_res = Consts.TEST_FAILED;
                    log.complain("TEST FAILED: caught unexpected exception: " + e);
                }
            }
        }
/////////////////////////////////////////////////////////////////////////////

        BPreq = setBP(rType, bpLine);
        BPreq.enable();
        CriticalSection critSect = new CriticalSection();
        log.display("\nStarting potential timed out section:\n\twaiting "
            + (argHandler.getWaitTime())
            + " minute(s) for JDI Breakpoint event ...\n");
        critSect.start();
        pipe.println(COMMAND_GO);
        try {
            critSect.join((argHandler.getWaitTime())*60000);
            if (critSect.isAlive()) {
                critSect.waitFor = false;
                throw new Failure("timeout occured while waiting for Breakpoint event");
            }
        } catch (InterruptedException e) {
            critSect.waitFor = false;
            throw new Failure("TEST INCOMPLETE: InterruptedException occured while waiting for Breakpoint event");
        } finally {
            BPreq.disable();
        }
        log.display("\nPotential timed out section successfully passed\n");
        if (gotEvent == false)
            throw new Failure("unable to suspend debuggee thread at breakpoint");
    }

    private int quitDebuggee() {
        log.display("Final resumption of debuggee VM");
        vm.resume();
        pipe.println(COMMAND_QUIT);
        debuggee.waitFor();
        int debStat = debuggee.getStatus();
        if (debStat != (Consts.JCK_STATUS_BASE + Consts.TEST_PASSED)) {
            log.complain("TEST FAILED: debuggee process finished with status: "
                + debStat);
            tot_res = Consts.TEST_FAILED;
        } else
            log.display("\nDebuggee process finished with the status: "
                + debStat);

        return tot_res;
    }
}
