/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package processing.mode.java2;

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
        String[] machineParamList = getMachineParams();
        String[] sketchParamList = getSketchParams();
        /*
         * System.out.println("vm launch sketch params:"); for (int i=0;
         * i<sketchParamList.length; i++) {
         * System.out.println(sketchParamList[i]); } System.out.println("vm
         * launch machine params:"); for (int i=0; i<machineParamList.length;
         * i++) { System.out.println(machineParamList[i]); }
         *
         */
        vm = launchVirtualMachine(machineParamList, sketchParamList); // will return null on failure
        if (vm != null) {
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
        MessageSiphon ms = new MessageSiphon(vm.process().getErrorStream(), this);
        errThread = ms.getThread();
        outThread = new StreamRedirectThread("VM output reader", vm.process().getInputStream(), System.out);
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
