/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-15 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.debug;

import com.sun.jdi.VirtualMachine;
import processing.app.RunnerListener;
import processing.app.SketchException;
import processing.app.exec.StreamRedirectThread;
import processing.mode.java.JavaBuild;
import processing.mode.java.runner.MessageSiphon;

/**
 * Runs a {@link JavaBuild}. Launches the build in a new debuggee VM.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class DebugRunner extends processing.mode.java.runner.Runner {

    // important inherited fields
    // protected VirtualMachine vm;
    public DebugRunner(JavaBuild build, RunnerListener listener) throws SketchException {
        super(build, listener);
    }

    /**
     * Launch the virtual machine. Simple non-blocking launch. VM starts
     * suspended.
     *
     * @return debuggee VM or null on failure
     */
    public VirtualMachine launch() {
//        String[] machineParamList = getMachineParams();
//        String[] sketchParamList = getSketchParams(false);
//        /*
//         * System.out.println("vm launch sketch params:"); for (int i=0;
//         * i<sketchParamList.length; i++) {
//         * System.out.println(sketchParamList[i]); } System.out.println("vm
//         * launch machine params:"); for (int i=0; i<machineParamList.length;
//         * i++) { System.out.println(machineParamList[i]); }
//         *
//         */
//        vm = launchVirtualMachine(machineParamList, sketchParamList); // will return null on failure
        if (launchVirtualMachine(false)) {  // will return null on failure
            redirectStreams(vm);
        }
        return vm;
    }

    /**
     * Redirect a VMs output and error streams to System.out and System.err
     *
     * @param vm the VM
     */
    protected void redirectStreams(VirtualMachine vm) {
        MessageSiphon ms = new MessageSiphon(process.getErrorStream(), this);
        errThread = ms.getThread();
        outThread = new StreamRedirectThread("VM output reader", process.getInputStream(), System.out);
        errThread.start();
        outThread.start();
    }

    /**
     * Additional access to the virtual machine. TODO: may not be needed
     *
     * @return debugge VM or null if not running
     */
    public VirtualMachine vm() {
        return vm;
    }
}
